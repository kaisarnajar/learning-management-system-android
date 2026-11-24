package com.darsequran.academy.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiCallTest {

    private fun httpError(code: Int, body: String = "") =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    private suspend fun failWith(e: Throwable) = apiCall<Unit> { throw e } as ApiResult.Err

    @Test
    fun okWrapsTheValue() = runTest {
        assertEquals(ApiResult.Ok(42), apiCall { 42 })
    }

    @Test
    fun serverMessageAndCodeAreUsed() = runTest {
        val err = failWith(httpError(400, """{"success":false,"error":"Coupon expired.","code":"COUPON_EXPIRED"}"""))
        assertEquals("Coupon expired.", err.message)
        assertEquals(400, err.status)
        assertEquals("COUPON_EXPIRED", err.code)
    }

    @Test
    fun statusFallbacksWithoutAServerMessage() = runTest {
        assertEquals("Please sign in again.", failWith(httpError(401)).message)
        assertEquals("You don't have access to this.", failWith(httpError(403)).message)
        assertEquals("Not found.", failWith(httpError(404)).message)
        assertEquals("Too many attempts. Please wait a moment and try again.", failWith(httpError(429)).message)
        assertEquals("The academy's server had a problem. Please try again.", failWith(httpError(503)).message)
        assertEquals("Something went wrong (418).", failWith(httpError(418)).message)
    }

    @Test
    fun anHtmlErrorPageFallsBackToTheStatusMessage() = runTest {
        assertEquals("Not found.", failWith(httpError(404, "<html>Not Found</html>")).message)
    }

    @Test
    fun networkFailureSaysNoConnection() = runTest {
        assertEquals("No connection. Check your internet and try again.", failWith(IOException("timeout")).message)
    }

    @Test
    fun unexpectedJsonSaysTheServerDiffers() = runTest {
        assertTrue(failWith(SerializationException("bad")).message.startsWith("The server's answer wasn't what this app expects."))
    }

    @Test
    fun unexpectedExceptionsDoNotShowTheirMessage() = runTest {
        val err = failWith(IllegalStateException("Expected BEGIN_OBJECT at com.darsequran.academy.data.remote.Course"))
        assertEquals("Something went wrong. Please try again.", err.message)
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        try {
            apiCall<Unit> { throw CancellationException("left the screen") }
            fail("CancellationException must propagate")
        } catch (_: CancellationException) {
        }
    }
}
