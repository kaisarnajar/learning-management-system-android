package com.darsequran.academy.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object HomeTab
@Serializable data object CoursesTab
@Serializable data object ExploreTab
@Serializable data object AccountTab

// Courses & learning
@Serializable data class CourseRoute(val id: String)
@Serializable data class LearnRoute(val courseId: String, val title: String)
@Serializable data class PayRoute(val courseId: String, val type: String, val title: String)
@Serializable data class TeacherRoute(val id: String)
@Serializable data object TeachersRoute

// Explore
@Serializable data object BlogRoute
@Serializable data class BlogPostRoute(val id: String)
@Serializable data object FatwaRoute
@Serializable data class FatwaDetailRoute(val id: String)
@Serializable data object AskFatwaRoute
@Serializable data object LibraryRoute
@Serializable data class LibraryItemRoute(val id: String)
@Serializable data object AnnouncementsRoute
@Serializable data class AnnouncementRoute(val id: String)

// Bookstore
@Serializable data object BookstoreRoute
@Serializable data class BookRoute(val id: String)
@Serializable data object CartRoute
@Serializable data object CheckoutRoute
@Serializable data object OrdersRoute
@Serializable data class OrderRoute(val id: String)

// Account
@Serializable data object SignInRoute
@Serializable data object RegisterRoute
@Serializable data object ForgotPasswordRoute
@Serializable data object NotificationsRoute
@Serializable data object EditProfileRoute
@Serializable data object PaymentsRoute
@Serializable data object WaiversRoute
@Serializable data object ReviewsRoute
@Serializable data object ChangePasswordRoute
@Serializable data object DeleteAccountRoute
@Serializable data object ContactRoute
@Serializable data object AboutRoute
@Serializable data object ServerRoute
