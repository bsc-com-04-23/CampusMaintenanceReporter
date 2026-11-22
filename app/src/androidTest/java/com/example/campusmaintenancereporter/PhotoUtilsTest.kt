package com.example.campusmaintenancereporter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PhotoUtilsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun photo(width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val file = File(context.cacheDir, "test_${System.nanoTime()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return Uri.fromFile(file)
    }

    private fun sizeOf(path: String): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return bounds.outWidth to bounds.outHeight
    }

    @Test
    fun aLargePhotoIsShrunkToTheLimit() {
        val path = PhotoUtils.compressToAppStorage(context, photo(4000, 3000))
        assertNotNull(path)
        val (w, h) = sizeOf(path!!)
        assertTrue(maxOf(w, h) <= 1280)
        assertEquals(w.toFloat() / h, 4000f / 3000f, 0.02f)
    }

    @Test
    fun aSmallPhotoKeepsItsSize() {
        val path = PhotoUtils.compressToAppStorage(context, photo(800, 600))
        assertNotNull(path)
        assertEquals(800 to 600, sizeOf(path!!))
    }

    @Test
    fun anUnreadablePhotoGivesNull() {
        assertNull(PhotoUtils.compressToAppStorage(context, Uri.fromFile(File("/no/such/photo.jpg"))))
    }

    @Test
    fun deleteRemovesTheSavedPhoto() {
        val path = PhotoUtils.compressToAppStorage(context, photo(800, 600))!!
        assertTrue(File(path).exists())
        PhotoUtils.delete(path)
        assertFalse(File(path).exists())
    }
}