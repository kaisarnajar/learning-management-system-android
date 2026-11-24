package com.darsequran.academy.ui.manage.bookstore

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.ActionResponse
import com.darsequran.academy.ui.common.Tone
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// ------------------------------------------------------------------ API (/api/v1/admin/bookstore)

@Serializable
data class AdminBook(
    val id: String,
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val priceInrPaise: Int = 0,
    val mrpInrPaise: Int = 0,
    val purchasePriceInrPaise: Int = 0,
    val inventoryPurchased: Int = 0,
    val weightInGrams: Int = 0,
    val status: String = "AVAILABLE",
    val statusLabel: String = "",
    val imagePath: String? = null,
    val published: Boolean = false,
    val featuredOnHomepage: Boolean = false,
    /** Featured and published: what the website's "Homepage" column calls "Featured". */
    val featured: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable data class BookStatusOption(val value: String, val label: String)

@Serializable data class BookStock(val purchased: Int = 0, val adjustments: Int = 0, val committed: Int = 0, val available: Int = 0)

@Serializable
data class AdminBookResponse(
    val book: AdminBook,
    val stock: BookStock? = null,
    val statusOptions: List<BookStatusOption> = emptyList(),
    val featuredCount: Int = 0,
    val featuredMax: Int = 4,
)

@Serializable
data class BookFormOptions(
    val statusOptions: List<BookStatusOption> = emptyList(),
    val featuredCount: Int = 0,
    val featuredMax: Int = 4,
)

@Serializable
data class BookOrderLine(
    val id: String,
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val imagePath: String? = null,
    val quantity: Int = 1,
    val priceAtPurchaseInrPaise: Int = 0,
    val lineTotalInrPaise: Int = 0,
)

@Serializable
data class BookOrderRow(
    val id: String,
    val shortId: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val customerName: String? = null,
    val customerEmail: String = "",
    val totalAmountInrPaise: Int = 0,
    val shippingChargeInrPaise: Int = 0,
    val paymentMethod: String? = null,
    val upiTransactionId: String? = null,
    val paymentScreenshotPath: String? = null,
    val deliveryAddress: String? = null,
    val deliveryPinCode: String? = null,
    val deliveryPhoneNumber: String? = null,
    val notes: String? = null,
    val courierServiceName: String? = null,
    val trackingId: String? = null,
    val createdAt: String? = null,
    val items: List<BookOrderLine> = emptyList(),
)

@Serializable data class BookOrderCounts(val pending: Int = 0, val approved: Int = 0, val completed: Int = 0)

@Serializable
data class BookOrdersPage(
    val data: List<BookOrderRow> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10,
    val status: String = "pending",
    val counts: BookOrderCounts = BookOrderCounts(),
) {
    fun paged() = Paged(data, totalCount, page, pageSize)
}

@Serializable data class BookOrderCustomer(val id: String = "", val name: String? = null, val email: String = "")

@Serializable
data class BookOrderDetail(
    val id: String,
    val shortId: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val deleted: Boolean = false,
    val createdAt: String? = null,
    val customer: BookOrderCustomer = BookOrderCustomer(),
    val deliveryAddress: String? = null,
    val deliveryPinCode: String? = null,
    val deliveryPhoneNumber: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null,
    val upiTransactionId: String? = null,
    val paymentScreenshotPath: String? = null,
    val courierServiceName: String? = null,
    val trackingId: String? = null,
    val items: List<BookOrderLine> = emptyList(),
    val subtotalInrPaise: Int = 0,
    val shippingChargeInrPaise: Int = 0,
    val totalAmountInrPaise: Int = 0,
)

@Serializable
data class BookOrderEvent(
    val id: String,
    val action: String = "",
    val label: String = "",
    val actorEmail: String? = null,
    val reason: String? = null,
    val createdAt: String? = null,
)

@Serializable data class BookOrderResponse(val order: BookOrderDetail, val timeline: List<BookOrderEvent> = emptyList())

interface BookstoreAdminApi {
    @GET("admin/bookstore")
    suspend fun books(@Query("page") page: Int, @Query("q") q: String?): Paged<AdminBook>

    @GET("admin/bookstore/new") suspend fun formOptions(): BookFormOptions

    @GET("admin/bookstore/{id}") suspend fun book(@Path("id") id: String): AdminBookResponse

    @Multipart @POST("admin/bookstore")
    suspend fun create(@Part parts: List<MultipartBody.Part>): ActionResponse

    @Multipart @PATCH("admin/bookstore/{id}")
    suspend fun update(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @DELETE("admin/bookstore/{id}") suspend fun delete(@Path("id") id: String): ActionResponse

    @GET("admin/bookstore/orders")
    suspend fun orders(
        @Query("page") page: Int,
        @Query("q") q: String?,
        @Query("status") status: String?,
        @Query("pageSize") pageSize: Int? = null,
    ): BookOrdersPage

    @GET("admin/bookstore/orders/{id}") suspend fun order(@Path("id") id: String): BookOrderResponse

    /** approve, decline, ship (courierServiceName, trackingId), refund, void (reason). */
    @Multipart @POST("admin/bookstore/orders/{id}/{action}")
    suspend fun orderAction(@Path("id") id: String, @Path("action") action: String, @Part parts: List<MultipartBody.Part>): ActionResponse
}

// --------------------------------------------------------------- labels

fun bookStatusTone(status: String): Tone = when (status) {
    "AVAILABLE" -> Tone.Success
    "OUT_OF_STOCK" -> Tone.Danger
    "COMING_SOON" -> Tone.Warning
    else -> Tone.Neutral
}

fun bookOrderTone(status: String): Tone = when (status) {
    "PENDING_VERIFICATION" -> Tone.Warning
    "APPROVED" -> Tone.Success
    "DECLINED" -> Tone.Danger
    "SHIPPED" -> Tone.Info
    else -> Tone.Neutral
}

// --------------------------------------------------------------- routes

@Serializable data object BooksAdminRoute
@Serializable data class BookAdminRoute(val id: String)
@Serializable data object BookNewRoute
@Serializable data class BookEditRoute(val id: String)
@Serializable data object BookOrdersAdminRoute
@Serializable data class BookOrderAdminRoute(val id: String)

fun NavGraphBuilder.bookstoreAdminGraph(nav: NavHostController) {
    composable<BooksAdminRoute> {
        BooksListScreen(
            onBack = nav::popBackStack,
            onOpen = { nav.navigate(BookAdminRoute(it)) },
            onAdd = { nav.navigate(BookNewRoute) },
            onOrders = { nav.navigate(BookOrdersAdminRoute) },
        )
    }
    composable<BookAdminRoute> {
        val id = it.toRoute<BookAdminRoute>().id
        BookDetailScreen(id, nav::popBackStack, onEdit = { nav.navigate(BookEditRoute(id)) })
    }
    composable<BookNewRoute> { BookFormScreen(null, nav::popBackStack) }
    composable<BookEditRoute> { BookFormScreen(it.toRoute<BookEditRoute>().id, nav::popBackStack) }
    composable<BookOrdersAdminRoute> {
        BookOrdersListScreen(
            onBack = nav::popBackStack,
            onOpen = { nav.navigate(BookOrderAdminRoute(it)) },
            onBooks = { nav.navigate(BooksAdminRoute) { launchSingleTop = true } },
        )
    }
    composable<BookOrderAdminRoute> { BookOrderDetailScreen(it.toRoute<BookOrderAdminRoute>().id, nav::popBackStack) }
}
