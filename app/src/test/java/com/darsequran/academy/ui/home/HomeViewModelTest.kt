package com.darsequran.academy.ui.home

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import com.darsequran.academy.testing.MainDispatcherRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                // The signed-in user's courses answer slowly...
                "/api/v1/enrollments" -> MockResponse.Builder()
                    .body("""{"data":[{"id":"e1","courseId":"c1","status":"active"}]}""")
                    .headersDelay(800, TimeUnit.MILLISECONDS).build()
                else -> MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
        runBlocking { app.container.sessionStore.save(TokenResponse("a", "r", user = SessionUser("u1", "student@example.test"))) }
    }

    @After fun tearDown() {
        runBlocking { app.container.sessionStore.clear() }
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    @Test
    fun aSlowLoadForThePreviousUserDoesNotOverwriteTheSignedOutHome() = runBlocking {
        val vm = HomeViewModel(app.container.repo, app.container.auth)
        withTimeout(5_000) { while (server.requestCount == 0) delay(10) } // the signed-in load is under way
        app.container.sessionStore.clear()                                  // ...and the user signs out
        withTimeout(5_000) { vm.state.first { it.session == null && !it.loading } }
        delay(1_200)                                                        // long after the slow answer arrived
        assertEquals(emptyList<Any>(), vm.state.value.enrollments)
    }

    @Test
    fun aTokenRefreshForTheSameUserDoesNotReload() = runBlocking {
        val vm = HomeViewModel(app.container.repo, app.container.auth)
        withTimeout(5_000) { vm.state.first { !it.loading } }
        val homeCalls = { server.requestCount }
        val before = homeCalls()
        app.container.sessionStore.save(TokenResponse("a2", "r2", user = SessionUser("u1", "student@example.test")))
        delay(300)
        assertEquals(before, homeCalls())
        assertEquals("a2", vm.state.value.session?.accessToken) // the new session still reaches the screen
    }
}
