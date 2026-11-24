package com.darsequran.academy.ui.courses

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.GradeEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseWordsTest {
    private fun enrollment(status: String, fee: Int = 0, completedAt: String? = null, rejectedAt: String? = null) =
        Enrollment(
            "e1", "c1", status, createdAt = "2026-03-02T10:00:00.000Z",
            completedAt = completedAt, rejectedAt = rejectedAt,
            course = Course("c1", "Tajweed", monthlyFeeInrPaise = fee),
        )

    @Test
    fun rollNumbersArePaddedAsOnTheWebsite() {
        assertEquals("Roll No: 05", rollNumberLabel(5))
        assertEquals("Roll No: 123", rollNumberLabel(123))
    }

    @Test
    fun aRejectedRequestSaysWhenAndWhenItWasMade() {
        assertEquals(
            listOf(
                "This request was not approved on 10 Mar 2026. Contact the academy if you have questions.",
                "Requested 2 Mar 2026",
            ),
            enrollmentNotes(enrollment("rejected", rejectedAt = "2026-03-10T08:00:00.000Z")),
        )
        assertEquals(
            "This request was not approved. Contact the academy if you have questions.",
            enrollmentNotes(enrollment("rejected")).first(),
        )
    }

    @Test
    fun theOtherStatuses() {
        assertEquals(listOf("No fee required"), enrollmentNotes(enrollment("active")))
        assertEquals(emptyList<String>(), enrollmentNotes(enrollment("active", fee = 50000)))
        assertEquals(
            listOf("Awaiting enrollment approval by the academy."),
            enrollmentNotes(enrollment("pending_approval")),
        )
        assertEquals(
            listOf("Completed 1 Jun 2026"),
            enrollmentNotes(enrollment("completed", completedAt = "2026-06-01T08:00:00.000Z")),
        )
    }

    @Test
    fun ungradedExamsAreLeftOutOfTheReport() {
        val grades = listOf(
            GradeEntry("g1", "Midterm", maxMarks = 50.0, marksObtained = 40.0),
            GradeEntry("g2", "Oral", maxMarks = 20.0, marksObtained = 0.0),
            GradeEntry("g3", "Final", maxMarks = 100.0, marksObtained = null),
        )
        assertEquals(GradeSummary(exams = 2, obtained = 40.0, max = 70.0, percentage = 57), gradeSummary(grades))
        assertEquals("—" to null, gradeMarks(grades[2]))
        assertEquals("0 / 20" to "recorded as 0", gradeMarks(grades[1]))
        assertEquals("40 / 50" to null, gradeMarks(grades[0]))
    }
}
