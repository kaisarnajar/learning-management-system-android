package com.darsequran.academy.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AcademyTodayTest {
    @Test
    fun usesTheAcademysDateNotThePhones() {
        // 20:00 UTC on 24 Nov is already 01:30 on 25 Nov in India; a phone set to New York still says 24 Nov (15:00).
        val clock = Clock.fixed(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("America/New_York"))
        assertEquals(LocalDate.of(2026, 11, 25), academyToday(clock))
    }

    @Test
    fun sameDayWhenBothAgree() {
        val clock = Clock.fixed(Instant.parse("2026-11-24T06:00:00Z"), ZoneId.of("UTC"))
        assertEquals(LocalDate.of(2026, 11, 24), academyToday(clock))
    }
}
