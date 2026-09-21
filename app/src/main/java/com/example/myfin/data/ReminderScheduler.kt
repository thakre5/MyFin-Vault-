package com.example.myfin.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import java.util.Calendar

object ReminderScheduler {

    const val CHANNEL_ID_REMINDERS = "myfin_daily_reminders"
    const val ACTION_DAILY_REMINDER = "com.example.myfin.ACTION_DAILY_REMINDER"
    const val ACTION_TEST_NOTIFICATION = "com.example.myfin.ACTION_TEST_NOTIFICATION"
    private const val REMINDER_REQUEST_CODE = 9001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val name = "Daily Check-in & AutoPay Alerts"
            val descriptionText = "Notifications for recurring bill reminders and end-of-day spend logging"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(CHANNEL_ID_REMINDERS, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        createNotificationChannels(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Alarm successfully scheduled for: ${calendar.time}")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm: ${e.message}", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (_: Exception) { }
        }
    }

    /**
     * Instantly triggers the receiver to post a notification immediately (bypasses 24-hour wait for testing).
     */
    fun triggerImmediateTestNotification(context: Context) {
        createNotificationChannels(context)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TEST_NOTIFICATION
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
