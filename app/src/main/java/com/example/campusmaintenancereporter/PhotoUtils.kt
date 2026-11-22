package com.example.campusmaintenancereporter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.ui.unit.dp

object PhotoUtils {
    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 80

    /** A temporary file for the camera to write into. */
    fun newCameraFile(context: Context): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    fun cameraUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Shrinks and compresses a photo, fixes its rotation, and saves it in the app's own storage.
     * Returns the saved path, or null if the photo could not be read.
     */
    fun compressToAppStorage(context: Context, source: Uri): String? {
        return try {
            val resolver = context.contentResolver

            // 1. Read the size only
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // 2. Decode a reduced copy, so large photos do not use too much memory
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
            var bitmap = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // 3. Turn it the right way up
            val rotation = resolver.openInputStream(source)?.use { rotationOf(it) } ?: 0
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            // 4. Limit the longest side
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                bitmap = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
                )
            }

            // 5. Save as a compressed JPEG
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val out = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            out.absolutePath
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var size = 1
        while (width / (size * 2) >= MAX_SIDE && height / (size * 2) >= MAX_SIDE) size *= 2
        return size
    }

    private fun rotationOf(input: InputStream): Int =
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}

/** Shows a saved photo. Pass onClick to make it tappable. */
@Composable
fun PhotoImage(path: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Attached photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        )
    }
}