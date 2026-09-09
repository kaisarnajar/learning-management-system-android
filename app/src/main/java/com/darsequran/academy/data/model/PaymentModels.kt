package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

data class PaymentSettingsDto(
    @SerializedName("upiId") val upiId: String? = "darsequran@upi",
    @SerializedName("upiNumber") val upiNumber: String? = "+91 96229 66911",
    @SerializedName("upiPayeeName") val upiPayeeName: String? = "Darse Quran Academy",
    @SerializedName("bankAccountName") val bankAccountName: String? = "Darse Quran Academy",
    @SerializedName("bankName") val bankName: String? = "Jammu & Kashmir Bank",
    @SerializedName("bankAccountNumber") val bankAccountNumber: String? = "0040010100000000",
    @SerializedName("bankIfsc") val bankIfsc: String? = "JAKA0TANGMR",
    @SerializedName("bankBranch") val bankBranch: String? = "Tangmarg",
    @SerializedName("feeWaiverEnabled") val feeWaiverEnabled: Boolean = true
)

data class PaymentSettingsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("settings") val settings: PaymentSettingsDto? = null
)

data class PaymentSubmissionDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("courseId") val courseId: String,
    @SerializedName("amount") val amount: Double? = 0.0,
    @SerializedName("amountInrPaise") val amountInrPaise: Long? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("status") val status: String = "PENDING", // PENDING, APPROVED, DECLINED, REJECTED
    @SerializedName("upiTransactionId") val upiTransactionId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val displayAmount: Double
        get() = if (amountInrPaise != null) amountInrPaise / 100.0 else (amount ?: 0.0)
}

data class PaymentRecordDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("courseId") val courseId: String? = null,
    @SerializedName("amount") val amount: Double? = 0.0,
    @SerializedName("amountInrPaise") val amountInrPaise: Long? = null,
    @SerializedName("receiptNumber") val receiptNumber: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("paidAt") val paidAt: String? = null
) {
    val displayAmount: Double
        get() = if (amountInrPaise != null) amountInrPaise / 100.0 else (amount ?: 0.0)
}

data class PaymentHistoryResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("submissions") val submissions: List<PaymentSubmissionDto>? = emptyList(),
    @SerializedName("records") val records: List<PaymentRecordDto>? = emptyList()
)

data class SubmitPaymentRequest(
    @SerializedName("courseId") val courseId: String,
    @SerializedName("paymentType") val paymentType: String = "monthly",
    @SerializedName("upiTransactionId") val upiTransactionId: String
)

// --- Fee Waiver / Coupon Request Models ---

data class CouponDto(
    @SerializedName("id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("percentage") val percentage: Int = 100
)

data class CouponRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("courseId") val courseId: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("status") val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("course") val course: CourseDto? = null,
    @SerializedName("coupon") val coupon: CouponDto? = null
)

data class WaiverRequestsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<CouponRequestDto>? = emptyList()
)

data class SubmitWaiverRequest(
    @SerializedName("courseId") val courseId: String,
    @SerializedName("reason") val reason: String
)

// --- Bookstore Orders Models ---

data class BookOrderItemBookDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String? = null,
    @SerializedName("imagePath") val imagePath: String? = null
)

data class BookOrderItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("priceAtPurchaseInrPaise") val priceAtPurchaseInrPaise: Long = 0,
    @SerializedName("book") val book: BookOrderItemBookDto
)

data class BookOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("totalAmountInrPaise") val totalAmountInrPaise: Long = 0,
    @SerializedName("status") val status: String = "PENDING", // PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("courierServiceName") val courierServiceName: String? = null,
    @SerializedName("trackingId") val trackingId: String? = null,
    @SerializedName("items") val items: List<BookOrderItemDto> = emptyList()
) {
    val totalAmountInRupees: Double
        get() = totalAmountInrPaise / 100.0
}

data class BookOrdersResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("orders") val orders: List<BookOrderDto>? = emptyList()
)

data class BookCheckoutResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("orderId") val orderId: String? = null,
    @SerializedName("order") val order: BookOrderDto? = null
)
