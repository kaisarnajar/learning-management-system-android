package com.darsequran.academy.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rememberContainer
import com.darsequran.academy.ui.manage.common.ManageScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val PING_TIMEOUT_SECONDS = 8L
private const val NANOS_PER_MILLI = 1_000_000

/** A plain client: the app's own one would send this server's sign-in token to the other one. */
private val pingClient by lazy { OkHttpClient.Builder().callTimeout(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS).build() }

/** How a server answered the health check. */
private sealed interface Health {
    data object Checking : Health
    data class Up(val ms: Long) : Health
    data class Down(val reason: String) : Health
}

/**
 * Debug build only: choose which copy of the website the app talks to. Each server keeps its own sign-in, so
 * switching back returns to the account used there. Both servers are pinged, so a dead one is obvious before
 * switching to it.
 */
@Composable
fun ServerScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val current by container.environments.environment.collectAsStateWithLifecycle()
    val health = remember { mutableStateMapOf<AppEnvironment, Health>() }

    LaunchedEffect(Unit) {
        AppEnvironment.available.forEach { env ->
            health[env] = Health.Checking
            launch {
                health[env] = withContext(Dispatchers.IO) {
                    val started = System.nanoTime()
                    runCatching {
                        val request = Request.Builder().url(env.baseUrl.trimEnd('/') + "/api/v1/app-config").build()
                        pingClient.newCall(request).execute().use { r ->
                            val ms = (System.nanoTime() - started) / NANOS_PER_MILLI
                            if (r.isSuccessful) Health.Up(ms) else Health.Down("HTTP ${r.code}")
                        }
                    }.getOrElse { Health.Down(it.javaClass.simpleName) }
                }
            }
        }
    }

    ManageScaffold("Server", onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Banner(
                "This debug build can talk to either copy of the website. You stay signed in separately on each.",
                Tone.Info,
            )
            AppEnvironment.available.forEach { env ->
                AppCard(
                    Modifier.fillMaxWidth(),
                    // App scope: leaving this screen must not cancel a switch halfway.
                    onClick = { if (env != current) container.appScope.launch { container.switchEnvironment(env) } },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(env == current, null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(env.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                env.host, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when (val h = health[env]) {
                            is Health.Up -> StatusChip("${h.ms} ms", Tone.Success)
                            is Health.Down -> StatusChip(h.reason, Tone.Danger)
                            else -> StatusChip("Checking", Tone.Neutral)
                        }
                    }
                }
            }
        }
    }
}
