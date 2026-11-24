package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class UserRefreshTest {
    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val profileCalls = AtomicInteger()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                if (request.url.encodedPath == "/api/v1/auth/me") { profileCalls.incrementAndGet(); MockResponse(body = """{"user":{"id":"u1","email":"staff@example.test","role":"USER"}}""") }
                else MockResponse(code = 404) // e.g. the cart, fetched on sign-in
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
        runBlocking { app.container.sessionStore.save(TokenResponse("a", "r", user = SessionUser("u1", "staff@example.test", role = "ADMIN"))) }
    }

    @After fun tearDown() { runBlocking { app.container.sessionStore.clear() }; CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun aRoleChangedOnTheWebsiteReachesTheApp() = runBlocking {
        app.container.refreshUserIfStale(now = 1_000_000)
        withTimeout(5_000) { while (app.container.sessionStore.session.value?.user?.role != "USER") delay(20) }
        app.container.refreshUserIfStale(now = 1_000_000 + 60_000) // a minute later: not again
        app.container.refreshUserIfStale(now = 1_000_000 + 9 * 60_000)
        delay(200)
        assertEquals(1, profileCalls.get())
    }
}
