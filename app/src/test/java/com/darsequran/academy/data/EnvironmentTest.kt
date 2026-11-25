package com.darsequran.academy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The release build only knows production; the debug build (which unit tests run) starts on staging and can switch. */
class EnvironmentTest {
    @Test
    fun theReleaseBuildOnlyHasProduction() =
        assertEquals(listOf(AppEnvironment.PRODUCTION), AppEnvironment.availableFor(debugBuild = false))

    @Test
    fun theDebugBuildHasBothAndStartsOnStaging() {
        assertEquals(listOf(AppEnvironment.PRODUCTION, AppEnvironment.STAGING), AppEnvironment.available)
        assertEquals(AppEnvironment.STAGING, AppEnvironment.default)
        assertTrue(AppEnvironment.available.all { it.baseUrl.startsWith("https://") })
    }

    @Test
    fun aRememberedKeyThatNoLongerExistsFallsBackToTheDefault() {
        assertNull(AppEnvironment.fromKey("local"))
        assertNull(AppEnvironment.fromKey(null))
    }
}
