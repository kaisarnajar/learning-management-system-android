package com.darsequran.academy.ui.manage.money

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

@RunWith(AndroidJUnit4::class)
class StudentFilterTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val server = MockWebServer()
    private var searched: String? = null

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                searched = request.url.queryParameter("q")
                return MockResponse(body = """{"data":[{"id":"s1","name":"Amina","email":"amina@example.test"}],"totalCount":1}""")
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun searchPickAndClear() {
        var filter by mutableStateOf<String?>("courseId=c1")
        compose.setContent { StudentFilter(filter) { filter = it } }
        compose.onNode(hasSetTextAction()).performTextInput("ami")
        compose.mainClock.advanceTimeBy(400) // past the typing pause
        compose.waitUntilSettled { compose.onAllNodes(hasText("Amina (amina@example.test)")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals("ami", searched)

        compose.onNodeWithText("Amina (amina@example.test)").performClick()
        assertEquals(mapOf("courseId" to "c1", "studentId" to "s1"), decodeFilters(filter))
        compose.onNodeWithText("Student: Amina (amina@example.test)").assertExists()

        compose.onNodeWithText("Clear").performClick()
        assertEquals(mapOf("courseId" to "c1"), decodeFilters(filter))
    }
}
