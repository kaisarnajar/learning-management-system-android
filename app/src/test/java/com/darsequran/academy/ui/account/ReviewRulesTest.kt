package com.darsequran.academy.ui.account

import com.darsequran.academy.data.remote.Review
import com.darsequran.academy.data.remote.ReviewRequest
import com.darsequran.academy.ui.common.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRulesTest {
    private val quote = "Twenty characters or more, surely."

    @Test
    fun theRatingStartsUnchosenAndMustBeChosen() {
        assertEquals(0, ReviewDraft().rating)
        assertEquals(REVIEW_RATING_ERROR, reviewErrors(ReviewDraft(quote = quote))[ReviewField.Rating])
        assertTrue(reviewErrors(ReviewDraft(rating = 5, quote = quote)).isEmpty())
    }

    @Test
    fun theReviewNeedsTwentyCharactersAfterTrimming() {
        val errors = reviewErrors(ReviewDraft(rating = 3, quote = "   short but padded         "))
        assertEquals("Your review must be at least 20 characters.", errors[ReviewField.Quote])
    }

    @Test
    fun theLocationStartsAsTheAddressCappedAtTheWebsitesLimit() {
        assertEquals("Srinagar", newReviewDraft("Srinagar").location)
        assertEquals(REVIEW_FIELD_MAX, newReviewDraft("x".repeat(200)).location.length)
        assertEquals("", newReviewDraft(null).location)
    }

    @Test
    fun blankOptionalFieldsAreLeftOut() = assertEquals(
        ReviewRequest(quote, 4, null, "Srinagar"),
        reviewRequest(ReviewDraft(4, "  $quote ", "  ", " Srinagar ")),
    )

    @Test
    fun badgesReadAsOnTheWebsite() {
        assertEquals("Pending approval" to Tone.Warning, reviewStatusLabel(Review("r", "q", status = "PENDING")))
        assertEquals("Approved" to Tone.Success, reviewStatusLabel(Review("r", "q", status = "APPROVED")))
        val featured = Review("r", "q", status = "APPROVED", featuredOnHomepage = true)
        assertEquals("On homepage" to Tone.Success, reviewStatusLabel(featured))
        assertEquals("Rejected" to Tone.Danger, reviewStatusLabel(Review("r", "q", status = "REJECTED")))
    }

    @Test
    fun theMetaLineJoinsCourseAndLocation() {
        assertEquals("Tajweed · Srinagar", reviewMeta(Review("r", "q", course = "Tajweed", location = "Srinagar")))
        assertEquals("Srinagar", reviewMeta(Review("r", "q", course = "", location = "Srinagar")))
        assertNull(reviewMeta(Review("r", "q")))
    }
}
