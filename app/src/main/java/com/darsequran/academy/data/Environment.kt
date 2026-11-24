package com.darsequran.academy.data

import androidx.annotation.VisibleForTesting
import com.darsequran.academy.BuildConfig

/**
 * A copy of the website. Each build talks to exactly one, chosen by its flavor when it is built (staging or
 * production); nothing inside the app can change it.
 */
enum class AppEnvironment(val key: String, val label: String) {
    PRODUCTION("production", "Production"),
    STAGING("staging", "Staging");

    companion object {
        /** This build's server. */
        val current: AppEnvironment = entries.first { it.key == BuildConfig.SERVER }
    }
}

/**
 * The environment requests go to. Read on every request (by the host-rewriting interceptor) and when building image
 * and file URLs. Fixed for the app; tests may point it elsewhere.
 */
object CurrentEnvironment {
    @Volatile var value: AppEnvironment = AppEnvironment.current
        @VisibleForTesting internal set
    val baseUrl: String get() = (testBaseUrl ?: BuildConfig.BASE_URL).trimEnd('/')
    val host: String get() = baseUrl.substringAfter("://")

    /** Tests point requests at a local fake server; always null in the app. */
    @VisibleForTesting @Volatile internal var testBaseUrl: String? = null
}
