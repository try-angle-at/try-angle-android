package com.tryangle.presentation.camera

import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tryangle.platform.camera.CameraConfig
import com.tryangle.platform.camera.CameraManager
import com.tryangle.platform.camera.CameraState
import com.tryangle.platform.camera.FlashMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager
) : ViewModel() {

    val cameraState: StateFlow<CameraState> = cameraManager.state
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val flashMode: StateFlow<FlashMode> = cameraManager.flashMode

    fun onResume() {
        viewModelScope.launch {
            try {
                // 후면 카메라 사용
                cameraManager.openCamera(CameraConfig.BACK_CAMERA)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onPause() {
        cameraManager.closeCamera()
    }

    suspend fun startPreview(surface: android.view.Surface) {
        cameraManager.startPreview(surface)
    }
    
    /**
     * 센서 방향 가져오기
     */
    fun getSensorOrientation(): Int {
        return cameraManager.getSensorOrientation()
    }
    
    /**
     * 최적 프리뷰 크기 가져오기
     */
    fun getOptimalPreviewSize(width: Int, height: Int): Size {
        return cameraManager.getOptimalPreviewSize(width, height)
    }
    
    /**
     * Get maximum zoom ratio
     */
    fun getMaxZoom(): Float {
        return cameraManager.getMaxZoom()
    }
    
    /**
     * Set zoom ratio
     */
    fun setZoom(ratio: Float) {
        cameraManager.setZoom(ratio)
    }
    
    /**
     * Set frame rate
     */
    fun setFrameRate(fps: Int) {
        cameraManager.setFrameRate(fps)
    }
    
    /**
     * Set exposure compensation
     */
    fun setExposure(value: Int) {
        cameraManager.setExposureCompensation(value)
    }
    
    /**
     * Set focus mode
     */
    fun setFocusMode(auto: Boolean) {
        cameraManager.setFocusMode(auto)
    }
    
    /**
     * Toggle flash mode
     */
    fun setFlashMode(mode: FlashMode) {
        cameraManager.setFlashMode(mode)
    }
    
    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        viewModelScope.launch {
            try {
                cameraManager.switchCamera()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Capture photo
     */
    fun capturePhoto(onCaptured: (ByteArray) -> Unit) {
        viewModelScope.launch {
            try {
                val imageData = cameraManager.captureImage()
                // 널 체크를 통해 타입 안전성 확보
                if (imageData != null) {
                    onCaptured(imageData)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
