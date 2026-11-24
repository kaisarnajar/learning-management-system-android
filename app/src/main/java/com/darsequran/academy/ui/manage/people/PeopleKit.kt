package com.darsequran.academy.ui.manage.people

import com.darsequran.academy.ui.common.ctaButtonColors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.theme.Brand

/** A list row with its actions underneath (a table row's buttons, on a phone). */
@Composable
fun ActionCard(
    title: String,
    subtitle: String? = null,
    meta: String? = null,
    chip: Pair<String, Tone>? = null,
    body: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!meta.isNullOrBlank()) Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!body.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (chip != null) { Spacer(Modifier.height(8.dp)); StatusChip(chip.first, chip.second) }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun SmallPrimary(label: String, onClick: () -> Unit, enabled: Boolean = true) =
    Button(
        colors = ctaButtonColors(),
        onClick = onClick,
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    ) { Text(label) }

@Composable
fun SmallOutlined(label: String, onClick: () -> Unit, enabled: Boolean = true) =
    OutlinedButton(onClick = onClick, enabled = enabled, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text(label) }

@Composable
fun SmallDanger(label: String, onClick: () -> Unit, enabled: Boolean = true) =
    TextButton(onClick = onClick, enabled = enabled) { Text(label, color = MaterialTheme.colorScheme.error) }

/** 1–5 stars. */
@Composable
fun Stars(rating: Int, size: Int = 18) {
    Row {
        (1..5).forEach { i ->
            Icon(
                if (i <= rating) Icons.Rounded.Star else Icons.Rounded.StarOutline, null,
                tint = if (i <= rating) Brand.gold else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(size.dp).padding(end = 1.dp),
            )
        }
    }
}
