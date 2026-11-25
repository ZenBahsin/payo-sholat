package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PrayerCountry
import com.example.data.PrayerSetting
import com.example.data.firebase.PrayerLogDoc
import com.example.data.firebase.WorshipDateUtils
import com.example.ui.MainViewModel
import com.example.ui.components.LocationPickerDialog
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

private data class WeeklyDayItem(
    val dateKey: String,
    val dayShort: String,
    val dayNumber: String,
    val fullReadableDate: String,
    val isToday: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PrayerTimesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    showBackButton: Boolean = false,
    bottomContentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val currentCalendar by viewModel.currentCalendar.collectAsState()
    val hijriDate by viewModel.hijriDateString.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val currentPrayerName by viewModel.currentPrayerName.collectAsState()
    val nextPrayerTime by viewModel.nextPrayerTime.collectAsState()
    val countdownText by viewModel.countdownText.collectAsState()
    val bannerCountdownText by viewModel.bannerCountdownText.collectAsState()
    val prayerSettings by viewModel.prayerSettings.collectAsState()
    val isLoading by viewModel.isPrayerTimesLoading.collectAsState()
    val loadError by viewModel.prayerTimesError.collectAsState()
    val useAutoGps by viewModel.useAutoGps.collectAsState()

    val todayDateKey by viewModel.todayDateKey.collectAsState()
    val selectedDateKey by viewModel.selectedScheduleDateKey.collectAsState()
    val selectedDateLogs by viewModel.selectedDatePrayerLogs.collectAsState()
    val selectedDateCompletedCount by viewModel.selectedDateCompletedCount.collectAsState()

    var showCityPresetsDialog by remember { mutableStateOf(false) }
    var showSourceInfoDialog by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.requestDeviceLocation(forceUserRequest = false)
        }
    }

    // Build 7-day interactive weekly date strip (5 past days + today + 1 upcoming day)
    val weeklyDays = remember(currentCalendar, currentLocation.timeZoneId, todayDateKey) {
        val tz = TimeZone.getTimeZone(currentLocation.timeZoneId.ifBlank { "Asia/Jakarta" })
        val idLocale = Locale("id", "ID")
        val dayShortFmt = SimpleDateFormat("EEE", idLocale).apply { timeZone = tz }
        val dayNumFmt = SimpleDateFormat("dd", Locale.US).apply { timeZone = tz }
        val fullFmt = SimpleDateFormat("d MMMM yyyy", idLocale).apply { timeZone = tz }

        (-4..2).map { offset ->
            val cal = (currentCalendar.clone() as Calendar).apply {
                timeZone = tz
                add(Calendar.DAY_OF_MONTH, offset)
            }
            val key = WorshipDateUtils.formatDateKey(cal, tz)
            val rawShort = dayShortFmt.format(cal.time).replace(".", "")
            val cleanShort = rawShort.take(3).replaceFirstChar { it.uppercase(idLocale) }
            WeeklyDayItem(
                dateKey = key,
                dayShort = cleanShort,
                dayNumber = dayNumFmt.format(cal.time),
                fullReadableDate = fullFmt.format(cal.time),
                isToday = key == todayDateKey
            )
        }
    }

    val selectedDayItem = weeklyDays.find { it.dateKey == selectedDateKey }
    val isViewingToday = selectedDateKey == todayDateKey

    Scaffold(
        containerColor = WarmCream,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Jadwal & Riwayat",
                            style = MaterialTheme.typography.headlineMedium,
                            color = DeepForestCharcoal
                        )
                        Text(
                            text = "${currentLocation.widgetDisplayName()} • ${currentLocation.country.authorityShort}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedOliveGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = DeepForestCharcoal
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCityPresetsDialog = true },
                        modifier = Modifier.testTag("schedule_location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pilih Lokasi",
                            tint = DeepSage
                        )
                    }
                    IconButton(onClick = { showSourceInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Sumber Jadwal Resmi",
                            tint = MutedOliveGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmCream
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(WarmCream),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = bottomContentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PEMILIH TANGGAL MINGGUAN (7 HARI) — Warm Sand Gold active pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weeklyDays.forEach { day ->
                        val isSelected = day.dateKey == selectedDateKey
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) WarmSandGold else Color.Transparent,
                            animationSpec = tween(220),
                            label = "day_pill_${day.dateKey}"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillBg)
                                .clickable { viewModel.selectScheduleDate(day.dateKey) }
                                .padding(vertical = 10.dp)
                                .testTag("week_day_${day.dateKey}")
                        ) {
                            Text(
                                text = day.dayShort,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PureWhite.copy(alpha = 0.92f) else MutedOliveGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = day.dayNumber,
                                style = TabularNumberTextStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = if (isSelected) PureWhite else DeepForestCharcoal
                            )
                            if (day.isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PureWhite else WarmSandGold)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }

            // 2. BANNER COUNTDOWN RINGKAS (Deep Sage + Serene Landscape Artwork on Right)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSage),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(DeepSageDark, DeepSage, Color(0xFF5A775F))
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isViewingToday) {
                                    "Maju ke $nextPrayerName dalam"
                                } else {
                                    "Riwayat Ibadah"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isViewingToday) {
                                    bannerCountdownText
                                } else {
                                    selectedDayItem?.fullReadableDate ?: selectedDateKey
                                },
                                style = TabularNumberTextStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isViewingToday) 26.sp else 18.sp
                                ),
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Softly blended golden-sunrise mountain lake illustration on right
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .padding(6.dp)
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_prayer_landscape_1795596473260),
                                contentDescription = "Lanskap Waktu Shalat",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                DeepSage.copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Loading / Offline Error Banner
            if (isLoading || loadError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (loadError != null) MaterialTheme.colorScheme.errorContainer else SoftSageLight,
                        border = BorderStroke(1.dp, SageBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = DeepSage
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = loadError ?: "Mengambil jadwal resmi ${currentLocation.country.authorityShort}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepForestCharcoal
                                )
                            }
                            if (loadError != null) {
                                TextButton(onClick = { viewModel.retryFetchPrayerTimes() }) {
                                    Text("Muat Ulang", color = DeepSage, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. DAFTAR JADWAL & TRACKER SHALAT (Imsak .. Isya)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, SageBorder),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val rows = listOf(
                            ScheduleRowSpec("Imsak", "imsak", prayerTimes.imsak.ifEmpty { "--:--" }, Icons.Default.Bedtime, false),
                            ScheduleRowSpec("Subuh", "subuh", prayerTimes.subuh.ifEmpty { "--:--" }, Icons.Default.WbTwilight, true),
                            ScheduleRowSpec("Syuruq", "syuruq", prayerTimes.syuruq.ifEmpty { "--:--" }, Icons.Default.WbSunny, false),
                            ScheduleRowSpec("Zuhur", "zuhur", prayerTimes.zuhur.ifEmpty { "--:--" }, Icons.Default.LightMode, true),
                            ScheduleRowSpec("Asar", "asar", prayerTimes.asar.ifEmpty { "--:--" }, Icons.Default.WbCloudy, true),
                            ScheduleRowSpec("Maghrib", "maghrib", prayerTimes.maghrib.ifEmpty { "--:--" }, Icons.Default.NightsStay, true),
                            ScheduleRowSpec("Isya", "isya", prayerTimes.isya.ifEmpty { "--:--" }, Icons.Default.DarkMode, true)
                        )

                        rows.forEach { row ->
                            val setting = prayerSettings.find { it.prayerName.equals(row.label, ignoreCase = true) }
                                ?: PrayerSetting(row.label, true, "ADZAN")
                            val log: PrayerLogDoc? = selectedDateLogs[row.key]
                            val isActive = isViewingToday && currentPrayerName.equals(row.label, ignoreCase = true)
                            val isNext = isViewingToday && nextPrayerName.equals(row.label, ignoreCase = true)

                            OrganicPrayerTrackerRow(
                                spec = row,
                                setting = setting,
                                log = log,
                                isActive = isActive,
                                isNext = isNext,
                                onToggleCheck = {
                                    viewModel.togglePrayerCompletion(selectedDateKey, row.key)
                                },
                                onToggleMode = {
                                    viewModel.togglePrayerMode(selectedDateKey, row.key)
                                },
                                onToggleAlert = {
                                    viewModel.togglePrayerSetting(row.label)
                                    val feedback = when (setting.alertType) {
                                        "ADZAN" -> "Diubah ke Pengingat Bip"
                                        "BEEP" -> "Diubah ke Senyap"
                                        else -> "Diubah ke Alarm Adzan"
                                    }
                                    Toast.makeText(context, "${row.label}: $feedback", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // 4. KARTU RINGKASAN BAWAH ("Ibadah Hari Ini: X/5" + Animated Circular Progress Ring)
            item {
                WorshipSummaryRingCard(
                    title = if (isViewingToday) {
                        "Ibadah Hari Ini: $selectedDateCompletedCount/5"
                    } else {
                        "Ibadah ${selectedDayItem?.dayShort ?: ""}: $selectedDateCompletedCount/5"
                    },
                    subtitle = "Shalat Tercatat",
                    completedCount = selectedDateCompletedCount,
                    totalCount = 5
                )
            }

            // 5. Horizontal Samsung Home Screen Widget Preview (preserved cleanly below)
            item {
                HorizontalWidgetPreviewCard(
                    locationName = currentLocation.widgetDisplayName(),
                    hijriDate = hijriDate,
                    prayerTimes = prayerTimes,
                    nextPrayerName = nextPrayerName,
                    nextPrayerTime = nextPrayerTime,
                    currentPrayerName = currentPrayerName,
                    countdownText = countdownText,
                    authorityShort = currentLocation.country.authorityShort,
                    useAutoGps = useAutoGps,
                    onToggleAutoGps = { enabled ->
                        if (enabled) {
                            if (locationPermissionState.status.isGranted) {
                                viewModel.setAutoGpsEnabled(true)
                                Toast.makeText(context, "Lokasi otomatis GPS widget diaktifkan", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.setAutoGpsEnabled(true)
                                locationPermissionState.launchPermissionRequest()
                            }
                        } else {
                            viewModel.setAutoGpsEnabled(false)
                            Toast.makeText(context, "Widget menggunakan lokasi pilihan manual", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefreshWidget = {
                        if (useAutoGps && locationPermissionState.status.isGranted) {
                            viewModel.requestDeviceLocation(forceUserRequest = true)
                        } else {
                            viewModel.retryFetchPrayerTimes()
                            com.example.receiver.PrayerWidgetProvider.updateAllWidgets(context)
                        }
                    }
                )
            }
        }
    }

    if (showCityPresetsDialog) {
        LocationPickerDialog(
            currentLocationName = locationName,
            currentCountry = currentLocation.country,
            currentJakimZone = currentLocation.jakimZone,
            onSelectLocation = { preset ->
                viewModel.selectLocationPreset(preset)
                Toast.makeText(
                    context,
                    "Wilayah diatur ke ${preset.widgetDisplayName()} (${preset.country.authorityShort})",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onUseGps = {
                if (locationPermissionState.status.isGranted) {
                    viewModel.requestDeviceLocation(forceUserRequest = true)
                    Toast.makeText(context, "Mendeteksi lokasi GPS...", Toast.LENGTH_SHORT).show()
                } else {
                    locationPermissionState.launchPermissionRequest()
                }
            },
            onDismiss = { showCityPresetsDialog = false }
        )
    }

    if (showSourceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSourceInfoDialog = false },
            containerColor = PureWhite,
            title = {
                Text(
                    text = "Sumber Jadwal Resmi",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepForestCharcoal
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aplikasi Payo Shalat mengambil jadwal shalat langsung dari sumber resmi pemerintah tanpa perhitungan manual:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedOliveGray
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentLocation.country == PrayerCountry.INDONESIA) SoftSageLight else SoftIvory,
                        border = BorderStroke(1.dp, SageBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Indonesia — Kemenag RI",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepSage
                            )
                            Text(
                                text = "Data Bimas Islam Kemenag RI resmi (WIB / WITA / WIT).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedOliveGray
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentLocation.country == PrayerCountry.MALAYSIA) SoftSageLight else SoftIvory,
                        border = BorderStroke(1.dp, SageBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Malaysia — JAKIM e-Solat",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepSage
                            )
                            Text(
                                text = "Takwim solat rasmi JAKIM mengikut zon rasmi Malaysia (MYT / GMT+8).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedOliveGray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSourceInfoDialog = false
                    showCityPresetsDialog = true
                }) {
                    Text("Ganti Lokasi", color = DeepSage, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceInfoDialog = false }) {
                    Text("Tutup", color = MutedOliveGray)
                }
            }
        )
    }
}

private data class ScheduleRowSpec(
    val label: String,
    val key: String,
    val time: String,
    val icon: ImageVector,
    val isObligatory: Boolean
)

@Composable
private fun OrganicPrayerTrackerRow(
    spec: ScheduleRowSpec,
    setting: PrayerSetting,
    log: PrayerLogDoc?,
    isActive: Boolean,
    isNext: Boolean,
    onToggleCheck: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleAlert: () -> Unit
) {
    val isCompleted = log?.completed == true
    val isBerjamaah = log?.prayMode == "berjamaah_masjid"

    val containerColor = when {
        isActive -> SoftSageLight
        isNext -> WarmSandGoldSoft.copy(alpha = 0.45f)
        else -> Color.Transparent
    }

    val borderStroke = when {
        isActive -> BorderStroke(1.dp, DeepSage.copy(alpha = 0.35f))
        isNext -> BorderStroke(1.dp, WarmSandGold.copy(alpha = 0.35f))
        else -> null
    }

    val iconTint = when (spec.key) {
        "imsak" -> Color(0xFF7B6B9B)
        "subuh" -> WarmSandGold
        "syuruq" -> Color(0xFFD9953B)
        "zuhur" -> Color(0xFFC8A258)
        "asar" -> Color(0xFFCD8B45)
        "maghrib" -> Color(0xFFBD6E4C)
        "isya" -> DeepSage
        else -> DeepSage
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${spec.key}"),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (isActive) 13.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Name + Active Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) PureWhite.copy(alpha = 0.78f) else SoftIvory
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = spec.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isActive || isNext) FontWeight.Bold else FontWeight.SemiBold,
                        color = DeepForestCharcoal
                    )
                    if (isActive) {
                        Text(
                            text = "Aktif Sekarang",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepSage,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (spec.isObligatory && isCompleted) {
                        Text(
                            text = if (isBerjamaah) "Berjamaah di Masjid" else "Tercatat • Sendiri",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedOliveGray,
                            modifier = Modifier.clickable(onClick = onToggleMode)
                        )
                    }
                }
            }

            // Right: Time (or single-line "Berikutnya • HH:mm") + Optional Berjamaah Toggle + Check / Audio Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (isNext) {
                        Text(
                            text = "Berikutnya • ${spec.time}",
                            style = TabularNumberTextStyle.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = DeepForestCharcoal,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Text(
                            text = spec.time,
                            style = TabularNumberTextStyle.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = DeepForestCharcoal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (isActive && spec.isObligatory) {
                        Text(
                            text = if (isBerjamaah) "Berjamaah di Masjid" else "Tap: Berjamaah",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = DeepSage,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onToggleMode)
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                        )
                    }
                }

                if (spec.isObligatory) {
                    // Subtle notification toggle icon
                    IconButton(
                        onClick = onToggleAlert,
                        modifier = Modifier.size(32.dp)
                    ) {
                        val alertIcon = when (setting.alertType) {
                            "ADZAN" -> Icons.Default.VolumeUp
                            "BEEP" -> Icons.Default.NotificationsActive
                            else -> Icons.Default.VolumeOff
                        }
                        val alertTint = when (setting.alertType) {
                            "ADZAN" -> DeepSage.copy(alpha = 0.72f)
                            "BEEP" -> WarmSandGoldDark
                            else -> MutedOliveGray.copy(alpha = 0.42f)
                        }
                        Icon(
                            imageVector = alertIcon,
                            contentDescription = "Notifikasi ${spec.label}",
                            tint = alertTint,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Interactive Prayer Checklist Circle (48.dp touch area)
                    val checkBg by animateColorAsState(
                        targetValue = if (isCompleted) DeepSage else Color.Transparent,
                        animationSpec = tween(200),
                        label = "check_bg_${spec.key}"
                    )
                    val checkBorder by animateColorAsState(
                        targetValue = if (isCompleted) DeepSage else MutedOliveGray.copy(alpha = 0.45f),
                        animationSpec = tween(200),
                        label = "check_border_${spec.key}"
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleCheck)
                            .testTag("schedule_check_${spec.key}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(checkBg)
                                .border(1.6.dp, checkBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "${spec.label} selesai",
                                    tint = PureWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Non-obligatory row (Imsak / Syuruq): Audio/Notification Toggle
                    IconButton(
                        onClick = onToggleAlert,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("schedule_alert_${spec.key}")
                    ) {
                        val alertIcon = when (setting.alertType) {
                            "ADZAN" -> Icons.Default.VolumeUp
                            "BEEP" -> Icons.Default.NotificationsActive
                            else -> Icons.Default.VolumeOff
                        }
                        Icon(
                            imageVector = alertIcon,
                            contentDescription = "Notifikasi ${spec.label}",
                            tint = if (setting.alertType == "SILENT") MutedOliveGray.copy(alpha = 0.45f) else DeepSage,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorshipSummaryRingCard(
    title: String,
    subtitle: String,
    completedCount: Int,
    totalCount: Int
) {
    val targetFraction = (completedCount.toFloat() / totalCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 500),
        label = "worship_ring_fraction"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_summary_card"),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SageBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = TabularNumberTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = DeepForestCharcoal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedOliveGray
                )
            }

            // Concentric Organic Circular Progress Ring
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeOuter = 6.dp.toPx()
                    val strokeInner = 4.dp.toPx()
                    drawArc(
                        color = SoftSage,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                    )
                    if (animatedFraction > 0f) {
                        drawArc(
                            color = DeepSage,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                        )
                    }
                    // Subtle inner Warm Sand Gold accent ring
                    val inset = 9.dp.toPx()
                    drawArc(
                        color = WarmSandGoldSoft,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                    )
                    if (animatedFraction > 0f) {
                        drawArc(
                            color = WarmSandGold,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                            style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = "$completedCount",
                    style = TabularNumberTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = DeepForestCharcoal
                )
            }
        }
    }
}
