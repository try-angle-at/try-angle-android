package com.tryangle.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tryangle.platform.camera.FlashMode

/**
 * Galaxy-style camera UI overlay
 */
@Composable
fun CameraControlsOverlay(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val flashMode by viewModel.flashMode.collectAsState()
    var currentZoomLevel by remember { mutableStateOf(1.0f) }
    var selectedMode by remember { mutableStateOf(CameraMode.PHOTO) }
    
    val maxZoom = remember { viewModel.getMaxZoom() }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Top Bar
        TopBar(
            flashMode = flashMode,
            onFlashToggle = { 
                val newMode = when (flashMode) {
                    FlashMode.OFF -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.OFF
                }
                viewModel.setFlashMode(newMode)
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Level Selection
            ZoomLevelBar(
                currentZoom = currentZoomLevel,
                maxZoom = maxZoom,
                onZoomSelected = { zoom ->
                    currentZoomLevel = zoom
                    viewModel.setZoom(zoom)
                },
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Main Controls (Grid, Shutter, Camera Switch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grid toggle
                IconButton(
                    onClick = { /* TODO: Toggle grid */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Grid",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Shutter Button
                ShutterButton(
                    onClick = {
                        viewModel.capturePhoto { imageData ->
                            // TODO: Save image
                            android.util.Log.d("CameraUI", "Captured ${imageData.size} bytes")
                        }
                    },
                    modifier = Modifier.size(72.dp)
                )
                
                // Camera switch
                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Bottom Mode Navigation
            ModeNavigationBar(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun TopBar(
    flashMode: FlashMode,
    onFlashToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash toggle
            IconButton(onClick = onFlashToggle) {
                Icon(
                    imageVector = when (flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                    },
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
            
            // Resolution indicator
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "12M",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            // Aspect ratio
            IconButton(onClick = { /* TODO: Change aspect ratio */ }) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Aspect Ratio",
                    tint = Color.White
                )
            }
        }
        
        // Settings
        IconButton(onClick = { /* TODO: Open settings */ }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ZoomLevelBar(
    currentZoom: Float,
    maxZoom: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomLevels = listOf(0.6f, 1.0f, 2.0f, 3.0f).filter { it <= maxZoom }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        zoomLevels.forEach { zoom ->
            ZoomLevelButton(
                zoom = zoom,
                isSelected = kotlin.math.abs(currentZoom - zoom) < 0.1f,
                onClick = { onZoomSelected(zoom) }
            )
        }
    }
}

@Composable
fun ZoomLevelButton(
    zoom: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when {
        zoom < 1.0f -> ".${(zoom * 10).toInt()}"
        zoom == 1.0f -> "1×"
        else -> ".${zoom.toInt()}"
    }
    
    Text(
        text = label,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    )
}

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun ModeNavigationBar(
    selectedMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CameraMode.values().forEach { mode ->
            ModeTab(
                mode = mode,
                isSelected = selectedMode == mode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
fun ModeTab(
    mode: CameraMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = mode.label,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    )
}

enum class CameraMode(val label: String) {
    PORTRAIT("인물 사진"),
    PHOTO("사진"),
    VIDEO("동영상"),
    MORE("더보기")
}
