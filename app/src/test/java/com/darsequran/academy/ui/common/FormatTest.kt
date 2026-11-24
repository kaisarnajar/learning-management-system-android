package com.darsequran.academy.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FormatTest {
    @Test fun oneDayIsSingular() = assertEquals("1 day ago", relativeTime(Instant.now().minusSeconds(30 * 3600).toString()))
    @Test fun moreDaysArePlural() = assertEquals("3 days ago", relativeTime(Instant.now().minusSeconds(3 * 86_400 + 60).toString()))

    @Test
    fun aZonelessDateTimeKeepsItsTime() = assertEquals("16 Nov 2026, 10:00 AM", formatDateTime("2026-11-16T10:00:00"))

    @Test
    fun aUtcInstantIsShownInAcademyTime() = assertEquals("16 Nov 2026, 3:30 PM", formatDateTime("2026-11-16T10:00:00Z"))

    @Test
    fun calendarDates() {
        assertEquals("16 Nov 2026", formatCalendarDate("2026-11-16T00:00:00.000Z"))
        assertEquals("", formatCalendarDate(null))
        assertEquals("To be announced", formatCalendarDate("To be announced"))
    }

    @Test
    fun rupeesWithAndWithoutPaise() { // grouping (1,00,000) comes from Android's ICU, not the JVM used here
        assertEquals("₹349", rupees(34_900))
        assertEquals("₹123.50", rupees(12_350))
    }
}
