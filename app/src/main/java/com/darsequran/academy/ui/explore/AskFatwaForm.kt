package com.darsequran.academy.ui.explore

import com.darsequran.academy.data.remote.AskFatwaRequest
import java.net.URI

/** The website's categories (services/fatwa.ts FATWA_CATEGORIES), used until the app config has loaded. */
internal val WEBSITE_FATWA_CATEGORIES = listOf(
    "Islam", "Quran", "Hadith", "Fiqh", "Tajweed", "Seerah", "Arabic Language", "Atheism", "Fatwa", "Others",
)

/** The category that opens the "Specify Category" free-text field on the website. */
internal const val OTHER_FATWA_CATEGORY = "Others"
internal const val ANONYMOUS_ASKER = "Anonymous"

private const val TITLE_MIN = 10
private const val QUESTION_MIN = 30
private const val CATEGORY_MIN = 2
private const val CATEGORY_MAX = 100
private const val NAME_MIN = 2
private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** What the ask form holds; [categoryChoice] is the picked option and [customCategory] the "Others" text. */
data class AskFatwaForm(
    val categoryChoice: String = "",
    val customCategory: String = "",
    val title: String = "",
    val question: String = "",
    val name: String = "",
    val email: String = "",
    val anonymous: Boolean = false,
    /** Signed in: the name and e-mail are the account's, as on the website. */
    val signedIn: Boolean = false,
)

enum class AskFatwaField { Category, Title, Question, Name, Email }

/** The category sent: the custom text when "Others" is picked (the website posts the typed text as `category`). */
internal fun AskFatwaForm.category(): String =
    if (categoryChoice == OTHER_FATWA_CATEGORY) customCategory.trim() else categoryChoice

/** The e-mail the website records for an anonymous question: anonymous@ the site's host. */
internal fun anonymousEmail(websiteUrl: String): String {
    val host = runCatching { URI(websiteUrl).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: "darsequranacademy.com"
    return "anonymous@$host"
}

/** The website's messages (utils/validations.ts fatwaQuestionSchema), by field; empty when the form can be sent. */
internal fun AskFatwaForm.errors(): Map<AskFatwaField, String> = buildMap {
    val category = category()
    if (category.length < CATEGORY_MIN) put(AskFatwaField.Category, "Select a topic.")
    else if (category.length > CATEGORY_MAX) put(AskFatwaField.Category, "Topic is too long.")
    if (title.trim().length < TITLE_MIN) put(AskFatwaField.Title, "Title must be at least 10 characters.")
    if (question.trim().length < QUESTION_MIN) put(AskFatwaField.Question, "Question must be at least 30 characters.")
    if (!anonymous) {
        if (name.trim().length < NAME_MIN) put(AskFatwaField.Name, "Name is required.")
        if (!EMAIL.matches(email.trim())) put(AskFatwaField.Email, "Enter a valid email address.")
    }
}

/** The body of POST /api/v1/fatwa; an anonymous question goes as "Anonymous", as the website records it. */
internal fun AskFatwaForm.toRequest(websiteUrl: String): AskFatwaRequest = AskFatwaRequest(
    title = title.trim(),
    question = question.trim(),
    category = category(),
    askerName = if (anonymous) ANONYMOUS_ASKER else name.trim(),
    askerEmail = if (anonymous) anonymousEmail(websiteUrl) else email.trim(),
)
