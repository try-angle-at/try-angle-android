package com.tryangle.platform.camera

import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import kotlin.math.abs

/**
 * 카메라 프리뷰 크기 계산 유틸리티
 *
 * 카메라 센서 출력과 화면 크기에 맞는 최적의 프리뷰 크기를 선택합니다.
 * 왜곡 없는 프리뷰를 위해 aspect ratio를 맞춥니다.
 */
object PreviewSize {

    /**
     * 최적의 프리뷰 크기 선택
     *
     * @param characteristics 카메라 특성
     * @param surfaceWidth Surface 가로 크기
     * @param surfaceHeight Surface 세로 크기
     * @param maxWidth 최대 가로 크기 (기본 1920)
     * @param maxHeight 최대 세로 크기 (기본 1080)
     * @return 최적의 프리뷰 Size
     */
    fun chooseOptimalSize(
        characteristics: CameraCharacteristics,
        surfaceWidth: Int,
        surfaceHeight: Int,
        maxWidth: Int = 3840,  // 4K 지원
        maxHeight: Int = 2160
    ): Size {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return Size(surfaceWidth, surfaceHeight)

        val supportedSizes = map.getOutputSizes(android.graphics.SurfaceTexture::class.java)
        
        // 센서 방향 확인
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val swappedDimensions = sensorOrientation == 90 || sensorOrientation == 270

        // 회전 고려한 실제 화면 크기
        val rotatedWidth = if (swappedDimensions) surfaceHeight else surfaceWidth
        val rotatedHeight = if (swappedDimensions) surfaceWidth else surfaceHeight

        // Aspect ratio (화면 비율)
        val targetRatio = rotatedWidth.toFloat() / rotatedHeight.toFloat()

        // 화면을 꽉 채울 수 있는 크기 중에서 선택
        // 1. aspect ratio가 비슷한 것
        // 2. 해상도가 높은 것 우선
        val optimalSizes = supportedSizes
            .filter { size ->
                size.width <= maxWidth && size.height <= maxHeight &&
                // Float 비교로 aspect ratio 체크 (5% 오차 허용 - 더 관대하게)
                abs(size.width.toFloat() / size.height.toFloat() - targetRatio) < 0.05f
            }
            .sortedByDescending { it.width * it.height }  // 큰 해상도 우선

        return if (optimalSizes.isNotEmpty()) {
            // 가장 큰 크기 선택 (화면을 꽉 채우기 위해)
            optimalSizes.first()
        } else {
            // 비율이 정확히 맞지 않아도 가장 비슷한 크기 선택
            supportedSizes
                .filter { it.width <= maxWidth && it.height <= maxHeight }
                .minByOrNull { size ->
                    val ratio = size.width.toFloat() / size.height.toFloat()
                    abs(ratio - targetRatio)
                } ?: Size(surfaceWidth, surfaceHeight)
        }
    }

    /**
     * Surface 크기를 카메라 프리뷰에 맞게 조정
     *
     * @param previewSize 카메라 프리뷰 크기
     * @param viewWidth View 가로 크기
     * @param viewHeight View 세로 크기
     * @param sensorOrientation 센서 방향 (0, 90, 180, 270)
     * @return 조정된 크기
     */
    fun calculateSurfaceSize(
        previewSize: Size,
        viewWidth: Int,
        viewHeight: Int,
        sensorOrientation: Int
    ): Point {
        val swappedDimensions = sensorOrientation == 90 || sensorOrientation == 270

        val previewWidth = if (swappedDimensions) previewSize.height else previewSize.width
        val previewHeight = if (swappedDimensions) previewSize.width else previewSize.height

        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val previewRatio = previewWidth.toFloat() / previewHeight.toFloat()

        val width: Int
        val height: Int

        if (viewRatio > previewRatio) {
            // View가 더 넓음 -> 높이 기준으로 맞춤
            height = viewHeight
            width = (viewHeight * previewRatio).toInt()
        } else {
            // View가 더 높음 -> 가로 기준으로 맞춤
            width = viewWidth
            height = (viewWidth / previewRatio).toInt()
        }

        return Point(width, height)
    }

    /**
     * 지원하는 프리뷰 크기 목록 가져오기
     *
     * @param characteristics 카메라 특성
     * @return 지원하는 크기 배열
     */
    fun getSupportedPreviewSizes(characteristics: CameraCharacteristics): Array<Size> {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return emptyArray()

        return map.getOutputSizes(android.graphics.SurfaceTexture::class.java) ?: emptyArray()
    }

    /**
     * 16:9 비율에 가장 가까운 크기 선택
     *
     * @param sizes 지원하는 크기 배열
     * @param maxWidth 최대 가로
     * @param maxHeight 최대 세로
     * @return 16:9 비율에 가장 가까운 크기
     */
    fun findClosest16By9Size(
        sizes: Array<Size>,
        maxWidth: Int = 3840,
        maxHeight: Int = 2160
    ): Size? {
        val target16By9 = 16f / 9f

        return sizes
            .filter { it.width <= maxWidth && it.height <= maxHeight }
            .minByOrNull { size ->
                val ratio = size.width.toFloat() / size.height.toFloat()
                abs(ratio - target16By9)
            }
    }
}
