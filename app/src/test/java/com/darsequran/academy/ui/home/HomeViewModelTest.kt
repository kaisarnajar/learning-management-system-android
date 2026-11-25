package com.darsequran.academy.ui.home

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.SavedHome
import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.HomeResponse
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
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.ui.account.VERIFICATION_SENT
import com.darsequran.academy.ui.account.resendLabel
import com.darsequran.academy.ui.account.resendOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    @Volatile private var resendFails = false
    @Volatile private var homeDelayMs = 0L
    @Volatile private var homeTitle = "Tajweed"
    private val resends = AtomicInteger()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                // The signed-in user's courses answer slowly...
                "/api/v1/enrollments" -> MockResponse.Builder()
                    .body("""{"data":[{"id":"e1","courseId":"c1","status":"active"}]}""")
                    .headersDelay(800, TimeUnit.MILLISECONDS).build()
                "/api/v1/home" -> MockResponse.Builder()
                    .body("""{"success":true,"courses":[{"id":"c1","title":"$homeTitle"}]}""")
                    .headersDelay(homeDelayMs, TimeUnit.MILLISECONDS).build()
                "/api/v1/auth/resend-verification" -> {
                    resends.incrementAndGet()
                    if (resendFails) MockResponse(code = 429, body = """{"error":"Too many requests."}""")
                    else MockResponse(body = """{"success":true}""")
                }
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
        withTimeout(5_000) { vm.state.first { it.enrollments.isNotEmpty() } } // the whole load, slowest call included
        val homeCalls = { server.requestCount }
        val before = homeCalls()
        app.container.sessionStore.save(TokenResponse("a2", "r2", user = SessionUser("u1", "student@example.test")))
        delay(300)
        assertEquals(before, homeCalls())
        assertEquals("a2", vm.state.value.session?.accessToken) // the new session still reaches the screen
    }

    @Test
    fun resendingTheVerificationEmailSaysItWasSentAndSendsOnce() = runBlocking {
        val vm = HomeViewModel(app.container.repo, app.container.auth)
        vm.resendVerification()
        vm.resendVerification() // a second tap while sending
        val sent = withTimeout(5_000) { vm.state.first { it.resend.sent } }.resend
        vm.resendVerification() // and after it was sent
        delay(200)
        assertEquals("Sent", resendLabel(sent))
        assertEquals(VERIFICATION_SENT, sent.message)
        assertEquals(1, resends.get())
    }

    @Test
    fun aFailedResendShowsTheServersErrorAndCanBeTriedAgain() = runBlocking {
        resendFails = true
        val vm = HomeViewModel(app.container.repo, app.container.auth)
        vm.resendVerification()
        val failed = withTimeout(5_000) { vm.state.first { it.resend.message != null } }.resend
        assertFalse(failed.sent)
        assertEquals("Resend Verification Email", resendLabel(failed))
        assertTrue(failed.message!!.isNotBlank())
    }

    @Test
    fun resendWordingFollowsTheWebsite() {
        assertEquals("Failed to resend verification email.", resendOutcome(ApiResult.Err("")).message)
        assertEquals("Too many requests.", resendOutcome(ApiResult.Err("Too many requests.")).message)
    }

    @Test
    fun theHomeContentShowsWithoutWaitingForTheStudentsSlowerCalls() = runBlocking {
        val vm = HomeViewModel(app.container.repo, app.container.auth)
        val shown = withTimeout(5_000) { vm.state.first { it.content?.courses?.isNotEmpty() == true } }
        assertEquals(emptyList<Any>(), shown.enrollments) // the enrollments (800 ms) are still on the way
        withTimeout(5_000) { vm.state.first { it.enrollments.isNotEmpty() } }
        Unit
    }

    @Test
    fun aColdStartShowsTheSavedHomeUntilTheNewOneArrives() = runBlocking {
        val saved = SavedHome(File(app.cacheDir, "home-${AppEnvironment.current.key}.json"), app.container.json)
        saved.write(HomeResponse(courses = listOf(Course(id = "c0", title = "Saved"))))
        homeDelayMs = 1_500
        homeTitle = "Fresh"
        val vm = HomeViewModel(app.container.repo, app.container.auth)
        withTimeout(1_000) { vm.state.first { it.content?.courses?.firstOrNull()?.title == "Saved" } }
        withTimeout(5_000) { vm.state.first { it.content?.courses?.firstOrNull()?.title == "Fresh" } }
        assertEquals("Fresh", saved.read()?.courses?.firstOrNull()?.title) // and the new one is saved for next time
    }
}
