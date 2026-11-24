package com.darsequran.academy.data.staff

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
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

    /** The id segment after [prefix] in [redirect], e.g. "/admin/courses/" → the new course's id. */
    fun idAfter(prefix: String): String? =
        redirect?.substringAfter(prefix, "")?.substringBefore('/')?.substringBefore('?')?.takeIf { it.isNotBlank() }
}

/** `{ success, data, totalCount, page, pageSize }` lists are read with the app's existing `Paged<T>`. */

/** A file chosen on the phone, ready to upload. */
data class PickedFile(val name: String, val mime: String, val bytes: ByteArray) {
    override fun equals(other: Any?) = other is PickedFile && other.name == name && other.bytes.contentEquals(bytes)
    override fun hashCode() = name.hashCode() * 31 + bytes.contentHashCode()
}

/** Reads a picked document or photo (off the main thread). */
suspend fun Context.readPicked(uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: ("upload." + mime.substringAfter('/', "bin"))
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
        PickedFile(name, mime, bytes)
    }.getOrNull()
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
