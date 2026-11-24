package com.darsequran.academy.ui.common

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val inr = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

private val inrPaise = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/** ₹ amount from paise: 34900 → "₹349", 12350 → "₹123.50". */
fun rupees(paise: Int): String = "₹" + (if (paise % 100 == 0) inr else inrPaise).format(paise / 100.0)

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val dayTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

private fun parse(value: String?): java.time.ZonedDateTime? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone) }.getOrNull()
        ?: runCatching { Instant.parse(value).atZone(zone) }.getOrNull()
        ?: runCatching { LocalDate.parse(value).atStartOfDay(zone) }.getOrNull()
        ?: runCatching { if (value.length > 10 && value[10] == 'T') LocalDate.parse(value.take(10)).atStartOfDay(zone) else null }.getOrNull()
}

/** "16 Nov 2026"; calendar dates (attendance, exams) keep their own day. */
fun formatDate(value: String?): String =
    parse(value)?.format(dayFormat) ?: (value ?: "") // free text (e.g. an event date typed by the admin) is shown as written

/** Calendar-only fields stored as UTC midnight: format in UTC so the day never shifts. */
fun formatCalendarDate(value: String?): String =
    runCatching { LocalDate.parse(value!!.take(10)).format(dayFormat) }.getOrDefault(value ?: "")

fun formatDateTime(value: String?): String = parse(value)?.format(dayTimeFormat) ?: ""

/** "2h ago", "3 days ago", or a date for older items. */
fun relativeTime(value: String?): String {
    val time = parse(value) ?: return ""
    val seconds = java.time.Duration.between(time.toInstant(), Instant.now()).seconds
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3600} h ago"
        seconds < 7 * 86_400 -> "${seconds / 86_400} days ago"
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
