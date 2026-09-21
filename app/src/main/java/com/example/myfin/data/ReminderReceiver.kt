package com.example.myfin.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myfin.MainActivity
import com.example.myfin.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val action = intent?.action
        Log.d("ReminderReceiver", "Broadcast received with action: $action")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context.applicationContext)
                val dao = database.budgetDao()
                val profile = dao.getUserProfileDirect() ?: UserProfile(id = 1)

                val isBoot = action == Intent.ACTION_BOOT_COMPLETED ||
                        action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                        action == "android.intent.action.QUICKBOOT_POWERON" ||
                        action == "com.htc.intent.action.QUICKBOOT_POWERON"

                if (isBoot) {
                    Log.d("ReminderReceiver", "System reboot detected. Rescheduling alarms...")
                    if (profile.reminderEnabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            context.applicationContext,
                            profile.reminderHour,
                            profile.reminderMinute
                        )
                    }
                    return@launch
                }

                val isTest = action == ReminderScheduler.ACTION_TEST_NOTIFICATION

                // Re-arm the next day's alarm cycle
                if (profile.reminderEnabled && !isTest) {
                    ReminderScheduler.scheduleDailyReminder(
                        context.applicationContext,
                        profile.reminderHour,
                        profile.reminderMinute
                    )
                }

                ReminderScheduler.createNotificationChannels(context.applicationContext)

                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)
                val maxDayThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val fixedBills = dao.getFixedBillsForMonthDirect(currentMonth, currentYear)
                    .filter { !it.isPaid && 
                        it.type != TransactionType.INCOME && 
                        !(it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true)) &&
                        it.dueDay != null && it.dueDay in 1..31 
                    }

                val dueToday = fixedBills.filter { it.dueDay == currentDay }

                // Check for bills due within 48h, handling end-of-month rollover to next month's 1st/2nd
                val dueWithin48h = if (profile.isAutoPayReminderEnabled) {
                    val withinCurrentMonth = fixedBills.filter { it.dueDay in (currentDay + 1)..(currentDay + 2) }
                    if (currentDay >= maxDayThisMonth - 1) {
                        val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                        val nextMonth = nextMonthCal.get(Calendar.MONTH) + 1
                        val nextYear = nextMonthCal.get(Calendar.YEAR)
                        val nextMonthBills = dao.getFixedBillsForMonthDirect(nextMonth, nextYear)
                            .filter { !it.isPaid && it.dueDay != null && it.dueDay in 1..2 }
                        withinCurrentMonth + nextMonthBills
                    } else {
                        withinCurrentMonth
                    }
                } else emptyList()

                val currency = profile.currencySymbol

                val notificationTitle = when {
                    isTest -> "🔔 MyFin Notification Test"
                    dueToday.isNotEmpty() -> "⚠️ AutoPay Due Today"
                    dueWithin48h.isNotEmpty() -> "Upcoming AutoPay Alert"
                    else -> "MyFin Daily Check-in"
                }

                val contentText = when {
                    isTest -> "Notifications and alarm dispatchers are working properly!"
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

                val launchIntent = Intent(context.applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context.applicationContext,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val smallIconRes = try {
                    R.drawable.ic_launcher_foreground
                } catch (_: Exception) {
                    android.R.drawable.ic_dialog_info
                }

                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val builder = NotificationCompat.Builder(context.applicationContext, ReminderScheduler.CHANNEL_ID_REMINDERS)
                    .setSmallIcon(smallIconRes)
                    .setContentTitle(notificationTitle)
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val hasPermission = ContextCompat.checkSelfPermission(
                    context.applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    try {
                        NotificationManagerCompat.from(context.applicationContext).notify(1001, builder.build())
                        Log.d("ReminderReceiver", "Notification posted successfully ID 1001")
                    } catch (e: Exception) {
                        Log.e("ReminderReceiver", "Failed to post notification: ${e.message}", e)
                    }
                } else {
                    Log.w("ReminderReceiver", "Notification NOT posted: POST_NOTIFICATIONS permission not granted.")
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
