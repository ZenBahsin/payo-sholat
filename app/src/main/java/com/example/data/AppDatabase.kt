package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Bookmark::class, Tasbih::class, PrayerSetting::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun prayerSettingDao(): PrayerSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "umat_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default values synchronously inside the creation transaction using raw SQL
                        db.execSQL("INSERT OR REPLACE INTO prayer_settings (prayerName, isNotificationEnabled, alertType) VALUES ('Subuh', 1, 'ADZAN')")
                        db.execSQL("INSERT OR REPLACE INTO prayer_settings (prayerName, isNotificationEnabled, alertType) VALUES ('Zuhur', 1, 'ADZAN')")
                        db.execSQL("INSERT OR REPLACE INTO prayer_settings (prayerName, isNotificationEnabled, alertType) VALUES ('Asar', 1, 'ADZAN')")
                        db.execSQL("INSERT OR REPLACE INTO prayer_settings (prayerName, isNotificationEnabled, alertType) VALUES ('Maghrib', 1, 'ADZAN')")
                        db.execSQL("INSERT OR REPLACE INTO prayer_settings (prayerName, isNotificationEnabled, alertType) VALUES ('Isya', 1, 'ADZAN')")
                        
                        db.execSQL("INSERT OR REPLACE INTO tasbih (id, currentCount, targetCount, dhikrName) VALUES (1, 0, 33, 'Subhanallah')")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
