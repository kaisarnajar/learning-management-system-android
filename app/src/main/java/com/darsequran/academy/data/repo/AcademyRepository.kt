package com.darsequran.academy.data.repo

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.apiCall
import com.darsequran.academy.data.remote.AcademyApi
import com.darsequran.academy.data.remote.AppConfig
import com.darsequran.academy.data.remote.AskFatwaRequest
import com.darsequran.academy.data.remote.CartLine
import com.darsequran.academy.data.remote.CartReplace
import com.darsequran.academy.data.remote.ContactRequest
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
class AcademyRepository(private val api: AcademyApi) {

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
    suspend fun home() = apiCall { api.home() }.also { if (it is ApiResult.Ok) homeCache = it.value }

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
    suspend fun library(page: Int, search: String?) = apiCall { api.library(page, search?.ifBlank { null }) }
    suspend fun libraryItem(id: String) = apiCall { api.libraryItem(id).item }
    suspend fun books(page: Int, search: String?) = apiCall { api.books(page, search?.ifBlank { null }) }
    suspend fun book(id: String) = apiCall { api.book(id).book }
    suspend fun announcements(page: Int, search: String?) = apiCall { api.announcements(page, search?.ifBlank { null }) }
    suspend fun announcement(id: String) = apiCall { api.announcement(id).announcement }
    suspend fun contact(body: ContactRequest) = apiCall { api.contact(body) }

    private var paymentSettingsCache: PaymentSettings? = null
    suspend fun paymentSettings(): ApiResult<PaymentSettings> =
        paymentSettingsCache?.let { ApiResult.Ok(it) }
            ?: apiCall { api.paymentSettings().settings }.also { if (it is ApiResult.Ok) paymentSettingsCache = it.value }

    // ---- student
    suspend fun enrollments() = apiCall { api.enrollments().data }
    suspend fun enroll(courseId: String) = apiCall { api.enroll(EnrollRequest(courseId)) }
    suspend fun courseAnnouncements(courseId: String) = apiCall { api.courseAnnouncements(courseId) }
    suspend fun attendance(courseId: String) = apiCall { api.attendance(courseId).attendance.firstOrNull() }
    suspend fun grades(courseId: String) = apiCall { api.grades(courseId).grades.firstOrNull() }
    suspend fun paymentQuote(courseId: String, type: String) = apiCall { api.paymentQuote(courseId, type) }
    suspend fun paymentHistory() = apiCall { api.paymentHistory() }
    // The body streams, so read it off the main thread.
    suspend fun receipt(recordId: String) = apiCall { withContext(Dispatchers.IO) { api.receipt(recordId).use { it.bytes() } } }
    suspend fun waiverRequests() = apiCall { api.waiverRequests().data }
    suspend fun requestWaiver(courseId: String, feeType: String, reason: String) =
        apiCall { api.requestWaiver(WaiverRequestBody(courseId, feeType, reason)) }
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
    suspend fun replaceCart(lines: List<CartLine>) = apiCall { api.replaceCart(CartReplace(lines)).items }
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

/** Unwraps or returns null; convenience for parallel loads where one failure is shown separately. */
fun <T> ApiResult<T>.orNull(): T? = (this as? ApiResult.Ok)?.value
