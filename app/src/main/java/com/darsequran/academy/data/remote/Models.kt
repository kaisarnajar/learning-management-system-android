package com.darsequran.academy.data.remote

import kotlinx.serialization.Serializable

/*
 * API models for /api/v1 (see docs/openapi.yaml in the web repository).
 * Every field the app does not strictly need is optional, and unknown fields
 * are ignored, so the server can grow without breaking installed apps.
 */

// ---------------------------------------------------------------- common

@Serializable
data class Paged<T>(
    val data: List<T> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val unreadCount: Int? = null,
)

@Serializable
data class OkResponse(val success: Boolean = true)

@Serializable
data class ErrorBody(val error: String? = null, val code: String? = null)

// ------------------------------------------------------------------ auth

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val name: String?, val email: String, val password: String, val acceptPolicies: Boolean = true)

@Serializable
data class GoogleRequest(val idToken: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int = 3600,
    val user: SessionUser? = null,
)

@Serializable
data class SessionUser(
    val id: String,
    val email: String,
    val name: String? = null,
    val image: String? = null,
    val role: String = "USER",
    val registrationNumber: String? = null,
)

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val name: String? = null,
    val fatherName: String? = null,
    val dateOfBirth: String? = null,
    val occupation: String? = null,
    val address: String? = null,
    val whatsapp: String? = null,
    val whatsappCountry: String? = null,
    val whatsappLocal: String? = null,
    val gender: String? = null,
    val registrationNumber: String? = null,
    val emailVerified: String? = null,
    val image: String? = null,
    val role: String = "USER",
    val profileComplete: Boolean = false,
    val hasPassword: Boolean = true,
    val createdAt: String? = null,
)

@Serializable
data class ProfileResponse(val user: Profile)

@Serializable
data class ProfileUpdate(
    val name: String? = null,
    val fatherName: String? = null,
    val dateOfBirth: String? = null,
    val occupation: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val whatsapp: String? = null,
)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class DeleteAccountRequest(val password: String? = null, val confirm: String? = null)

@Serializable
data class PhotoResponse(val image: String? = null)

// -------------------------------------------------------------- config

@Serializable
data class AppConfig(
    val brand: Brand = Brand(),
    val policies: Policies = Policies(),
    val lookups: Lookups = Lookups(),
    val features: Features = Features(),
    val limits: Limits = Limits(),
)

@Serializable
data class Brand(
    val name: String = "Darse Quran Academy",
    val websiteUrl: String = "https://darsequranacademy.com",
    val contact: Contact = Contact(),
    val social: Social = Social(),
)

@Serializable
data class Contact(
    val email: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val whatsappDefaultMessage: String = "",
    val address: String = "",
)

@Serializable
data class Social(
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val youtubeUrl: String? = null,
)

@Serializable
data class Policies(val version: String = "", val privacyPolicyUrl: String = "", val termsUrl: String = "")

@Serializable
data class Lookups(
    val countries: List<Country> = emptyList(),
    val defaultCountryCode: String = "IN",
    val occupations: List<Option> = emptyList(),
    val fatwaCategories: List<String> = emptyList(),
)

@Serializable
data class Country(
    val code: String,
    val name: String,
    val dialCode: String,
    val flag: String = "",
    val localNumberMinLength: Int = 4,
    val localNumberMaxLength: Int = 15,
)

@Serializable
data class Option(val value: String, val label: String)

@Serializable
data class Features(val googleSignIn: Boolean = false, val onlinePayments: Boolean = true, val feeWaiverRequests: Boolean = false)

@Serializable
data class Limits(val maxBookQuantityPerOrder: Int = 20)

// ------------------------------------------------------------- content

@Serializable
data class Teacher(
    val id: String,
    val name: String,
    val specialization: String = "",
    val bio: String = "",
    val initials: String = "",
    val imageUrl: String? = null,
)

@Serializable
data class Course(
    val id: String,
    val title: String,
    val description: String = "",
    val startDate: String = "",
    val duration: String = "",
    val level: String = "",
    val category: String = "",
    val status: String = "PUBLISHED",
    val priceInrPaise: Int = 0,
    val monthlyFeeInrPaise: Int = 0,
    val feeFrequency: String? = null,
    val teacher: Teacher? = null,
)

@Serializable
data class Inspiration(
    val id: String,
    val kind: String = "QURAN",
    val arabicText: String = "",
    val englishTranslation: String = "",
    val reference: String? = null,
)

@Serializable
data class ImageRef(val id: String? = null, val imagePath: String, val caption: String? = null)

@Serializable
data class Author(val name: String? = null, val image: String? = null)

@Serializable
data class SiteAnnouncement(
    val id: String,
    val title: String,
    val body: String = "",
    val eventDate: String? = null,
    val location: String? = null,
    val createdAt: String = "",
    val images: List<ImageRef> = emptyList(),
)

@Serializable
data class BlogPost(
    val id: String,
    val title: String,
    val excerpt: String? = null,
    val body: String = "",
    val createdAt: String = "",
    val images: List<ImageRef> = emptyList(),
    val createdBy: Author? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByMe: Boolean = false,
    val comments: List<Comment> = emptyList(),
)

@Serializable
data class Comment(
    val id: String,
    val content: String,
    val createdAt: String = "",
    val user: CommentUser? = null,
)

@Serializable
data class CommentUser(val id: String? = null, val name: String? = null, val image: String? = null)

@Serializable
data class Fatwa(
    val id: String,
    val title: String,
    val question: String = "",
    val category: String = "",
    val answer: String? = null,
    val askerName: String? = null,
    val answeredAt: String? = null,
    val createdAt: String = "",
)

@Serializable
data class LibraryItem(
    val id: String,
    val title: String,
    val author: String = "",
    val topic: String = "",
    val level: String = "",
    val language: String = "",
    val pdfUrl: String? = null,
    val imagePath: String? = null,
)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val description: String = "",
    val priceInrPaise: Int = 0,
    val mrpInrPaise: Int = 0,
    val status: String = "AVAILABLE",
    val imagePath: String? = null,
)

@Serializable
data class Review(
    val id: String,
    val quote: String,
    val course: String? = null,
    val location: String? = null,
    val name: String? = null,
    val initials: String? = null,
    val rating: Int = 5,
    val status: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class HomeResponse(
    val dailyInspiration: Inspiration? = null,
    val announcements: List<SiteAnnouncement> = emptyList(),
    val courses: List<Course> = emptyList(),
    val blogPosts: List<BlogPost> = emptyList(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val books: List<Book> = emptyList(),
    val fatwas: List<Fatwa> = emptyList(),
    val reviews: List<Review> = emptyList(),
)

@Serializable
data class InspirationResponse(val inspiration: Inspiration? = null)

@Serializable
data class CourseEnrollmentState(
    val id: String,
    val status: String,
    val rollNumber: Int? = null,
    val enrollmentFeePending: Boolean = false,
)

@Serializable
data class CourseDetailResponse(
    val course: Course,
    val enrollmentOpen: Boolean = true,
    val enrollment: CourseEnrollmentState? = null,
)

@Serializable
data class TeacherDetailResponse(val teacher: Teacher, val courses: List<Course> = emptyList())

@Serializable
data class BlogPostResponse(val post: BlogPost)

@Serializable
data class CommentsPage(val data: List<Comment> = emptyList())

@Serializable
data class CommentResponse(val comment: Comment)

@Serializable
data class ContentRequest(val content: String)

@Serializable
data class LikeResponse(val liked: Boolean, val likeCount: Int)

@Serializable
data class FatwaResponse(val fatwa: Fatwa)

@Serializable
data class AskFatwaRequest(val title: String, val question: String, val category: String, val askerName: String, val askerEmail: String)

@Serializable
data class LibraryItemResponse(val item: LibraryItem)

@Serializable
data class BookResponse(val book: Book)

@Serializable
data class AnnouncementResponse(val announcement: SiteAnnouncement)

@Serializable
data class ContactRequest(val name: String?, val email: String?, val phone: String?, val message: String)

// ------------------------------------------------------------- courses

@Serializable
data class Enrollment(
    val id: String,
    val courseId: String,
    val status: String,
    val rollNumber: Int? = null,
    val createdAt: String = "",
    val course: Course? = null,
)

@Serializable
data class EnrollmentsResponse(val data: List<Enrollment> = emptyList())

@Serializable
data class EnrollRequest(val courseId: String)

@Serializable
data class EnrollResponse(
    val success: Boolean = true,
    val alreadyEnrolled: Boolean = false,
    val requiresEnrollmentFee: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
)

@Serializable
data class CourseAnnouncement(
    val id: String,
    val title: String,
    val body: String = "",
    val category: String? = null,
    val authorName: String = "",
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val enrollmentId: String? = null,
    val createdAt: String = "",
)

@Serializable
data class CourseAnnouncementsResponse(
    val courseWide: List<CourseAnnouncement> = emptyList(),
    val courseWideTotal: Int = 0,
    val personal: List<CourseAnnouncement> = emptyList(),
    val personalTotal: Int = 0,
)

@Serializable
data class AttendanceRecord(val id: String, val date: String, val isPresent: Boolean)

@Serializable
data class CourseAttendance(
    val enrollmentId: String,
    val courseId: String,
    val rollNumber: Int? = null,
    val totalClasses: Int = 0,
    val presentClasses: Int = 0,
    val absentClasses: Int = 0,
    val percentage: Int = 0,
    val records: List<AttendanceRecord> = emptyList(),
)

@Serializable
data class AttendanceResponse(val attendance: List<CourseAttendance> = emptyList())

@Serializable
data class GradeEntry(
    val id: String,
    val title: String,
    val date: String = "",
    val maxMarks: Double = 0.0,
    val marksObtained: Double? = null,
    val percentage: Int? = null,
)

@Serializable
data class CourseGrades(
    val enrollmentId: String,
    val courseId: String,
    val totalMaxMarks: Double = 0.0,
    val totalMarksObtained: Double = 0.0,
    val overallPercentage: Int = 0,
    val grades: List<GradeEntry> = emptyList(),
)

@Serializable
data class GradesResponse(val grades: List<CourseGrades> = emptyList())

// ------------------------------------------------------------ payments

@Serializable
data class PaymentSettings(
    val upiId: String? = null,
    val upiNumber: String? = null,
    val upiPayeeName: String? = null,
    val bankAccountName: String? = null,
    val bankName: String? = null,
    val bankAccountNumber: String? = null,
    val bankIfsc: String? = null,
    val bankBranch: String? = null,
    val feeWaiverEnabled: Boolean = false,
)

@Serializable
data class PaymentSettingsResponse(val settings: PaymentSettings = PaymentSettings())

@Serializable
data class QuoteCoupon(val id: String, val code: String, val percentage: Int, val amountPaise: Int, val validUntil: String? = null)

@Serializable
data class PaymentQuote(
    val courseId: String,
    val type: String,
    val canPay: Boolean,
    val reason: String? = null,
    val baseAmountPaise: Int = 0,
    val coupons: List<QuoteCoupon> = emptyList(),
    val feeFrequency: String? = null,
    val feeFrequencyLabel: String? = null,
    val oneTimeFee: Boolean = false,
    val yearOptions: List<String> = emptyList(),
)

@Serializable
data class PaymentSubmission(
    val id: String,
    val courseId: String = "",
    val paymentType: String = "",
    val label: String = "",
    val amountInrPaise: Int = 0,
    val status: String = "",
    val paymentMethod: String? = null,
    val upiTransactionId: String? = null,
    val paymentRecordId: String? = null,
    val createdAt: String = "",
)

@Serializable
data class PaymentRecord(
    val id: String,
    val courseId: String? = null,
    val amountInrPaise: Int = 0,
    val paidAt: String = "",
    val paymentType: String? = null,
    val description: String? = null,
    val invoiceNumber: String? = null,
    val receiptGeneratedAt: String? = null,
)

@Serializable
data class PaymentHistory(
    val submissions: List<PaymentSubmission> = emptyList(),
    val records: List<PaymentRecord> = emptyList(),
)

@Serializable
data class WaiverRequest(
    val id: String,
    val courseId: String,
    val reason: String = "",
    val status: String = "PENDING",
    val feeType: String? = null,
    val createdAt: String = "",
    val course: CourseTitle? = null,
    /** The first issued coupon; [couponCodes] lists every code (two for a "both fees" waiver). */
    val coupon: WaiverCoupon? = null,
    val couponCodes: List<String> = emptyList(),
)

@Serializable
data class WaiverCoupon(val code: String, val percentage: Int = 0)

@Serializable
data class CourseTitle(val title: String? = null)

@Serializable
data class WaiverRequestsResponse(val data: List<WaiverRequest> = emptyList())

@Serializable
data class WaiverRequestBody(val courseId: String, val feeType: String, val reason: String)

@Serializable
data class ReviewsResponse(val data: List<Review> = emptyList())

@Serializable
data class ReviewRequest(val quote: String, val rating: Int, val course: String? = null, val location: String? = null)

// ----------------------------------------------------------- bookstore

@Serializable
data class CartItem(
    val bookId: String,
    val title: String = "",
    val author: String = "",
    val priceInrPaise: Int = 0,
    val mrpInrPaise: Int = 0,
    val imagePath: String? = null,
    val quantity: Int = 1,
)

@Serializable
data class CartResponse(val items: List<CartItem> = emptyList())

@Serializable
data class CartLine(val bookId: String, val quantity: Int)

@Serializable
data class CartReplace(val items: List<CartLine>)

@Serializable
data class QuoteLine(
    val bookId: String,
    val title: String,
    val quantity: Int,
    val unitPriceInrPaise: Int,
    val lineTotalInrPaise: Int,
)

@Serializable
data class CheckoutQuote(
    val lines: List<QuoteLine> = emptyList(),
    val subtotalAmountInrPaise: Int = 0,
    val shippingChargeInrPaise: Int = 0,
    val totalAmountInrPaise: Int = 0,
)

@Serializable
data class OrderBook(val id: String, val title: String = "", val author: String = "", val imagePath: String? = null)

@Serializable
data class OrderItem(val id: String, val bookId: String, val quantity: Int, val priceAtPurchaseInrPaise: Int, val book: OrderBook? = null)

@Serializable
data class BookOrder(
    val id: String,
    val totalAmountInrPaise: Int = 0,
    val shippingChargeInrPaise: Int = 0,
    val status: String = "",
    val deliveryAddress: String? = null,
    val deliveryPinCode: String? = null,
    val deliveryPhoneNumber: String? = null,
    val courierServiceName: String? = null,
    val trackingId: String? = null,
    val createdAt: String = "",
    val items: List<OrderItem> = emptyList(),
    val paymentRecordId: String? = null,
    val receiptGeneratedAt: String? = null,
)

@Serializable
data class OrdersResponse(val orders: List<BookOrder> = emptyList())

@Serializable
data class OrderResponse(val order: BookOrder)

// ------------------------------------------------------- notifications

@Serializable
data class Notification(
    val id: String,
    val type: String = "",
    val title: String,
    val body: String? = null,
    val href: String = "",
    val readAt: String? = null,
    val createdAt: String = "",
)

@Serializable
data class NotificationResponse(val notification: Notification)

@Serializable
data class MarkReadRequest(val notificationId: String? = null, val markAllRead: Boolean? = null)
