package com.tryangle.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Camera controls overlay
 */
@Composable
fun CameraControlsOverlay(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    var showZoomSlider by remember { mutableStateOf(false) }
    var currentFps by remember { mutableStateOf(60) }
    
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val maxZoom = remember { viewModel.getMaxZoom() }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 상단 정보 표시부
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zoom Badge
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = "${String.format("%.1f", zoomRatio)}x",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // FPS Badge
            Surface(
                color = if (currentFps == 60) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = "${currentFps} FPS",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        
        // 하단 제어 버튼부
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FPS 전환 버튼
            CameraControlIconButton(
                onClick = {
                    currentFps = if (currentFps == 60) 30 else 60
                    viewModel.setFrameRate(currentFps)
                },
                icon = Icons.Default.Refresh,
                label = "${currentFps}fps"
            )
            
            // 줌 패널 토글 버튼
            CameraControlIconButton(
                onClick = { showZoomSlider = !showZoomSlider },
                icon = if (showZoomSlider) Icons.Default.Close else Icons.Default.Search,
                label = "Zoom",
                isSelected = showZoomSlider
            )
        }
        
        // 우측 세로 줌 슬라이더
        if (showZoomSlider && maxZoom > 1.0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .width(60.dp)
                    .height(250.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = zoomRatio,
                    onValueChange = { viewModel.setZoom(it) },
                    valueRange = 1.0f..maxZoom,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = -90f
                        }
                        .width(200.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
fun CameraControlIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isSelected) Color.White else Color.Black.copy(alpha = 0.6f),
                contentColor = if (isSelected) Color.Black else Color.White
            )
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
