package com.darsequran.academy.ui.account

/** The website takes a profile photo only from men (ProfileForm.tsx disables it unless gender is MALE). */
fun photoAllowed(gender: String?): Boolean = gender == "MALE"

/** The website's hint next to "Profile Photo". */
fun photoHint(gender: String?): String = when (gender) {
    "FEMALE" -> "Profile Photo (Disabled for Female)"
    "MALE" -> "Profile Photo (Optional)"
    else -> "Profile Photo (Select gender first)"
}

/** True when this save is the one that completed the profile, so the "profile is complete" step is due. */
fun profileJustCompleted(wasComplete: Boolean, isComplete: Boolean): Boolean = !wasComplete && isComplete

const val PROFILE_COMPLETE_TITLE = "Your profile is complete"
const val PROFILE_COMPLETE_TEXT =
    "You can browse courses and request enrollment. Any enrollment or monthly fees are shown on each course."
const val PROFILE_INCOMPLETE_TITLE = "Complete your profile to enroll in courses"
const val PROFILE_INCOMPLETE_TEXT =
    "The academy needs your full details for course registration. Fill in all fields below, then return to " +
        "the course page to request enrollment."
