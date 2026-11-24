package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Signing out and profile refreshes can't leave the wrong person signed in. */
@RunWith(AndroidJUnit4::class)
class SessionSafetyTest {
    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val logoutArrived = CountDownLatch(1)
    private val releaseLogout = CountDownLatch(1)

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath == "/api/v1/auth/logout") {
                    logoutArrived.countDown()
                    releaseLogout.await(10, TimeUnit.SECONDS) // a server that is slow to answer
                    return MockResponse(body = """{"ok":true}""")
                }
                return MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
        val user = SessionUser("u1", "one@example.test")
        runBlocking { app.container.sessionStore.save(TokenResponse("a", "r", user = user)) }
    }

    @After fun tearDown() {
        releaseLogout.countDown()
        runBlocking { app.container.sessionStore.clear() }
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    @Test
    fun aProfileForSomeoneElseDoesNotOverwriteTheSession() = runBlocking {
        app.container.sessionStore.save(TokenResponse("b", "r2", user = SessionUser("u2", "two@example.test")))
        app.container.sessionStore.updateUser(SessionUser("u1", "one@example.test", role = "ADMIN"))
        val user = app.container.sessionStore.session.value?.user
        assertEquals("u2", user?.id)
        assertEquals("USER", user?.role)
    }

    @Test
    fun leavingTheScreenWhileSigningOutStillSignsOut() = runBlocking {
        val screen = CoroutineScope(Dispatchers.IO)
        screen.launch { app.container.auth.logout() }
        logoutArrived.await(5, TimeUnit.SECONDS)
        screen.coroutineContext[kotlinx.coroutines.Job]!!.cancel() // the Account screen's ViewModel is cleared
        releaseLogout.countDown()
        withTimeout(5_000) { while (app.container.sessionStore.session.value != null) delay(20) }
    }
}
