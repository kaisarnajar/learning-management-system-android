package com.darsequran.academy.ui.common

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.testing.waitUntilSettled
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.io.File

/** An attached screenshot survives recreation (rotation). */
@RunWith(AndroidJUnit4::class)
class ProofPickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theAttachmentSurvivesRecreation() {
        val activity = compose.activity
        val file = File(activity.cacheDir, "proof.png").apply {
            outputStream().use { Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        lateinit var picker: ProofImagePicker
        val restore = StateRestorationTester(compose)
        restore.setContent {
            picker = rememberProofImagePicker()
            Button(onClick = { picker.pick() }) { Text(if (picker.image == null) "Attach" else "Attached") }
        }
        compose.onNodeWithText("Attach").performClick()
        // Answer the photo picker the launcher started with our file.
        val shadow = shadowOf(activity)
        val started = shadow.nextStartedActivityForResult
        shadow.receiveResult(started.intent, android.app.Activity.RESULT_OK, android.content.Intent().setData(Uri.fromFile(file)))
        compose.waitUntilSettled { picker.image != null }

        restore.emulateSavedInstanceStateRestore()
        compose.waitUntilSettled { picker.image != null }
        assertNotNull(picker.image)
        compose.onNodeWithText("Attached").assertExists()
    }
}
