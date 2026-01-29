package com.tryangle.presentation.camera

import android.app.Activity
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
    ) {
        when (permissionState) {
            PermissionState.Granted -> {
                if (cameraState is CameraState.Opened || cameraState is CameraState.Streaming) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraTexturePreview(
                            viewModel = viewModel,
                            onSurfaceReady = { surface ->
                                scope.launch {
                                    viewModel.startPreview(surface)
                                }
                            }
                        )
                        CameraControlsOverlay(viewModel = viewModel)
                    }
                } else if (cameraState is CameraState.Error) {
                    Text(
                        text = (cameraState as CameraState.Error).message,
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
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
    
    var currentZoom by remember { mutableStateOf(1.0f) }
    val maxZoom = remember { viewModel.getMaxZoom() }
    
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        val previewSize = viewModel.getOptimalPreviewSize(width, height)
                        st.setDefaultBufferSize(previewSize.width, previewSize.height)
                        
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
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    currentZoom = (currentZoom * zoom).coerceIn(1.0f, maxZoom)
                    viewModel.setZoom(currentZoom)
                }
            }
    )
}

private fun configureTransform(
    view: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    sensorOrientation: Int,
    displayRotation: Int
) {
    val matrix = Matrix()
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f
    
    // 회전 없음 - 카메라가 이미 올바른 방향으로 설정됨
    // 단순히 화면을 꽉 채우도록 스케일만 조정
    val scaleX = viewWidth.toFloat() / previewWidth
    val scaleY = viewHeight.toFloat() / previewHeight
    val scale = maxOf(scaleX, scaleY)  // 화면을 꽉 채움
    
    matrix.postScale(scale, scale, centerX, centerY)
    
    view.setTransform(matrix)
}
