package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DocumentCacheTest {
    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val store = app.container.sessionStore

    @After fun tearDown() = runBlocking { store.clear() }

    private fun receipt() = File(documentCacheDir(app).apply { mkdirs() }, "receipt.pdf").apply { writeText("%PDF") }

    private fun signIn(id: String) = runBlocking { store.save(TokenResponse("a-$id", "r-$id", user = SessionUser(id, "$id@example.test"))) }

    private fun awaitGone(file: File) = runBlocking { withTimeout(5_000) { while (file.exists()) delay(20) } }

    @Test
    fun signingOutClearsOpenedDocuments() {
        signIn("u1")
        val file = receipt()
        runBlocking { store.clear() }
        awaitGone(file)
        assertFalse(file.exists())
    }

    @Test
    fun aTokenRefreshForTheSameUserKeepsThem() {
        signIn("u1")
        val file = receipt()
        signIn("u1") // a new token pair, same user
        runBlocking { delay(300) }
        assertTrue(file.exists())
    }
}
