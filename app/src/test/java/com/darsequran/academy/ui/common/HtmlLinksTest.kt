package com.darsequran.academy.ui.common

import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HtmlLinksTest {
    private val base = "https://darsequranacademy.com"

    @Test
    fun academyLinksStayInTheApp() {
        assertEquals(LinkTarget.InApp("/blog/patience"), linkTarget("/blog/patience", base))
        assertEquals(LinkTarget.InApp("/fatwa/q1"), linkTarget("https://darsequranacademy.com/fatwa/q1", base))
        assertEquals(LinkTarget.InApp("/courses/c1?tab=fees"), linkTarget("https://www.darsequranacademy.com/courses/c1?tab=fees", base))
    }

    @Test
    fun otherLinksGoOutside() {
        assertEquals(LinkTarget.Outside("https://sunnah.com/bukhari/1"), linkTarget("https://sunnah.com/bukhari/1", base))
        assertEquals(LinkTarget.Outside("mailto:info@example.test"), linkTarget("mailto:info@example.test", base))
        assertEquals(LinkTarget.Outside("https://cdn.example.test/x"), linkTarget("//cdn.example.test/x", base))
        assertEquals(LinkTarget.InApp("/blog/x"), linkTarget("//darsequranacademy.com/blog/x", base))
    }

    @Test
    fun linksCallTheAppAndImagesAreLeftOut() {
        val clicked = mutableListOf<String>()
        val text = renderHtml("""<p>Read <a href="/blog/patience">this</a> first.<img src="/uploads/a.png"></p>""") { clicked += it } as Spanned
        assertFalse("no object-replacement box", text.contains('￼'))
        assertEquals(0, text.getSpans(0, text.length, URLSpan::class.java).size)
        val link = text.getSpans(0, text.length, ClickableSpan::class.java).single()
        assertEquals("this", text.subSequence(text.getSpanStart(link), text.getSpanEnd(link)).toString())
        link.onClick(View(ApplicationProvider.getApplicationContext()))
        assertEquals(listOf("/blog/patience"), clicked)
    }

    @Test
    fun previewsHaveNoImageBoxes() = assertEquals("Hello world", plainText("<p>Hello<img src='x.png'> world</p>"))

    @Test
    fun phoneLinksGoToTheirAppAndUnsafeSchemesNowhere() {
        assertEquals(OutsideOpen.Web("https://sunnah.com/bukhari/1"), outsideOpen("https://sunnah.com/bukhari/1"))
        assertEquals(OutsideOpen.PhoneApp("mailto:info@example.test"), outsideOpen("mailto:info@example.test"))
        assertEquals(OutsideOpen.PhoneApp("tel:+911234567890"), outsideOpen("tel:+911234567890"))
        assertEquals(OutsideOpen.Ignored, outsideOpen("javascript:alert(1)"))
        assertEquals(OutsideOpen.Ignored, outsideOpen("intent://x#Intent;scheme=x;end"))
        assertEquals(OutsideOpen.Ignored, outsideOpen("file:///data/data/x"))
    }
}
