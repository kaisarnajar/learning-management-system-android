package com.darsequran.academy.data.repository

import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.model.AuthResponse
import com.darsequran.academy.data.model.CoursesResponse
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
