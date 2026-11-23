package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerCountry
import com.example.data.PrayerSetting
import com.example.ui.MainViewModel
import com.example.ui.components.LocationPickerDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PrayerTimesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val gregorianDate by viewModel.gregorianDateString.collectAsState()
    val hijriDate by viewModel.hijriDateString.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val currentPrayerName by viewModel.currentPrayerName.collectAsState()
    val nextPrayerTime by viewModel.nextPrayerTime.collectAsState()
    val countdownText by viewModel.countdownText.collectAsState()
    val prayerSettings by viewModel.prayerSettings.collectAsState()
    val isLoading by viewModel.isPrayerTimesLoading.collectAsState()
    val loadError by viewModel.prayerTimesError.collectAsState()

    var showCityPresetsDialog by remember { mutableStateOf(false) }
    var showSourceInfoDialog by remember { mutableStateOf(false) }

    // Setup permission state using Accompanist
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Only auto-detect GPS on startup if user hasn't picked a manual location
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.requestDeviceLocation(forceUserRequest = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Shalat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Manual selection city/zone (Indonesia & Malaysia)
                    IconButton(onClick = { showCityPresetsDialog = true }) {
                        Icon(Icons.Default.LocationCity, contentDescription = "Pilih Lokasi")
                    }
                    // Explicit GPS trigger
                    IconButton(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                viewModel.requestDeviceLocation(forceUserRequest = true)
                                Toast.makeText(context, "Mendeteksi lokasi GPS...", Toast.LENGTH_SHORT).show()
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Gunakan GPS")
                    }
                    // Official API Source info
                    IconButton(onClick = { showSourceInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Sumber Jadwal Resmi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero display for today and active status (matching design spec but refined)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable { showCityPresetsDialog = true }
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = currentLocation.widgetDisplayName(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Ganti Lokasi",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = gregorianDate,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = hijriDate,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // Main Countdown Callout
                                Text(
                                    text = if (nextPrayerName == "Isya" && countdownText.contains("Telah")) {
                                        "Sekarang: Isya • $nextPrayerTime"
                                    } else {
                                        "Selanjutnya: $nextPrayerName • $nextPrayerTime"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }

                            // Beautiful Animated Alarm Countdown Bubble
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AnimatedTimerIcon()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = countdownText,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Official API Source Badge Indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jadwal Shalat Hari Ini",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    SuggestionChip(
                        onClick = { showSourceInfoDialog = true },
                        label = {
                            Text(
                                text = "API: ${currentLocation.country.authorityShort}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Loading / Offline Error Banner (No manual calculation fallback!)
            if (isLoading || loadError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (loadError != null) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
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
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = loadError ?: "Mengambil jadwal resmi ${currentLocation.country.authorityShort}...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (loadError != null) {
                                TextButton(onClick = { viewModel.retryFetchPrayerTimes() }) {
                                    Text("Muat Ulang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // List of prayer timings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val items = listOf(
                            PrayerRowData("Imsak", prayerTimes.imsak.ifEmpty { "--:--" }, Icons.Default.Bedtime),
                            PrayerRowData("Subuh", prayerTimes.subuh.ifEmpty { "--:--" }, Icons.Default.WbTwilight),
                            PrayerRowData("Syuruq", prayerTimes.syuruq.ifEmpty { "--:--" }, Icons.Default.WbSunny),
                            PrayerRowData("Zuhur", prayerTimes.zuhur.ifEmpty { "--:--" }, Icons.Default.WbSunny),
                            PrayerRowData("Asar", prayerTimes.asar.ifEmpty { "--:--" }, Icons.Default.CloudQueue),
                            PrayerRowData("Maghrib", prayerTimes.maghrib.ifEmpty { "--:--" }, Icons.Default.WbTwilight),
                            PrayerRowData("Isya", prayerTimes.isya.ifEmpty { "--:--" }, Icons.Default.NightsStay)
                        )

                        items.forEach { prayer ->
                            val setting = prayerSettings.find { it.prayerName == prayer.name } ?: PrayerSetting(prayer.name, true, "ADZAN")
                            val isNext = nextPrayerName.equals(prayer.name, ignoreCase = true)
                            val isCurrent = currentPrayerName.equals(prayer.name, ignoreCase = true)

                            PrayerTimeRow(
                                name = prayer.name,
                                time = prayer.time,
                                icon = prayer.icon,
                                setting = setting,
                                isCurrent = isCurrent,
                                isNext = isNext,
                                countdownText = formatShortCountdown(countdownText),
                                onToggleAlarm = {
                                    viewModel.togglePrayerSetting(prayer.name)
                                    val feedback = when (setting.alertType) {
                                        "ADZAN" -> "Diubah ke Pengingat Bip"
                                        "BEEP" -> "Diubah ke Senyap/Mute"
                                        else -> "Diubah ke Alarm Adzan"
                                    }
                                    Toast.makeText(context, "${prayer.name}: $feedback", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Horizontal Samsung Home Screen Widget Preview & Quick Installer
            item {
                HorizontalWidgetPreviewCard(
                    locationName = currentLocation.widgetDisplayName(),
                    hijriDate = hijriDate,
                    prayerTimes = prayerTimes,
                    nextPrayerName = nextPrayerName,
                    nextPrayerTime = nextPrayerTime,
                    currentPrayerName = currentPrayerName,
                    countdownText = countdownText,
                    authorityShort = currentLocation.country.authorityShort
                )
            }
        }
    }

    // Unified Indonesia & Malaysia Location/Zone Picker Dialog
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

    // Official API Source Info Dialog (No Manual Calculation)
    if (showSourceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSourceInfoDialog = false },
            title = { Text("Sumber Jadwal Resmi", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aplikasi Payo Sholat! mengambil jadwal shalat langsung dari API resmi pemerintah tanpa perhitungan manual:",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentLocation.country == PrayerCountry.INDONESIA) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🇮🇩 Indonesia — Kemenag RI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Menggunakan data Bimas Islam Kemenag RI via API resmi Equran.id (WIB / WITA / WIT).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentLocation.country == PrayerCountry.MALAYSIA) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🇲🇾 Malaysia — JAKIM e-Solat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Menggunakan data takwim solat rasmi JAKIM (e-Solat & Waktu Solat API Malaysia) mengikut zon rasmi (MYT / GMT+8).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
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
                    Text("Ganti Lokasi / Negara", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceInfoDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

data class PrayerRowData(
    val name: String,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PrayerTimeRow(
    name: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    setting: PrayerSetting,
    isCurrent: Boolean,
    isNext: Boolean,
    countdownText: String?,
    onToggleAlarm: () -> Unit
) {
    // Choose beautiful background color & border for the active/current row
    val rowBgColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }
    
    val rowBorderModifier = if (isCurrent) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
    } else {
        Modifier
    }

    // Hand-crafted colors for each prayer's celestial state
    val prayerIconColor = when (name.lowercase()) {
        "imsak" -> Color(0xFF7E57C2)      // Deep spiritual violet/purple
        "subuh" -> Color(0xFF0288D1)      // Dawn sky blue
        "syuruq" -> Color(0xFFF57C00)     // Sunrise golden orange
        "zuhur" -> Color(0xFFFBC02D)      // Noon bright sun gold
        "asar" -> Color(0xFFE65100)       // Afternoon warm sunset orange
        "maghrib" -> Color(0xFFD81B60)    // Sunset dark crimson/pink
        "isya" -> Color(0xFF3F51B5)       // Starry night navy/indigo
        else -> MaterialTheme.colorScheme.onSurface
    }

    val textColor = MaterialTheme.colorScheme.onSurface

    // Interactive notification status colors
    val alertColor = when (setting.alertType) {
        "ADZAN" -> MaterialTheme.colorScheme.primary // Rich brand blue
        "BEEP" -> Color(0xFF00ACC1) // Vibrant modern cyan-teal for general alert
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) // Soft slate grey for silence
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(rowBorderModifier)
            .testTag("prayer_row_${name.lowercase()}"),
        color = rowBgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Part 1: Left (Icon and Name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(110.dp) // Fixed width to align all name labels & badges perfectly!
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = prayerIconColor,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = name,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    color = textColor
                )
            }

            // Part 2: Middle (Countdown or Status badge)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp), // Pushes the badge slightly to the right with comfortable breathing room!
                contentAlignment = Alignment.CenterStart
            ) {
                if (isCurrent) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Sekarang",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isNext && countdownText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "dalam $countdownText",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Part 3: Right (Time String & Alert Button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = time,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor
                )

                // Speaker alarm option toggle button
                IconButton(
                    onClick = onToggleAlarm, 
                    modifier = Modifier.size(36.dp)
                ) {
                    val alertIcon = when (setting.alertType) {
                        "ADZAN" -> Icons.Default.VolumeUp
                        "BEEP" -> Icons.Default.NotificationsActive
                        else -> Icons.Default.VolumeMute
                    }
                    Icon(
                        imageVector = alertIcon,
                        contentDescription = "Notifikasi",
                        tint = alertColor,
                        modifier = Modifier.size(22.dp) // Perfect consistent touch size and visual balance
                    )
                }
            }
        }
    }
}

private fun formatShortCountdown(fullText: String): String {
    if (fullText.contains("Telah", ignoreCase = true) || fullText.contains("Memasuki", ignoreCase = true)) {
        return "Tiba"
    }
    // Input format example: "5 jam 54 menit 39 detik lagi"
    val text = fullText.replace(" lagi", "").trim()
    
    val hourPattern = "(\\d+)\\s*jam".toRegex()
    val minutePattern = "(\\d+)\\s*menit".toRegex()
    val secondPattern = "(\\d+)\\s*detik".toRegex()
    
    val hourMatch = hourPattern.find(text)
    val minuteMatch = minutePattern.find(text)
    val secondMatch = secondPattern.find(text)
    
    val h = hourMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val m = minuteMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val s = secondMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    
    return when {
        h > 0 -> "${h}j ${m}m"
        m > 0 -> "${m}m"
        else -> "${s}d"
    }
}

@Composable
fun AnimatedTimerIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "timer_transition")
    
    // Smooth pulse/scale effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Continuous rotation of an outer ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(40.dp)
    ) {
        // Glowing background circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
                .background(Color.White.copy(alpha = 0.25f), shape = CircleShape)
        )
        
        // Outer rotating ring
        Canvas(
            modifier = Modifier
                .size(36.dp)
                .rotate(rotation)
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = size.minDimension / 2f,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f), 0f
                    )
                )
            )
        }
        
        // Timer/Alarm icon in center
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
