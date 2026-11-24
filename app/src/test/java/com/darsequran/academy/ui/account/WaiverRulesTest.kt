package com.darsequran.academy.ui.account

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.WaiverRequest
import com.darsequran.academy.data.remote.WaiverRequestBody
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaiverRulesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun theReasonsAreTheWebsitesInItsOrder() {
        assertEquals(11, WAIVER_REASONS.size)
        assertEquals("Financial hardship", WAIVER_REASONS.first())
        assertEquals(WAIVER_REASON_OTHER, WAIVER_REASONS.last())
    }

    @Test
    fun onlyEnrollmentAndCourseFeesAreOfferedWithTheCourseFeeFirstChosen() {
        assertEquals(listOf("enrollment", "course"), WaiverFee.entries.map { it.value })
        assertEquals(WaiverFee.Course, DEFAULT_WAIVER_FEE)
    }

    @Test
    fun theBodyCarriesThePresetAndTheDetailsAsTheWebsiteJoinsThem() {
        assertEquals(
            WaiverRequestBody("c1", "course", "Financial hardship - Lost my job"),
            waiverRequestBody("c1", WaiverFee.Course, "Financial hardship", "  Lost my job "),
        )
        assertEquals("Student / Currently studying", waiverReason("Student / Currently studying", "   "))
    }

    @Test
    fun detailsAreRequiredOnlyForOtherAndACourseMustBeChosen() {
        assertTrue(waiverFormReady("c1", "Financial hardship", ""))
        assertFalse(waiverFormReady("c1", WAIVER_REASON_OTHER, " "))
        assertTrue(waiverFormReady("c1", WAIVER_REASON_OTHER, "My reason"))
        assertFalse(waiverFormReady("", "Financial hardship", "x"))
    }

    @Test
    fun onlyCoursesOpenForEnrollmentAreOfferedByTitle() {
        val courses = listOf(
            Course("1", "tajweed", status = "ONGOING"),
            Course("2", "Arabic", status = "PUBLISHED"),
            Course("3", "Fiqh", status = "COMPLETED"),
            Course("4", "Hadith", status = "ON_HOLD"),
            Course("2", "Arabic", status = "PUBLISHED"),
        )
        assertEquals(listOf("Arabic", "tajweed"), waiverCourseOptions(courses).map { it.title })
    }

    @Test
    fun feeTypesReadAsOnTheWebsite() {
        assertEquals("Enrollment Fee", waiverFeeLabel("ENROLLMENT"))
        assertEquals("Course Fee", waiverFeeLabel("COURSE"))
        assertEquals("Both", waiverFeeLabel("BOTH"))
    }

    @Test
    fun theAssignedCouponListsEveryIssuedCode() {
        val both = json.decodeFromString(
            WaiverRequest.serializer(),
            """{"id":"w","courseId":"c","status":"APPROVED","feeType":"BOTH",
               "coupon":{"id":"x","code":"WAIVE1","percentage":100},"couponCodes":["WAIVE1","WAIVE1-C"]}""",
        )
        assertEquals("WAIVE1  WAIVE1-C (100% OFF)", waiverCouponText(both))
        val pending = json.decodeFromString(WaiverRequest.serializer(), """{"id":"w","courseId":"c","coupon":null}""")
        assertNull(waiverCouponText(pending))
    }
}
