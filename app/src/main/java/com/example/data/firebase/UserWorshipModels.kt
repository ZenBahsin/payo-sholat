package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "Sahabat Ibadah",
    val email: String? = null,
    val avatarUrl: String? = null,
    val city: String = "Palembang",
    val region: String? = "Sumatera Selatan",
    val countryCode: String = "ID",
    val jakimZone: String? = null,
    val timeZoneId: String = "Asia/Jakarta",
    val latitude: Double = -2.9761,
    val longitude: Double = 104.7754,
    val notificationPrefs: Map<String, Any>? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrayerLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prayerDate: String = "",        // "YYYY-MM-DD"
    val prayerName: String = "",        // "subuh", "zuhur", "asar", "maghrib", "isya"
    val completed: Boolean = false,     // Never prefix with "is" for Firestore compatibility
    val prayMode: String = "sendiri",   // "sendiri" | "berjamaah_masjid"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuranProgressDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lastSurahNumber: Int = 0,
    val lastSurahName: String = "",
    val lastAyah: Int = 0,
    val totalAyahsRead: Int = 0,
    val khatamPercentage: Int = 0,
    val bookmarks: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TasbihSessionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dzikirTitle: String = "Subhanallah",
    val currentCount: Int = 0,
    val targetCount: Int = 33,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object WorshipDateUtils {
    val obligatoryPrayers = listOf("subuh", "zuhur", "asar", "maghrib", "isya")

    fun formatDateKey(cal: Calendar, timeZone: TimeZone = cal.timeZone): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        return sdf.format(cal.time)
    }

    fun prayerLogDocId(prayerDate: String, prayerName: String): String {
        return "${prayerDate}_${prayerName.lowercase(Locale.US)}"
    }

    /**
     * Computes consecutive days with >= 1 completed obligatory prayer ending today (or yesterday if today hasn't started yet).
     */
    fun calculatePrayerStreak(
        logs: List<PrayerLogDoc>,
        todayCal: Calendar,
        timeZone: TimeZone
    ): Int {
        val completedDates = logs
            .filter { it.completed && it.prayerName in obligatoryPrayers }
            .map { it.prayerDate }
            .toSet()

        if (completedDates.isEmpty()) return 0

        val cursor = (todayCal.clone() as Calendar).apply {
            this.timeZone = timeZone
        }
        val todayKey = formatDateKey(cursor, timeZone)

        var streak = 0
        // If user hasn't checked any prayer today yet, check starting from yesterday so active streaks don't drop at 00:01
        if (todayKey !in completedDates) {
            cursor.add(Calendar.DAY_OF_MONTH, -1)
        }

        while (true) {
            val key = formatDateKey(cursor, timeZone)
            if (key in completedDates) {
                streak += 1
                cursor.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }
        return streak
    }

    // Standard cumulative ayah counts prior to each Surah (1..114) for accurate Khatam % calculation (6236 total ayahs)
    private val surahAyahCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    const val TOTAL_QURAN_AYAHS = 6236

    fun computeCumulativeAyahs(surahNumber: Int, ayahNumber: Int): Int {
        if (surahNumber !in 1..114) return 0
        var sum = 0
        for (i in 0 until (surahNumber - 1)) {
            sum += surahAyahCounts[i]
        }
        val clampedAyah = ayahNumber.coerceIn(0, surahAyahCounts[surahNumber - 1])
        return (sum + clampedAyah).coerceIn(0, TOTAL_QURAN_AYAHS)
    }

    fun computeKhatamPercentage(totalAyahsRead: Int): Int {
        if (totalAyahsRead <= 0) return 0
        return ((totalAyahsRead.toDouble() / TOTAL_QURAN_AYAHS.toDouble()) * 100.0)
            .toInt()
            .coerceIn(1, 100)
    }
}
