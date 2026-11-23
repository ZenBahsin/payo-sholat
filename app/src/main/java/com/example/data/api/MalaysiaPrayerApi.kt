package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.LocationPreset
import com.example.data.PrayerCountry
import com.example.data.PrayerLocationCatalog
import com.example.utils.HijriDateHelper
import com.example.utils.PrayerTimesCalculator.PrayerTimes
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Official Malaysia Prayer Times API Client (JAKIM e-Solat & Waktu Solat Malaysia API).
 * Uses ONLY official JAKIM schedule data — NO manual astronomical prayer time calculations.
 */
object MalaysiaPrayerApi {
    private const val TAG = "MalaysiaPrayerApi"
    private val MALAYSIA_TZ: TimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    data class MalaysiaDailyResult(
        val prayerTimes: PrayerTimes,
        val hijriDateFormatted: String?
    )

    /**
     * Resolves a GPS coordinate in Malaysia to an official JAKIM Zone using api.waktusolat.app/zones/{lat}/{long}.
     * Returns null if coordinates are outside Malaysia or API fails.
     */
    fun resolveZoneFromGps(lat: Double, lng: Double): LocationPreset? {
        val url = "https://api.waktusolat.app/zones/$lat/$lng"
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val zoneCode = json.optString("zone", "").trim()
                    .ifEmpty { json.optString("jakimCode", "").trim() }
                if (zoneCode.isEmpty()) return null

                // Prefer rich preset metadata if available in catalog
                PrayerLocationCatalog.malaysiaPresets
                    .firstOrNull { it.jakimZone.equals(zoneCode, ignoreCase = true) }
                    ?.let { preset ->
                        return preset.copy(lat = lat, lng = lng)
                    }

                val district = json.optString("district", "").trim()
                    .ifEmpty { json.optString("daerah", "").trim() }
                val state = json.optString("state", "").trim()
                    .ifEmpty { json.optString("negeri", "").trim() }
                    .ifEmpty { "Malaysia" }
                val displayName = district.ifEmpty { state }

                return LocationPreset(
                    name = displayName,
                    region = state,
                    country = PrayerCountry.MALAYSIA,
                    lat = lat,
                    lng = lng,
                    timeZoneId = "Asia/Kuala_Lumpur",
                    jakimZone = zoneCode.uppercase(Locale.US),
                    daerahDetail = district.ifEmpty { null }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving Malaysia GPS zone via API: ${e.message}")
            return null
        }
    }

    /**
     * Reads cached official JAKIM monthly schedule synchronously (used by Widget & ViewModel init).
     */
    fun getCachedDailyPrayerTimesSync(
        context: Context,
        jakimZone: String,
        year: Int,
        month: Int,
        day: Int
    ): MalaysiaDailyResult? {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val cacheKey = monthlyCacheKey(jakimZone, year, month)
        val cachedStr = prefs.getString(cacheKey, null) ?: return null
        return try {
            val root = JSONObject(cachedStr)
            val dayObj = root.optJSONObject(day.toString()) ?: return null
            val times = PrayerTimes(
                imsak = dayObj.optString("imsak", ""),
                subuh = dayObj.optString("subuh", ""),
                syuruq = dayObj.optString("syuruq", ""),
                zuhur = dayObj.optString("zuhur", ""),
                asar = dayObj.optString("asar", ""),
                maghrib = dayObj.optString("maghrib", ""),
                isya = dayObj.optString("isya", "")
            )
            if (times.zuhur.isEmpty()) return null
            val hijri = dayObj.optString("hijri", "").takeIf { it.isNotBlank() }
            MalaysiaDailyResult(times, hijri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading cached Malaysia monthly JSON", e)
            null
        }
    }

    /**
     * Fetches official JAKIM prayer schedule for the given zone & date.
     * Order:
     * 1) Local monthly cache (official JAKIM data previously downloaded)
     * 2) Waktu Solat Malaysia API v2 (official JAKIM mirror supporting year & month)
     * 3) Official JAKIM e-Solat Portal API (www.e-solat.gov.my)
     */
    fun fetchDailyPrayerTimes(
        context: Context,
        jakimZone: String,
        year: Int,
        month: Int,
        day: Int
    ): MalaysiaDailyResult? {
        val normalizedZone = jakimZone.trim().uppercase(Locale.US).ifEmpty { "WLY01" }

        // 1. Check cache first
        getCachedDailyPrayerTimesSync(context, normalizedZone, year, month, day)?.let {
            return it
        }

        // 2. Fetch from api.waktusolat.app v2 (Official JAKIM data)
        val monthMapFromV2 = fetchMonthFromWaktuSolatV2(normalizedZone, year, month)
        if (monthMapFromV2 != null && monthMapFromV2.isNotEmpty()) {
            saveMonthlyCache(context, normalizedZone, year, month, monthMapFromV2)
            monthMapFromV2[day]?.let { result ->
                cacheHijriIfPresent(context, year, month, day, result.hijriDateFormatted)
                return result
            }
        }

        // 3. Fallback directly to official JAKIM e-Solat Portal API
        val monthMapFromJakim = fetchMonthFromOfficialESolat(normalizedZone, year, month)
        if (monthMapFromJakim != null && monthMapFromJakim.isNotEmpty()) {
            saveMonthlyCache(context, normalizedZone, year, month, monthMapFromJakim)
            monthMapFromJakim[day]?.let { result ->
                cacheHijriIfPresent(context, year, month, day, result.hijriDateFormatted)
                return result
            }
        }

        return null
    }

    private fun fetchMonthFromWaktuSolatV2(
        zone: String,
        year: Int,
        month: Int
    ): Map<Int, MalaysiaDailyResult>? {
        val url = "https://api.waktusolat.app/v2/solat/$zone?year=$year&month=$month"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "WaktuSolat v2 HTTP ${response.code} for $zone")
                    return null
                }
                val body = response.body?.string() ?: return null
                parseWaktuSolatV2Json(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WaktuSolat v2 fetch error for $zone: ${e.message}")
            null
        }
    }

    private fun parseWaktuSolatV2Json(jsonStr: String): Map<Int, MalaysiaDailyResult>? {
        return try {
            val root = JSONObject(jsonStr)
            val prayersArray = root.optJSONArray("prayers") ?: return null
            val resultMap = mutableMapOf<Int, MalaysiaDailyResult>()

            for (i in 0 until prayersArray.length()) {
                val item = prayersArray.optJSONObject(i) ?: continue
                val dayNum = item.optInt("day", i + 1)
                if (dayNum !in 1..31) continue

                val subuh = parseFlexibleTimeField(item, "fajr", "subuh")
                val syuruq = parseFlexibleTimeField(item, "syuruk", "sunrise")
                val zuhur = parseFlexibleTimeField(item, "dhuhr", "zohor")
                val asar = parseFlexibleTimeField(item, "asr", "asar")
                val maghrib = parseFlexibleTimeField(item, "maghrib")
                val isya = parseFlexibleTimeField(item, "isha", "isyak")

                if (subuh.isEmpty() || zuhur.isEmpty()) continue

                val imsakRaw = parseFlexibleTimeField(item, "imsak")
                val imsak = imsakRaw.ifEmpty { subtractMinutes(subuh, 10) }

                val hijriRaw = item.optString("hijri", "")
                val hijriFormatted = formatJakimHijriDate(hijriRaw)

                resultMap[dayNum] = MalaysiaDailyResult(
                    prayerTimes = PrayerTimes(
                        imsak = imsak,
                        subuh = subuh,
                        syuruq = syuruq,
                        zuhur = zuhur,
                        asar = asar,
                        maghrib = maghrib,
                        isya = isya
                    ),
                    hijriDateFormatted = hijriFormatted
                )
            }
            resultMap.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WaktuSolat v2 JSON", e)
            null
        }
    }

    private fun fetchMonthFromOfficialESolat(
        zone: String,
        targetYear: Int,
        targetMonth: Int
    ): Map<Int, MalaysiaDailyResult>? {
        // Use period=month for current month, or period=year if querying another month in current year
        val nowCal = Calendar.getInstance(MALAYSIA_TZ)
        val isCurrentMonth = nowCal.get(Calendar.YEAR) == targetYear && (nowCal.get(Calendar.MONTH) + 1) == targetMonth
        val period = if (isCurrentMonth) "month" else "year"
        val url = "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=$period&zone=$zone"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "e-Solat JAKIM HTTP ${response.code} for $zone")
                    return null
                }
                val body = response.body?.string() ?: return null
                parseOfficialESolatJson(body, targetYear, targetMonth)
            }
        } catch (e: Exception) {
            Log.w(TAG, "e-Solat JAKIM fetch error for $zone: ${e.message}")
            null
        }
    }

    private fun parseOfficialESolatJson(
        jsonStr: String,
        targetYear: Int,
        targetMonth: Int
    ): Map<Int, MalaysiaDailyResult>? {
        return try {
            val root = JSONObject(jsonStr)
            val prayerTimeArray: JSONArray = root.optJSONArray("prayerTime") ?: return null
            val resultMap = mutableMapOf<Int, MalaysiaDailyResult>()

            val dfShort = SimpleDateFormat("dd-MMM-yyyy", Locale.US).apply { timeZone = MALAYSIA_TZ }
            val dfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = MALAYSIA_TZ }

            for (i in 0 until prayerTimeArray.length()) {
                val item = prayerTimeArray.optJSONObject(i) ?: continue
                val dateStr = item.optString("date", "").trim()
                val parsedDate = try {
                    dfShort.parse(dateStr)
                } catch (_: Exception) {
                    try {
                        dfIso.parse(dateStr)
                    } catch (_: Exception) {
                        null
                    }
                }

                val dayNum: Int = if (parsedDate != null) {
                    val cal = Calendar.getInstance(MALAYSIA_TZ).apply { time = parsedDate }
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH) + 1
                    if (y != targetYear || m != targetMonth) continue
                    cal.get(Calendar.DAY_OF_MONTH)
                } else {
                    i + 1
                }

                val subuh = normalizeHHmm(item.optString("fajr", ""))
                val syuruq = normalizeHHmm(item.optString("syuruk", ""))
                val zuhur = normalizeHHmm(item.optString("dhuhr", ""))
                val asar = normalizeHHmm(item.optString("asr", ""))
                val maghrib = normalizeHHmm(item.optString("maghrib", ""))
                val isya = normalizeHHmm(item.optString("isha", ""))
                val imsakRaw = normalizeHHmm(item.optString("imsak", ""))
                val imsak = imsakRaw.ifEmpty { subtractMinutes(subuh, 10) }

                if (subuh.isEmpty() || zuhur.isEmpty()) continue

                val hijriFormatted = formatJakimHijriDate(item.optString("hijri", ""))
                resultMap[dayNum] = MalaysiaDailyResult(
                    prayerTimes = PrayerTimes(
                        imsak = imsak,
                        subuh = subuh,
                        syuruq = syuruq,
                        zuhur = zuhur,
                        asar = asar,
                        maghrib = maghrib,
                        isya = isya
                    ),
                    hijriDateFormatted = hijriFormatted
                )
            }
            resultMap.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing official e-Solat JSON", e)
            null
        }
    }

    /**
     * Parses a prayer time field that may be EITHER:
     * - Unix epoch timestamp in seconds (Long/Int, used by api.waktusolat.app v2)
     * - Formatted time string ("05:47:00" or "05:47")
     */
    private fun parseFlexibleTimeField(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key) ?: continue
            when (raw) {
                is Number -> {
                    val epochSec = raw.toLong()
                    if (epochSec > 100_000_000L) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = MALAYSIA_TZ }
                        return sdf.format(Date(epochSec * 1000L))
                    }
                }
                is String -> {
                    val trimmed = raw.trim()
                    val asLong = trimmed.toLongOrNull()
                    if (asLong != null && asLong > 100_000_000L) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = MALAYSIA_TZ }
                        return sdf.format(Date(asLong * 1000L))
                    }
                    val normalized = normalizeHHmm(trimmed)
                    if (normalized.isNotEmpty()) return normalized
                }
            }
        }
        return ""
    }

    private fun normalizeHHmm(raw: String): String {
        val parts = raw.trim().split(":")
        if (parts.size < 2) return ""
        val h = parts[0].toIntOrNull() ?: return ""
        val m = parts[1].toIntOrNull() ?: return ""
        if (h !in 0..23 || m !in 0..59) return ""
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    private fun subtractMinutes(hhmm: String, minutesToSubtract: Int): String {
        val parts = hhmm.split(":")
        if (parts.size < 2) return ""
        val h = parts[0].toIntOrNull() ?: return ""
        val m = parts[1].toIntOrNull() ?: return ""
        val totalMins = ((h * 60 + m - minutesToSubtract) + 1440) % 1440
        return String.format(Locale.US, "%02d:%02d", totalMins / 60, totalMins % 60)
    }

    /**
     * Converts JAKIM Hijri format "1448-06-12" into "12 Jumadil Akhir 1448 H".
     */
    private fun formatJakimHijriDate(raw: String): String? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val parts = clean.split("-")
        if (parts.size != 3) return null
        val hYear = parts[0].toIntOrNull() ?: return null
        val hMonth = parts[1].toIntOrNull() ?: return null
        val hDay = parts[2].toIntOrNull() ?: return null
        val monthNames = arrayOf(
            "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Akhir",
            "Jumadil Awwal", "Jumadil Akhir", "Rajab", "Sya'ban",
            "Ramadhan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
        )
        if (hMonth !in 1..12) return null
        return "$hDay ${monthNames[hMonth - 1]} $hYear H"
    }

    private fun saveMonthlyCache(
        context: Context,
        zone: String,
        year: Int,
        month: Int,
        monthData: Map<Int, MalaysiaDailyResult>
    ) {
        try {
            val root = JSONObject()
            for ((dayNum, data) in monthData) {
                val obj = JSONObject().apply {
                    put("imsak", data.prayerTimes.imsak)
                    put("subuh", data.prayerTimes.subuh)
                    put("syuruq", data.prayerTimes.syuruq)
                    put("zuhur", data.prayerTimes.zuhur)
                    put("asar", data.prayerTimes.asar)
                    put("maghrib", data.prayerTimes.maghrib)
                    put("isya", data.prayerTimes.isya)
                    if (!data.hijriDateFormatted.isNullOrBlank()) {
                        put("hijri", data.hijriDateFormatted)
                    }
                }
                root.put(dayNum.toString(), obj)
            }
            val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(monthlyCacheKey(zone, year, month), root.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Malaysia monthly cache", e)
        }
    }

    private fun cacheHijriIfPresent(
        context: Context,
        year: Int,
        month: Int,
        day: Int,
        hijriStr: String?
    ) {
        if (hijriStr.isNullOrBlank()) return
        val cal = Calendar.getInstance(MALAYSIA_TZ).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        HijriDateHelper.saveCachedHijriDate(context, cal, hijriStr)
    }

    private fun monthlyCacheKey(zone: String, year: Int, month: Int): String {
        return "malaysia_jakim_cache_${zone.uppercase(Locale.US)}_${year}_${month}"
    }
}
