package com.darsequran.academy.ui.manage.money

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.testing.waitUntilSettled
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
import org.robolectric.annotation.Config
import java.util.Collections

/** The money path an admin takes most: confirming or declining a student's fee payment. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp") // a phone's screen: the card's buttons sit below Robolectric's small default one
class PaymentApprovalTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val server = MockWebServer()
    private val requests = Collections.synchronizedList(mutableListOf<String>())

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += "${request.method} ${request.url.encodedPath}"
                return when (request.url.encodedPath) {
                    "/api/v1/admin/payments" -> MockResponse(
                        body = """{"data":[{"kind":"submission","id":"p1","amountInrPaise":50000,"status":"pending",
                            "user":{"name":"Amina","email":"amina@example.test"},"courseTitle":"Tajweed"}],
                            "totalCount":1}""",
                    )
                    "/api/v1/admin/payments/p1/approve", "/api/v1/admin/payments/p1/decline" ->
                        MockResponse(body = """{"success":true}""")
                    else -> MockResponse(code = 404)
                }
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    private fun count(what: String) = requests.count { it == what }

    private fun openScreen() {
        compose.setContent { PendingPaymentsScreen(onBack = {}, onOpenReceipt = {}) }
        compose.waitUntilSettled { compose.onAllNodes(hasText("Amina")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun confirmingApprovesOnceAndReloadsTheList() {
        openScreen()
        val listLoads = count("GET /api/v1/admin/payments")
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("from Amina and notify the student?", substring = true).assertExists()
        assertEquals("not sent before the dialog is confirmed", 0, count("POST /api/v1/admin/payments/p1/approve"))
        compose.onAllNodesWithText("Confirm")[1].performClick() // the card's button, then the dialog's
        compose.waitUntilSettled { count("GET /api/v1/admin/payments") > listLoads }
        assertEquals(1, count("POST /api/v1/admin/payments/p1/approve"))
        assertEquals(0, count("POST /api/v1/admin/payments/p1/decline"))
    }

    @Test
    fun decliningAsksFirstThenDeclinesOnce() {
        openScreen()
        compose.onNodeWithText("Decline").performClick()
        assertEquals("not sent before the dialog is confirmed", 0, count("POST /api/v1/admin/payments/p1/decline"))
        compose.onNodeWithText("Decline payment").assertExists()
        compose.onAllNodesWithText("Decline")[1].performClick() // the card's button, then the dialog's
        compose.waitUntilSettled { count("POST /api/v1/admin/payments/p1/decline") == 1 }
        assertEquals(0, count("POST /api/v1/admin/payments/p1/approve"))
    }
}
