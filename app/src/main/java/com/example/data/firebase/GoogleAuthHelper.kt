package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface AuthOutcome {
    data class Success(val user: FirebaseUser) : AuthOutcome
    data object Cancelled : AuthOutcome
    data class Error(val message: String) : AuthOutcome
}

/**
 * Jetpack Credential Manager + Firebase Auth helper using Google Sign-In.
 * Strictly follows single-option-per-request invariant and reads default_web_client_id from resources.
 */
object GoogleAuthHelper {

    private const val TAG = "Auth"

    /**
     * Resolves the Google Services generated `default_web_client_id` string resource at runtime.
     * Avoids compile-time breakage in CI environments where `app/google-services.json` is injected via secrets.
     */
    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return context.getString(resId).trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Silent auto sign-in for returning users using GetGoogleIdOption (single option in request).
     */
    suspend fun attemptAutoSignIn(activityContext: Context): AuthOutcome {
        if (Firebase.auth.currentUser != null) {
            return AuthOutcome.Success(Firebase.auth.currentUser!!)
        }
        return try {
            val webClientId = resolveWebClientId(activityContext)
            if (webClientId == null) {
                Log.w(TAG, "default_web_client_id not found; ensure app/google-services.json is configured.")
                return AuthOutcome.Cancelled
            }
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .setServerClientId(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredential(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No previously authorized Google credential found for silent sign-in.")
            AuthOutcome.Cancelled
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google sign-in cancelled by user: ${e.message}")
            AuthOutcome.Cancelled
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Silent Google sign-in unavailable: ${e.message}")
            AuthOutcome.Cancelled
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google sign-in skipped: ${e.message}")
            AuthOutcome.Cancelled
        }
    }

    /**
     * Interactive Google Sign-In triggered by tapping "Lanjutkan dengan Google".
     * Always uses GetSignInWithGoogleOption as the single option in GetCredentialRequest.
     */
    suspend fun signInWithGoogle(activityContext: Context): AuthOutcome {
        return try {
            val webClientId = resolveWebClientId(activityContext)
            if (webClientId == null) {
                Log.e(TAG, "default_web_client_id missing; app/google-services.json was not present during build.")
                return AuthOutcome.Error("Konfigurasi Firebase (google-services.json) belum terpasang pada build APK ini.")
            }
            val credentialManager = CredentialManager.create(activityContext)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredential(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google sign-in cancelled by user", e)
            AuthOutcome.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Credential Manager failed: ${e.type} - ${e.message}", e)
            AuthOutcome.Error("Gagal masuk dengan Google. Pastikan layanan Google Play aktif di perangkat ini.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google sign-in", e)
            AuthOutcome.Error(e.localizedMessage ?: "Terjadi kesalahan saat masuk dengan Google.")
        }
    }

    private suspend fun exchangeGoogleCredential(credential: androidx.credentials.Credential): AuthOutcome {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            return if (user != null) {
                AuthOutcome.Success(user)
            } else {
                AuthOutcome.Error("Gagal memverifikasi akun Google ke Firebase.")
            }
        }
        Log.e(TAG, "Unexpected credential type returned: ${credential.type}")
        return AuthOutcome.Error("Format kredensial Google tidak dikenali.")
    }

    /**
     * Safe sign-out: runs in NonCancellable to clear Credential Manager state before signing out of Firebase Auth.
     */
    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                val credentialManager = CredentialManager.create(context.applicationContext)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear CredentialManager state during signOut: ${e.message}")
            } finally {
                Firebase.auth.signOut()
            }
        }
    }
}
