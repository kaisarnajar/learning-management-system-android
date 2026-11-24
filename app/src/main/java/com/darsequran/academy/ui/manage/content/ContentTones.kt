package com.darsequran.academy.ui.manage.content

import com.darsequran.academy.ui.common.Tone

/*
 * Content management: site announcements, blogs and blog approvals, fatwa Q&A, the digital library and the
 * verse & hadith of the day (/api/v1/admin/...), and the teacher's own blog posts and fatwa answers
 * (/api/v1/teacher/...). Writes run the website's server actions, so their bodies are the website forms' fields.
 */

// ------------------------------------------------------------------ shared

/** Chip colours matching the website's badges. */
fun contentTone(label: String): Tone = when (label) {
    "Published", "Answered" -> Tone.Success
    "Home + Public", "On homepage", "Approved" -> Tone.Info
    "Pending approval", "Pending Review" -> Tone.Warning
    "Rejected" -> Tone.Danger
    else -> Tone.Neutral
}
