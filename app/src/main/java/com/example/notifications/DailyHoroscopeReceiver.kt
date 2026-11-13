package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.VedicAstrologyEngine
import java.util.Calendar

class DailyHoroscopeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val rashiId = intent?.getStringExtra(EXTRA_RASHI_ID) ?: "mesha"
        HoroscopeNotificationScheduler.postDailyHoroscopeNotification(context, rashiId)
    }

    companion object {
        const val EXTRA_RASHI_ID = "extra_rashi_id"
    }
}

object HoroscopeNotificationScheduler {
    private const val CHANNEL_ID = "jevanmantra_daily_rashifal"
    private const val NOTIFICATION_ID = 1008
    private const val ALARM_REQUEST_CODE = 2008

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Rashifal & Panchang Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily personalized Vedic horoscope and auspicious Muhurat notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(
        context: Context,
        enabled: Boolean,
        hourOfDay: Int,
        minute: Int,
        rashiId: String
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyHoroscopeReceiver::class.java).apply {
            putExtra(DailyHoroscopeReceiver.EXTRA_RASHI_ID, rashiId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun postDailyHoroscopeNotification(context: Context, rashiId: String): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val sign = VedicAstrologyEngine.getSignById(rashiId)
        val preview = VedicAstrologyEngine.getInstantDailySummary(sign)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✨ ${sign.sanskritName} (${sign.englishName}) Daily Rashifal")
            .setContentText(preview)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$preview\nLucky Color: ${sign.luckyColor} • Lucky No: ${sign.luckyNumber}\nTap to ask AI Jyotish or check today's Shubh Muhurat."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }
}
