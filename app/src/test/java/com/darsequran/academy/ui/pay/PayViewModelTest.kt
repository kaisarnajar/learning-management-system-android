package com.darsequran.academy.ui.pay

import androidx.lifecycle.SavedStateHandle
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
    @Volatile private var settingsFail = false
    @Volatile private var oneTimeFee = false
    @Volatile private var lastSubmission = ""

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/api/v1/payment-settings" ->
                    if (settingsFail) MockResponse(code = 503) else MockResponse(body = """{"settings":{"upiId":"academy@upi"}}""")
                "/api/v1/payments/quote" -> MockResponse(
                    body = """{"courseId":"c1","type":"course","canPay":true,"baseAmountPaise":50000,"yearOptions":["2026"],
                        "oneTimeFee":$oneTimeFee,"coupons":[
                        {"id":"waiver-50","code":"HALF","percentage":50,"amountPaise":25000},
                        {"id":"waiver-100","code":"FULL","percentage":100,"amountPaise":0}]}""",
                )
                "/api/v1/payments/submit" -> {
                    submissions.incrementAndGet()
                    lastSubmission = request.body?.utf8().orEmpty()
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
        val vm = PayViewModel(app.container.repo, "c1", "course", SavedStateHandle())
        withTimeout(5_000) { vm.state.first { it.quote != null } }
        vm.submit("UPI", "123456789012", null)
        vm.submit("UPI", "123456789012", null)
        withTimeout(5_000) { vm.state.first { it.done } }
        vm.submit("UPI", "123456789012", null) // a tap after success
        delay(300)
        assertEquals(1, submissions.get())
    }

    @Test
    fun choicesComeBackAfterTheAppWasKilled() = runBlocking {
        // What the system hands back after process death, from the choices made before paying.
        val saved = SavedStateHandle(mapOf("couponId" to "waiver-50", "month" to 3, "year" to "2025"))
        val vm = PayViewModel(app.container.repo, "c1", "course", saved)
        withTimeout(5_000) { vm.state.first { it.quote != null } }
        val s = vm.state.value
        assertEquals("waiver-50", s.couponId)
        assertEquals(3, s.month)
        assertEquals("2025", s.year) // the quote's year options don't override a year already chosen
    }

    @Test
    fun choicesAreRecorded() {
        val saved = SavedStateHandle()
        val vm = PayViewModel(app.container.repo, "c1", "course", saved)
        vm.selectCoupon("waiver-50"); vm.selectMonth(7); vm.selectYear("2026")
        assertEquals("waiver-50", saved.get<String>("couponId"))
        assertEquals(7, saved.get<Int>("month"))
        assertEquals("2026", saved.get<String>("year"))
    }

    @Test
    fun paymentDetailsThatFailToLoadAreReportedAndCanBeRetried() = runBlocking {
        settingsFail = true
        val vm = PayViewModel(app.container.repo, "c1", "course", SavedStateHandle())
        withTimeout(5_000) { vm.state.first { it.quote != null } }
        assertEquals("The academy's server had a problem. Please try again.", vm.state.value.settingsError)
        settingsFail = false
        vm.reloadSettings()
        withTimeout(5_000) { vm.state.first { it.settings != null } }
        assertEquals(null, vm.state.value.settingsError)
        assertEquals("academy@upi", vm.state.value.settings?.upiId)
    }

    /** The form fields of the last submission (multipart: name, then the value after a blank line). */
    private fun fields(): Map<String, String> =
        Regex("name=\"([^\"]+)\"\\r\n(?:[^\\r]+\\r\n)*\\r\n([^\\r]*)\\r\n").findAll(lastSubmission)
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun submitted(vm: PayViewModel): Map<String, String> = runBlocking {
        withTimeout(5_000) { vm.state.first { it.quote != null } }
        vm.submit("upi", " 123456789012 ", null)
        withTimeout(5_000) { vm.state.first { it.done } }
        fields()
    }

    @Test
    fun aMonthlyFeeWithAWaiverSendsTheWaivedAmountAndThePaddedMonth() {
        val vm = PayViewModel(app.container.repo, "c1", "course", SavedStateHandle())
        runBlocking { withTimeout(5_000) { vm.state.first { it.quote != null } } }
        vm.selectCoupon("waiver-50"); vm.selectMonth(7); vm.selectYear("2026")
        assertEquals(
            mapOf(
                "courseId" to "c1", "paymentType" to "monthly", "paymentMonth" to "07", "paymentYear" to "2026",
                "paymentMethod" to "upi", "upiTransactionId" to "123456789012",
                "couponId" to "waiver-50", "expectedAmountPaise" to "25000",
            ),
            submitted(vm),
        )
    }

    @Test
    fun aOneTimeFeeIsSentForMonth01AndTheStandardFeeAsNone() {
        oneTimeFee = true
        val vm = PayViewModel(app.container.repo, "c1", "course", SavedStateHandle())
        runBlocking { withTimeout(5_000) { vm.state.first { it.quote != null } } }
        vm.selectMonth(9)
        val sent = submitted(vm)
        assertEquals("01", sent["paymentMonth"])
        assertEquals("none", sent["couponId"])
        assertEquals("50000", sent["expectedAmountPaise"])
    }

    @Test
    fun aFullyWaivedFeeSendsNoPaymentMethodOrTransaction() {
        val vm = PayViewModel(app.container.repo, "c1", "course", SavedStateHandle())
        runBlocking { withTimeout(5_000) { vm.state.first { it.quote != null } } }
        vm.selectCoupon("waiver-100")
        val sent = submitted(vm)
        assertEquals("0", sent["expectedAmountPaise"])
        assertEquals(null, sent["paymentMethod"])
        assertEquals(null, sent["upiTransactionId"])
    }

    @Test
    fun theEnrollmentFeeSendsNoMonthOrYear() {
        val sent = submitted(PayViewModel(app.container.repo, "c1", "enrollment", SavedStateHandle()))
        assertEquals("enrollment", sent["paymentType"])
        assertEquals(null, sent["paymentMonth"])
        assertEquals(null, sent["paymentYear"])
    }

    @Test
    fun aRestoredWaiverThatIsNoLongerOfferedIsDropped() = runBlocking {
        val saved = SavedStateHandle(mapOf("couponId" to "withdrawn"))
        val vm = PayViewModel(app.container.repo, "c1", "course", saved)
        withTimeout(5_000) { vm.state.first { it.quote != null } }
        assertEquals(null, vm.state.value.couponId)
        assertEquals(null, saved.get<String>("couponId"))
        assertEquals(50000, vm.state.value.amountPaise)
    }
}
