package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.LocationPreset
import com.example.data.PrayerCountry
import com.example.data.PrayerLocationCatalog
import com.example.data.PrayerSetting
import com.example.data.api.EquranApiClient
import com.example.data.api.EquranSholatRequest
import com.example.data.api.MalaysiaPrayerApi
import com.example.utils.PrayerTimesCalculator.PrayerTimes
import com.squareup.moshi.Moshi
import java.util.Calendar
import java.util.TimeZone

object PrayerAlarmScheduler {
    private const val TAG = "PrayerAlarmScheduler"

    private val prayerRequestCodes = mapOf(
        "Imsak" to 1000,
        "Subuh" to 1001,
        "Syuruq" to 1002,
        "Zuhur" to 1003,
        "Asar" to 1004,
        "Maghrib" to 1005,
        "Isya" to 1006
    )

    fun scheduleAlarms(
        context: Context,
        times: PrayerTimes,
        settings: List<PrayerSetting>,
        azanSound: String
    ) {
        if (times.zuhur.isEmpty() || times.subuh.isEmpty()) {
            Log.d(TAG, "Cannot schedule: Official API prayer times are not loaded yet")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prayerNames = listOf("Imsak", "Subuh", "Syuruq", "Zuhur", "Asar", "Maghrib", "Isya")
        val prayerTimeStrings = listOf(
            times.imsak,
            times.subuh,
            times.syuruq,
            times.zuhur,
            times.asar,
            times.maghrib,
            times.isya
        )

        val timeZone = PrayerLocationCatalog.resolveTimeZone(context)
        val nowMs = System.currentTimeMillis()

        for (i in prayerNames.indices) {
            val name = prayerNames[i]
            val timeStr = prayerTimeStrings[i]
            if (timeStr.isEmpty()) continue

            val setting = settings.find { it.prayerName.equals(name, ignoreCase = true) }
                ?: PrayerSetting(name, true, "ADZAN")

            val alertType = if (!setting.isNotificationEnabled) "SILENT" else setting.alertType
            val triggerTime = getPrayerTimeInMillis(timeStr, timeZone)
            if (triggerTime == 0L) continue

            val scheduledTime = if (triggerTime <= nowMs) {
                triggerTime + 24 * 60 * 60 * 1000L
            } else {
                triggerTime
            }

            val intent = Intent().apply {
                setClassName(context.packageName, "com.example.receiver.PrayerAlarmReceiver")
                action = "com.example.ACTION_PRAYER_ALARM"
                putExtra("prayer_name", name)
                putExtra("alert_type", alertType)
                putExtra("azan_sound", azanSound)
            }

            val requestCode = prayerRequestCodes[name] ?: (1000 + i)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            setAlarm(alarmManager, scheduledTime, pendingIntent)
            Log.d(TAG, "Scheduled alarm ($alertType) for $name at ${Calendar.getInstance(timeZone).apply { timeInMillis = scheduledTime }.time}")
        }

        com.example.receiver.PrayerWidgetProvider.updateAllWidgets(context)
    }

    private fun getPrayerTimeInMillis(timeStr: String, timeZone: TimeZone): Long {
        return try {
            val parts = timeStr.split(":")
            if (parts.size < 2) return 0L
            val hour = parts[0].toInt()
            val min = parts[1].toInt()

            val cal = Calendar.getInstance(timeZone).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            0L
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun locationCacheId(location: LocationPreset): String {
        return if (location.country == PrayerCountry.MALAYSIA) {
            "MY_${location.jakimZone?.uppercase() ?: "WLY01"}"
        } else {
            "ID_${location.name}"
        }
    }

    /**
     * Persists ONLY verified official API prayer times (Kemenag RI / JAKIM e-Solat).
     * Never used for manual astronomical calculations.
     */
    fun saveOfficialDailyPrayerTimesSync(
        context: Context,
        location: LocationPreset,
        year: Int,
        month: Int,
        day: Int,
        times: PrayerTimes
    ) {
        if (times.zuhur.isEmpty() || times.subuh.isEmpty()) return
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val activeKey = "official_daily_${locationCacheId(location)}_${year}_${month}_${day}"
        val packed = "${times.imsak}|${times.subuh}|${times.syuruq}|${times.zuhur}|${times.asar}|${times.maghrib}|${times.isya}"
        prefs.edit().putString(activeKey, packed).apply()
    }

    fun getCachedPrayerTimesSync(
        context: Context,
        location: LocationPreset,
        year: Int,
        month: Int,
        day: Int
    ): PrayerTimes? {
        if (location.country == PrayerCountry.MALAYSIA) {
            val zone = location.jakimZone?.takeIf { it.isNotBlank() } ?: "WLY01"
            MalaysiaPrayerApi.getCachedDailyPrayerTimesSync(context, zone, year, month, day)?.let {
                return it.prayerTimes
            }
        } else {
            val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
            val cacheKey = "api_schedule_cache_${location.name}_${year}_${month}"
            val cachedJson = prefs.getString(cacheKey, null)
            if (cachedJson != null) {
                try {
                    val moshi = Moshi.Builder().build()
                    val adapter = moshi.adapter(com.example.data.api.EquranSholatResponse::class.java)
                    val response = adapter.fromJson(cachedJson)
                    if (response != null && response.code == 200 && response.data != null) {
                        val apiJadwal = response.data.jadwal.find { it.tanggal == day }
                        if (apiJadwal != null) {
                            return PrayerTimes(
                                imsak = apiJadwal.imsak,
                                subuh = apiJadwal.subuh,
                                syuruq = apiJadwal.terbit,
                                zuhur = apiJadwal.dzuhur,
                                asar = apiJadwal.ashar,
                                maghrib = apiJadwal.maghrib,
                                isya = apiJadwal.isya
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing cached Equran JSON", e)
                }
            }
        }

        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val activeKey = "official_daily_${locationCacheId(location)}_${year}_${month}_${day}"
        val packed = prefs.getString(activeKey, null)
        if (!packed.isNullOrBlank()) {
            val parts = packed.split("|")
            if (parts.size == 7 && parts[1].isNotEmpty() && parts[3].isNotEmpty()) {
                return PrayerTimes(
                    imsak = parts[0],
                    subuh = parts[1],
                    syuruq = parts[2],
                    zuhur = parts[3],
                    asar = parts[4],
                    maghrib = parts[5],
                    isya = parts[6]
                )
            }
        }
        return null
    }

    /**
     * Fetches official prayer schedule from:
     * - Malaysia: JAKIM e-Solat / Waktu Solat Malaysia API
     * - Indonesia: Kemenag RI via Equran.id API
     * Zero manual astronomical calculation.
     */
    suspend fun getPrayerTimesFromApiOrCache(
        context: Context,
        location: LocationPreset,
        year: Int,
        month: Int,
        day: Int
    ): PrayerTimes? {
        if (location.country == PrayerCountry.MALAYSIA) {
            val zone = location.jakimZone?.takeIf { it.isNotBlank() } ?: "WLY01"
            val result = MalaysiaPrayerApi.fetchDailyPrayerTimes(context, zone, year, month, day)
            if (result != null) {
                saveOfficialDailyPrayerTimesSync(context, location, year, month, day, result.prayerTimes)
                return result.prayerTimes
            }
            return null
        }

        // Indonesia: Equran.id Kemenag RI API
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val cacheKey = "api_schedule_cache_${location.name}_${year}_${month}"
        val cachedJson = prefs.getString(cacheKey, null)

        var apiData: com.example.data.api.EquranData? = null
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(com.example.data.api.EquranSholatResponse::class.java)

        if (cachedJson != null) {
            try {
                val response = adapter.fromJson(cachedJson)
                if (response != null && response.code == 200) {
                    apiData = response.data
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing cached Equran JSON", e)
            }
        }

        if (apiData == null) {
            val provinsiName = location.region.ifBlank {
                PrayerLocationCatalog.indonesiaPresets
                    .find { it.name.equals(location.name, ignoreCase = true) }?.region ?: "Jawa Barat"
            }

            try {
                val request = EquranSholatRequest(
                    provinsi = provinsiName,
                    kabkota = location.name,
                    bulan = month,
                    tahun = year
                )
                val response = EquranApiClient.service.getMonthlySchedule(request)
                if (response.code == 200 && response.data != null) {
                    apiData = response.data
                    val json = adapter.toJson(response)
                    prefs.edit().putString(cacheKey, json).apply()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Equran API", e)
            }
        }

        if (apiData != null) {
            val apiJadwal = apiData.jadwal.find { it.tanggal == day }
            if (apiJadwal != null) {
                val times = PrayerTimes(
                    imsak = apiJadwal.imsak,
                    subuh = apiJadwal.subuh,
                    syuruq = apiJadwal.terbit,
                    zuhur = apiJadwal.dzuhur,
                    asar = apiJadwal.ashar,
                    maghrib = apiJadwal.maghrib,
                    isya = apiJadwal.isya
                )
                saveOfficialDailyPrayerTimesSync(context, location, year, month, day, times)
                return times
            }
        }
        return null
    }
}
