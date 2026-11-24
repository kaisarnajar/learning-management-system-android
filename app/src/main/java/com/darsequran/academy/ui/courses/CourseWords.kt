package com.darsequran.academy.ui.courses

import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.EnrollmentStatus
import com.darsequran.academy.data.remote.GradeEntry
import com.darsequran.academy.ui.common.formatDate
import kotlin.math.roundToInt

// The website's words for a student's courses: /profile/courses and its attendance, grades and announcements pages.

private const val PERCENT = 100

/** "Roll No: 05": the website pads roll numbers to two digits (services/roll-numbers.ts formatRollNumber). */
internal fun rollNumberLabel(rollNumber: Int): String = "Roll No: ${rollNumber.toString().padStart(2, '0')}"

/** The lines under a course on My courses, as the website's course card shows them for its status. */
internal fun enrollmentNotes(e: Enrollment): List<String> = buildList {
    when (e.status) {
        EnrollmentStatus.PENDING_APPROVAL -> add("Awaiting enrollment approval by the academy.")
        EnrollmentStatus.AWAITING_ENROLLMENT_FEE -> add("Pay the enrollment fee to complete your application.")
        EnrollmentStatus.REJECTED -> add(
            "This request was not approved${e.rejectedAt?.let { " on ${formatDate(it)}" } ?: ""}. " +
                "Contact the academy if you have questions.",
        )
        EnrollmentStatus.ACTIVE -> if ((e.course?.monthlyFeeInrPaise ?: 0) <= 0) add("No fee required")
    }
    e.completedAt?.let { add("Completed ${formatDate(it)}") }
    val waiting = e.status == EnrollmentStatus.AWAITING_ENROLLMENT_FEE || e.status == EnrollmentStatus.PENDING_APPROVAL
    if (!EnrollmentStatus.opensLearn(e.status) && !waiting && e.createdAt.isNotBlank()) {
        add("Requested ${formatDate(e.createdAt)}")
    }
}

/** The grade report's totals (components/grades/StudentGradeReport.tsx): ungraded exams are left out. */
internal data class GradeSummary(val exams: Int, val obtained: Double, val max: Double, val percentage: Int)

internal fun gradeSummary(grades: List<GradeEntry>): GradeSummary {
    val graded = grades.filter { it.marksObtained != null }
    val obtained = graded.sumOf { it.marksObtained ?: 0.0 }
    val max = graded.sumOf { it.maxMarks }
    val percentage = if (max == 0.0) 0 else (obtained / max * PERCENT).roundToInt()
    return GradeSummary(graded.size, obtained, max, percentage)
}

/** An exam's marks: "—" when not graded; a zero is noted as "recorded as 0", as on the website. */
internal fun gradeMarks(g: GradeEntry): Pair<String, String?> {
    val marks = g.marksObtained ?: return "—" to null
    return "${formatMarks(marks)} / ${formatMarks(g.maxMarks)}" to if (marks == 0.0) "recorded as 0" else null
}

internal fun formatMarks(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
