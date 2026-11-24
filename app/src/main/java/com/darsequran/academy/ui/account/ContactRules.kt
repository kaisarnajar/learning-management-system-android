package com.darsequran.academy.ui.account

import com.darsequran.academy.data.remote.ContactRequest

/** The website's contactInquirySchema limits. */
const val CONTACT_NAME_MIN = 2
const val CONTACT_NAME_MAX = 100
const val CONTACT_PHONE_MAX = 20
const val CONTACT_MESSAGE_MIN = 20
const val CONTACT_MESSAGE_MAX = 5000
private const val PHONE_DIGITS_MIN = 7
private const val PHONE_DIGITS_MAX = 15

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

data class ContactDraft(val name: String, val email: String, val phone: String, val message: String)

/** The website's first message for an invalid form, in its field order; null when it can be sent. */
fun contactError(draft: ContactDraft): String? {
    val name = draft.name.trim()
    val digits = draft.phone.count(Char::isDigit)
    val message = draft.message.trim()
    return when {
        name.length < CONTACT_NAME_MIN -> "Name must be at least 2 characters."
        name.length > CONTACT_NAME_MAX -> "Name must be at most 100 characters."
        !EMAIL.matches(draft.email.trim()) -> "Enter a valid email address."
        draft.phone.trim().length > CONTACT_PHONE_MAX -> "Phone number is too long."
        draft.phone.isNotBlank() && digits !in PHONE_DIGITS_MIN..PHONE_DIGITS_MAX ->
            "Enter a valid phone number with 7 to 15 digits."
        message.length < CONTACT_MESSAGE_MIN -> "Your message must be at least 20 characters."
        message.length > CONTACT_MESSAGE_MAX -> "Message must be at most 5000 characters."
        else -> null
    }
}

fun contactRequest(draft: ContactDraft) = ContactRequest(
    name = draft.name.trim().ifBlank { null },
    email = draft.email.trim().ifBlank { null },
    phone = draft.phone.trim().ifBlank { null },
    message = draft.message.trim(),
)

const val CONTACT_SENT_TITLE = "Message Sent Successfully"
const val CONTACT_SENT =
    "Thank you. Your message has been received. We aim to respond within 2–3 business days."
