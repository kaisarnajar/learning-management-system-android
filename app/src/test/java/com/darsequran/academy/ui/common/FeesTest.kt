package com.darsequran.academy.ui.common

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.PaymentSubmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FeesTest {
    private val today = LocalDate.of(2026, 11, 24)
    private val active = Enrollment("e1", "c1", "active", course = Course(id = "c1", title = "Tajweed", monthlyFeeInrPaise = 50000))
    private fun paid(label: String, status: String = "approved") = PaymentSubmission("p1", courseId = "c1", paymentType = "monthly", label = label, status = status)

    @Test fun theLabelMatchesTheWebsites() = assertEquals("Monthly fee — November 2026", monthlyFeeLabel(today))

    @Test fun dueWithNoPaymentForTheMonth() = assertTrue(monthlyFeeDue(active, emptyList(), today))
    @Test fun notDueOncePaid() = assertFalse(monthlyFeeDue(active, listOf(paid("Monthly fee — November 2026")), today))
    @Test fun notDueWhileUnderReview() = assertFalse(monthlyFeeDue(active, listOf(paid("Monthly fee — November 2026", "pending_verification")), today))
    @Test fun dueAgainWhenRefused() = assertTrue(monthlyFeeDue(active, listOf(paid("Monthly fee — November 2026", "rejected")), today))
    @Test fun lastMonthsPaymentDoesNotCount() = assertTrue(monthlyFeeDue(active, listOf(paid("Monthly fee — October 2026")), today))
    @Test fun aYearlyFeeStartingThisMonthIsNotThisMonthsFee() =
        assertTrue(monthlyFeeDue(active, listOf(paid("Yearly fee — starting November 2026")), today))
    @Test fun olderLabelsStillCount() = assertFalse(monthlyFeeDue(active, listOf(paid("November 2026")), today))
    @Test fun onlyForActiveMonthlyCourses() {
        assertFalse(monthlyFeeDue(active.copy(status = "completed"), emptyList(), today))
        assertFalse(monthlyFeeDue(active.copy(course = active.course!!.copy(feeFrequency = "YEARLY")), emptyList(), today))
    }
}
