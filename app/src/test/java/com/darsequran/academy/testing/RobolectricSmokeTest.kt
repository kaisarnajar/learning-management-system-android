package com.darsequran.academy.testing

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the Robolectric harness: Compose UI renders and can be queried on the JVM. */
@RunWith(AndroidJUnit4::class)
class RobolectricSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun composeRendersOnTheJvm() {
        compose.setContent { Text("Assalamu alaikum") }
        compose.onNodeWithText("Assalamu alaikum").assertExists()
    }
}
