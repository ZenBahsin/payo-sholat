package com.example.utils

import kotlin.math.*

class PrayerTimesCalculator {

    enum class CalculationMethod(val fajrAngle: Double, val ishaAngle: Double, val ishaIntervalMins: Int = 0) {
        KEMENAG(20.0, 18.0),             // Kementerian Agama RI (Indonesia)
        MWL(18.0, 17.0),                 // Muslim World League
        ISNA(15.0, 15.0),                // Islamic Society of North America
        EGYPT(19.5, 17.5),               // Egyptian General Authority of Survey
        MAKKAH(18.5, 0.0, 90),           // Umm al-Qura, Makkah
        KARACHI(18.0, 18.0)              // University of Islamic Sciences, Karachi
    }

    enum class AsrJuristic(val shadowFactor: Int) {
        STANDARD(1), // Shafi'i, Maliki, Hanbali
        HANAFI(2)    // Hanafi
    }

    data class PrayerTimes(
        val imsak: String,
        val subuh: String,
        val syuruq: String,
        val zuhur: String,
        val asar: String,
        val maghrib: String,
        val isya: String
    )

    data class PrayerStatusEvaluation(
        val currentPrayerName: String,
        val nextPrayerName: String,
        val nextPrayerTime: String,
        val isAfterIsya: Boolean,
        val countdownText: String,
        val widgetBadgeText: String,
        val widgetHighlightedPrayer: String
    )

    companion object {
        fun parseTimeToSeconds(timeStr: String): Int {
            return try {
                val parts = timeStr.trim().split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                h * 3600 + m * 60
            } catch (e: Exception) {
                0
            }
        }

        /**
         * Single source of truth for active ("Sekarang") and upcoming ("Selanjutnya") prayer evaluation,
         * shared across Jadwal Sholat Hari Ini, Home Screen, Widget Preview, and the Actual Home Screen Widget.
         */
        fun evaluatePrayerStatus(
            times: PrayerTimes,
            currentSeconds: Int
        ): PrayerStatusEvaluation {
            val safeSubuh = times.subuh.ifEmpty { "04:38" }
            val safeImsak = times.imsak.ifEmpty { "04:28" }
            val safeSyuruq = times.syuruq.ifEmpty { "05:56" }
            val safeZuhur = times.zuhur.ifEmpty { "11:57" }
            val safeAsar = times.asar.ifEmpty { "15:14" }
            val safeMaghrib = times.maghrib.ifEmpty { "18:01" }
            val safeIsya = times.isya.ifEmpty { "19:11" }

            val prayerNames = listOf("Imsak", "Subuh", "Syuruq", "Zuhur", "Asar", "Maghrib", "Isya")
            val prayerTimeStrings = listOf(
                safeImsak,
                safeSubuh,
                safeSyuruq,
                safeZuhur,
                safeAsar,
                safeMaghrib,
                safeIsya
            )
            val prayerSeconds = prayerTimeStrings.map { parseTimeToSeconds(it) }

            var foundIndex = -1
            for (i in prayerSeconds.indices) {
                if (currentSeconds < prayerSeconds[i]) {
                    foundIndex = i
                    break
                }
            }

            return if (foundIndex != -1) {
                val targetSeconds = prayerSeconds[foundIndex]
                val targetName = prayerNames[foundIndex]
                val targetTimeStr = prayerTimeStrings[foundIndex]

                val diffSeconds = (targetSeconds - currentSeconds).coerceAtLeast(0)
                val diffHours = diffSeconds / 3600
                val remainingMinutes = (diffSeconds % 3600) / 60
                val remainingSeconds = diffSeconds % 60

                val currentName = if (foundIndex > 0) prayerNames[foundIndex - 1] else "Isya"
                val countdown = when {
                    diffHours > 0 -> "${diffHours} jam ${remainingMinutes} menit ${remainingSeconds} detik lagi"
                    remainingMinutes > 0 -> "${remainingMinutes} menit ${remainingSeconds} detik lagi"
                    else -> "${remainingSeconds} detik lagi"
                }

                val highlightedFiveWaktu = when (targetName) {
                    "Imsak", "Subuh", "Syuruq" -> "Subuh"
                    else -> targetName
                }

                PrayerStatusEvaluation(
                    currentPrayerName = currentName,
                    nextPrayerName = targetName,
                    nextPrayerTime = targetTimeStr,
                    isAfterIsya = false,
                    countdownText = countdown,
                    widgetBadgeText = "Selanjutnya: $targetName $targetTimeStr",
                    widgetHighlightedPrayer = highlightedFiveWaktu
                )
            } else {
                // Already in Isya time window (after Isya and before 24:00 midnight)
                PrayerStatusEvaluation(
                    currentPrayerName = "Isya",
                    nextPrayerName = "Isya",
                    nextPrayerTime = safeIsya,
                    isAfterIsya = true,
                    countdownText = "Telah masuk waktu Isya",
                    widgetBadgeText = "Sekarang: Isya $safeIsya",
                    widgetHighlightedPrayer = "Isya"
                )
            }
        }
    }

    /**
     * Menghitung jadwal sholat berdasarkan koordinat, zona waktu, tanggal, dan ketinggian tempat.
     * 
     * @param lat Koordinat Lintang (Latitude)
     * @param lng Koordinat Bujur (Longitude)
     * @param timezone Selisih waktu dengan UTC (misal: WIB = 7.0)
     * @param altitude Ketinggian tempat dari permukaan laut dalam satuan meter (mdpl)
     */
    fun calculateTimes(
        lat: Double,
        lng: Double,
        timezone: Double,
        altitude: Double, // Menampung data ketinggian tempat untuk koreksi Maghrib & Syuruq
        year: Int,
        month: Int,
        day: Int,
        method: CalculationMethod = CalculationMethod.KEMENAG,
        asrJuristic: AsrJuristic = AsrJuristic.STANDARD
    ): PrayerTimes {
        
        // 1. Hitung Julian Date
        val jd = getJulianDate(year, month, day)

        // 2. Hitung Posisi Matahari (Solar Koordinat)
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2.0 * g)))

        val e = 23.439 - 0.00000036 * d
        val rDecl = asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l)))
        val decl = Math.toDegrees(rDecl)

        val raRad = atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))
        val ra = fixAngle(Math.toDegrees(raRad)) / 15.0

        // Equation of Time (Perata Waktu dalam satuan Jam)
        val eqt = (q / 15.0) - ra

        // Midday / Transit Utama (Waktu matahari tepat di atas kepala secara murni)
        val midday = 12.0 + timezone - (lng / 15.0) - eqt

        // --- PROSES KALKULASI SETIAP WAKTU SHOLAT ---

        // ZUHUR: Waktu transit murni ditambah 2 menit Ihtiyati (Pengaman) Kemenag
        val zuhurTime = midday + (2.0 / 60.0)

        // SYURUQ & MAGHRIB: Memperhitungkan kerendahan ufuk berdasarkan ketinggian tempat (Altitude)
        // Nilai -0.8333 adalah konstanta pembiasan cahaya atmosfer standar di permukaan laut
        val dipOfHorizon = 0.0347 * sqrt(altitude)
        val correctedSunsetAngle = -0.8333 - dipOfHorizon
        
        // Cari sudut waktu matahari untuk terbit/terbenam
        val tSunset = calcHourAngle(correctedSunsetAngle, lat, decl) ?: 6.0
        
        // Syuruq: Waktu terbit murni dikurangi 2 menit ihtiyati agar aman dari waktu larangan sholat
        val syuruqTime = (midday - tSunset) - (2.0 / 60.0)
        
        // Maghrib: Waktu terbenam murni ditambah 2 menit ihtiyati Kemenag
        val maghribTime = (midday + tSunset) + (2.0 / 60.0)

        // SUBUH: Sudut fajar berdasarkan metode yang dipilih ditambah 2 menit ihtiyati Kemenag
        val tFajr = calcHourAngle(-method.fajrAngle, lat, decl) ?: 5.0
        val subuhTime = (midday - tFajr) + (2.0 / 60.0)

        // ASAR: Sudut matahari dihitung menggunakan rumus Kontangen trigonometri bola
        val latRad = Math.toRadians(lat)
        val declRad = Math.toRadians(decl)
        val angleAsr = calculateAsrAngle(asrJuristic.shadowFactor, latRad, declRad)
        val tAsr = calcHourAngle(angleAsr, lat, decl) ?: 3.5
        val asarTime = (midday + tAsr) + (2.0 / 60.0)

        // ISYA: Penyesuaian metode sudut biasa vs metode durasi konstan (Makkah)
        val isyaTime = if (method == CalculationMethod.MAKKAH) {
            maghribTime + (method.ishaIntervalMins / 60.0)
        } else {
            val tIsha = calcHourAngle(-method.ishaAngle, lat, decl) ?: 6.5
            (midday + tIsha) + (2.0 / 60.0)
        }

        // IMSAK: Selalu 10 menit sebelum waktu Subuh yang telah dihitung
        val imsakTime = subuhTime - (10.0 / 60.0)

        // Kembalikan objek data class dalam format String HH:MM
        return PrayerTimes(
            imsak = formatTime(imsakTime),
            subuh = formatTime(subuhTime),
            syuruq = formatTime(syuruqTime),
            zuhur = formatTime(zuhurTime),
            asar = formatTime(asarTime),
            maghrib = formatTime(maghribTime),
            isya = formatTime(isyaTime)
        )
    }

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun calcHourAngle(angle: Double, lat: Double, decl: Double): Double? {
        val latRad = Math.toRadians(lat)
        val declRad = Math.toRadians(decl)
        val angleRad = Math.toRadians(angle)
        
        val cosH = (sin(angleRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        if (cosH > 1.0 || cosH < -1.0) return null
        
        return Math.toDegrees(acos(cosH)) / 15.0
    }

    // Mengembalikan sudut tinggi matahari untuk waktu Asar
    private fun calculateAsrAngle(factor: Int, latRad: Double, declRad: Double): Double {
        val diff = abs(latRad - declRad)
        val gAsr = atan(1.0 / (factor + tan(diff)))
        return Math.toDegrees(gAsr)
    }

    // Memastikan sudut derajat astronomi tetap berada di rentang positif 0 - 360 derajat
    private fun fixAngle(angle: Double): Double {
        var result = angle % 360.0
        if (result < 0) {
            result += 360.0
        }
        return result
    }

    // Melakukan konversi desimal jam ke format waktu String dengan pembulatan ke atas (Fikih Safe)
    private fun formatTime(time: Double): String {
        val cleanTime = (time + 24.0) % 24.0
        
        // Menggunakan ceil untuk melakukan pembulatan ke atas apabila ada sisa detik sekecil apapun
        val totalMins = ceil(cleanTime * 60.0).toInt()
        
        val hours = (totalMins / 60) % 24
        val minutes = totalMins % 60
        return String.format("%02d:%02d", hours, minutes)
    }
}