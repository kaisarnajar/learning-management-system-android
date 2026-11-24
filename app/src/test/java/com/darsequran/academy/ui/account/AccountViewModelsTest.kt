package com.darsequran.academy.ui.account

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.testing.MainDispatcherRule
import com.darsequran.academy.ui.common.Load
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
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class AccountViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val posted = CopyOnWriteArrayList<String>()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "POST") posted += request.body?.utf8().orEmpty()
                return when (request.url.encodedPath) {
                    "/api/v1/auth/me" ->
                        MockResponse(body = """{"user":{"id":"u","email":"a@b.c","address":"Srinagar"}}""")
                    "/api/v1/reviews" -> MockResponse(body = """{"success":true,"data":[]}""")
                    "/api/v1/waiver-requests" -> MockResponse(body = """{"success":true,"data":[]}""")
                    "/api/v1/courses" -> coursesPage(request.url.queryParameter("page"))
                    else -> MockResponse(code = 404)
                }
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() {
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    private fun coursesPage(page: String?) = MockResponse(
        body = if (page == "1") {
            """{"data":[{"id":"1","title":"Tajweed","status":"ONGOING"},{"id":"2","title":"Fiqh","status":"COMPLETED"}],
               "totalCount":3,"page":1,"pageSize":50}"""
        } else {
            """{"data":[{"id":"3","title":"Arabic","status":"PUBLISHED"}],"totalCount":3,"page":2,"pageSize":50}"""
        },
    )

    @Test
    fun aReviewStartsUnratedWithTheAddressAsItsLocationAndSendsTheWebsitesFields() = runBlocking {
        val vm = ReviewsViewModel(app.container.repo, app.container.auth)
        withTimeout(5_000) { vm.state.first { it is Load.Ready } }
        assertEquals(ReviewDraft(rating = 0, location = "Srinagar"), vm.draft.value)

        vm.edit { it.copy(quote = "A wonderful course with patient teachers.") }
        vm.send().join()
        assertTrue("nothing is sent without a rating", posted.isEmpty())

        vm.edit { it.copy(rating = 4) }
        vm.send().join()
        withTimeout(5_000) { vm.message.first { it != null } }
        assertEquals(
            """{"quote":"A wonderful course with patient teachers.","rating":4,"location":"Srinagar"}""",
            posted.single(),
        )
        assertEquals(ReviewDraft(location = "Srinagar"), vm.draft.value)
    }

    @Test
    fun theWaiverFormOffersEveryOpenCourseFromAllPages() = runBlocking {
        val vm = WaiversViewModel(app.container.repo)
        val page = withTimeout(5_000) { vm.state.first { it is Load.Ready } } as Load.Ready
        assertEquals(listOf("Arabic", "Tajweed"), page.value.courses.map { it.title })
    }
}
