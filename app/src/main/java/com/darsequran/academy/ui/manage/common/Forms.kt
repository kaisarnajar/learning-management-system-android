package com.darsequran.academy.ui.manage.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darsequran.academy.data.staff.PickedFile
import com.darsequran.academy.data.staff.readPicked
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.common.academyToday
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Form fields for admin and teacher screens. Values are kept as the strings the website's forms send
 * (dates as YYYY-MM-DD, amounts as typed), so a screen can hand them straight to `formOf { text(...) }`.
 */

/**
 * A form screen: scrolling fields, an error banner on top, and a Save button at the bottom (and in the top bar).
 */
@Composable
fun FormScreen(
    title: String,
    onBack: () -> Unit,
    saving: Boolean,
    onSave: () -> Unit,
    snackbar: SnackbarHostState? = null,
    saveLabel: String = "Save",
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ManageScaffold(title, onBack, snackbar, actions = {
        TextButton(onClick = onSave, enabled = !saving) { Text(saveLabel) }
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Notice(error)
            content()
            Spacer(Modifier.height(4.dp))
            PrimaryButton(saveLabel, onSave, loading = saving)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A titled group of fields inside a form. */
@Composable
fun FormSection(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
fun FormTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLength: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (maxLength != null) it.take(maxLength) else it) },
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Whole numbers only. */
@Composable
fun FormNumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, supporting: String? = null) =
    FormTextField(label, value, { v -> onChange(v.filter(Char::isDigit)) }, modifier, supporting, keyboardType = KeyboardType.Number)

/** Rupees, with up to two decimals (what the website's amount inputs take). */
@Composable
fun FormMoneyField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, supporting: String? = null) =
    FormTextField(
        label, value,
        { v -> val clean = v.filter { it.isDigit() || it == '.' }; if (clean.count { it == '.' } <= 1 && clean.substringAfter('.', "").length <= 2) onChange(clean) },
        modifier, supporting, keyboardType = KeyboardType.Decimal,
    )

/** "₹ 1,250.50" typed → paise, or null when blank/invalid. */
fun rupeesToPaise(value: String): Int? = value.trim().toBigDecimalOrNull()?.movePointRight(2)?.toInt()

/** Paise → the text a money field shows ("1250" or "1250.5"). */
fun paiseToRupeesText(paise: Int?): String = paise?.let { java.math.BigDecimal(it).movePointLeft(2).stripTrailingZeros().toPlainString() } ?: ""

/** On/off with an explanation, sent as a checkbox ("on" or absent). */
@Composable
fun FormSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit, description: String? = null) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked, onChange)
    }
}

/** A checkbox row (for confirmations such as "I understand this cannot be undone"). */
@Composable
fun FormCheckbox(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, null, Modifier.padding(end = 8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Pick one of [options] (value to label). An empty value shows [placeholder]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDropdown(
    label: String,
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose…",
    supporting: String? = null,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { if (enabled) open = it }, modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second ?: if (value.isBlank()) "" else value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            supportingText = supporting?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(open, { open = false }) {
            options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onChange(v); open = false }) }
        }
    }
}

/** Several of [options]; [values] are the chosen option values. */
@Composable
fun FormMultiSelect(label: String, options: List<Pair<String, String>>, values: Set<String>, onChange: (Set<String>) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        options.forEach { (v, l) -> FormCheckbox(l, v in values) { on -> onChange(if (on) values + v else values - v) } }
    }
}

private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
private val showDate = DateTimeFormatter.ofPattern("d MMM yyyy")

/** A date as YYYY-MM-DD ("" when not set). Accepts full ISO timestamps from the API too. */
fun dateOnly(value: String?): String = value?.take(10)?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess } ?: ""

/** Pick a date; [value] and the result are YYYY-MM-DD. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, supporting: String? = null, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = date?.format(showDate) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = supporting?.let { { Text(it) } },
            trailingIcon = {
                if (clearable && date != null) IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, "Clear") }
                else Icon(Icons.Rounded.CalendarMonth, null)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // The read-only field does not take clicks, so a transparent layer opens the picker.
        Box(Modifier.matchParentSize().padding(end = if (clearable && date != null) 48.dp else 0.dp).clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: academyToday()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(isoDate)) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

/** Pick a date and time; [value] and the result are local "YYYY-MM-DDTHH:mm" (an HTML datetime-local value). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDateTimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, supporting: String? = null) {
    val parsed = runCatching { LocalDateTime.parse(value.take(16)) }.getOrNull()
    var pickTime by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf(parsed?.toLocalDate()?.format(isoDate) ?: "") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormDateField(label, day, { day = it; pickTime = true }, supporting = supporting)
        if (parsed != null) TextButton(onClick = { pickTime = true }) { Text("Time: " + parsed.toLocalTime().toString().take(5)) }
    }
    if (pickTime && day.isNotBlank()) {
        val state = rememberTimePickerState(parsed?.hour ?: 9, parsed?.minute ?: 0)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = { TextButton(onClick = { onChange("${day}T%02d:%02d".format(state.hour, state.minute)); pickTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }
}

/** ISO timestamp from the API → local "YYYY-MM-DDTHH:mm" for [FormDateTimeField]. */
fun localDateTimeValue(iso: String?): String =
    iso?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDateTime().toString().take(16) }.getOrNull() } ?: ""

/**
 * An image: shows the picked one, else the current one from the server ([currentUrl]); "Choose" opens the photo
 * picker, "Remove" clears the pick (and, with [onRemoveCurrent], asks to remove the saved image).
 */
@Composable
fun FormImageField(
    label: String,
    currentUrl: String?,
    picked: PickedFile?,
    onPick: (PickedFile?) -> Unit,
    supporting: String? = null,
    onRemoveCurrent: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { onPick(context.readPicked(uri)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier.fillMaxWidth().heightIn(min = 140.dp).height(180.dp).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, Brand.outlineSoft, MaterialTheme.shapes.large)
                .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            contentAlignment = Alignment.Center,
        ) {
            when {
                picked != null -> AsyncImage(picked.bytes, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                !currentUrl.isNullOrBlank() -> RemoteImage(currentUrl, null, Modifier.fillMaxSize())
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap to choose an image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(if (picked != null || !currentUrl.isNullOrBlank()) "Change" else "Choose")
            }
            if (picked != null) TextButton(onClick = { onPick(null) }) { Text("Undo") }
            else if (!currentUrl.isNullOrBlank() && onRemoveCurrent != null) TextButton(onClick = onRemoveCurrent) { Text("Remove", color = MaterialTheme.colorScheme.error) }
        }
        if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A document (PDF, etc.) from the phone's files. [currentName] describes what is saved already. */
@Composable
fun FormFileField(
    label: String,
    mimeTypes: Array<String>,
    picked: PickedFile?,
    onPick: (PickedFile?) -> Unit,
    currentName: String? = null,
    supporting: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch { onPick(context.readPicked(uri)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).border(1.dp, Brand.outlineSoft, MaterialTheme.shapes.medium)
                .clickable { launcher.launch(mimeTypes) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(picked?.name ?: currentName ?: "Choose a file", style = MaterialTheme.typography.bodyMedium)
                if (picked != null) Text("%.1f KB, will be uploaded".format(picked.bytes.size / 1024.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (picked != null) IconButton(onClick = { onPick(null) }) { Icon(Icons.Rounded.Close, "Remove") }
        }
        if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Rich text, edited as on the website (bold, italic, underline, strike, heading, lists, links, alignment) and
 * read/written as HTML. [initialHtml] is loaded once; every edit reports the new HTML through [onChange].
 */
@Composable
fun RichTextField(label: String, initialHtml: String, onChange: (String) -> Unit, minHeight: Int = 220) {
    val state = rememberRichTextState()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!loaded) { state.setHtml(initialHtml); loaded = true } }
    LaunchedEffect(state.annotatedString) { if (loaded) onChange(state.toHtml()) }
    var linkDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
                .horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val span = state.currentSpanStyle
            ToolButton(Icons.Rounded.FormatBold, "Bold", span.fontWeight == FontWeight.Bold) { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            ToolButton(Icons.Rounded.FormatItalic, "Italic", span.fontStyle == FontStyle.Italic) { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            ToolButton(Icons.Rounded.FormatUnderlined, "Underline", span.textDecoration?.contains(TextDecoration.Underline) == true) { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            ToolButton(Icons.Rounded.FormatStrikethrough, "Strikethrough", span.textDecoration?.contains(TextDecoration.LineThrough) == true) { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
            ToolButton(Icons.Rounded.Title, "Heading", span.fontSize == 22.sp) { state.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)) }
            ToolButton(Icons.AutoMirrored.Rounded.FormatListBulleted, "Bulleted list", state.isUnorderedList) { state.toggleUnorderedList() }
            ToolButton(Icons.Rounded.FormatListNumbered, "Numbered list", state.isOrderedList) { state.toggleOrderedList() }
            ToolButton(Icons.AutoMirrored.Rounded.FormatAlignLeft, "Align left", state.currentParagraphStyle.textAlign == TextAlign.Left) { state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Left)) }
            ToolButton(Icons.Rounded.FormatAlignCenter, "Centre", state.currentParagraphStyle.textAlign == TextAlign.Center) { state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center)) }
            ToolButton(Icons.AutoMirrored.Rounded.FormatAlignRight, "Align right", state.currentParagraphStyle.textAlign == TextAlign.Right) { state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Right)) }
            if (state.isLink) ToolButton(Icons.Rounded.LinkOff, "Remove link", true) { state.removeLink() }
            else ToolButton(Icons.Rounded.Link, "Link", false) { linkDialog = true }
        }
        OutlinedRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp))
    }
    if (linkDialog) {
        TextInputDialog(
            title = "Add a link", label = "https://…", confirmLabel = "Add", singleLine = true, required = true,
            message = "Select the words to link first, or the address is added as the link text.",
            onConfirm = { url ->
                val href = if (url.startsWith("http")) url else "https://$url"
                if (state.selection.collapsed) state.addLink(href, href) else state.addLinkToSelection(href)
            },
            onDismiss = { linkDialog = false },
        )
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    IconToggleButton(checked = active, onCheckedChange = { onClick() }, modifier = Modifier.size(40.dp)) {
        Icon(icon, label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Padding for content placed directly in a form column. */
val FormPadding = PaddingValues(20.dp)
