package com.example.myfin.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myfin.MainActivity
import com.example.myfin.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val dao = database.budgetDao()
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val currentYear = cal.get(Calendar.YEAR)

            // Check if there are unpaid AutoPay bills due today or upcoming within 2 days
            val fixedBills = dao.getAllFixedBills().filter {
                it.month == currentMonth && it.year == currentYear && !it.isPaid && it.dueDay != null
            }
            val dueToday = fixedBills.filter { it.dueDay == currentDay }

            val contentText = when {
                dueToday.isNotEmpty() -> {
                    "⚠️ ${dueToday.size} AutoPay commitment(s) due today: ${dueToday.joinToString { it.title }}"
                }
                fixedBills.isNotEmpty() -> {
                    "You have ${fixedBills.size} pending recurring bills this month. Don't forget to record your daily spending."
                }
                else -> {
                    "Keep your vault accurate! Tap to log today's expenses and track your safe-to-spend balance."
                }
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("MyFin Daily Check-in")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(1001, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted on Android 13+
            }
        }
    }
}
