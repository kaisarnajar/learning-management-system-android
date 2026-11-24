package com.darsequran.academy.ui.pay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.remote.PaymentSettings
import com.darsequran.academy.ui.common.rememberProofImagePicker
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentConfirmFieldsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun referenceKeepsOnlyLettersAndDigitsAndBankAddsAMethodChoice() {
        var typed = ""
        compose.setContent {
            var reference by remember { mutableStateOf("") }
            Column {
                PaymentConfirmFields(
                    PaymentSettings(upiId = "academy@upi", bankAccountNumber = "1234567890"),
                    method = "upi", onMethod = {},
                    reference = reference, onReference = { reference = it; typed = it },
                    proof = rememberProofImagePicker(),
                )
            }
        }
        compose.onNodeWithText("Bank transfer").assertExists()
        compose.onNodeWithText("The 12-digit reference shown in your UPI or bank app after paying.").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("4127-8890 1234")
        assertEquals("412788901234", typed)
    }
}
