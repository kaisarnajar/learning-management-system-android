package com.darsequran.academy.ui.explore

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AskFatwaTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pickingTheFallbackCategoryDoesNotCrashWhenCategoriesHaventLoaded() {
        compose.setContent { AskFatwaScreen({}, null, null, categories = emptyList()) }
        compose.onNodeWithText("General").performClick()            // open the menu
        compose.onAllNodesWithText("General")[1].performClick()     // pick the menu item
        compose.onNodeWithText("General").assertExists()
    }

    @Test
    fun theCategoryFollowsTheRealListWhenItArrives() {
        var categories by mutableStateOf(emptyList<String>())
        compose.setContent { AskFatwaScreen({}, null, null, categories) }
        compose.onNodeWithText("General").assertExists()
        categories = listOf("Salah", "Zakat")
        compose.waitForIdle()
        compose.onNodeWithText("Salah").assertExists()
    }
}
