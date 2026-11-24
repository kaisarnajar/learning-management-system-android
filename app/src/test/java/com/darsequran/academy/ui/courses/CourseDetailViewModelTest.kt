package com.darsequran.academy.ui.courses

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import com.darsequran.academy.testing.MainDispatcherRule
import com.darsequran.academy.ui.common.Load
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
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CourseDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val enrolls = AtomicInteger()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val signedIn = request.headers["Authorization"] != null
                return when (request.url.encodedPath) {
                    "/api/v1/courses/c1" -> MockResponse(
                        body = """{"course":{"id":"c1","title":"Tajweed"},"enrollmentOpen":true""" +
                            (if (signedIn) ""","enrollment":{"id":"e1","courseId":"c1","status":"awaiting_enrollment_fee"}}""" else "}"),
                    )
                    "/api/v1/enrollments" -> {
                        enrolls.incrementAndGet()
                        MockResponse.Builder().body("""{"success":true,"requiresEnrollmentFee":true}""").headersDelay(200, TimeUnit.MILLISECONDS).build()
                    }
                    else -> MockResponse(code = 404)
                }
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() {
        runBlocking { app.container.sessionStore.clear() }
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    private fun CourseDetailViewModel.enrollment() = (state.value as? Load.Ready)?.value?.enrollment

    @Test
    fun signingInShowsTheEnrollment() = runBlocking {
        val vm = CourseDetailViewModel(app.container.repo, app.container.auth, "c1")
        withTimeout(5_000) { vm.state.first { it is Load.Ready } }
        assertEquals(null, vm.enrollment())
        app.container.sessionStore.save(TokenResponse("a", "r", user = SessionUser("u1", "student@example.test")))
        withTimeout(5_000) { while (vm.enrollment() == null) delay(20) }
        assertEquals("awaiting_enrollment_fee", vm.enrollment()?.status)
    }

    @Test
    fun aDoubleTapAppliesOnce() = runBlocking {
        val vm = CourseDetailViewModel(app.container.repo, app.container.auth, "c1")
        withTimeout(5_000) { vm.state.first { it is Load.Ready } }
        vm.apply(); vm.apply()
        withTimeout(5_000) { vm.busy.first { !it } }
        delay(300)
        assertEquals(1, enrolls.get())
    }

    @Test
    fun aPaidCourseSendsTheStudentOnToPayThroughState() = runBlocking {
        val vm = CourseDetailViewModel(app.container.repo, app.container.auth, "c1")
        withTimeout(5_000) { vm.state.first { it is Load.Ready } }
        vm.apply()
        assertEquals(CourseDetailViewModel.AfterApply.PAY, withTimeout(5_000) { vm.next.first { it != null } })
        vm.consumeNext()
        assertEquals(null, vm.next.value)
    }
}
