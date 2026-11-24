package com.darsequran.academy.ui.manage.courses

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormatMarksTest {
    private val original = Locale.getDefault()

    @After fun restoreLocale() = Locale.setDefault(original)

    private val cases = mapOf(12.0 to "12", 12.5 to "12.5", 7.25 to "7.25", 0.0 to "0", 9.999 to "10", 33.333 to "33.33", 100.0 to "100")

    @Test
    fun formatsWithoutTrailingZeros() = cases.forEach { (marks, text) -> assertEquals(text, formatMarks(marks)) }

    @Test
    fun ignoresThePhonesLanguageAndRoundTrips() {
        for (locale in listOf(Locale.GERMANY, Locale.forLanguageTag("ar-EG"), Locale.forLanguageTag("ur-PK"), Locale.forLanguageTag("fa-IR"))) {
            Locale.setDefault(locale)
            cases.forEach { (marks, text) ->
                assertEquals("$locale", text, formatMarks(marks))
                assertEquals("$locale", text.toDouble(), formatMarks(marks).toDouble(), 0.0)
            }
        }
    }
}
