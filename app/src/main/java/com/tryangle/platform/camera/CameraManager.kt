package com.tryangle.platform.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera2 API 매니저
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var captureImageReader: ImageReader? = null
    
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    
    private var currentConfig: CameraConfig = CameraConfig.DEFAULT
    private var cameraId: String? = null
    
    private val _state = MutableStateFlow<CameraState>(CameraState.Idle)
    val state: StateFlow<CameraState> = _state.asStateFlow()
    
    suspend fun openCamera(config: CameraConfig = CameraConfig.DEFAULT) {
        if (_state.value.isActive()) return
        
        _state.value = CameraState.Opening
        currentConfig = config
        
        try {
            val foundCameraId = findCamera(config.lensFacing)
                ?: throw IllegalStateException("Camera not found")
            
            cameraId = foundCameraId
            startCameraThread()
            
            val device = openCameraDevice(foundCameraId)
            cameraDevice = device
            
            _state.value = CameraState.Opened
        } catch (e: Exception) {
            _state.value = CameraState.Error("Failed to open camera: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun startPreview(surface: Surface) {
        val device = cameraDevice ?: throw IllegalStateException("Camera not opened")
        
        try {
            // 분석용 ImageReader (필요할 때만 생성하거나 항상 비워줘야 함)
            imageReader = ImageReader.newInstance(
                currentConfig.previewWidth,
                currentConfig.previewHeight,
                ImageFormat.YUV_420_888,
                3 // 버퍼를 약간 늘림
            )
            
            // ImageReader가 버퍼를 점유하지 않도록 더미 리스너 등록 (분석 전까지)
            imageReader?.setOnImageAvailableListener({ reader ->
                reader.acquireLatestImage()?.close()
            }, cameraHandler)

            captureImageReader = ImageReader.newInstance(
                currentConfig.captureWidth,
                currentConfig.captureHeight,
                ImageFormat.JPEG,
                2
            )
            
            val surfaces = listOf(surface, imageReader!!.surface)
            val session = createCaptureSession(device, surfaces)
            captureSession = session
            
            val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                addTarget(imageReader!!.surface)
                
                // 최적의 FPS 범위 선택
                val fpsRange = selectBestFpsRange(device.id, currentConfig.targetFps)
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                
                // 60fps를 위해 AE 모드 최적화
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                // 비디오 안정화가 프레임드랍을 유발할 수 있으므로 설정 확인 필요
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
            }.build()
            
            session.setRepeatingRequest(captureRequest, null, cameraHandler)
            _state.value = CameraState.Streaming

        } catch (e: Exception) {
            _state.value = CameraState.Error("Failed to start preview: ${e.message}", e)
            throw e
        }
    }

    private fun selectBestFpsRange(cameraId: String, targetFps: Int): Range<Int> {
        val chars = cameraManager.getCameraCharacteristics(cameraId)
        val ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()

        Log.d("CameraManager", "Available FPS Ranges: ${ranges.joinToString()}")

        // 1. 고정 FPS 범위 확인 (e.g., 60-60)
        ranges.find { it.lower == targetFps && it.upper == targetFps }?.let { return it }

        // 2. targetFps를 포함하는 가장 넓은 범위 확인 (e.g., 30-60)
        return ranges.filter { it.upper >= targetFps }
            .minByOrNull { it.lower } ?: Range(30, 30)
    }
    
    fun previewFrames() = callbackFlow<CameraFrame.Preview> {
        val reader = imageReader ?: return@callbackFlow
        
        val listener = ImageReader.OnImageAvailableListener { r ->
            val image = r.acquireLatestImage() ?: return@OnImageAvailableListener
            val frame = CameraFrame.Preview(image, image.timestamp, 0)
            if (!trySend(frame).isSuccess) {
                image.close() // 소비되지 않으면 즉시 닫기
            }
        }
        
        reader.setOnImageAvailableListener(listener, cameraHandler)
        awaitClose { reader.setOnImageAvailableListener(null, null) }
    }
    
    suspend fun captureImage(): Bitmap? {
        val device = cameraDevice ?: return null
        val session = captureSession ?: return null
        val reader = captureImageReader ?: return null
        
        _state.value = CameraState.Capturing
        return try {
            suspendCancellableCoroutine { continuation ->
                reader.setOnImageAvailableListener({ r ->
                    val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        continuation.resume(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    } finally {
                        image.close()
                        _state.value = CameraState.Streaming
                    }
                }, cameraHandler)
                
                val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                }.build()
                session.capture(request, null, cameraHandler)
            }
        } catch (e: Exception) {
            _state.value = CameraState.Error("Capture failed", e)
            null
        }
    }
    
    fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        captureImageReader?.close()
        captureImageReader = null
        stopCameraThread()
        _state.value = CameraState.Idle
    }
    
    /**
     * 카메라 특성 가져오기
     */
    fun getCameraCharacteristics(): CameraCharacteristics? {
        val id = cameraId ?: return null
        return cameraManager.getCameraCharacteristics(id)
    }
    
    /**
     * 센서 방향 가져오기
     * 
     * @return 센서 방향 (0, 90, 180, 270)
     */
    fun getSensorOrientation(): Int {
        return getCameraCharacteristics()
            ?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }
    
    /**
     * 최적의 프리뷰 크기 가져오기
     * 
     * @param surfaceWidth Surface 가로 크기
     * @param surfaceHeight Surface 세로 크기
     * @return 최적 프리뷰 크기
     */
    fun getOptimalPreviewSize(surfaceWidth: Int, surfaceHeight: Int): android.util.Size {
        val characteristics = getCameraCharacteristics()
            ?: return android.util.Size(currentConfig.previewWidth, currentConfig.previewHeight)
        
        return PreviewSize.chooseOptimalSize(
            characteristics,
            surfaceWidth,
            surfaceHeight,
            currentConfig.previewWidth,
            currentConfig.previewHeight
        )
    }

    private fun findCamera(lensFacing: Int): String? {
        return cameraManager.cameraIdList.find { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == lensFacing
        }
    }
    
    private suspend fun openCameraDevice(cameraId: String): CameraDevice = suspendCancellableCoroutine { cont ->
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(c: CameraDevice) = cont.resume(c)
            override fun onDisconnected(c: CameraDevice) = c.close()
            override fun onError(c: CameraDevice, e: Int) = c.close()
        }, cameraHandler)
    }
    
    private suspend fun createCaptureSession(device: CameraDevice, surfaces: List<Surface>): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(s: CameraCaptureSession) = cont.resume(s)
            override fun onConfigureFailed(s: CameraCaptureSession) = cont.resumeWithException(IllegalStateException())
        }
        device.createCaptureSession(SessionConfiguration(SessionConfiguration.SESSION_REGULAR, surfaces.map { OutputConfiguration(it) }, context.mainExecutor, callback))
    }
    
    private fun startCameraThread() {
        cameraThread = HandlerThread("CameraThread").apply { start(); cameraHandler = Handler(looper) }
    }
    
    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }
}
