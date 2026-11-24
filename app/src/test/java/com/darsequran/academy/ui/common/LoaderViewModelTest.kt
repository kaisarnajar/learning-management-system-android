package com.darsequran.academy.ui.common

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoaderViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun theNewestLoadWins() {
        val answers = mutableListOf<CompletableDeferred<ApiResult<String>>>()
        val vm = LoaderViewModel { CompletableDeferred<ApiResult<String>>().also { answers += it }.await() }
        vm.reload()                                  // a second load while the first is out
        answers[1].complete(ApiResult.Ok("new"))
        answers[0].complete(ApiResult.Ok("old"))     // the first answers late
        assertEquals(Load.Ready("new"), vm.state.value)
    }

    @Test
    fun reloadKeepsContentUpWhileLoadStartsOver() {
        var n = 0
        val vm = LoaderViewModel { ApiResult.Ok(++n) }
        assertEquals(Load.Ready(1), vm.state.value)
        vm.reload(); assertEquals(Load.Ready(2), vm.state.value)
        vm.load(); assertEquals(Load.Ready(3), vm.state.value)
    }
}
