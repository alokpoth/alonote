package com.noto.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Handles saving, retrieving, downsampling, and deleting attached note images
 * within the app's secure internal private files directory (`files/note_images/`).
 *
 * Keeps the application 100% offline with zero cloud sync and zero sensitive permissions.
 */
object ImageStorageManager {

    private const val IMAGES_DIR = "note_images"
    private const val MAX_DIMENSION = 1920

    /**
     * Resolves the app-internal directory dedicated to storing note photos.
     */
    fun getImagesDir(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Resolves the absolute File object for a given image filename.
     */
    fun getImageFile(context: Context, filename: String): File {
        return File(getImagesDir(context), filename)
    }

    /**
     * Reads an image stream from a picked URI, scales it down to safe dimensions (max 1920px),
     * rotates according to EXIF metadata, and saves it into app internal storage as a compressed JPEG.
     *
     * @return Relative filename (e.g., "img_1729182390123_a1b2c3.jpg") or null on failure.
     */
    suspend fun saveImageFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Measure dimensions without loading the full bitmap into memory
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return@withContext null

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // 2. Compute inSampleSize power of 2 for memory safety on low-end devices
            var sampleSize = 1
            while ((origWidth / sampleSize) > MAX_DIMENSION * 1.5 || (origHeight / sampleSize) > MAX_DIMENSION * 1.5) {
                sampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 4. Handle EXIF orientation
            val rotatedBitmap = try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(decodedBitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(decodedBitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(decodedBitmap, 270f)
                        else -> decodedBitmap
                    }
                } ?: decodedBitmap
            } catch (e: Exception) {
                decodedBitmap
            }

            // 5. Downscale if still larger than MAX_DIMENSION
            val finalBitmap = if (rotatedBitmap.width > MAX_DIMENSION || rotatedBitmap.height > MAX_DIMENSION) {
                val ratio = minOf(
                    MAX_DIMENSION.toFloat() / rotatedBitmap.width,
                    MAX_DIMENSION.toFloat() / rotatedBitmap.height
                )
                val targetW = (rotatedBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (rotatedBitmap.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(rotatedBitmap, targetW, targetH, true)
                if (scaled != rotatedBitmap && rotatedBitmap != decodedBitmap) {
                    rotatedBitmap.recycle()
                }
                scaled
            } else {
                rotatedBitmap
            }

            // 6. Save JPEG into internal private files dir
            val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6)
            val filename = "img_${System.currentTimeMillis()}_$randomSuffix.jpg"
            val targetFile = File(getImagesDir(context), filename)

            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            // Clean up intermediary bitmap memory if applicable
            if (finalBitmap != decodedBitmap && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
            if (finalBitmap != rotatedBitmap && !rotatedBitmap.isRecycled) {
                rotatedBitmap.recycle()
            }

            filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Rotates a Bitmap by the specified degrees.
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Deletes an image file from internal storage.
     */
    fun deleteImage(context: Context, filename: String): Boolean {
        return try {
            val file = getImageFile(context, filename)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }
}
