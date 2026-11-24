package com.darsequran.academy.ui.account

import com.darsequran.academy.data.ApiResult

/** The website's VerificationBanner states: idle, sending, or the answer to the last resend. */
data class ResendState(val sending: Boolean = false, val sent: Boolean = false, val message: String? = null)

const val VERIFICATION_SENT =
    "Verification email sent! Please check your Inbox. If you don't see it, check your Spam/Junk folder as well."

/** The state after a resend answered with [result]. */
fun resendOutcome(result: ApiResult<*>): ResendState = when (result) {
    is ApiResult.Ok -> ResendState(sent = true, message = VERIFICATION_SENT)
    is ApiResult.Err -> ResendState(message = result.message.ifBlank { "Failed to resend verification email." })
}

/** The button's words, as on the website. */
fun resendLabel(state: ResendState): String = when {
    state.sending -> "Sending..."
    state.sent -> "Sent"
    else -> "Resend Verification Email"
}
