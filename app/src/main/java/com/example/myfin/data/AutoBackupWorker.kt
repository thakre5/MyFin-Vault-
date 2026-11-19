package com.example.myfin.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.budgetDao()
            val profile = dao.getUserProfileDirect() ?: return@withContext Result.success()

            if (!profile.autoBackupEnabled || profile.autoBackupTreeUri.isNullOrBlank()) {
                return@withContext Result.success()
            }

            val treeUri = Uri.parse(profile.autoBackupTreeUri)
            val rootDir = DocumentFile.fromTreeUri(applicationContext, treeUri)
                ?: return@withContext Result.failure()

            if (!rootDir.canWrite()) return@withContext Result.failure()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val fileName = "MyFin_AutoBackup_$timestamp.json"
            val backupFile = rootDir.createFile("application/json", fileName)
                ?: return@withContext Result.failure()

            val jsonPayload = buildFullBackupJson(dao, profile)
            applicationContext.contentResolver.openOutputStream(backupFile.uri)?.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return@withContext Result.failure()

            // Rolling retention: keep only the latest 5 auto-backups
            val existingAutoBackups = rootDir.listFiles()
                .filter { it.isFile && (it.name?.startsWith("MyFin_AutoBackup_") == true) }
                .sortedByDescending { it.lastModified() }

            existingAutoBackups.drop(5).forEach { oldFile ->
                runCatching { oldFile.delete() }
            }

            dao.updateLastAutoBackupTimestamp(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun buildFullBackupJson(dao: BudgetDao, currentProfile: UserProfile): String {
        val root = JSONObject()
        val allTxs = dao.getAllTransactions()
        val allCats = dao.getAllCategoriesDirect()
        val allSubcats = dao.getAllSubcategoriesDirect()
        val allAccounts = dao.getAllAccountsDirect()
        val allFixedBills = dao.getAllFixedBills()
        val allBudgetPlans = dao.getAllBudgetPlans()
        val allNotes = dao.getAllNotesDirect()
        val allInvestments = dao.getAllInvestmentsDirect()
        val allGoals = dao.getAllGoalsDirect()

        root.put("userProfile", JSONObject().apply {
            put("displayName", currentProfile.displayName)
            put("email", currentProfile.email)
            put("dateOfBirth", currentProfile.dateOfBirth)
            put("baseMonthlyIncome", currentProfile.baseMonthlyIncome)
            put("currencySymbol", currentProfile.currencySymbol)
            put("vaultMode", currentProfile.vaultMode)
            put("fortressSweepThreshold", currentProfile.fortressSweepThreshold)
            put("fortressEmergencyMonths", currentProfile.fortressEmergencyMonths)
            put("fortressManualTarget", currentProfile.fortressManualTarget)
            put("initialReimbursementClaim", currentProfile.initialReimbursementClaim)
            put("initialCompanyAdvance", currentProfile.initialCompanyAdvance)
            put("isOnboardingCompleted", currentProfile.isOnboardingCompleted)
        })

        root.put("transactions", JSONArray().apply {
            allTxs.forEach { tx ->
                put(JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("subcategory", tx.subcategory)
                    put("accountName", tx.accountName)
                    put("toAccountName", tx.toAccountName ?: "")
                    put("type", tx.type.name)
                    put("date", tx.date)
                    put("month", tx.month)
                    put("year", tx.year)
                    put("linkedFixedBillId", tx.linkedFixedBillId ?: JSONObject.NULL)
                    put("transferSubtype", tx.transferSubtype.name)
                    put("isSettled", tx.isSettled)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            allCats.forEach { c ->
                put(JSONObject().apply {
                    put("name", c.name); put("type", c.type.name)
                    put("isLegacy", c.isLegacy); put("isNew", c.isNew)
                })
            }
        })

        root.put("subcategories", JSONArray().apply {
            allSubcats.forEach { s ->
                put(JSONObject().apply {
                    put("parentCategory", s.parentCategory); put("name", s.name)
                    put("type", s.type.name); put("isLegacy", s.isLegacy); put("isNew", s.isNew)
                })
            }
        })

        root.put("accounts", JSONArray().apply {
            allAccounts.forEach { a ->
                put(JSONObject().apply {
                    put("accountName", a.accountName); put("startingBalance", a.startingBalance)
                    put("accountType", a.accountType); put("minBalance", a.minBalance)
                    put("isArchived", a.isArchived); put("sortOrder", a.sortOrder)
                })
            }
        })

        root.put("fixedBills", JSONArray().apply {
            allFixedBills.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id); put("title", b.title); put("amount", b.amount)
                    put("category", b.category); put("subcategory", b.subcategory)
                    put("accountName", b.accountName); put("toAccountName", b.toAccountName ?: "")
                    put("type", b.type.name); put("isPaid", b.isPaid)
                    put("dueDay", b.dueDay ?: JSONObject.NULL)
                    put("month", b.month); put("year", b.year)
                })
            }
        })

        root.put("budgetPlans", JSONArray().apply {
            allBudgetPlans.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("category", p.category); put("plannedAmount", p.plannedAmount)
                    put("type", p.type.name); put("month", p.month); put("year", p.year)
                })
            }
        })

        root.put("notes", JSONArray().apply {
            allNotes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id); put("title", n.title); put("content", n.content)
                    put("tag", n.tag); put("isPinned", n.isPinned)
                    put("isChecklist", n.isChecklist); put("checklistJson", n.checklistJson)
                    put("updatedAt", n.updatedAt)
                })
            }
        })

        root.put("investments", JSONArray().apply {
            allInvestments.forEach { inv ->
                put(JSONObject().apply {
                    put("id", inv.id); put("name", inv.name); put("assetClass", inv.assetClass)
                    put("openingInvestedAmount", inv.openingInvestedAmount)
                    put("currentValuation", inv.currentValuation)
                    put("linkedSubcategory", inv.linkedSubcategory ?: JSONObject.NULL)
                    put("institution", inv.institution); put("notes", inv.notes)
                    put("isArchived", inv.isArchived); put("updatedAt", inv.updatedAt)
                })
            }
        })

        root.put("goals", JSONArray().apply {
            allGoals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id); put("title", g.title); put("targetAmount", g.targetAmount)
                    put("initialSavedAmount", g.initialSavedAmount)
                    put("targetDateMillis", g.targetDateMillis)
                    put("linkedAccountName", g.linkedAccountName ?: JSONObject.NULL)
                    put("linkedInvestmentId", g.linkedInvestmentId ?: JSONObject.NULL)
                    put("iconName", g.iconName); put("isCompleted", g.isCompleted)
                    put("createdAt", g.createdAt)
                })
            }
        })

        return root.toString(2)
    }

    companion object {
        private const val WORK_NAME = "MyFinWeeklyAutoBackup"

        fun scheduleWeekly(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
