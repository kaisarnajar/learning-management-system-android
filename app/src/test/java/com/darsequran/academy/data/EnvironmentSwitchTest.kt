package com.darsequran.academy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentSwitchTest {
    @Test
    fun debugBuildsAlwaysOfferTheSwitch() {
        listOf(null, "USER", "TEACHER", "ADMIN", "DEVELOPER").forEach { assertTrue(canSwitchEnvironment(debugBuild = true, role = it)) }
    }

    @Test
    fun releaseBuildsOfferItOnlyToASignedInDeveloper() {
        assertTrue(canSwitchEnvironment(debugBuild = false, role = "DEVELOPER"))
        listOf(null, "USER", "TEACHER", "ADMIN").forEach { assertFalse(canSwitchEnvironment(debugBuild = false, role = it)) }
    }

    @Test
    fun onlyStagingAndProductionExist() {
        assertEquals(listOf(AppEnvironment.PRODUCTION, AppEnvironment.STAGING), AppEnvironment.available)
        assertTrue(AppEnvironment.available.all { it.baseUrl.startsWith("https://") })
    }

    @Test
    fun theDebugBuildStartsOnStaging() {
        // Unit tests run the debug variant.
        assertEquals(AppEnvironment.STAGING, AppEnvironment.default)
        assertEquals(AppEnvironment.STAGING, AppEnvironment.defaultFor("staging"))
        assertEquals(AppEnvironment.PRODUCTION, AppEnvironment.defaultFor("production"))
    }

    @Test
    fun aReleaseBuildKeepsATestServerOnlyForASignedInDeveloper() {
        val prod = AppEnvironment.PRODUCTION
        val staging = AppEnvironment.STAGING
        assertTrue(keepsEnvironment(debugBuild = false, staging, default = prod, role = "DEVELOPER"))
        listOf(null, "USER", "TEACHER", "ADMIN").forEach {
            assertFalse(keepsEnvironment(debugBuild = false, staging, prod, it))
        }
        assertTrue("its default server is always kept", keepsEnvironment(debugBuild = false, prod, prod, null))
        assertTrue("a minified build's default is staging", keepsEnvironment(false, staging, staging, null))
        assertTrue("debug builds keep any server", keepsEnvironment(debugBuild = true, prod, staging, null))
    }

    @Test
    fun aRememberedLocalEnvironmentFallsBackToTheDefault() {
        assertNull(AppEnvironment.fromKey("local"))
    }
}
