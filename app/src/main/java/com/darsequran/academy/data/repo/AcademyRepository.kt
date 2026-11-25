package com.darsequran.academy.data.repo

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.SavedHome
import com.darsequran.academy.data.apiCall
import com.darsequran.academy.data.remote.AcademyApi
import com.darsequran.academy.data.remote.AppConfig
import com.darsequran.academy.data.remote.AskFatwaRequest
import com.darsequran.academy.data.remote.CartLine
import com.darsequran.academy.data.remote.CartReplace
import com.darsequran.academy.data.remote.ContactRequest
import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.ContentRequest
import com.darsequran.academy.data.remote.EnrollRequest
import com.darsequran.academy.data.remote.HomeResponse
import com.darsequran.academy.data.remote.MarkReadRequest
import com.darsequran.academy.data.remote.PaymentSettings
import com.darsequran.academy.data.remote.ReviewRequest
import com.darsequran.academy.data.remote.WaiverRequestBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Everything the app reads and writes apart from sign-in. Thin: errors become [ApiResult.Err]. */
/** [savedHome] gives the file that keeps the home screen between runs, one per server. */
class AcademyRepository(private val api: AcademyApi, private val savedHome: ((AppEnvironment) -> SavedHome)? = null) {

    private val configLock = Mutex()
    private var config: AppConfig? = null

    /** Branding, lookups and feature switches; fetched once per process. */
    suspend fun appConfig(force: Boolean = false): ApiResult<AppConfig> = configLock.withLock {
        val cached = config
        if (cached != null && !force) return@withLock ApiResult.Ok(cached)
        apiCall { api.appConfig() }.also { if (it is ApiResult.Ok) config = it.value }
    }

    private var homeCache: HomeResponse? = null
    fun cachedHome() = homeCache

    /** Forgets what was cached from the previous environment. */
    fun clearCaches() { config = null; homeCache = null }

    suspend fun home(): ApiResult<HomeResponse> {
        // An answer that arrives after a server switch belongs to the old server: don't keep it for the new one.
        val env = CurrentEnvironment.value
        return apiCall { api.home() }.also {
            if (it is ApiResult.Ok && env == CurrentEnvironment.value) {
                homeCache = it.value
                savedHome?.invoke(env)?.write(it.value)
            }
        }
    }

    /** The home screen to show while [home] loads: this run's, else the one saved by the last run on this server. */
    suspend fun lastHome(): HomeResponse? = homeCache ?: savedHome?.invoke(CurrentEnvironment.value)?.read()

    // ---- catalogue & content
    suspend fun courses(page: Int, search: String?, category: String? = null) = apiCall { api.courses(page, search?.ifBlank { null }, category) }
    suspend fun course(id: String) = apiCall { api.course(id) }
    suspend fun teachers(page: Int, search: String?) = apiCall { api.teachers(page, search?.ifBlank { null }) }
    suspend fun teacher(id: String) = apiCall { api.teacher(id) }
    suspend fun blog(page: Int, search: String?) = apiCall { api.blog(page, search?.ifBlank { null }) }
    suspend fun blogPost(id: String) = apiCall { api.blogPost(id).post }
    suspend fun blogComments(id: String, skip: Int) = apiCall { api.blogComments(id, skip).data }
    suspend fun addComment(id: String, content: String) = apiCall { api.addComment(id, ContentRequest(content)).comment }
    suspend fun deleteComment(id: String) = apiCall { api.deleteComment(id) }
    suspend fun toggleLike(id: String) = apiCall { api.toggleLike(id) }
    suspend fun fatwas(page: Int, search: String?, category: String?) = apiCall { api.fatwas(page, search?.ifBlank { null }, category) }
    suspend fun fatwa(id: String) = apiCall { api.fatwa(id).fatwa }
    suspend fun askFatwa(body: AskFatwaRequest) = apiCall { api.askFatwa(body) }
    suspend fun library(page: Int, search: String?, topic: String? = null) =
        apiCall { api.library(page, search?.ifBlank { null }, topic) }
    suspend fun libraryItem(id: String) = apiCall { api.libraryItem(id).item }
    suspend fun books(page: Int, search: String?) = apiCall { api.books(page, search?.ifBlank { null }) }
    suspend fun book(id: String) = apiCall { api.book(id).book }
    suspend fun announcements(page: Int, search: String?) = apiCall { api.announcements(page, search?.ifBlank { null }) }
    suspend fun announcement(id: String) = apiCall { api.announcement(id).announcement }
    suspend fun contact(body: ContactRequest) = apiCall { api.contact(body) }

    suspend fun paymentSettings(): ApiResult<PaymentSettings> =
        // Not cached: the academy can change its UPI or bank details at any time.
        apiCall { api.paymentSettings().settings }

    // ---- student
    suspend fun enrollments() = apiCall { api.enrollments().data }
    suspend fun enroll(courseId: String) = apiCall { api.enroll(EnrollRequest(courseId)) }
    suspend fun courseAnnouncements(courseId: String) = apiCall { api.courseAnnouncements(courseId) }
    suspend fun attendance(courseId: String) = apiCall { api.attendance(courseId).attendance.firstOrNull() }
    suspend fun grades(courseId: String) = apiCall { api.grades(courseId).grades.firstOrNull() }
    suspend fun paymentQuote(courseId: String, type: String) = apiCall { api.paymentQuote(courseId, type) }
    suspend fun paymentHistory() = apiCall { api.paymentHistory() }
    suspend fun waiverRequests() = apiCall { api.waiverRequests().data }
    suspend fun requestWaiver(body: WaiverRequestBody) = apiCall { api.requestWaiver(body) }

    /** Every listed course (all pages), for pickers that need the whole catalogue. */
    suspend fun allCourses(): ApiResult<List<Course>> = apiCall {
        val all = mutableListOf<Course>()
        var page = 1
        do {
            val batch = api.coursePage(page, ALL_COURSES_PAGE_SIZE)
            all += batch.data
            page++
        } while (batch.data.isNotEmpty() && all.size < batch.totalCount && page <= ALL_COURSES_MAX_PAGES)
        all
    }
    suspend fun myReviews() = apiCall { api.myReviews().data }
    suspend fun addReview(body: ReviewRequest) = apiCall { api.addReview(body) }

    /**
     * Submits a fee payment. [fields] are the form fields of /payments/submit;
     * the screenshot is optional.
     */
    suspend fun submitPayment(fields: Map<String, String>, screenshot: Pair<ByteArray, String>?) = apiCall {
        api.submitPayment(fields.toParts(), screenshot?.let { (bytes, mime) -> filePart("screenshot", bytes, mime) })
    }

    // ---- bookstore
    suspend fun cart() = apiCall { api.cart().items }
    suspend fun setCartLine(bookId: String, quantity: Int) = apiCall { api.setCartLine(CartLine(bookId, quantity)).items }
    suspend fun checkoutQuote(lines: List<CartLine>) = apiCall { api.checkoutQuote(CartReplace(lines)) }
    suspend fun placeOrder(fields: Map<String, String>, screenshot: Pair<ByteArray, String>?) = apiCall {
        api.placeOrder(fields.toParts(), screenshot?.let { (bytes, mime) -> filePart("screenshot", bytes, mime) })
    }
    suspend fun orders() = apiCall { api.orders().orders }
    suspend fun order(id: String) = apiCall { api.order(id).order }

    // ---- notifications
    suspend fun notifications(page: Int, unreadOnly: Boolean) = apiCall { api.notifications(page, if (unreadOnly) "unread" else "all") }
    suspend fun notification(id: String) = apiCall { api.notification(id).notification }
    suspend fun markAllRead() = apiCall { api.markRead(MarkReadRequest(markAllRead = true)) }

    private fun Map<String, String>.toParts(): Map<String, RequestBody> =
        mapValues { (_, v) -> v.toRequestBody("text/plain".toMediaType()) }

    private fun filePart(name: String, bytes: ByteArray, mime: String) =
        MultipartBody.Part.createFormData(name, "$name.${if (mime.endsWith("png")) "png" else "jpg"}", bytes.toRequestBody(mime.toMediaType()))
}

/** The server's largest page size (PAGINATION.MAX_PAGE_SIZE). */
private const val ALL_COURSES_PAGE_SIZE = 50
private const val ALL_COURSES_MAX_PAGES = 20

/** Unwraps or returns null; convenience for parallel loads where one failure is shown separately. */
fun <T> ApiResult<T>.orNull(): T? = (this as? ApiResult.Ok)?.value
