package com.darsequran.academy.data

import com.darsequran.academy.data.remote.ErrorBody
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** The outcome of an API call, with a message ready to show to the user on failure. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val status: Int? = null, val code: String? = null) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(transform(value))
    is ApiResult.Err -> this
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Runs [block], turning HTTP and network failures into [ApiResult.Err] with the server's message. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val body = raw?.let { runCatching { errorJson.decodeFromString<ErrorBody>(it) }.getOrNull() }
    val message = body?.error ?: when (e.code()) {
        401 -> "Please sign in again."
        403 -> "You don't have access to this."
        404 -> "Not found."
        429 -> "Too many attempts. Please wait a moment and try again."
        in 500..599 -> "The academy's server had a problem. Please try again."
        else -> "Something went wrong (${e.code()})."
    }
    ApiResult.Err(message, e.code(), body?.code)
} catch (e: kotlinx.serialization.SerializationException) {
    // Usually a web page where JSON was expected: the server is older (or newer) than this app.
    android.util.Log.w("ApiCall", "Unexpected response", e)
    ApiResult.Err("The server's answer wasn't what this app expects. The server may not have this feature yet.")
} catch (e: IOException) {
    ApiResult.Err("No connection. Check your internet and try again.")
} catch (e: Exception) {
    // The exception's own message is for developers (it can name classes or internals), so it goes to the log only.
    android.util.Log.w("ApiCall", "Unexpected failure", e)
    ApiResult.Err("Something went wrong. Please try again.")
}
