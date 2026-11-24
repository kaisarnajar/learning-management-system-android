package com.darsequran.academy.ui.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val server = MockWebServer()
    private val tokens = """{"accessToken":"a","refreshToken":"r","user":{"id":"u1","email":"student@example.test"}}"""

    @Before fun setUp() { server.start(); CurrentEnvironment.testBaseUrl = server.url("/").toString() }

    @After fun tearDown() {
        GoogleWelcome.disarm()
        runBlocking { ApplicationProvider.getApplicationContext<AcademyApp>().container.sessionStore.clear() }
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    private fun vm() = AuthViewModel(ApplicationProvider.getApplicationContext<AcademyApp>().container.auth)

    private fun AuthViewModel.settled() = runBlocking { withTimeout(5_000) { state.first { it.done || it.error != null } } }

    @Test
    fun googleSignInIsDoneWithoutTheVerificationStep() {
        server.enqueue(MockResponse(body = tokens))
        val vm = vm().apply { google("id-token") }
        val state = vm.settled()
        assertTrue(state.done)
        assertTrue(state.googleSignedIn)
    }

    @Test
    fun registeringStillAsksToVerifyTheEmail() {
        server.enqueue(MockResponse(body = tokens))
        val vm = vm().apply { register("Amina", "student@example.test", "password123") }
        val state = vm.settled()
        assertTrue(state.done)
        assertFalse(state.googleSignedIn)
    }

    @Test
    fun googleRegistrationWaitsForThePolicies() {
        val vm = vm().apply { policiesNotAccepted() }
        assertEquals("Please agree to the Terms & Conditions and Privacy Policy to continue.", vm.state.value.error)
    }

    @Test
    fun aGoogleStudentWithAnIncompleteProfileIsWelcomedOnce() = runBlocking {
        server.enqueue(MockResponse(body = tokens))
        vm().apply { google("id-token") }.settled()
        val incomplete = ApiResult.Ok(Profile(id = "u1", email = "student@example.test", role = "USER"))
        assertTrue(GoogleWelcome.consume { incomplete })
        assertFalse(GoogleWelcome.consume { incomplete }) // once per sign-in
    }

    @Test
    fun aFailedGoogleSignInWelcomesNoOne() = runBlocking {
        server.enqueue(MockResponse(code = 401, body = """{"success":false,"error":"Invalid Google ID token."}"""))
        vm().apply { google("id-token") }.settled()
        assertFalse(GoogleWelcome.consume { ApiResult.Ok(Profile(id = "u1", email = "s@example.test")) })
    }

    @Test
    fun onlyStudentsWithAnIncompleteProfileAreSentToTheForm() {
        val profile = Profile(id = "u1", email = "s@example.test")
        assertTrue(needsProfileAfterGoogle(ApiResult.Ok(profile)))
        assertFalse(needsProfileAfterGoogle(ApiResult.Ok(profile.copy(profileComplete = true))))
        assertFalse(needsProfileAfterGoogle(ApiResult.Ok(profile.copy(role = "TEACHER"))))
        assertFalse(needsProfileAfterGoogle(ApiResult.Err("offline")))
    }
}
