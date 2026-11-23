package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.LocationPreset
import com.example.data.PrayerCountry
import com.example.data.PrayerLocationCatalog
import com.example.data.api.AladhanApiClient
import com.example.data.api.MalaysiaPrayerApi
import com.example.receiver.PrayerWidgetProvider
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * Real-time location detection & synchronization engine powered by FusedLocationProviderClient
 * for the Home Screen Prayer Widget and MainViewModel.
 *
 * Strictly uses official APIs (Kemenag RI for Indonesia, JAKIM e-Solat for Malaysia) with ZERO
 * manual astronomical calculations.
 */
object WidgetLocationSyncHelper {
    private const val TAG = "WidgetLocationSync"
    private const val PREF_NAME = "sholat_prefs"
    private const val KEY_USE_AUTO_GPS = "use_auto_gps"
    private const val KEY_LAST_GPS_SYNC_MS = "widget_last_gps_sync_ms"

    private const val LOCATION_UPDATE_REQUEST_CODE = 8844
    private const val AUTO_SYNC_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes throttle for passive refreshes
    private const val MAX_CACHED_FIX_AGE_MS = 10 * 60 * 1000L // 10 minutes freshness window
    private const val WIDGET_LOCATION_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes balanced polling
    private const val WIDGET_LOCATION_MIN_INTERVAL_MS = 5 * 60 * 1000L
    private const val WIDGET_LOCATION_MIN_DISTANCE_METERS = 2_000f // 2 km displacement trigger

    data class SyncResult(
        val locationPreset: LocationPreset,
        val locationChanged: Boolean,
        val scheduleSynced: Boolean
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    fun isAutoGpsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_AUTO_GPS, true)
    }

    fun setAutoGpsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_AUTO_GPS, enabled).apply()
        if (enabled) {
            registerWidgetLocationUpdates(context)
        } else {
            unregisterWidgetLocationUpdates(context)
        }
    }

    fun hasActiveWidgets(context: Context): Boolean {
        return try {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            ids != null && ids.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Registers battery-efficient real-time location updates via FusedLocationProviderClient
     * delivered directly to PrayerWidgetProvider via PendingIntent whenever the user moves >= 2km.
     */
    @SuppressLint("MissingPermission")
    fun registerWidgetLocationUpdates(context: Context) {
        val appContext = context.applicationContext
        if (!isAutoGpsEnabled(appContext) || !hasLocationPermission(appContext)) {
            return
        }
        if (!hasActiveWidgets(appContext)) {
            return
        }
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
            val request = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                WIDGET_LOCATION_INTERVAL_MS
            )
                .setMinUpdateIntervalMillis(WIDGET_LOCATION_MIN_INTERVAL_MS)
                .setMinUpdateDistanceMeters(WIDGET_LOCATION_MIN_DISTANCE_METERS)
                .setWaitForAccurateLocation(false)
                .build()

            val pendingIntent = buildLocationUpdatePendingIntent(appContext)
            fusedClient.requestLocationUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Registered FusedLocationProviderClient real-time updates for widget")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Could not register widget FusedLocation updates: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing while registering widget updates", e)
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error registering widget location updates", e)
        }
    }

    /**
     * Unregisters FusedLocationProviderClient PendingIntent updates (called when widget removed
     * or user switches to manual city/zone mode).
     */
    fun unregisterWidgetLocationUpdates(context: Context) {
        val appContext = context.applicationContext
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
            fusedClient.removeLocationUpdates(buildLocationUpdatePendingIntent(appContext))
                .addOnSuccessListener {
                    Log.d(TAG, "Unregistered FusedLocationProviderClient updates for widget")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing widget location updates: ${e.message}")
        }
    }

    /**
     * Explicit component PendingIntent for FusedLocationProviderClient.
     * Note: FLAG_MUTABLE is required by Google Play Services so LocationResult extras can be populated.
     */
    private fun buildLocationUpdatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
            action = PrayerWidgetProvider.ACTION_WIDGET_LOCATION_UPDATE
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            LOCATION_UPDATE_REQUEST_CODE,
            intent,
            flags
        )
    }

    /**
     * Fetches the device's real-time location using FusedLocationProviderClient.
     * Uses getCurrentLocation() for fresh fixes and falls back cleanly to lastLocation.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(
        context: Context,
        forceFreshFix: Boolean = false
    ): Location? {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

        return try {
            if (!forceFreshFix) {
                val cachedLast = withTimeoutOrNull(3_000L) {
                    fusedClient.lastLocation.awaitNullable()
                }
                if (cachedLast != null && isLocationFresh(cachedLast)) {
                    return cachedLast
                }
            }

            // Request an active real-time fix from FusedLocationProviderClient
            val cts = CancellationTokenSource()
            val currentRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setDurationMillis(7_000L)
                .setMaxUpdateAgeMillis(if (forceFreshFix) 30_000L else MAX_CACHED_FIX_AGE_MS)
                .build()

            val freshLocation = withTimeoutOrNull(8_500L) {
                try {
                    fusedClient.getCurrentLocation(currentRequest, cts.token).awaitNullable()
                } finally {
                    cts.cancel()
                }
            }

            freshLocation ?: withTimeoutOrNull(2_500L) {
                fusedClient.lastLocation.awaitNullable()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException reading FusedLocationProviderClient", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching location from FusedLocationProviderClient: ${e.message}")
            null
        }
    }

    private fun isLocationFresh(location: Location): Boolean {
        val ageMs = System.currentTimeMillis() - location.time
        return ageMs in 0..MAX_CACHED_FIX_AGE_MS
    }

    /**
     * Resolves GPS coordinates into an official Indonesia (Kemenag RI) or Malaysia (JAKIM) LocationPreset.
     * For Malaysia, queries official api.waktusolat.app/zones/{lat}/{lng} first.
     */
    suspend fun resolveLocationFromCoordinates(
        lat: Double,
        lng: Double
    ): LocationPreset = withContext(Dispatchers.IO) {
        val resolvedPreset: LocationPreset? = if (PrayerLocationCatalog.isLikelyInMalaysia(lat, lng)) {
            MalaysiaPrayerApi.resolveZoneFromGps(lat, lng)
                ?: PrayerLocationCatalog.findClosestPreset(lat, lng, PrayerCountry.MALAYSIA)
                    .copy(lat = lat, lng = lng)
        } else {
            val closestAny = PrayerLocationCatalog.findClosestPreset(lat, lng)
            if (closestAny.country == PrayerCountry.MALAYSIA) {
                MalaysiaPrayerApi.resolveZoneFromGps(lat, lng) ?: closestAny.copy(lat = lat, lng = lng)
            } else {
                closestAny.copy(lat = lat, lng = lng)
            }
        }

        resolvedPreset ?: PrayerLocationCatalog.findClosestPreset(lat, lng, PrayerCountry.INDONESIA)
            .copy(lat = lat, lng = lng)
    }

    /**
     * Applies a newly received Location (from FusedLocationProviderClient), updates saved preferences,
     * syncs official API prayer times & Hijri date, and reschedules alarms if the location/zone changed.
     */
    suspend fun applyDetectedLocationAndSyncSchedule(
        context: Context,
        location: Location,
        forceApiRefresh: Boolean = false
    ): SyncResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val previous = PrayerLocationCatalog.getSavedLocationPreset(appContext)
        val resolved = resolveLocationFromCoordinates(location.latitude, location.longitude)

        val locationChanged = !previous.name.equals(resolved.name, ignoreCase = true) ||
            previous.country != resolved.country ||
            !previous.jakimZone.orEmpty().equals(resolved.jakimZone.orEmpty(), ignoreCase = true)

        PrayerLocationCatalog.saveLocationPreset(appContext, resolved, isAutoGps = true)
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_GPS_SYNC_MS, System.currentTimeMillis())
            .apply()

        val timeZone = TimeZone.getTimeZone(resolved.timeZoneId)
        val cal = Calendar.getInstance(timeZone)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        var syncedTimes: PrayerTimesCalculator.PrayerTimes? = null
        val existingCache = if (forceApiRefresh) null else {
            PrayerAlarmScheduler.getCachedPrayerTimesSync(appContext, resolved, year, month, day)
        }

        if (existingCache == null) {
            val fetched = PrayerAlarmScheduler.getPrayerTimesFromApiOrCache(
                context = appContext,
                location = resolved,
                year = year,
                month = month,
                day = day
            )
            if (fetched != null) {
                syncedTimes = fetched
            }
        } else {
            // Ensure active cache key reflects the resolved preset immediately
            PrayerAlarmScheduler.saveOfficialDailyPrayerTimesSync(
                appContext,
                resolved,
                year,
                month,
                day,
                existingCache
            )
            syncedTimes = existingCache
        }
        val scheduleSynced = syncedTimes != null

        // Sync Hijri date (prefer JAKIM Hijri for Malaysia, Aladhan fallback otherwise)
        syncHijriDateForLocation(appContext, resolved, timeZone, cal)

        // Reschedule Adzan alarms if the user moved into a new city or JAKIM zone
        if (locationChanged && syncedTimes != null) {
            try {
                val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val azanSound = prefs.getString("azan_sound", "Makkah") ?: "Makkah"
                val db = AppDatabase.getDatabase(appContext, CoroutineScope(Dispatchers.IO))
                val settingsList = db.prayerSettingDao().getAllSettings().firstOrNull() ?: emptyList()
                PrayerAlarmScheduler.scheduleAlarms(appContext, syncedTimes, settingsList, azanSound)
            } catch (e: Exception) {
                Log.w(TAG, "Failed rescheduling alarms after location change: ${e.message}")
            }
        }

        SyncResult(
            locationPreset = resolved,
            locationChanged = locationChanged,
            scheduleSynced = scheduleSynced
        )
    }

    /**
     * Full widget location synchronization entry point:
     * Checks auto-GPS setting, permission, and throttle cooldown; fetches location via
     * FusedLocationProviderClient; and updates official prayer times & Hijri cache.
     */
    suspend fun syncWidgetLocationIfEligible(
        context: Context,
        forceFreshFix: Boolean = false
    ): SyncResult? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (!isAutoGpsEnabled(appContext) || !hasLocationPermission(appContext)) {
            return@withContext null
        }

        if (!forceFreshFix) {
            val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val lastSyncMs = prefs.getLong(KEY_LAST_GPS_SYNC_MS, 0L)
            if (System.currentTimeMillis() - lastSyncMs < AUTO_SYNC_COOLDOWN_MS) {
                return@withContext null
            }
        }

        val deviceLocation = fetchCurrentDeviceLocation(appContext, forceFreshFix = forceFreshFix)
            ?: return@withContext null

        applyDetectedLocationAndSyncSchedule(
            context = appContext,
            location = deviceLocation,
            forceApiRefresh = forceFreshFix
        )
    }

    private suspend fun syncHijriDateForLocation(
        appContext: Context,
        location: LocationPreset,
        timeZone: TimeZone,
        cal: Calendar
    ) {
        try {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            if (location.country == PrayerCountry.MALAYSIA) {
                val jakimHijri = MalaysiaPrayerApi.getCachedDailyPrayerTimesSync(
                    context = appContext,
                    jakimZone = location.jakimZone ?: "WLY01",
                    year = year,
                    month = month,
                    day = day
                )?.hijriDateFormatted
                if (!jakimHijri.isNullOrBlank()) {
                    HijriDateHelper.saveCachedHijriDate(appContext, cal, jakimHijri)
                    return
                }
            }

            if (!HijriDateHelper.hasCachedHijriDate(appContext, cal)) {
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
                    HijriDateHelper.saveCachedHijriDate(appContext, cal, "$hDay $indonesianMonth $hYear H")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed syncing Hijri date during location sync: ${e.message}")
        }
    }

    private suspend fun <T> Task<T>.awaitNullable(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }
}
