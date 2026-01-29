package com.tryangle.platform.camera

import android.graphics.Bitmap
import android.media.Image

/**
 * 카메라 상태를 나타내는 Sealed Class
 *
 * StateFlow로 관리되어 UI에서 반응형으로 상태 변화를 관찰할 수 있습니다.
 */
sealed class CameraState {
    /**
     * 카메라가 초기화되지 않은 상태
     */
    data object Idle : CameraState()

    /**
     * 카메라를 여는 중
     */
    data object Opening : CameraState()

    /**
     * 카메라가 열리고 프리뷰 준비 완료
     */
    data object Opened : CameraState()

    /**
     * 프리뷰 세션이 활성화되어 프레임을 전송 중
     */
    data object Streaming : CameraState()

    /**
     * 이미지 캡처 진행 중
     */
    data object Capturing : CameraState()

    /**
     * 카메라 에러 발생
     *
     * @param message 에러 메시지 (사용자에게 표시 가능)
     * @param exception 원본 예외 (디버깅용)
     */
    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : CameraState()

    /**
     * 카메라가 정상 동작 중인지 확인
     */
    fun isActive(): Boolean = this is Opened || this is Streaming || this is Capturing
}

/**
 * 카메라에서 전달되는 프레임 데이터
 *
 * iOS의 CMSampleBuffer와 유사한 역할을 수행합니다.
 */
sealed class CameraFrame {
    /**
     * 프리뷰 프레임 (YUV 형식)
     *
     * @param image Camera2 API에서 제공하는 YUV_420_888 이미지
     * @param timestamp 프레임 타임스탬프 (나노초)
     * @param rotation 디바이스 회전 각도 (0, 90, 180, 270)
     */
    data class Preview(
        val image: Image,
        val timestamp: Long,
        val rotation: Int = 0
    ) : CameraFrame()

    /**
     * 캡처된 이미지 (RGB 형식)
     *
     * @param bitmap 변환된 Bitmap 이미지
     * @param timestamp 캡처 시각 (나노초)
     */
    data class Captured(
        val bitmap: Bitmap,
        val timestamp: Long
    ) : CameraFrame()
}

/**
 * 카메라 에러 타입
 */
enum class CameraErrorType {
    /**
     * 카메라 권한이 없음
     */
    PERMISSION_DENIED,

    /**
     * 카메라 디바이스를 찾을 수 없음
     */
    DEVICE_NOT_FOUND,

    /**
     * 카메라 디바이스를 열 수 없음
     */
    DEVICE_OPEN_FAILED,

    /**
     * 카메라 세션 생성 실패
     */
    SESSION_CREATION_FAILED,

    /**
     * 지원하지 않는 설정
     */
    UNSUPPORTED_CONFIG,

    /**
     * 알 수 없는 에러
     */
    UNKNOWN
}

/**
 * 카메라 에러 정보
 */
data class CameraError(
    val type: CameraErrorType,
    val message: String,
    val exception: Exception? = null
) {
    companion object {
        fun fromException(exception: Exception): CameraError {
            return when (exception) {
                is SecurityException -> CameraError(
                    type = CameraErrorType.PERMISSION_DENIED,
                    message = "카메라 권한이 필요합니다",
                    exception = exception
                )
                else -> CameraError(
                    type = CameraErrorType.UNKNOWN,
                    message = exception.message ?: "알 수 없는 에러 발생",
                    exception = exception
                )
            }
        }
    }
}
