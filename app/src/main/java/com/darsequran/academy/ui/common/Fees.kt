package com.darsequran.academy.ui.common

import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.PaymentSubmission
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** "November 2026": the period a monthly fee is for. */
fun feePeriod(month: LocalDate): String = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"

/** The label the website gives a monthly fee payment ("Monthly fee — November 2026"; its buildMonthlyFeeLabel). */
fun monthlyFeeLabel(month: LocalDate): String = "Monthly fee — ${feePeriod(month)}"

/**
 * Whether [enrollment]'s fee for [today]'s month is still to pay: an active course billed monthly, with no payment
 * for that month that the academy hasn't refused. A payment is matched by the label the website gives it, in full
 * (matching any text containing "November 2026" broke as soon as the wording around it changed).
 */
fun monthlyFeeDue(enrollment: Enrollment, submissions: List<PaymentSubmission>, today: LocalDate = academyToday()): Boolean {
    val course = enrollment.course ?: return false
    if (enrollment.status != "active" || course.monthlyFeeInrPaise <= 0 || (course.feeFrequency ?: "MONTHLY") != "MONTHLY") return false
    val label = monthlyFeeLabel(today)
    return submissions.none {
        it.courseId == enrollment.courseId && it.paymentType != "enrollment" &&
            (it.label == label || it.label == feePeriod(today)) && // the second: labels from before the website's format
            it.status != "rejected" && it.status != "declined"
    }
}
