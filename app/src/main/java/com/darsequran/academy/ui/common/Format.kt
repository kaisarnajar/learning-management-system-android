package com.darsequran.academy.ui.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indianEnglish: Locale = Locale.forLanguageTag("en-IN")

private val inr = NumberFormat.getNumberInstance(indianEnglish).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

private val inrPaise = NumberFormat.getNumberInstance(indianEnglish).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/**
 * ₹ from paise in Indian grouping: 34900 → "₹349", 12350 → "₹123.50", -158475350 → "-₹15,84,753.50". With [exact]
 * the paise always show ("₹50.00"), as charge tables print them. The one rupee formatter: worked out in exact
 * decimals, not by dividing a Double.
 */
fun formatInr(paise: Long, exact: Boolean = false): String {
    val amount = BigDecimal.valueOf(paise).abs().movePointLeft(2)
    val whole = amount.remainder(BigDecimal.ONE).signum() == 0
    val text = (if (whole && !exact) inr else inrPaise).format(amount)
    return (if (paise < 0) "-₹" else "₹") + text
}

/** ₹ from paise: 34900 → "₹349", 12350 → "₹123.50". */
fun rupees(paise: Int): String = formatInr(paise.toLong())

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val dayTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
/** The academy's time zone: "today", a fee's month and a class date are the academy's, not the phone's. */
val academyZone: ZoneId = ZoneId.of("Asia/Kolkata")
private val zone: ZoneId = academyZone

/** Today's date at the academy, whatever time zone the phone is set to. */
fun academyToday(clock: Clock = Clock.system(academyZone)): LocalDate = LocalDate.now(clock.withZone(academyZone))

private fun parse(value: String?): java.time.ZonedDateTime? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone) }.getOrNull()
        ?: runCatching { Instant.parse(value).atZone(zone) }.getOrNull()
        ?: runCatching { LocalDate.parse(value).atStartOfDay(zone) }.getOrNull()
        // A date and time without a zone ("2026-11-16T10:00:00") is academy time; it used to lose its time of day.
        ?: runCatching { LocalDateTime.parse(value).atZone(zone) }.getOrNull()
        ?: runCatching { if (value.length > 10 && value[10] == 'T') LocalDate.parse(value.take(10)).atStartOfDay(zone) else null }.getOrNull()
}

/** "16 Nov 2026"; calendar dates (attendance, exams) keep their own day. */
fun formatDate(value: String?): String =
    parse(value)?.format(dayFormat) ?: (value ?: "") // free text (e.g. an event date typed by the admin) is shown as written

/** Calendar-only fields stored as UTC midnight: format in UTC so the day never shifts. */
fun formatCalendarDate(value: String?): String =
    value?.let { runCatching { LocalDate.parse(it.take(10)).format(dayFormat) }.getOrNull() } ?: (value ?: "")

fun formatDateTime(value: String?): String = parse(value)?.format(dayTimeFormat) ?: ""

/** "2h ago", "3 days ago", or a date for older items. */
fun relativeTime(value: String?): String {
    val time = parse(value) ?: return ""
    val seconds = java.time.Duration.between(time.toInstant(), Instant.now()).seconds
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3600} h ago"
        seconds < 7 * 86_400 -> "${countOf((seconds / 86_400).toInt(), "day")} ago"
        else -> time.format(dayFormat)
    }
}

fun feeFrequencySuffix(frequency: String?): String = when (frequency) {
    "MONTHLY", null -> "/ month"
    "EVERY_3_MONTHS" -> "/ 3 months"
    "EVERY_6_MONTHS" -> "/ 6 months"
    "YEARLY" -> "/ year"
    "ONE_TIME" -> "one time"
    else -> ""
}

fun titleCase(value: String): String =
    value.lowercase(Locale.ENGLISH).split('_', ' ').joinToString(" ") { it.replaceFirstChar { c -> c.titlecase(Locale.ENGLISH) } }

/** Labels the web app shows for announcement categories. */
fun announcementCategory(value: String): String = when (value) {
    "EXAMS_TESTS" -> "Exams & Tests"
    "ASSIGNMENTS_HOMEWORK" -> "Assignments & Homework"
    "STUDY_MATERIALS" -> "Study Materials"
    "CLASS_SCHEDULE" -> "Class Schedule"
    "COURSE_ANNOUNCEMENT" -> "Course Announcement"
    "GENERAL_NOTICE" -> "General Notice"
    else -> titleCase(value)
}

/** "1 comment", "3 comments". */
fun countOf(n: Int, singular: String, plural: String = singular + "s"): String = "$n ${if (n == 1) singular else plural}"
