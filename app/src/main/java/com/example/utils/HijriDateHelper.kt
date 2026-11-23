package com.example.utils

import android.content.Context
import android.os.Build
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import kotlin.math.floor

object HijriDateHelper {
    private val MONTH_NAMES_INDONESIAN = arrayOf(
        "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Akhir",
        "Jumadil Awwal", "Jumadil Akhir", "Rajab", "Sya'ban",
        "Ramadhan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
    )

    private fun getCacheKey(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "hijri_cache_${year}_${month}_${day}"
    }

    fun saveCachedHijriDate(context: Context, calendar: Calendar, hijriStr: String) {
        if (hijriStr.isBlank()) return
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(getCacheKey(calendar), hijriStr)
            .putString("hijri_cache_latest", hijriStr)
            .apply()
    }

    fun hasCachedHijriDate(context: Context, calendar: Calendar): Boolean {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        return !prefs.getString(getCacheKey(calendar), null).isNullOrBlank()
    }

    fun getHijriDate(context: Context, calendar: Calendar = Calendar.getInstance()): String {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val cachedForToday = prefs.getString(getCacheKey(calendar), null)
        if (!cachedForToday.isNullOrBlank()) {
            return cachedForToday
        }
        return getTodayHijriString(calendar)
    }

    fun getTodayHijriString(calendar: Calendar = Calendar.getInstance()): String {
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val localDate = LocalDate.of(gYear, gMonth, gDay)
                val hijrahDate = HijrahDate.from(localDate)
                val hDay = hijrahDate.get(ChronoField.DAY_OF_MONTH)
                val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
                val hYear = hijrahDate.get(ChronoField.YEAR)
                val monthName = MONTH_NAMES_INDONESIAN.getOrElse(hMonth - 1) { "Muharram" }
                return "$hDay $monthName $hYear H"
            } catch (e: Exception) {
                // Fallback to tabular calculation below
            }
        }

        val jd = getJulianDay(calendar)
        val l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        val lRem = l - 10631 * n + 354

        val j = ((10985 - lRem) / 5316) * ((50 * lRem) / 17719) +
            (lRem / 5670) * ((43 * lRem) / 15238)
        val lRem2 = lRem - ((30 - j) / 15) * ((17719 * j) / 50) -
            (j / 16) * ((15238 * j) / 43) + 29

        val m = ((24 * lRem2) / 709).coerceIn(1, 12)
        val d = (lRem2 - (709 * m) / 24).coerceIn(1, 30)
        val y = 30 * n + j - 30

        val monthName = MONTH_NAMES_INDONESIAN[m - 1]
        return "$d $monthName $y H"
    }

    fun translateHijriMonthToIndonesian(enName: String): String {
        val clean = enName.lowercase()
            .replace("ḥ", "h")
            .replace("ṣ", "s")
            .replace("ʿ", "")
            .replace("’", "")
            .replace("'", "")
            .replace("ā", "a")
            .replace("ū", "u")
            .replace("ī", "i")
            .replace("-", " ")
            .trim()
        return when {
            clean.contains("muharram") -> "Muharram"
            clean.contains("safar") -> "Safar"
            clean.contains("rabi al awwal") || clean.contains("rabiul awwal") || clean.contains("rabi'ul awwal") -> "Rabi'ul Awwal"
            clean.contains("rabi") && (clean.contains("thani") || clean.contains("akhir")) -> "Rabi'ul Akhir"
            clean.contains("jumada al ula") || clean.contains("jumadaul ula") || clean.contains("jumadil awwal") || clean.contains("jumada al awwal") -> "Jumadil Awwal"
            clean.contains("jumada") && (clean.contains("akhir") || clean.contains("thani")) -> "Jumadil Akhir"
            clean.contains("rajab") -> "Rajab"
            clean.contains("shaban") || clean.contains("sya'ban") -> "Sya'ban"
            clean.contains("ramadhan") || clean.contains("ramadan") -> "Ramadhan"
            clean.contains("shawwal") || clean.contains("syawwal") -> "Syawwal"
            clean.contains("qa'dah") || clean.contains("qadah") -> "Dzulqa'dah"
            clean.contains("hijjah") -> "Dzulhijjah"
            else -> enName
        }
    }

    private fun getJulianDay(calendar: Calendar): Int {
        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        if (month <= 2) {
            year -= 1
            month += 12
        }

        val a = floor(year / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return (floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524).toInt()
    }
}
