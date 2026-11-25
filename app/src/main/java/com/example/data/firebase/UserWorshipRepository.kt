package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.LocationPreset
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Per-user Cloud Firestore repository for Payo Shalat.
 * Always connects to the provisioned named Firestore database via R.string.firestore_database_id.
 */
class UserWorshipRepository(context: Context) {

    private val appContext = context.applicationContext

    private val db: FirebaseFirestore by lazy {
        val databaseId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private fun requireUserId(): String =
        FirebaseRuntimeGuard.currentUserOrNull(appContext)?.uid
            ?: throw IllegalStateException("Pengguna belum masuk ke akun Google.")

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

    private fun prayerLogsCol(uid: String) = userDocRef(uid).collection("prayer_logs")

    private fun quranProgressDoc(uid: String) = userDocRef(uid).collection("quran_progress").document("main")

    private fun tasbihSessionDoc(uid: String) = userDocRef(uid).collection("tasbih_sessions").document("active")

    // =========================================================================
    // Real-time Snapshot Streams (ServerTimestampBehavior.ESTIMATE + Safe Unwrap)
    // =========================================================================

    fun observeUserProfile(uid: String): Flow<UiState<UserProfileDoc?>> {
        val path = "users/$uid"
        return userDocRef(uid)
            .snapshots()
            .map<DocumentSnapshot, UiState<UserProfileDoc?>> { snap ->
                val profile = if (snap.exists()) {
                    snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
                UiState.Success(profile)
            }
            .catch { e ->
                val cause = (e as? CancellationException)?.cause ?: e
                if (e is CancellationException && cause !is FirebaseFirestoreException) throw e
                val msg = handleFirestoreError(cause, OperationType.GET, path)
                emit(UiState.Error(msg))
            }
    }

    fun observePrayerLogs(uid: String): Flow<UiState<List<PrayerLogDoc>>> {
        val path = "users/$uid/prayer_logs"
        return prayerLogsCol(uid)
            .orderBy("prayerDate", Query.Direction.DESCENDING)
            .limit(450) // ~90 days x 5 obligatory prayers
            .snapshots()
            .map<com.google.firebase.firestore.QuerySnapshot, UiState<List<PrayerLogDoc>>> { snap ->
                val items = snap.documents.mapNotNull { doc ->
                    doc.toObject(PrayerLogDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                UiState.Success(items)
            }
            .catch { e ->
                val cause = (e as? CancellationException)?.cause ?: e
                if (e is CancellationException && cause !is FirebaseFirestoreException) throw e
                val msg = handleFirestoreError(cause, OperationType.LIST, path)
                emit(UiState.Error(msg))
            }
    }

    fun observeQuranProgress(uid: String): Flow<UiState<QuranProgressDoc?>> {
        val path = "users/$uid/quran_progress/main"
        return quranProgressDoc(uid)
            .snapshots()
            .map<DocumentSnapshot, UiState<QuranProgressDoc?>> { snap ->
                val doc = if (snap.exists()) {
                    snap.toObject(QuranProgressDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
                UiState.Success(doc)
            }
            .catch { e ->
                val cause = (e as? CancellationException)?.cause ?: e
                if (e is CancellationException && cause !is FirebaseFirestoreException) throw e
                val msg = handleFirestoreError(cause, OperationType.GET, path)
                emit(UiState.Error(msg))
            }
    }

    fun observeTasbihSession(uid: String): Flow<UiState<TasbihSessionDoc?>> {
        val path = "users/$uid/tasbih_sessions/active"
        return tasbihSessionDoc(uid)
            .snapshots()
            .map<DocumentSnapshot, UiState<TasbihSessionDoc?>> { snap ->
                val doc = if (snap.exists()) {
                    snap.toObject(TasbihSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
                UiState.Success(doc)
            }
            .catch { e ->
                val cause = (e as? CancellationException)?.cause ?: e
                if (e is CancellationException && cause !is FirebaseFirestoreException) throw e
                val msg = handleFirestoreError(cause, OperationType.GET, path)
                emit(UiState.Error(msg))
            }
    }

    // =========================================================================
    // Crash-Safe Mutations (Strict 1:1 Parity with firestore.rules)
    // =========================================================================

    suspend fun ensureUserProfile(
        user: FirebaseUser,
        defaultLocation: LocationPreset
    ): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = userDocRef(uid)
            val existing = docRef.get().await()
            val safeName = (user.displayName?.trim()?.take(100)).takeIf { !it.isNullOrBlank() }
                ?: user.email?.substringBefore("@")?.take(100)
                ?: "Sahabat Ibadah"
            val safeEmail = user.email?.trim()?.take(150)
            val safeAvatar = user.photoUrl?.toString()?.trim()?.take(500)
            val countryCode = if (defaultLocation.country.code == "MY") "MY" else "ID"

            if (!existing.exists()) {
                val createData = buildMap<String, Any?> {
                    put("userId", uid)
                    put("fullName", safeName)
                    put("email", safeEmail)
                    put("avatarUrl", safeAvatar)
                    put("city", defaultLocation.name.take(100).ifBlank { "Palembang" })
                    put("region", defaultLocation.region.take(100).ifBlank { null })
                    put("countryCode", countryCode)
                    put("jakimZone", defaultLocation.jakimZone?.take(20))
                    put("timeZoneId", defaultLocation.timeZoneId.take(60).ifBlank { "Asia/Jakarta" })
                    put("latitude", defaultLocation.lat.coerceIn(-90.0, 90.0))
                    put("longitude", defaultLocation.lng.coerceIn(-180.0, 180.0))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                docRef.set(createData).await()
            } else {
                // Keep profile identity fresh without overwriting user's saved city/prefs or createdAt
                val updateData = buildMap<String, Any?> {
                    put("fullName", safeName)
                    if (safeEmail != null) put("email", safeEmail)
                    if (safeAvatar != null) put("avatarUrl", safeAvatar)
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                docRef.update(updateData).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserLocationAndPrefs(
        location: LocationPreset,
        notificationPrefs: Map<String, String>? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid"
        return try {
            val docRef = userDocRef(uid)
            val existing = docRef.get().await()
            val countryCode = if (location.country.code == "MY") "MY" else "ID"
            val trimmedPrefs = notificationPrefs?.entries?.take(15)?.associate { it.key to it.value }

            if (existing.exists()) {
                val updateData = mutableMapOf<String, Any?>(
                    "city" to location.name.take(100).ifBlank { "Palembang" },
                    "region" to location.region.take(100).ifBlank { null },
                    "countryCode" to countryCode,
                    "jakimZone" to location.jakimZone?.take(20),
                    "timeZoneId" to location.timeZoneId.take(60).ifBlank { "Asia/Jakarta" },
                    "latitude" to location.lat.coerceIn(-90.0, 90.0),
                    "longitude" to location.lng.coerceIn(-180.0, 180.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (trimmedPrefs != null) {
                    updateData["notificationPrefs"] = trimmedPrefs
                }
                docRef.update(updateData).await()
            } else {
                val user = FirebaseRuntimeGuard.currentUserOrNull(appContext)
                val safeName = user?.displayName?.trim()?.take(100).takeIf { !it.isNullOrBlank() } ?: "Sahabat Ibadah"
                val createData = buildMap<String, Any?> {
                    put("userId", uid)
                    put("fullName", safeName)
                    put("email", user?.email?.take(150))
                    put("avatarUrl", user?.photoUrl?.toString()?.take(500))
                    put("city", location.name.take(100).ifBlank { "Palembang" })
                    put("region", location.region.take(100).ifBlank { null })
                    put("countryCode", countryCode)
                    put("jakimZone", location.jakimZone?.take(20))
                    put("timeZoneId", location.timeZoneId.take(60).ifBlank { "Asia/Jakarta" })
                    put("latitude", location.lat.coerceIn(-90.0, 90.0))
                    put("longitude", location.lng.coerceIn(-180.0, 180.0))
                    if (trimmedPrefs != null) put("notificationPrefs", trimmedPrefs)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                docRef.set(createData).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertPrayerLog(
        prayerDate: String,
        prayerName: String,
        completed: Boolean,
        prayMode: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val normalizedName = prayerName.lowercase(Locale.US)
        if (normalizedName !in WorshipDateUtils.obligatoryPrayers) {
            return Result.failure(IllegalArgumentException("Nama shalat tidak valid: $prayerName"))
        }
        val safeMode = if (prayMode == "berjamaah_masjid") "berjamaah_masjid" else "sendiri"
        val docId = WorshipDateUtils.prayerLogDocId(prayerDate, normalizedName)
        val path = "users/$uid/prayer_logs/$docId"

        return try {
            val docRef = prayerLogsCol(uid).document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                // Strictly only mutable fields allowed by firestore.rules
                docRef.update(
                    mapOf(
                        "completed" to completed,
                        "prayMode" to safeMode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val createData = mapOf(
                    "userId" to uid,
                    "prayerDate" to prayerDate,
                    "prayerName" to normalizedName,
                    "completed" to completed,
                    "prayMode" to safeMode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createData).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveQuranProgress(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        totalAyahsRead: Int,
        khatamPercentage: Int,
        bookmarks: List<String> = emptyList()
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/quran_progress/main"
        return try {
            val docRef = quranProgressDoc(uid)
            val existing = docRef.get().await()
            val safeSurahNum = surahNumber.coerceIn(0, 114)
            val safeSurahName = surahName.take(100)
            val safeAyah = ayahNumber.coerceIn(0, 300)
            val safeTotal = totalAyahsRead.coerceIn(0, 6236)
            val safePct = khatamPercentage.coerceIn(0, 100)
            val safeBookmarks = bookmarks.take(50)

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "lastSurahNumber" to safeSurahNum,
                        "lastSurahName" to safeSurahName,
                        "lastAyah" to safeAyah,
                        "totalAyahsRead" to safeTotal,
                        "khatamPercentage" to safePct,
                        "bookmarks" to safeBookmarks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "lastSurahNumber" to safeSurahNum,
                        "lastSurahName" to safeSurahName,
                        "lastAyah" to safeAyah,
                        "totalAyahsRead" to safeTotal,
                        "khatamPercentage" to safePct,
                        "bookmarks" to safeBookmarks,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTasbihSession(
        dzikirTitle: String,
        currentCount: Int,
        targetCount: Int
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/tasbih_sessions/active"
        return try {
            val docRef = tasbihSessionDoc(uid)
            val existing = docRef.get().await()
            val safeTitle = dzikirTitle.trim().take(120).ifBlank { "Subhanallah" }
            val safeCount = currentCount.coerceIn(0, 1_000_000)
            val safeTarget = targetCount.coerceIn(1, 100_000)

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "dzikirTitle" to safeTitle,
                        "currentCount" to safeCount,
                        "targetCount" to safeTarget,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dzikirTitle" to safeTitle,
                        "currentCount" to safeCount,
                        "targetCount" to safeTarget,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
