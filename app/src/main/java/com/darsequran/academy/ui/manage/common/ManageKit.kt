package com.darsequran.academy.ui.manage.common

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.AppContainer
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.apiCall
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.PagedViewModel
import com.darsequran.academy.ui.common.TabTopBar
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.cacheDocument
import com.darsequran.academy.ui.common.openDocument
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/*
 * Building blocks of the admin and teacher screens. A typical section is:
 *   list   — ManageListScreen + rememberPaged { service<XApi>().list(page, q, filter) }
 *   detail — ManageScaffold + rememberLoader { service<XApi>().get(id) } + DetailCard/Field + ActionRunner
 *   form   — FormScreen (Forms.kt) + fields, saving through ActionRunner
 */

// ------------------------------------------------------------------ data

/** Loads one thing for a screen; `reload()` after a change. */
class LoaderViewModel<T>(private val fetch: suspend () -> ApiResult<T>) : ViewModel() {
    private val _state = MutableStateFlow<Load<T>>(Load.Loading)
    val state: StateFlow<Load<T>> = _state.asStateFlow()

    init { reload() }

    fun reload(showLoading: Boolean = false) = viewModelScope.launch {
        if (showLoading || _state.value !is Load.Ready) _state.value = Load.Loading
        _state.value = fetch().toLoad()
    }
}

/** A [LoaderViewModel] for this screen. [key] must differ per record (e.g. "student-$id"). */
@Composable
fun <T> rememberLoader(key: String, fetch: suspend AppContainer.() -> T): LoaderViewModel<T> =
    viewModel(key = key, factory = vmFactory { c -> LoaderViewModel { apiCall { c.fetch() } } })

/** A searchable, filterable, paged list. `fetch(page, query, filter)` returns the API's `Paged` body. */
@Composable
fun <T> rememberPaged(key: String, fetch: suspend AppContainer.(page: Int, query: String, filter: String?) -> Paged<T>): PagedViewModel<T> =
    viewModel(key = key, factory = vmFactory { c -> PagedViewModel { page, q, f -> apiCall { c.fetch(page, q, f) } } })

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember { (context.applicationContext as AcademyApp).container }
}

// ------------------------------------------------------------- actions

/**
 * Runs writes from a screen: one at a time ([busy]), errors and confirmations as snackbars.
 * `runner.run({ service<XApi>().delete(id) }, success = "Deleted.") { onBack() }`
 *
 * A ViewModel, so a write keeps running, and its message still shows, when the phone rotates or the screen is
 * recreated (it used to be cancelled, leaving the admin unsure whether it happened). [busy] is set before the write
 * is launched, so a second tap in the same frame is ignored rather than sending the write twice.
 */
class ActionRunner(private val container: AppContainer) : ViewModel() {
    val snackbar = SnackbarHostState()

    var busy by mutableStateOf(false)
        private set

    fun <T> run(block: suspend AppContainer.() -> T, success: String? = null, onSuccess: (T) -> Unit = {}) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val result = try { apiCall { container.block() } } finally { busy = false }
            when (result) {
                is ApiResult.Ok -> { onSuccess(result.value); success?.let { message(it) } }
                is ApiResult.Err -> message(result.message)
            }
        }
    }

    fun message(text: String) { viewModelScope.launch { snackbar.showSnackbar(text) } }

    /** Downloads a PDF (or other file) and opens it in the phone's viewer. */
    fun openFile(fileName: String, mime: String = "application/pdf", fetch: suspend AppContainer.() -> ResponseBody, context: android.content.Context) {
        run({ withContext(Dispatchers.IO) { fetch().use { body -> cacheDocument(context, fileName, body.byteStream()) } } }) { file -> openDocument(context, file, mime) }
    }
}

/** This call site's [ActionRunner], kept per back-stack entry (several on one screen stay separate). */
@Composable
fun rememberActionRunner(): ActionRunner {
    val key = currentCompositeKeyHash
    return viewModel(key = "action-runner-$key", factory = vmFactory { c -> ActionRunner(c) })
}

// -------------------------------------------------------------- layout

/**
 * The frame of every admin and teacher screen: a plain top bar (back arrow on pushed screens, centred title on
 * tabs), a snackbar host and an optional floating action.
 */
@Composable
fun ManageScaffold(
    title: String,
    onBack: (() -> Unit)?,
    snackbar: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = { if (onBack != null) BackTopBar(title, onBack, { Row { actions() } }) else TabTopBar(title) { Row { actions() } } },
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        floatingActionButton = { fab?.invoke() },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** "+ New …" floating button. */
@Composable
fun AddFab(label: String, onClick: () -> Unit, icon: ImageVector = Icons.Rounded.Add) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, null) },
        text = { Text(label) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

/** A filter: [value] null means "all". */
data class FilterOption(val value: String?, val label: String)

@Composable
fun FilterChips(options: List<FilterOption>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option.value == selected,
                onClick = { onSelect(option.value) },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer),
            )
        }
    }
}

/**
 * A full list screen: search, optional filter chips, pull to refresh, infinite scroll, empty and error states.
 * Pass `onBack = null` when the list is a tab.
 */
@Composable
fun <T> ManageListScreen(
    title: String,
    onBack: (() -> Unit)?,
    vm: PagedViewModel<T>,
    key: (T) -> Any,
    emptyTitle: String,
    searchHint: String? = "Search",
    filters: List<FilterOption> = emptyList(),
    snackbar: SnackbarHostState? = null,
    fab: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: LazyListScope.() -> Unit = {},
    item: @Composable (T) -> Unit,
) {
    ManageScaffold(title, onBack, snackbar, actions, fab) { padding ->
        val state by vm.state.collectAsStateWithLifecycle()
        PagedListBody(
            vm = vm,
            searchHint = searchHint,
            emptyTitle = emptyTitle,
            key = key,
            modifier = Modifier.padding(padding),
            header = {
                if (filters.isNotEmpty()) item { FilterChips(filters, state.filter, vm::filter) }
                header()
            },
            itemContent = item,
        )
    }
}

/** A screen that loads one record and shows it in a scrolling column, with pull to refresh. */
@Composable
fun <T> LoadedColumn(
    loader: LoaderViewModel<T>,
    padding: PaddingValues,
    content: LazyListScope.(T) -> Unit,
) {
    val state by loader.state.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    Box(Modifier.padding(padding).fillMaxSize()) {
        LoadContent(state, { loader.reload(showLoading = true) }) { value ->
            PullToRefreshBox(refreshing, { refreshing = true; loader.reload().invokeOnCompletion { refreshing = false } }) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) { content(value) }
            }
        }
    }
}

// ------------------------------------------------------------- content

/**
 * One row of an admin list: title, up to two lines of detail, an optional chip and a leading picture or icon.
 */
@Composable
fun RecordCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    chip: Pair<String, Tone>? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!meta.isNullOrBlank()) Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (chip != null) { Spacer(Modifier.height(6.dp)); com.darsequran.academy.ui.common.StatusChip(chip.first, chip.second) }
            }
            when {
                trailing != null -> trailing()
                onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A titled card of label/value rows (`Field`) or any other content. */
@Composable
fun DetailCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** A label above its value; hidden when [value] is blank unless [showEmpty]. */
@Composable
fun Field(label: String, value: String?, showEmpty: Boolean = true) {
    if (value.isNullOrBlank() && !showEmpty) return
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Label on the left, value on the right: for amounts and short facts. */
@Composable
fun FieldRow(label: String, value: String?, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value ?: "—", style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, fontWeight = if (strong) FontWeight.SemiBold else null)
    }
}

@Composable
fun Divider() = HorizontalDivider(color = Brand.outlineSoft)

/** A small count tile (dashboard and summaries). */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false, onClick: (() -> Unit)? = null) {
    AppCard(
        modifier,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        color = if (highlight) Brand.warningContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (highlight) Brand.warning else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = if (highlight) Brand.warning else MaterialTheme.colorScheme.onSurface)
    }
}

/** An inline error or note at the top of a screen. */
@Composable
fun Notice(text: String?, tone: Tone = Tone.Danger) { if (!text.isNullOrBlank()) Banner(text, tone) }

// ------------------------------------------------------------- dialogs

/** Ask before a destructive or important action. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Ask for a short text (a reason, a note) before an action. [required] blocks an empty answer. */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
    required: Boolean = false,
    message: String? = null,
    singleLine: Boolean = false,
    destructive: Boolean = false,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value, { value = it }, label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm(value.trim()) }, enabled = !required || value.isNotBlank()) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Small icon for list rows without a picture. */
@Composable
fun RowIcon(icon: ImageVector, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
}
