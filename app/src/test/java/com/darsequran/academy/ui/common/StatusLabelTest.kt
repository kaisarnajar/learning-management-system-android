package com.darsequran.academy.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

/** Status words and colours as the website shows them. */
class StatusLabelTest {
    @Test
    fun enrolmentsReadAsOnTheWebsite() {
        assertEquals("Active" to Tone.Info, statusLabel("ACTIVE"))
        assertEquals("Completed" to Tone.Success, statusLabel("completed"))
        assertEquals("Awaiting approval" to Tone.Warning, statusLabel("PENDING_APPROVAL"))
        assertEquals("Awaiting enrollment fee" to Tone.Warning, statusLabel("AWAITING_ENROLLMENT_FEE"))
        assertEquals("Rejected" to Tone.Danger, statusLabel("REJECTED"))
    }

    @Test
    fun aDeclinedPaymentIsRejectedButADeclinedOrderIsDeclined() {
        assertEquals("Pending Verification" to Tone.Warning, statusLabel("PENDING_VERIFICATION", StatusKind.Payment))
        assertEquals("Rejected" to Tone.Danger, statusLabel("DECLINED", StatusKind.Payment))
        assertEquals("Declined" to Tone.Danger, statusLabel("DECLINED", StatusKind.Order))
        assertEquals("Approved" to Tone.Success, statusLabel("APPROVED", StatusKind.Order))
    }

    @Test
    fun aPendingWaiverIsUnderReviewAndAPendingReviewAwaitsApproval() {
        assertEquals("Pending Review" to Tone.Warning, statusLabel("PENDING", StatusKind.Waiver))
        assertEquals("Pending approval" to Tone.Warning, statusLabel("PENDING", StatusKind.Review))
        assertEquals("Pending" to Tone.Warning, statusLabel("PENDING"))
    }
}
