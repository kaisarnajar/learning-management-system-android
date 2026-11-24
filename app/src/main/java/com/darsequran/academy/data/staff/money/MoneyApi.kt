package com.darsequran.academy.data.staff.money

import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.ActionResponse
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Streaming

@Serializable data class Option(val value: String, val label: String)
@Serializable data class IdTitle(val id: String, val title: String = "")
@Serializable data class PersonRef(val id: String, val name: String? = null, val email: String = "")
@Serializable data class TeacherRef(val id: String, val name: String = "")

@Serializable
data class MoneyUser(val id: String? = null, val name: String? = null, val email: String = "", val whatsapp: String? = null) {
    val display get() = name?.takeIf { it.isNotBlank() } ?: email
}

@Serializable data class BookLine(val id: String, val title: String = "", val quantity: Int = 0, val lineTotalPaise: Int = 0)

/**
 * One row of the payment lists. [kind] says which fields are set: "submission" (a student's fee payment),
 * "book_order", "income" (a payment record) or "expense".
 */
@Serializable
data class MoneyRow(
    val kind: String = "",
    val id: String,
    val amountInrPaise: Int = 0,
    val user: MoneyUser? = null,
    val paymentRecordId: String? = null,
    val receiptGeneratedAt: String? = null,
    val courseTitle: String? = null,
    // fee submission
    val paymentType: String? = null,
    val label: String? = null,
    val status: String? = null,
    val paymentMethod: String? = null,
    val reference: String? = null,
    val screenshotPath: String? = null,
    val screenshotIsPdf: Boolean = false,
    val submittedAt: String? = null,
    val createdAt: String? = null,
    // book order
    val statusLabel: String? = null,
    val deliveryAddress: String? = null,
    val deliveryPinCode: String? = null,
    val deliveryPhoneNumber: String? = null,
    val courierServiceName: String? = null,
    val trackingId: String? = null,
    val items: List<BookLine> = emptyList(),
    val canVoid: Boolean = false,
    // payment record
    val paidAt: String? = null,
    val paymentTypeLabel: String? = null,
    val description: String? = null,
    // expense
    val category: String? = null,
    val categoryLabel: String? = null,
    val note: String? = null,
    val teacher: TeacherRef? = null,
)

@Serializable
data class PendingPayments(
    val data: List<MoneyRow> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val type: String = "",
    val counts: Map<String, Int> = emptyMap(),
)

@Serializable
data class CompletedPayments(
    val data: List<MoneyRow> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val type: String = "",
    val tabs: List<Option> = emptyList(),
)

// --------------------------------------------------------------- receipts

@Serializable
data class ReceiptRow(
    val id: String,
    val invoiceNumber: String = "",
    val receiptGeneratedAt: String? = null,
    val paymentTypeLabel: String = "",
    val amountInrPaise: Int = 0,
    val receiptIncludesGst: Boolean = false,
    val user: MoneyUser? = null,
    val courseTitle: String? = null,
)

@Serializable
data class ReceiptDetail(
    val id: String,
    val invoiceNumber: String? = null,
    val receiptGeneratedAt: String? = null,
    val receiptEmailSentAt: String? = null,
    val receiptIncludesGst: Boolean = false,
    val receiptFeeAmountPaise: Int? = null,
    val receiptGstAmountPaise: Int? = null,
    val receiptShippingAmountPaise: Int? = null,
    val amountInrPaise: Int = 0,
    val paidAt: String? = null,
    val paymentTypeLabel: String = "",
    val description: String? = null,
    val paymentMethod: String? = null,
    val reference: String? = null,
    val voided: Boolean = false,
    val courseTitle: String? = null,
    val user: MoneyUser? = null,
)

@Serializable data class ReceiptResponse(val receipt: ReceiptDetail)

// ---------------------------------------------------------------- finance

@Serializable data class FinanceRange(val preset: String = "this_month", val from: String? = null, val to: String? = null)
@Serializable data class FinanceSummary(val incomeTotalPaise: Long = 0, val expenseTotalPaise: Long = 0, val netBalancePaise: Long = 0)
@Serializable data class FilteredTotals(val recordCount: Int = 0, val totalPaise: Long = 0)

@Serializable
data class FinanceResponse(
    val data: List<MoneyRow> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val tab: String = "income",
    val range: FinanceRange = FinanceRange(),
    val summary: FinanceSummary = FinanceSummary(),
    val filtered: FilteredTotals = FilteredTotals(),
)

@Serializable
data class FinanceOptions(
    val courses: List<IdTitle> = emptyList(),
    val students: List<PersonRef> = emptyList(),
    val teachers: List<TeacherRef> = emptyList(),
    val expenseCategories: List<Option> = emptyList(),
    val incomePaymentTypes: List<Option> = emptyList(),
    val recordPaymentTypes: List<Option> = emptyList(),
    val datePresets: List<Option> = emptyList(),
    val today: String = "",
)

@Serializable
data class BookstoreSummary(
    val totalBooksPurchased: Int = 0,
    val totalBooksSold: Int = 0,
    val currentInventoryCount: Int = 0,
    val totalPurchaseCostPaise: Long = 0,
    val totalSalesRevenuePaise: Long = 0,
    val totalShippingCollectedPaise: Long = 0,
    val totalProfitLossPaise: Long = 0,
    val pendingOrdersValuePaise: Long = 0,
    val approvedOrdersValuePaise: Long = 0,
    val completedOrdersValuePaise: Long = 0,
    val averageOrderValuePaise: Long = 0,
)

/** A row of the Book Store reports: [kind] "sale" (one book) or "order". */
@Serializable
data class BookFinanceRow(
    val kind: String = "",
    val id: String,
    // sale
    val title: String? = null,
    val author: String? = null,
    val purchasePriceInrPaise: Int = 0,
    val sellingPriceInrPaise: Int = 0,
    val quantityPurchased: Int = 0,
    val quantitySold: Int = 0,
    val remainingStock: Int = 0,
    val revenuePaise: Long = 0,
    val profitPaise: Long = 0,
    // order
    val orderNumber: String? = null,
    val orderDate: String? = null,
    val customerName: String? = null,
    val customerEmail: String? = null,
    val deliveryAddress: String? = null,
    val deliveryPinCode: String? = null,
    val deliveryPhoneNumber: String? = null,
    val status: String? = null,
    val statusLabel: String? = null,
    val totalAmountPaise: Long = 0,
    val shippingChargePaise: Long = 0,
    val refundedAmountPaise: Long = 0,
    val profitContributionPaise: Long = 0,
)

@Serializable
data class BookstoreFinanceResponse(
    val data: List<BookFinanceRow> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val view: String = "sales",
    val range: FinanceRange = FinanceRange(),
    val summary: BookstoreSummary = BookstoreSummary(),
)

// ----------------------------------------------------------- transactions

@Serializable data class LookedUpStudent(val userId: String, val name: String = "")
@Serializable data class StudentLookup(val student: LookedUpStudent)

// --------------------------------------------------------------- settings

@Serializable
data class PaymentSettings(
    val upiId: String = "",
    val upiNumber: String = "",
    val upiPayeeName: String = "",
    val bankAccountName: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val bankBranch: String = "",
    val includeGstByDefault: Boolean = false,
    val feeWaiverEnabled: Boolean = false,
    val autoApproveFullWaivers: Boolean = false,
)

@Serializable data class PaymentSettingsResponse(val settings: PaymentSettings)

@Serializable data class ShippingSlab(val id: String, val minWeightGrams: Int = 0, val maxWeightGrams: Int = 0, val chargeInrPaise: Int = 0)
@Serializable data class ShippingResponse(val slabs: List<ShippingSlab> = emptyList())

// ----------------------------------------------------------- fee waivers

@Serializable
data class CouponRow(
    val id: String,
    val code: String = "",
    val type: String = "DEFAULT",
    val percentage: Int = 0,
    val validFrom: String? = null,
    val validUntil: String? = null,
    val courseId: String? = null,
    val courseTitle: String? = null,
    val gender: String? = null,
    val user: MoneyUser? = null,
    val isActive: Boolean = true,
    val applyTo: String = "BOTH",
    val applyToLabel: String = "",
    val createdAt: String? = null,
)

@Serializable
data class CouponsResponse(
    val data: List<CouponRow> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val type: String = "DEFAULT",
    val courses: List<IdTitle> = emptyList(),
)

@Serializable
data class WaiverRequest(
    val id: String,
    val createdAt: String? = null,
    val reason: String = "",
    val feeTypeLabel: String = "",
    val status: String = "PENDING",
    val couponId: String? = null,
    val user: MoneyUser? = null,
    val courseTitle: String = "",
)

@Serializable
data class WaiverStats(
    val totalWaiverCount: Int = 0,
    val totalSumWaivedPaise: Long = 0,
    val totalOriginalFeePaise: Long = 0,
    val totalNetPaidPaise: Long = 0,
    val fullWaiverCount: Int = 0,
    val fullWaiverSumPaise: Long = 0,
    val partialWaiverCount: Int = 0,
    val partialWaiverSumPaise: Long = 0,
)

@Serializable
data class WaiverItem(
    val id: String,
    val date: String? = null,
    val studentName: String = "",
    val studentEmail: String = "",
    val courseTitle: String = "",
    val paymentType: String = "",
    val label: String = "",
    val originalFeePaise: Int = 0,
    val waivedAmountPaise: Int = 0,
    val discountPercentage: Int = 0,
    val netPaidPaise: Int = 0,
    val paymentRecordId: String? = null,
    val receiptGeneratedAt: String? = null,
)

@Serializable
data class WaiverFinanceResponse(
    val data: List<WaiverItem> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val range: FinanceRange = FinanceRange(),
    val stats: WaiverStats = WaiverStats(),
)

interface MoneyApi {
    // /admin/payments
    @GET("admin/payments")
    suspend fun pending(@Query("type") type: String, @Query("page") page: Int, @Query("q") q: String?): PendingPayments

    @POST("admin/payments/{id}/approve") suspend fun approve(@Path("id") id: String): ActionResponse
    @POST("admin/payments/{id}/decline") suspend fun decline(@Path("id") id: String): ActionResponse

    @Multipart @POST("admin/payments/{id}/void")
    suspend fun voidPayment(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    // /admin/completed-payments
    @GET("admin/completed-payments")
    suspend fun completed(@Query("type") type: String, @Query("page") page: Int, @Query("q") q: String?): CompletedPayments

    @Multipart @POST("admin/payment-records/{id}/void")
    suspend fun voidRecord(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @DELETE("admin/expenses/{id}") suspend fun deleteExpense(@Path("id") id: String): ActionResponse

    // /admin/receipts
    @GET("admin/receipts") suspend fun receipts(@Query("page") page: Int, @Query("q") q: String?): Paged<ReceiptRow>
    @GET("admin/receipts/{id}") suspend fun receipt(@Path("id") id: String): ReceiptResponse
    @POST("admin/receipts/{id}") suspend fun generateReceipt(@Path("id") id: String): ActionResponse
    @DELETE("admin/receipts/{id}") suspend fun deleteReceipt(@Path("id") id: String): ActionResponse
    @POST("admin/receipts/{id}/email") suspend fun emailReceipt(@Path("id") id: String): ActionResponse
    @Streaming @GET("admin/receipts/{id}/pdf") suspend fun receiptPdf(@Path("id") id: String): ResponseBody

    // /admin/finance
    @GET("admin/finance")
    suspend fun finance(@Query("page") page: Int, @Query("q") q: String?, @QueryMap filters: Map<String, String>): FinanceResponse

    @GET("admin/finance/options") suspend fun options(@Query("students") students: String? = null): FinanceOptions

    @Streaming @GET("admin/finance/export") suspend fun export(@QueryMap params: Map<String, String>): ResponseBody

    @GET("admin/finance/bookstore")
    suspend fun bookstore(@Query("page") page: Int, @Query("q") q: String?, @QueryMap filters: Map<String, String>): BookstoreFinanceResponse

    // /admin/transactions
    @GET("admin/transactions/student") suspend fun lookupStudent(@Query("email") email: String): StudentLookup

    @Multipart @POST("admin/transactions/payments")
    suspend fun recordPayment(@Part parts: List<MultipartBody.Part>): ActionResponse

    @Multipart @POST("admin/transactions/expenses")
    suspend fun recordExpense(@Part parts: List<MultipartBody.Part>): ActionResponse

    // /admin/payment-settings, /admin/shipping-charges
    @GET("admin/payment-settings") suspend fun settings(): PaymentSettingsResponse

    @Multipart @PUT("admin/payment-settings")
    suspend fun saveSettings(@Part parts: List<MultipartBody.Part>): ActionResponse

    @GET("admin/shipping-charges") suspend fun slabs(): ShippingResponse

    @Multipart @POST("admin/shipping-charges")
    suspend fun addSlab(@Part parts: List<MultipartBody.Part>): ActionResponse

    @DELETE("admin/shipping-charges/{id}") suspend fun deleteSlab(@Path("id") id: String): ActionResponse

    // /admin/coupons
    @GET("admin/coupons") suspend fun coupons(@Query("type") type: String, @Query("page") page: Int): CouponsResponse

    @Multipart @POST("admin/coupons")
    suspend fun createCoupon(@Part parts: List<MultipartBody.Part>): ActionResponse

    @Multipart @PATCH("admin/coupons/{id}")
    suspend fun updateCoupon(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @DELETE("admin/coupons/{id}") suspend fun deleteCoupon(@Path("id") id: String): ActionResponse

    @GET("admin/coupons/requests")
    suspend fun waiverRequests(@Query("page") page: Int, @Query("status") status: String?): Paged<WaiverRequest>

    @Multipart @POST("admin/coupons/requests/{id}/approve")
    suspend fun approveRequest(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @POST("admin/coupons/requests/{id}/reject") suspend fun rejectRequest(@Path("id") id: String): ActionResponse
    @DELETE("admin/coupons/requests/{id}") suspend fun deleteRequest(@Path("id") id: String): ActionResponse

    @GET("admin/coupons/finance")
    suspend fun waiverFinance(@Query("page") page: Int, @Query("q") q: String?, @QueryMap filters: Map<String, String>): WaiverFinanceResponse
}
