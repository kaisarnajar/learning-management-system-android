package com.darsequran.academy.ui.manage.people

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.people.AuditEntry
import com.darsequran.academy.data.staff.people.AuditLogApi
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.formatDateTime
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

private val prettyJson = Json { prettyPrint = true }

private fun jsonText(value: JsonElement?): String =
    if (value == null || value is JsonNull) "" else prettyJson.encodeToString(JsonElement.serializer(), value).take(1200)

/**
 * /admin/audit-log: every void, delete, approval and other admin change. Filters as on the page: entity (chips),
 * actor e-mail (search box) and entity id (the field above the list).
 */
@Composable
fun AuditLogScreen(onBack: () -> Unit) {
    var entityId by rememberSaveable { mutableStateOf("") }
    var entityIdDraft by rememberSaveable { mutableStateOf("") }
    // One list per entity id: the id is part of the list's key, so changing it starts a fresh list.
    val vm = rememberPaged("admin-audit-log-$entityId") { page, actor, entity ->
        service<AuditLogApi>().list(page, entity, entityId.ifBlank { null }, actor.ifBlank { null })
    }
    val entities = rememberLoader("admin-audit-entities") { service<AuditLogApi>().entities().entities }
    ReloadOnReturn(vm::refresh)
    val entityList = (entities.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value.orEmpty()

    ManageListScreen(
        title = "Audit log",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No audit entries match",
        searchHint = "Actor e-mail",
        filters = listOf(FilterOption(null, "All entities")) + entityList.map { FilterOption(it, it) },
        header = {
            item {
                OutlinedTextField(
                    value = entityIdDraft,
                    onValueChange = { entityIdDraft = it },
                    label = { Text("Entity id") },
                    singleLine = true,
                    trailingIcon = {
                        if (entityId.isNotBlank()) IconButton(onClick = { entityIdDraft = ""; entityId = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                        else IconButton(onClick = { entityId = entityIdDraft.trim() }) { Icon(Icons.Rounded.Search, "Filter") }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { entityId = entityIdDraft.trim() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { e -> AuditEntryCard(e) }
}

@Composable
private fun AuditEntryCard(e: AuditEntry) {
    var open by rememberSaveable(e.id) { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.action, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(formatDateTime(e.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(e.actor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(e.entity, style = MaterialTheme.typography.bodyMedium)
        e.entityId?.let { Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("Reason: " + (e.reason?.ifBlank { null } ?: "—"), style = MaterialTheme.typography.bodySmall)
        val before = remember(e.before) { jsonText(e.before) }
        val after = remember(e.after) { jsonText(e.after) }
        if (before.isNotEmpty() || after.isNotEmpty()) {
            TextButton(onClick = { open = !open }) { Text(if (open) "Hide before → after" else "Show before → after") }
            if (open) Text(
                listOf(before, after).filter { it.isNotEmpty() }.joinToString("\n→\n"),
                style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
