package com.example.data

import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    fun getLastReadBookmark(): Flow<Bookmark?> = bookmarkDao.getBookmarkById(1)

    suspend fun saveBookmark(surahId: Int, surahName: String, ayatNumber: Int) {
        bookmarkDao.insertBookmark(
            Bookmark(
                id = 1,
                surahId = surahId,
                surahName = surahName,
                ayatNumber = ayatNumber
            )
        )
    }

    suspend fun deleteBookmark() {
        bookmarkDao.deleteBookmark(1)
    }
}

class TasbihRepository(private val tasbihDao: TasbihDao) {
    fun getTasbih(): Flow<Tasbih?> = tasbihDao.getTasbih()

    suspend fun saveTasbih(tasbih: Tasbih) {
        tasbihDao.saveTasbih(tasbih)
    }
}

class PrayerSettingRepository(private val prayerSettingDao: PrayerSettingDao) {
    fun getAllSettings(): Flow<List<PrayerSetting>> = prayerSettingDao.getAllSettings()

    suspend fun saveSetting(setting: PrayerSetting) {
        prayerSettingDao.saveSetting(setting)
    }
}
