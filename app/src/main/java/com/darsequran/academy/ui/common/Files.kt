package com.darsequran.academy.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.resolveUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/** Opens a web page in a Custom Tab (falls back to any browser). */
fun openWeb(context: Context, url: String?) {
    val target = resolveUrl(url) ?: return
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(target))
    } catch (e: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
    }
}

/** Saves [bytes] to the cache and opens it with the system viewer for [mime]. */
fun openBytes(context: Context, bytes: ByteArray, fileName: String, mime: String) {
    val dir = File(context.cacheDir, "documents").apply { mkdirs() }
    val file = File(dir, fileName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app on this phone can open this file.", Toast.LENGTH_LONG).show()
    }
}

/**
 * Downloads a file the API protects (course attachments under /api/files/…)
 * with the signed-in session, following the redirect to the short-lived
 * storage URL, and opens it.
 */
suspend fun downloadAndOpen(context: Context, path: String, fileName: String): String? {
    val url = resolveUrl(path) ?: return "This file is not available."
    val client = (context.applicationContext as AcademyApp).container.network.client
    return try {
        val (bytes, mime) = withContext(Dispatchers.IO) {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body
                body.bytes() to (body.contentType()?.toString()?.substringBefore(';') ?: "application/octet-stream")
            }
        }
        openBytes(context, bytes, fileName, mime)
        null
    } catch (e: Exception) {
        "Couldn't download the file. Check your connection and try again."
    }
}
