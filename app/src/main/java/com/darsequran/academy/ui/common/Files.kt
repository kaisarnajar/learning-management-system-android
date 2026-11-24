package com.darsequran.academy.ui.common

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import com.darsequran.academy.data.documentCacheDir
import com.darsequran.academy.data.resolveUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.InputStream

/** Opens a web page in a Custom Tab (falls back to any browser). */
fun openWeb(context: Context, url: String?) {
    val target = resolveUrl(url) ?: return
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(target))
    } catch (e: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
    }
}

/**
 * Opens a link that leads outside the academy: web pages in a Custom Tab, `mailto:`/`tel:` and the like in the app
 * that handles them (not as a page of the academy's site, which is what [openWeb] would make of them).
 */
fun openOutside(context: Context, url: String) {
    when (val how = outsideOpen(url)) {
        is OutsideOpen.Web -> openWeb(context, how.url)
        is OutsideOpen.PhoneApp -> try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(how.uri)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app on this phone can open this link.", Toast.LENGTH_SHORT).show()
        }
        OutsideOpen.Ignored -> Unit
    }
}

/** Where an opened document is kept: the document cache, under a name safe for a file system. */
private fun cacheFile(context: Context, fileName: String): File =
    File(documentCacheDir(context).apply { mkdirs() }, fileName.replace(Regex("[^A-Za-z0-9._-]"), "_"))

/** Streams [source] into the document cache as [fileName], off the main thread, and returns the file. */
suspend fun cacheDocument(context: Context, fileName: String, source: InputStream): File = withContext(Dispatchers.IO) {
    cacheFile(context, fileName).also { file -> source.use { input -> file.outputStream().use { input.copyTo(it) } } }
}

/**
 * Opens a cached document with the system viewer for [mime]. When the phone has no app for that type (no PDF viewer,
 * say), the file goes to Downloads instead so it is not lost. Call on the main thread.
 */
fun openDocument(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (intent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
            return
        } catch (e: ActivityNotFoundException) {
            // fall through to saving it
        }
    }
    val saved = saveToDownloads(context, file, mime)
    Toast.makeText(
        context,
        if (saved) "No app here opens this file, so it was saved to Downloads as ${file.name}." else "No app on this phone can open this file.",
        Toast.LENGTH_LONG,
    ).show()
}

private fun saveToDownloads(context: Context, file: File, mime: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false // needs a storage permission before Android 10
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
        put(MediaStore.Downloads.MIME_TYPE, mime)
    }
    return runCatching {
        val resolver = context.contentResolver
        val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
        true
    }.getOrDefault(false)
}

/**
 * The type to open a downloaded file as: the server's, unless it only says "some bytes" (storage often does), in
 * which case the file name's extension decides.
 */
fun mimeFor(fileName: String, contentType: String?): String {
    val declared = contentType?.substringBefore(';')?.trim()?.lowercase()
    if (!declared.isNullOrBlank() && declared != "application/octet-stream" && declared != "binary/octet-stream") return declared
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}

/**
 * Downloads a file the API protects (course attachments under /api/files/…) with the signed-in session, following the
 * redirect to the short-lived storage URL, and opens it. The body is streamed to the cache rather than held in memory
 * (attachments can be large). Returns a message to show when it fails.
 */
suspend fun downloadAndOpen(context: Context, path: String, fileName: String): String? {
    val url = resolveUrl(path) ?: return "This file is not available."
    val client = context.appContainer.network.client
    return try {
        val (file, mime) = withContext(Dispatchers.IO) {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body
                cacheDocument(context, fileName, body.byteStream()) to mimeFor(fileName, body.contentType()?.toString())
            }
        }
        openDocument(context, file, mime)
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        "Couldn't download the file. Check your connection and try again."
    }
}
