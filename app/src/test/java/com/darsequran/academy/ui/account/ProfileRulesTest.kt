package com.darsequran.academy.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRulesTest {
    @Test
    fun aPhotoIsTakenOnlyOnceTheGenderIsMale() {
        assertTrue(photoAllowed("MALE"))
        assertFalse(photoAllowed("FEMALE"))
        assertFalse("the website asks for the gender first", photoAllowed(null))
        assertFalse(photoAllowed(""))
    }

    @Test
    fun thePhotoHintIsTheWebsites() {
        assertEquals("Profile Photo (Optional)", photoHint("MALE"))
        assertEquals("Profile Photo (Disabled for Female)", photoHint("FEMALE"))
        assertEquals("Profile Photo (Select gender first)", photoHint(null))
    }

    @Test
    fun theCompleteStepFollowsOnlyTheSaveThatCompletedTheProfile() {
        assertTrue(profileJustCompleted(wasComplete = false, isComplete = true))
        assertFalse(profileJustCompleted(wasComplete = true, isComplete = true))
        assertFalse(profileJustCompleted(wasComplete = false, isComplete = false))
    }
}
