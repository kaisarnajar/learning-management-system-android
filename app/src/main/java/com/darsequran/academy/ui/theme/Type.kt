package com.darsequran.academy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.darsequran.academy.R

/** Headings: the website's serif (variable font, weights set through the wght axis). */
val SerifFamily = FontFamily(
    serif(FontWeight.Normal),
    serif(FontWeight.Medium),
    serif(FontWeight.SemiBold),
    serif(FontWeight.Bold),
    serif(FontWeight.Normal, italic = true),
)

private fun serif(weight: FontWeight, italic: Boolean = false) = Font(
    if (italic) R.font.sourceserif4_italic else R.font.sourceserif4,
    weight,
    if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Body and interface: the website's sans. */
val SansFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibold, FontWeight.Bold),
)

/** Amiri, the website's general right-to-left face. */
val ArabicFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

private val base = Typography()

val AcademyTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    // Below the page title the website's headings are Poppins bold (its font-serif maps to Poppins); Source Serif is
    // kept for the hero and page titles, as on the website's PageHero.
    headlineSmall = base.headlineSmall.copy(
        fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
    ),
    titleLarge = base.titleLarge.copy(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
    titleMedium = base.titleMedium.copy(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall = base.titleSmall.copy(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    bodyLarge = base.bodyLarge.copy(fontFamily = SansFamily, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = SansFamily, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = base.bodySmall.copy(fontFamily = SansFamily, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = base.labelLarge.copy(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = base.labelMedium.copy(fontFamily = SansFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = base.labelSmall.copy(fontFamily = SansFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

/** The website's split section title (SplitSectionTitle): bold, uppercase, slightly spaced. */
val SectionTitleStyle = TextStyle(
    fontFamily = SansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.06.em,
)

/** Small-caps style label used above sections and on the inspiration card. */
val OverlineStyle = TextStyle(
    fontFamily = SansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 0.14.em,
)

/**
 * Indo-Pak Nastaleeq, the script of South Asian printed Qurans: the face the website sets verses and hadith in
 * (its .indo-pak-arabic class, on the homepage's Verse/Hadith of the Day). It has one weight only.
 */
val IndoPakFamily = FontFamily(Font(R.font.indopak_nastaleeq, FontWeight.Normal))

/** Verses and hadith, as the website's .indo-pak-arabic: Indo-Pak Nastaleeq, line height twice the size. */
val ArabicStyle = TextStyle(
    fontFamily = IndoPakFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 26.sp,
    lineHeight = 52.sp,
    fontFeatureSettings = "liga, calt",
)
