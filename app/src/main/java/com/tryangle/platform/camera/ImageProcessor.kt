package com.tryangle.platform.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * 이미지 처리 유틸리티
 *
 * Camera2 API에서 제공하는 YUV 이미지를 RGB Bitmap으로 변환하고,
 * 회전, 크롭 등의 처리를 수행합니다.
 */
object ImageProcessor {

    /**
     * YUV_420_888 이미지를 Bitmap으로 변환
     *
     * Camera2 API에서 제공하는 기본 포맷인 YUV_420_888을 ARGB_8888 Bitmap으로 변환합니다.
     * iOS의 CVPixelBuffer -> UIImage 변환과 유사한 역할을 수행합니다.
     *
     * @param image Camera2 API에서 제공하는 YUV 이미지
     * @return 변환된 RGB Bitmap
     */
    fun yuv420ToBitmap(image: Image): Bitmap {
        require(image.format == ImageFormat.YUV_420_888) {
            "Image must be in YUV_420_888 format"
        }

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // YUV_420_888 -> NV21 변환
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // NV21 -> Bitmap 변환
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * Bitmap을 지정된 각도만큼 회전
     *
     * 디바이스 회전에 따라 이미지를 올바른 방향으로 회전시킵니다.
     * iOS의 UIImage orientation 처리와 유사합니다.
     *
     * @param bitmap 원본 Bitmap
     * @param degrees 회전 각도 (0, 90, 180, 270)
     * @return 회전된 Bitmap
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * Bitmap을 지정된 화면 비율로 크롭
     *
     * 이미지 중앙을 유지하면서 지정된 화면 비율로 크롭합니다.
     * Gate System의 AspectRatioGate와 함께 사용됩니다.
     *
     * @param bitmap 원본 Bitmap
     * @param aspectRatio 목표 화면 비율 (예: 16f/9f)
     * @return 크롭된 Bitmap
     */
    fun cropToAspectRatio(bitmap: Bitmap, aspectRatio: Float): Bitmap {
        val currentAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        // 이미 원하는 비율이면 그대로 반환
        if (kotlin.math.abs(currentAspectRatio - aspectRatio) < 0.01f) {
            return bitmap
        }

        val newWidth: Int
        val newHeight: Int

        if (currentAspectRatio > aspectRatio) {
            // 너무 넓음 -> 가로를 줄임
            newHeight = bitmap.height
            newWidth = (newHeight * aspectRatio).toInt()
        } else {
            // 너무 높음 -> 세로를 줄임
            newWidth = bitmap.width
            newHeight = (newWidth / aspectRatio).toInt()
        }

        // 중앙 기준으로 크롭
        val x = (bitmap.width - newWidth) / 2
        val y = (bitmap.height - newHeight) / 2

        return Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
    }

    /**
     * Bitmap 리사이즈
     *
     * 이미지를 지정된 크기로 리사이즈합니다.
     * 화면 비율을 유지하면서 최대 크기 내로 조정합니다.
     *
     * @param bitmap 원본 Bitmap
     * @param maxWidth 최대 가로 크기
     * @param maxHeight 최대 세로 크기
     * @return 리사이즈된 Bitmap
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scale = min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        if (scale >= 1.0f) {
            return bitmap // 이미 작거나 같은 크기
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Bitmap을 좌우 반전
     *
     * 전면 카메라 이미지를 미러링합니다 (셀카 모드).
     * iOS의 mirroredImage()와 유사합니다.
     *
     * @param bitmap 원본 Bitmap
     * @return 좌우 반전된 Bitmap
     */
    fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * 디바이스 회전 각도 계산
     *
     * 화면 회전과 카메라 센서 방향을 고려하여 최종 회전 각도를 계산합니다.
     *
     * @param displayRotation 디스플레이 회전 각도 (0, 90, 180, 270)
     * @param sensorOrientation 카메라 센서 방향 (0, 90, 180, 270)
     * @param isFrontFacing 전면 카메라 여부
     * @return 최종 회전 각도
     */
    fun computeRotation(
        displayRotation: Int,
        sensorOrientation: Int,
        isFrontFacing: Boolean
    ): Int {
        val rotation = when (displayRotation) {
            android.view.Surface.ROTATION_0 -> 0
            android.view.Surface.ROTATION_90 -> 90
            android.view.Surface.ROTATION_180 -> 180
            android.view.Surface.ROTATION_270 -> 270
            else -> 0
        }

        return if (isFrontFacing) {
            (sensorOrientation + rotation) % 360
        } else {
            (sensorOrientation - rotation + 360) % 360
        }
    }

    /**
     * Bitmap의 메모리 사용량 계산 (바이트)
     *
     * @param bitmap Bitmap 객체
     * @return 메모리 사용량 (바이트)
     */
    fun getBitmapSize(bitmap: Bitmap): Long {
        return bitmap.byteCount.toLong()
    }

    /**
     * Bitmap을 안전하게 재활용
     *
     * @param bitmap 재활용할 Bitmap
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
    
    /**
     * TextureView 변환 매트릭스 계산
     * 
     * 카메라 프리뷰의 왜곡을 방지하기 위해 TextureView에 적용할 변환 매트릭스를 계산합니다.
     * 
     * @param viewWidth TextureView 가로 크기
     * @param viewHeight TextureView 세로 크기
     * @param sensorOrientation 센서 방향 (0, 90, 180, 270)
     * @param displayRotation 디스플레이 회전 (Surface.ROTATION_*)
     * @return 변환 매트릭스
     */
    fun configureTransform(
        viewWidth: Int,
        viewHeight: Int,
        sensorOrientation: Int,
        displayRotation: Int
    ): android.graphics.Matrix {
        val matrix = android.graphics.Matrix()
        val viewRect = android.graphics.RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        
        // 디스플레이 회전 각도 계산
        val rotation = when (displayRotation) {
            android.view.Surface.ROTATION_0 -> 0
            android.view.Surface.ROTATION_90 -> 90
            android.view.Surface.ROTATION_180 -> 180
            android.view.Surface.ROTATION_270 -> 270
            else -> 0
        }
        
        // 센서가 세로 방향인 경우 (90도 또는 270도)
        if (rotation == 90 || rotation == 270) {
            // 버퍼 크기를 회전
            val bufferRect = android.graphics.RectF(
                0f, 0f, 
                viewHeight.toFloat(), 
                viewWidth.toFloat()
            )
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            
            // 스케일 계산
            val scale = kotlin.math.max(
                viewHeight.toFloat() / bufferRect.height(),
                viewWidth.toFloat() / bufferRect.width()
            )
            
            matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(-rotation.toFloat(), centerX, centerY)
        } else if (rotation == 180) {
            matrix.postRotate(180f, centerX, centerY)
        }
        
        return matrix
    }
}
