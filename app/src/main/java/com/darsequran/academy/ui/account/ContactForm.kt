package com.darsequran.academy.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.SubmitResult
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rememberContainer
import com.darsequran.academy.ui.common.rememberSubmitter
import com.darsequran.academy.ui.theme.Brand

/** The website's contact form (components/contact/ContactForm.tsx); [user] is the signed-in account, if any. */
@Composable
fun ContactForm(user: SessionUser?) {
    val container = rememberContainer()
    val submitter = rememberSubmitter("contact")
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(submitter.result) { if (submitter.result?.ok == true) message = "" }
    Text("Send a Message", style = MaterialTheme.typography.titleLarge)
    Text(
        "We would love to hear from you. Please fill out the form below and our team will get back to " +
            "you within 2-3 business days.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    submitter.result?.let { r ->
        val text = if (r.ok) "$CONTACT_SENT_TITLE. ${r.message}" else r.message
        Banner(text, if (r.ok) Tone.Success else Tone.Danger)
    }
    // Signed in, the website sends as the account and shows who it is from instead of the two fields.
    val draft = ContactDraft(user?.name ?: name, user?.email ?: email, phone, message)
    if (user == null) {
        ContactField(name, "Full name *", KeyboardType.Text) { name = it.take(CONTACT_NAME_MAX) }
        ContactField(email, "Email *", KeyboardType.Email) { email = it.trim() }
    } else {
        Text(
            "Submitting as ${draft.name} (${draft.email}). We will email you when the academy replies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ContactField(phone, "Phone / WhatsApp (optional)", KeyboardType.Phone, "e.g. +919622966911") {
        phone = it.take(CONTACT_PHONE_MAX)
    }
    OutlinedTextField(
        message,
        { message = it.take(CONTACT_MESSAGE_MAX) },
        Modifier.fillMaxWidth().height(180.dp),
        label = { Text("Your message *") },
        placeholder = {
            Text("Tell us about your query — course enrollment, schedules, fees, or anything else…")
        },
        supportingText = { Text("${message.length}/$CONTACT_MESSAGE_MAX") },
    )
    val error = contactError(draft)
    if (error != null && message.isNotEmpty()) {
        Text(error, style = MaterialTheme.typography.bodySmall, color = Brand.danger)
    }
    PrimaryButton("Send message", {
        val request = contactRequest(draft)
        submitter.submit {
            when (val r = container.repo.contact(request)) {
                is ApiResult.Ok -> SubmitResult(CONTACT_SENT, ok = true)
                is ApiResult.Err -> SubmitResult(r.message, ok = false)
            }
        }
    }, enabled = error == null, loading = submitter.busy)
}

@Composable
private fun ContactField(
    value: String,
    label: String,
    keyboard: KeyboardType,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}
