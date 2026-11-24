package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.ErrorBody
import com.darsequran.academy.data.remote.OkResponse
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests run the debug build, where the API's JSON is parsed strictly. */
@RunWith(AndroidJUnit4::class)
class JsonStrictnessTest {
    private val json = ApplicationProvider.getApplicationContext<AcademyApp>().container.json

    @Test
    fun anUnquotedStringIsReportedInDebugBuilds() {
        val failure = runCatching { json.decodeFromString(ErrorBody.serializer(), """{"error":Unauthorized}""") }.exceptionOrNull()
        assertTrue("$failure", failure is SerializationException)
    }

    @Test
    fun unknownKeysAreStillFine() =
        assertEquals(OkResponse(true), json.decodeFromString(OkResponse.serializer(), """{"success":true,"addedLater":1}"""))
}
