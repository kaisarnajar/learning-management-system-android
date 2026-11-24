package com.darsequran.academy.ui.courses

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.PaymentSubmission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayCourseFeeTest {
    private fun enrollment(status: String = "active", fee: Int = 50000, frequency: String? = "MONTHLY") =
        Enrollment("e1", "c1", status, course = Course(id = "c1", title = "Tajweed", monthlyFeeInrPaise = fee, feeFrequency = frequency))

    private fun courseFee(status: String) = PaymentSubmission("p1", courseId = "c1", paymentType = "monthly", status = status)

    @Test fun anActiveCourseWithAMonthlyFeeCanBePaid() = assertTrue(canPayCourseFee(enrollment(), emptyList()))
    @Test fun aFinishedCourseCannot() = assertFalse(canPayCourseFee(enrollment(status = "completed"), emptyList()))
    @Test fun aFreeCourseCannot() = assertFalse(canPayCourseFee(enrollment(fee = 0), emptyList()))
    @Test fun anUnknownEnrollmentCannot() = assertFalse(canPayCourseFee(null, emptyList()))

    @Test
    fun aOneTimeFeeOnlyUntilItIsPaid() {
        val once = enrollment(frequency = "ONE_TIME")
        assertTrue(canPayCourseFee(once, emptyList()))
        assertFalse(canPayCourseFee(once, listOf(courseFee("pending_verification"))))
        assertFalse(canPayCourseFee(once, listOf(courseFee("approved"))))
        assertTrue(canPayCourseFee(once, listOf(courseFee("rejected"))))
    }
}
