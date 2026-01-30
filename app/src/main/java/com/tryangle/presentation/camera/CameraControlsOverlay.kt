package com.tryangle.presentation.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tryangle.platform.camera.FlashMode

/**
 * Galaxy-style camera UI overlay - 최종 래퍼런스 디자인 적용 (가이드 문구 제거 버전)
 */
@Composable
fun CameraControlsOverlay(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val flashMode by viewModel.flashMode.collectAsState()
    var currentZoomLevel by remember { mutableStateOf(1.0f) }
    var selectedMode by remember { mutableStateOf(CameraMode.CAMERA) }
    val context = LocalContext.current
    
    val maxZoom = remember { viewModel.getMaxZoom() }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 1. 최상단 상태 칩 (비율, 샷타입, 위치 등)
        StatusHeader(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp))

        // 2. 상단 제어 바 (플래시, 설정 등)
        TopControlBar(
            flashMode = flashMode,
            onFlashToggle = { 
                val newMode = when (flashMode) {
                    FlashMode.OFF -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.OFF
                }
                viewModel.setFlashMode(newMode)
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        )
        
        // 하단 영역
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3. 줌 선택 바 (.6, 1.0x, 2, 3)
            ZoomLevelBar(
                currentZoom = currentZoomLevel,
                maxZoom = maxZoom,
                onZoomSelected = { zoom ->
                    currentZoomLevel = zoom
                    viewModel.setZoom(zoom)
                },
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // 4. 메인 액션 버튼 (갤러리, 셔터, 전환)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 갤러리 썸네일
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray)
                ) {
                    // TODO: 최근 이미지 표시
                }
                
                // 셔터 버튼
                ShutterButton(
                    onClick = {
                        viewModel.savePhoto(context) { uri ->
                            // TODO: Show captured image or toast
                        }
                    },
                    modifier = Modifier.size(80.dp)
                )
                
                // 카메라 전환
                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // 5. 하단 모드 네비게이션
            ModeNavigationBar(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun StatusHeader(modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusItem(label = "비율", isActive = true)
            StatusItem(label = "샷타입", isActive = false)
            StatusItem(label = "위치", isActive = false)
            StatusItem(label = "줌", isActive = false)
            StatusItem(label = "포즈", isActive = false)
        }
    }
}

@Composable
fun StatusItem(label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFF4CAF50) else Color.Transparent)
                .then(if (!isActive) Modifier.background(Color.White.copy(alpha = 0.5f)) else Modifier)
        ) {
            if (isActive) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(10.dp).align(Alignment.Center))
        }
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun TopControlBar(flashMode: FlashMode, onFlashToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onFlashToggle) {
            Icon(
                imageVector = when (flashMode) {
                    FlashMode.OFF -> Icons.Default.FlashOff
                    FlashMode.ON -> Icons.Default.FlashOn
                    FlashMode.AUTO -> Icons.Default.FlashAuto
                },
                contentDescription = null, tint = Color.White
            )
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = Color.White) }
    }
}

@Composable
fun ZoomLevelBar(currentZoom: Float, maxZoom: Float, onZoomSelected: (Float) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(0.6f, 1.0f, 2.0f, 3.0f).filter { it <= maxZoom }.forEach { zoom ->
                val isSelected = kotlin.math.abs(currentZoom - zoom) < 0.1f
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.Yellow.copy(alpha = 0.8f) else Color.Transparent)
                        .clickable { onZoomSelected(zoom) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (zoom == 1.0f) "1.0x" else if (zoom < 1f) ".6" else zoom.toInt().toString(),
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.1f))
        ) {}
    }
}

@Composable
fun ModeNavigationBar(selectedMode: CameraMode, onModeSelected: (CameraMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CameraMode.values().forEach { mode ->
            Text(
                text = mode.label,
                color = if (selectedMode == mode) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable { onModeSelected(mode) }.padding(12.dp)
            )
        }
    }
}

enum class CameraMode(val label: String) {
    GALLERY("갤러리"),
    CAMERA("카메라"),
    REFERENCE("레퍼런스")
}
