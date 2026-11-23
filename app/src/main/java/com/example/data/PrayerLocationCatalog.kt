package com.example.data

import android.content.Context
import java.util.TimeZone

enum class PrayerCountry(
    val code: String,
    val displayName: String,
    val authorityShort: String,
    val authorityFull: String
) {
    INDONESIA(
        code = "ID",
        displayName = "Indonesia",
        authorityShort = "Kemenag RI",
        authorityFull = "Kemenag RI (API Resmi Equran.id)"
    ),
    MALAYSIA(
        code = "MY",
        displayName = "Malaysia",
        authorityShort = "JAKIM",
        authorityFull = "JAKIM e-Solat (API Resmi Malaysia)"
    );

    companion object {
        fun fromCode(code: String?): PrayerCountry {
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true)
            } ?: INDONESIA
        }
    }
}

data class LocationPreset(
    val name: String,
    val region: String, // Provinsi (ID) or Negeri (MY)
    val country: PrayerCountry,
    val lat: Double,
    val lng: Double,
    val timeZoneId: String,
    val jakimZone: String? = null,
    val daerahDetail: String? = null
) {
    /**
     * Concise, crystal-clear label shown on the Home Screen Widget & Home Pill.
     */
    fun widgetDisplayName(): String {
        return when (country) {
            PrayerCountry.MALAYSIA -> {
                val zonePart = jakimZone?.takeIf { it.isNotBlank() }
                if (zonePart != null) "$name ($zonePart)" else name
            }
            PrayerCountry.INDONESIA -> name.removePrefix("Kota ").trim()
        }
    }
}

object PrayerLocationCatalog {

    val indonesiaPresets: List<LocationPreset> = listOf(
        LocationPreset("Kota Depok", "Jawa Barat", PrayerCountry.INDONESIA, -6.4025, 106.7942, "Asia/Jakarta"),
        LocationPreset("Kota Jakarta", "DKI Jakarta", PrayerCountry.INDONESIA, -6.2088, 106.8456, "Asia/Jakarta"),
        LocationPreset("Kota Bekasi", "Jawa Barat", PrayerCountry.INDONESIA, -6.2383, 106.9756, "Asia/Jakarta"),
        LocationPreset("Kota Bogor", "Jawa Barat", PrayerCountry.INDONESIA, -6.5971, 106.8060, "Asia/Jakarta"),
        LocationPreset("Kota Tangerang", "Banten", PrayerCountry.INDONESIA, -6.1783, 106.6319, "Asia/Jakarta"),
        LocationPreset("Kota Bandung", "Jawa Barat", PrayerCountry.INDONESIA, -6.9175, 107.6191, "Asia/Jakarta"),
        LocationPreset("Kota Semarang", "Jawa Tengah", PrayerCountry.INDONESIA, -6.9667, 110.4167, "Asia/Jakarta"),
        LocationPreset("Kota Surakarta", "Jawa Tengah", PrayerCountry.INDONESIA, -7.5667, 110.8167, "Asia/Jakarta"),
        LocationPreset("Kota Yogyakarta", "D.I. Yogyakarta", PrayerCountry.INDONESIA, -7.7956, 110.3695, "Asia/Jakarta"),
        LocationPreset("Kota Surabaya", "Jawa Timur", PrayerCountry.INDONESIA, -7.2575, 112.7521, "Asia/Jakarta"),
        LocationPreset("Kota Malang", "Jawa Timur", PrayerCountry.INDONESIA, -7.9797, 112.6304, "Asia/Jakarta"),
        LocationPreset("Kota Denpasar", "Bali", PrayerCountry.INDONESIA, -8.6705, 115.2126, "Asia/Makassar"),
        LocationPreset("Kota Medan", "Sumatera Utara", PrayerCountry.INDONESIA, 3.5952, 98.6722, "Asia/Jakarta"),
        LocationPreset("Kota Banda Aceh", "Aceh", PrayerCountry.INDONESIA, 5.5483, 95.3238, "Asia/Jakarta"),
        LocationPreset("Kota Padang", "Sumatera Barat", PrayerCountry.INDONESIA, -0.9471, 100.4172, "Asia/Jakarta"),
        LocationPreset("Kota Pekanbaru", "Riau", PrayerCountry.INDONESIA, 0.5070, 101.4478, "Asia/Jakarta"),
        LocationPreset("Kota Palembang", "Sumatera Selatan", PrayerCountry.INDONESIA, -2.9909, 104.7567, "Asia/Jakarta"),
        LocationPreset("Kota Bandar Lampung", "Lampung", PrayerCountry.INDONESIA, -5.3971, 105.2667, "Asia/Jakarta"),
        LocationPreset("Kota Pontianak", "Kalimantan Barat", PrayerCountry.INDONESIA, -0.0263, 109.3425, "Asia/Jakarta"),
        LocationPreset("Kota Banjarmasin", "Kalimantan Selatan", PrayerCountry.INDONESIA, -3.3186, 114.5944, "Asia/Makassar"),
        LocationPreset("Kota Samarinda", "Kalimantan Timur", PrayerCountry.INDONESIA, -0.4949, 117.1436, "Asia/Makassar"),
        LocationPreset("Kota Balikpapan", "Kalimantan Timur", PrayerCountry.INDONESIA, -1.2654, 116.8312, "Asia/Makassar"),
        LocationPreset("Kota Manado", "Sulawesi Utara", PrayerCountry.INDONESIA, 1.4748, 124.8428, "Asia/Makassar"),
        LocationPreset("Kota Makassar", "Sulawesi Selatan", PrayerCountry.INDONESIA, -5.1476, 119.4327, "Asia/Makassar"),
        LocationPreset("Kota Palu", "Sulawesi Tengah", PrayerCountry.INDONESIA, -0.8917, 119.8707, "Asia/Makassar"),
        LocationPreset("Kota Kendari", "Sulawesi Tenggara", PrayerCountry.INDONESIA, -3.9804, 122.5149, "Asia/Makassar"),
        LocationPreset("Kota Mataram", "Nusa Tenggara Barat", PrayerCountry.INDONESIA, -8.5833, 116.1167, "Asia/Makassar"),
        LocationPreset("Kota Kupang", "Nusa Tenggara Timur", PrayerCountry.INDONESIA, -10.1772, 123.6078, "Asia/Makassar"),
        LocationPreset("Kota Ambon", "Maluku", PrayerCountry.INDONESIA, -3.6954, 128.1814, "Asia/Jayapura"),
        LocationPreset("Kota Jayapura", "Papua", PrayerCountry.INDONESIA, -2.5488, 140.7178, "Asia/Jayapura")
    )

    /**
     * Official JAKIM e-Solat Malaysia zones across all 13 states & 3 federal territories.
     * All Malaysia zones officially use Malaysia Time (MYT / Asia/Kuala_Lumpur, UTC+8).
     */
    val malaysiaPresets: List<LocationPreset> = listOf(
        // Wilayah Persekutuan
        LocationPreset("Kuala Lumpur & Putrajaya", "Wilayah Persekutuan", PrayerCountry.MALAYSIA, 3.1390, 101.6869, "Asia/Kuala_Lumpur", "WLY01", "Kuala Lumpur, Putrajaya"),
        LocationPreset("Labuan", "Wilayah Persekutuan", PrayerCountry.MALAYSIA, 5.2831, 115.2308, "Asia/Kuala_Lumpur", "WLY02", "Wilayah Persekutuan Labuan"),
        // Selangor
        LocationPreset("Shah Alam & Petaling", "Selangor", PrayerCountry.MALAYSIA, 3.0733, 101.5185, "Asia/Kuala_Lumpur", "SGR01", "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Shah Alam"),
        LocationPreset("Kuala Selangor", "Selangor", PrayerCountry.MALAYSIA, 3.3500, 101.2500, "Asia/Kuala_Lumpur", "SGR02", "Kuala Selangor, Sabak Bernam"),
        LocationPreset("Klang & Kuala Langat", "Selangor", PrayerCountry.MALAYSIA, 3.0449, 101.4456, "Asia/Kuala_Lumpur", "SGR03", "Klang, Kuala Langat"),
        // Johor
        LocationPreset("Johor Bahru", "Johor", PrayerCountry.MALAYSIA, 1.4927, 103.7414, "Asia/Kuala_Lumpur", "JHR02", "Johor Bahru, Kota Tinggi, Mersing, Kulai"),
        LocationPreset("Kluang & Pontian", "Johor", PrayerCountry.MALAYSIA, 2.0301, 103.3185, "Asia/Kuala_Lumpur", "JHR03", "Kluang, Pontian"),
        LocationPreset("Batu Pahat & Muar", "Johor", PrayerCountry.MALAYSIA, 1.8548, 102.9325, "Asia/Kuala_Lumpur", "JHR04", "Batu Pahat, Muar, Segamat, Gemas Johor, Tangkak"),
        LocationPreset("Pulau Aur & Pemanggil", "Johor", PrayerCountry.MALAYSIA, 2.4480, 104.5240, "Asia/Kuala_Lumpur", "JHR01", "Pulau Aur dan Pulau Pemanggil"),
        // Pulau Pinang
        LocationPreset("Pulau Pinang", "Pulau Pinang", PrayerCountry.MALAYSIA, 5.4141, 100.3288, "Asia/Kuala_Lumpur", "PNG01", "Seluruh Negeri Pulau Pinang (George Town, Butterworth)"),
        // Melaka
        LocationPreset("Melaka", "Melaka", PrayerCountry.MALAYSIA, 2.1896, 102.2501, "Asia/Kuala_Lumpur", "MLK01", "Seluruh Negeri Melaka"),
        // Negeri Sembilan
        LocationPreset("Seremban & Port Dickson", "Negeri Sembilan", PrayerCountry.MALAYSIA, 2.7297, 101.9381, "Asia/Kuala_Lumpur", "NGS02", "Jelebu, Kuala Pilah, Port Dickson, Rembau, Seremban"),
        LocationPreset("Tampin & Jempol", "Negeri Sembilan", PrayerCountry.MALAYSIA, 2.4701, 102.2302, "Asia/Kuala_Lumpur", "NGS01", "Tampin, Jempol"),
        // Perak
        LocationPreset("Ipoh & Kuala Kangsar", "Perak", PrayerCountry.MALAYSIA, 4.5975, 101.0901, "Asia/Kuala_Lumpur", "PRK02", "Kuala Kangsar, Sg. Siput, Ipoh, Batu Gajah, Kampar"),
        LocationPreset("Taiping & Selama", "Perak", PrayerCountry.MALAYSIA, 4.8519, 100.7416, "Asia/Kuala_Lumpur", "PRK06", "Selama, Taiping, Bagan Serai, Parit Buntar"),
        LocationPreset("Tanjung Malim & Tapah", "Perak", PrayerCountry.MALAYSIA, 3.6833, 101.5167, "Asia/Kuala_Lumpur", "PRK01", "Tapah, Slim River, Tanjung Malim"),
        LocationPreset("Lumut & Manjung", "Perak", PrayerCountry.MALAYSIA, 4.2333, 100.6333, "Asia/Kuala_Lumpur", "PRK05", "Kg Gajah, Teluk Intan, Bagan Datuk, Seri Iskandar, Lumut, Sitiawan"),
        LocationPreset("Gerik & Lenggong", "Perak", PrayerCountry.MALAYSIA, 5.4333, 101.1333, "Asia/Kuala_Lumpur", "PRK03", "Lenggong, Pengkalan Hulu, Grik"),
        // Kedah
        LocationPreset("Alor Setar", "Kedah", PrayerCountry.MALAYSIA, 6.1248, 100.3678, "Asia/Kuala_Lumpur", "KDH01", "Kota Setar, Kubang Pasu, Pokok Sena"),
        LocationPreset("Sungai Petani", "Kedah", PrayerCountry.MALAYSIA, 5.6470, 100.4877, "Asia/Kuala_Lumpur", "KDH02", "Kuala Muda, Yan, Pendang"),
        LocationPreset("Kulim & Baling", "Kedah", PrayerCountry.MALAYSIA, 5.3649, 100.5618, "Asia/Kuala_Lumpur", "KDH04", "Baling, Kulim, Bandar Baharu"),
        LocationPreset("Langkawi", "Kedah", PrayerCountry.MALAYSIA, 6.3500, 99.8000, "Asia/Kuala_Lumpur", "KDH06", "Pulau Langkawi"),
        // Perlis
        LocationPreset("Kangar (Perlis)", "Perlis", PrayerCountry.MALAYSIA, 6.4414, 100.1986, "Asia/Kuala_Lumpur", "PLS01", "Kangar, Padang Besar, Arau"),
        // Pahang
        LocationPreset("Kuantan & Pekan", "Pahang", PrayerCountry.MALAYSIA, 3.8077, 103.3260, "Asia/Kuala_Lumpur", "PHG02", "Kuantan, Pekan, Rompin, Muadzam Shah"),
        LocationPreset("Temerloh & Jerantut", "Pahang", PrayerCountry.MALAYSIA, 3.4506, 102.4176, "Asia/Kuala_Lumpur", "PHG03", "Jerantut, Temerloh, Maran, Bera, Chenor, Jengka"),
        LocationPreset("Bentong & Raub", "Pahang", PrayerCountry.MALAYSIA, 3.5223, 101.9087, "Asia/Kuala_Lumpur", "PHG04", "Bentong, Lipis, Raub"),
        LocationPreset("Cameron Highlands", "Pahang", PrayerCountry.MALAYSIA, 4.4721, 101.3801, "Asia/Kuala_Lumpur", "PHG06", "Cameron Highlands, Genting Highlands, Bukit Fraser"),
        // Terengganu
        LocationPreset("Kuala Terengganu", "Terengganu", PrayerCountry.MALAYSIA, 5.3296, 103.1370, "Asia/Kuala_Lumpur", "TRG01", "Kuala Terengganu, Marang, Kuala Nerus"),
        LocationPreset("Besut & Setiu", "Terengganu", PrayerCountry.MALAYSIA, 5.8333, 102.5500, "Asia/Kuala_Lumpur", "TRG02", "Besut, Setiu"),
        LocationPreset("Dungun & Kemaman", "Terengganu", PrayerCountry.MALAYSIA, 4.7500, 103.4167, "Asia/Kuala_Lumpur", "TRG04", "Dungun, Kemaman"),
        // Kelantan
        LocationPreset("Kota Bharu", "Kelantan", PrayerCountry.MALAYSIA, 6.1254, 102.2381, "Asia/Kuala_Lumpur", "KTN01", "Bachok, Kota Bharu, Machang, Pasir Mas, Pasir Puteh, Tanah Merah, Tumpat"),
        LocationPreset("Gua Musang & Jeli", "Kelantan", PrayerCountry.MALAYSIA, 4.8844, 101.9686, "Asia/Kuala_Lumpur", "KTN02", "Gua Musang, Jeli, Lojing"),
        // Sabah
        LocationPreset("Kota Kinabalu", "Sabah", PrayerCountry.MALAYSIA, 5.9804, 116.0735, "Asia/Kuala_Lumpur", "SBH07", "Kota Kinabalu, Ranau, Kota Belud, Tuaran, Penampang, Papar, Putatan"),
        LocationPreset("Sandakan", "Sabah", PrayerCountry.MALAYSIA, 5.8402, 118.1179, "Asia/Kuala_Lumpur", "SBH01", "Bahagian Sandakan (Timur), Bukit Garam, Semawang, Temanggong"),
        LocationPreset("Tawau & Lahad Datu", "Sabah", PrayerCountry.MALAYSIA, 4.2446, 117.8912, "Asia/Kuala_Lumpur", "SBH03", "Lahad Datu, Silabukan, Kunak, Sahabat, Semporna, Tungku"),
        LocationPreset("Keningau & Beaufort", "Sabah", PrayerCountry.MALAYSIA, 5.3378, 116.1602, "Asia/Kuala_Lumpur", "SBH09", "Beaufort, Kuala Penyu, Sipitang, Tenom, Long Pa Sia, Membakut, Weston"),
        // Sarawak
        LocationPreset("Kuching & Samarahan", "Sarawak", PrayerCountry.MALAYSIA, 1.5533, 110.3592, "Asia/Kuala_Lumpur", "SWK08", "Kuching, Bau, Lundu, Samarahan"),
        LocationPreset("Miri & Niah", "Sarawak", PrayerCountry.MALAYSIA, 4.3995, 113.9914, "Asia/Kuala_Lumpur", "SWK02", "Miri, Niah, Bekenu, Sibuti, Marudi"),
        LocationPreset("Sibu & Mukah", "Sarawak", PrayerCountry.MALAYSIA, 2.2873, 111.8305, "Asia/Kuala_Lumpur", "SWK04", "Sibu, Mukah, Dalat, Song, Igan, Oya, Balingian, Kanowit, Kapit"),
        LocationPreset("Bintulu & Tatau", "Sarawak", PrayerCountry.MALAYSIA, 3.1667, 113.0333, "Asia/Kuala_Lumpur", "SWK03", "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu"),
        LocationPreset("Limbang & Lawas", "Sarawak", PrayerCountry.MALAYSIA, 4.7500, 115.0000, "Asia/Kuala_Lumpur", "SWK01", "Limbang, Lawas, Sundar, Trusan")
    )

    val allPresets: List<LocationPreset> = indonesiaPresets + malaysiaPresets

    fun findByNameOrZone(name: String?, jakimZone: String? = null): LocationPreset? {
        if (!jakimZone.isNullOrBlank()) {
            malaysiaPresets.firstOrNull { it.jakimZone.equals(jakimZone, ignoreCase = true) }?.let { return it }
        }
        if (!name.isNullOrBlank()) {
            allPresets.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun findClosestPreset(lat: Double, lng: Double, countryFilter: PrayerCountry? = null): LocationPreset {
        val candidates = when (countryFilter) {
            PrayerCountry.INDONESIA -> indonesiaPresets
            PrayerCountry.MALAYSIA -> malaysiaPresets
            null -> allPresets
        }
        var closest = candidates.first()
        var minDistance = Double.MAX_VALUE
        for (city in candidates) {
            val dLat = city.lat - lat
            val dLng = city.lng - lng
            val dist = dLat * dLat + dLng * dLng
            if (dist < minDistance) {
                minDistance = dist
                closest = city
            }
        }
        return closest
    }

    /**
     * Checks whether GPS coordinates roughly fall within Peninsular or East Malaysia bounding boxes.
     */
    fun isLikelyInMalaysia(lat: Double, lng: Double): Boolean {
        val inPeninsularMalaysia = lat in 1.2..6.8 && lng in 99.5..104.6
        val inEastMalaysia = lat in 0.85..7.5 && lng in 109.5..119.3
        return inPeninsularMalaysia || inEastMalaysia
    }

    fun resolveTimeZone(context: Context): TimeZone {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val savedTz = prefs.getString("timezone_id", null)
        if (!savedTz.isNullOrBlank()) {
            return TimeZone.getTimeZone(savedTz)
        }
        val country = PrayerCountry.fromCode(prefs.getString("country_code", PrayerCountry.INDONESIA.code))
        if (country == PrayerCountry.MALAYSIA) {
            return TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        }
        val locationName = prefs.getString("location_name", null)
        val jakimZone = prefs.getString("jakim_zone", null)
        findByNameOrZone(locationName, jakimZone)?.let {
            return TimeZone.getTimeZone(it.timeZoneId)
        }
        val lng = prefs.getFloat("longitude", 106.7942f).toDouble()
        return when {
            lng < 114.0 -> TimeZone.getTimeZone("Asia/Jakarta")
            lng < 126.0 -> TimeZone.getTimeZone("Asia/Makassar")
            else -> TimeZone.getTimeZone("Asia/Jayapura")
        }
    }

    fun getSavedLocationPreset(context: Context): LocationPreset {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("location_name", "Kota Depok") ?: "Kota Depok"
        val region = prefs.getString("provinsi_name", "Jawa Barat") ?: "Jawa Barat"
        val country = PrayerCountry.fromCode(prefs.getString("country_code", PrayerCountry.INDONESIA.code))
        val jakimZone = prefs.getString("jakim_zone", null)
        val lat = prefs.getFloat("latitude", -6.4025f).toDouble()
        val lng = prefs.getFloat("longitude", 106.7942f).toDouble()
        val tzId = prefs.getString("timezone_id", null)
            ?: if (country == PrayerCountry.MALAYSIA) "Asia/Kuala_Lumpur" else {
                findByNameOrZone(name, jakimZone)?.timeZoneId ?: "Asia/Jakarta"
            }

        findByNameOrZone(name, jakimZone)?.let { matched ->
            if (matched.country == country && matched.name.equals(name, ignoreCase = true)) {
                return matched
            }
        }

        return LocationPreset(
            name = name,
            region = region,
            country = country,
            lat = lat,
            lng = lng,
            timeZoneId = tzId,
            jakimZone = if (country == PrayerCountry.MALAYSIA) (jakimZone ?: "WLY01") else null
        )
    }

    fun saveLocationPreset(context: Context, preset: LocationPreset, isAutoGps: Boolean? = null) {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putFloat("latitude", preset.lat.toFloat())
            .putFloat("longitude", preset.lng.toFloat())
            .putString("location_name", preset.name)
            .putString("provinsi_name", preset.region)
            .putString("country_code", preset.country.code)
            .putString("timezone_id", preset.timeZoneId)
            .putString("jakim_zone", preset.jakimZone ?: "")
        if (isAutoGps != null) {
            editor.putBoolean("use_auto_gps", isAutoGps)
        }
        editor.apply()
    }
}
