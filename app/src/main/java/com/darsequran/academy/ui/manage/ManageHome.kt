package com.darsequran.academy.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.dashboard.DashboardApi
import com.darsequran.academy.data.staff.dashboard.DashboardLink
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.HeroBox
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.SectionHeader
import com.darsequran.academy.ui.common.academyToday
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import com.darsequran.academy.data.remote.Roles
import java.time.format.DateTimeFormatter

// ------------------------------------------------------------------ API

/**
 * The admin's first tab: what needs action now, then every section of the website's admin panel with its count.
 * [onOpen] receives a website admin path ("/admin/payments"); the caller maps it to an app screen.
 */
@Composable
fun AdminDashboardScreen(name: String?, role: String, onOpen: (String) -> Unit) {
    val loader = rememberLoader("admin-dashboard") { service<DashboardApi>().dashboard() }
    ReloadOnReturn { loader.reload() }
    val state by loader.state.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(refreshing, { refreshing = true; loader.reload().invokeOnCompletion { refreshing = false } }, Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                HeroUnderStatusBar()
                HeroBox(Modifier.fillMaxWidth()) {
                    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(20.dp).padding(top = 8.dp, bottom = 8.dp)) {
                        Overline(if (role == Roles.DEVELOPER) "Developer panel" else "Admin panel", Brand.goldSoft)
                        Spacer(Modifier.height(6.dp))
                        Text("Assalamu alaikum${name?.let { ", " + it.substringBefore(' ') } ?: ""}", style = MaterialTheme.typography.headlineSmall, color = Brand.onHero)
                        Text(academyToday().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.bodyMedium, color = Brand.onHero.copy(alpha = 0.8f))
                    }
                }
            }
            when (val s = state) {
                is Load.Ready -> {
                    val data = s.value
                    item { Spacer(Modifier.height(20.dp)) }
                    if (data.actionRequired.isNotEmpty()) {
                        item { SectionHeader("Needs your attention") }
                        item { Spacer(Modifier.height(12.dp)) }
                        tiles(data.actionRequired, onOpen)
                    } else {
                        item {
                            AppCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), color = Brand.successContainer) {
                                Text("All caught up", style = MaterialTheme.typography.titleMedium, color = Brand.success)
                                Text("No payments, enrolments, waivers or approvals are waiting.", style = MaterialTheme.typography.bodyMedium, color = Brand.success)
                            }
                        }
                    }
                    data.groups.forEach { group ->
                        item { Spacer(Modifier.height(24.dp)); SectionHeader(group.title); Spacer(Modifier.height(12.dp)) }
                        tiles(group.links, onOpen)
                    }
                }
                else -> item { LoadContent(s, { loader.reload(showLoading = true) }) {} }
            }
        }
    }
}

/** A hub tab: some of the admin panel's groups as tiles, e.g. Finance = payments, fee waivers and the bookstore. */
@Composable
fun AdminHubScreen(title: String, groupTitles: List<String>, onOpen: (String) -> Unit) {
    val loader = rememberLoader("admin-dashboard") { service<DashboardApi>().dashboard() }
    ReloadOnReturn { loader.reload() }
    val state by loader.state.collectAsStateWithLifecycle()
    ManageScaffold(title, onBack = null) { padding ->
        LoadContent(state, { loader.reload(showLoading = true) }) { data ->
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                data.groups.filter { it.title in groupTitles }.forEach { group ->
                    item { Spacer(Modifier.height(16.dp)); SectionHeader(group.title); Spacer(Modifier.height(12.dp)) }
                    tiles(group.links, onOpen)
                }
            }
        }
    }
}

private fun LazyListScope.tiles(links: List<DashboardLink>, onOpen: (String) -> Unit) {
    links.chunked(2).forEach { pair ->
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { link -> SectionTile(link, Modifier.weight(1f)) { onOpen(link.href) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionTile(link: DashboardLink, modifier: Modifier, onClick: () -> Unit) {
    AppCard(
        modifier.height(96.dp),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        color = if (link.highlight) Brand.warningContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        val accent = if (link.highlight) Brand.warning else MaterialTheme.colorScheme.onSurfaceVariant
        Text(link.label, style = MaterialTheme.typography.labelLarge, color = accent, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        if (link.count != null) {
            Text(link.count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = if (link.highlight) Brand.warning else MaterialTheme.colorScheme.onSurface)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(start = 4.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
            }
        }
    }
}
