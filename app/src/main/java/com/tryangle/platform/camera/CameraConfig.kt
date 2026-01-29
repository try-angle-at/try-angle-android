package com.tryangle.platform.camera

import android.hardware.camera2.CameraCharacteristics

/**
 * 카메라 설정을 위한 데이터 클래스
 */
data class CameraConfig(
    val targetFps: Int = 60,
    val lensFacing: Int = CameraCharacteristics.LENS_FACING_BACK, // 기본 후면 카메라
    val previewWidth: Int = 3840,  // 4K UHD
    val previewHeight: Int = 2160,
    val captureWidth: Int = 3840,
    val captureHeight: Int = 2160
) {
    val previewAspectRatio: Float
        get() = previewWidth.toFloat() / previewHeight.toFloat()

    companion object {
        /**
         * 4K 60fps 기본 설정 (S25 최적화)
         */
        val DEFAULT = CameraConfig()

        /**
         * 후면 카메라 (기본과 동일)
         */
        val BACK_CAMERA = DEFAULT

        /**
         * 전면 카메라 설정
         */
        val FRONT_CAMERA = CameraConfig(
            lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
            previewWidth = 1920, // 전면은 1080p 권장
            previewHeight = 1080
        )

        /**
         * 1080p 설정
         */
        val FULL_HD = CameraConfig(
            previewWidth = 1920,
            previewHeight = 1080,
            captureWidth = 1920,
            captureHeight = 1080
        )
    }
}
