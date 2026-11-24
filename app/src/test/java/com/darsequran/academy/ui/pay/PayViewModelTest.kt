package com.darsequran.academy.ui.pay

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
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
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PayViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val submissions = AtomicInteger()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/api/v1/payments/quote" -> MockResponse(body = """{"courseId":"c1","type":"course","canPay":true,"baseAmountPaise":50000,"yearOptions":["2026"]}""")
                "/api/v1/payments/submit" -> {
                    submissions.incrementAndGet()
                    MockResponse.Builder().body("""{"success":true}""").headersDelay(200, TimeUnit.MILLISECONDS).build()
                }
                else -> MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun aDoubleTapSubmitsOnePayment() = runBlocking {
        val vm = PayViewModel(app.container.repo, "c1", "course")
        withTimeout(5_000) { vm.state.first { it.quote != null } }
        vm.submit("UPI", "123456789012", null)
        vm.submit("UPI", "123456789012", null)
        withTimeout(5_000) { vm.state.first { it.done } }
        vm.submit("UPI", "123456789012", null) // a tap after success
        delay(300)
        assertEquals(1, submissions.get())
    }
}
