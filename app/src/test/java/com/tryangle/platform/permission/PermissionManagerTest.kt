package com.tryangle.platform.permission

import org.junit.Assert.*
import org.junit.Test

/**
 * PermissionManager 단위 테스트
 *
 * 권한 상태 관리 로직을 검증합니다.
 */
class PermissionManagerTest {

    @Test
    fun `test permission state is granted check`() {
        assertTrue(PermissionState.Granted.isGranted())
        assertFalse(PermissionState.Denied.isGranted())
        assertFalse(PermissionState.PermanentlyDenied.isGranted())
        assertFalse(PermissionState.Checking.isGranted())
    }
    
    @Test
    fun `test permission state needs request check`() {
        assertTrue(PermissionState.Denied.needsRequest())
        assertTrue(PermissionState.Checking.needsRequest())
        
        assertFalse(PermissionState.Granted.needsRequest())
        assertFalse(PermissionState.PermanentlyDenied.needsRequest())
    }
    
    @Test
    fun `test permission state needs settings check`() {
        assertTrue(PermissionState.PermanentlyDenied.needsSettings())
        
        assertFalse(PermissionState.Granted.needsSettings())
        assertFalse(PermissionState.Denied.needsSettings())
        assertFalse(PermissionState.Checking.needsSettings())
    }
    
    @Test
    fun `test permission enum manifest values`() {
        assertEquals(
            android.Manifest.permission.CAMERA,
            Permission.CAMERA.manifestPermission
        )
        
        assertEquals(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Permission.WRITE_EXTERNAL_STORAGE.manifestPermission
        )
        
        assertEquals(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            Permission.READ_MEDIA_IMAGES.manifestPermission
        )
    }
    
    @Test
    fun `test permission from string`() {
        val cameraPermission = Permission.from(android.Manifest.permission.CAMERA)
        assertEquals(Permission.CAMERA, cameraPermission)
        
        val storagePermission = Permission.from(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertEquals(Permission.WRITE_EXTERNAL_STORAGE, storagePermission)
        
        val unknownPermission = Permission.from("unknown.permission")
        assertNull(unknownPermission)
    }
    
    @Test
    fun `test permission result types`() {
        val granted: PermissionResult = PermissionResult.Granted
        assertTrue(granted is PermissionResult.Granted)
        
        val denied: PermissionResult = PermissionResult.Denied(shouldShowRationale = true)
        assertTrue(denied is PermissionResult.Denied)
        
        if (denied is PermissionResult.Denied) {
            assertTrue(denied.shouldShowRationale)
        }
    }
    
    // Note: Context가 필요한 실제 권한 체크는 Instrumented Test에서 수행해야 합니다.
    // 여기서는 데이터 클래스와 상태 로직만 테스트합니다.
}
