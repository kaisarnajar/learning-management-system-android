package com.darsequran.academy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darsequran.academy.data.AppEnvironment

private val StagingAmber = Color(0xFFB45309)

/**
 * A thin band above everything while the debug build is on staging, so test data is never mistaken for real.
 * Tapping it opens the Server screen.
 */
@Composable
fun EnvironmentStrip(env: AppEnvironment, onClick: () -> Unit) {
    if (env == AppEnvironment.PRODUCTION) return
    Row(
        Modifier.fillMaxWidth().background(StagingAmber).windowInsetsPadding(WindowInsets.statusBars)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Dns, null, tint = Color.White, modifier = Modifier.padding(end = 6.dp).width(14.dp))
        Text(
            "${env.label.uppercase()} · ${env.host}", style = MaterialTheme.typography.labelSmall,
            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
