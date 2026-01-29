package com.tryangle.platform.camera

import com.tryangle.platform.camera.CameraConfig
import com.tryangle.platform.camera.CameraState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * CameraManager 단위 테스트
 *
 * Camera2 API의 핵심 동작을 검증합니다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraManagerTest {

    @Test
    fun `test camera config default values`() {
        val config = CameraConfig.DEFAULT
        
        assertEquals(60, config.targetFps)
        assertEquals(1920, config.previewWidth)
        assertEquals(1080, config.previewHeight)
        assertEquals(16f / 9f, config.previewAspectRatio, 0.01f)
    }
    
    @Test
    fun `test camera config aspect ratio calculation`() {
        val config = CameraConfig(
            previewWidth = 1280,
            previewHeight = 720
        )
        
        assertEquals(16f / 9f, config.previewAspectRatio, 0.01f)
    }
    
    @Test
    fun `test camera config low resolution preset`() {
        val config = CameraConfig.LOW_RESOLUTION
        
        assertEquals(30, config.targetFps)
        assertEquals(1280, config.previewWidth)
        assertEquals(720, config.previewHeight)
    }
    
    @Test
    fun `test camera state transitions`() {
        // 초기 상태
        val idleState: CameraState = CameraState.Idle
        assertFalse(idleState.isActive())
        
        // 열림 상태
        val openedState: CameraState = CameraState.Opened
        assertTrue(openedState.isActive())
        
        // 스트리밍 상태
        val streamingState: CameraState = CameraState.Streaming
        assertTrue(streamingState.isActive())
        
        // 에러 상태
        val errorState: CameraState = CameraState.Error("Test error")
        assertFalse(errorState.isActive())
    }
    
    @Test
    fun `test camera state is active check`() {
        assertTrue(CameraState.Opened.isActive())
        assertTrue(CameraState.Streaming.isActive())
        assertTrue(CameraState.Capturing.isActive())
        
        assertFalse(CameraState.Idle.isActive())
        assertFalse(CameraState.Opening.isActive())
        assertFalse(CameraState.Error("test").isActive())
    }
    
    @Test
    fun `test camera error from exception`() {
        val securityException = SecurityException("No camera permission")
        val error = CameraError.fromException(securityException)
        
        assertEquals(CameraErrorType.PERMISSION_DENIED, error.type)
        assertEquals("카메라 권한이 필요합니다", error.message)
        assertNotNull(error.exception)
    }
    
    @Test
    fun `test camera error unknown type`() {
        val genericException = RuntimeException("Unknown error")
        val error = CameraError.fromException(genericException)
        
        assertEquals(CameraErrorType.UNKNOWN, error.type)
        assertEquals("Unknown error", error.message)
    }
    
    // Note: 실제 Camera2 API 테스트는 안드로이드 디바이스가 필요하므로
    // Instrumented Test에서 수행해야 합니다.
    // 여기서는 데이터 클래스와 상태 관리 로직만 테스트합니다.
}
