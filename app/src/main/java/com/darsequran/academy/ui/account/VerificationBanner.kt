package com.darsequran.academy.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darsequran.academy.ui.theme.Brand

/** Shown to a signed-in user whose e-mail is not verified, as the website's profile pages do. */
@Composable
fun VerificationBanner(state: ResendState, onResend: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Brand.warningContainer, RoundedCornerShape(12.dp)).padding(14.dp),
    ) {
        Icon(Icons.Rounded.Warning, null, tint = Brand.warning)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Email Verification Required", style = MaterialTheme.typography.titleSmall, color = Brand.warning)
            Text(
                "Please verify your email address to access all features like enrolling in courses and making " +
                    "purchases.",
                style = MaterialTheme.typography.bodyMedium,
                color = Brand.warning,
            )
            TextButton(onResend, enabled = !state.sending && !state.sent) { Text(resendLabel(state)) }
            state.message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.sent) Brand.success else Brand.danger,
                )
            }
        }
    }
}
