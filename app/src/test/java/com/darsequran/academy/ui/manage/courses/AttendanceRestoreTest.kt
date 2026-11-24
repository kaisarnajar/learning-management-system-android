package com.darsequran.academy.ui.manage.courses

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.activity.ComponentActivity
import com.darsequran.academy.data.CurrentEnvironment
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import com.darsequran.academy.testing.waitUntilSettled
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** A rotation while marking attendance must keep the marks, so saving can't overwrite stored absences. */
@RunWith(AndroidJUnit4::class)
class AttendanceRestoreTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when {
                request.method == "GET" && request.url.encodedPath.endsWith("/attendance/students") -> MockResponse(
                    body = """{"data":[
                        {"enrollmentId":"e1","rollNumberLabel":"1","name":"Amina","email":"amina@example.test"},
                        {"enrollmentId":"e2","rollNumberLabel":"2","name":"Bilal","email":"bilal@example.test"}]}""",
                )
                request.method == "PUT" -> MockResponse(body = """{"success":true}""")
                else -> MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun marksSurviveRecreationAndAreSaved() {
        val restore = StateRestorationTester(compose)
        restore.setContent { AttendanceTakeScreen(Portal.Teacher, "c1", date = null, onBack = {}) }
        compose.waitUntilSettled { compose.onAllNodes(hasText("Amina")).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithText("Amina").performClick() // present → absent
        compose.onNodeWithText("Save attendance (1 of 2 present)").assertExists()

        restore.emulateSavedInstanceStateRestore()

        compose.waitUntilSettled { compose.onAllNodes(hasText("Amina")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save attendance (1 of 2 present)").assertExists().performClick()

        compose.waitUntilSettled { server.requestCount >= 3 } // students, the day check, then the save
        val put = generateSequence { server.takeRequest(5, TimeUnit.SECONDS) }.first { it.method == "PUT" }
        val body = put.body!!.utf8()
        assertTrue(body, body.contains(""""enrollmentId":"e1","isPresent":false"""))
        assertTrue(body, body.contains(""""enrollmentId":"e2","isPresent":true"""))
    }
}
