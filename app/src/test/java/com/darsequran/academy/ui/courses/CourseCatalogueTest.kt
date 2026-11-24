package com.darsequran.academy.ui.courses

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.CurrentEnvironment
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The catalogue renders from the shared pager and pages on. */
@RunWith(AndroidJUnit4::class)
class CourseCatalogueTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val server = MockWebServer()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val page = request.url.queryParameter("page") ?: "1"
                return if (request.url.encodedPath.endsWith("/courses")) MockResponse(
                    body = """{"data":[{"id":"c$page","title":"Course page $page"}],"totalCount":2,"page":$page,"pageSize":1}""",
                ) else MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun cataloguePagesThroughTheSharedPager() {
        compose.setContent { CoursesScreen(onOpenCourse = {}, onOpenLearn = {}, onSignIn = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Course page 1")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Course page 2")).fetchSemanticsNodes().isNotEmpty() }
    }
}
