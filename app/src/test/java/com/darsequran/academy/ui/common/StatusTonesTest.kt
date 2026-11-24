package com.darsequran.academy.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusTonesTest {
    @Test
    fun anOrderStatusLooksTheSameWhateverItsCase() {
        assertEquals(Tone.Info, orderStatusTone("SHIPPED"))
        assertEquals(Tone.Info, orderStatusTone("shipped"))
        assertEquals(Tone.Warning, orderStatusTone("PENDING_VERIFICATION"))
        assertEquals(Tone.Success, orderStatusTone("approved"))
        assertEquals(Tone.Danger, orderStatusTone("DECLINED"))
        assertEquals(Tone.Neutral, orderStatusTone("refunded"))
        assertEquals(Tone.Neutral, orderStatusTone(null))
    }

    @Test
    fun comingSoonIsAmberEverywhere() = assertEquals(Tone.Warning, courseStatusTone("COMING_SOON"))
}
