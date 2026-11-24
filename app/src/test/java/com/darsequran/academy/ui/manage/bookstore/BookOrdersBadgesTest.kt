package com.darsequran.academy.ui.manage.bookstore

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import java.util.Collections

@RunWith(AndroidJUnit4::class)
class BookOrdersBadgesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val server = MockWebServer()
    private val requests = Collections.synchronizedList(mutableListOf<String>())

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request.url.encodedPath
                return MockResponse(body = """{"data":[],"totalCount":0,"counts":{"pending":3,"approved":1,"completed":7}}""")
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun theTabCountsComeWithTheListInOneRequest() {
        compose.setContent { BookOrdersListScreen(onBack = {}, onOpen = {}, onBooks = {}) }
        compose.waitUntilSettled { compose.onAllNodes(hasText("Pending approvals (3)"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, requests.size)
    }
}
