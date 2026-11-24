package com.darsequran.academy.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.darsequran.academy.data.CurrentEnvironment
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Where a link found in academy content should go. */
sealed interface LinkTarget {
    /** A page of the academy's own site: open the matching screen ([path] like "/blog/abc"). */
    data class InApp(val path: String) : LinkTarget
    /** Anywhere else (another site, mail, phone): hand it to the phone. */
    data class Outside(val url: String) : LinkTarget
}

/** Relative links and links to the academy's own host stay in the app; the rest go outside. */
fun linkTarget(href: String, baseUrl: String = CurrentEnvironment.baseUrl): LinkTarget {
    val trimmed = href.trim()
    if (trimmed.startsWith("/") && !trimmed.startsWith("//")) return LinkTarget.InApp(trimmed)
    val url = trimmed.toHttpUrlOrNull() ?: return LinkTarget.Outside(trimmed)
    val base = baseUrl.toHttpUrlOrNull()
    val host = url.host.removePrefix("www.")
    return if (base != null && host == base.host.removePrefix("www.")) {
        LinkTarget.InApp(url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: ""))
    } else LinkTarget.Outside(url.toString())
}

/** Opens a link from academy content; set by the app's navigation (see AppRoot), the browser otherwise. */
val LocalOpenLink = staticCompositionLocalOf<((String) -> Unit)?> { null }
