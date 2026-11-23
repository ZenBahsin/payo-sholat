package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.LocationPickerDialog
import com.example.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToQuran: (initialSurahId: Int?) -> Unit,
    onNavigateToPrayers: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHadith: (category: String?) -> Unit = {}
) {
    val context = LocalContext.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val gregorianDate by viewModel.gregorianDateString.collectAsState()
    val hijriDate by viewModel.hijriDateString.collectAsState()
    val countdownText by viewModel.countdownText.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val currentPrayerName by viewModel.currentPrayerName.collectAsState()
    val nextPrayerTime by viewModel.nextPrayerTime.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val bookmark by viewModel.bookmarkState.collectAsState()
    val dailyHadith by viewModel.dailyHadith.collectAsState()

    var showMenuLainnyaDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var showLocationPickerDialog by remember { mutableStateOf(false) }

    // Setup permission state using Accompanist
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Activity launcher for notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // No toast to prevent annoying the user
    }

    // Run once on initial screen launch to request permissions safely without overwriting manual selection
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            viewModel.requestDeviceLocation(forceUserRequest = false)
        } else {
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

    // Inspirational quotes list
    val quotes = listOf(
        "Maka sesungguhnya beserta kesulitan ada kemudahan. (QS. Al-Insyirah: 5)",
        "Hanya dengan mengingat Allah hati menjadi tenteram. (QS. Ar-Ra'd: 28)",
        "Jadikanlah sabar dan shalat sebagai penolongmu. (QS. Al-Baqarah: 45)",
        "Barangsiapa bertakwa kepada Allah, niscaya Dia akan mengadakan baginya jalan keluar. (QS. At-Talaq: 2)"
    )
    val activeQuote = remember { quotes.random() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP HEADER SECTION (Assalamu'alaikum!)
                item(span = { GridItemSpan(4) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Assalamu'alaikum 👋,",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF5C6B73)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Payo Sholat!",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A2530)
                            )
                        }
                        
                        // Location Pill -> Opens Indonesia & Malaysia Location/Zone Picker (+ Auto GPS)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .widthIn(max = 175.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .clickable { showLocationPickerDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("home_location_pill")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pilih Lokasi",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentLocation.widgetDisplayName(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A2530),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF5C6B73),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // 2. TIMINGS MAIN SUMMARY CARD
                item(span = { GridItemSpan(4) }) {
                    val progressFraction = remember(countdownText) {
                        try {
                            val jamIndex = countdownText.indexOf("jam")
                            val hours = if (jamIndex != -1) {
                                countdownText.substring(0, jamIndex).trim().toFloatOrNull() ?: 2f
                            } else {
                                0.5f
                            }
                            ((6f - hours).coerceIn(0.5f, 6f) / 6f)
                        } catch (e: Exception) {
                            0.65f
                        }
                    }

                    Card(
                        onClick = onNavigateToPrayers,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_prayer_summary_card"),
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            TealGradientStart,
                                            TealGradientEnd
                                        )
                                    )
                                )
                        ) {
                            // Layer beautiful mosque silhouette background
                            IslamicPatternBackground(
                                modifier = Modifier.matchParentSize()
                            )
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (nextPrayerName == "Isya" && countdownText.contains("Telah")) "Sekarang: " else "Selanjutnya: ",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = nextPrayerName,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = currentLocation.country.authorityShort,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Text(
                                    text = nextPrayerTime,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 48.sp
                                )
                                
                                Spacer(modifier = Modifier.height(1.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = countdownText,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Progress bar track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFraction)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. TERAKHIR BACA (Last Read Ribbon)
                item(span = { GridItemSpan(4) }) {
                    Card(
                        onClick = {
                            if (bookmark != null) {
                                onNavigateToQuran(bookmark?.surahId)
                            } else {
                                onNavigateToQuran(null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_last_read_ribbon"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(HighlightBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Terakhir Baca",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = PrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (bookmark != null) "${bookmark?.surahName} • Ayat ${bookmark?.ayatNumber}" else "Belum ada riwayat membaca",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 4. MAIN FEATURES GRID - ColSpan 1 each (Total 4 in a row!)
                item {
                    SleekMenuButton(
                        title = "Al-Qur'an",
                        icon = Icons.Default.MenuBook,
                        onClick = { onNavigateToQuran(null) },
                        testTag = "menu_quran"
                    )
                }

                item {
                    SleekMenuButton(
                        title = "Shalat",
                        icon = Icons.Default.AccessTime,
                        onClick = onNavigateToPrayers,
                        testTag = "menu_prayer_times"
                    )
                }

                item {
                    SleekMenuButton(
                        title = "Kiblat",
                        icon = Icons.Default.Explore,
                        onClick = onNavigateToQibla,
                        testTag = "menu_qibla"
                    )
                }

                item {
                    SleekMenuButton(
                        title = "Tasbih",
                        icon = Icons.Default.FormatListBulleted,
                        onClick = onNavigateToTasbih,
                        testTag = "menu_tasbih"
                    )
                }

                // 5. SECONDARY ROW ACTIONS (Spanned across 2 columns each so they fit side by side elegantly)
                item(span = { GridItemSpan(2) }) {
                    SecondaryActionRow(
                        title = "Menu Lainnya",
                        icon = Icons.Default.GridView,
                        onClick = { showMenuLainnyaDialog = true }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    SecondaryActionRow(
                        title = "Pengaturan",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings
                    )
                }

                // 6. BOTTOM BANNER ("Hadits Hari Ini - Temani Ibadahmu")
                item(span = { GridItemSpan(4) }) {
                    Card(
                        onClick = { onNavigateToHadith(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .testTag("home_bottom_banner"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Custom vector/canvas arch scene illustration
                                IbadahIllustration()

                                // 2. Verified Daily Hadith Content
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = HighlightBlue,
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = "Hadits Hari Ini",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                color = PrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "Terverifikasi",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = dailyHadith.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "\"${dailyHadith.translation}\"",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                        lineHeight = 17.sp,
                                        maxLines = 3
                                    )

                                    Text(
                                        text = "${dailyHadith.narrator} • ${dailyHadith.category}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }

                                // 3. Chevron navigation indicator
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Lihat Hadits Selengkapnya",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )

                            // Quick Action Footer: Browse by Category or View More
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Ibadah & Shalat", "Akhlak & Adab", "Dzikir & Doa").forEach { cat ->
                                        Surface(
                                            onClick = { onNavigateToHadith(cat) },
                                            color = HighlightBlue.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = cat.substringBefore(" "),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onNavigateToHadith(null) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Lihat Semua",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryBlue
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(13.dp).padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Menu Lainnya Dialog with interactive features (Daily Hadith & Horizontal Widget for Samsung)
    if (showMenuLainnyaDialog) {
        AlertDialog(
            onDismissRequest = { showMenuLainnyaDialog = false },
            title = { Text("Menu & Fitur Pilihan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = {
                            showMenuLainnyaDialog = false
                            onNavigateToHadith(null)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = HighlightBlue.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Hadits Harian & Kategori",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    "Kumpulan hadits shahih harian & jelajahi per kategori",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            showMenuLainnyaDialog = false
                            showWidgetDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = HighlightBlue.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Widget Jadwal Sholat Horizontal",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    "Pasang widget horizontal (4x1) di layar utama HP Samsung & Android",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenuLainnyaDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetDialog = false },
            title = { Text("Widget Horizontal Layar Utama", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
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
            },
            confirmButton = {
                TextButton(onClick = { showWidgetDialog = false }) {
                    Text("Selesai")
                }
            }
        )
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
fun BulletItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MenuGridCard(
    title: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(colors = gradientColors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Circular White Icon background bubble
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SecondaryActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2638) else Color(0xFFF1F3F9)
    val contentColor = if (isDark) Color(0xFFE2E2E6) else Color(0xFF1A2530)
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun SleekMenuButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HighlightBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}@Composable
fun IslamicPatternBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Silhouette colors
        val silColor = Color.White.copy(alpha = 0.12f)
        val silColorBack = Color.White.copy(alpha = 0.05f)
        val moonColor = Color.White // Set moon color to white (translucent styles applied via alpha)
        val starColor = Color.White.copy(alpha = 0.35f)
        val starGlowColor = Color.White.copy(alpha = 0.10f)
        
        // ----------------- HELPER DRAWING FUNCTIONS -----------------
        fun Path.onionDome(startX: Float, endX: Float, baseY: Float, apexY: Float) {
            val w = endX - startX
            val midX = (startX + endX) / 2f
            val h = baseY - apexY
            
            // Left bulbous curve
            cubicTo(
                startX - w * 0.08f, baseY - h * 0.15f,
                midX - w * 0.12f, apexY + h * 0.12f,
                midX, apexY
            )
            // Right bulbous curve
            cubicTo(
                midX + w * 0.12f, apexY + h * 0.12f,
                endX + w * 0.08f, baseY - h * 0.15f,
                endX, baseY
            )
        }
        
        fun Path.minaret(centerX: Float, baseW: Float, baseY: Float, topY: Float) {
            val left = centerX - baseW / 2f
            val right = centerX + baseW / 2f
            val h = baseY - topY
            
            lineTo(left, baseY)
            lineTo(left, baseY - h * 0.6f) // Shaft
            
            // Balcony 1
            val b1Y1 = baseY - h * 0.6f
            val b1Y2 = baseY - h * 0.65f
            lineTo(left - baseW * 0.2f, b1Y1)
            lineTo(left - baseW * 0.2f, b1Y2)
            lineTo(left, b1Y2)
            
            lineTo(left * 0.9f + centerX * 0.1f, baseY - h * 0.85f) // Upper neck
            
            // Balcony 2
            val b2Y1 = baseY - h * 0.85f
            val b2Y2 = baseY - h * 0.90f
            lineTo(centerX - baseW * 0.3f, b2Y1)
            lineTo(centerX - baseW * 0.3f, b2Y2)
            lineTo(centerX, topY) // Spire tip
            
            lineTo(centerX + baseW * 0.3f, b2Y2)
            lineTo(centerX + baseW * 0.3f, b2Y1)
            lineTo(right * 0.9f + centerX * 0.1f, b2Y1)
            
            lineTo(right, b1Y2)
            lineTo(right + baseW * 0.2f, b1Y2)
            lineTo(right + baseW * 0.2f, b1Y1)
            lineTo(right, b1Y1)
            lineTo(right, baseY)
        }
        
        fun Path.draw4PointStar(cx: Float, cy: Float, r: Float) {
            moveTo(cx, cy - r)
            quadraticTo(cx, cy, cx + r, cy)
            quadraticTo(cx, cy, cx, cy + r)
            quadraticTo(cx, cy, cx - r, cy)
            quadraticTo(cx, cy, cx, cy - r)
        }
        
        // 1. DRAW STARS IN THE BACKGROUND
        // Multiple sparkling 4-point stars
        val stars = listOf(
            Triple(width * 0.12f, height * 0.18f, 5.dp.toPx()),
            Triple(width * 0.26f, height * 0.12f, 3.5f.dp.toPx()),
            Triple(width * 0.38f, height * 0.22f, 4.5f.dp.toPx()),
            Triple(width * 0.48f, height * 0.14f, 6.dp.toPx()),
            Triple(width * 0.68f, height * 0.25f, 4.5f.dp.toPx()),
            Triple(width * 0.82f, height * 0.15f, 5.dp.toPx()),
            Triple(width * 0.92f, height * 0.22f, 3.5f.dp.toPx())
        )
        
        for ((sx, sy, r) in stars) {
            // Glow
            val glowPath = Path().apply { draw4PointStar(sx, sy, r * 2.2f) }
            drawPath(path = glowPath, color = starGlowColor)
            
            // Core
            val starPath = Path().apply { draw4PointStar(sx, sy, r) }
            drawPath(path = starPath, color = starColor)
        }
        
        // Glowing moon in the center-right sky
        val moonX = width * 0.76f
        val moonY = height * 0.18f
        val moonR = 16.dp.toPx()
        
        // Glow of the moon
        drawCircle(
            color = moonColor.copy(alpha = 0.06f),
            radius = moonR * 1.8f,
            center = androidx.compose.ui.geometry.Offset(moonX, moonY)
        )
        drawCircle(
            color = moonColor.copy(alpha = 0.12f),
            radius = moonR * 1.3f,
            center = androidx.compose.ui.geometry.Offset(moonX, moonY)
        )
        
        // Golden crescent moon
        val crescentPath = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(moonX - moonR, moonY - moonR, moonX + moonR, moonY + moonR),
                startAngleDegrees = -115f,
                sweepAngleDegrees = 230f
            )
            val offset = 4.dp.toPx()
            val rInner = moonR * 0.82f
            addArc(
                oval = androidx.compose.ui.geometry.Rect(moonX - rInner + offset, moonY - rInner, moonX + rInner + offset, moonY + rInner),
                startAngleDegrees = 115f,
                sweepAngleDegrees = -230f
            )
            close()
        }
        drawPath(path = crescentPath, color = moonColor.copy(alpha = 0.85f))
        
        // 2. DISTANT BACKGROUND SILHOUETTE (LAYER 1)
        val bgPath = Path().apply {
            moveTo(0f, height)
            
            // Distant broad horizon hill
            val hillHeight = height * 0.82f
            cubicTo(0f, hillHeight, width * 0.5f, hillHeight - 15.dp.toPx(), width, hillHeight)
            lineTo(width, height)
            close()
        }
        drawPath(path = bgPath, color = silColorBack)
        
        // 3. DETAILED MOSQUE SILHOUETTE (LAYER 2) matching 2069853.svg
        val mosquePath = Path().apply {
            moveTo(0f, height)
            
            // Broad convex ground curve base
            val groundY = height * 0.90f
            cubicTo(
                width * 0.15f, groundY - 12.dp.toPx(),
                width * 0.85f, groundY - 12.dp.toPx(),
                width, height
            )
            
            // Tracing Left to Right
            // Far Left minaret
            val flmCenterX = width * 0.08f
            val flmBaseW = width * 0.04f
            minaret(flmCenterX, flmBaseW, height * 0.86f, height * 0.34f)
            
            // Wall section 1 with crenellations
            lineTo(width * 0.12f, height * 0.84f)
            lineTo(width * 0.12f, height * 0.80f)
            lineTo(width * 0.14f, height * 0.80f)
            lineTo(width * 0.14f, height * 0.84f)
            lineTo(width * 0.16f, height * 0.84f)
            lineTo(width * 0.16f, height * 0.80f)
            lineTo(width * 0.18f, height * 0.80f)
            lineTo(width * 0.18f, height * 0.84f)
            lineTo(width * 0.19f, height * 0.84f)
            
            // Left Small dome
            onionDome(width * 0.19f, width * 0.27f, height * 0.84f, height * 0.58f)
            
            // Left Medium dome
            onionDome(width * 0.28f, width * 0.41f, height * 0.84f, height * 0.42f)
            
            // Center-Left minaret pillar
            val clmCenterX = width * 0.43f
            minaret(clmCenterX, width * 0.025f, height * 0.84f, height * 0.38f)
            
            // Center connecting wall with battlements
            lineTo(width * 0.45f, height * 0.84f)
            lineTo(width * 0.45f, height * 0.78f)
            lineTo(width * 0.465f, height * 0.78f)
            lineTo(width * 0.465f, height * 0.84f)
            lineTo(width * 0.48f, height * 0.84f)
            
            // GRAND MAIN DOME (Centered nicely around width * 0.60)
            onionDome(width * 0.48f, width * 0.72f, height * 0.84f, height * 0.24f)
            
            // Center-Right connecting wall with battlements
            lineTo(width * 0.735f, height * 0.84f)
            lineTo(width * 0.735f, height * 0.78f)
            lineTo(width * 0.75f, height * 0.78f)
            lineTo(width * 0.75f, height * 0.84f)
            lineTo(width * 0.755f, height * 0.84f)
            
            // Center-Right minaret pillar
            val crmCenterX = width * 0.77f
            minaret(crmCenterX, width * 0.025f, height * 0.84f, height * 0.38f)
            
            // Right Medium dome
            onionDome(width * 0.79f, width * 0.92f, height * 0.84f, height * 0.42f)
            
            // Right Small dome
            onionDome(width * 0.93f, width * 1.01f, height * 0.84f, height * 0.58f)
            
            // Wall section right with crenellations
            lineTo(width * 1.02f, height * 0.84f)
            
            // Far Right minaret
            val frmCenterX = width * 1.05f
            minaret(frmCenterX, width * 0.04f, height * 0.86f, height * 0.34f)
            
            lineTo(width, height)
            close()
        }
        drawPath(path = mosquePath, color = silColor)
        
        // 4. EMBEDDED GOLDEN CRESCENT SPIRES FOR THE DOMES (To match high-quality design)
        fun drawSpireWithCrescent(domeMidX: Float, domeApexY: Float, heightPx: Float, sizeFactor: Float = 1f) {
            val sTipY = domeApexY - heightPx
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = androidx.compose.ui.geometry.Offset(domeMidX, domeApexY),
                end = androidx.compose.ui.geometry.Offset(domeMidX, sTipY),
                strokeWidth = 1.5.dp.toPx()
            )
            // Tiny gold crescent on top of the spire
            val cR = 3.dp.toPx() * sizeFactor
            val cX = domeMidX
            val cY = sTipY - cR
            val tinyCrescent = Path().apply {
                addArc(
                    oval = androidx.compose.ui.geometry.Rect(cX - cR, cY - cR, cX + cR, cY + cR),
                    startAngleDegrees = -110f,
                    sweepAngleDegrees = 220f
                )
                val rInner = cR * 0.8f
                val offset = 1.dp.toPx()
                addArc(
                    oval = androidx.compose.ui.geometry.Rect(cX - rInner + offset, cY - rInner, cX + rInner + offset, cY + rInner),
                    startAngleDegrees = 110f,
                    sweepAngleDegrees = -220f
                )
                close()
            }
            drawPath(path = tinyCrescent, color = moonColor.copy(alpha = 0.7f))
        }
        
        // Draw spires for Main Dome, Left Medium Dome, Right Medium Dome
        drawSpireWithCrescent(width * 0.60f, height * 0.24f, 18.dp.toPx(), 1.5f)
        drawSpireWithCrescent(width * 0.345f, height * 0.42f, 12.dp.toPx(), 1.0f)
        drawSpireWithCrescent(width * 0.855f, height * 0.42f, 12.dp.toPx(), 1.0f)
        
        // 5. INNER SHINING ARCHES (DOORS/WINDOWS)
        val innerArches = Path().apply {
            // Main dome center door
            val mDx = width * 0.60f
            val mDw = width * 0.05f
            val mDh = height * 0.12f
            val mBaseY = height * 0.84f
            val mL = mDx - mDw/2f
            val mR = mDx + mDw/2f
            moveTo(mL, mBaseY)
            lineTo(mL, mBaseY - mDh * 0.6f)
            cubicTo(mL, mBaseY - mDh, mR, mBaseY - mDh, mR, mBaseY - mDh * 0.6f)
            lineTo(mR, mBaseY)
            
            // Left dome door
            val lDx = width * 0.345f
            val lDw = width * 0.035f
            val lDh = height * 0.08f
            val lL = lDx - lDw/2f
            val lR = lDx + lDw/2f
            moveTo(lL, mBaseY)
            lineTo(lL, mBaseY - lDh * 0.6f)
            cubicTo(lL, mBaseY - lDh, lR, mBaseY - lDh, lR, mBaseY - lDh * 0.6f)
            lineTo(lR, mBaseY)
            
            // Right dome door
            val rDx = width * 0.855f
            val rDw = width * 0.035f
            val rDh = height * 0.08f
            val rL = rDx - rDw/2f
            val rR = rDx + rDw/2f
            moveTo(rL, mBaseY)
            lineTo(rL, mBaseY - rDh * 0.6f)
            cubicTo(rL, mBaseY - rDh, rR, mBaseY - rDh, rR, mBaseY - rDh * 0.6f)
            lineTo(rR, mBaseY)
        }
        drawPath(path = innerArches, color = Color.White.copy(alpha = 0.05f))
    }
}

@Composable
fun IbadahIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(60.dp)
            .height(84.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Dimensions of the arch
            val archWidth = width * 0.72f
            val archHeight = height * 0.94f
            val archLeft = width * 0.05f
            val archRight = archLeft + archWidth
            val archTop = height * 0.04f
            val archBottom = height
            val r = archWidth / 2f // radius for semicircle top

            // 1. Tall rounded arch path
            val archPath = Path().apply {
                moveTo(archLeft, archBottom)
                lineTo(archLeft, archTop + r)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(archLeft, archTop, archRight, archTop + archWidth),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(archRight, archBottom)
                close()
            }

            // 2. Draw background clouds behind or overlapping the arch
            // We draw them with custom transparent white/light-blue shapes for organic look
            fun drawCloudShape(cx: Float, cy: Float, baseW: Float, baseH: Float, opacity: Float) {
                val cloudPath = Path().apply {
                    val left = cx - baseW / 2f
                    val right = cx + baseW / 2f
                    val bottom = cy + baseH / 2f
                    
                    moveTo(left, bottom)
                    lineTo(right, bottom)
                    
                    // Right small bump
                    cubicTo(
                        right + baseW * 0.05f, bottom - baseH * 0.3f,
                        right - baseW * 0.1f, bottom - baseH * 0.7f,
                        right - baseW * 0.2f, bottom - baseH * 0.5f
                    )
                    // Center big bump
                    cubicTo(
                        right - baseW * 0.3f, bottom - baseH * 1.1f,
                        left + baseW * 0.3f, bottom - baseH * 1.1f,
                        left + baseW * 0.2f, bottom - baseH * 0.4f
                    )
                    // Left small bump
                    cubicTo(
                        left + baseW * 0.1f, bottom - baseH * 0.4f,
                        left - baseW * 0.05f, bottom - baseH * 0.3f,
                        left, bottom
                    )
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = opacity), Color(0xFFE4EDFC).copy(alpha = opacity))
                    )
                )
            }

            // 3. Render contents clipped inside the arch
            clipPath(path = archPath) {
                // Background Sky Gradient
                val skyGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF82A6F1), Color(0xFFD2E1FD), Color(0xFFA3C2F7))
                )
                drawRect(brush = skyGradient, size = size)

                // White crescent moon inside the arch
                val moonX = archLeft + archWidth * 0.46f
                val moonY = archTop + archWidth * 0.48f
                val moonR = archWidth * 0.18f
                val moonPath = Path().apply {
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(moonX - moonR, moonY - moonR, moonX + moonR, moonY + moonR),
                        startAngleDegrees = -120f,
                        sweepAngleDegrees = 240f
                    )
                    val rInner = moonR * 0.85f
                    val offset = moonR * 0.25f
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(moonX - rInner + offset, moonY - rInner, moonX + rInner + offset, moonY + rInner),
                        startAngleDegrees = 110f,
                        sweepAngleDegrees = -220f
                    )
                    close()
                }
                rotate(degrees = -18f, pivot = androidx.compose.ui.geometry.Offset(moonX, moonY)) {
                    drawPath(path = moonPath, color = Color.White)
                }

                // Beautiful layered wavy hills at the bottom
                // Background wave (Light blue)
                val bgWavePath = Path().apply {
                    moveTo(archLeft, archBottom)
                    lineTo(archLeft, height * 0.70f)
                    cubicTo(
                        archLeft + archWidth * 0.35f, height * 0.64f,
                        archLeft + archWidth * 0.70f, height * 0.73f,
                        archRight, height * 0.64f
                    )
                    lineTo(archRight, archBottom)
                    close()
                }
                drawPath(
                    path = bgWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF9EC0F9), Color(0xFF759DF0))
                    )
                )

                // Middle wave (Medium blue)
                val midWavePath = Path().apply {
                    moveTo(archLeft, archBottom)
                    lineTo(archLeft, height * 0.80f)
                    cubicTo(
                        archLeft + archWidth * 0.40f, height * 0.82f,
                        archLeft + archWidth * 0.70f, height * 0.70f,
                        archRight, height * 0.76f
                    )
                    lineTo(archRight, archBottom)
                    close()
                }
                drawPath(
                    path = midWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF5A91F5), Color(0xFF2B5ECD))
                    )
                )

                // Foreground wave (Deep blue)
                val fgWavePath = Path().apply {
                    moveTo(archLeft, archBottom)
                    lineTo(archLeft, height * 0.85f)
                    cubicTo(
                        archLeft + archWidth * 0.35f, height * 0.89f,
                        archLeft + archWidth * 0.75f, height * 0.80f,
                        archRight, height * 0.84f
                    )
                    lineTo(archRight, archBottom)
                    close()
                }
                drawPath(
                    path = fgWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1357DF), Color(0xFF0433A0))
                    )
                )
            }

            // 4. Draw overlapping clouds
            // Left Cloud (sits on the left edge of the arch)
            drawCloudShape(
                cx = archLeft + archWidth * 0.12f,
                cy = height * 0.56f,
                baseW = archWidth * 0.45f,
                baseH = height * 0.12f,
                opacity = 0.85f
            )

            // Right Cloud (extends outside the right edge of the arch)
            drawCloudShape(
                cx = archRight + width * 0.08f,
                cy = height * 0.46f,
                baseW = archWidth * 0.48f,
                baseH = height * 0.13f,
                opacity = 0.90f
            )

            // 5. Draw elegant decorative leafy branch growing up overlapping bottom right
            val stemPath = Path().apply {
                moveTo(archRight, height)
                quadraticTo(
                    archRight + width * 0.06f, height * 0.86f,
                    archRight + width * 0.15f, height * 0.66f
                )
            }
            drawPath(
                path = stemPath,
                color = Color(0xFF759DF0),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Function to draw an elegant leaf with color gradient
            fun drawGradientLeaf(cx: Float, cy: Float, rx: Float, ry: Float, angle: Float) {
                val leafPath = Path().apply {
                    moveTo(cx, cy - ry)
                    quadraticTo(cx + rx, cy - ry, cx + rx, cy)
                    quadraticTo(cx + rx, cy + ry, cx, cy + ry)
                    quadraticTo(cx - rx, cy + ry, cx - rx, cy)
                    quadraticTo(cx - rx, cy - ry, cx, cy - ry)
                }
                val leafGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8BB1F7), Color(0xFF225DC4)),
                    startY = cy - ry,
                    endY = cy + ry
                )
                rotate(degrees = angle, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
                    drawPath(path = leafPath, brush = leafGradient)
                }
            }

            // Draw our beautifully-positioned botanical leaves
            drawGradientLeaf(archRight + width * 0.15f, height * 0.66f, 3.2f.dp.toPx(), 7.dp.toPx(), 35f) // Top tip
            drawGradientLeaf(archRight + width * 0.18f, height * 0.76f, 3.0f.dp.toPx(), 6.5f.dp.toPx(), 70f) // Mid right
            drawGradientLeaf(archRight + width * 0.08f, height * 0.74f, 3.0f.dp.toPx(), 6.5f.dp.toPx(), -15f) // Mid left
            drawGradientLeaf(archRight + width * 0.12f, height * 0.87f, 2.7f.dp.toPx(), 5.8f.dp.toPx(), 80f) // Low right
            drawGradientLeaf(archRight + width * 0.03f, height * 0.84f, 2.7f.dp.toPx(), 5.8f.dp.toPx(), -10f) // Low left
        }
    }
}

