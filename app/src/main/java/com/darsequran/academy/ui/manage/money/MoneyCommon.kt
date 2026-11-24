package com.darsequran.academy.ui.manage.money

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.downloadAndOpen
import com.darsequran.academy.ui.manage.common.ActionRunner
import com.darsequran.academy.ui.manage.common.FilterChips
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

/*
 * Pieces shared by the money screens: amounts, list filters kept as one string, the date range bar, summary tiles,
 * a small bar chart, the payment screenshot and the receipt button.
 */

// ------------------------------------------------------------------ amounts

private val inr = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 0; maximumFractionDigits = 0 }
private val inrPaise = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

/** ₹ amount from paise for totals that can pass Int (and be negative): -158475350 → "-₹15,84,753.50". */
fun money(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val abs = kotlin.math.abs(paise)
    val text = if (abs % 100 == 0L) inr.format(abs / 100) else inrPaise.format(abs / 100.0)
    return "$sign₹$text"
}

fun money(paise: Int): String = money(paise.toLong())

// ------------------------------------------------------------------ filters

/**
 * A list's filters travel through `PagedViewModel.filter` as one string ("tab=income&preset=this_month"):
 * these turn it into the query map the endpoint takes and back.
 */
fun decodeFilters(value: String?): Map<String, String> =
    value.orEmpty().split('&').filter { '=' in it }.associate {
        URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
    }

fun encodeFilters(map: Map<String, String?>): String? =
    map.filterValues { !it.isNullOrBlank() }.entries.joinToString("&") {
        URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value!!, "UTF-8")
    }.ifBlank { null }

/** Returns [current] with [changes] applied (a null or blank value removes the key). */
fun withFilters(current: String?, vararg changes: Pair<String, String?>): String? =
    encodeFilters(decodeFilters(current) + changes.toMap())

/**
 * Extra data that comes with a list page (summary totals, tab counts) kept beside the list's view model, so it
 * survives rotation with it. The list's fetch writes [value]; the screen reads it.
 */
class ListExtras : ViewModel() {
    val value = MutableStateFlow<Any?>(null)
}

@Composable
fun rememberListExtras(key: String): ListExtras = viewModel(key = "$key-extras") { ListExtras() }

// ---------------------------------------------------------------- date range

private val presetOptions = listOf(
    FilterOption("this_month", "This month"),
    FilterOption("last_month", "Last month"),
    FilterOption("this_year", "This year"),
    FilterOption("all_time", "All time"),
    FilterOption("custom", "Custom"),
)

/**
 * The finance pages' date filter: preset chips, and From/To dates for a custom range. [range] is what the server
 * resolved; [onChange] gets the new preset/from/to.
 */
@Composable
fun DateRangeBar(range: FinanceRange?, filter: String?, onChange: (String?) -> Unit) {
    val current = decodeFilters(filter)
    val preset = current["preset"] ?: range?.preset ?: "this_month"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChips(presetOptions, preset, onSelect = { p ->
            onChange(
                if (p == "custom") withFilters(filter, "preset" to "custom", "from" to (current["from"] ?: range?.from), "to" to (current["to"] ?: range?.to))
                else withFilters(filter, "preset" to p, "from" to null, "to" to null),
            )
        })
        if (preset == "custom") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormDateField("From", current["from"] ?: range?.from.orEmpty(), { onChange(withFilters(filter, "preset" to "custom", "from" to it)) }, Modifier.weight(1f), clearable = true)
                FormDateField("To", current["to"] ?: range?.to.orEmpty(), { onChange(withFilters(filter, "preset" to "custom", "to" to it)) }, Modifier.weight(1f), clearable = true)
            }
        } else if (range?.from != null && range.to != null) {
            Text(
                "${com.darsequran.academy.ui.common.formatCalendarDate(range.from)} – ${com.darsequran.academy.ui.common.formatCalendarDate(range.to)}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A dropdown filter with an "All …" choice (value ""). */
@Composable
fun FilterDropdown(label: String, allLabel: String, options: List<Pair<String, String>>, filter: String?, key: String, onChange: (String?) -> Unit) {
    FormDropdown(label, listOf("" to allLabel) + options, decodeFilters(filter)[key].orEmpty(), { onChange(withFilters(filter, key to it)) }, placeholder = allLabel)
}

// ----------------------------------------------------------------- summaries

/** A figure on a summary card; [tone] colours the amount (success = income, danger = expense). */
data class Figure(val label: String, val value: String, val tone: Tone? = null, val note: String? = null)

/** Summary cards, two per row. */
@Composable
fun FigureGrid(figures: List<Figure>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        figures.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { FigureTile(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FigureTile(figure: Figure, modifier: Modifier = Modifier) {
    AppCard(modifier.heightIn(min = 88.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Text(figure.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(figure.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = toneColor(figure.tone), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (figure.note != null) Text(figure.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun toneColor(tone: Tone?): Color = when (tone) {
    Tone.Success -> Brand.success
    Tone.Danger -> Brand.danger
    Tone.Warning -> Brand.warning
    Tone.Info -> Brand.info
    else -> MaterialTheme.colorScheme.onSurface
}

/** One bar of [BarChart]. */
data class Bar(val label: String, val value: Long, val color: Color)

/** Horizontal bars scaled to the largest absolute value, each with its label and amount. */
@Composable
fun BarChart(bars: List<Bar>, modifier: Modifier = Modifier) {
    val max = bars.maxOfOrNull { kotlin.math.abs(it.value) }?.takeIf { it > 0 } ?: 1L
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    AppCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            bars.forEach { bar ->
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(bar.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Text(money(bar.value), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                        val radius = CornerRadius(size.height / 2, size.height / 2)
                        drawRoundRect(track, cornerRadius = radius)
                        val fraction = (kotlin.math.abs(bar.value).toFloat() / max).coerceIn(0f, 1f)
                        if (fraction > 0f) drawRoundRect(bar.color, Offset.Zero, Size(size.width * fraction, size.height), radius)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- row helpers

/** Label, a value, on one line inside a card (small). */
@Composable
fun InfoLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

/** The proof a student uploaded: a thumbnail that opens full screen, or a PDF that opens in a viewer. */
@Composable
fun ScreenshotThumb(path: String?, isPdf: Boolean) {
    if (path.isNullOrBlank()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    if (isPdf) {
        OutlinedButton(onClick = { scope.launch { downloadAndOpen(context, path, "payment-proof.pdf") } }) {
            Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("Open proof (PDF)")
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RemoteImage(path, "Payment screenshot", Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).clickable { open = true })
        Spacer(Modifier.width(10.dp))
        Text("Tap to view the screenshot", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                RemoteImage(path, "Payment screenshot", Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                IconButton(onClick = { open = false }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
            }
        }
    }
}

/**
 * What the website's receipt button does: "View receipt" when one exists, else "Generate receipt" (which generates
 * it and opens it).
 */
@Composable
fun ReceiptButton(recordId: String?, generatedAt: String?, runner: ActionRunner, onOpen: (String) -> Unit, onGenerated: () -> Unit = {}) {
    if (recordId == null) {
        Text("No payment record", style = MaterialTheme.typography.labelSmall, color = Brand.warning)
        return
    }
    if (generatedAt != null) {
        OutlinedButton(onClick = { onOpen(recordId) }) { Text("View receipt") }
    } else {
        OutlinedButton(onClick = {
            runner.run({ service<MoneyApi>().generateReceipt(recordId) }, success = "Receipt generated.") { onGenerated(); onOpen(recordId) }
        }, enabled = !runner.busy) { Text("Generate receipt") }
    }
}

/** The form body of a "void with reason" call. */
fun reasonForm(reason: String) = formOf { text("reason", reason) }

/** Downloads an Excel export and opens it. */
fun ActionRunner.openExport(context: Context, name: String, params: Map<String, String>) =
    openFile(name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", { service<MoneyApi>().export(params) }, context)

/** Status chip for fee submissions, book orders, waiver requests. */
fun moneyStatus(status: String?, label: String? = null): Pair<String, Tone> {
    val s = status.orEmpty().lowercase()
    val tone = when (s) {
        "approved", "shipped" -> Tone.Success
        "pending", "pending_verification" -> Tone.Warning
        "rejected", "declined" -> Tone.Danger
        "refunded" -> Tone.Neutral
        else -> Tone.Neutral
    }
    val text = label ?: when (s) {
        "pending_verification" -> "Pending verification"
        else -> com.darsequran.academy.ui.common.titleCase(s.ifBlank { "unknown" })
    }
    return text to tone
}
