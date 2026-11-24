package com.darsequran.academy.data.repo

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.SessionStore
import com.darsequran.academy.data.apiCall
import com.darsequran.academy.data.remote.AcademyApi
import com.darsequran.academy.data.remote.ChangePasswordRequest
import com.darsequran.academy.data.remote.DeleteAccountRequest
import com.darsequran.academy.data.remote.EmailRequest
import com.darsequran.academy.data.remote.GoogleRequest
import com.darsequran.academy.data.remote.LoginRequest
import com.darsequran.academy.data.remote.Profile
import com.darsequran.academy.data.remote.ProfileUpdate
import com.darsequran.academy.data.remote.RefreshRequest
import com.darsequran.academy.data.remote.RegisterRequest
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepository(private val api: AcademyApi, private val sessionStore: SessionStore) {

    val session get() = sessionStore.session

    private suspend fun signedIn(result: ApiResult<TokenResponse>): ApiResult<Unit> = when (result) {
        is ApiResult.Ok -> { sessionStore.save(result.value); ApiResult.Ok(Unit) }
        is ApiResult.Err -> result
    }

    suspend fun login(email: String, password: String) =
        signedIn(apiCall { api.login(LoginRequest(email.trim(), password)) })

    suspend fun register(name: String, email: String, password: String) =
        signedIn(apiCall { api.register(RegisterRequest(name.trim().ifBlank { null }, email.trim(), password)) })

    suspend fun google(idToken: String) = signedIn(apiCall { api.google(GoogleRequest(idToken)) })

    suspend fun forgotPassword(email: String) = apiCall { api.forgotPassword(EmailRequest(email.trim())) }

    suspend fun resendVerification(email: String) = apiCall { api.resendVerification(EmailRequest(email.trim())) }

    /**
     * Signs out: tells the server to forget the refresh token (best effort, for at most [LOGOUT_REVOKE_TIMEOUT_MS]),
     * then forgets the session here. Not cancellable: leaving the screen mid-way must not leave the user signed in.
     */
    suspend fun logout(): Unit = withContext(NonCancellable) {
        val refresh = sessionStore.session.value?.refreshToken
        if (refresh != null) {
            withTimeoutOrNull(LOGOUT_REVOKE_TIMEOUT_MS) { apiCall { api.logout(RefreshRequest(refresh)) } }
        }
        sessionStore.clear()
    }

    suspend fun profile(): ApiResult<Profile> = apiCall { api.me().user }.also { result ->
        if (result is ApiResult.Ok) syncSessionUser(result.value)
    }

    suspend fun updateProfile(update: ProfileUpdate): ApiResult<Profile> =
        apiCall { api.updateProfile(update).user }.also { if (it is ApiResult.Ok) syncSessionUser(it.value) }

    suspend fun uploadPhoto(bytes: ByteArray, mime: String): ApiResult<String?> = apiCall {
        val part = MultipartBody.Part.createFormData("image", "photo.jpg", bytes.toRequestBody(mime.toMediaType()))
        api.uploadPhoto(part).image
    }

    suspend fun removePhoto() = apiCall { api.removePhoto().image }

    suspend fun changePassword(current: String, new: String) =
        apiCall { api.changePassword(ChangePasswordRequest(current, new)) }

    /** Deletes the account; [beforeSignOut] runs once the server has deleted it, before the session here is cleared. */
    suspend fun deleteAccount(password: String?, confirm: String?, beforeSignOut: () -> Unit = {}): ApiResult<Unit> =
        when (val r = apiCall { api.deleteAccount(DeleteAccountRequest(password, confirm)) }) {
            is ApiResult.Ok -> { beforeSignOut(); sessionStore.clear(); ApiResult.Ok(Unit) }
            is ApiResult.Err -> r
        }

    private suspend fun syncSessionUser(profile: Profile) {
        sessionStore.updateUser(
            SessionUser(
                id = profile.id,
                email = profile.email,
                name = profile.name,
                image = profile.image,
                role = profile.role,
                registrationNumber = profile.registrationNumber,
            ),
        )
    }

    private companion object {
        /** Offline, the revoke would wait for the connect timeout; signing out shouldn't. */
        const val LOGOUT_REVOKE_TIMEOUT_MS = 5_000L
    }
}
