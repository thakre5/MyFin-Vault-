package com.example.myfin.data

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myfin.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val dao = database.budgetDao()
                val profile = dao.getUserProfileDirect() ?: UserProfile(id = 1)

                val isBoot = intent?.action == Intent.ACTION_BOOT_COMPLETED ||
                        intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED

                // 1. If device rebooted, restore scheduled alarms without showing instant notifications
                if (isBoot) {
                    if (profile.reminderEnabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            context,
                            profile.reminderHour,
                            profile.reminderMinute
                        )
                    }
                    return@launch
                }

                // 2. Exact alarms do not auto-repeat. Reschedule for tomorrow using saved profile settings
                if (profile.reminderEnabled) {
                    ReminderScheduler.scheduleDailyReminder(
                        context,
                        profile.reminderHour,
                        profile.reminderMinute
                    )
                }

                // 3. Make sure notification channel exists
                ReminderScheduler.createNotificationChannels(context)

                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)

                // 4. Targeted query for current month's fixed bills
                val fixedBills = dao.getFixedBillsForMonthDirect(currentMonth, currentYear)
                    .filter { !it.isPaid && it.dueDay != null }

                val dueToday = fixedBills.filter { it.dueDay == currentDay }
                val dueWithin48h = if (profile.isAutoPayReminderEnabled) {
                    fixedBills.filter { it.dueDay in (currentDay + 1)..(currentDay + 2) }
                } else emptyList()

                val currency = profile.currencySymbol

                val contentText = when {
                    dueToday.isNotEmpty() -> {
                        val names = dueToday.joinToString(", ") { "${it.title} ($currency${it.amount.toInt()})" }
                        "⚠️ AutoPay due today: $names. Verify Commitments vault balance."
                    }
                    dueWithin48h.isNotEmpty() -> {
                        val names = dueWithin48h.joinToString(", ") { "${it.title} (Day ${it.dueDay})" }
                        "Upcoming AutoPay bills in 48h: $names. Ensure funds are staged."
                    }
                    fixedBills.isNotEmpty() -> {
                        "You have ${fixedBills.size} pending fixed bills this month. Log daily spends to maintain your safe-to-spend buffer."
                    }
                    else -> {
                        "Keep your vault accurate! Tap to log today's transactions and maintain zero leakage."
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

                // 5. Post notification safely with permission check
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    try {
                        NotificationManagerCompat.from(context).notify(1001, builder.build())
                    } catch (_: SecurityException) { }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
