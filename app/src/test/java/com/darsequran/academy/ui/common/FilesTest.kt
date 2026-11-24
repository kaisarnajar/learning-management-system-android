package com.darsequran.academy.ui.common

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.documentCacheDir
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FilesTest {
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val server = MockWebServer()

    @Before fun setUp() { server.start(); CurrentEnvironment.testBaseUrl = server.url("/").toString() }
    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun aGenericTypeFallsBackToTheExtension() {
        assertEquals("application/pdf", mimeFor("notes.pdf", "application/octet-stream"))
        assertEquals("application/pdf", mimeFor("notes.PDF", null))
        assertEquals("image/png", mimeFor("diagram.png", "binary/octet-stream"))
        assertEquals("application/pdf", mimeFor("whatever", "application/pdf; charset=binary"))
        assertEquals("application/octet-stream", mimeFor("archive", null))
    }

    @Test
    fun aLargeFileIsStreamedIntoTheCache() = runBlocking {
        val size = 5 * 1024 * 1024
        server.enqueue(MockResponse.Builder().body(Buffer().write(ByteArray(size) { (it % 251).toByte() })).build())
        assertNull(downloadAndOpen(activity, "/api/files/a1/lesson.pdf", "lesson.pdf"))
        val file = File(documentCacheDir(activity), "lesson.pdf")
        assertEquals(size.toLong(), file.length())
    }

    @Test
    fun leavingTheScreenCancelsTheDownloadInsteadOfReportingAnError() = runBlocking {
        server.enqueue(MockResponse.Builder().body("late").headersDelay(3, TimeUnit.SECONDS).build())
        val download = async { downloadAndOpen(activity, "/api/files/a1/lesson.pdf", "lesson.pdf") }
        delay(200)
        download.cancel()
        val outcome = runCatching { download.await() }
        assertTrue("cancelled, not a message: $outcome", outcome.exceptionOrNull() is CancellationException)
    }
}
