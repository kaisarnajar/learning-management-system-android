package com.darsequran.academy.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.BuildConfig
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.rememberContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

/** A plain client: the app's own one would send this environment's sign-in token to the other servers. */
private val pingClient by lazy { okhttp3.OkHttpClient.Builder().callTimeout(8, java.util.concurrent.TimeUnit.SECONDS).build() }

/** How a server answered the health check. */
private sealed interface Health {
    data object Checking : Health
    data class Up(val ms: Long) : Health
    data class Down(val reason: String) : Health
}

/**
 * Developers: choose which copy of the website the app talks to. Each environment keeps its own sign-in, so
 * switching back returns to the account used there. Every server is pinged so a dead one is obvious before
 * switching to it.
 */
@Composable
fun EnvironmentScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val current by container.environments.environment.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val health = androidx.compose.runtime.remember { mutableStateMapOf<AppEnvironment, Health>() }

    LaunchedEffect(Unit) {
        AppEnvironment.available.forEach { env ->
            health[env] = Health.Checking
            launch {
                health[env] = withContext(Dispatchers.IO) {
                    val started = System.nanoTime()
                    runCatching {
                        pingClient.newCall(Request.Builder().url(env.baseUrl.trimEnd('/') + "/api/v1/app-config").build()).execute().use { r ->
                            if (r.isSuccessful) Health.Up((System.nanoTime() - started) / 1_000_000) else Health.Down("HTTP ${r.code}")
                        }
                    }.getOrElse { Health.Down(it.javaClass.simpleName) }
                }
            }
        }
    }

    ManageScaffold("Environment", onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Banner("The app talks to one copy of the website at a time. You stay signed in separately on each.", Tone.Info)
            AppEnvironment.available.forEach { env ->
                AppCard(
                    Modifier.fillMaxWidth(),
                    onClick = { if (env != current) scope.launch { container.switchEnvironment(env) } },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(env == current, null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(env.label, style = MaterialTheme.typography.titleMedium)
                            Text(env.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when (val h = health[env]) {
                            is Health.Up -> StatusChip("${h.ms} ms", Tone.Success)
                            is Health.Down -> StatusChip(h.reason, Tone.Danger)
                            else -> StatusChip("Checking", Tone.Neutral)
                        }
                    }
                }
            }
            Text(
                "App ${BuildConfig.VERSION_NAME} (${if (BuildConfig.DEBUG) "debug" else "release"} build)",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A thin band above everything while the app is not on production, so test data is never mistaken for real.
 * Tapping it opens the environment screen.
 */
@Composable
fun EnvironmentStrip(env: AppEnvironment, onClick: () -> Unit) {
    if (env == AppEnvironment.PRODUCTION) return
    val color = if (env == AppEnvironment.STAGING) Color(0xFFB45309) else Color(0xFF1D4ED8)
    Row(
        Modifier.fillMaxWidth().background(color).windowInsetsPadding(WindowInsets.statusBars).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Dns, null, tint = Color.White, modifier = Modifier.padding(end = 6.dp).width(14.dp))
        Text(
            "${env.label.uppercase()} · ${env.host}", style = MaterialTheme.typography.labelSmall, color = Color.White,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** "Server: Staging (change)" under the sign-in form, for developers. */
@Composable
fun EnvironmentLink(env: AppEnvironment, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Server: ${env.label} · change", style = MaterialTheme.typography.labelLarge)
    }
}
