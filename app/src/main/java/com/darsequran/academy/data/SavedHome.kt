package com.darsequran.academy.data

import com.darsequran.academy.data.remote.HomeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The last home screen the server sent, kept on disk so that a cold start shows it at once while the new one loads.
 * It is the public /home answer, the same for every visitor, so it is not cleared when someone signs out.
 */
class SavedHome(private val file: File, private val json: Json) {

    /** The saved home screen, or null when there is none or it no longer reads (e.g. written by an older app). */
    suspend fun read(): HomeResponse? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString(HomeResponse.serializer(), file.readText()) }.getOrNull()
    }

    /** Replaces the saved copy; written beside it first, so a crash mid-write can't leave half a file. */
    suspend fun write(home: HomeResponse): Unit = withContext(Dispatchers.IO) {
        runCatching {
            val partial = File(file.path + ".partial")
            partial.writeText(json.encodeToString(HomeResponse.serializer(), home))
            if (!partial.renameTo(file)) partial.delete()
        }
    }
}
