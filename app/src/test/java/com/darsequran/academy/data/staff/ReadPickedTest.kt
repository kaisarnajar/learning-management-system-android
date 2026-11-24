package com.darsequran.academy.data.staff

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.readImageAsJpeg
import com.darsequran.academy.data.sampleSizeFor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

/** A stand-in for the photo picker / document provider: files with a type, a name and (optionally) a size. */
class FakeFilesProvider : ContentProvider() {
    data class Entry(val mime: String, val bytes: ByteArray, val reportSize: Boolean = true)
    companion object { val files = mutableMapOf<String, Entry>() }

    override fun onCreate() = true
    override fun getType(uri: Uri) = files[uri.lastPathSegment]?.mime
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val entry = files.getValue(uri.lastPathSegment!!)
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            addRow(arrayOf<Any?>(uri.lastPathSegment, if (entry.reportSize) entry.bytes.size.toLong() else null))
        }
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = File.createTempFile("picked", null).apply { writeBytes(files.getValue(uri.lastPathSegment!!).bytes) }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadPickedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun setUp() {
        Robolectric.setupContentProvider(FakeFilesProvider::class.java, "test.files")
        FakeFilesProvider.files.clear()
    }

    private fun put(name: String, entry: FakeFilesProvider.Entry): Uri {
        FakeFilesProvider.files[name] = entry
        return Uri.parse("content://test.files/$name")
    }

    private fun png(width: Int, height: Int) = ByteArrayOutputStream().use {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray()
    }

    @Test
    fun aFileWithinTheLimitIsRead() = runBlocking {
        val uri = put("cover.png", FakeFilesProvider.Entry("image/png", png(20, 20)))
        val outcome = context.readPicked(uri, UploadLimit.Cover)
        assertTrue(outcome is PickOutcome.Picked)
        assertEquals("cover.png", (outcome as PickOutcome.Picked).file.name)
    }

    @Test
    fun aFileOverTheLimitIsRefusedFromItsReportedSize() = runBlocking {
        val uri = put("big.pdf", FakeFilesProvider.Entry("application/pdf", ByteArray(3 * 1024 * 1024)))
        val outcome = context.readPicked(uri, UploadLimit.Cover.copy(mimeTypes = emptySet()))
        assertEquals("This file is 3.0 MB; the limit here is 2.0 MB.", (outcome as PickOutcome.Rejected).reason)
    }

    @Test
    fun aFileOverTheLimitIsRefusedEvenWhenItsSizeIsUnknown() = runBlocking {
        val uri = put("big.pdf", FakeFilesProvider.Entry("application/pdf", ByteArray(3 * 1024 * 1024), reportSize = false))
        assertTrue(context.readPicked(uri, UploadLimit.Cover.copy(mimeTypes = emptySet())) is PickOutcome.Rejected)
    }

    @Test
    fun aTypeTheWebsiteRefusesIsRefusedHere() = runBlocking {
        val uri = put("notes.pdf", FakeFilesProvider.Entry("application/pdf", ByteArray(10)))
        val outcome = context.readPicked(uri, UploadLimit.Image)
        assertEquals("This type of file can't be used here; choose a JPEG, PNG, WebP or GIF file.", (outcome as PickOutcome.Rejected).reason)
    }

    @Test
    fun aHugePhotoIsShrunkInsteadOfDecodedAtFullSize() = runBlocking {
        val uri = put("camera.png", FakeFilesProvider.Entry("image/png", png(4000, 3000)))
        val jpeg = context.readImageAsJpeg(uri, maxSide = 1600)!!
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        assertEquals(1600, maxOf(decoded.width, decoded.height))
    }

    @Test
    fun sampleSizeIsThePowerOfTwoThatStaysAboveTheTarget() {
        assertEquals(1, sampleSizeFor(1200, 900, 1600))
        assertEquals(2, sampleSizeFor(4000, 3000, 1600))
        assertEquals(4, sampleSizeFor(8000, 6000, 1600))
        assertEquals(1, sampleSizeFor(3199, 100, 1600))
    }
}
