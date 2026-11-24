package com.darsequran.academy.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import com.darsequran.academy.testing.waitUntilSettled
import kotlinx.coroutines.runBlocking
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

/** The next student on the phone starts fresh, not on the previous student's open screens. */
@RunWith(AndroidJUnit4::class)
class AppRootSessionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse(code = 404)
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { runBlocking { app.container.sessionStore.clear() }; CurrentEnvironment.testBaseUrl = null; server.close() }

    private fun signIn(id: String) = runBlocking {
        app.container.sessionStore.save(TokenResponse("a-$id", "r-$id", user = SessionUser(id, "$id@example.test", name = "Student $id")))
    }

    @Test
    fun anotherStudentDoesNotLandOnThePreviousStudentsScreen() {
        signIn("amina")
        compose.setContent { AppRoot() }
        compose.waitUntilSettled { compose.onAllNodes(hasText("Account")).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Account")[0].performClick()
        compose.waitUntilSettled { compose.onAllNodes(hasText("amina@example.test")).fetchSemanticsNodes().isNotEmpty() }

        signIn("bilal")
        compose.waitForIdle()
        assertEquals("starts on Home, not on the Account tab left open", 0, compose.onAllNodesWithText("bilal@example.test").fetchSemanticsNodes().size)
        compose.onNodeWithText("amina@example.test").assertDoesNotExist()
    }
}
