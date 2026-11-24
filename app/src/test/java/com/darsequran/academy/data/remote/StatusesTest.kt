package com.darsequran.academy.data.remote

import com.darsequran.academy.ui.common.courseFeeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusesTest {
    @Test
    fun onlyStudyingOrFinishedStudentsOpenTheCourse() {
        assertTrue(EnrollmentStatus.opensLearn("active"))
        assertTrue(EnrollmentStatus.opensLearn("completed"))
        assertFalse(EnrollmentStatus.opensLearn("awaiting_enrollment_fee"))
        assertFalse(EnrollmentStatus.opensLearn("pending_approval"))
    }

    @Test
    fun adminsAndDevelopersUseTheAdminArea() {
        assertTrue(Roles.isAdmin("ADMIN")); assertTrue(Roles.isAdmin("DEVELOPER"))
        assertFalse(Roles.isAdmin("TEACHER")); assertFalse(Roles.isAdmin(null))
    }

    @Test
    fun feeText() {
        assertEquals("Free", courseFeeText(Course(id = "c1", title = "Tajweed")))
        assertEquals("₹500 / month", courseFeeText(Course(id = "c1", title = "Tajweed", monthlyFeeInrPaise = 50_000)))
        assertEquals("₹2,000 one time", courseFeeText(Course(id = "c1", title = "Tajweed", monthlyFeeInrPaise = 200_000, feeFrequency = "ONE_TIME")))
    }
}
