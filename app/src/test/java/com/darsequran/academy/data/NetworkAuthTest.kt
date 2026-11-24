package com.darsequran.academy.data

import com.darsequran.academy.data.remote.OkResponse
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/** The token handling of [Network]: the Bearer header, refresh on 401, and when it must not refresh. */
class NetworkAuthTest {

    private interface TestApi {
        @GET("ping") suspend fun ping(): OkResponse
        @GET suspend fun absolute(@Url url: String): OkResponse
    }

    private class FakeSession(initial: Session?) : SessionSource {
        private val state = MutableStateFlow(initial)
        override val session: StateFlow<Session?> = state
        val savedFor = mutableListOf<AppEnvironment>()
        override suspend fun saveRefreshed(old: Session, tokens: TokenResponse) {
            savedFor += old.env
            if (state.value?.env == old.env) state.value = Session(tokens.accessToken, tokens.refreshToken, old.user, old.env)
        }
        override suspend fun expire(old: Session) { if (state.value?.refreshToken == old.refreshToken) state.value = null }
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val user = SessionUser(id = "u1", email = "student@example.test")
    private val server = MockWebServer()
    private val other = MockWebServer()

    /** Tokens the fake server accepts; "refresh-1" is exchanged once for "access-2". */
    private var validAccess = "access-1"
    private val refreshCalls = AtomicInteger()
    private val seenAuth: MutableList<String?> = Collections.synchronizedList(mutableListOf())
    private var refreshStatus = 200

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val auth = request.headers["Authorization"]
                return when (request.url.encodedPath) {
                    "/api/v1/auth/refresh" -> {
                        refreshCalls.incrementAndGet()
                        if (switchDuringRefresh) CurrentEnvironment.value = elsewhere
                        Thread.sleep(50) // long enough for concurrent 401s to queue behind the mutex
                        if (refreshStatus != 200) MockResponse(code = refreshStatus, body = """{"error":"Session expired."}""")
                        else { validAccess = "access-2"; MockResponse(body = """{"accessToken":"access-2","refreshToken":"refresh-2"}""") }
                    }
                    else -> {
                        seenAuth += auth
                        if (auth == "Bearer $validAccess") MockResponse(body = """{"success":true}""")
                        else MockResponse(code = 401, body = """{"error":"Unauthorized"}""")
                    }
                }
            }
        }
        other.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse(code = 401).also { seenAuth += request.headers["Authorization"] }
        }
        server.start()
        other.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After
    fun tearDown() {
        CurrentEnvironment.testBaseUrl = null
        CurrentEnvironment.value = here
        server.close()
        other.close()
    }

    /** [other] under a different host name than [server] (both listen on loopback). */
    private fun otherHostUrl(): String {
        val url = other.url("/file")
        return url.newBuilder().host(if (server.url("/").host == "127.0.0.1") "localhost" else "127.0.0.1").build().toString()
    }

    private val here = CurrentEnvironment.value
    private val elsewhere = AppEnvironment.entries.first { it != here }
    private var switchDuringRefresh = false

    private fun session(env: AppEnvironment = here) = Session("access-1", "refresh-1", user, env)

    private fun network(session: Session?) = FakeSession(session).let { it to Network(it, json).create(TestApi::class.java) }

    @Test
    fun signedInRequestsCarryTheBearerToken() = runBlocking {
        val (_, api) = network(session())
        api.ping()
        assertEquals(listOf("Bearer access-1"), seenAuth.toList())
    }

    @Test
    fun anotherEnvironmentsSessionIsNotSent() = runBlocking {
        val (_, api) = network(session(env = elsewhere))
        runCatching { api.ping() }
        assertEquals(listOf<String?>(null), seenAuth.toList())
        assertEquals(0, refreshCalls.get())
    }

    @Test
    fun aRefreshThatFinishesAfterASwitchIsStoredForTheOldEnvironmentAndNotRetried() = runBlocking {
        validAccess = "expired"
        switchDuringRefresh = true
        val (session, api) = network(session())
        val failure = runCatching { api.ping() }.exceptionOrNull()
        assertEquals(401, (failure as HttpException).code())
        assertEquals(listOf(here), session.savedFor)
        assertEquals(listOf<String?>("Bearer access-1"), seenAuth.toList()) // the new token never went anywhere
    }

    @Test
    fun anotherHostNeverSeesTheToken() = runBlocking {
        val (_, api) = network(session())
        runCatching { api.absolute(otherHostUrl()) }
        assertEquals(listOf<String?>(null), seenAuth.toList())
    }

    @Test
    fun theSameHostOnAnotherPortNeverSeesTheToken() = runBlocking {
        val (_, api) = network(session())
        val sameHostOtherPort = other.url("/file").newBuilder().host(server.url("/").host).build().toString()
        runCatching { api.absolute(sameHostOtherPort) }
        assertEquals(listOf<String?>(null), seenAuth.toList())
    }

    @Test
    fun signedOutRequestsHaveNoAuthorizationAndAreNotRefreshed() = runBlocking {
        val (_, api) = network(null)
        val failure = runCatching { api.ping() }.exceptionOrNull()
        assertEquals(401, (failure as HttpException).code())
        assertEquals(listOf<String?>(null), seenAuth.toList())
        assertEquals(0, refreshCalls.get())
    }

    @Test
    fun a401RefreshesOnceAndRetriesWithTheNewToken() = runBlocking {
        validAccess = "expired" // the stored access-1 is no longer accepted
        val (session, api) = network(session())
        api.ping()
        assertEquals(1, refreshCalls.get())
        assertEquals(listOf("Bearer access-1", "Bearer access-2"), seenAuth.toList())
        assertEquals("access-2", session.session.value?.accessToken)
        assertEquals("refresh-2", session.session.value?.refreshToken)
    }

    @Test
    fun concurrent401sShareOneRefresh() = runBlocking {
        validAccess = "expired"
        val (session, api) = network(session())
        (1..3).map { async(Dispatchers.IO) { api.ping() } }.awaitAll()
        assertEquals(1, refreshCalls.get())
        assertEquals("access-2", session.session.value?.accessToken)
    }

    /** More 401s than OkHttp's per-host limit (5) once deadlocked: the refresh queued behind the requests awaiting it. */
    @Test(timeout = 15_000)
    fun manyConcurrent401sDoNotDeadlockTheRefresh() = runBlocking {
        validAccess = "expired"
        val (session, api) = network(session())
        (1..10).map { async(Dispatchers.IO) { api.ping() } }.awaitAll()
        assertEquals(1, refreshCalls.get())
        assertEquals("access-2", session.session.value?.accessToken)
    }

    @Test
    fun aRejectedRefreshTokenSignsOut() = runBlocking {
        validAccess = "expired"
        refreshStatus = 401
        val (session, api) = network(session())
        val failure = runCatching { api.ping() }.exceptionOrNull()
        assertEquals(401, (failure as HttpException).code())
        assertNull(session.session.value)
    }

    @Test
    fun aServerErrorDuringRefreshKeepsTheSession() = runBlocking {
        validAccess = "expired"
        refreshStatus = 500
        val (session, api) = network(session())
        runCatching { api.ping() }
        assertEquals("access-1", session.session.value?.accessToken)
    }

    @Test
    fun a401FromAnotherHostIsNotRefreshed() = runBlocking {
        val (_, api) = network(session())
        val failure = runCatching { api.absolute(otherHostUrl()) }.exceptionOrNull()
        assertTrue(failure is HttpException)
        assertEquals(0, refreshCalls.get())
    }
}
