package com.tryangle.platform.camera

import android.hardware.camera2.CameraCharacteristics

/**
 * 카메라 설정을 위한 데이터 클래스
 *
 * iOS AVFoundation의 AVCaptureDeviceFormat과 유사한 역할을 수행하며,
 * Camera2 API에서 사용할 카메라 설정 값들을 정의합니다.
 */
data class CameraConfig(
    /**
     * 타겟 프레임레이트 (기본값: 60fps)
     * iOS의 실시간 자세 분석과 동일한 성능을 제공하기 위해 60fps 고정
     */
    val targetFps: Int = 60,

    /**
     * 카메라 렌즈 방향
     * - LENS_FACING_FRONT: 전면 카메라 (셀카)
     * - LENS_FACING_BACK: 후면 카메라
     */
    val lensFacing: Int = CameraCharacteristics.LENS_FACING_FRONT,

    /**
     * 프리뷰 해상도 (가로)
     * 16:9 비율로 1920x1080 설정
     */
    val previewWidth: Int = 1920,

    /**
     * 프리뷰 해상도 (세로)
     */
    val previewHeight: Int = 1080,

    /**
     * 캡처 이미지 해상도 (가로)
     * 프리뷰와 동일한 해상도 사용
     */
    val captureWidth: Int = 1920,

    /**
     * 캡처 이미지 해상도 (세로)
     */
    val captureHeight: Int = 1080
) {
    /**
     * 프리뷰 화면 비율 계산
     */
    val previewAspectRatio: Float
        get() = previewWidth.toFloat() / previewHeight.toFloat()

    /**
     * 캡처 이미지 화면 비율 계산
     */
    val captureAspectRatio: Float
        get() = captureWidth.toFloat() / captureHeight.toFloat()

    companion object {
        /**
         * 기본 설정 (60fps, 전면 카메라, 1920x1080)
         */
        val DEFAULT = CameraConfig()

        /**
         * 후면 카메라 설정
         */
        val BACK_CAMERA = CameraConfig(
            lensFacing = CameraCharacteristics.LENS_FACING_BACK
        )

        /**
         * 저해상도 설정 (30fps, 1280x720)
         * 성능이 낮은 디바이스용
         */
        val LOW_RESOLUTION = CameraConfig(
            targetFps = 30,
            previewWidth = 1280,
            previewHeight = 720,
            captureWidth = 1280,
            captureHeight = 720
        )
    }
}
