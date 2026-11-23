package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.PrayerWidgetProvider
import com.example.receiver.PrayerWidgetProvider.WidgetTheme
import com.example.ui.theme.HighlightBlue
import com.example.ui.theme.PrimaryBlue
import com.example.utils.PrayerTimesCalculator
import com.example.utils.WidgetLocationSyncHelper

@Composable
fun HorizontalWidgetPreviewCard(
    locationName: String,
    hijriDate: String,
    prayerTimes: PrayerTimesCalculator.PrayerTimes,
    nextPrayerName: String,
    nextPrayerTime: String,
    currentPrayerName: String = "",
    countdownText: String = "",
    authorityShort: String = "Kemenag RI",
    useAutoGps: Boolean? = null,
    onToggleAutoGps: ((Boolean) -> Unit)? = null,
    onRefreshWidget: (() -> Unit)? = null,
    selectedTheme: WidgetTheme? = null,
    onThemeSelected: ((WidgetTheme) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val onePxDp = with(density) { 1f.toDp() }

    var showSamsungGuideDialog by remember { mutableStateOf(false) }
    var internalTheme by remember {
        mutableStateOf(PrayerWidgetProvider.getSavedTheme(context))
    }
    var internalAutoGps by remember {
        mutableStateOf(WidgetLocationSyncHelper.isAutoGpsEnabled(context))
    }

    val activeTheme = selectedTheme ?: internalTheme
    val isAutoGpsActive = useAutoGps ?: internalAutoGps
    val isLightTheme = activeTheme == WidgetTheme.FROSTED_LIGHT

    val handleThemeChange: (WidgetTheme) -> Unit = { newTheme ->
        internalTheme = newTheme
        PrayerWidgetProvider.setSavedTheme(context, newTheme)
        onThemeSelected?.invoke(newTheme)
    }

    val handleAutoGpsToggle: (Boolean) -> Unit = { enabled ->
        internalAutoGps = enabled
        if (onToggleAutoGps != null) {
            onToggleAutoGps(enabled)
        } else {
            WidgetLocationSyncHelper.setAutoGpsEnabled(context, enabled)
            PrayerWidgetProvider.updateAllWidgets(context)
        }
    }

    val hasSchedule = prayerTimes.zuhur.isNotEmpty() && prayerTimes.subuh.isNotEmpty()
    val isAfterIsya = countdownText.contains("Telah", ignoreCase = true) ||
        (currentPrayerName.equals("Isya", ignoreCase = true) && nextPrayerName.equals("Isya", ignoreCase = true))

    val badgeText = when {
        !hasSchedule -> "Memuat $authorityShort..."
        isAfterIsya -> "Sekarang: Isya ${prayerTimes.isya.ifEmpty { nextPrayerTime }}"
        else -> "Selanjutnya: $nextPrayerName $nextPrayerTime"
    }

    val highlightedPrayerName = when {
        !hasSchedule -> ""
        isAfterIsya -> "Isya"
        else -> when (nextPrayerName) {
            "Imsak", "Subuh", "Syuruq" -> "Subuh"
            else -> nextPrayerName
        }
    }

    // Theme Palette Definitions (Tema 1: Muted Slate Blue vs Tema 2: Frosted Light)
    val primaryTextColor = if (isLightTheme) {
        Color(0xFF1E2430) // Dark slate
    } else {
        Color(0xFFFFFFFF) // Pure white
    }

    val secondaryTextColor = if (isLightTheme) {
        Color(0xFF687385) // Muted gray
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.78f) // rgba(255, 255, 255, 0.78)
    }

    val inactiveItemBg = if (isLightTheme) {
        Color.Transparent // Transparan tanpa border
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.08f) // rgba(255, 255, 255, 0.08) tanpa border
    }

    val activeItemAndPillBg = if (isLightTheme) {
        Color(0xFF5873A3) // Calm blue accent (#5873A3)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.24f) // rgba(255, 255, 255, 0.24)
    }

    val activeBorderColor = if (isLightTheme) {
        Color.Transparent
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.50f) // 1px solid rgba(255, 255, 255, 0.5)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("horizontal_widget_preview_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighlightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Widget Jadwal Sholat Horizontal",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Estetika Native One UI Samsung & Android (4x1)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = { showSamsungGuideDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Panduan Pasang Widget",
                        tint = PrimaryBlue
                    )
                }
            }

            // Theme Selection Toggle (Tema 1: Muted Slate Blue vs Tema 2: Frosted Light)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pilihan Tema Widget (One UI):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_theme_selector_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetThemeOptionChip(
                        title = "Muted Slate Blue",
                        subtitle = "Widget Cuaca",
                        swatchBrush = Brush.linearGradient(
                            colors = listOf(Color(0xFF536D9C), Color(0xFF6782B2))
                        ),
                        swatchBorderColor = Color.White.copy(alpha = 0.4f),
                        isSelected = activeTheme == WidgetTheme.SLATE_BLUE,
                        onClick = { handleThemeChange(WidgetTheme.SLATE_BLUE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_option_slate_blue")
                    )

                    WidgetThemeOptionChip(
                        title = "Frosted Light",
                        subtitle = "Widget Health",
                        swatchBrush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF4F6FA), Color(0xFFE4E9F2))
                        ),
                        swatchBorderColor = Color(0xFF5873A3).copy(alpha = 0.5f),
                        isSelected = activeTheme == WidgetTheme.FROSTED_LIGHT,
                        onClick = { handleThemeChange(WidgetTheme.FROSTED_LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_option_frosted_light")
                    )
                }
            }

            // Real-Time GPS Auto-Sync Toggle for Home Screen Widget (FusedLocationProviderClient)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = 1.dp,
                        color = if (isAutoGpsActive) {
                            PrimaryBlue.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { handleAutoGpsToggle(!isAutoGpsActive) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("widget_auto_gps_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = if (isAutoGpsActive) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Lokasi Otomatis GPS (Real-Time)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAutoGpsActive) {
                                "Widget otomatis mengikuti lokasi perangkat"
                            } else {
                                "Mode lokasi manual — ketuk untuk sinkron GPS"
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
                Switch(
                    checked = isAutoGpsActive,
                    onCheckedChange = { handleAutoGpsToggle(it) },
                    modifier = Modifier.testTag("widget_auto_gps_switch")
                )
            }

            // Live Interactive Preview of the Horizontal 4x1 One UI Widget
            // Corner radius: 24dp (container utama), 14dp (kartu item waktu sholat), flat tanpa drop-shadow
            val mainWidgetBackgroundModifier = if (isLightTheme) {
                Modifier
                    .background(Color(0xFFF4F6FA).copy(alpha = 0.90f))
                    .border(
                        width = onePxDp,
                        color = Color(0xFF1E2430).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    )
            } else {
                Modifier.background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF536D9C).copy(alpha = 0.92f),
                            Color(0xFF6782B2).copy(alpha = 0.92f)
                        ),
                        start = Offset(0f, Float.POSITIVE_INFINITY), // kiri/bawah
                        end = Offset(Float.POSITIVE_INFINITY, 0f)    // kanan/atas
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .then(mainWidgetBackgroundModifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("widget_live_preview_container")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Widget Header Row: Dedicated Location Name + Pin on Line 1, Hijri & Authority on Line 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Lokasi Widget",
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = locationName,
                                    color = primaryTextColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "$hijriDate • $authorityShort",
                                color = secondaryTextColor,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 17.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Pill "Selanjutnya" / "Sekarang"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(activeItemAndPillBg)
                                    .then(
                                        if (!isLightTheme) {
                                            Modifier.border(
                                                width = onePxDp,
                                                color = activeBorderColor,
                                                shape = RoundedCornerShape(50.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = Color(0xFFFFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Refresh Button
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(activeItemAndPillBg)
                                    .then(
                                        if (!isLightTheme) {
                                            Modifier.border(
                                                width = onePxDp,
                                                color = activeBorderColor,
                                                shape = RoundedCornerShape(50.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        if (onRefreshWidget != null) {
                                            onRefreshWidget()
                                        } else {
                                            PrayerWidgetProvider.updateAllWidgets(context)
                                        }
                                        Toast.makeText(
                                            context,
                                            if (isAutoGpsActive) "Menyinkronkan lokasi GPS & jadwal widget..." else "Widget Horizontal disinkronkan!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Widget",
                                    tint = Color(0xFFFFFFFF),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Widget Horizontal 5-Prayer Bar (Radius item = 14dp)
                    val fivePrayers = listOf(
                        Pair("Subuh", prayerTimes.subuh.ifEmpty { "--:--" }),
                        Pair("Zuhur", prayerTimes.zuhur.ifEmpty { "--:--" }),
                        Pair("Asar", prayerTimes.asar.ifEmpty { "--:--" }),
                        Pair("Maghrib", prayerTimes.maghrib.ifEmpty { "--:--" }),
                        Pair("Isya", prayerTimes.isya.ifEmpty { "--:--" })
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        fivePrayers.forEach { (name, time) ->
                            val isHighlighted = highlightedPrayerName.isNotEmpty() && name.equals(highlightedPrayerName, ignoreCase = true)
                            val itemShape = RoundedCornerShape(14.dp)

                            val itemLabelColor = if (isHighlighted) {
                                Color(0xFFFFFFFF)
                            } else {
                                secondaryTextColor
                            }

                            val itemTimeColor = if (isHighlighted) {
                                Color(0xFFFFFFFF)
                            } else {
                                primaryTextColor
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(itemShape)
                                    .background(
                                        if (isHighlighted) activeItemAndPillBg
                                        else inactiveItemBg
                                    )
                                    .then(
                                        if (isHighlighted && !isLightTheme) {
                                            Modifier.border(
                                                width = onePxDp,
                                                color = activeBorderColor,
                                                shape = itemShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = name,
                                    color = itemLabelColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = time,
                                    color = itemTimeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Pin Directly to Home Screen + Manual Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        PrayerWidgetProvider.updateAllWidgets(context)
                        val pinned = PrayerWidgetProvider.requestPinHorizontalWidget(context)
                        if (!pinned) {
                            showSamsungGuideDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Konfirmasi pemasangan widget horizontal di layar utama Anda",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_pin_horizontal_widget"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddToHomeScreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pasang Widget Horizontal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { showSamsungGuideDialog = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Cara Pasang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showSamsungGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSamsungGuideDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = PrimaryBlue)
                    Text(
                        "Cara Pasang Widget Horizontal di HP Samsung",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ikuti langkah mudah berikut untuk menampilkan jadwal 5 waktu sholat secara horizontal di layar depan HP Samsung (One UI) atau Android Anda:",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    BulletItem("1. Pilih tema widget favorit Anda di atas (Muted Slate Blue atau Frosted Light).")
                    BulletItem("2. Tekan tahan area kosong di Layar Utama (Home Screen) HP Samsung Anda atau cubit layar dengan 2 jari.")
                    BulletItem("3. Pilih menu 'Widgets' (Widget) di bagian bawah layar.")
                    BulletItem("4. Cari 'Payo Sholat!' lalu pilih 'Jadwal Sholat (Horizontal)' berukuran 4x1.")
                    BulletItem("5. Ketuk 'Tambah' (Add) atau seret ke layar utama. Tema widget otomatis mengikuti pilihan Anda.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSamsungGuideDialog = false }) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun WidgetThemeOptionChip(
    title: String,
    subtitle: String,
    swatchBrush: Brush,
    swatchBorderColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        HighlightBlue.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val borderColor = if (isSelected) {
        PrimaryBlue
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(swatchBrush)
                .border(1.dp, swatchBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (title.contains("Light")) Color(0xFF1E2430) else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
