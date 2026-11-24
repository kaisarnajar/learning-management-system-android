package com.darsequran.academy.ui.explore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import com.darsequran.academy.testing.MainDispatcherRule
import com.darsequran.academy.ui.common.Load
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BlogPostViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val requests = java.util.concurrent.CopyOnWriteArrayList<String>()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += "${request.method} ${request.url.encodedPath}"
                val signedIn = request.headers["Authorization"] != null
                return when (request.url.encodedPath) {
                    "/api/v1/blog/p1" -> MockResponse(body = """{"post":{"id":"p1","title":"Patience","likedByMe":$signedIn}}""")
                    "/api/v1/blog/p1/comments" -> if (request.method == "POST") {
                        MockResponse.Builder().body("""{"comment":{"id":"c1","content":"JazakAllah"}}""").headersDelay(300, TimeUnit.MILLISECONDS).build()
                    } else MockResponse(body = """{"data":[]}""")
                    "/api/v1/blog/p1/like" -> MockResponse.Builder().body("""{"liked":true,"likeCount":1}""").headersDelay(300, TimeUnit.MILLISECONDS).build()
                    else -> MockResponse(code = 404)
                }
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() {
        runBlocking { app.container.sessionStore.clear() }
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    private fun vm() = BlogPostViewModel(app.container.repo, app.container.auth, "p1")
    private fun BlogPostViewModel.post() = (state.value.post as? Load.Ready)?.value

    @Test
    fun aSecondTapWhileLikingIsIgnored() = runBlocking {
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.post is Load.Ready } }
        vm.like(); vm.like(); vm.like()
        withTimeout(5_000) { while (vm.post()?.likedByMe != true) delay(20) }
        delay(400)
        assertEquals(1, requests.count { it == "POST /api/v1/blog/p1/like" })
    }

    @Test
    fun aSecondTapWhilePostingACommentIsIgnored() = runBlocking {
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.post is Load.Ready } }
        var done = 0
        vm.comment("JazakAllah") { done++ }; vm.comment("JazakAllah") { done++ }
        withTimeout(5_000) { while (done == 0) delay(20) }
        delay(400)
        assertEquals(1, requests.count { it == "POST /api/v1/blog/p1/comments" })
        assertEquals(1, done)
        assertEquals(listOf("c1"), vm.state.value.comments.map { it.id })
    }

    @Test
    fun signingInReloadsThePost() = runBlocking {
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.post is Load.Ready } }
        assertEquals(false, vm.post()?.likedByMe)
        app.container.sessionStore.save(TokenResponse("a", "r", user = SessionUser("u1", "student@example.test")))
        withTimeout(5_000) { while (vm.post()?.likedByMe != true) delay(20) }
        assertTrue(requests.count { it == "GET /api/v1/blog/p1" } >= 2)
    }
}
