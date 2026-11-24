package com.darsequran.academy.ui.manage.money

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException

@RunWith(AndroidJUnit4::class)
class RegenerateReceiptTest {
    private val server = MockWebServer()
    private val container get() = ApplicationProvider.getApplicationContext<AcademyApp>().container

    @Before fun setUp() { server.start(); CurrentEnvironment.testBaseUrl = server.url("/").toString() }
    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun bothStepsWorking() = runBlocking {
        server.enqueue(MockResponse(body = """{"success":true}"""))
        server.enqueue(MockResponse(body = """{"success":true}"""))
        assertNull(container.regenerateReceipt("p1"))
        assertEquals("DELETE", server.takeRequest().method)
        assertEquals("POST", server.takeRequest().method)
    }

    @Test
    fun generateFailingAfterTheDeleteIsReported() = runBlocking {
        server.enqueue(MockResponse(body = """{"success":true}"""))
        server.enqueue(MockResponse(code = 500, body = """{"error":"PDF service unavailable."}"""))
        assertEquals("PDF service unavailable.", container.regenerateReceipt("p1"))
    }

    @Test
    fun aFailedDeleteChangesNothingAndThrows() = runBlocking {
        server.enqueue(MockResponse(code = 403, body = """{"error":"Forbidden"}"""))
        val failure = runCatching { container.regenerateReceipt("p1") }.exceptionOrNull()
        assertTrue(failure is HttpException)
        assertEquals(1, server.requestCount)
    }
}
