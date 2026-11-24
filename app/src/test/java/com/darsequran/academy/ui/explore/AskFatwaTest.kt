package com.darsequran.academy.ui.explore

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AskFatwaTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theWebsitesCategoriesAreOfferedBeforeTheConfigLoads() {
        compose.setContent { AskFatwaScreen({}, null, null, categories = emptyList()) }
        compose.onNodeWithText("Select a topic").performClick()    // open the menu
        compose.onNodeWithText("Tajweed").performClick()           // pick the menu item
        compose.onNodeWithText("Tajweed").assertExists()
    }

    @Test
    fun pickingOthersAsksForTheCategory() {
        var categories by mutableStateOf(emptyList<String>())
        compose.setContent { AskFatwaScreen({}, null, null, categories) }
        categories = listOf("Salah", "Others")
        compose.waitForIdle()
        compose.onNodeWithText("Select a topic").performClick()
        compose.onNodeWithText("Others").performClick()
        compose.onNodeWithText("Specify Category").assertExists()
    }

    @Test
    fun aSignedInUserAsksUnderTheirAccount() {
        compose.setContent { AskFatwaScreen({}, "Amina", "amina@example.com", emptyList()) }
        compose.onNodeWithText("Submitting as Amina (amina@example.com). We will email you when answered.")
            .assertExists()
        compose.onNodeWithText("Your name").assertDoesNotExist()
        compose.onNodeWithText("Send Question Anonymously").performScrollTo().performClick()
        compose.onNodeWithText("Submitting as", substring = true).assertDoesNotExist()
    }
}
