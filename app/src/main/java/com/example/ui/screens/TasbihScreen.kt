package com.example.ui.screens

import android.content.Context
import android.os.Build
import kotlinx.coroutines.delay
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tasbihState by viewModel.tasbihState.collectAsState()

    val dhikrPresets = listOf(
        "Subhanallah",
        "Alhamdulillah",
        "Allahu Akbar",
        "Astaghfirullah",
        "La ilaha illallah",
        "Subhanallah wal Hamdulillah"
    )

    val targetPresets = listOf(33, 99, 100, 1000)

    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    // Helper function for vibrating phone on count click
    val triggerVibrate = { durationMs: Long ->
        if (hapticFeedbackEnabled) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                durationMs,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(durationMs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Progress bar calculation
    val progress = remember(tasbihState.currentCount, tasbihState.targetCount) {
        if (tasbihState.targetCount > 0) {
            (tasbihState.currentCount.toFloat() / tasbihState.targetCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "TasbihProgressAnim")

    // Check if target is reached to trigger unique double vibe
    LaunchedEffect(tasbihState.currentCount) {
        if (tasbihState.currentCount > 0 && tasbihState.currentCount == tasbihState.targetCount) {
            triggerVibrate(200)
            delay(150)
            triggerVibrate(200)
            Toast.makeText(context, "Masha Allah! Target Zikir Tercapai!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasbih Digital", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Reset Button
                    IconButton(onClick = {
                        viewModel.resetTasbih()
                        triggerVibrate(100)
                        Toast.makeText(context, "Hitungan direset ke 0", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                    // Haptic feedback toggle
                    IconButton(onClick = {
                        hapticFeedbackEnabled = !hapticFeedbackEnabled
                        triggerVibrate(100)
                    }) {
                        Icon(
                            imageVector = if (hapticFeedbackEnabled) Icons.Default.Vibration else Icons.Default.VolumeUp,
                            contentDescription = "Haptic Toggle",
                            tint = if (hapticFeedbackEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Preset Dhikr Horizontal Selector List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pilih Lafadz Zikir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dhikrPresets) { text ->
                        val isSelected = tasbihState.dhikrName == text
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateDhikrName(text)
                                triggerVibrate(50)
                            },
                            label = { Text(text) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Giant Count Dial (Visual Centerpiece)
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    )
                    .clickable {
                        viewModel.incrementTasbih()
                        triggerVibrate(60)
                    }
                    .testTag("tasbih_tap_area"),
                contentAlignment = Alignment.Center
            ) {
                // Circular Progress rim (Draw behind)
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(240.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tasbihState.dhikrName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tasbihState.currentCount.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target: ${tasbihState.targetCount}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Bottom Target & Settings Controls Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Target Presets
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Hitungan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrement count
                            IconButton(
                                onClick = {
                                    viewModel.decrementTasbih()
                                    triggerVibrate(50)
                                },
                                modifier = Modifier.size(32.dp),
                                enabled = tasbihState.currentCount > 0
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang")
                            }
                            Text(
                                "Kurangi Hitungan",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targetPresets.forEach { target ->
                            val isSelected = tasbihState.targetCount == target
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        viewModel.updateTasbihTarget(target)
                                        triggerVibrate(50)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (target == 1000) "Bebas" else target.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
