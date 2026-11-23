package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    fun getBookmarkById(id: Int): Flow<Bookmark?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)
}

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih WHERE id = 1 LIMIT 1")
    fun getTasbih(): Flow<Tasbih?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTasbih(tasbih: Tasbih)
}

@Dao
interface PrayerSettingDao {
    @Query("SELECT * FROM prayer_settings")
    fun getAllSettings(): Flow<List<PrayerSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: PrayerSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: List<PrayerSetting>)
}
