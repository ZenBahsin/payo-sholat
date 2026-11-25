package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.PrayerLogDoc
import com.example.data.firebase.QuranProgressDoc
import com.example.ui.MainViewModel
import com.example.ui.components.LocationPickerDialog
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToQuran: (initialSurahId: Int?) -> Unit,
    onNavigateToPrayers: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit = onNavigateToSettings,
    onNavigateToHadith: (category: String?) -> Unit = {},
    bottomContentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val hijriDate by viewModel.hijriDateString.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val currentPrayerName by viewModel.currentPrayerName.collectAsState()
    val nextPrayerTime by viewModel.nextPrayerTime.collectAsState()
    val shortCountdownPill by viewModel.shortCountdownPill.collectAsState()
    val solarArcProgress by viewModel.solarArcProgress.collectAsState()
    val qiblaDirectionLabel by viewModel.qiblaDirectionLabel.collectAsState()

    val userFirstName by viewModel.userFirstName.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()

    val quranProgress by viewModel.quranProgress.collectAsState()
    val bookmark by viewModel.bookmarkState.collectAsState()
    val tasbih by viewModel.tasbihState.collectAsState()

    val todayDateKey by viewModel.todayDateKey.collectAsState()
    val todayPrayerLogs by viewModel.todayPrayerLogs.collectAsState()
    val prayerStreakDays by viewModel.prayerStreakDays.collectAsState()

    var showLocationPickerDialog by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.requestDeviceLocation(forceUserRequest = false)
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream),
        contentPadding = PaddingValues(
            bottom = bottomContentPadding.calculateBottomPadding() + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP SOFT SAGE CURVED HEADER + DEEP SAGE HERO CARD
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Organic Soft Sage gradient curved backdrop behind header & upper hero card
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h * 0.82f)
                        quadraticTo(w * 0.5f, h * 1.04f, 0f, h * 0.82f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFC9D7C5), SoftSage)
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row: Greeting + Location Pill + Profile Avatar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (userFirstName.isNotBlank()) {
                                    "Assalamu'alaikum, $userFirstName"
                                } else {
                                    "Assalamu'alaikum"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepForestCharcoal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = hijriDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepForestCharcoal.copy(alpha = 0.72f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Location Pill
                            Surface(
                                onClick = { showLocationPickerDialog = true },
                                shape = CircleShape,
                                color = PureWhite.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.9f)),
                                modifier = Modifier
                                    .widthIn(max = 152.dp)
                                    .testTag("home_location_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pilih Lokasi",
                                        tint = DeepSage,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = currentLocation.widgetDisplayName(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestCharcoal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Clickable User Avatar -> Profile Tab
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WarmSandGoldSoft)
                                    .border(1.5.dp, PureWhite, CircleShape)
                                    .clickable(onClick = onNavigateToProfile)
                                    .testTag("home_profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!userAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userAvatarUrl,
                                        contentDescription = "Buka Profil $userDisplayName",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = userDisplayName.firstOrNull()?.uppercase() ?: "P",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepSage
                                    )
                                }
                            }
                        }
                    }

                    // 2. HERO CARD WAKTU SHALAT (Deep Sage + Real-Time Solar Arc Progress)
                    PrayerHeroSolarCard(
                        currentPrayerName = currentPrayerName.ifBlank { "Zuhur" },
                        nextPrayerName = nextPrayerName.ifBlank { "Asar" },
                        nextPrayerTime = nextPrayerTime,
                        countdownPill = shortCountdownPill,
                        solarProgress = solarArcProgress,
                        authorityShort = currentLocation.country.authorityShort,
                        qiblaLabel = qiblaDirectionLabel,
                        onCardClick = onNavigateToPrayers,
                        onQiblaClick = onNavigateToQibla
                    )
                }
            }
        }

        // 3. KARTU "LANJUTKAN MEMBACA" (Synced to user's quran_progress)
        item {
            ContinueReadingCard(
                quranProgress = quranProgress,
                fallbackSurahId = bookmark?.surahId,
                fallbackSurahName = bookmark?.surahName,
                fallbackAyat = bookmark?.ayatNumber,
                onClickContinue = { surahId -> onNavigateToQuran(surahId) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 4. BENTO GRID (2x2): Al-Qur'an, Kiblat AR, Tasbih Digital, Doa & Dzikir
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Al-Qur'an (Soft Sage)
                    BentoQuranCard(
                        onClick = { onNavigateToQuran(null) },
                        modifier = Modifier.weight(1f)
                    )
                    // Kiblat AR (Deep Sage Dark)
                    BentoQiblaCard(
                        qiblaLabel = qiblaDirectionLabel,
                        onClick = onNavigateToQibla,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tasbih Digital (Pure White + live DB count)
                    BentoSimpleCard(
                        title = "Tasbih Digital",
                        subtitle = "${tasbih.currentCount}/${tasbih.targetCount}",
                        icon = Icons.Default.Adjust,
                        iconTint = DeepSage,
                        isTabularSubtitle = true,
                        onClick = onNavigateToTasbih,
                        testTag = "menu_tasbih",
                        modifier = Modifier.weight(1f)
                    )
                    // Doa & Dzikir (Pure White)
                    BentoSimpleCard(
                        title = "Doa & Dzikir",
                        subtitle = "Kumpulan doa",
                        icon = Icons.Default.MenuBook,
                        iconTint = DeepSage,
                        isTabularSubtitle = false,
                        onClick = { onNavigateToHadith(null) },
                        testTag = "menu_hadith",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. WIDGET "STREAK SHALAT HARI INI" (Interactive 5-Prayer Checklist + Auto Streak Badge)
        item {
            TodayPrayerStreakCard(
                streakDays = prayerStreakDays,
                todayLogs = todayPrayerLogs,
                onTogglePrayer = { prayerKey ->
                    viewModel.togglePrayerCompletion(todayDateKey, prayerKey)
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    if (showLocationPickerDialog) {
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
            onDismiss = { showLocationPickerDialog = false }
        )
    }
}

@Composable
private fun PrayerHeroSolarCard(
    currentPrayerName: String,
    nextPrayerName: String,
    nextPrayerTime: String,
    countdownPill: String,
    solarProgress: Float,
    authorityShort: String,
    qiblaLabel: String,
    onCardClick: () -> Unit,
    onQiblaClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = solarProgress.coerceIn(0.05f, 0.96f),
        animationSpec = tween(durationMillis = 600),
        label = "solar_arc_progress"
    )

    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = DeepSageDark.copy(alpha = 0.28f),
                spotColor = DeepSageDark.copy(alpha = 0.28f)
            )
            .testTag("home_prayer_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF56735B), DeepSage, DeepSageDark)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: "Selanjutnya: Asar (15:15)" + countdown pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (countdownPill == "Waktu Isya") {
                            "Sekarang: Isya ($nextPrayerTime)"
                        } else {
                            "Selanjutnya: $nextPrayerName ($nextPrayerTime)"
                        },
                        style = TabularNumberTextStyle.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = PureWhite.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = countdownPill,
                            style = TabularNumberTextStyle.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = PureWhite,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                        )
                    }
                }

                // Solar Arc Progress Canvas
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        val padX = 24.dp.toPx()
                        val startX = padX
                        val endX = size.width - padX
                        val baseY = size.height - 8.dp.toPx()
                        val controlX = size.width * 0.5f
                        val controlY = -size.height * 0.25f

                        // Background full arc track
                        val fullArc = Path().apply {
                            moveTo(startX, baseY)
                            quadraticTo(controlX, controlY, endX, baseY)
                        }
                        drawPath(
                            path = fullArc,
                            color = Color.White.copy(alpha = 0.22f),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Active illuminated golden arc up to t = animatedProgress
                        val steps = 40
                        val activePath = Path()
                        var sunX = startX
                        var sunY = baseY
                        for (i in 0..steps) {
                            val t = (i.toFloat() / steps) * animatedProgress
                            val oneMinusT = 1f - t
                            val x = oneMinusT * oneMinusT * startX +
                                2f * oneMinusT * t * controlX +
                                t * t * endX
                            val y = oneMinusT * oneMinusT * baseY +
                                2f * oneMinusT * t * controlY +
                                t * t * baseY
                            if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
                            if (i == steps) {
                                sunX = x
                                sunY = y
                            }
                        }

                        drawPath(
                            path = activePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    WarmSandGold.copy(alpha = 0.78f),
                                    Color(0xFFF3D48B)
                                )
                            ),
                            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Glowing Gold Sun Indicator at progress point
                        val sunCenter = Offset(sunX, sunY)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFE6A3).copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                center = sunCenter,
                                radius = 18.dp.toPx()
                            ),
                            radius = 18.dp.toPx(),
                            center = sunCenter
                        )
                        drawCircle(
                            color = WarmSandGold,
                            radius = 7.dp.toPx(),
                            center = sunCenter
                        )
                        drawCircle(
                            color = Color(0xFFFFF6DC),
                            radius = 3.8.dp.toPx(),
                            center = sunCenter
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentPrayerName,
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite.copy(alpha = 0.88f)
                        )
                        Text(
                            text = nextPrayerName,
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite.copy(alpha = 0.88f)
                        )
                    }
                }

                // Footer Row: Verified Authority Badge ("Kemenag RI" / "JAKIM") + Qibla Direction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PureWhite.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = authorityShort,
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        onClick = onQiblaClick,
                        shape = CircleShape,
                        color = PureWhite.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = Color(0xFFF3D48B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Kiblat: $qiblaLabel",
                                style = TabularNumberTextStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    quranProgress: QuranProgressDoc?,
    fallbackSurahId: Int?,
    fallbackSurahName: String?,
    fallbackAyat: Int?,
    onClickContinue: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasHistory = (quranProgress != null && quranProgress.lastSurahNumber > 0 && quranProgress.lastSurahName.isNotBlank()) ||
        (fallbackSurahId != null && !fallbackSurahName.isNullOrBlank())

    val surahId = if (quranProgress != null && quranProgress.lastSurahNumber > 0) {
        quranProgress.lastSurahNumber
    } else {
        fallbackSurahId
    }
    val surahName = if (quranProgress != null && quranProgress.lastSurahName.isNotBlank()) {
        quranProgress.lastSurahName
    } else {
        fallbackSurahName ?: ""
    }
    val lastAyat = if (quranProgress != null && quranProgress.lastAyah > 0) {
        quranProgress.lastAyah
    } else {
        fallbackAyat ?: 1
    }
    val khatamPercent = quranProgress?.khatamPercentage ?: if (hasHistory) 1 else 0

    Surface(
        onClick = { onClickContinue(surahId) },
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_last_read_ribbon"),
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SageBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Styled Organic Sage Mushaf Bookmark Emblem
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepSage, DeepSageDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = WarmCream,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Lanjutkan Membaca",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestCharcoal
                )
                if (hasHistory) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$surahName: $lastAyat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedOliveGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$khatamPercent%",
                            style = TabularNumberTextStyle.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = DeepSage
                        )
                    }
                    // Slim Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(SoftSage)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((khatamPercent / 100f).coerceIn(0.04f, 1f))
                                .clip(CircleShape)
                                .background(DeepSage)
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada riwayat baca. Mulai tilawah hari ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedOliveGray,
                        maxLines = 2
                    )
                }
            }

            // Warm Sand Gold CTA Button ("Lanjut" / "Mulai")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    onClick = { onClickContinue(surahId) },
                    shape = CircleShape,
                    color = WarmSandGold,
                    modifier = Modifier.testTag("home_continue_reading_cta")
                ) {
                    Text(
                        text = if (hasHistory) "Lanjut" else "Mulai",
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = "Target Khatam",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MutedOliveGray
                )
            }
        }
    }
}

@Composable
private fun BentoQuranCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(98.dp)
            .testTag("menu_quran"),
        shape = RoundedCornerShape(20.dp),
        color = SoftSage,
        border = BorderStroke(1.dp, SageBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Al-Qur'an",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestCharcoal
                )
                Text(
                    text = "114 Surah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedOliveGray
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepSage),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Al-Qur'an",
                    tint = Color(0xFFF3D48B),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BentoQiblaCard(
    qiblaLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(98.dp)
            .testTag("menu_qibla"),
        shape = RoundedCornerShape(20.dp),
        color = DeepSageDark
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4F6B54), DeepSageDark)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Kiblat AR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = "Live camera compass",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = PureWhite.copy(alpha = 0.78f),
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }

                // Miniature Compass Dial Accent
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.14f))
                        .border(1.5.dp, Color(0xFFF3D48B).copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Kiblat $qiblaLabel",
                        tint = Color(0xFFF3D48B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoSimpleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isTabularSubtitle: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(78.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SageBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftSageLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = if (isTabularSubtitle) {
                        TabularNumberTextStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                    color = MutedOliveGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TodayPrayerStreakCard(
    streakDays: Int,
    todayLogs: Map<String, PrayerLogDoc>,
    onTogglePrayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prayers = listOf(
        "subuh" to "Subuh",
        "zuhur" to "Zuhur",
        "asar" to "Asar",
        "maghrib" to "Maghrib",
        "isya" to "Isya"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SageBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Streak Shalat Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestCharcoal
                )

                Surface(
                    shape = CircleShape,
                    color = WarmSandGoldSoft,
                    border = BorderStroke(1.dp, WarmSandGold.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$streakDays Hari Streak 🔥",
                        style = TabularNumberTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = WarmSandGoldDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                prayers.forEach { (key, label) ->
                    val isDone = todayLogs[key]?.completed == true
                    val circleBg by animateColorAsState(
                        targetValue = if (isDone) DeepSage else SoftIvory,
                        animationSpec = tween(220),
                        label = "streak_circle_$key"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isDone) DeepSage else SageBorderStrong,
                        animationSpec = tween(220),
                        label = "streak_border_$key"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTogglePrayer(key) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("home_streak_toggle_$key")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(circleBg)
                                .border(1.5.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "$label sudah dikerjakan",
                                    tint = PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SoftSage)
                                )
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDone) DeepForestCharcoal else MutedOliveGray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared helper composable used by HorizontalWidgetPreviewCard's Samsung setup guide dialog.
 */
@Composable
fun BulletItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = DeepSage,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepForestCharcoal
        )
    }
}
