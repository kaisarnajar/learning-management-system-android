package com.darsequran.academy.ui.manage.common

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun saveButtons() = compose.onAllNodes(hasText("Save") and hasClickAction())

    @Test
    fun saveIsDisabledUntilTheRecordHasLoaded() {
        var saves = 0
        compose.setContent { FormScreen("Edit student", {}, saving = false, onSave = { saves++ }, canSave = false) { Text("Loading…") } }
        saveButtons().fetchSemanticsNodes().also { assertEquals(2, it.size) } // top bar and bottom
        saveButtons()[0].assertIsNotEnabled()
        saveButtons()[1].assertIsNotEnabled()
        assertEquals(0, saves)
    }

    @Test
    fun saveWorksOnceLoaded() {
        var saves = 0
        compose.setContent { FormScreen("Edit student", {}, saving = false, onSave = { saves++ }, canSave = true) { Text("Name") } }
        saveButtons()[1].assertIsEnabled().performClick()
        assertEquals(1, saves)
    }

    @Test
    fun aFailedLoadShowsRetryInsteadOfTheFormAndSave() {
        var retries = 0
        compose.setContent {
            FormScreen("Edit student", {}, saving = false, onSave = {}, canSave = false, loadError = "No connection.", onRetry = { retries++ }) { Text("Full name") }
        }
        compose.onNodeWithText("No connection.").assertExists()
        assertEquals(0, compose.onAllNodesWithText("Full name").fetchSemanticsNodes().size)
        assertEquals(0, saveButtons().fetchSemanticsNodes().size)
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }
}
