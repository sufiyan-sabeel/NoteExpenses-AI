package com.example.data.remote

import android.content.Context
import com.example.data.model.UserSession
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _currentUserState = MutableStateFlow(buildSession(null))
    val currentUserState: StateFlow<UserSession> = _currentUserState.asStateFlow()

    init {
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUserState.value = buildSession(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildSession(user: FirebaseUser?): UserSession {
        return if (user != null) {
            UserSession(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "User",
                photoUrl = user.photoUrl?.toString() ?: "",
                isAuthenticated = true,
                isEmailVerified = user.isEmailVerified
            )
        } else {
            UserSession(
                uid = "guest_user_123",
                email = "offline@notesexpenses.app",
                displayName = "Offline Notes User",
                isAuthenticated = true,
                isEmailVerified = true
            )
        }
    }

    /**
     * Sign in with Email & Password.
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<UserSession> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val session = buildSession(result.user)
            _currentUserState.value = session
            Result.success(session)
        } catch (e: Exception) {
            val friendlyMsg = formatFirebaseError(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Sign up with Email & Password and send Email Verification.
     */
    suspend fun signUpWithEmail(email: String, pass: String): Result<UserSession> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user
            try {
                user?.sendEmailVerification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val session = buildSession(user)
            _currentUserState.value = session
            Result.success(session)
        } catch (e: Exception) {
            val friendlyMsg = formatFirebaseError(e)
            Result.failure(Exception(friendlyMsg))
        }
    }

    private fun formatFirebaseError(e: Exception): String {
        val msg = e.localizedMessage ?: e.message ?: "Authentication failed"
        return when {
            msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("provider is disabled", ignoreCase = true) -> {
                "Firebase Auth is not enabled in Firebase Console. Please enable 'Email/Password' under Firebase Console > Authentication > Sign-in method, or use 'Continue Offline as Guest'."
            }
            msg.contains("INVALID_EMAIL", ignoreCase = true) -> "Invalid email address format."
            msg.contains("WRONG_PASSWORD", ignoreCase = true) || msg.contains("INVALID_PASSWORD", ignoreCase = true) -> "Invalid email or password."
            msg.contains("USER_NOT_FOUND", ignoreCase = true) -> "No account found with this email. Please sign up."
            msg.contains("EMAIL_EXISTS", ignoreCase = true) -> "Email is already registered. Please sign in instead."
            else -> msg
        }
    }

    /**
     * Google Sign-In with ID Token (e.g. from Credential Manager or Google Sign In ID Token).
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserSession> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val session = buildSession(result.user)
            _currentUserState.value = session
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send password reset link to user email.
     */
    suspend fun sendForgotPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send email verification to currently logged in user.
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reauthenticate user before sensitive actions like account deletion.
     */
    suspend fun reauthenticate(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            val email = user.email ?: return Result.failure(Exception("No user email found"))
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user account from Firebase Auth.
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.delete().await()
            _currentUserState.value = buildSession(null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user.
     */
    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUserState.value = buildSession(null)
    }

    /**
     * Google Sign In quick fallback session helper.
     */
    fun simulateGoogleSignIn(email: String, name: String): UserSession {
        val session = UserSession(
            uid = "google_" + System.currentTimeMillis(),
            email = email,
            displayName = name,
            isAuthenticated = true,
            isEmailVerified = true
        )
        _currentUserState.value = session
        return session
    }
}
