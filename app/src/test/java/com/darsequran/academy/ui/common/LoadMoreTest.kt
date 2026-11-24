package com.darsequran.academy.ui.common

import androidx.compose.material3.Text
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Paged
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadMoreTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun aFailedPageShowsRetryAndRetryLoadsIt() {
        val pages = mutableListOf<Int>()
        var failPageTwo = true
        val vm = PagedViewModel { page, _, _ ->
            pages += page
            when {
                page == 1 -> ApiResult.Ok(Paged(listOf("Item 1"), 2, 1, 1))
                failPageTwo -> ApiResult.Err("No connection.")
                else -> ApiResult.Ok(Paged(listOf("Item 2"), 2, 2, 1))
            }
        }
        compose.setContent { PagedListBody(vm, searchHint = null, emptyTitle = "Nothing", key = { it }) { Text(it) } }

        // The first page fits on screen, so the next one is asked for straight away, and fails.
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Retry")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No connection.").assertExists()
        compose.waitForIdle()
        assertEquals("no automatic retry loop", listOf(1, 2), pages)

        failPageTwo = false
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Item 2")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf(1, 2, 2), pages)
    }
}
