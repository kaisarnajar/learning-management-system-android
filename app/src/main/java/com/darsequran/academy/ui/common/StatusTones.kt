package com.darsequran.academy.ui.common

/*
 * Status → chip colour, one mapping per kind of record, so the same status looks the same on every screen.
 */

/**
 * Fee submissions, book orders and waiver requests (any case): waiting is amber, approved green, shipped (on its way,
 * not finished) blue, refused red. The money screens showed "shipped" green and the book-order screens blue.
 */
fun orderStatusTone(status: String?): Tone = when (status.orEmpty().lowercase()) {
    "pending", "pending_verification" -> Tone.Warning
    "approved" -> Tone.Success
    "shipped" -> Tone.Info
    "rejected", "declined" -> Tone.Danger
    else -> Tone.Neutral // refunded, cancelled, unknown
}

/** Course status, as the website's CourseStatusBadge colours it. */
fun courseStatusTone(status: String): Tone = when (status) {
    "PUBLISHED" -> Tone.Info
    "ONGOING" -> Tone.Success
    "ON_HOLD", "COMING_SOON" -> Tone.Warning
    else -> Tone.Neutral
}
