package com.darsequran.academy.data

import com.darsequran.academy.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each build talks to the one server its flavor names. */
class EnvironmentTest {
    @Test
    fun theServerIsTheFlavors() {
        assertEquals(BuildConfig.SERVER, AppEnvironment.current.key)
        assertEquals(AppEnvironment.current, CurrentEnvironment.value)
        assertEquals(BuildConfig.BASE_URL.trimEnd('/'), CurrentEnvironment.baseUrl)
        assertTrue(CurrentEnvironment.baseUrl.startsWith("https://"))
    }

    @Test
    fun onlyStagingAndProductionExist() =
        assertEquals(listOf(AppEnvironment.PRODUCTION, AppEnvironment.STAGING), AppEnvironment.entries)
}
