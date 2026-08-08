package com.example.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

object GoogleSignInHelper {

    // Default Web Client ID from google-services.json
    const val FALLBACK_WEB_CLIENT_ID = "257724909113-t8e9fdebl946dl72kuqr9m51ghrt5g2u.apps.googleusercontent.com"

    private fun getWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val found = context.getString(resId)
                if (found.isNotBlank()) found else FALLBACK_WEB_CLIENT_ID
            } else {
                FALLBACK_WEB_CLIENT_ID
            }
        } catch (e: Exception) {
            FALLBACK_WEB_CLIENT_ID
        }
    }

    suspend fun launchGoogleSignIn(
        context: Context,
        authManager: FirebaseAuthManager
    ): Result<UserSession> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = getWebClientId(context)

            // Generate raw nonce for Google ID Option
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                authManager.signInWithGoogleIdToken(idToken)
            } else {
                Result.failure(Exception("Unsupported credential type returned"))
            }
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In cancelled or unavailable: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
