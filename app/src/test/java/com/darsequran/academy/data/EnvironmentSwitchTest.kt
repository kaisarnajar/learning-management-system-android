package com.darsequran.academy.data

import org.junit.Assert.assertFalse
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
}
