package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerCountry
import com.example.ui.MainViewModel
import com.example.ui.components.LocationPickerDialog

import android.media.MediaPlayer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import com.example.R
import android.os.Build
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val azanSound by viewModel.azanSound.collectAsState()

    // Observe global service states for active background simulation
    val isServicePlaying by com.example.receiver.PrayerAlarmService.isPlayingAdzan.collectAsState()
    val servicePlayingSound by com.example.receiver.PrayerAlarmService.playingAdzanSound.collectAsState()
    val serviceError by com.example.receiver.PrayerAlarmService.lastError.collectAsState()

    // States for alarm and reminders test
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    var testAlarmCountdown by remember { mutableStateOf(0) }
    var testAlarmActive by remember { mutableStateOf(false) }

    // Use current lifecycle owner to refresh permission on ON_RESUME
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Coroutine countdown logic
    LaunchedEffect(testAlarmActive, testAlarmCountdown) {
        if (testAlarmActive && testAlarmCountdown > 0) {
            kotlinx.coroutines.delay(1000L)
            testAlarmCountdown -= 1
            if (testAlarmCountdown == 0) {
                testAlarmActive = false
            }
        }
    }

    // PendingIntent for our 10-second test alarm
    val testAlarmPendingIntent = remember(context, azanSound) {
        val intent = android.content.Intent().apply {
            setClassName(context.packageName, "com.example.receiver.PrayerAlarmReceiver")
            action = "com.example.ACTION_PRAYER_ALARM"
            putExtra("prayer_name", "Uji Coba Alarm")
            putExtra("alert_type", "ADZAN")
            putExtra("azan_sound", azanSound)
        }
        PendingIntent.getBroadcast(
            context,
            9999, // Unique test request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    LaunchedEffect(serviceError) {
        serviceError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            com.example.receiver.PrayerAlarmService.lastError.value = null
        }
    }

    var showMethodsDialog by remember { mutableStateOf(false) }
    var showCitiesDialog by remember { mutableStateOf(false) }
    var showAdhanDialog by remember { mutableStateOf(false) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSound by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Title
            item {
                Text(
                    text = "Konfigurasi Jadwal Shalat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Official API Source Card (Indonesia Kemenag / Malaysia JAKIM)
            item {
                Card(
                    onClick = { showMethodsDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("setting_calc_method"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sumber Jadwal Shalat Resmi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                currentLocation.country.authorityFull,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Preset City / Zone Overrides Option (Indonesia & Malaysia)
            item {
                Card(
                    onClick = { showCitiesDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("setting_city_override"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Lokasi Wilayah (Indonesia & Malaysia)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${currentLocation.widgetDisplayName()} • ${currentLocation.region}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Adhan Sound Option
            item {
                Card(
                    onClick = { showAdhanDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("setting_adhan_sound"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Suara Adzan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                azanSound,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Active adzan playback status card
            if (isServicePlaying) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("active_adzan_playing_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Simulasi Adzan Sedang Bermain",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Mengumandangkan Adzan ${servicePlayingSound ?: azanSound}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val stopIntent = android.content.Intent(context, com.example.receiver.PrayerAlarmService::class.java).apply {
                                        action = com.example.receiver.PrayerAlarmService.ACTION_STOP_ADZAN
                                    }
                                    try {
                                        context.startService(stopIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Testing Azan Notification Button
            item {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        val testIntent = android.content.Intent(context, com.example.receiver.PrayerAlarmService::class.java).apply {
                            putExtra("prayer_name", "Uji Coba")
                            putExtra("azan_sound", azanSound)
                        }
                        try {
                            ContextCompat.startForegroundService(context, testIntent)
                            Toast.makeText(context, "Memulai simulasi adzan $azanSound...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal memulai simulasi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Izin notifikasi diperlukan untuk simulasi adzan.", Toast.LENGTH_SHORT).show()
                    }
                }

                Button(
                    onClick = {
                        if (isServicePlaying) {
                            val stopIntent = android.content.Intent(context, com.example.receiver.PrayerAlarmService::class.java).apply {
                                action = com.example.receiver.PrayerAlarmService.ACTION_STOP_ADZAN
                            }
                            try {
                                context.startService(stopIntent)
                                Toast.makeText(context, "Menghentikan simulasi adzan...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal menghentikan: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    val testIntent = android.content.Intent(context, com.example.receiver.PrayerAlarmService::class.java).apply {
                                        putExtra("prayer_name", "Uji Coba")
                                        putExtra("azan_sound", azanSound)
                                    }
                                    try {
                                        ContextCompat.startForegroundService(context, testIntent)
                                        Toast.makeText(context, "Memulai simulasi adzan $azanSound...", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal memulai simulasi: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                val testIntent = android.content.Intent(context, com.example.receiver.PrayerAlarmService::class.java).apply {
                                    putExtra("prayer_name", "Uji Coba")
                                    putExtra("azan_sound", azanSound)
                                }
                                try {
                                    ContextCompat.startForegroundService(context, testIntent)
                                    Toast.makeText(context, "Memulai simulasi adzan $azanSound...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal memulai simulasi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("test_azan_notification"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServicePlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isServicePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isServicePlaying) "Hentikan" else "Mainkan"
                        )
                        Text(if (isServicePlaying) "Hentikan Simulasi Adzan" else "Uji Coba Notifikasi Adzan")
                    }
                }
            }

            // Alarm & Reminders Test panel
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("alarm_test_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Uji Coba Alarm & Pengingat (10 Detik)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            "Untuk memastikan sistem adzan otomatis berjalan lancar saat HP terkunci atau dalam keadaan idle (Doze Mode). Fitur ini mendaftarkan alarm fisik ke sistem Android.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )

                        // Permission status section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (hasExactAlarmPermission) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else 
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Status Izin Alarm Presisi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasExactAlarmPermission)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (hasExactAlarmPermission)
                                        "Aktif (Auto Allowed / Diizinkan)"
                                    else
                                        "Belum Diizinkan (Dapat menyebabkan keterlambatan adzan)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            if (!hasExactAlarmPermission) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // General app details fallback
                                                try {
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (ex: Exception) {
                                                    Toast.makeText(context, "Gagal membuka pengaturan: ${ex.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Izin ini otomatis aktif pada versi Android Anda.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Aktifkan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Diizinkan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Countdown UI / Start button
                        if (testAlarmActive) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = testAlarmCountdown / 10f,
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Menguji Alarm... $testAlarmCountdown detik tersisa",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                            alarmManager?.cancel(testAlarmPendingIntent)
                                            testAlarmActive = false
                                            testAlarmCountdown = 0
                                            Toast.makeText(context, "Uji coba alarm dibatalkan.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Batalkan", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                    if (alarmManager != null) {
                                        val triggerTime = System.currentTimeMillis() + 10 * 1000L
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                if (alarmManager.canScheduleExactAlarms()) {
                                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, testAlarmPendingIntent)
                                                } else {
                                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, testAlarmPendingIntent)
                                                }
                                            } else {
                                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, testAlarmPendingIntent)
                                            }
                                            testAlarmCountdown = 10
                                            testAlarmActive = true
                                            Toast.makeText(context, "Alarm tes dijadwalkan dalam 10 detik! Silakan kunci layar atau tunggu...", Toast.LENGTH_LONG).show()
                                        } catch (e: SecurityException) {
                                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, testAlarmPendingIntent)
                                            testAlarmCountdown = 10
                                            testAlarmActive = true
                                            Toast.makeText(context, "Menggunakan alarm tidak presisi (diperbolehkan). Menunggu 10 detik...", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Gagal menjadwalkan alarm: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("test_exact_alarm_manager"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Text("Jadwalkan Alarm Tes (10 Detik)")
                                }
                            }
                        }
                    }
                }
            }

            // App details info panel
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Informasi Aplikasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            "Payo Shalat — Asisten Ibadah Harianmu yang akurat, responsif, dan tersinkronisasi aman di cloud.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "versi 2.0 (Payo Shalat)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                lineHeight = 14.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Developed with",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "love",
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "by Zen Bahsin",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Official API Source Dialog
    if (showMethodsDialog) {
        AlertDialog(
            onDismissRequest = { showMethodsDialog = false },
            title = { Text("Sumber Jadwal Resmi", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Jadwal shalat diambil langsung dari API resmi tanpa perhitungan manual. Sumber otomatis menyesuaikan negara lokasi yang dipilih:",
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
                                text = "Data resmi Bimas Islam Kemenag RI via Equran.id API.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                                text = "Data takwim solat rasmi JAKIM (Waktu Solat Malaysia API & e-Solat).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showMethodsDialog = false
                    showCitiesDialog = true
                }) {
                    Text("Pilih Lokasi / Negara", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMethodsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Unified Indonesia & Malaysia Location Picker Dialog
    if (showCitiesDialog) {
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
                val hasLoc = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasLoc) {
                    viewModel.requestDeviceLocation(forceUserRequest = true)
                    Toast.makeText(context, "Mendeteksi lokasi GPS...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Aktifkan izin lokasi di menu Jadwal Shalat untuk GPS otomatis.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showCitiesDialog = false }
        )
    }

    // Adhan Dialog
    if (showAdhanDialog) {
        val adhanOptions = listOf("Makkah", "Madinah")
        AlertDialog(
            onDismissRequest = {
                mediaPlayer?.release()
                mediaPlayer = null
                playingSound = null
                showAdhanDialog = false
            },
            title = { Text("Pilih Suara Adzan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    adhanOptions.forEach { sound ->
                        val isSelected = azanSound == sound
                        val isPlaying = playingSound == sound
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateAzanSound(sound)
                                    Toast.makeText(context, "Suara adzan diatur ke $sound", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sound,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                playingSound = null
                                            } else {
                                                try {
                                                    mediaPlayer?.release()
                                                    val resId = if (sound == "Makkah") R.raw.azan_makkah else R.raw.azan_madinah
                                                    mediaPlayer = MediaPlayer.create(context, resId)
                                                    mediaPlayer?.start()
                                                    playingSound = sound
                                                    mediaPlayer?.setOnCompletionListener {
                                                        it.release()
                                                        mediaPlayer = null
                                                        playingSound = null
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Gagal memutar: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Hentikan" else "Mainkan",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Terpilih", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    playingSound = null
                    showAdhanDialog = false
                }) {
                    Text("Tutup")
                }
            }
        )
    }
}
