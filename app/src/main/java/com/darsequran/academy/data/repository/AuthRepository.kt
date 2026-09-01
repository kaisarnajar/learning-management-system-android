package com.darsequran.academy.data.repository

import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.model.AuthResponse
import com.darsequran.academy.data.model.GoogleLoginRequest
import com.darsequran.academy.data.model.LoginRequest
import com.darsequran.academy.data.remote.AuthApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.IOException

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

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
        } catch (e: IOException) {
            NetworkResult.Error("Network error. Please check your internet connection.")
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
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
        } catch (e: IOException) {
            NetworkResult.Error("Network error. Please check your internet connection.")
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun logout() {
        tokenManager.clearSession()
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        return try {
            if (errorBody.isNull_or_Empty()) return null
            val parsed = Gson().fromJson(errorBody, AuthResponse::class.java)
            parsed.message
        } catch (e: Exception) {
            null
        }
    }

    private fun String?.isNull_or_Empty(): Boolean = this.isNullOrEmpty()
}
