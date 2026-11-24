package com.darsequran.academy.ui.manage.courses

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.ui.common.academyToday
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

/** "Mark attendance" on a day that is already marked must not silently replace it. */
@RunWith(AndroidJUnit4::class)
class AttendanceOverwriteTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer()
    private val methods = Collections.synchronizedList(mutableListOf<String>())
    private var todayMarked = true

    @Before fun setUp() {
        val today = academyToday().toString()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                methods += request.method!!
                val path = request.url.encodedPath
                return when {
                    path.endsWith("/attendance/students") -> MockResponse(body = """{"data":[{"enrollmentId":"e1","name":"Amina","email":"a@example.test"}]}""")
                    path.endsWith("/attendance/$today") && request.method == "GET" ->
                        if (todayMarked) MockResponse(body = """{"date":"$today","records":[{"enrollmentId":"e1","email":"a@example.test","isPresent":false}]}""")
                        else MockResponse(code = 404, body = """{"error":"Not found"}""")
                    request.method == "PUT" -> MockResponse(body = """{"success":true}""")
                    else -> MockResponse(code = 404)
                }
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { CurrentEnvironment.testBaseUrl = null; server.close() }

    private fun saveNewDay(onEdit: (String) -> Unit = {}) {
        compose.setContent { AttendanceTakeScreen(Portal.Teacher, "c1", date = null, onBack = {}, onEditExisting = onEdit) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Amina")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save attendance (1 of 1 present)").performClick()
    }

    @Test
    fun anAlreadyMarkedDayAsksInsteadOfReplacingIt() {
        var edited: String? = null
        saveNewDay { edited = it }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Attendance already marked")).fetchSemanticsNodes().isNotEmpty() }
        assertFalse("nothing was saved", "PUT" in methods)

        compose.onNodeWithText("Open that day").performClick()
        assertEquals(academyToday().toString(), edited)
        assertFalse("PUT" in methods)
    }

    @Test
    fun aNewDayIsSavedStraightAway() {
        todayMarked = false
        saveNewDay()
        compose.waitUntil(5_000) { "PUT" in methods }
    }
}
