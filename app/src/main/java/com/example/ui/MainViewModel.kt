package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import com.example.data.api.AladhanApiClient
import com.example.data.api.EquranApiClient
import com.example.data.api.MalaysiaPrayerApi
import com.example.utils.HijriDateHelper
import com.example.utils.PrayerAlarmScheduler
import com.example.utils.PrayerTimesCalculator
import com.example.utils.WidgetLocationSyncHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    data class FetchQuery(
        val location: LocationPreset,
        val year: Int,
        val month: Int,
        val day: Int,
        val refreshNonce: Int = 0
    )

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val bookmarkRepo = BookmarkRepository(db.bookmarkDao())
    private val tasbihRepo = TasbihRepository(db.tasbihDao())
    private val prayerSettingRepo = PrayerSettingRepository(db.prayerSettingDao())

    private val prefs = application.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)

    // 1. Unified Location State (Indonesia & Malaysia with exact IANA TimeZone & JAKIM Zone)
    private val initialLocation = PrayerLocationCatalog.getSavedLocationPreset(application)

    private val _currentLocation = MutableStateFlow(initialLocation)
    val currentLocation: StateFlow<LocationPreset> = _currentLocation.asStateFlow()

    val latitude: StateFlow<Double> = _currentLocation
        .map { it.lat }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialLocation.lat)

    val longitude: StateFlow<Double> = _currentLocation
        .map { it.lng }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialLocation.lng)

    val locationName: StateFlow<String> = _currentLocation
        .map { it.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialLocation.name)

    val activeCountry: StateFlow<PrayerCountry> = _currentLocation
        .map { it.country }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialLocation.country)

    val activeJakimZone: StateFlow<String?> = _currentLocation
        .map { it.jakimZone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialLocation.jakimZone)

    val activeAuthorityLabel: StateFlow<String> = _currentLocation
        .map { it.country.authorityFull }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialLocation.country.authorityFull)

    private val _useAutoGps = MutableStateFlow(prefs.getBoolean("use_auto_gps", true))
    val useAutoGps: StateFlow<Boolean> = _useAutoGps.asStateFlow()

    private var hasRunStartupGps = false

    private fun currentTimeZone(): TimeZone {
        val tzId = _currentLocation.value.timeZoneId.ifBlank {
            if (_currentLocation.value.country == PrayerCountry.MALAYSIA) "Asia/Kuala_Lumpur" else "Asia/Jakarta"
        }
        return TimeZone.getTimeZone(tzId)
    }

    // 2. Date state — ONLY updates when the calendar day or timezone changes (prevents 1s recomposition/SharedPrefs spam)
    private val _currentCalendar = MutableStateFlow(Calendar.getInstance(currentTimeZone()))
    val currentCalendar: StateFlow<Calendar> = _currentCalendar.asStateFlow()

    private var lastCalendarDaySignature: String = calendarDaySignature(_currentCalendar.value)

    private fun calendarDaySignature(cal: Calendar): String {
        return "${cal.timeZone.id}-${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun syncCalendarDayIfNeeded() {
        val now = Calendar.getInstance(currentTimeZone())
        val sig = calendarDaySignature(now)
        if (sig != lastCalendarDaySignature) {
            lastCalendarDaySignature = sig
            _currentCalendar.value = now
        }
    }

    private val _apiHijriDate = MutableStateFlow<String?>(
        if (HijriDateHelper.hasCachedHijriDate(application, _currentCalendar.value)) {
            HijriDateHelper.getHijriDate(application, _currentCalendar.value)
        } else {
            null
        }
    )
    val apiHijriDate: StateFlow<String?> = _apiHijriDate.asStateFlow()

    val hijriDateString: StateFlow<String> = combine(_apiHijriDate, _currentCalendar) { apiHijri, cal ->
        apiHijri ?: HijriDateHelper.getHijriDate(application, cal)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HijriDateHelper.getHijriDate(application, _currentCalendar.value)
    )

    val gregorianDateString: StateFlow<String> = combine(_currentCalendar, _currentLocation) { cal, loc ->
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone(loc.timeZoneId)
        }
        sdf.format(cal.time)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // 3. Prayer Settings (Alert toggles)
    val prayerSettings: StateFlow<List<PrayerSetting>> = prayerSettingRepo.getAllSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 4. Official API Prayer Times State (Indonesia Kemenag API / Malaysia JAKIM API — NO manual calculation)
    private val emptyPrayerTimes = PrayerTimesCalculator.PrayerTimes("", "", "", "", "", "", "")

    private val _apiPrayerTimes = MutableStateFlow<PrayerTimesCalculator.PrayerTimes?>(
        PrayerAlarmScheduler.getCachedPrayerTimesSync(
            application,
            initialLocation,
            _currentCalendar.value.get(Calendar.YEAR),
            _currentCalendar.value.get(Calendar.MONTH) + 1,
            _currentCalendar.value.get(Calendar.DAY_OF_MONTH)
        )
    )

    private val _isPrayerTimesLoading = MutableStateFlow(_apiPrayerTimes.value == null)
    val isPrayerTimesLoading: StateFlow<Boolean> = _isPrayerTimesLoading.asStateFlow()

    private val _prayerTimesError = MutableStateFlow<String?>(null)
    val prayerTimesError: StateFlow<String?> = _prayerTimesError.asStateFlow()

    private val _manualRefreshNonce = MutableStateFlow(0)

    private val apiScheduleCache = mutableMapOf<String, PrayerTimesCalculator.PrayerTimes>()

    val prayerTimes: StateFlow<PrayerTimesCalculator.PrayerTimes> = _apiPrayerTimes
        .map { it ?: emptyPrayerTimes }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _apiPrayerTimes.value ?: emptyPrayerTimes)

    // 5. Next Prayer and Countdown state
    private val _nextPrayerName = MutableStateFlow("-")
    val nextPrayerName: StateFlow<String> = _nextPrayerName.asStateFlow()

    private val _currentPrayerName = MutableStateFlow("")
    val currentPrayerName: StateFlow<String> = _currentPrayerName.asStateFlow()

    private val _nextPrayerTime = MutableStateFlow("--:--")
    val nextPrayerTime: StateFlow<String> = _nextPrayerTime.asStateFlow()

    private val _countdownText = MutableStateFlow("Memuat jadwal resmi...")
    val countdownText: StateFlow<String> = _countdownText.asStateFlow()

    // 6. Tasbih State
    val tasbihState: StateFlow<Tasbih> = tasbihRepo.getTasbih()
        .map { it ?: Tasbih(1, 0, 33, "Subhanallah") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Tasbih(1, 0, 33, "Subhanallah"))

    // 7. Bookmark State (Terakhir Baca)
    val bookmarkState: StateFlow<Bookmark?> = bookmarkRepo.getLastReadBookmark()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Quran API Fetched States ---
    private val _quranSurahs = MutableStateFlow<List<Surah>>(emptyList())
    val quranSurahs: StateFlow<List<Surah>> = _quranSurahs.asStateFlow()

    private val _currentSurahDetail = MutableStateFlow<Surah?>(null)
    val currentSurahDetail: StateFlow<Surah?> = _currentSurahDetail.asStateFlow()

    private val _isQuranLoading = MutableStateFlow(false)
    val isQuranLoading: StateFlow<Boolean> = _isQuranLoading.asStateFlow()

    private val _quranError = MutableStateFlow<String?>(null)
    val quranError: StateFlow<String?> = _quranError.asStateFlow()

    fun loadQuranSurahList() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_quranSurahs.value.isNotEmpty()) return@launch
            _isQuranLoading.value = true
            _quranError.value = null
            try {
                val response = EquranApiClient.service.getSurahList()
                if (response.code == 200 && response.data != null) {
                    val list = response.data.map { apiSurah ->
                        Surah(
                            id = apiSurah.nomor,
                            name = apiSurah.namaLatin,
                            arabicName = apiSurah.nama,
                            translation = apiSurah.arti,
                            totalAyat = apiSurah.jumlahAyat,
                            type = apiSurah.tempatTurun.replace("Mekah", "Makkiyah").replace("Madinah", "Madaniyah")
                        )
                    }
                    _quranSurahs.value = list
                } else {
                    _quranSurahs.value = QuranDataset.surahs
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _quranSurahs.value = QuranDataset.surahs
            } finally {
                _isQuranLoading.value = false
            }
        }
    }

    fun loadSurahDetail(surahId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isQuranLoading.value = true
            _quranError.value = null
            _currentSurahDetail.value = null
            try {
                val response = EquranApiClient.service.getSurahDetail(surahId)
                if (response.code == 200 && response.data != null) {
                    val apiDetail = response.data
                    val ayats = apiDetail.ayat.map { apiAyat ->
                        Ayat(
                            number = apiAyat.nomorAyat,
                            arabic = apiAyat.teksArab,
                            translation = apiAyat.teksIndonesia,
                            latin = apiAyat.teksLatin,
                            audio = apiAyat.audio
                        )
                    }
                    _currentSurahDetail.value = Surah(
                        id = apiDetail.nomor,
                        name = apiDetail.namaLatin,
                        arabicName = apiDetail.nama,
                        translation = apiDetail.arti,
                        totalAyat = apiDetail.jumlahAyat,
                        type = apiDetail.tempatTurun.replace("Mekah", "Makkiyah").replace("Madinah", "Madaniyah"),
                        ayats = ayats
                    )
                } else {
                    _currentSurahDetail.value = QuranDataset.getSurahById(surahId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _currentSurahDetail.value = QuranDataset.getSurahById(surahId)
            } finally {
                _isQuranLoading.value = false
            }
        }
    }

    // --- Quran Audio Playback States & Methods ---
    data class Qori(val id: String, val name: String)

    val qoriList = listOf(
        Qori("01", "Abdullah Al-Juhany"),
        Qori("02", "Abdul Muhsin Al-Qasim"),
        Qori("03", "Abdurrahman As-Sudais"),
        Qori("04", "Ibrahim Al-Dossari"),
        Qori("05", "Misyari Rasyid Al-Afasy")
    )

    private val _selectedQori = MutableStateFlow(qoriList[4])
    val selectedQori: StateFlow<Qori> = _selectedQori.asStateFlow()

    private val _playingSurahId = MutableStateFlow<Int?>(null)
    val playingSurahId: StateFlow<Int?> = _playingSurahId.asStateFlow()

    private val _playingAyatNumber = MutableStateFlow<Int?>(null)
    val playingAyatNumber: StateFlow<Int?> = _playingAyatNumber.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _isAudioLoading = MutableStateFlow(false)
    val isAudioLoading: StateFlow<Boolean> = _isAudioLoading.asStateFlow()

    private var quranMediaPlayer: MediaPlayer? = null

    fun selectQori(qori: Qori) {
        _selectedQori.value = qori
        val surahId = _playingSurahId.value
        val ayatNum = _playingAyatNumber.value
        if (surahId != null && ayatNum != null && _isAudioPlaying.value) {
            playQuranAudio(surahId, ayatNum)
        }
    }

    fun playQuranAudio(surahId: Int, ayatNumber: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _isAudioLoading.value = true
                quranMediaPlayer?.release()
                quranMediaPlayer = null

                _playingSurahId.value = surahId
                _playingAyatNumber.value = ayatNumber
                _isAudioPlaying.value = false

                val detail = _currentSurahDetail.value
                if (detail == null || detail.id != surahId) {
                    _isAudioLoading.value = false
                    return@launch
                }

                val ayat = detail.ayats.find { it.number == ayatNumber }
                if (ayat == null) {
                    _isAudioLoading.value = false
                    return@launch
                }

                val audioUrl = ayat.audio?.get(_selectedQori.value.id)
                if (audioUrl.isNullOrEmpty()) {
                    _isAudioLoading.value = false
                    return@launch
                }

                val mp = MediaPlayer()
                mp.setDataSource(audioUrl)
                mp.prepareAsync()
                mp.setOnPreparedListener { player ->
                    _isAudioLoading.value = false
                    _isAudioPlaying.value = true
                    player.start()
                }
                mp.setOnCompletionListener { player ->
                    player.release()
                    quranMediaPlayer = null
                    _isAudioPlaying.value = false
                    playNextAyat()
                }
                mp.setOnErrorListener { player, _, _ ->
                    player.release()
                    quranMediaPlayer = null
                    _isAudioLoading.value = false
                    _isAudioPlaying.value = false
                    true
                }
                quranMediaPlayer = mp
            } catch (e: Exception) {
                e.printStackTrace()
                _isAudioLoading.value = false
                _isAudioPlaying.value = false
            }
        }
    }

    fun stopQuranAudio() {
        quranMediaPlayer?.release()
        quranMediaPlayer = null
        _isAudioPlaying.value = false
        _isAudioLoading.value = false
        _playingSurahId.value = null
        _playingAyatNumber.value = null
    }

    fun togglePlayPauseQuranAudio(surahId: Int, ayatNumber: Int) {
        if (_playingSurahId.value == surahId && _playingAyatNumber.value == ayatNumber) {
            if (_isAudioPlaying.value) {
                quranMediaPlayer?.pause()
                _isAudioPlaying.value = false
            } else {
                if (quranMediaPlayer != null) {
                    quranMediaPlayer?.start()
                    _isAudioPlaying.value = true
                } else {
                    playQuranAudio(surahId, ayatNumber)
                }
            }
        } else {
            playQuranAudio(surahId, ayatNumber)
        }
    }

    fun playNextAyat() {
        val surahId = _playingSurahId.value ?: return
        val ayatNum = _playingAyatNumber.value ?: return
        val detail = _currentSurahDetail.value ?: return
        if (detail.id != surahId) return

        val nextAyatNum = ayatNum + 1
        val nextAyat = detail.ayats.find { it.number == nextAyatNum }
        if (nextAyat != null) {
            playQuranAudio(surahId, nextAyatNum)
        } else {
            stopQuranAudio()
        }
    }

    private val _azanSound = MutableStateFlow(prefs.getString("azan_sound", "Makkah") ?: "Makkah")
    val azanSound: StateFlow<String> = _azanSound.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // Keep ViewModel live-synced if PrayerWidgetProvider updates location via FusedLocationProviderClient in background
    private val prefsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "location_name" || key == "jakim_zone" || key == "country_code" || key == "use_auto_gps") {
            val savedAutoGps = prefs.getBoolean("use_auto_gps", true)
            if (_useAutoGps.value != savedAutoGps) {
                _useAutoGps.value = savedAutoGps
            }
            val latestSaved = PrayerLocationCatalog.getSavedLocationPreset(getApplication())
            val current = _currentLocation.value
            val changed = !latestSaved.name.equals(current.name, ignoreCase = true) ||
                latestSaved.country != current.country ||
                !latestSaved.jakimZone.orEmpty().equals(current.jakimZone.orEmpty(), ignoreCase = true)
            if (changed) {
                _currentLocation.value = latestSaved
                lastCalendarDaySignature = ""
                syncCalendarDayIfNeeded()
                refreshCountdown()
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)
        // Ensure widget real-time FusedLocationProviderClient listener is registered if eligible
        WidgetLocationSyncHelper.registerWidgetLocationUpdates(application)
        // Reactive official API schedule fetching (Kemenag RI for Indonesia, JAKIM e-Solat for Malaysia)
        viewModelScope.launch {
            combine(_currentLocation, _currentCalendar, _manualRefreshNonce) { loc, cal, nonce ->
                FetchQuery(
                    location = loc,
                    year = cal.get(Calendar.YEAR),
                    month = cal.get(Calendar.MONTH) + 1,
                    day = cal.get(Calendar.DAY_OF_MONTH),
                    refreshNonce = nonce
                )
            }
            .distinctUntilChanged()
            .collectLatest { query ->
                val cacheKey = "${query.location.country.code}-${query.location.jakimZone ?: query.location.name}-${query.year}-${query.month}-${query.day}"
                val cached = apiScheduleCache[cacheKey]
                    ?: PrayerAlarmScheduler.getCachedPrayerTimesSync(
                        application,
                        query.location,
                        query.year,
                        query.month,
                        query.day
                    )
                if (cached != null) {
                    apiScheduleCache[cacheKey] = cached
                    _apiPrayerTimes.value = cached
                    _isPrayerTimesLoading.value = false
                    _prayerTimesError.value = null
                    refreshCountdown()
                } else {
                    _apiPrayerTimes.value = null
                    _isPrayerTimesLoading.value = true
                    _prayerTimesError.value = null
                }
                fetchOfficialSchedule(query, cacheKey)
            }
        }

        // Fetch Aladhan Hijri Date reactively when calendar day or location timezone changes
        viewModelScope.launch {
            combine(_currentCalendar, _currentLocation) { cal, loc ->
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone(loc.timeZoneId)
                }
                Triple(sdf.format(cal.time), cal, loc)
            }
            .distinctUntilChanged { old, new ->
                old.first == new.first && old.third.country == new.third.country
            }
            .collectLatest { (dateStr, cal, loc) ->
                if (loc.country == PrayerCountry.MALAYSIA) {
                    val zone = loc.jakimZone ?: "WLY01"
                    val cachedMy = MalaysiaPrayerApi.getCachedDailyPrayerTimesSync(
                        application,
                        zone,
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    if (!cachedMy?.hijriDateFormatted.isNullOrBlank()) {
                        _apiHijriDate.value = cachedMy?.hijriDateFormatted
                        return@collectLatest
                    }
                }
                fetchAladhanHijri(dateStr, cal)
            }
        }

        // Start live 1-second countdown refresh loop (without mutating _currentCalendar unless day changes!)
        viewModelScope.launch {
            while (true) {
                refreshCountdown()
                delay(1000)
            }
        }

        // Reactive scheduling of background alarms whenever official prayer times or settings change
        viewModelScope.launch {
            combine(prayerTimes, prayerSettings, _azanSound) { times, settings, sound ->
                Triple(times, settings, sound)
            }
            .distinctUntilChanged()
            .collect { (times, settings, sound) ->
                if (times.zuhur.isNotEmpty() && times.subuh.isNotEmpty()) {
                    PrayerAlarmScheduler.scheduleAlarms(application, times, settings, sound)
                }
            }
        }
    }

    fun retryFetchPrayerTimes() {
        _manualRefreshNonce.value += 1
    }

    private suspend fun fetchAladhanHijri(dateStr: String, cal: Calendar) {
        withContext(Dispatchers.IO) {
            try {
                val response = AladhanApiClient.service.getHijriDate(dateStr)
                if (response.code == 200 && response.data?.hijri != null) {
                    val hijri = response.data.hijri
                    val day = hijri.day
                    val enMonth = hijri.month?.en ?: ""
                    val indonesianMonth = HijriDateHelper.translateHijriMonthToIndonesian(enMonth)
                    val year = hijri.year
                    val hijriStr = "$day $indonesianMonth $year H"
                    HijriDateHelper.saveCachedHijriDate(getApplication(), cal, hijriStr)
                    withContext(Dispatchers.Main) {
                        _apiHijriDate.value = hijriStr
                        com.example.receiver.PrayerWidgetProvider.updateAllWidgets(getApplication())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchOfficialSchedule(query: FetchQuery, cacheKey: String) {
        withContext(Dispatchers.IO) {
            val times = PrayerAlarmScheduler.getPrayerTimesFromApiOrCache(
                context = getApplication(),
                location = query.location,
                year = query.year,
                month = query.month,
                day = query.day
            )
            // If Malaysia API also provided official JAKIM Hijri date, sync it right away
            val jakimHijri = if (query.location.country == PrayerCountry.MALAYSIA) {
                MalaysiaPrayerApi.getCachedDailyPrayerTimesSync(
                    getApplication(),
                    query.location.jakimZone ?: "WLY01",
                    query.year,
                    query.month,
                    query.day
                )?.hijriDateFormatted
            } else {
                null
            }

            withContext(Dispatchers.Main) {
                _isPrayerTimesLoading.value = false
                if (times != null) {
                    apiScheduleCache[cacheKey] = times
                    _apiPrayerTimes.value = times
                    _prayerTimesError.value = null
                    if (!jakimHijri.isNullOrBlank()) {
                        _apiHijriDate.value = jakimHijri
                    }
                    refreshCountdown()
                    com.example.receiver.PrayerWidgetProvider.updateAllWidgets(getApplication())
                } else if (_apiPrayerTimes.value == null) {
                    _prayerTimesError.value = "Gagal memuat jadwal resmi ${query.location.country.authorityShort}. Periksa koneksi internet lalu ketuk Muat Ulang."
                    _countdownText.value = "Offline • Ketuk Muat Ulang"
                }
            }
        }
    }

    // Refresh countdown based on official API prayer times
    private var lastPlayedAlarmTime: String? = null
    private var lastSyncedWidgetSignature: String? = null
    private var mediaPlayer: MediaPlayer? = null

    fun refreshCountdown() {
        syncCalendarDayIfNeeded()

        val times = prayerTimes.value
        if (times.zuhur.isEmpty() || times.subuh.isEmpty()) {
            if (_isPrayerTimesLoading.value) {
                _countdownText.value = "Memuat jadwal ${_currentLocation.value.country.authorityShort}..."
            }
            return
        }

        val now = Calendar.getInstance(currentTimeZone())
        val curHour = now.get(Calendar.HOUR_OF_DAY)
        val curMin = now.get(Calendar.MINUTE)
        val curSec = now.get(Calendar.SECOND)
        val currentSeconds = curHour * 3600 + curMin * 60 + curSec
        val currentFormattedTime = String.format(Locale.US, "%02d:%02d", curHour, curMin)

        val prayerNames = listOf("Imsak", "Subuh", "Syuruq", "Zuhur", "Asar", "Maghrib", "Isya")
        val prayerTimeStrings = listOf(times.imsak, times.subuh, times.syuruq, times.zuhur, times.asar, times.maghrib, times.isya)

        val matchingPrayerIndex = prayerTimeStrings.indexOf(currentFormattedTime)
        if (matchingPrayerIndex != -1) {
            val matchingName = prayerNames[matchingPrayerIndex]
            val cacheKey = "$currentFormattedTime-$matchingName"
            if (lastPlayedAlarmTime != cacheKey) {
                lastPlayedAlarmTime = cacheKey
                val setting = prayerSettings.value.find { it.prayerName.equals(matchingName, ignoreCase = true) }
                if (setting == null || setting.alertType == "ADZAN") {
                    playAdhan()
                }
            }
        }

        val status = PrayerTimesCalculator.evaluatePrayerStatus(times, currentSeconds)
        _currentPrayerName.value = status.currentPrayerName
        _nextPrayerName.value = status.nextPrayerName
        _nextPrayerTime.value = status.nextPrayerTime
        _countdownText.value = status.countdownText

        val loc = _currentLocation.value
        val widgetSignature = "${loc.widgetDisplayName()}|${loc.country.code}|${hijriDateString.value}|${status.widgetBadgeText}|${status.widgetHighlightedPrayer}|${times.subuh}|${times.zuhur}|${times.asar}|${times.maghrib}|${times.isya}"
        if (lastSyncedWidgetSignature != widgetSignature) {
            lastSyncedWidgetSignature = widgetSignature
            com.example.receiver.PrayerWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    private fun playAdhan() {
        try {
            mediaPlayer?.release()
            val resId = if (_azanSound.value == "Makkah") R.raw.azan_makkah else R.raw.azan_madinah

            val mp = MediaPlayer()
            try {
                val assetFileDescriptor = getApplication<Application>().resources.openRawResourceFd(resId)
                mp.setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                assetFileDescriptor.close()
                mp.prepare()

                mediaPlayer = mp.apply {
                    isLooping = false
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                    }
                    start()
                }
            } catch (fdException: Exception) {
                mp.release()
                val fallbackMp = MediaPlayer.create(getApplication(), resId)
                if (fallbackMp != null) {
                    mediaPlayer = fallbackMp.apply {
                        isLooping = false
                        setOnCompletionListener {
                            it.release()
                            mediaPlayer = null
                        }
                        start()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener)
        mediaPlayer?.release()
        mediaPlayer = null
        quranMediaPlayer?.release()
        quranMediaPlayer = null
    }

    /**
     * Toggles automatic real-time GPS location synchronization for both the app and Home Screen widget.
     */
    fun setAutoGpsEnabled(enabled: Boolean) {
        _useAutoGps.value = enabled
        WidgetLocationSyncHelper.setAutoGpsEnabled(getApplication(), enabled)
        if (enabled) {
            requestDeviceLocation(forceUserRequest = true)
        } else {
            com.example.receiver.PrayerWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    /**
     * Requests GPS only if auto-GPS is enabled and hasn't run on startup yet (or when user explicitly requests GPS).
     * Automatically detects whether user is in Malaysia (resolving official JAKIM zone via API) or Indonesia!
     */
    @SuppressLint("MissingPermission")
    fun requestDeviceLocation(forceUserRequest: Boolean = false) {
        if (!forceUserRequest) {
            if (!_useAutoGps.value || hasRunStartupGps) return
            hasRunStartupGps = true
        } else {
            _useAutoGps.value = true
            WidgetLocationSyncHelper.setAutoGpsEnabled(getApplication(), true)
        }

        // Keep Home Screen widget's FusedLocationProviderClient listener active when auto-GPS is on
        WidgetLocationSyncHelper.registerWidgetLocationUpdates(getApplication())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deviceLocation = WidgetLocationSyncHelper.fetchCurrentDeviceLocation(
                    context = getApplication(),
                    forceFreshFix = forceUserRequest
                )
                if (deviceLocation != null) {
                    resolveAndApplyGpsCoordinates(deviceLocation.latitude, deviceLocation.longitude)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun resolveAndApplyGpsCoordinates(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPreset = WidgetLocationSyncHelper.resolveLocationFromCoordinates(lat, lng)
            PrayerLocationCatalog.saveLocationPreset(getApplication(), finalPreset, isAutoGps = true)

            withContext(Dispatchers.Main) {
                _useAutoGps.value = true
                _currentLocation.value = finalPreset
                lastCalendarDaySignature = ""
                syncCalendarDayIfNeeded()
                refreshCountdown()
                com.example.receiver.PrayerWidgetProvider.updateAllWidgets(getApplication())
            }
        }
    }

    /**
     * User manually selected a city/zone from Indonesia or Malaysia.
     * Disables auto-GPS overwrite & unregisters widget background GPS updates so the chosen city/zone stays locked.
     */
    fun selectLocationPreset(preset: LocationPreset) {
        _useAutoGps.value = false
        WidgetLocationSyncHelper.unregisterWidgetLocationUpdates(getApplication())
        PrayerLocationCatalog.saveLocationPreset(getApplication(), preset, isAutoGps = false)
        _currentLocation.value = preset
        lastCalendarDaySignature = "" // Force timezone-accurate calendar refresh
        syncCalendarDayIfNeeded()
        refreshCountdown()
        com.example.receiver.PrayerWidgetProvider.updateAllWidgets(getApplication())
    }

    fun updateAzanSound(sound: String) {
        _azanSound.value = sound
        prefs.edit()
            .putString("azan_sound", sound)
            .apply()
    }

    // Prayer Setting Alert toggles
    fun togglePrayerSetting(prayerName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = prayerSettings.value.find { it.prayerName.equals(prayerName, ignoreCase = true) }
            if (current != null) {
                val updatedType = when (current.alertType) {
                    "ADZAN" -> "BEEP"
                    "BEEP" -> "SILENT"
                    else -> "ADZAN"
                }
                prayerSettingRepo.saveSetting(
                    current.copy(
                        isNotificationEnabled = updatedType != "SILENT",
                        alertType = updatedType
                    )
                )
            } else {
                prayerSettingRepo.saveSetting(PrayerSetting(prayerName, true, "ADZAN"))
            }
        }
    }

    // Tasbih Actions
    fun incrementTasbih() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = tasbihState.value
            tasbihRepo.saveTasbih(current.copy(currentCount = current.currentCount + 1))
        }
    }

    fun decrementTasbih() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = tasbihState.value
            if (current.currentCount > 0) {
                tasbihRepo.saveTasbih(current.copy(currentCount = current.currentCount - 1))
            }
        }
    }

    fun resetTasbih() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = tasbihState.value
            tasbihRepo.saveTasbih(current.copy(currentCount = 0))
        }
    }

    fun updateDhikrName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = tasbihState.value
            tasbihRepo.saveTasbih(current.copy(dhikrName = name))
        }
    }

    fun updateTasbihTarget(target: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = tasbihState.value
            tasbihRepo.saveTasbih(current.copy(targetCount = target))
        }
    }

    // Bookmark actions
    fun saveQuranBookmark(surahId: Int, surahName: String, ayatNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepo.saveBookmark(surahId, surahName, ayatNumber)
        }
    }

    fun deleteQuranBookmark() {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepo.deleteBookmark()
        }
    }

    // Qibla direction math: Bearing to Kaaba
    fun getQiblaBearing(lat: Double = _currentLocation.value.lat, lng: Double = _currentLocation.value.lng): Double {
        val qiblaLatitude = Math.toRadians(21.4225)
        val qiblaLongitude = Math.toRadians(39.8262)
        val userLatitude = Math.toRadians(lat)
        val userLongitude = Math.toRadians(lng)

        val deltaLon = qiblaLongitude - userLongitude
        val y = sin(deltaLon)
        val x = cos(userLatitude) * tan(qiblaLatitude) - sin(userLatitude) * cos(deltaLon)
        val qiblaBearing = Math.toDegrees(atan2(y, x))
        return (qiblaBearing + 360.0) % 360.0
    }

    // --- Daily Hadith & Category Browser State ---
    private val _dailyHadithOffset = MutableStateFlow(0)

    val dailyHadith: StateFlow<HadithItem> = combine(_currentCalendar, _dailyHadithOffset) { cal, offset ->
        val base = HadithDataset.getDailyHadith(cal)
        if (offset == 0) {
            base
        } else {
            val all = HadithDataset.allHadiths
            val baseIndex = all.indexOfFirst { it.id == base.id }.coerceAtLeast(0)
            val newIndex = (baseIndex + offset).mod(all.size)
            all[newIndex]
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HadithDataset.getDailyHadith()
    )

    private val _selectedHadithCategory = MutableStateFlow(HadithDataset.CATEGORY_ALL)
    val selectedHadithCategory: StateFlow<String> = _selectedHadithCategory.asStateFlow()

    private val _hadithSearchQuery = MutableStateFlow("")
    val hadithSearchQuery: StateFlow<String> = _hadithSearchQuery.asStateFlow()

    private val _favoriteHadithIds = MutableStateFlow<Set<Int>>(
        prefs.getStringSet("favorite_hadith_ids", emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    )
    val favoriteHadithIds: StateFlow<Set<Int>> = _favoriteHadithIds.asStateFlow()

    val filteredHadiths: StateFlow<List<HadithItem>> = combine(
        _selectedHadithCategory,
        _hadithSearchQuery,
        _favoriteHadithIds
    ) { category, query, favIds ->
        val byCategory = HadithDataset.getHadithsByCategory(category, favIds)
        if (query.isBlank()) {
            byCategory
        } else {
            val q = query.trim().lowercase()
            byCategory.filter { hadith ->
                hadith.title.lowercase().contains(q) ||
                    hadith.translation.lowercase().contains(q) ||
                    hadith.narrator.lowercase().contains(q) ||
                    hadith.category.lowercase().contains(q) ||
                    hadith.hikmah.lowercase().contains(q)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HadithDataset.allHadiths
    )

    fun selectHadithCategory(category: String) {
        _selectedHadithCategory.value = category
    }

    fun updateHadithSearchQuery(query: String) {
        _hadithSearchQuery.value = query
    }

    fun cycleDailyHadith() {
        _dailyHadithOffset.value += 1
    }

    fun resetDailyHadithToToday() {
        _dailyHadithOffset.value = 0
    }

    fun toggleFavoriteHadith(hadithId: Int) {
        val current = _favoriteHadithIds.value.toMutableSet()
        if (current.contains(hadithId)) {
            current.remove(hadithId)
        } else {
            current.add(hadithId)
        }
        _favoriteHadithIds.value = current
        prefs.edit()
            .putStringSet("favorite_hadith_ids", current.map { it.toString() }.toSet())
            .apply()
    }
}
