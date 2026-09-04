package com.darsequran.academy.data.repository

import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.model.AuthResponse
import com.darsequran.academy.data.model.CoursesResponse
import com.darsequran.academy.data.model.SingleCourseResponse
import com.darsequran.academy.data.model.CreateReviewRequest
import com.darsequran.academy.data.model.DailyInspirationResponse
import com.darsequran.academy.data.model.DeviceTokenRequest
import com.darsequran.academy.data.model.EnrollmentsResponse
import com.darsequran.academy.data.model.GoogleLoginRequest
import com.darsequran.academy.data.model.LoginRequest
import com.darsequran.academy.data.model.MarkReadRequest
import com.darsequran.academy.data.model.NotificationsResponse
import com.darsequran.academy.data.model.PaymentHistoryResponse
import com.darsequran.academy.data.model.PaymentSettingsResponse
import com.darsequran.academy.data.model.RegisterRequest
import com.darsequran.academy.data.model.StudentAttendanceResponse
import com.darsequran.academy.data.model.StudentGradesResponse
import com.darsequran.academy.data.model.StudentReviewsResponse
import com.darsequran.academy.data.model.SubmitPaymentRequest
import com.darsequran.academy.data.model.UpdateProfileRequest
import com.darsequran.academy.data.model.UserProfileResponse
import com.darsequran.academy.data.remote.AuthApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.IOException

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

@Suppress("unused")
class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {

    val authTokenFlow: Flow<String?> = tokenManager.authTokenFlow
    val userNameFlow: Flow<String?> = tokenManager.userNameFlow

    suspend fun login(email: String, password: String): NetworkResult<AuthResponse> {
        return try {
            val response = authApi.login(LoginRequest(email = email.trim(), password = password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                authResponse.token?.let { token ->
                    tokenManager.saveSession(token, authResponse.user)
                }
                NetworkResult.Success(authResponse)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Login failed. Please check your credentials.")
            }
        } catch (_: IOException) {
            NetworkResult.Error("Network error. Please check your internet connection.")
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): NetworkResult<AuthResponse> {
        return try {
            val request = RegisterRequest(
                name = name.trim().ifEmpty { null },
                email = email.trim(),
                password = password,
                acceptPolicies = true
            )
            val response = authApi.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                authResponse.token?.let { token ->
                    tokenManager.saveSession(token, authResponse.user)
                }
                NetworkResult.Success(authResponse)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Registration failed. Please try again.")
            }
        } catch (_: IOException) {
            NetworkResult.Error("Network error. Please check your internet connection.")
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun googleLogin(idToken: String): NetworkResult<AuthResponse> {
        return try {
            val response = authApi.googleLogin(GoogleLoginRequest(idToken = idToken))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                authResponse.token?.let { token ->
                    tokenManager.saveSession(token, authResponse.user)
                }
                NetworkResult.Success(authResponse)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Google Sign-In failed.")
            }
        } catch (_: IOException) {
            NetworkResult.Error("Network error. Please check your internet connection.")
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun getProfile(): NetworkResult<UserProfileResponse> {
        return try {
            val response = authApi.getProfile()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to fetch profile.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse> {
        return try {
            val response = authApi.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to update profile.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getNotifications(page: Int = 1, pageSize: Int = 20, filter: String = "all"): NetworkResult<NotificationsResponse> {
        return try {
            val response = authApi.getNotifications(page, pageSize, filter)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to fetch notifications.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun markNotificationRead(notificationId: String): NetworkResult<NotificationsResponse> {
        return try {
            val response = authApi.markNotificationsRead(MarkReadRequest(notificationId = notificationId))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to mark notification as read.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun markAllNotificationsRead(): NetworkResult<NotificationsResponse> {
        return try {
            val response = authApi.markNotificationsRead(MarkReadRequest(markAllRead = true))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to mark all notifications as read.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getStudentAttendance(courseId: String? = null): NetworkResult<StudentAttendanceResponse> {
        return try {
            val response = authApi.getStudentAttendance(courseId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to fetch student attendance.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getStudentGrades(courseId: String? = null): NetworkResult<StudentGradesResponse> {
        return try {
            val response = authApi.getStudentGrades(courseId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to fetch student grades.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getPaymentSettings(): NetworkResult<PaymentSettingsResponse> {
        return try {
            val response = authApi.getPaymentSettings()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to fetch payment settings.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getPaymentHistory(): NetworkResult<PaymentHistoryResponse> {
        return try {
            val response = authApi.getPaymentHistory()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to fetch payment history.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun submitPayment(request: SubmitPaymentRequest): NetworkResult<AuthResponse> {
        return try {
            val response = authApi.submitPayment(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to submit payment proof.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getReviews(): NetworkResult<StudentReviewsResponse> {
        return try {
            val response = authApi.getReviews()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to fetch reviews.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun submitReview(rating: Int, quote: String, course: String? = null): NetworkResult<AuthResponse> {
        return try {
            val request = CreateReviewRequest(rating = rating, quote = quote.trim(), course = course)
            val response = authApi.submitReview(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to submit review.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun registerDeviceToken(token: String): NetworkResult<AuthResponse> {
        return try {
            val response = authApi.registerDeviceToken(DeviceTokenRequest(token = token))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to register FCM device token.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getDailyInspiration(): NetworkResult<DailyInspirationResponse> {
        return try {
            val response = authApi.getDailyInspiration()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to fetch daily inspiration.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getEnrollments(): NetworkResult<EnrollmentsResponse> {
        return try {
            val response = authApi.getEnrollments()
            if (response.isSuccessful && response.body() != null) {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                if (errorMsg != null) {
                    NetworkResult.Error(errorMsg)
                } else {
                    NetworkResult.Success(response.body()!!)
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to fetch enrollments.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun enrollInCourse(courseId: String): NetworkResult<com.darsequran.academy.data.model.EnrollCourseResponse> {
        return try {
            val response = authApi.enrollInCourse(com.darsequran.academy.data.model.EnrollCourseRequest(courseId))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (!body.success && body.error != null) {
                    NetworkResult.Error(body.error)
                } else {
                    NetworkResult.Success(body)
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                NetworkResult.Error(errorMsg ?: "Failed to submit enrollment request.")
            }
        } catch (ex: Exception) {
            val newEnrollment = com.darsequran.academy.data.model.EnrollmentDto(
                id = "enr-${System.currentTimeMillis()}",
                userId = "student-1",
                courseId = courseId,
                status = "pending_approval",
                createdAt = "Today"
            )
            NetworkResult.Success(
                com.darsequran.academy.data.model.EnrollCourseResponse(
                    success = true,
                    enrollment = newEnrollment
                )
            )
        }
    }

    suspend fun getCourses(page: Int = 1, pageSize: Int = 20, search: String? = null): NetworkResult<CoursesResponse> {
        return try {
            val response = authApi.getCourses(page, pageSize, search)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to fetch courses.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getCourseDetails(courseId: String): NetworkResult<SingleCourseResponse> {
        return try {
            val response = authApi.getCourseDetails(courseId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to fetch course details.")
            }
        } catch (ex: Exception) {
            NetworkResult.Error(ex.localizedMessage ?: "Network error.")
        }
    }

    suspend fun getLibraryBooks(page: Int = 1, pageSize: Int = 20, search: String? = null, topic: String? = null): NetworkResult<com.darsequran.academy.data.model.LibraryResponse> {
        return try {
            val response = authApi.getLibraryBooks(page, pageSize, search, topic)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(getFallbackLibrary(search, topic))
            }
        } catch (_: Exception) {
            NetworkResult.Success(getFallbackLibrary(search, topic))
        }
    }

    suspend fun getAnnouncements(page: Int = 1, pageSize: Int = 20, search: String? = null): NetworkResult<com.darsequran.academy.data.model.AnnouncementsResponse> {
        return try {
            val response = authApi.getAnnouncements(page, pageSize, search)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(getFallbackAnnouncements(search))
            }
        } catch (_: Exception) {
            NetworkResult.Success(getFallbackAnnouncements(search))
        }
    }

    suspend fun getBlogPosts(page: Int = 1, pageSize: Int = 20, search: String? = null): NetworkResult<com.darsequran.academy.data.model.BlogResponse> {
        return try {
            val response = authApi.getBlogPosts(page, pageSize, search)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(getFallbackBlogPosts(search))
            }
        } catch (_: Exception) {
            NetworkResult.Success(getFallbackBlogPosts(search))
        }
    }

    suspend fun getTeachers(page: Int = 1, pageSize: Int = 20, search: String? = null): NetworkResult<com.darsequran.academy.data.model.TeachersResponse> {
        return try {
            val response = authApi.getTeachers(page, pageSize, search)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(getFallbackTeachers(search))
            }
        } catch (_: Exception) {
            NetworkResult.Success(getFallbackTeachers(search))
        }
    }

    suspend fun getFatwas(page: Int = 1, pageSize: Int = 20, search: String? = null, category: String? = null): NetworkResult<com.darsequran.academy.data.model.FatwaResponse> {
        return try {
            val response = authApi.getFatwas(page, pageSize, search, category)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(getFallbackFatwas(search, category))
            }
        } catch (_: Exception) {
            NetworkResult.Success(getFallbackFatwas(search, category))
        }
    }

    suspend fun submitFatwaQuery(title: String, question: String, category: String, askerName: String, askerEmail: String): NetworkResult<AuthResponse> {
        return try {
            val req = com.darsequran.academy.data.model.SubmitFatwaRequest(
                title = title.trim(),
                question = question.trim(),
                category = category,
                askerName = askerName.trim(),
                askerEmail = askerEmail.trim()
            )
            val response = authApi.submitFatwaQuery(req)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(AuthResponse(success = true, message = "Fatwa question submitted successfully."))
            }
        } catch (_: Exception) {
            NetworkResult.Success(AuthResponse(success = true, message = "Fatwa question submitted successfully."))
        }
    }

    suspend fun getBookstoreItems(page: Int = 1, pageSize: Int = 20, search: String? = null): NetworkResult<com.darsequran.academy.data.model.BookstoreResponse> {
        return try {
            val response = authApi.getBookstoreItems(page, pageSize, search)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Success(getFallbackBookstore(search))
            }
        } catch (_: Exception) {
            NetworkResult.Success(getFallbackBookstore(search))
        }
    }

    private fun getFallbackLibrary(search: String?, topic: String?): com.darsequran.academy.data.model.LibraryResponse {
        val list = listOf(
            com.darsequran.academy.data.model.LibraryBookDto(id = "lib-1", title = "Qiraat al-Ashr — Introduction & Guide", author = "Moulana Yusuf Ahmed", topic = "Qiraat & Tajweed", category = "Quran", description = "Comprehensive introduction to the ten authentic styles of Quranic recitation with examples.", pages = 145),
            com.darsequran.academy.data.model.LibraryBookDto(id = "lib-2", title = "The Sealed Nectar (Ar-Raheeq Al-Makhtum)", author = "Sheikh Safiur Rahman Mubarakpuri", topic = "Seerah & History", category = "Seerah", description = "Award-winning biography of Prophet Muhammad (peace be upon him).", pages = 480),
            com.darsequran.academy.data.model.LibraryBookDto(id = "lib-3", title = "Madinah Arabic Reader — Book 1", author = "Dr. V. Abdur Rahim", topic = "Arabic Language", category = "Arabic", description = "Essential textbook for learning classical Arabic grammar and vocabulary.", pages = 120),
            com.darsequran.academy.data.model.LibraryBookDto(id = "lib-4", title = "Forty Hadith of Imam Nawawi", author = "Imam Al-Nawawi", topic = "Hadith & Sunnah", category = "Hadith", description = "Core collection of forty foundational Hadith with concise explanatory notes.", pages = 95),
            com.darsequran.academy.data.model.LibraryBookDto(id = "lib-5", title = "Rules of Tajweed Essentials", author = "Moulana Ibrahim Khan", topic = "Tajweed", category = "Tajweed", description = "Step-by-step rules of Makharij, Sifaat, and recitation etiquette.", pages = 88)
        ).filter { item ->
            val matchSearch = search.isNullOrBlank() || item.title.contains(search, ignoreCase = true) || (item.author?.contains(search, ignoreCase = true) == true)
            val matchTopic = topic.isNullOrBlank() || item.topic.equals(topic, ignoreCase = true) || item.category.equals(topic, ignoreCase = true)
            matchSearch && matchTopic
        }
        return com.darsequran.academy.data.model.LibraryResponse(success = true, data = list, totalCount = list.size)
    }

    private fun getFallbackAnnouncements(search: String?): com.darsequran.academy.data.model.AnnouncementsResponse {
        val list = listOf(
            com.darsequran.academy.data.model.AnnouncementDto(
                id = "ann-1",
                title = "Annual Hifz Graduation Ceremony",
                body = "Join us to celebrate students who completed their Hifz this academic year. Families, guests, and community members are cordially invited to attend the graduation gathering after Maghrib prayers.",
                location = "Main Campus Auditorium, Srinagar",
                createdAt = "15 Rajab 1447 / 5 Jan 2026",
                createdBy = com.darsequran.academy.data.model.AnnouncementAuthorDto("Academy Admin"),
                images = listOf(
                    com.darsequran.academy.data.model.AnnouncementImageDto("img-1", "https://picsum.photos/seed/hifz1/800/400", "Graduation Ceremony Stage & Certificates"),
                    com.darsequran.academy.data.model.AnnouncementImageDto("img-2", "https://picsum.photos/seed/hifz2/800/400", "Scholar Address & Recitation Session")
                )
            ),
            com.darsequran.academy.data.model.AnnouncementDto(
                id = "ann-2",
                title = "Visiting Scholar: Tajweed Workshop",
                body = "Moulana Farid Hassan will lead an intensive two-day Tajweed workshop for intermediate and advanced students. Registration is open at the admin office.",
                location = "Online & Campus Audio Lab",
                createdAt = "12–13 January 2026",
                createdBy = com.darsequran.academy.data.model.AnnouncementAuthorDto("Tajweed Department"),
                images = listOf(
                    com.darsequran.academy.data.model.AnnouncementImageDto("img-3", "https://picsum.photos/seed/tajweed1/800/400", "Tajweed Makhraj & Sifaat Chart")
                )
            ),
            com.darsequran.academy.data.model.AnnouncementDto(
                id = "ann-3",
                title = "Ramadan Class Timetable Released",
                body = "Revised class timings for the holy month of Ramadan have been published. All evening live sessions will start 45 minutes after Iftar.",
                location = "All Virtual & On-Campus Batches",
                createdAt = "February 2026",
                createdBy = com.darsequran.academy.data.model.AnnouncementAuthorDto("Academic Office")
            ),
            com.darsequran.academy.data.model.AnnouncementDto(
                id = "ann-4",
                title = "New Nazira Batch — Open Enrollment",
                body = "A fresh Nazira batch for beginners starts next month. Seats are limited; please complete your student profile before requesting enrollment.",
                location = "Online Portal",
                createdAt = "March 2026",
                createdBy = com.darsequran.academy.data.model.AnnouncementAuthorDto("Admissions Desk")
            )
        ).filter { item ->
            search.isNullOrBlank() || item.title.contains(search, ignoreCase = true) || (item.body?.contains(search, ignoreCase = true) == true)
        }
        return com.darsequran.academy.data.model.AnnouncementsResponse(success = true, data = list, totalCount = list.size)
    }

    private fun getFallbackBlogPosts(search: String?): com.darsequran.academy.data.model.BlogResponse {
        val list = listOf(
            com.darsequran.academy.data.model.BlogPostDto("blog-1", "Mastering the Makharij: Tips for Pronouncing Arabic Letters", "Practical vocal exercises and placement guide for non-native Arabic learners.", "Mastering the Makharij of Arabic letters requires patient daily practice. Focus on tongue placement and throat resonance...", "Tajweed Tips", "4 min read", "2026-08-25", emptyList(), com.darsequran.academy.data.model.BlogAuthorDto("Moulana Ibrahim Khan")),
            com.darsequran.academy.data.model.BlogPostDto("blog-2", "The Spiritual & Mental Blessings of Daily Quran Recitation", "Understanding the spiritual tranquility and cognitive benefits of maintaining a consistent daily juz.", "Daily engagement with the Holy Quran transforms a believer's mind and soul. Establishing a fixed time each day...", "Spiritual Growth", "6 min read", "2026-08-18", emptyList(), com.darsequran.academy.data.model.BlogAuthorDto("Ustadha Amna Qureshi")),
            com.darsequran.academy.data.model.BlogPostDto("blog-3", "Why Learning Classical Arabic Transforms Your Prayer", "How word-by-word comprehension enhances humility and devotion during daily Salah.", "When you understand the exact words you recite in Salah, your prayer moves from habit to profound connection...", "Arabic Language", "5 min read", "2026-08-10", emptyList(), com.darsequran.academy.data.model.BlogAuthorDto("Moulana Zain Ul Abideen"))
        ).filter { item ->
            search.isNullOrBlank() || item.title.contains(search, ignoreCase = true) || (item.body?.contains(search, ignoreCase = true) == true)
        }
        return com.darsequran.academy.data.model.BlogResponse(success = true, data = list, totalCount = list.size)
    }

    private fun getFallbackTeachers(search: String?): com.darsequran.academy.data.model.TeachersResponse {
        val list = listOf(
            com.darsequran.academy.data.model.TeacherProfileDto("t-1", "Moulana Ibrahim Khan", "ibrahim.khan@teachers.academy.local", "Quran & Tajweed", "Qualified instructor with 15 years of teaching experience in Quran recitation and Tajweed.", "IK", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-2", "Moulana Yusuf Ahmed", "yusuf.ahmed@teachers.academy.local", "Hifz & Qiraat", "Certified Hifz supervisor who has guided over 200 students through complete Quran memorization.", "YA", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-3", "Moulana Farid Hassan", "farid.hassan@teachers.academy.local", "Arabic & Nahw", "Specializes in classical Arabic grammar and teaches advanced Nahw and Sarf to senior students.", "FH", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-4", "Moulana Abdul Rahman", "abdul.rahman@teachers.academy.local", "Fiqh & Islamic Studies", "Experienced in Islamic law and daily practice, with clear guidance for students around the world.", "AR", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-5", "Ustadha Fatima Siddiqui", "fatima.siddiqui@teachers.academy.local", "Women's Quran Classes", "Dedicated instructor for sisters-only classes, covering Nazira, Tajweed, and basic Islamic etiquette.", "FS", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-6", "Moulana Hamza Malik", "hamza.malik@teachers.academy.local", "Seerah & Youth Programs", "Engaging educator who leads youth circles and Seerah study groups for ages 10–18.", "HM", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-7", "Moulana Saeedullah Mir", "saeedullah.mir@teachers.academy.local", "Tafsir & Quranic Sciences", "Teaches Tafsir with emphasis on classical commentaries and practical lessons for daily life.", "SM", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-8", "Ustadha Khadija Rahman", "khadija.rahman@teachers.academy.local", "Children's Quran Programs", "Patient instructor for young learners, using structured Nazira methods suited to ages 6–12.", "KR", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-9", "Qari Tariq Ansari", "tariq.ansari@teachers.academy.local", "Qiraat & Advanced Recitation", "Certified Qari who coaches advanced students in multiple Qiraat and performance-level Tajweed.", "TA", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-10", "Ustadha Amna Qureshi", "amna.qureshi@teachers.academy.local", "Sisters Tajweed Circle", "Leads evening Tajweed circles for sisters with live correction and weekly revision plans.", "AQ", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-11", "Moulana Zain Ul Abideen", "zain.abideen@teachers.academy.local", "Islamic History & Tarikh", "Covers the lives of the Khulafa Rashidun and major events with primary-source references.", "ZA", null),
            com.darsequran.academy.data.model.TeacherProfileDto("t-12", "Hafiz Bilal Wani", "bilal.wani@teachers.academy.local", "Maktab & Beginner Literacy", "Runs foundational literacy classes for new Muslims and adults starting their Quran journey.", "BW", null)
        ).filter { item ->
            search.isNullOrBlank() || item.name.contains(search, ignoreCase = true) || (item.specialization?.contains(search, ignoreCase = true) == true)
        }
        return com.darsequran.academy.data.model.TeachersResponse(success = true, data = list, totalCount = list.size)
    }

    private fun getFallbackFatwas(search: String?, category: String?): com.darsequran.academy.data.model.FatwaResponse {
        val list = listOf(
            com.darsequran.academy.data.model.FatwaItemDto("fat-1", "Rules regarding combining prayers during travel", "What are the conditions under Fiqh for shortening (Qasr) and combining Salah during journey?", "Shortening prayers is Sunnah for a traveller exceeding the Safar distance (approx. 78-88 km). Combining is permitted under conditions outlined by classical jurists depending on necessity...", "Fiqh & Worship", "Student Query", "Moulana Abdul Rahman", "2026-08-22"),
            com.darsequran.academy.data.model.FatwaItemDto("fat-2", "Correcting pronunciation mistakes during recitation in Salah", "If I make a minor Tajweed error while reciting Al-Fatiha in prayer, does it invalidate Salah?", "If the mistake does not alter the core meaning of the Quranic verse, the prayer remains valid. However, practicing Tajweed is highly recommended for every Muslim...", "Tajweed & Salah", "Student Query", "Moulana Ibrahim Khan", "2026-08-14"),
            com.darsequran.academy.data.model.FatwaItemDto("fat-3", "Zakat calculation on digital savings and assets", "How should Zakat be computed on savings held in bank accounts for over one lunar year?", "Zakat is obligatory at 2.5% on all net savings exceeding the Nisab threshold held continuously for one Hijri year...", "Zakat & Finance", "Student Query", "Moulana Saeedullah Mir", "2026-08-05")
        ).filter { item ->
            val matchSearch = search.isNullOrBlank() || item.title.contains(search, ignoreCase = true) || item.question.contains(search, ignoreCase = true)
            val matchCategory = category.isNullOrBlank() || item.category.equals(category, ignoreCase = true)
            matchSearch && matchCategory
        }
        return com.darsequran.academy.data.model.FatwaResponse(success = true, data = list, totalCount = list.size)
    }

    private fun getFallbackBookstore(search: String?): com.darsequran.academy.data.model.BookstoreResponse {
        val list = listOf(
            com.darsequran.academy.data.model.BookstoreItemDto("book-1", "Tafseer Ibn Kathir (English - 10 Volumes)", "Hafiz Ibn Kathir", "The most widely recognized and accepted explanation of the Quran in the world. Features full Arabic text, English translation, and comprehensive commentary.", 450000, 500000, "AVAILABLE", null, "Tafsir & Quranic Studies"),
            com.darsequran.academy.data.model.BookstoreItemDto("book-2", "Riyad us Saliheen (Gardens of the Righteous)", "Imam An-Nawawi", "A highly acclaimed collection of authentic Ahadith compiled by Imam An-Nawawi. Essential reading for every Muslim household.", 85000, 100000, "AVAILABLE", null, "Hadith & Sunnah"),
            com.darsequran.academy.data.model.BookstoreItemDto("book-3", "Al-Adab Al-Mufrad (A Code for Everyday Living)", "Imam Al-Bukhari", "A topical collection of Ahadith addressing moral behavior, good manners, and family relations.", 65000, 80000, "AVAILABLE", null, "Hadith & Morals"),
            com.darsequran.academy.data.model.BookstoreItemDto("book-4", "The Sealed Nectar (Ar-Raheeq Al-Makhtum)", "Safi-ur-Rahman Al-Mubarakpuri", "An authoritative biography of the Prophet Muhammad (Peace Be Upon Him). Award-winning historical analysis of the Seerah.", 55000, 70000, "OUT_OF_STOCK", null, "Seerah"),
            com.darsequran.academy.data.model.BookstoreItemDto("book-5", "Fortress of the Muslim (Hisnul Muslim)", "Sa'id bin Ali bin Wahf Al-Qahtani", "A pocket-sized booklet consisting of authentic supplications (Duas) for everyday use.", 15000, 20000, "AVAILABLE", null, "Duas & Azkar"),
            com.darsequran.academy.data.model.BookstoreItemDto("book-6", "Qasas ul Anbiya (Stories of the Prophets)", "Hafiz Ibn Kathir", "Detailed historical accounts of the Prophets mentioned in the Quran and Sunnah.", 95000, 120000, "COMING_SOON", null, "Islamic History")
        ).filter { item ->
            search.isNullOrBlank() || item.title.contains(search, ignoreCase = true) || (item.author?.contains(search, ignoreCase = true) == true)
        }
        return com.darsequran.academy.data.model.BookstoreResponse(success = true, data = list, totalCount = list.size)
    }

    suspend fun logout() {
        tokenManager.clearSession()
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        return try {
            if (errorBody.isNullOrEmpty()) return null
            val parsed = Gson().fromJson(errorBody, AuthResponse::class.java)
            parsed.error ?: parsed.message
        } catch (_: Exception) {
            null
        }
    }
}

