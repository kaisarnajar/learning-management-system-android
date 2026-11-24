package com.darsequran.academy.ui.teach

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
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
class TeachHomeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val server = MockWebServer()
    private val paths = Collections.synchronizedList(mutableListOf<String>())

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                paths += request.url.encodedPath
                return if (request.url.encodedPath == "/api/v1/teacher/courses") MockResponse(
                    body = """{"data":[{"id":"c1","title":"Tajweed basics"}],"totalCount":1,
                        "teacher":{"name":"Ustadh Bilal","email":"bilal@example.test"},"stats":{"totalCourses":1,"totalStudents":12}}""",
                ) else MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun oneRequestFillsTheHeaderAndTheFirstPage() {
        compose.setContent { TeachHomeScreen(rememberNavController()) }
        // The header's texts are merged into its card, so they are found in the unmerged tree.
        compose.waitUntilSettled { compose.onAllNodes(hasText("Ustadh Bilal"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("12"), useUnmergedTree = true).fetchSemanticsNodes().also { assertEquals(1, it.size) } // students
        assertEquals(1, paths.count { it == "/api/v1/teacher/courses" })
    }
}
