package com.tryangle.platform.permission

import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Compose용 카메라 권한 관리 유틸리티
 */

/**
 * 카메라 권한 요청 Launcher 생성
 */
@Composable
fun rememberCameraPermissionLauncher(
    onResult: (PermissionResult) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val result = if (isGranted) {
            PermissionResult.Granted
        } else {
            val activity = context as? ComponentActivity
            val shouldShowRationale = activity?.let {
                permissionManager.shouldShowRationale(it, Permission.CAMERA)
            } ?: false
            
            PermissionResult.Denied(shouldShowRationale)
        }
        
        onResult(result)
    }
}

/**
 * 카메라 권한 상태를 관찰하는 Composable
 */
@Composable
fun CameraPermissionEffect(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    onPermanentlyDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.Checking) }
    
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        permissionState = if (activity != null) {
            permissionManager.isPermanentlyDenied(activity, Permission.CAMERA)
        } else {
            permissionManager.checkCameraPermission()
        }
        
        when (permissionState) {
            is PermissionState.Granted -> onPermissionGranted()
            is PermissionState.Denied -> onPermissionDenied()
            is PermissionState.PermanentlyDenied -> onPermanentlyDenied()
            else -> {}
        }
    }
}

/**
 * 카메라 권한 상태와 요청 Launcher를 함께 제공하는 Composable
 */
@Composable
fun rememberCameraPermissionState(): Pair<PermissionState, ManagedActivityResultLauncher<String, Boolean>> {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.Checking) }
    
    // 초기 권한 상태 확인
    LaunchedEffect(Unit) {
        permissionState = permissionManager.checkCameraPermission()
    }
    
    // 권한 요청 Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState = if (isGranted) {
            PermissionState.Granted
        } else {
            val activity = context as? ComponentActivity
            if (activity != null && !permissionManager.shouldShowRationale(activity, Permission.CAMERA)) {
                PermissionState.PermanentlyDenied
            } else {
                PermissionState.Denied
            }
        }
    }
    
    return permissionState to launcher
}

/**
 * 앱 설정 화면으로 이동하는 유틸리티
 */
@Composable
fun rememberOpenAppSettings(): () -> Unit {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    
    return remember {
        {
            val activity = context as? ComponentActivity
            activity?.let { permissionManager.openAppSettings(it) }
        }
    }
}
