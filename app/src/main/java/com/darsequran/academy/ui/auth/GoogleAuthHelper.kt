package com.darsequran.academy.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.darsequran.academy.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GoogleAuthHelper(
    private val context: Context,
    private val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID
) {

    private val credentialManager = CredentialManager.create(context)

    fun launchGoogleSignIn(
        scope: CoroutineScope,
        onTokenReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onTokenReceived(googleIdTokenCredential.idToken)
                } else {
                    onError("Unexpected credential response")
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed Google Sign-In sheet
            } catch (e: GetCredentialException) {
                onError(e.localizedMessage ?: "Google Sign-In failed")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "An error occurred during Google Sign-In")
            }
        }
    }
}
