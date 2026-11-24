package com.darsequran.academy.ui.manage.common

import androidx.compose.runtime.mutableStateOf
import com.darsequran.academy.data.ApiResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCheckTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val answers = mutableMapOf<String, CompletableDeferred<ApiResult<AccountAnswer>>>()

    private val check = AccountCheck(
        mutableStateOf(""), mutableStateOf(""), mutableStateOf(""), mutableStateOf(null), scope,
    ) { email -> answers.getOrPut(email) { CompletableDeferred() }.await() }

    private fun answer(email: String, result: ApiResult<AccountAnswer>) { answers.getOrPut(email) { CompletableDeferred() }.complete(result); dispatcher.scheduler.advanceUntilIdle() }

    @Test
    fun aFoundAccountIsVerifiedForThatEmailOnly() {
        check.edit("amina@example.test"); check.verify(); dispatcher.scheduler.advanceUntilIdle()
        answer("amina@example.test", ApiResult.Ok(AccountAnswer("Amina")))
        assertTrue(check.verified)
        assertEquals("Amina", check.name)
        check.edit("bilal@example.test")
        assertFalse("an answer for the previous e-mail doesn't count", check.verified)
        assertEquals("", check.name)
    }

    @Test
    fun checkingEndsEvenWhenTheCheckIsReplaced() {
        check.edit("ami"); check.verify(); dispatcher.scheduler.advanceUntilIdle()
        assertTrue(check.checking)
        check.edit("amina@example.test"); check.verify(); dispatcher.scheduler.advanceUntilIdle() // the first is cancelled
        answer("amina@example.test", ApiResult.Ok(AccountAnswer("Amina")))
        assertFalse(check.checking)
        assertTrue(check.verified)
    }

    @Test
    fun anUnusableAccountSaysWhy() {
        check.edit("teacher@example.test"); check.verify(); dispatcher.scheduler.advanceUntilIdle()
        answer("teacher@example.test", ApiResult.Ok(AccountAnswer(null, "This account is already a teacher.")))
        assertEquals("This account is already a teacher.", check.error)
        assertFalse(check.verified)
    }

    @Test
    fun resetClearsEverything() {
        check.restore("amina@example.test", "Amina")
        assertTrue(check.verified)
        check.reset()
        assertEquals("", check.email); assertNull(check.error); assertFalse(check.verified)
    }
}
