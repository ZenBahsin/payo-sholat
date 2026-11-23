package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.LocationPreset
import com.example.data.PrayerCountry
import com.example.data.PrayerLocationCatalog
import com.example.data.api.AladhanApiClient
import com.example.data.api.MalaysiaPrayerApi
import com.example.utils.HijriDateHelper
import com.example.utils.PrayerAlarmScheduler
import com.example.utils.PrayerTimesCalculator
import com.example.utils.WidgetLocationSyncHelper
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start battery-friendly real-time FusedLocationProviderClient updates as soon as a widget is placed
        WidgetLocationSyncHelper.registerWidgetLocationUpdates(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Stop location updates when the last widget instance is removed from Home Screen
        WidgetLocationSyncHelper.unregisterWidgetLocationUpdates(context.applicationContext)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        // Ensure real-time FusedLocationProviderClient listener is registered & perform throttled GPS sync
        val appContext = context.applicationContext
        WidgetLocationSyncHelper.registerWidgetLocationUpdates(appContext)
        if (WidgetLocationSyncHelper.isAutoGpsEnabled(appContext) &&
            WidgetLocationSyncHelper.hasLocationPermission(appContext)
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = WidgetLocationSyncHelper.syncWidgetLocationIfEligible(
                        context = appContext,
                        forceFreshFix = false
                    )
                    if (result != null && (result.locationChanged || result.scheduleSynced)) {
                        updateAllWidgets(appContext)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext

        when (intent.action) {
            ACTION_WIDGET_LOCATION_UPDATE -> {
                // Real-time location update delivered directly from FusedLocationProviderClient
                if (!WidgetLocationSyncHelper.isAutoGpsEnabled(appContext)) return
                val locationResult = LocationResult.extractResult(intent)
                val newLocation = locationResult?.lastLocation ?: return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        WidgetLocationSyncHelper.applyDetectedLocationAndSyncSchedule(
                            context = appContext,
                            location = newLocation,
                            forceApiRefresh = false
                        )
                        updateAllWidgets(appContext)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_REFRESH_WIDGET -> {
                val fromUserTap = intent.getBooleanExtra("from_user_tap", false)
                // Render immediately with current cached state first
                updateAllWidgets(appContext)

                val shouldSyncGps = WidgetLocationSyncHelper.isAutoGpsEnabled(appContext) &&
                    WidgetLocationSyncHelper.hasLocationPermission(appContext)

                if (fromUserTap || shouldSyncGps) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            WidgetLocationSyncHelper.registerWidgetLocationUpdates(appContext)
                            val gpsResult = if (shouldSyncGps) {
                                WidgetLocationSyncHelper.syncWidgetLocationIfEligible(
                                    context = appContext,
                                    forceFreshFix = fromUserTap
                                )
                            } else {
                                null
                            }

                            // Also guarantee today's official API schedule is cached when user taps refresh
                            if (fromUserTap && gpsResult == null) {
                                val location = PrayerLocationCatalog.getSavedLocationPreset(appContext)
                                val tz = PrayerLocationCatalog.resolveTimeZone(appContext)
                                val cal = Calendar.getInstance(tz)
                                PrayerAlarmScheduler.getPrayerTimesFromApiOrCache(
                                    context = appContext,
                                    location = location,
                                    year = cal.get(Calendar.YEAR),
                                    month = cal.get(Calendar.MONTH) + 1,
                                    day = cal.get(Calendar.DAY_OF_MONTH)
                                )
                            }

                            updateAllWidgets(appContext)

                            if (fromUserTap) {
                                val toastText = if (gpsResult != null) {
                                    "Lokasi & jadwal diperbarui: ${gpsResult.locationPreset.widgetDisplayName()}"
                                } else {
                                    "Jadwal Sholat Widget diperbarui"
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(appContext, toastText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                updateAllWidgets(appContext)
                if (WidgetLocationSyncHelper.isAutoGpsEnabled(appContext) &&
                    WidgetLocationSyncHelper.hasLocationPermission(appContext)
                ) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            WidgetLocationSyncHelper.registerWidgetLocationUpdates(appContext)
                            val res = WidgetLocationSyncHelper.syncWidgetLocationIfEligible(
                                context = appContext,
                                forceFreshFix = intent.action == Intent.ACTION_TIMEZONE_CHANGED
                            )
                            if (res != null) {
                                updateAllWidgets(appContext)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    enum class WidgetTheme(val prefKey: String, val displayName: String, val subtitle: String) {
        SLATE_BLUE(
            prefKey = "SLATE_BLUE",
            displayName = "Muted Slate Blue",
            subtitle = "Menyesuaikan Widget Cuaca"
        ),
        FROSTED_LIGHT(
            prefKey = "FROSTED_LIGHT",
            displayName = "Frosted Light",
            subtitle = "Menyesuaikan Widget Health"
        );

        companion object {
            fun fromKey(key: String?): WidgetTheme {
                return entries.firstOrNull { it.prefKey.equals(key, ignoreCase = true) } ?: SLATE_BLUE
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.ACTION_REFRESH_PRAYER_WIDGET"
        const val ACTION_WIDGET_LOCATION_UPDATE = "com.example.ACTION_WIDGET_LOCATION_UPDATE"
        const val PREF_WIDGET_THEME = "widget_theme"

        fun getSavedTheme(context: Context): WidgetTheme {
            val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
            return WidgetTheme.fromKey(prefs.getString(PREF_WIDGET_THEME, WidgetTheme.SLATE_BLUE.prefKey))
        }

        fun setSavedTheme(context: Context, theme: WidgetTheme) {
            val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_WIDGET_THEME, theme.prefKey).apply()
            updateAllWidgets(context)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, PrayerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun isPinWidgetSupported(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.isRequestPinAppWidgetSupported
            } else {
                false
            }
        }

        fun requestPinHorizontalWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        val myProvider = ComponentName(context, PrayerWidgetProvider::class.java)
                        val successIntent = Intent(context, PrayerWidgetProvider::class.java).apply {
                            action = ACTION_REFRESH_WIDGET
                        }
                        val successCallback = PendingIntent.getBroadcast(
                            context,
                            7788,
                            successIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        return appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return false
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
            val location = PrayerLocationCatalog.getSavedLocationPreset(context)
            val timeZone = PrayerLocationCatalog.resolveTimeZone(context)
            val selectedTheme = WidgetTheme.fromKey(
                prefs.getString(PREF_WIDGET_THEME, WidgetTheme.SLATE_BLUE.prefKey)
            )
            val isLightTheme = selectedTheme == WidgetTheme.FROSTED_LIGHT

            val cal = Calendar.getInstance(timeZone)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            // 1. Read cached official API prayer times (Kemenag RI / JAKIM e-Solat) — NO manual calculation
            val cachedTimes = PrayerAlarmScheduler.getCachedPrayerTimesSync(
                context = context,
                location = location,
                year = year,
                month = month,
                day = day
            )

            // 2. Get synchronized Hijri date (prefer official JAKIM Hijri date when in Malaysia)
            val jakimHijri = if (location.country == PrayerCountry.MALAYSIA) {
                MalaysiaPrayerApi.getCachedDailyPrayerTimesSync(
                    context = context,
                    jakimZone = location.jakimZone ?: "WLY01",
                    year = year,
                    month = month,
                    day = day
                )?.hijriDateFormatted
            } else {
                null
            }
            if (!jakimHijri.isNullOrBlank() && !HijriDateHelper.hasCachedHijriDate(context, cal)) {
                HijriDateHelper.saveCachedHijriDate(context, cal, jakimHijri)
            }
            val hijriStr = jakimHijri ?: HijriDateHelper.getHijriDate(context, cal)

            // 3. Build crystal-clear Location Label (always visible on dedicated top line)
            val locationDisplay = location.widgetDisplayName()
            val subtitleDisplay = "$hijriStr • ${location.country.authorityShort}"

            val views = RemoteViews(context.packageName, R.layout.widget_prayer_horizontal)

            // Apply Main Container & Header Theme Styling
            val rootBgRes = if (isLightTheme) {
                R.drawable.widget_bg_horizontal_light
            } else {
                R.drawable.widget_bg_horizontal
            }
            val badgeBgRes = if (isLightTheme) {
                R.drawable.widget_badge_bg_light
            } else {
                R.drawable.widget_badge_bg
            }
            val primaryTextColor = if (isLightTheme) {
                Color.parseColor("#1E2430")
            } else {
                Color.WHITE
            }
            val secondaryTextColor = if (isLightTheme) {
                Color.parseColor("#687385")
            } else {
                Color.parseColor("#C7FFFFFF") // rgba(255, 255, 255, 0.78)
            }

            views.setInt(R.id.widget_root, "setBackgroundResource", rootBgRes)
            views.setInt(R.id.widget_location_icon, "setColorFilter", primaryTextColor)
            views.setTextColor(R.id.widget_location_text, primaryTextColor)
            views.setTextColor(R.id.widget_hijri_text, secondaryTextColor)
            views.setInt(R.id.widget_next_prayer_badge, "setBackgroundResource", badgeBgRes)
            views.setTextColor(R.id.widget_next_prayer_badge, Color.WHITE)
            views.setInt(R.id.widget_btn_refresh, "setBackgroundResource", badgeBgRes)
            views.setTextColor(R.id.widget_btn_refresh, Color.WHITE)

            // Header texts (Dedicated Location line + Hijri & Source subtitle)
            views.setTextViewText(R.id.widget_location_text, locationDisplay)
            views.setTextViewText(R.id.widget_hijri_text, subtitleDisplay)

            val curSeconds = cal.get(Calendar.HOUR_OF_DAY) * 3600 +
                cal.get(Calendar.MINUTE) * 60 +
                cal.get(Calendar.SECOND)

            val status = if (cachedTimes != null) {
                PrayerTimesCalculator.evaluatePrayerStatus(cachedTimes, curSeconds)
            } else {
                null
            }

            if (cachedTimes != null && status != null) {
                views.setTextViewText(R.id.widget_next_prayer_badge, status.widgetBadgeText)
                views.setTextViewText(R.id.widget_time_subuh, cachedTimes.subuh)
                views.setTextViewText(R.id.widget_time_zuhur, cachedTimes.zuhur)
                views.setTextViewText(R.id.widget_time_asar, cachedTimes.asar)
                views.setTextViewText(R.id.widget_time_maghrib, cachedTimes.maghrib)
                views.setTextViewText(R.id.widget_time_isya, cachedTimes.isya)
            } else {
                views.setTextViewText(R.id.widget_next_prayer_badge, "Memuat ${location.country.authorityShort}...")
                views.setTextViewText(R.id.widget_time_subuh, "--:--")
                views.setTextViewText(R.id.widget_time_zuhur, "--:--")
                views.setTextViewText(R.id.widget_time_asar, "--:--")
                views.setTextViewText(R.id.widget_time_maghrib, "--:--")
                views.setTextViewText(R.id.widget_time_isya, "--:--")
            }

            // Highlight the active/upcoming column matching status.widgetHighlightedPrayer
            data class PrayerColIds(val name: String, val colId: Int, val labelId: Int, val timeId: Int)
            val prayerColumns = listOf(
                PrayerColIds("Subuh", R.id.widget_col_subuh, R.id.widget_label_subuh, R.id.widget_time_subuh),
                PrayerColIds("Zuhur", R.id.widget_col_zuhur, R.id.widget_label_zuhur, R.id.widget_time_zuhur),
                PrayerColIds("Asar", R.id.widget_col_asar, R.id.widget_label_asar, R.id.widget_time_asar),
                PrayerColIds("Maghrib", R.id.widget_col_maghrib, R.id.widget_label_maghrib, R.id.widget_time_maghrib),
                PrayerColIds("Isya", R.id.widget_col_isya, R.id.widget_label_isya, R.id.widget_time_isya)
            )
            for (col in prayerColumns) {
                val isHighlighted = status != null && col.name.equals(status.widgetHighlightedPrayer, ignoreCase = true)
                val bgRes = when {
                    isHighlighted && isLightTheme -> R.drawable.widget_item_active_bg_light
                    isHighlighted && !isLightTheme -> R.drawable.widget_item_active_bg
                    !isHighlighted && isLightTheme -> R.drawable.widget_item_inactive_bg_light
                    else -> R.drawable.widget_item_inactive_bg
                }
                val labelColor = if (isHighlighted) {
                    Color.WHITE
                } else {
                    secondaryTextColor
                }
                val timeColor = if (isHighlighted) {
                    Color.WHITE
                } else {
                    primaryTextColor
                }
                views.setInt(col.colId, "setBackgroundResource", bgRes)
                views.setTextColor(col.labelId, labelColor)
                views.setTextColor(col.timeId, timeColor)
            }

            // Click on widget body -> Open MainActivity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Click on Refresh button -> Refresh Widget immediately
            val refreshIntent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra("from_user_tap", true)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 5000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Schedule automatic transition alarm for the next prayer boundary or midnight
            if (cachedTimes != null) {
                scheduleNextWidgetRefresh(context, cachedTimes, curSeconds)
            }

            // If official API schedule or Hijri date for today is not cached yet, fetch in background and refresh
            if (cachedTimes == null || !HijriDateHelper.hasCachedHijriDate(context, cal)) {
                syncApiDataForWidgetInBackground(context.applicationContext, location, timeZone)
            }
        }

        private fun scheduleNextWidgetRefresh(
            context: Context,
            times: PrayerTimesCalculator.PrayerTimes,
            curSeconds: Int
        ) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val allTimesSec = listOf(
                    times.imsak,
                    times.subuh,
                    times.syuruq,
                    times.zuhur,
                    times.asar,
                    times.maghrib,
                    times.isya
                ).map { PrayerTimesCalculator.parseTimeToSeconds(it) }

                val nextTransitionSec = allTimesSec.firstOrNull { it > curSeconds }
                val delaySeconds = if (nextTransitionSec != null) {
                    (nextTransitionSec - curSeconds) + 1
                } else {
                    (86400 - curSeconds) + 2
                }

                val triggerAtMillis = System.currentTimeMillis() + (delaySeconds.coerceAtLeast(5) * 1000L)
                val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                    putExtra("from_user_tap", false)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    9911,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun syncApiDataForWidgetInBackground(
            appContext: Context,
            location: LocationPreset,
            timeZone: TimeZone
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cal = Calendar.getInstance(timeZone)
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    var updated = false

                    if (PrayerAlarmScheduler.getCachedPrayerTimesSync(appContext, location, year, month, day) == null) {
                        val fetchedTimes = PrayerAlarmScheduler.getPrayerTimesFromApiOrCache(
                            context = appContext,
                            location = location,
                            year = year,
                            month = month,
                            day = day
                        )
                        if (fetchedTimes != null) {
                            updated = true
                        }
                    }

                    if (!HijriDateHelper.hasCachedHijriDate(appContext, cal)) {
                        val jakimHijri = if (location.country == PrayerCountry.MALAYSIA) {
                            MalaysiaPrayerApi.getCachedDailyPrayerTimesSync(
                                context = appContext,
                                jakimZone = location.jakimZone ?: "WLY01",
                                year = year,
                                month = month,
                                day = day
                            )?.hijriDateFormatted
                        } else {
                            null
                        }

                        if (!jakimHijri.isNullOrBlank()) {
                            HijriDateHelper.saveCachedHijriDate(appContext, cal, jakimHijri)
                            updated = true
                        } else {
                            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
                                this.timeZone = timeZone
                            }
                            val dateStr = sdf.format(cal.time)
                            val response = AladhanApiClient.service.getHijriDate(dateStr)
                            if (response.code == 200 && response.data?.hijri != null) {
                                val hijri = response.data.hijri
                                val hDay = hijri.day
                                val enMonth = hijri.month?.en ?: ""
                                val indonesianMonth = HijriDateHelper.translateHijriMonthToIndonesian(enMonth)
                                val hYear = hijri.year
                                val hijriStr = "$hDay $indonesianMonth $hYear H"
                                HijriDateHelper.saveCachedHijriDate(appContext, cal, hijriStr)
                                updated = true
                            }
                        }
                    }

                    if (updated) {
                        updateAllWidgets(appContext)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
