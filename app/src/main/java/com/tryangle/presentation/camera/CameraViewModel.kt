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
    
    fun getSensorOrientation(): Int {
        return cameraManager.getSensorOrientation()
    }
    
    fun getOptimalPreviewSize(width: Int, height: Int): Size {
        return cameraManager.getOptimalPreviewSize(width, height)
    }
    
    fun getMaxZoom(): Float {
        return cameraManager.getMaxZoom()
    }
    
    fun setZoom(ratio: Float) {
        cameraManager.setZoom(ratio)
    }
    
    fun setFrameRate(fps: Int) {
        cameraManager.setFrameRate(fps)
    }
    
    fun setExposure(value: Int) {
        cameraManager.setExposureCompensation(value)
    }
    
    fun setFocusMode(auto: Boolean) {
        cameraManager.setFocusMode(auto)
    }
    
    fun setFlashMode(mode: FlashMode) {
        cameraManager.setFlashMode(mode)
    }
    
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
     * Capture and save photo with correct orientation
     */
    fun savePhoto(context: android.content.Context, onSaved: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val imageData = cameraManager.captureImage()
                if (imageData != null) {
                    // 센서 방향을 가져와 저장 로직에 전달 (회전 문제 해결)
                    val rotation = cameraManager.getSensorOrientation()
                    val uri = com.tryangle.platform.storage.ImageSaver.saveImage(context, imageData, rotation)
                    onSaved(uri)
                } else {
                    onSaved(null)
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraViewModel", "Failed to save photo: ${e.message}")
                onSaved(null)
            }
        }
    }
}
