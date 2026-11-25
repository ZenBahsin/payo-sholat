package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.MainViewModel
import com.example.ui.components.LocationPickerDialog
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToHadith: () -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val displayName by viewModel.userDisplayName.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val avatarUrl by viewModel.userAvatarUrl.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val hijriDate by viewModel.hijriDateString.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val nextPrayerTime by viewModel.nextPrayerTime.collectAsState()
    val currentPrayerName by viewModel.currentPrayerName.collectAsState()
    val countdownText by viewModel.countdownText.collectAsState()
    val useAutoGps by viewModel.useAutoGps.collectAsState()

    val streakDays by viewModel.prayerStreakDays.collectAsState()
    val todayCompletedCount by viewModel.todayCompletedCount.collectAsState()
    val quranProgress by viewModel.quranProgress.collectAsState()
    val tasbih by viewModel.tasbihState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var isSigningOut by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream),
        contentPadding = PaddingValues(
            bottom = bottomContentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Soft Sage Gradient Header with User Identity
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SoftSage, WarmCream)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Profil & Akun",
                        style = MaterialTheme.typography.headlineLarge,
                        color = DeepForestCharcoal
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, SageBorder),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // User Avatar
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SoftSage)
                                    .border(2.dp, WarmSandGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Foto Profil $displayName",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = displayName.firstOrNull()?.uppercase() ?: "P",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = DeepSage
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DeepForestCharcoal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (email.isNotBlank()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedOliveGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = SoftSageLight
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = DeepSage,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Tersinkron Cloud",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepSage,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Worship Summary Metrics (2x2 Grid)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ringkasan Ibadahmu",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepForestCharcoal
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        title = "Streak Shalat",
                        value = "$streakDays Hari",
                        subtitle = "Konsistensi harian",
                        icon = Icons.Default.LocalFireDepartment,
                        accentBg = WarmSandGoldSoft,
                        accentTint = WarmSandGoldDark,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        title = "Hari Ini",
                        value = "$todayCompletedCount/5",
                        subtitle = "Shalat tercatat",
                        icon = Icons.Default.CheckCircle,
                        accentBg = SoftSageLight,
                        accentTint = DeepSage,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        title = "Target Khatam",
                        value = "${quranProgress?.khatamPercentage ?: 0}%",
                        subtitle = quranProgress?.lastSurahName?.takeIf { it.isNotBlank() }
                            ?: "Belum mulai",
                        icon = Icons.Default.MenuBook,
                        accentBg = SoftSageLight,
                        accentTint = DeepSage,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        title = "Tasbih Digital",
                        value = "${tasbih.currentCount}/${tasbih.targetCount}",
                        subtitle = tasbih.dhikrName,
                        icon = Icons.Default.Adjust,
                        accentBg = WarmSandGoldSoft,
                        accentTint = WarmSandGoldDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Preferences & Quick Features List
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, SageBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileActionRow(
                        icon = Icons.Default.LocationOn,
                        title = "Lokasi & Jadwal Shalat",
                        subtitle = "${currentLocation.widgetDisplayName()} • ${currentLocation.country.authorityShort}",
                        onClick = { showLocationDialog = true },
                        testTag = "profile_location_row"
                    )
                    HorizontalDivider(color = SoftSageDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileActionRow(
                        icon = Icons.Default.Settings,
                        title = "Pengaturan Adzan & Notifikasi",
                        subtitle = "Atur suara adzan, GPS otomatis, dan pengingat",
                        onClick = onNavigateToSettings,
                        testTag = "profile_settings_row"
                    )
                    HorizontalDivider(color = SoftSageDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileActionRow(
                        icon = Icons.Default.Widgets,
                        title = "Widget Layar Utama (4x1)",
                        subtitle = "Pratinjau & panduan pasang widget horizontal",
                        onClick = { showWidgetDialog = true },
                        testTag = "profile_widget_row"
                    )
                    HorizontalDivider(color = SoftSageDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileActionRow(
                        icon = Icons.Default.Adjust,
                        title = "Tasbih Digital",
                        subtitle = "Lanjutkan dzikir: ${tasbih.dhikrName}",
                        onClick = onNavigateToTasbih,
                        testTag = "profile_tasbih_row"
                    )
                    HorizontalDivider(color = SoftSageDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileActionRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Doa & Hadits Harian",
                        subtitle = "Kumpulan hadits shahih & dzikir harian",
                        onClick = onNavigateToHadith,
                        testTag = "profile_hadith_row"
                    )
                }
            }
        }

        // 4. Logout Button
        item {
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                enabled = !isSigningOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .testTag("logout_button"),
                shape = CircleShape,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = PureWhite
                )
            ) {
                if (isSigningOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Keluar...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keluar dari Akun",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = PureWhite,
            title = {
                Text(
                    text = "Keluar dari Payo Shalat?",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepForestCharcoal
                )
            },
            text = {
                Text(
                    text = "Seluruh riwayat shalat, streak, progres Al-Qur'an, dan tasbihmu tetap tersimpan aman di akun Google-mu dan akan dimuat otomatis saat masuk kembali.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedOliveGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        isSigningOut = true
                        scope.launch {
                            GoogleAuthHelper.signOut(context)
                            isSigningOut = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = CircleShape,
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Keluar", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = DeepSage, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showLocationDialog) {
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
            onDismiss = { showLocationDialog = false }
        )
    }

    if (showWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetDialog = false },
            containerColor = PureWhite,
            title = {
                Text(
                    text = "Widget Horizontal Layar Utama",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepForestCharcoal
                )
            },
            text = {
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
                        viewModel.setAutoGpsEnabled(enabled)
                        if (enabled && !locationPermissionState.status.isGranted) {
                            locationPermissionState.launchPermissionRequest()
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
            },
            confirmButton = {
                TextButton(onClick = { showWidgetDialog = false }) {
                    Text("Selesai", color = DeepSage, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentBg: Color,
    accentTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SageBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = TabularNumberTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = DeepForestCharcoal
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepForestCharcoal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedOliveGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoftSageLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepSage,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = DeepForestCharcoal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedOliveGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MutedOliveGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
