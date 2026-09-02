package com.darsequran.academy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    background = OffWhiteBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF9FAFB),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFE5E5E5),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    primaryContainer = DarkAccentMuted,
    onPrimaryContainer = GoldAccent,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun DarseQuranAcademyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Dynamic status bar background: EmeraldDark in light mode, DarkBackground (#121212) in dark mode
            val statusBarColor = if (darkTheme) DarkBackground else EmeraldDark
            window.statusBarColor = statusBarColor.toArgb()

            // Dynamic status bar icons:
            // Light Mode: isAppearanceLightStatusBars = true -> DARK status bar icons (time, battery) for high contrast on light/transparent backgrounds
            // Dark Mode: isAppearanceLightStatusBars = false -> WHITE status bar icons for high contrast on dark backgrounds
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
