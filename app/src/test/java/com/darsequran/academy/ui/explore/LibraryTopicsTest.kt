package com.darsequran.academy.ui.explore

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTopicsTest {
    @Test
    fun theWebsitesTopicsInItsOrder() {
        assertEquals("Islam", LIBRARY_TOPICS.first())
        assertEquals(listOf("Fatwa", "Qiraat", "Other"), LIBRARY_TOPICS.takeLast(3))
    }

    @Test
    fun theEmptyLineNamesThePickedTopic() {
        assertEquals("No library items available yet.", libraryEmptyText(null))
        assertEquals("No materials in “Tajweed” yet.", libraryEmptyText("Tajweed"))
    }
}
