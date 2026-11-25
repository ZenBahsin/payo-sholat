package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

/**
 * Safely detects whether FirebaseApp was initialized (i.e. app/google-services.json was bundled at build time).
 * Prevents startup IllegalStateException crashes on APKs compiled in CI without google-services.json.
 */
object FirebaseRuntimeGuard {

    private const val TAG = "FirebaseRuntimeGuard"
    const val LOCAL_FALLBACK_UID = "local_offline_device"

    fun isFirebaseInitialized(context: Context): Boolean {
        val appContext = context.applicationContext
        return try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                true
            } else {
                FirebaseApp.initializeApp(appContext) != null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "FirebaseApp is not initialized (missing google-services.json): ${t.message}")
            false
        }
    }

    fun authOrNull(context: Context): FirebaseAuth? {
        if (!isFirebaseInitialized(context)) return null
        return try {
            Firebase.auth
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to access FirebaseAuth: ${t.message}")
            null
        }
    }

    fun currentUserOrNull(context: Context): FirebaseUser? {
        return authOrNull(context)?.currentUser
    }
}
