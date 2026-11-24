package com.darsequran.academy.ui.explore

import com.darsequran.academy.data.remote.AskFatwaRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskFatwaFormTest {
    private val valid = AskFatwaForm(
        categoryChoice = "Fiqh",
        title = "Praying while travelling",
        question = "How many rakat should I pray when I am on a journey?",
        name = "Amina",
        email = "amina@example.com",
    )

    @Test
    fun aCompleteFormHasNoErrors() = assertTrue(valid.errors().isEmpty())

    @Test
    fun theServersMinimumsWithItsMessages() {
        val errors = valid.copy(title = "Too short", question = "Not thirty characters long.").errors()
        assertEquals("Title must be at least 10 characters.", errors[AskFatwaField.Title])
        assertEquals("Question must be at least 30 characters.", errors[AskFatwaField.Question])
    }

    @Test
    fun othersSendsTheTypedCategory() {
        val others = valid.copy(categoryChoice = "Others", customCategory = "  Zakat  ")
        assertEquals("Zakat", others.toRequest("https://darsequranacademy.com").category)
        assertEquals("Select a topic.", others.copy(customCategory = "").errors()[AskFatwaField.Category])
        assertEquals("Select a topic.", valid.copy(categoryChoice = "").errors()[AskFatwaField.Category])
    }

    @Test
    fun nameAndEmailAreRequiredUnlessAnonymous() {
        val noOne = valid.copy(name = "", email = "not-an-email")
        assertEquals("Name is required.", noOne.errors()[AskFatwaField.Name])
        assertEquals("Enter a valid email address.", noOne.errors()[AskFatwaField.Email])
        assertTrue(noOne.copy(anonymous = true).errors().isEmpty())
    }

    @Test
    fun anAnonymousQuestionIsRecordedAsTheWebsiteDoes() {
        val body = valid.copy(anonymous = true).toRequest("https://darsequranacademy.com/")
        assertEquals(
            AskFatwaRequest(valid.title, valid.question, "Fiqh", "Anonymous", "anonymous@darsequranacademy.com"),
            body,
        )
    }
}
