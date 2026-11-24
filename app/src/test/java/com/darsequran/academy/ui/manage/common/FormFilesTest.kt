package com.darsequran.academy.ui.manage.common

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.staff.PickedFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Chosen files and "remove" marks of an admin form survive recreation (rotation). */
@RunWith(AndroidJUnit4::class)
class FormFilesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun picksAndRemovalMarksSurviveRecreation() {
        val restore = StateRestorationTester(compose)
        restore.setContent {
            val files = rememberFormFiles("blog-files-admin-new")
            var picked by files::picked
            var removed by files::removed
            Button(onClick = { picked = picked + PickedFile("cover.jpg", "image/jpeg", byteArrayOf(1, 2)); removed = removed + "img-1" }) {
                Text("${picked.size} picked, ${removed.size} removed")
            }
        }
        compose.onNodeWithText("0 picked, 0 removed").performClick()
        compose.onNodeWithText("1 picked, 1 removed").assertExists()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("1 picked, 1 removed").assertExists()
    }
}
