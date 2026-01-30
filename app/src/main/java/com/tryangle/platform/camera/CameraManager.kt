package com.tryangle.platform.camera

import android.content.Context
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
 * Camera2 API 매니저 - 플래시 및 제어 기능 최적화 버전
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
    
    private val TAG = "CameraManager"

    suspend fun openCamera(config: CameraConfig = CameraConfig.DEFAULT) {
        if (_state.value.isActive()) return
        _state.value = CameraState.Opening
        currentConfig = config
        
        try {
            val foundCameraId = findCamera(config.lensFacing)
                ?: throw IllegalStateException("No camera found")
            
            cameraId = foundCameraId
            startCameraThread()
            cameraDevice = openCameraDevice(foundCameraId)
            _state.value = CameraState.Opened
        } catch (e: Exception) {
            _state.value = CameraState.Error("Open failed: ${e.message}", e)
            closeCamera()
            throw e
        }
    }
    
    suspend fun startPreview(surface: Surface) {
        val device = cameraDevice ?: throw IllegalStateException("Device null")
        
        try {
            val chars = getCameraCharacteristics() ?: throw IllegalStateException("No chars")
            val optimalSize = PreviewSize.chooseOptimalSize(chars, 1080, 1920, currentConfig.previewWidth, currentConfig.previewHeight)

            imageReader = ImageReader.newInstance(optimalSize.width, optimalSize.height, ImageFormat.YUV_420_888, 3).apply {
                setOnImageAvailableListener({ r -> r.acquireLatestImage()?.close() }, cameraHandler)
            }

            captureImageReader = ImageReader.newInstance(currentConfig.captureWidth, currentConfig.captureHeight, ImageFormat.JPEG, 2)
            
            val surfaces = listOf(surface, imageReader!!.surface, captureImageReader!!.surface)
            captureSession = createCaptureSession(device, surfaces)
            
            previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                addTarget(imageReader!!.surface)
                
                val fpsRange = selectBestFpsRange(device.id, currentConfig.targetFps)
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                
                // 현재 플래시 모드 적용
                applyFlashToRequestBuilder(this, _flashMode.value, isPreview = true)
            }
            
            maxZoom = getMaxZoom()
            captureSession?.setRepeatingRequest(previewRequest!!.build(), null, cameraHandler)
            _state.value = CameraState.Streaming

        } catch (e: Exception) {
            _state.value = CameraState.Error("Preview failed: ${e.message}", e)
            throw e
        }
    }

    fun setFlashMode(mode: FlashMode) {
        _flashMode.value = mode
        val session = captureSession ?: return
        val request = previewRequest ?: return
        
        applyFlashToRequestBuilder(request, mode, isPreview = true)
        
        try {
            session.setRepeatingRequest(request.build(), null, cameraHandler)
            Log.d(TAG, "Flash mode set to $mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set flash mode", e)
        }
    }

    private fun applyFlashToRequestBuilder(builder: CaptureRequest.Builder, mode: FlashMode, isPreview: Boolean) {
        val chars = getCameraCharacteristics() ?: return
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        if (!hasFlash) return

        when (mode) {
            FlashMode.OFF -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
            FlashMode.ON -> {
                if (isPreview) {
                    // 프리뷰 중에는 토치(조명)를 켜서 'ON'임을 알림
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    // 실제 촬영 시에는 강제 플래시 발광
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                }
            }
            FlashMode.AUTO -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
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
        val sensorRect = getCameraCharacteristics()?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
        val cropW = (sensorRect.width() / clampedRatio).toInt()
        val cropH = (sensorRect.height() / clampedRatio).toInt()
        val cropX = (sensorRect.width() - cropW) / 2
        val cropY = (sensorRect.height() - cropH) / 2
        request.set(CaptureRequest.SCALER_CROP_REGION, Rect(cropX, cropY, cropX + cropW, cropY + cropH))
        try { session.setRepeatingRequest(request.build(), null, cameraHandler) } catch (e: Exception) {}
    }

    fun setFrameRate(fps: Int) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        val fpsRange = selectBestFpsRange(cameraId ?: return, fps)
        request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        try { 
            session.setRepeatingRequest(request.build(), null, cameraHandler) 
            currentConfig = currentConfig.copy(targetFps = fps)
        } catch (e: Exception) {}
    }

    fun setExposureCompensation(value: Int) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        val range = getCameraCharacteristics()?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return
        request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value.coerceIn(range.lower, range.upper))
        try { session.setRepeatingRequest(request.build(), null, cameraHandler) } catch (e: Exception) {}
    }

    fun setFocusMode(auto: Boolean) {
        val session = captureSession ?: return
        val request = previewRequest ?: return
        request.set(CaptureRequest.CONTROL_AF_MODE, if (auto) CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE else CaptureRequest.CONTROL_AF_MODE_OFF)
        try { session.setRepeatingRequest(request.build(), null, cameraHandler) } catch (e: Exception) {}
    }

    suspend fun captureImage(): ByteArray? = suspendCancellableCoroutine { cont ->
        val reader = captureImageReader ?: run { cont.resume(null); return@suspendCancellableCoroutine }
        val session = captureSession ?: run { cont.resume(null); return@suspendCancellableCoroutine }
        
        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                if (cont.isActive) cont.resume(bytes)
            } finally {
                image.close()
            }
        }, cameraHandler)
        
        try {
            val request = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                addTarget(reader.surface)
                // 캡처용 플래시 설정 적용
                applyFlashToRequestBuilder(this, _flashMode.value, isPreview = false)
            }?.build()
            if (request != null) session.capture(request, null, cameraHandler)
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    private fun selectBestFpsRange(id: String, target: Int): Range<Int> {
        val ranges = cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
        return ranges.find { it.lower == target && it.upper == target } ?: ranges.filter { it.upper >= target }.minByOrNull { it.lower } ?: Range(30, 30)
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

    private fun findCamera(facing: Int): String? = cameraManager.cameraIdList.find { cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == facing }
    
    private suspend fun openCameraDevice(id: String): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) = cont.resume(c)
                override fun onDisconnected(c: CameraDevice) { c.close(); if(cont.isActive) cont.resumeWithException(IllegalStateException()) }
                override fun onError(c: CameraDevice, e: Int) { c.close(); if(cont.isActive) cont.resumeWithException(IllegalStateException()) }
            }, cameraHandler)
        } catch (e: Exception) { cont.resumeWithException(e) }
    }
    
    private suspend fun createCaptureSession(d: CameraDevice, s: List<Surface>): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
            override fun onConfigureFailed(s: CameraCaptureSession) = cont.resumeWithException(IllegalStateException())
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val configs = s.map { OutputConfiguration(it) }
                d.createCaptureSession(SessionConfiguration(SessionConfiguration.SESSION_REGULAR, configs, context.mainExecutor, callback))
            } else {
                @Suppress("DEPRECATION") d.createCaptureSession(s, callback, cameraHandler)
            }
        } catch (e: Exception) { cont.resumeWithException(e) }
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
