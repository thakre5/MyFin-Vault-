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
                val isAnyReminderActive = profile.reminderEnabled || profile.isAutoPayReminderEnabled

                val isBoot = action == Intent.ACTION_BOOT_COMPLETED ||
                        action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                        action == "android.intent.action.QUICKBOOT_POWERON" ||
                        action == "com.htc.intent.action.QUICKBOOT_POWERON"

                if (isBoot) {
                    Log.d("ReminderReceiver", "System reboot detected. Rescheduling alarms...")
                    if (isAnyReminderActive) {
                        ReminderScheduler.scheduleDailyReminder(
                            context.applicationContext,
                            profile.reminderHour,
                            profile.reminderMinute
                        )
                    }
                    return@launch
                }

                val isTest = action == ReminderScheduler.ACTION_TEST_NOTIFICATION

                // Re-arm the next day's alarm cycle if either Daily Reminder or AutoPay Alerts is enabled
                if (isAnyReminderActive && !isTest) {
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

                val isActionableOutflowBill = { bill: FixedBillEntity ->
                    !bill.isPaid &&
                    bill.type != TransactionType.INCOME &&
                    !(bill.type == TransactionType.CORPORATE && bill.category.equals("Reimbursements & Claims", ignoreCase = true)) &&
                    bill.dueDay != null && bill.dueDay in 1..31
                }

                val fixedBills = dao.getFixedBillsForMonthDirect(currentMonth, currentYear)
                    .filter(isActionableOutflowBill)

                // Clamp dueDay to maxDayThisMonth so 30th/31st bills fire on the last day of shorter months
                val dueToday = fixedBills.filter { bill ->
                    val effectiveDueDay = minOf(bill.dueDay ?: 1, maxDayThisMonth)
                    effectiveDueDay == currentDay
                }

                // Check for bills due within 48h, handling end-of-month rollover to next month's 1st/2nd
                val dueWithin48h = if (profile.isAutoPayReminderEnabled) {
                    val withinCurrentMonth = fixedBills.filter { bill ->
                        val effectiveDueDay = minOf(bill.dueDay ?: 1, maxDayThisMonth)
                        effectiveDueDay in (currentDay + 1)..(currentDay + 2)
                    }
                    if (currentDay >= maxDayThisMonth - 1) {
                        val maxRolloverDay = if (currentDay == maxDayThisMonth) 2 else 1
                        val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                        val nextMonth = nextMonthCal.get(Calendar.MONTH) + 1
                        val nextYear = nextMonthCal.get(Calendar.YEAR)
                        val nextMonthBills = dao.getFixedBillsForMonthDirect(nextMonth, nextYear)
                            .filter(isActionableOutflowBill)
                            .filter { (it.dueDay ?: 0) in 1..maxRolloverDay }
                        withinCurrentMonth + nextMonthBills
                    } else {
                        withinCurrentMonth
                    }
                } else emptyList()

                // If Daily Review Reminder is OFF and only AutoPay Alerts are ON, skip generic daily check-in if no bills are due
                if (!isTest && !profile.reminderEnabled && dueToday.isEmpty() && dueWithin48h.isEmpty()) {
                    return@launch
                }

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
                            val dayOrd = formatOrdinalDay(bill.dueDay ?: 1)
                            "${formatBillDisplayName(bill)} ($currency$formattedAmt, Due $dayOrd)"
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

    private fun formatOrdinalDay(day: Int): String {
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }

    private fun formatBillDisplayName(bill: FixedBillEntity): String {
        val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
            when (bill.subcategory.trim()) {
                "WEALTH_ALLOCATION" -> "Fortress Sweep"
                "BILL_FUNDING" -> "Bill Funding"
                "REBALANCE" -> "Vault Rebalance"
                "CASH_WITHDRAWAL" -> "Cash ATM Withdrawal"
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
