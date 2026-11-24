package com.darsequran.academy.ui.account

import com.darsequran.academy.data.remote.ContactRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactRulesTest {
    private val message = "I would like to know when the next Tajweed class starts."
    private val ok = ContactDraft("Amina", "amina@example.com", "", message)

    @Test
    fun aCompleteFormCanBeSent() = assertNull(contactError(ok))

    @Test
    fun theMessageNeedsTwentyCharactersAndAtMostFiveThousand() {
        assertEquals("Your message must be at least 20 characters.", contactError(ok.copy(message = "Too short")))
        assertEquals(
            "Message must be at most 5000 characters.",
            contactError(ok.copy(message = "x".repeat(CONTACT_MESSAGE_MAX + 1))),
        )
        assertNull(contactError(ok.copy(message = "x".repeat(CONTACT_MESSAGE_MAX))))
    }

    @Test
    fun nameEmailAndPhoneAreCheckedAsOnTheWebsite() {
        assertEquals("Name must be at least 2 characters.", contactError(ok.copy(name = " A ")))
        assertEquals("Enter a valid email address.", contactError(ok.copy(email = "amina@")))
        assertEquals("Enter a valid phone number with 7 to 15 digits.", contactError(ok.copy(phone = "12345")))
        assertNull(contactError(ok.copy(phone = "+91 96229 66911")))
    }

    @Test
    fun theRequestIsTrimmedAndLeavesOutABlankPhone() = assertEquals(
        ContactRequest("Amina", "amina@example.com", null, message),
        contactRequest(ok.copy(name = " Amina ", phone = "  ", message = " $message ")),
    )
}
