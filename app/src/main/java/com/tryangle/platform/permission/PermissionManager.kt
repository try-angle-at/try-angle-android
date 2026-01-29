package com.tryangle.platform.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 권한 관리 클래스
 *
 * Android 런타임 권한 시스템을 추상화하여 관리합니다.
 * Hilt를 통해 싱글톤으로 주입됩니다.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 카메라 권한 상태 확인
     *
     * @return 현재 권한 상태
     */
    fun checkCameraPermission(): PermissionState {
        return checkPermission(Permission.CAMERA)
    }

    /**
     * 특정 권한 상태 확인
     *
     * @param permission 확인할 권한
     * @return 현재 권한 상태
     */
    fun checkPermission(permission: Permission): PermissionState {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED

        return if (isGranted) {
            PermissionState.Granted
        } else {
            // 거부 상태이지만 영구 거부인지는 Activity context가 필요하므로
            // 일단 Denied로 반환 (Activity에서 shouldShowRationale 확인 필요)
            PermissionState.Denied
        }
    }

    /**
     * 모든 카메라 관련 권한 확인
     *
     * @return 모든 권한이 부여되었는지 여부
     */
    fun checkAllCameraPermissions(): Boolean {
        val cameraGranted = checkPermission(Permission.CAMERA).isGranted()

        // Android 13+ (API 33)에서는 READ_MEDIA_IMAGES 권한도 필요
        val mediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(Permission.READ_MEDIA_IMAGES).isGranted()
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 (API 28) 이하에서는 WRITE_EXTERNAL_STORAGE 필요
            checkPermission(Permission.WRITE_EXTERNAL_STORAGE).isGranted()
        } else {
            // Android 10-12는 추가 권한 불필요
            true
        }

        return cameraGranted && mediaGranted
    }

    /**
     * Rationale을 표시해야 하는지 확인
     *
     * 사용자가 권한을 거부했지만 "다시 묻지 않기"를 선택하지 않은 경우 true 반환
     *
     * @param activity Activity 컨텍스트
     * @param permission 확인할 권한
     * @return Rationale 표시 필요 여부
     */
    fun shouldShowRationale(activity: Activity, permission: Permission): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission.manifestPermission
        )
    }

    /**
     * 권한이 영구적으로 거부되었는지 확인
     *
     * @param activity Activity 컨텍스트
     * @param permission 확인할 권한
     * @return 영구 거부 여부
     */
    fun isPermanentlyDenied(activity: Activity, permission: Permission): PermissionState {
        val isGranted = checkPermission(permission).isGranted()
        
        return when {
            isGranted -> PermissionState.Granted
            shouldShowRationale(activity, permission) -> PermissionState.Denied
            else -> PermissionState.PermanentlyDenied
        }
    }

    /**
     * 앱 설정 화면으로 이동
     *
     * 권한이 영구 거부된 경우 사용자를 설정 화면으로 안내합니다.
     *
     * @param activity Activity 컨텍스트
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }

    /**
     * 필요한 카메라 권한 목록 반환
     *
     * Android 버전에 따라 다른 권한 목록을 반환합니다.
     *
     * @return 필요한 권한 문자열 배열
     */
    fun getRequiredCameraPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+
                arrayOf(
                    Permission.CAMERA.manifestPermission,
                    Permission.READ_MEDIA_IMAGES.manifestPermission
                )
            }
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
                // Android 9 이하
                arrayOf(
                    Permission.CAMERA.manifestPermission,
                    Permission.WRITE_EXTERNAL_STORAGE.manifestPermission
                )
            }
            else -> {
                // Android 10-12
                arrayOf(Permission.CAMERA.manifestPermission)
            }
        }
    }

    companion object {
        /**
         * 권한 요청 코드
         */
        const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
