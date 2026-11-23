package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val id: Int = 1, // 1 for "Last Read" (Terakhir Baca)
    val surahId: Int,
    val surahName: String,
    val ayatNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasbih")
data class Tasbih(
    @PrimaryKey val id: Int = 1,
    val currentCount: Int = 0,
    val targetCount: Int = 33,
    val dhikrName: String = "Subhanallah"
)

@Entity(tableName = "prayer_settings")
data class PrayerSetting(
    @PrimaryKey val prayerName: String, // "Subuh", "Zuhur", "Asar", "Maghrib", "Isya"
    val isNotificationEnabled: Boolean = true,
    val alertType: String = "ADZAN" // "ADZAN", "BEEP", "SILENT"
)
