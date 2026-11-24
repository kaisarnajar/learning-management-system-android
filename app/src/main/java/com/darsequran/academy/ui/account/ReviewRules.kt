package com.darsequran.academy.ui.account

import com.darsequran.academy.data.remote.Review
import com.darsequran.academy.data.remote.ReviewRequest
import com.darsequran.academy.ui.common.Tone

/** The website's studentReviewSchema limits. */
const val REVIEW_QUOTE_MIN = 20
const val REVIEW_QUOTE_MAX = 1000
const val REVIEW_FIELD_MAX = 120
const val REVIEW_RATING_MAX = 5

/** Featured on the homepage at most this many at a time (HOMEPAGE_FEATURED_REVIEWS_MAX). */
const val HOMEPAGE_FEATURED_REVIEWS_MAX = 4

const val REVIEW_RATING_ERROR = "Select a star rating from 1 to 5."

/** The review form's fields; the rating starts unchosen (0), as on the website. */
data class ReviewDraft(val rating: Int = 0, val quote: String = "", val course: String = "", val location: String = "")

/** Course and location are capped at [REVIEW_FIELD_MAX] as they are typed, as the website's inputs are. */
enum class ReviewField { Rating, Quote }

/** The website's messages for each invalid field (studentReviewSchema); empty when the review can be sent. */
fun reviewErrors(draft: ReviewDraft): Map<ReviewField, String> = buildMap {
    val quote = draft.quote.trim()
    if (quote.length < REVIEW_QUOTE_MIN) put(ReviewField.Quote, "Your review must be at least 20 characters.")
    if (quote.length > REVIEW_QUOTE_MAX) put(ReviewField.Quote, "Review must be at most 1000 characters.")
    if (draft.rating !in 1..REVIEW_RATING_MAX) put(ReviewField.Rating, REVIEW_RATING_ERROR)
}

fun reviewRequest(draft: ReviewDraft) = ReviewRequest(
    quote = draft.quote.trim(),
    rating = draft.rating,
    course = draft.course.trim().ifBlank { null },
    location = draft.location.trim().ifBlank { null },
)

/** A fresh form: the location starts as the student's address, as the website pre-fills it. */
fun newReviewDraft(address: String?) = ReviewDraft(location = address.orEmpty().take(REVIEW_FIELD_MAX))

/** The website's badge (services/student-reviews.ts reviewStatusLabel, plus "On homepage"). */
fun reviewStatusLabel(review: Review): Pair<String, Tone> = when (review.status?.uppercase()) {
    "APPROVED" -> (if (review.featuredOnHomepage) "On homepage" else "Approved") to Tone.Success
    "REJECTED" -> "Rejected" to Tone.Danger
    else -> "Pending approval" to Tone.Warning
}

/** The "course · location" line under a submitted review, or null when neither was given. */
fun reviewMeta(review: Review): String? =
    listOfNotNull(review.course?.ifBlank { null }, review.location?.ifBlank { null })
        .joinToString(" · ")
        .ifEmpty { null }
