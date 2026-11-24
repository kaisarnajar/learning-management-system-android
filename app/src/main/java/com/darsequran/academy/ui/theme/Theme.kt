package com.darsequran.academy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Brand colours outside the Material roles: gold accents, the hero green, status tones. */
@Immutable
data class BrandColors(
    val gold: Color,
    val goldSoft: Color,
    val hero: Color,
    val heroDeep: Color,
    val onHero: Color,
    val mint: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val outlineSoft: Color,
)

private val LightBrand = BrandColors(
    gold = Color(0xFF9A7B0A),
    goldSoft = Color(0xFFD4AF37),
    hero = Color(0xFF0F3D2E),
    heroDeep = Color(0xFF062A1F),
    onHero = Color.White,
    mint = Color(0xFF7FD1AE),
    success = Color(0xFF1E6B47),
    successContainer = Color(0xFFDDF2E6),
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFCEFCF),
    info = Color(0xFF1F5B8F),
    infoContainer = Color(0xFFDDEBF8),
    danger = Color(0xFFB3261E),
    dangerContainer = Color(0xFFFBE3E1),
    outlineSoft = Color(0xFFE7E2D6),
)

private val DarkBrand = BrandColors(
    gold = Color(0xFFE9C349),
    goldSoft = Color(0xFFD4AF37),
    hero = Color(0xFF0C3326),
    heroDeep = Color(0xFF04170F),
    onHero = Color(0xFFF3F1EA),
    mint = Color(0xFF8FD5B6),
    success = Color(0xFF8FD5B6),
    successContainer = Color(0xFF173A2B),
    warning = Color(0xFFF2C46D),
    warningContainer = Color(0xFF3D2E0C),
    info = Color(0xFF9CC8F2),
    infoContainer = Color(0xFF172C40),
    danger = Color(0xFFF2B8B5),
    dangerContainer = Color(0xFF4A1816),
    outlineSoft = Color(0xFF29322E),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0F3D2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EFE1),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF9A7B0A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8EBC0),
    onSecondaryContainer = Color(0xFF2E2400),
    tertiary = Color(0xFF3B6658),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBDEBDA),
    onTertiaryContainer = Color(0xFF002019),
    background = Color(0xFFFBF9F4),
    onBackground = Color(0xFF1B1C19),
    surface = Color(0xFFFBF9F4),
    onSurface = Color(0xFF1B1C19),
    surfaceVariant = Color(0xFFE5E3DA),
    onSurfaceVariant = Color(0xFF5B5E57),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F4EC),
    surfaceContainer = Color(0xFFF2EFE7),
    surfaceContainerHigh = Color(0xFFECE9E0),
    surfaceContainerHighest = Color(0xFFE6E3DA),
    outline = Color(0xFF8A8D85),
    outlineVariant = Color(0xFFDDD9CD),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E1),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF2F0E9),
    inversePrimary = Color(0xFF8FD5B6),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FD5B6),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0F4D38),
    onPrimaryContainer = Color(0xFFABF2D1),
    secondary = Color(0xFFE9C349),
    onSecondary = Color(0xFF3C2F00),
    secondaryContainer = Color(0xFF544200),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFA1CFBF),
    onTertiary = Color(0xFF05372B),
    tertiaryContainer = Color(0xFF234E41),
    onTertiaryContainer = Color(0xFFBDEBDA),
    background = Color(0xFF0E1412),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF0E1412),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFF090E0C),
    surfaceContainerLow = Color(0xFF141A18),
    surfaceContainer = Color(0xFF1A211E),
    surfaceContainerHigh = Color(0xFF242B28),
    surfaceContainerHighest = Color(0xFF2F3633),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4943),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFE2E3DE),
    inverseOnSurface = Color(0xFF2F312D),
    inversePrimary = Color(0xFF0F3D2E),
)

val AcademyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalBrand = staticCompositionLocalOf { LightBrand }

/** Shortcut: `Brand.gold` inside composables. */
val Brand: BrandColors
    @Composable get() = LocalBrand.current

@Composable
fun AcademyTheme(themeSetting: String = "system", content: @Composable () -> Unit) {
    val dark = when (themeSetting) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalBrand provides if (dark) DarkBrand else LightBrand) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AcademyTypography,
            shapes = AcademyShapes,
            content = content,
        )
    }
}
