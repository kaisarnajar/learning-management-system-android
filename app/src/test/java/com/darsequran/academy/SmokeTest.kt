package com.darsequran.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Checks the unit-test harness itself: a ViewModel's coroutines run on the test dispatcher. */
class SmokeTest {
    @get:Rule val main = MainDispatcherRule()

    private class CountingViewModel : ViewModel() {
        val count = MutableStateFlow(0)
        fun increment() { viewModelScope.launch { count.value++ } }
    }

    @Test
    fun viewModelScopeRunsOnTheTestDispatcher() = runTest {
        val vm = CountingViewModel()
        vm.increment()
        vm.increment()
        assertEquals(2, vm.count.value)
    }
}
