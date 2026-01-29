package com.tryangle.platform.permission

/**
 * 권한 상태를 나타내는 Sealed Class
 *
 * Android의 런타임 권한 시스템 상태를 추상화합니다.
 */
sealed class PermissionState {
    /**
     * 권한이 부여됨
     */
    data object Granted : PermissionState()

    /**
     * 권한이 거부됨 (재요청 가능)
     * 사용자에게 권한 필요성을 설명하는 Rationale을 표시할 수 있습니다.
     */
    data object Denied : PermissionState()

    /**
     * 권한이 영구적으로 거부됨
     * "다시 묻지 않기"를 선택한 경우 또는 여러 번 거부한 경우
     * 이 경우 앱 설정 화면으로 이동하도록 안내해야 합니다.
     */
    data object PermanentlyDenied : PermissionState()

    /**
     * 권한 상태 확인 중 (초기 상태)
     */
    data object Checking : PermissionState()

    /**
     * 권한이 부여되었는지 확인
     */
    fun isGranted(): Boolean = this is Granted

    /**
     * 권한 요청이 필요한지 확인
     */
    fun needsRequest(): Boolean = this is Denied || this is Checking

    /**
     * 설정 화면으로 이동이 필요한지 확인
     */
    fun needsSettings(): Boolean = this is PermanentlyDenied
}

/**
 * 권한 요청 결과
 */
sealed class PermissionResult {
    /**
     * 권한 부여됨
     */
    data object Granted : PermissionResult()

    /**
     * 권한 거부됨
     *
     * @param shouldShowRationale Rationale을 표시해야 하는지 여부
     */
    data class Denied(
        val shouldShowRationale: Boolean
    ) : PermissionResult()
}

/**
 * 필요한 권한 목록
 */
enum class Permission(val manifestPermission: String) {
    /**
     * 카메라 권한
     */
    CAMERA(android.Manifest.permission.CAMERA),

    /**
     * 외부 저장소 쓰기 (Android 9 이하)
     */
    WRITE_EXTERNAL_STORAGE(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),

    /**
     * 미디어 이미지 읽기 (Android 13+)
     */
    READ_MEDIA_IMAGES(android.Manifest.permission.READ_MEDIA_IMAGES);

    companion object {
        /**
         * 문자열에서 Permission enum 찾기
         */
        fun from(permission: String): Permission? {
            return entries.find { it.manifestPermission == permission }
        }
    }
}
