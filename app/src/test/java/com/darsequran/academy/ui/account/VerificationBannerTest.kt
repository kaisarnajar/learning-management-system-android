package com.darsequran.academy.ui.account

import com.darsequran.academy.data.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationBannerTest {
    @Test
    fun theButtonReadsAsOnTheWebsite() {
        assertEquals("Resend Verification Email", resendLabel(ResendState()))
        assertEquals("Sending...", resendLabel(ResendState(sending = true)))
        assertEquals("Sent", resendLabel(ResendState(sent = true)))
    }

    @Test
    fun aResendThatWentThroughSaysToCheckTheInbox() =
        assertEquals(ResendState(sent = true, message = VERIFICATION_SENT), resendOutcome(ApiResult.Ok(Unit)))

    @Test
    fun aFailedResendShowsTheServersMessageAndCanBeTriedAgain() {
        val state = resendOutcome(ApiResult.Err("Too many requests. Please try again later."))
        assertEquals(ResendState(message = "Too many requests. Please try again later."), state)
        assertEquals("Resend Verification Email", resendLabel(state))
    }
}
