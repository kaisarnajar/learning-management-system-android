package com.darsequran.academy.data.staff

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionResponseTest {
    private fun outcome(redirect: String?) = ActionResponse(redirect = redirect).emailOutcome()

    @Test
    fun noEmailParameterMeansItWasSent() {
        assertEquals(EmailOutcome.Sent, outcome("/admin/fatwa/q1"))
        assertEquals(EmailOutcome.Sent, outcome(null))
        assertEquals(EmailOutcome.Sent, outcome("/admin/inquiries?page=2&emailed=1"))
    }

    @Test
    fun theWebsiteSaysWhenItDidNotGoOut() {
        assertEquals(EmailOutcome.Anonymous, outcome("/admin/fatwa?email=anonymous"))
        assertEquals(EmailOutcome.NotConfigured, outcome("/admin/inquiries?page=1&email=skipped"))
        assertEquals(EmailOutcome.Failed, outcome("/admin/inquiries?email=failed#top"))
    }
}
