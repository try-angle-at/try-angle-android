package com.tryangle.platform.camera

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

/**
 * ImageProcessor 단위 테스트
 *
 * 이미지 처리 로직을 검증합니다.
 */
class ImageProcessorTest {

    @Test
    fun `test compute rotation for front camera`() {
        val rotation = ImageProcessor.computeRotation(
            displayRotation = android.view.Surface.ROTATION_0,
            sensorOrientation = 270,
            isFrontFacing = true
        )
        
        assertEquals(270, rotation)
    }
    
    @Test
    fun `test compute rotation for back camera`() {
        val rotation = ImageProcessor.computeRotation(
            displayRotation = android.view.Surface.ROTATION_0,
            sensorOrientation = 90,
            isFrontFacing = false
        )
        
        assertEquals(90, rotation)
    }
    
    @Test
    fun `test compute rotation with display rotation 90`() {
        val frontRotation = ImageProcessor.computeRotation(
            displayRotation = android.view.Surface.ROTATION_90,
            sensorOrientation = 270,
            isFrontFacing = true
        )
        
        assertEquals(0, frontRotation)
        
        val backRotation = ImageProcessor.computeRotation(
            displayRotation = android.view.Surface.ROTATION_90,
            sensorOrientation = 90,
            isFrontFacing = false
        )
        
        assertEquals(0, backRotation)
    }
    
    @Test
    fun `test bitmap size calculation`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val size = ImageProcessor.getBitmapSize(bitmap)
        
        // ARGB_8888 = 4 bytes per pixel
        // 100x100 = 10,000 pixels
        // 10,000 * 4 = 40,000 bytes
        assertEquals(40000L, size)
        
        bitmap.recycle()
    }
    
    @Test
    fun `test bitmap rotation 0 degrees returns same bitmap`() {
        val original = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageProcessor.rotateBitmap(original, 0)
        
        assertSame(original, rotated)
        
        original.recycle()
    }
    
    @Test
    fun `test bitmap rotation 90 degrees swaps dimensions`() {
        val original = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageProcessor.rotateBitmap(original, 90)
        
        assertEquals(200, rotated.width)
        assertEquals(100, rotated.height)
        
        original.recycle()
        rotated.recycle()
    }
    
    @Test
    fun `test crop to aspect ratio wider image`() {
        // 2:1 비율 이미지를 16:9로 크롭
        val original = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val cropped = ImageProcessor.cropToAspectRatio(original, 16f / 9f)
        
        // 세로는 그대로, 가로가 줄어들어야 함
        assertEquals(100, cropped.height)
        assertTrue(cropped.width < 200)
        
        original.recycle()
        cropped.recycle()
    }
    
    @Test
    fun `test crop to aspect ratio taller image`() {
        // 1:2 비율 이미지를 16:9로 크롭
        val original = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val cropped = ImageProcessor.cropToAspectRatio(original, 16f / 9f)
        
        // 가로는 그대로, 세로가 줄어들어야 함
        assertEquals(100, cropped.width)
        assertTrue(cropped.height < 200)
        
        original.recycle()
        cropped.recycle()
    }
    
    @Test
    fun `test resize bitmap scales down correctly`() {
        val original = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val resized = ImageProcessor.resizeBitmap(original, 500, 500)
        
        assertEquals(500, resized.width)
        assertEquals(500, resized.height)
        
        original.recycle()
        resized.recycle()
    }
    
    @Test
    fun `test resize bitmap keeps aspect ratio`() {
        val original = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
        val resized = ImageProcessor.resizeBitmap(original, 400, 400)
        
        // 2:1 비율 유지되어야 함
        assertEquals(400, resized.width)
        assertEquals(200, resized.height)
        
        original.recycle()
        resized.recycle()
    }
    
    @Test
    fun `test flip bitmap horizontally`() {
        val original = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val flipped = ImageProcessor.flipBitmapHorizontally(original)
        
        // 크기는 동일해야 함
        assertEquals(100, flipped.width)
        assertEquals(200, flipped.height)
        
        original.recycle()
        flipped.recycle()
    }
    
    @Test
    fun `test recycle bitmap safely`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertFalse(bitmap.isRecycled)
        
        ImageProcessor.recycleBitmap(bitmap)
        assertTrue(bitmap.isRecycled)
        
        // 이미 재활용된 bitmap을 다시 재활용해도 에러 없어야 함
        ImageProcessor.recycleBitmap(bitmap)
        
        // null bitmap도 안전하게 처리되어야 함
        ImageProcessor.recycleBitmap(null)
    }
}
