package com.example.myfin.data

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myfin.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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

                if (profile.reminderEnabled) {
                    ReminderScheduler.scheduleDailyReminder(
                        context,
                        profile.reminderHour,
                        profile.reminderMinute
                    )
                }

                ReminderScheduler.createNotificationChannels(context)

                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)

                val fixedBills = dao.getFixedBillsForMonthDirect(currentMonth, currentYear)
                    .filter { !it.isPaid && it.dueDay != null && it.dueDay in 1..31 }

                val dueToday = fixedBills.filter { it.dueDay == currentDay }
                val dueWithin48h = if (profile.isAutoPayReminderEnabled) {
                    fixedBills.filter { it.dueDay in (currentDay + 1)..(currentDay + 2) }
                } else emptyList()

                val currency = profile.currencySymbol

                val notificationTitle = when {
                    dueToday.isNotEmpty() -> "⚠️ AutoPay Due Today"
                    dueWithin48h.isNotEmpty() -> "Upcoming AutoPay Alert"
                    else -> "MyFin Daily Check-in"
                }

                val contentText = when {
                    dueToday.isNotEmpty() -> {
                        val names = dueToday.joinToString(", ") { bill ->
                            val formattedAmt = String.format(Locale.US, "%,.0f", bill.amount)
                            "${formatBillDisplayName(bill)} ($currency$formattedAmt)"
                        }
                        "AutoPay due today: $names. Verify deduction vault balance to avoid bounce."
                    }
                    dueWithin48h.isNotEmpty() -> {
                        val names = dueWithin48h.joinToString(", ") { bill ->
                            val formattedAmt = String.format(Locale.US, "%,.0f", bill.amount)
                            "${formatBillDisplayName(bill)} ($currency$formattedAmt, Due ${bill.dueDay}th)"
                        }
                        "Upcoming AutoPay in 48h: $names. Ensure funding vault is staged."
                    }
                    fixedBills.isNotEmpty() -> {
                        "You have ${fixedBills.size} pending fixed commitments this month. Log daily spends to maintain your safe-to-spend buffer."
                    }
                    else -> {
                        "Keep your vaults accurate! Tap to log today's transactions and maintain zero leakage."
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
                    .setContentTitle(notificationTitle)
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    try {
                        NotificationManagerCompat.from(context).notify(1001, builder.build())
                    } catch (_: SecurityException) { }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun formatBillDisplayName(bill: FixedBillEntity): String {
        val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
            when (bill.subcategory.trim()) {
                "WEALTH_ALLOCATION" -> "Fortress Sweep"
                "BILL_FUNDING" -> "Bill Funding"
                "REBALANCE" -> "Vault Rebalance"
                else -> bill.subcategory.trim().ifBlank { "Vault Sweep" }
            }
        } else {
            bill.subcategory.trim()
        }

        val cleanTitle = bill.title.trim()
        return when {
            cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> {
                friendlySubcat.ifBlank { cleanTitle.ifBlank { "AutoPay Commitment" } }
            }
            cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
                val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
            }
            friendlySubcat.isBlank() -> cleanTitle
            else -> "$friendlySubcat ($cleanTitle)"
        }
    }
}
