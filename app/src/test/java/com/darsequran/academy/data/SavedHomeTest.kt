package com.darsequran.academy.data

import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.HomeResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SavedHomeTest {
    @get:Rule val folder = TemporaryFolder()
    private val json = Json { ignoreUnknownKeys = true }
    private val file get() = File(folder.root, "home.json")

    @Test
    fun whatIsWrittenIsReadBackByTheNextRun() = runBlocking {
        val home = HomeResponse(courses = listOf(Course(id = "c1", title = "Tajweed")))
        SavedHome(file, json).write(home)
        assertEquals(home, SavedHome(file, json).read())
    }

    @Test
    fun nothingSavedReadsAsNothing() = runBlocking {
        assertNull(SavedHome(file, json).read())
    }

    @Test
    fun aFileThatNoLongerReadsIsIgnored() = runBlocking {
        file.writeText("{not json")
        assertNull(SavedHome(file, json).read())
    }
}
