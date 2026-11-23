package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class PrayerAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var prayerName: String = "Shalat"
    private var azanSound: String = "Makkah"

    companion object {
        private const val TAG = "PrayerAlarmService"
        private const val CHANNEL_ID = "prayer_alarm_channel"
        private const val NOTIFICATION_ID = 1007
        const val ACTION_STOP_ADZAN = "com.example.ACTION_STOP_ADZAN"

        // Expose playing state globally for Jetpack Compose screens to observe
        val isPlayingAdzan = kotlinx.coroutines.flow.MutableStateFlow(false)
        val playingAdzanSound = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        val lastError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PrayerAlarmService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_STOP_ADZAN) {
            Log.d(TAG, "Stop adzan action triggered")
            stopAdzan()
            return START_NOT_STICKY
        }

        // Reset last error
        lastError.value = null

        prayerName = intent?.getStringExtra("prayer_name") ?: "Shalat"
        azanSound = intent?.getStringExtra("azan_sound") ?: "Makkah"

        Log.d(TAG, "Starting adzan playback for $prayerName with sound $azanSound")

        // 1. Start foreground service with notification FIRST.
        // This is a strict requirement to prevent ForegroundServiceDidNotStartInTimeException on Android 14+.
        try {
            startForegroundServiceWithNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error in startForegroundServiceWithNotification: ${e.message}")
            lastError.value = "Gagal menampilkan notifikasi adzan: ${e.message}"
        }

        // 3. Play audio SECOND. If this fails, the service is already safely in the foreground,
        // so calling stopSelf() on error will not crash the app.
        try {
            playAdhanSound()
        } catch (e: Exception) {
            Log.e(TAG, "Error in playAdhanSound: ${e.message}")
            lastError.value = "Gagal memutar suara adzan: ${e.message}"
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        // Check if notifications are enabled in system settings
        val isNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (!isNotificationsEnabled) {
            Log.w(TAG, "Notifications are disabled in system settings for this app!")
            lastError.value = "Notifikasi dinonaktifkan di setelan sistem! Suara adzan tetap diupayakan diputar."
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adzan Shalat Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel untuk mengumandangkan adzan jadwal shalat"
                setSound(null, null) // Silent channel so it doesn't conflict with our MediaPlayer
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Action Intent to stop the Adzan
        val stopIntent = Intent(this, PrayerAlarmService::class.java).apply {
            action = ACTION_STOP_ADZAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content Intent to open MainActivity when clicked
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get user configured location name and format current time in the location's official timezone
        val locationPreset = com.example.data.PrayerLocationCatalog.getSavedLocationPreset(this)
        val locationLabel = locationPreset.widgetDisplayName()
        val timeZone = com.example.data.PrayerLocationCatalog.resolveTimeZone(this)

        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        val currentTime = sdf.format(java.util.Date())

        // Build standard clean notification without MediaStyle/MediaSession to look identical to standard system notification
        val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Memasuki waktu $prayerName")
            .setContentText("$locationLabel ($currentTime)")
            .setSmallIcon(R.drawable.ic_notification) // Use app logo monochrome icon for custom notification brand
            .setLargeIcon(appIcon)
            .setColor(Color.parseColor("#0061A4"))
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Add a standard clean action button to stop the adzan without changing the notification style
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "Hentikan",
            stopPendingIntent
        ).build()
        builder.addAction(stopAction)

        val notification = builder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Successfully started foreground service with mediaPlayback type")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service with type: ${e.message}. Attempting fallback...")
            // On Android 14+, do NOT call the single-parameter startForeground as that will crash with MissingForegroundServiceTypeException
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback startForeground failed: ${ex.message}")
                }
            } else {
                lastError.value = "SecurityException starting foreground service: ${e.message}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}. Attempting fallback...")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback startForeground failed: ${ex.message}")
                }
            } else {
                lastError.value = "Error starting foreground service: ${e.message}"
            }
        }
    }

    private fun getAudioFileUri(resId: Int): java.io.File? {
        return try {
            val inputStream = resources.openRawResource(resId)
            val tempFile = java.io.File(cacheDir, "temp_adzan_${resId}.mp3")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            java.io.FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(1024 * 4)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
            inputStream.close()
            Log.d(TAG, "Copied raw resource $resId to temp file: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy raw resource to temp file: ${e.message}")
            null
        }
    }

    private fun playAdhanSound() {
        try {
            mediaPlayer?.release()

            val primaryResId = if (azanSound.equals("Makkah", ignoreCase = true)) {
                R.raw.azan_makkah
            } else {
                R.raw.azan_madinah
            }

            var success = false
            var errorDetails = ""
            var mp = MediaPlayer()

            fun applyAttributes(player: MediaPlayer) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    player.setAudioStreamType(AudioManager.STREAM_ALARM)
                }
            }

            fun tryPlayResource(resId: Int): Boolean {
                applyAttributes(mp)
                
                // Try 1: Local Cache File via FileInputStream FD (Most robust)
                try {
                    val tempFile = getAudioFileUri(resId)
                    if (tempFile != null && tempFile.exists()) {
                        val fis = java.io.FileInputStream(tempFile)
                        mp.setDataSource(fis.fd)
                        mp.prepare()
                        fis.close()
                        Log.d(TAG, "Prepared MediaPlayer successfully using temp file FD for res $resId")
                        return true
                    } else {
                        errorDetails += "T1: Temp file null/missing for $resId; "
                    }
                } catch (tempFileEx: Exception) {
                    Log.e(TAG, "Temp file FD playback failed for $resId: ${tempFileEx.message}")
                    errorDetails += "T1: ${tempFileEx.message} for $resId; "
                    mp.reset()
                    
                    // Fallback to path string
                    try {
                        val tempFile = java.io.File(cacheDir, "temp_adzan_${resId}.mp3")
                        if (tempFile.exists()) {
                            mp.setDataSource(tempFile.absolutePath)
                            mp.prepare()
                            Log.d(TAG, "Prepared MediaPlayer successfully using temp file path string for res $resId")
                            return true
                        }
                    } catch (pathEx: Exception) {
                        Log.e(TAG, "Temp file path playback failed for $resId: ${pathEx.message}")
                        errorDetails += "T1-path: ${pathEx.message}; "
                        mp.reset()
                    }
                }

                // Try 2: AssetFileDescriptor
                try {
                    val assetFileDescriptor = resources.openRawResourceFd(resId)
                    mp.setDataSource(
                        assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length
                    )
                    assetFileDescriptor.close()
                    mp.prepare()
                    Log.d(TAG, "Prepared MediaPlayer successfully using file descriptor for res $resId")
                    return true
                } catch (fdException: Exception) {
                    Log.e(TAG, "File descriptor playback failed for $resId: ${fdException.message}")
                    errorDetails += "T2: ${fdException.message} for $resId; "
                    mp.reset()
                }

                // Try 3: MediaPlayer.create fallback
                try {
                    Log.d(TAG, "Attempting MediaPlayer.create fallback for $resId...")
                    mp.release()
                    val fallbackMp = MediaPlayer.create(this@PrayerAlarmService, resId)
                    if (fallbackMp != null) {
                        mp = fallbackMp
                        Log.d(TAG, "MediaPlayer.create succeeded for res $resId")
                        return true
                    } else {
                        errorDetails += "T3: MediaPlayer.create returned null for $resId; "
                    }
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fallback MediaPlayer.create failed for $resId: ${fallbackEx.message}")
                    errorDetails += "T3: ${fallbackEx.message} for $resId; "
                }
                
                mp = MediaPlayer()
                return false
            }

            // Attempt primary resource
            success = tryPlayResource(primaryResId)

            // If primary fails and we were trying Madinah, fall back to Madinah1
            if (!success && primaryResId == R.raw.azan_madinah) {
                Log.d(TAG, "Primary Madinah failed, trying Madinah1 fallback...")
                errorDetails += "[Falling back to Madinah1] "
                success = tryPlayResource(R.raw.azan_madinah1)
            }

            if (!success) {
                Log.e(TAG, "All playback attempts failed")
                isPlayingAdzan.value = false
                playingAdzanSound.value = null
                lastError.value = "Gagal memutar adzan: $errorDetails"
                stopSelf()
                return
            }

            // At this point, `mp` is prepared and ready to play
            mediaPlayer = mp.apply {
                isLooping = false
                setOnCompletionListener {
                    Log.d(TAG, "Adzan playback completed naturally")
                    stopSelf()
                }
                start()
            }

            isPlayingAdzan.value = true
            playingAdzanSound.value = azanSound
            Log.d(TAG, "Adzan playback started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing adzan sound: ${e.message}")
            isPlayingAdzan.value = false
            playingAdzanSound.value = null
            lastError.value = "Gagal memutar suara adzan: ${e.message}"
            stopSelf()
        }
    }

    private fun stopAdzan() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping adzan: ${e.message}")
        } finally {
            isPlayingAdzan.value = false
            playingAdzanSound.value = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PrayerAlarmService destroyed")
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isPlayingAdzan.value = false
            playingAdzanSound.value = null
        }
    }
}
