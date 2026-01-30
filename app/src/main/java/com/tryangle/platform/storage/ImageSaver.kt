package com.tryangle.platform.storage

import android.content.ContentValues
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for saving images with EXIF support
 */
object ImageSaver {
    
    private const val TAG = "ImageSaver"
    private const val FOLDER_NAME = "TryAngle"
    
    /**
     * Save JPEG image with orientation metadata
     * 
     * @param context Application context
     * @param imageData JPEG image data
     * @param rotation Degrees to rotate (0, 90, 180, 270)
     */
    suspend fun saveImage(context: Context, imageData: ByteArray, rotation: Int = 0): Uri? = withContext(Dispatchers.IO) {
        try {
            val filename = generateFilename()
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageMediaStore(context, imageData, filename)
            } else {
                saveImageLegacy(context, imageData, filename)
            }

            // 저장된 파일에 EXIF 방향 정보 주입
            uri?.let { updateExifOrientation(context, it, rotation) }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image: ${e.message}", e)
            null
        }
    }
    
    private fun generateFilename(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "${dateFormat.format(Date())}.jpg"
    }
    
    private fun saveImageMediaStore(context: Context, imageData: ByteArray, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$FOLDER_NAME")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        return imageUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { it.write(imageData) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                uri
            } catch (e: IOException) {
                resolver.delete(uri, null, null)
                null
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun saveImageLegacy(context: Context, imageData: ByteArray, filename: String): Uri? {
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val appDir = File(dcimDir, FOLDER_NAME).apply { if (!exists()) mkdirs() }
        val imageFile = File(appDir, filename)
        
        return try {
            FileOutputStream(imageFile).use { it.write(imageData) }
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: IOException) { null }
    }

    /**
     * 이미지의 EXIF 방향 메타데이터를 업데이트합니다.
     */
    private fun updateExifOrientation(context: Context, uri: Uri, rotation: Int) {
        if (rotation == 0) return

        try {
            val exifOrientation = when (rotation) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation.toString())
                    exif.saveAttributes()
                }
            } else {
                // 구형 기기 대응 (경로 기반)
                val path = getPathFromUri(context, uri)
                if (path != null) {
                    val exif = ExifInterface(path)
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation.toString())
                    exif.saveAttributes()
                }
            }
            Log.d(TAG, "Exif orientation updated to $rotation degrees")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update EXIF", e)
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }
}
