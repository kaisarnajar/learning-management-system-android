package com.darsequran.academy.ui.account

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.WaiverRequest
import com.darsequran.academy.data.remote.WaiverRequestBody

/** The website's preset reasons (components/profile/WaiverRequestModal.tsx), in its order. */
val WAIVER_REASONS = listOf(
    "Financial hardship",
    "Student / Currently studying",
    "Unemployed / Job seeking",
    "Single parent / Primary caregiver",
    "Orphan / Loss of family breadwinner",
    "Medical expenses / Health condition",
    "Disaster / Emergency relief",
    "Madrasa / Full-time Islamic student",
    "Revert / New Muslim support",
    "Senior citizen / Retired with fixed income",
    "Other",
)

const val WAIVER_REASON_OTHER = "Other"

/** The website's limit on the details box (maxLength on the textarea). */
const val WAIVER_DETAILS_MAX = 900

/** Which fee a waiver is for; the website offers these two, course fee first selected. */
enum class WaiverFee(val value: String, val label: String) {
    Enrollment("enrollment", "Enrollment Fee"),
    Course("course", "Course Fee"),
}

val DEFAULT_WAIVER_FEE = WaiverFee.Course

private val WAIVER_OPEN_STATUSES = setOf("PUBLISHED", "ONGOING")

/** Courses a waiver can be asked for: those open for enrollment, by title, as the website lists them. */
fun waiverCourseOptions(courses: List<Course>): List<Course> =
    courses.filter { it.status in WAIVER_OPEN_STATUSES }
        .distinctBy { it.id }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

/** The reason the website sends: the preset, then " - details" when details were typed. */
fun waiverReason(category: String, details: String): String {
    val extra = details.trim()
    return if (extra.isEmpty()) category else "$category - $extra"
}

/** The details box is required only for "Other", as on the website. */
fun waiverFormReady(courseId: String, category: String, details: String): Boolean =
    courseId.isNotBlank() && (category != WAIVER_REASON_OTHER || details.isNotBlank())

fun waiverRequestBody(courseId: String, fee: WaiverFee, category: String, details: String) =
    WaiverRequestBody(courseId = courseId, feeType = fee.value, reason = waiverReason(category, details))

/** The fee-type column of the website's table (services/waiver-requests.ts waiverFeeTypeLabel). */
fun waiverFeeLabel(feeType: String?): String = when (feeType?.uppercase()) {
    "ENROLLMENT" -> "Enrollment Fee"
    "COURSE" -> "Course Fee"
    else -> "Both"
}

/** The "Assigned Coupon" column: every issued code and the discount, or null for the website's dash. */
fun waiverCouponText(request: WaiverRequest): String? {
    val coupon = request.coupon ?: return null
    val codes = request.couponCodes.ifEmpty { listOf(coupon.code) }
    return "${codes.joinToString("  ")} (${coupon.percentage}% OFF)"
}
