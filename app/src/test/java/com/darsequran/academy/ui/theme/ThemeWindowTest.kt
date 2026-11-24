package com.darsequran.academy.ui.theme

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The window behind the app follows the in-app theme, not the phone's. */
@RunWith(AndroidJUnit4::class)
class ThemeWindowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun windowColourFor(theme: String): Int {
        var background = 0
        compose.setContent { AcademyTheme(theme) { background = MaterialTheme.colorScheme.background.toArgb() } }
        compose.waitForIdle()
        val window = (compose.activity.window.decorView.background as ColorDrawable).color
        assertEquals("window matches the theme's background", background, window)
        return window
    }

    @Test
    fun darkAndLightGiveDifferentWindows() {
        val dark = windowColourFor("dark")
        compose.activityRule.scenario.recreate()
        val light = windowColourFor("light")
        assertNotEquals(dark, light)
    }
}
