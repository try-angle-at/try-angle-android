package com.tryangle.platform.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
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

enum class FlashMode {
    OFF, ON, AUTO
}

/**
 * Camera2 API 매니저 - 갤럭시 S25 4K 60fps 최적화 버전
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
    
    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()
    
    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()
    
    private var maxZoom: Float = 1.0f
    private var previewRequest: CaptureRequest.Builder? = null
    
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
            imageReader = ImageReader.newInstance(
                currentConfig.previewWidth,
                currentConfig.previewHeight,
                ImageFormat.YUV_420_888,
                3
            )
            
            imageReader?.setOnImageAvailableListener({ reader ->
                reader.acquireLatestImage()?.close()
            }, cameraHandler)

            captureImageReader = ImageReader.newInstance(
                currentConfig.captureWidth,
                currentConfig.captureHeight,
                ImageFormat.JPEG,
                2
            )
            
            val surfaces = listOf(surface, imageReader!!.surface, captureImageReader!!.surface)
            val session = createCaptureSession(device, surfaces)
            captureSession = session
            
            previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                addTarget(imageReader!!.surface)
                
                val fpsRange = selectBestFpsRange(device.id, currentConfig.targetFps)
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                
                applyFlashMode(this, _flashMode.value)
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
            }
            
            maxZoom = getMaxZoom()
            
            session.setRepeatingRequest(previewRequest!!.build(), null, cameraHandler)
            _state.value = CameraState.Streaming

        } catch (e: Exception) {
            _state.value = CameraState.Error("Failed to start preview: ${e.message}", e)
            throw e
        }
    }

    private fun selectBestFpsRange(cameraId: String, targetFps: Int): Range<Int> {
        val chars = cameraManager.getCameraCharacteristics(cameraId)
        val ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
        ranges.find { it.lower == targetFps && it.upper == targetFps }?.let { return it }
        return ranges.filter { it.upper >= targetFps }
            .minByOrNull { it.lower } ?: Range(30, 30)
    }
    
    fun previewFrames() = callbackFlow<CameraFrame.Preview> {
        val reader = imageReader ?: return@callbackFlow
        val listener = ImageReader.OnImageAvailableListener { r ->
            val image = r.acquireLatestImage() ?: return@OnImageAvailableListener
            val frame = CameraFrame.Preview(image, image.timestamp, 0)
            if (!trySend(frame).isSuccess) image.close()
        }
        reader.setOnImageAvailableListener(listener, cameraHandler)
        awaitClose { reader.setOnImageAvailableListener(null, null) }
    }
    
    fun setFlashMode(mode: FlashMode) {
        _flashMode.value = mode
        val session = captureSession ?: return
        val request = previewRequest ?: return
        applyFlashMode(request, mode)
        try {
            session.setRepeatingRequest(request.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e("CameraManager", "Flash error: ${e.message}")
        }
    }

    private fun applyFlashMode(builder: CaptureRequest.Builder, mode: FlashMode) {
        when (mode) {
            FlashMode.OFF -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
            FlashMode.ON -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
            FlashMode.AUTO -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
        }
    }

    suspend fun switchCamera(currentSurface: Surface? = null) {
        val newLensFacing = if (currentConfig.lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        
        closeCamera()
        currentConfig = currentConfig.copy(lensFacing = newLensFacing)
        openCamera(currentConfig)
        
        currentSurface?.let { startPreview(it) }
    }

    fun setZoom(ratio: Float) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        val clampedRatio = ratio.coerceIn(1.0f, maxZoom)
        _zoomRatio.value = clampedRatio
        
        val characteristics = getCameraCharacteristics() ?: return
        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
        
        val cropWidth = (sensorRect.width() / clampedRatio).toInt()
        val cropHeight = (sensorRect.height() / clampedRatio).toInt()
        val cropX = (sensorRect.width() - cropWidth) / 2
        val cropY = (sensorRect.height() - cropHeight) / 2
        
        request.set(CaptureRequest.SCALER_CROP_REGION, Rect(cropX, cropY, cropX + cropWidth, cropY + cropHeight))
        try {
            session.setRepeatingRequest(request.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e("CameraManager", "Zoom failed: ${e.message}")
        }
    }

    fun setFrameRate(fps: Int) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        val device = cameraDevice ?: return
        val fpsRange = selectBestFpsRange(device.id, fps)
        request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        try {
            session.setRepeatingRequest(request.build(), null, cameraHandler)
            currentConfig = currentConfig.copy(targetFps = fps)
        } catch (e: Exception) {
            Log.e("CameraManager", "FPS fail: ${e.message}")
        }
    }

    /**
     * 노출 보정 설정
     */
    fun setExposureCompensation(value: Int) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        val characteristics = getCameraCharacteristics() ?: return
        val range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return
        
        val clampedValue = value.coerceIn(range.lower, range.upper)
        request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, clampedValue)
        
        try {
            session.setRepeatingRequest(request.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e("CameraManager", "Exposure failed: ${e.message}")
        }
    }

    /**
     * 포커스 모드 설정
     */
    fun setFocusMode(auto: Boolean) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        
        val mode = if (auto) {
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        } else {
            CaptureRequest.CONTROL_AF_MODE_OFF
        }
        
        request.set(CaptureRequest.CONTROL_AF_MODE, mode)
        
        try {
            session.setRepeatingRequest(request.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e("CameraManager", "Focus failed: ${e.message}")
        }
    }

    suspend fun captureImage(): ByteArray? {
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
                        continuation.resume(bytes)
                    } finally {
                        image.close()
                        _state.value = CameraState.Streaming
                    }
                }, cameraHandler)
                
                val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    applyFlashMode(this, _flashMode.value)
                }.build()
                session.capture(request, null, cameraHandler)
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Capture fail: ${e.message}")
            _state.value = CameraState.Streaming
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

    fun getCameraCharacteristics(): CameraCharacteristics? = cameraId?.let { cameraManager.getCameraCharacteristics(it) }
    fun getSensorOrientation(): Int = getCameraCharacteristics()?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    fun getMaxZoom(): Float = getCameraCharacteristics()?.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
    
    fun getOptimalPreviewSize(width: Int, height: Int): Size {
        val chars = getCameraCharacteristics() ?: return Size(currentConfig.previewWidth, currentConfig.previewHeight)
        return PreviewSize.chooseOptimalSize(chars, width, height, currentConfig.previewWidth, currentConfig.previewHeight)
    }

    private fun findCamera(lensFacing: Int): String? = cameraManager.cameraIdList.find { id ->
        cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == lensFacing
    }
    
    private suspend fun openCameraDevice(id: String): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) = cont.resume(c)
                override fun onDisconnected(c: CameraDevice) { c.close(); if(cont.isActive) cont.resumeWithException(IllegalStateException("Disconnected")) }
                override fun onError(c: CameraDevice, e: Int) { c.close(); if(cont.isActive) cont.resumeWithException(IllegalStateException("Error: $e")) }
            }, cameraHandler)
        } catch (e: SecurityException) {
            cont.resumeWithException(e)
        }
    }
    
    private suspend fun createCaptureSession(d: CameraDevice, s: List<Surface>): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
            override fun onConfigureFailed(s: CameraCaptureSession) = cont.resumeWithException(IllegalStateException("Session failed"))
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+
                val outputConfigs = s.map { OutputConfiguration(it) }
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputConfigs,
                    context.mainExecutor,
                    callback
                )
                d.createCaptureSession(sessionConfig)
            } else {
                // API 26, 27
                @Suppress("DEPRECATION")
                d.createCaptureSession(s, callback, cameraHandler)
            }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
    
    private fun startCameraThread() {
        if (cameraThread == null) {
            cameraThread = HandlerThread("CameraThread").apply { start(); cameraHandler = Handler(looper) }
        }
    }
    
    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }
}
