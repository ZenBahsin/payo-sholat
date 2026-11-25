package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

/**
 * Structured Firestore error logger with current Firebase Auth context
 * for rapid debugging of security rules and query failures.
 */
fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val user = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providerArray = JSONArray()
    user?.providerData?.forEach { info ->
        providerArray.put(
            JSONObject().apply {
                put("providerId", info.providerId)
                put("displayName", info.displayName)
                put("email", info.email)
            }
        )
    }

    val firestoreCode = (error as? FirebaseFirestoreException)?.code?.name

    val payload = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("firestoreCode", firestoreCode)
        put("operationType", operationType.name)
        put("path", path)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", user?.uid)
                put("email", user?.email)
                put("emailVerified", user?.isEmailVerified)
                put("isAnonymous", user?.isAnonymous)
                put("providerInfo", providerArray)
            }
        )
    }

    Log.e("FirestoreError", payload.toString())

    return when ((error as? FirebaseFirestoreException)?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Akses ditolak. Pastikan kamu sudah masuk dengan akun Google."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Koneksi tidak stabil. Perubahan akan disinkronkan saat online."
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Sesi berakhir. Silakan masuk kembali."
        else ->
            error.localizedMessage ?: "Terjadi kendala saat menyinkronkan data ibadah."
    }
}
