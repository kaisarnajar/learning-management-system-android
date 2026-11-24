package com.darsequran.academy.ui.manage.common

import org.junit.Assert.assertEquals
import org.junit.Test

class WithOtherTest {
    @Test fun anEmptyListStillOffersOther() = assertEquals(listOf("Other"), withOther(emptyList()))
    @Test fun otherIsAddedAtTheEnd() = assertEquals(listOf("Fiqh", "Seerah", "Other"), withOther(listOf("Fiqh", "Seerah")))
    @Test fun otherIsNotRepeated() = assertEquals(listOf("Other", "Fiqh"), withOther(listOf("Other", "Fiqh")))
    @Test fun anyCaseCounts() = assertEquals(listOf("Fiqh", "other"), withOther(listOf("Fiqh", "other")))
}
