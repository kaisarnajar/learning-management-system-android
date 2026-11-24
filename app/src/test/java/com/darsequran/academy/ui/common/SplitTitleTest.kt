package com.darsequran.academy.ui.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitTitleTest {
    @Test
    fun theLastWordIsTheAccentAsOnTheWebsite() {
        val t = splitTitle("Featured courses", Color.Red)
        assertEquals("FEATURED COURSES", t.text)
        assertEquals(1, t.spanStyles.size)
        assertEquals("COURSES", t.text.substring(t.spanStyles[0].start, t.spanStyles[0].end))
    }

    @Test
    fun aOneWordTitleIsAllAccent() {
        val t = splitTitle("Announcements", Color.Red)
        assertEquals(0 to "ANNOUNCEMENTS".length, t.spanStyles[0].start to t.spanStyles[0].end)
    }
}
