package com.darsequran.academy.ui.manage.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.ui.manage.content.answerProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeaturedSwitchTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun slots() {
        assertTrue(canFeature(currentlyFeatured = false, used = 3, max = 4))
        assertFalse(canFeature(currentlyFeatured = false, used = 4, max = 4))
        assertTrue("an item already featured keeps its slot", canFeature(currentlyFeatured = true, used = 4, max = 4))
    }

    @Test
    fun aFullHomepageRefusesANewItemButLetsOneGo() {
        var featured by mutableStateOf(false)
        compose.setContent { FeaturedSwitch(featured, { featured = it }, currentlyFeatured = false, used = 4, max = 4, "Fatwa", "Fatwa page", "answers") }
        compose.onNodeWithText("Show on homepage").performClick()
        assertFalse(featured)
        compose.onNodeWithText("There are already 4 featured answers. Remove one from the homepage to feature this one.").assertExists()
    }

    @Test
    fun aFreeSlotCanBeUsed() {
        var featured by mutableStateOf(false)
        compose.setContent { FeaturedSwitch(featured, { featured = it }, currentlyFeatured = false, used = 1, max = 4, "Fatwa", "Fatwa page", "answers", condition = "when this answer is published") }
        compose.onNodeWithText("Show on homepage").performClick()
        assertTrue(featured)
        compose.onNodeWithText("Featured in the Fatwa section when this answer is published (up to 4; 1/4 slots used). All published answers still appear on the Fatwa page.").assertExists()
    }

    @Test
    fun fatwaAnswersNeedTwentyVisibleCharacters() {
        assertEquals("Write the answer first.", answerProblem("<p>&nbsp;</p>"))
        assertEquals("The answer must be at least 20 characters.", answerProblem("<p><b>Yes.</b></p>"))
        assertNull(answerProblem("<p>Yes, it is permissible in this case.</p>"))
    }
}
