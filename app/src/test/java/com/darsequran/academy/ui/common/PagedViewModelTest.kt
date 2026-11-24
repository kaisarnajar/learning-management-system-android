package com.darsequran.academy.ui.common

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class PagedViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    /** Every fetch waits until the test answers it. */
    private class Requests {
        data class Call(val page: Int, val query: String, val filter: String?, val answer: CompletableDeferred<ApiResult<Paged<String>>>)
        val calls = mutableListOf<Call>()
        val fetch: suspend (Int, String, String?) -> ApiResult<Paged<String>> = { p, q, f ->
            CompletableDeferred<ApiResult<Paged<String>>>().also { calls += Call(p, q, f, it) }.await()
        }
        fun answer(index: Int, vararg items: String, total: Int = 100) =
            calls[index].answer.complete(ApiResult.Ok(Paged(items.toList(), total, calls[index].page, items.size)))
    }

    @Test
    fun aLatePageOfTheOldQueryIsNotAppendedToTheNewOne() {
        val r = Requests()
        val vm = PagedViewModel(r.fetch)
        r.answer(0, "a1", "a2")
        vm.loadMore()                         // page 2 of "" in flight
        vm.search("fiqh")                     // page 1 of "fiqh" in flight
        r.answer(2, "f1")                     // the new query answers first
        r.answer(1, "a3", "a4")               // then the stale page 2 arrives
        assertEquals(listOf("f1"), vm.state.value.items)
        assertEquals(1, vm.state.value.page)
        assertEquals("fiqh", r.calls[2].query)
    }

    @Test
    fun aRefreshDuringLoadMoreEndsOnPageOne() {
        val r = Requests()
        val vm = PagedViewModel(r.fetch)
        r.answer(0, "a1", "a2")
        vm.loadMore()
        vm.refresh()
        r.answer(1, "a3")                     // stale page 2
        r.answer(2, "b1", "b2")
        assertEquals(listOf("b1", "b2"), vm.state.value.items)
        assertEquals(1, vm.state.value.page)
        assertFalse(vm.state.value.refreshing)
    }

    @Test
    fun loadMoreRunsOneAtATime() {
        val r = Requests()
        val vm = PagedViewModel(r.fetch)
        r.answer(0, "a1")
        vm.loadMore(); vm.loadMore(); vm.loadMore()
        assertEquals(2, r.calls.size)
        r.answer(1, "a2")
        assertEquals(listOf("a1", "a2"), vm.state.value.items)
    }

    @Test
    fun aFilterChangeWinsOverTheOldFilter() {
        val r = Requests()
        val vm = PagedViewModel(r.fetch)
        vm.filter("unread")
        r.answer(1, "u1")
        r.answer(0, "x1", "x2")               // the initial unfiltered load answers last
        assertEquals(listOf("u1"), vm.state.value.items)
        assertEquals("unread", vm.state.value.filter)
    }

    private fun Requests.fail(index: Int) = calls[index].answer.complete(ApiResult.Err("No connection."))

    @Test
    fun aFailedFilterChangeDoesNotShowTheOldListAndRetryStartsFromPageOne() {
        val r = Requests()
        val vm = PagedViewModel(r.fetch)
        r.answer(0, "x1", "x2")
        vm.filter("unread")
        r.fail(1)
        assertEquals(emptyList<String>(), vm.state.value.items)
        assertEquals("No connection.", vm.state.value.error)
        vm.retry()
        assertEquals(1, r.calls[2].page)
        assertEquals("unread", r.calls[2].filter)
        r.answer(2, "u1")
        assertEquals(listOf("u1"), vm.state.value.items)
    }

    @Test
    fun aFailedRefreshKeepsTheListAndAFailedPageIsRetried() {
        val r = Requests()
        val vm = PagedViewModel(r.fetch)
        r.answer(0, "a1")
        vm.refresh()
        r.fail(1)
        assertEquals(listOf("a1"), vm.state.value.items)
        vm.retry()
        assertEquals(1, r.calls[2].page)
        r.answer(2, "a1")
        vm.loadMore()
        r.fail(3)
        vm.retry()
        assertEquals(2, r.calls[4].page)
    }

    @Test
    fun repeatedKeysAreDroppedForDisplay() {
        assertEquals(listOf("a", "b", "c"), listOf("a", "b", "b", "c", "a").distinctByKey { it })
    }
}
