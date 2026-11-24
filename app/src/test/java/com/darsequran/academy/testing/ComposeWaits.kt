package com.darsequran.academy.testing

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Like `waitUntil`, but idles between checks. Compose tests run `Dispatchers.Main` (and so `viewModelScope`) on a
 * test scheduler that only moves when the test idles; `waitUntil` alone never lets a coroutine launched from another
 * one (a write started from a write's callback, say) run until the test ends.
 */
fun ComposeTestRule.waitUntilSettled(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (true) {
        waitForIdle()
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "Condition still not met after $timeoutMillis ms" }
        Thread.sleep(10)
    }
}
