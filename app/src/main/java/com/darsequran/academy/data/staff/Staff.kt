package com.darsequran.academy.data.staff

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.darsequran.academy.data.readImageAsJpeg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import java.io.InputStream
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/*
 * Shared pieces of the admin and teacher API (/api/v1/admin, /api/v1/teacher).
 *
 * Writes run the website's own server actions, so their bodies are the same form fields the website's forms send:
 * text fields as they are, a ticked checkbox as "on" (an unticked one is left out), files as multipart parts.
 * Every write answers with [ActionResponse].
 */

/** The answer to a write. [redirect] is where the website would have gone next (it can carry a new record's id). */
@Serializable
data class ActionResponse(
    val success: Boolean = true,
    val redirect: String? = null,
    val result: JsonElement? = null,
) {
    /** A string field of [result], e.g. `stringResult("id")`. */
    fun stringResult(key: String): String? =
        (result as? JsonObject)?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

    /**
     * Whether the notification e-mail went out. The website adds `email=anonymous|skipped|failed` to [redirect] when
     * it didn't (fatwa answers, inquiry replies); no such parameter means it was sent.
     */
    fun emailOutcome(): EmailOutcome = when (redirect?.let { Regex("[?&]email=([^&#]+)").find(it)?.groupValues?.get(1) }) {
        null -> EmailOutcome.Sent
        "anonymous" -> EmailOutcome.Anonymous
        "skipped" -> EmailOutcome.NotConfigured
        else -> EmailOutcome.Failed
    }
}

/** What happened to the notification e-mail of a write; see [ActionResponse.emailOutcome]. */
enum class EmailOutcome { Sent, Anonymous, NotConfigured, Failed }

/** `{ success, data, totalCount, page, pageSize }` lists are read with the app's existing `Paged<T>`. */

/** A file chosen on the phone, ready to upload. */
data class PickedFile(val name: String, val mime: String, val bytes: ByteArray) {
    override fun equals(other: Any?) = other is PickedFile && other.name == name && other.bytes.contentEquals(bytes)
    override fun hashCode() = name.hashCode() * 31 + bytes.contentHashCode()
}

/** What an upload field accepts: the website's limits (its config/constants/file.ts), checked before reading. */
data class UploadLimit(val maxBytes: Long, val mimeTypes: Set<String> = emptySet(), val typesLabel: String? = null) {
    companion object {
        private const val MB = 1024L * 1024
        private val IMAGES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")

        /** Blog and announcement images. */
        val Image = UploadLimit(5 * MB, IMAGES, "JPEG, PNG, WebP or GIF")
        /** Book and library covers. */
        val Cover = UploadLimit(2 * MB, IMAGES, "JPEG, PNG, WebP or GIF")
        /** Course announcement attachments (the picker already filters the types). */
        val Attachment = UploadLimit(15 * MB)
    }
}

/** A file read for upload, or why it can't be used. */
sealed interface PickOutcome {
    data class Picked(val file: PickedFile) : PickOutcome
    data class Rejected(val reason: String) : PickOutcome
}

/**
 * Reads a picked document or photo for an upload field (off the main thread), within [limit].
 *
 * The size is checked from the file's metadata before reading, and again while reading (some apps don't report it),
 * so a huge file is refused instead of filling memory. A HEIC/HEIF photo (what many phone cameras save) is turned
 * into a JPEG, which the website accepts.
 */
suspend fun Context.readPicked(uri: Uri, limit: UploadLimit): PickOutcome = withContext(Dispatchers.IO) {
    try {
        val resolver = contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        var name: String? = null
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0)
                size = if (c.isNull(1)) null else c.getLong(1)
            }
        }
        val fileName = name ?: ("upload." + mime.substringAfter('/', "bin"))
        val tooBig = { bytes: Long -> PickOutcome.Rejected("This file is ${megabytes(bytes)}; the limit here is ${megabytes(limit.maxBytes)}.") }

        if (mime == "image/heic" || mime == "image/heif") {
            if (limit.mimeTypes.isNotEmpty() && "image/jpeg" !in limit.mimeTypes) return@withContext PickOutcome.Rejected(typesMessage(limit))
            val jpeg = readImageAsJpeg(uri, maxSide = 2048) ?: return@withContext PickOutcome.Rejected("This photo couldn't be converted. Choose a JPEG or PNG.")
            if (jpeg.size > limit.maxBytes) return@withContext tooBig(jpeg.size.toLong())
            return@withContext PickOutcome.Picked(PickedFile(fileName.substringBeforeLast('.') + ".jpg", "image/jpeg", jpeg))
        }
        if (limit.mimeTypes.isNotEmpty() && mime !in limit.mimeTypes) return@withContext PickOutcome.Rejected(typesMessage(limit))
        size?.let { if (it > limit.maxBytes) return@withContext tooBig(it) }
        val bytes = resolver.openInputStream(uri)?.use { it.readAtMost(limit.maxBytes + 1) }
            ?: return@withContext PickOutcome.Rejected("This file couldn't be read.")
        if (bytes.size > limit.maxBytes) return@withContext tooBig(size ?: bytes.size.toLong())
        PickOutcome.Picked(PickedFile(fileName, mime, bytes))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PickOutcome.Rejected("This file couldn't be read.")
    }
}

private fun typesMessage(limit: UploadLimit) = "This type of file can't be used here" + (limit.typesLabel?.let { "; choose a $it file." } ?: ".")

private fun megabytes(bytes: Long) = "%.1f MB".format(java.util.Locale.ROOT, bytes / (1024.0 * 1024.0))

/** Up to [max] bytes of the stream: enough to tell that a file is over a limit without reading all of it. */
private fun InputStream.readAtMost(max: Long): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (total < max) {
        val n = read(buffer, 0, minOf(buffer.size.toLong(), max - total).toInt())
        if (n < 0) break
        out.write(buffer, 0, n)
        total += n
    }
    return out.toByteArray()
}

/** Builds the multipart body of a server-action call. */
class FormFields {
    private val parts = mutableListOf<MultipartBody.Part>()

    /** A text field; null is left out (as an empty input is not). */
    fun text(name: String, value: String?) {
        if (value != null) parts += MultipartBody.Part.createFormData(name, value)
    }

    fun number(name: String, value: Number?) = text(name, value?.toString())

    /** A checkbox: "on" when ticked, absent when not — what an HTML form sends. */
    fun check(name: String, on: Boolean) { if (on) text(name, "on") }

    /** The same field several times (a multi-select). */
    fun texts(name: String, values: Iterable<String>) = values.forEach { text(name, it) }

    fun file(name: String, file: PickedFile?) {
        if (file == null) return
        parts += MultipartBody.Part.createFormData(name, file.name, file.bytes.toRequestBody(file.mime.toMediaType()))
    }

    /** Retrofit refuses an empty multipart body, so a form with no fields sends one harmless field. */
    fun build(): List<MultipartBody.Part> = parts.ifEmpty { listOf(MultipartBody.Part.createFormData("_", "")) }
}

fun formOf(block: FormFields.() -> Unit): List<MultipartBody.Part> = FormFields().apply(block).build()

/** `JsonPrimitive` helpers for loosely typed JSON fields. */
fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.contentOrNull
