package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val PREFS_NAME = "gol_mx_notifications_prefs"
    
    const val KEY_BREAKING_ENABLED = "breaking_news_notifications_enabled"
    const val KEY_SUMMARY_ENABLED = "daily_summary_notifications_enabled"
    
    const val CHANNEL_BREAKING_ID = "breaking_news_channel"
    const val CHANNEL_SUMMARY_ID = "daily_summary_channel"
    
    private const val NOTIFY_BREAKING_ID = 101
    private const val NOTIFY_SUMMARY_ID = 102

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initializes notification channels required for Android Oreo (API 26) and above.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            
            // Channel 1: Breaking News (High Importance)
            val breakingChannel = NotificationChannel(
                CHANNEL_BREAKING_ID,
                "Noticias de Última Hora",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas instantáneas y noticias de última hora sobre deportes"
                enableLights(true)
                lightColor = 0xFF00A651.toInt() // PrimaryGreen
                enableVibration(true)
            }
            
            // Channel 2: Daily Summary (Default Importance)
            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY_ID,
                "Resumen Diario Inteligente",
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resumen diario de las mejores noticias nacionales e internacionales de fútbol"
                enableLights(true)
                lightColor = 0xFFE10600.toInt() // AccentRed
            }
            
            notificationManager.createNotificationChannel(breakingChannel)
            notificationManager.createNotificationChannel(summaryChannel)
            Log.d(TAG, "Notification channels created successfully")
        }
    }

    /**
     * Checks if breaking news notifications are enabled in settings.
     */
    fun isBreakingEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_BREAKING_ENABLED, true)
    }

    /**
     * Toggles breaking news notifications.
     */
    fun setBreakingEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_BREAKING_ENABLED, enabled).apply()
    }

    /**
     * Checks if daily summary notifications are enabled in settings.
     */
    fun isSummaryEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_SUMMARY_ENABLED, true)
    }

    /**
     * Toggles daily summary notifications.
     */
    fun setSummaryEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_SUMMARY_ENABLED, enabled).apply()
    }

    /**
     * Posts a breaking news notification if the preference is enabled.
     */
    fun showBreakingNewsNotification(context: Context, title: String, message: String) {
        if (!isBreakingEnabled(context)) {
            Log.d(TAG, "Breaking news notifications are disabled by the user")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_BREAKING_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFY_BREAKING_ID, builder.build())
            Log.d(TAG, "Breaking news notification posted")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission POST_NOTIFICATIONS is missing: ${e.message}")
        }
    }

    /**
     * Posts a daily summary notification if the preference is enabled.
     */
    fun showDailySummaryNotification(context: Context, title: String, message: String) {
        if (!isSummaryEnabled(context)) {
            Log.d(TAG, "Daily summary notifications are disabled by the user")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SUMMARY_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFY_SUMMARY_ID, builder.build())
            Log.d(TAG, "Daily summary notification posted")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission POST_NOTIFICATIONS is missing: ${e.message}")
        }
    }
}
