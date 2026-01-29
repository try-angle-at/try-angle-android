package com.tryangle.presentation.camera

import android.app.Activity
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.tryangle.platform.camera.CameraState
import com.tryangle.platform.permission.PermissionState
import com.tryangle.platform.permission.rememberCameraPermissionState
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraState by viewModel.cameraState.collectAsState()
    val (permissionState, launcher) = rememberCameraPermissionState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(permissionState) {
        if (permissionState is PermissionState.Granted) {
            viewModel.onResume()
        } else if (permissionState is PermissionState.Denied || permissionState is PermissionState.Checking) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onPause()
        }
    }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        when (permissionState) {
            PermissionState.Granted -> {
                if (cameraState is CameraState.Opened || cameraState is CameraState.Streaming) {
                    CameraTexturePreview(
                        viewModel = viewModel,
                        onSurfaceReady = { surface ->
                            scope.launch {
                                viewModel.startPreview(surface)
                            }
                        }
                    )
                } else if (cameraState is CameraState.Error) {
                    Text(
                        text = (cameraState as CameraState.Error).message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            else -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CameraTexturePreview(
    viewModel: CameraViewModel,
    onSurfaceReady: (Surface) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        // 1. 최적 프리뷰 크기 가져오기
                        val previewSize = viewModel.getOptimalPreviewSize(width, height)
                        
                        // 2. SurfaceTexture 크기 설정
                        st.setDefaultBufferSize(previewSize.width, previewSize.height)
                        
                        // 3. 변환 매트릭스 적용
                        activity?.let { act ->
                            configureTransform(
                                view = this@apply,
                                viewWidth = width,
                                viewHeight = height,
                                previewWidth = previewSize.width,
                                previewHeight = previewSize.height,
                                sensorOrientation = viewModel.getSensorOrientation(),
                                displayRotation = act.windowManager.defaultDisplay.rotation
                            )
                        }
                        
                        // 4. 프리뷰 시작
                        onSurfaceReady(Surface(st))
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        val previewSize = viewModel.getOptimalPreviewSize(width, height)
                        
                        activity?.let { act ->
                            configureTransform(
                                view = this@apply,
                                viewWidth = width,
                                viewHeight = height,
                                previewWidth = previewSize.width,
                                previewHeight = previewSize.height,
                                sensorOrientation = viewModel.getSensorOrientation(),
                                displayRotation = act.windowManager.defaultDisplay.rotation
                            )
                        }
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 카메라 프리뷰 변환 매트릭스 설정
 * 
 * 디버그: 로그를 찍어서 실제 값 확인
 */
private fun configureTransform(
    view: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    sensorOrientation: Int,
    displayRotation: Int
) {
    android.util.Log.d("CameraTransform", "===== Camera Transform Debug =====")
    android.util.Log.d("CameraTransform", "View Size: ${viewWidth}x${viewHeight}")
    android.util.Log.d("CameraTransform", "Preview Size: ${previewWidth}x${previewHeight}")
    android.util.Log.d("CameraTransform", "Sensor Orientation: $sensorOrientation")
    android.util.Log.d("CameraTransform", "Display Rotation: $displayRotation")
    
    val matrix = Matrix()
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f
    
    // 테스트: 0도 회전 (회전 없음)
    val rotation = 0f
    
    android.util.Log.d("CameraTransform", "Applying Rotation: ${rotation}도")
    
    if (rotation != 0f) {
        matrix.postRotate(rotation, centerX, centerY)
    }
    
    // 회전 후 크기
    val rotatedWidth = if (rotation % 180f != 0f) previewHeight else previewWidth
    val rotatedHeight = if (rotation % 180f != 0f) previewWidth else previewHeight
    
    android.util.Log.d("CameraTransform", "Rotated Size: ${rotatedWidth}x${rotatedHeight}")
    
    // 화면에 맞게 스케일
    val scaleX = viewWidth.toFloat() / rotatedWidth
    val scaleY = viewHeight.toFloat() / rotatedHeight
    val scale = maxOf(scaleX, scaleY)
    
    android.util.Log.d("CameraTransform", "Scale: $scale (scaleX=$scaleX, scaleY=$scaleY)")
    
    matrix.postScale(scale, scale, centerX, centerY)
    
    view.setTransform(matrix)
    android.util.Log.d("CameraTransform", "Transform applied!")
}
