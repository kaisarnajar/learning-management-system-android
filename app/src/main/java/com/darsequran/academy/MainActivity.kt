package com.darsequran.academy

import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.ui.nav.AppRoot
import com.darsequran.academy.ui.theme.AcademyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as AcademyApp).container
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val theme by container.sessionStore.theme.collectAsStateWithLifecycle()
            LaunchedEffect(theme) { applyAppNightMode(theme) }
            AcademyTheme(theme) { AppRoot() }
        }
    }

    /**
     * On Android 12+, tells the system the app's own light/dark choice, so the window and splash screen drawn before
     * Compose starts use it on the next launch (otherwise they follow the phone's setting and flash). Only when it
     * changed: setting it recreates the activity.
     */
    private fun applyAppNightMode(theme: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModes = getSystemService(UiModeManager::class.java) ?: return
        val mode = when (theme) {
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
        if (uiModes.nightMode != mode) uiModes.setApplicationNightMode(mode)
    }

    override fun onStart() {
        super.onStart()
        (application as AcademyApp).container.refreshUserIfStale()
    }
}
