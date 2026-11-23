package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PrayerSetting
import com.example.utils.PrayerAlarmScheduler
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        private const val CHANNEL_BEEP_ID = "prayer_beep_channel"
        private const val CHANNEL_SILENT_ID = "prayer_silent_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        if (action == "android.intent.action.BOOT_COMPLETED" || action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            Log.d(TAG, "Device booted. Rescheduling all prayer alarms...")
            rescheduleAlarms(context)
            return
        }

        if (action != "com.example.ACTION_PRAYER_ALARM") return

        val prayerName = intent.getStringExtra("prayer_name") ?: "Shalat"
        val alertType = intent.getStringExtra("alert_type") ?: "ADZAN"
        val azanSound = intent.getStringExtra("azan_sound") ?: "Makkah"

        Log.d(TAG, "Alarm triggered for $prayerName! Mode: $alertType, Sound: $azanSound")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (alertType) {
                    "ADZAN" -> {
                        // Ring: Start the Foreground Service to play full adzan and show STOP action
                        val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
                            putExtra("prayer_name", prayerName)
                            putExtra("azan_sound", azanSound)
                        }
                        try {
                            ContextCompat.startForegroundService(context, serviceIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start foreground service, using fallback notification with custom sound: ${e.message}")
                            showAdzanNotificationFallback(context, prayerName, azanSound)
                        }
                    }
                    "BEEP" -> {
                        // Vibrate and Play default Notification sound
                        showBeepNotification(context, prayerName)
                        triggerVibrate(context)
                    }
                    "SILENT" -> {
                        // Mute: Silent notification with no vibration, no sound
                        showSilentNotification(context, prayerName)
                    }
                }

                // Automatically reschedule the next cycle of alarms
                rescheduleAlarms(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error in alarm receiver: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showBeepNotification(context: Context, prayerName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_BEEP_ID,
                "Notifikasi Shalat (Bip/Getar)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi shalat dengan getar dan suara bip bawaan"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            manager.createNotificationChannel(channel)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val isPrayer = !prayerName.equals("Imsak", ignoreCase = true) && !prayerName.equals("Syuruq", ignoreCase = true)
        val title = if (isPrayer) "Waktu Shalat $prayerName Telah Tiba" else "Waktu $prayerName Telah Tiba"
        val contentText = if (isPrayer) {
            "Ayo bersiap-siap melakukan ibadah shalat $prayerName."
        } else if (prayerName.equals("Imsak", ignoreCase = true)) {
            "Waktu Imsak telah tiba, ayo bersiap-siap menjelang shalat Subuh."
        } else {
            "Waktu Syuruq (Matahari Terbit) telah tiba, batas waktu shalat Subuh telah habis."
        }

        val appIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
        val notification = NotificationCompat.Builder(context, CHANNEL_BEEP_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(appIcon)
            .setColor(Color.parseColor("#0061A4"))
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setAutoCancel(true)
            .build()

        manager.notify(1008, notification)
    }

    private fun showSilentNotification(context: Context, prayerName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SILENT_ID,
                "Notifikasi Shalat (Senyap)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi shalat dalam keadaan senyap / mute"
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }

        val isPrayer = !prayerName.equals("Imsak", ignoreCase = true) && !prayerName.equals("Syuruq", ignoreCase = true)
        val title = if (isPrayer) "Waktu Shalat $prayerName" else "Waktu $prayerName"
        val contentText = if (isPrayer) {
            "Sudah memasuki waktu shalat $prayerName (Mute)."
        } else if (prayerName.equals("Imsak", ignoreCase = true)) {
            "Sudah memasuki waktu Imsak (Mute)."
        } else {
            "Sudah memasuki waktu Syuruq (Mute)."
        }

        val appIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
        val notification = NotificationCompat.Builder(context, CHANNEL_SILENT_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(appIcon)
            .setColor(Color.parseColor("#0061A4"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        manager.notify(1009, notification)
    }

    private fun triggerVibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(800)
        }
    }

    private fun showAdzanNotificationFallback(context: Context, prayerName: String, azanSound: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_fallback_channel_${azanSound.lowercase()}"
        
        val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/raw/${if (azanSound.equals("Makkah", ignoreCase = true)) "azan_makkah" else "azan_madinah"}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Adzan Shalat Alarms (Fallback)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fallback channel untuk mengumandangkan adzan jadwal shalat"
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPrayer = !prayerName.equals("Imsak", ignoreCase = true) && !prayerName.equals("Syuruq", ignoreCase = true)
        val title = if (isPrayer) "Waktu Shalat $prayerName Telah Tiba" else "Waktu $prayerName Telah Tiba"
        val contentText = if (isPrayer) {
            "Ayo bersiap-siap melakukan ibadah shalat $prayerName. (Mengumandangkan Adzan $azanSound)"
        } else if (prayerName.equals("Imsak", ignoreCase = true)) {
            "Waktu Imsak telah tiba, ayo bersiap-siap menjelang shalat Subuh."
        } else {
            "Waktu Syuruq (Matahari Terbit) telah tiba, batas waktu shalat Subuh telah habis."
        }

        val appIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(appIcon)
            .setColor(Color.parseColor("#0061A4"))
            .setSound(soundUri)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        manager.notify(1010, builder.build())
    }

    private fun rescheduleAlarms(context: Context) {
        val prefs = context.getSharedPreferences("sholat_prefs", Context.MODE_PRIVATE)
        val azanSound = prefs.getString("azan_sound", "Makkah") ?: "Makkah"
        val location = com.example.data.PrayerLocationCatalog.getSavedLocationPreset(context)
        val timeZone = com.example.data.PrayerLocationCatalog.resolveTimeZone(context)
        val cal = Calendar.getInstance(timeZone)

        // Query database to get latest settings
        val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        CoroutineScope(Dispatchers.IO).launch {
            val settingsList = db.prayerSettingDao().getAllSettings().firstOrNull() ?: emptyList()

            val times = PrayerAlarmScheduler.getPrayerTimesFromApiOrCache(
                context = context,
                location = location,
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )

            if (times != null) {
                Log.d("PrayerAlarmReceiver", "Using official API/cache times for reschedule (${location.name}): $times")
                PrayerAlarmScheduler.scheduleAlarms(context, times, settingsList, azanSound)
            } else {
                Log.w("PrayerAlarmReceiver", "Official API/Cache unavailable for ${location.name}; skipping manual calculation")
            }
        }
    }
}
