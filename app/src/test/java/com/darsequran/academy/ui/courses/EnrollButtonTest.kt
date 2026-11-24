package com.darsequran.academy.ui.courses

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.CourseDetailResponse
import com.darsequran.academy.data.remote.CourseEnrollmentState
import com.darsequran.academy.data.remote.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

/** The enrol button follows the website's CourseEnrollButton, state by state. */
class EnrollButtonTest {
    private val actions = EnrollActions({}, {}, {}, {}, {}, {})
    private fun detail(status: String? = null, feePending: Boolean = false, open: Boolean = true) =
        CourseDetailResponse(
        Course("c1", "Tajweed", priceInrPaise = 50_000),
        enrollmentOpen = open,
        enrollment = status?.let { CourseEnrollmentState("e1", it, enrollmentFeePending = feePending) },
    )
    private fun label(d: CourseDetailResponse, gate: EnrollGate = EnrollGate.Ready) =
        enrollButton(d, gate, actions).first
    private fun profile(complete: Boolean, verified: Boolean) =
        Profile("u1", "a@example.test", profileComplete = complete, emailVerified = "2026-01-01".takeIf { verified })

    @Test
    fun beforeRequestingTheChecksComeInTheWebsitesOrder() {
        assertEquals(EnrollGate.SignIn, enrollGate(false, null))
        assertEquals(EnrollGate.CompleteProfile, enrollGate(true, profile(complete = false, verified = false)))
        assertEquals(EnrollGate.VerifyEmail, enrollGate(true, profile(complete = true, verified = false)))
        assertEquals(EnrollGate.Ready, enrollGate(true, profile(complete = true, verified = true)))
        assertEquals("Sign in to request enrollment", label(detail(), EnrollGate.SignIn))
        assertEquals("Complete profile to enroll", label(detail(), EnrollGate.CompleteProfile))
        assertEquals("Verify your email to enroll", label(detail(), EnrollGate.VerifyEmail))
        assertEquals("Request enrollment", label(detail()))
    }

    @Test
    fun anExistingEnrollmentDecidesTheButton() {
        assertEquals("Awaiting enrollment approval", label(detail("pending_approval")))
        assertEquals("Pay enrollment fee · ₹500", label(detail("awaiting_enrollment_fee")))
        assertEquals("Awaiting payment verification", label(detail("awaiting_enrollment_fee", feePending = true)))
        assertEquals("Enrolled — Open course", label(detail("active")))
        assertEquals("Completed — Open course", label(detail("completed")))
    }

    @Test
    fun aRejectedRequestMayBeMadeAgain() = assertEquals("Request enrollment", label(detail("rejected")))

    @Test
    fun aClosedCourseSaysWhy() {
        assertEquals("Not accepting students", label(detail(open = false)))
        assertEquals("Enrollment has not started yet. Coming soon!", enrollmentClosedMessage("COMING_SOON"))
    }
}
