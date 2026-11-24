package com.darsequran.academy.ui.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubmitViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun twoTapsInOneFrameSendOnce() {
        val vm = SubmitViewModel()
        var sends = 0
        vm.submit { sends++; SubmitResult("Password changed.", ok = true) }
        vm.submit { sends++; SubmitResult("Password changed.", ok = true) }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, sends)
        assertEquals(SubmitResult("Password changed.", ok = true), vm.result)
    }

    @Test
    fun busyWhileSendingAndClearedAfter() {
        val vm = SubmitViewModel()
        val gate = CompletableDeferred<Unit>()
        vm.submit { gate.await(); SubmitResult("Wrong password.", ok = false) }
        assertTrue(vm.busy)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.busy)
        assertNull(vm.result)
        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.busy)
        assertEquals(SubmitResult("Wrong password.", ok = false), vm.result)
    }
}
