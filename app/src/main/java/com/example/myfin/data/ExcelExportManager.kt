package com.example.myfin.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ExcelExportManager {

    suspend fun exportToUri(context: Context, uri: Uri, currencySymbol: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val dao = database.budgetDao()

            val allTransactions = dao.getAllTransactions()
            val allAccounts = dao.getAccountBalances().first()
            val allFixedBills = dao.getAllFixedBills()
            val userProfile = dao.getUserProfileDirect() ?: UserProfile(id = 1)
            val masterCategories = dao.getAllCategoriesDirect().associateBy { "${it.type.name}_${it.name.trim().lowercase()}" }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false

            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                // UTF-8 Byte Order Mark (BOM) so Excel renders currency symbols correctly
                writer.write("\uFEFF")

                // ============================================================
                // EXECUTIVE RECONCILIATION & 3-VAULT AUDIT SUMMARY
                // ============================================================
                val totalLiquid = allAccounts.filter { !it.isArchived }.sumOf { it.currentBalance }

                // Corporate float calculation supporting active and legacy classifications
                val isCorporateOutlay = { tx: TransactionEntity ->
                    (tx.type == TransactionType.CORPORATE && !tx.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
                    (tx.type == TransactionType.EXPENSE && tx.category.equals("Work & Professional", ignoreCase = true))
                }
                val isCorporateInflow = { tx: TransactionEntity ->
                    (tx.type == TransactionType.CORPORATE && tx.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
                    (tx.type == TransactionType.INCOME && tx.category.equals("Reimbursements & Corporate Inflow", ignoreCase = true))
                }

                val totalWorkOutlays = allTransactions.filter(isCorporateOutlay).sumOf { it.amount }
                val totalClaimsSettled = allTransactions.filter(isCorporateInflow).sumOf { it.amount }
                val netPendingClaim = (totalWorkOutlays - totalClaimsSettled).coerceAtLeast(0.0)
                val excessAdvanceHeld = (totalClaimsSettled - totalWorkOutlays).coerceAtLeast(0.0)

                // Decoupled Fortress calculations
                val fortressAcc = allAccounts.find {
                    it.accountType.equals("Fortress", ignoreCase = true) ||
                    it.accountName.contains("FORTRESS", ignoreCase = true) ||
                    it.accountName.contains("TERTIARY", ignoreCase = true)
                }
                val fortressTotal = fortressAcc?.currentBalance ?: 0.0
                val sweepFloor = userProfile.fortressSweepThreshold
                val currentFdBalance = if (sweepFloor > 0.0) max(0.0, fortressTotal - sweepFloor) else 0.0
                val currentCushionBalance = if (sweepFloor > 0.0) min(fortressTotal, sweepFloor) else fortressTotal

                val personalExpensesList = allTransactions.filter {
                    it.type == TransactionType.EXPENSE && !it.category.equals("Work & Professional", ignoreCase = true)
                }
                val monthlyBurnGroups = personalExpensesList.groupBy { "${it.year}-${it.month}" }.values.map { it.sumOf { tx -> tx.amount } }
                val avgMonthlyBurn = if (monthlyBurnGroups.isNotEmpty()) monthlyBurnGroups.average() else 0.0
                val emergencyMonths = if (userProfile.fortressEmergencyMonths > 0) userProfile.fortressEmergencyMonths else 6
                val emergencyTarget = if (userProfile.fortressManualTarget > 0.0) {
                    userProfile.fortressManualTarget
                } else {
                    avgMonthlyBurn * emergencyMonths
                }
                val fortressFundedPct = if (emergencyTarget > 0.0) ((currentFdBalance / emergencyTarget) * 100).toInt() else 0

                writer.write("=== EXECUTIVE FINANCIAL AUDIT SUMMARY ===\n")
                writer.write("Audit Category,Metric,Value ($currencySymbol),Status / Notes\n")
                writer.write("\"Liquidity\",\"Total Active Liquid Reserves\",${String.format(Locale.US, "%.2f", totalLiquid)},\"Across all active vault accounts\"\n")
                writer.write("\"Corporate Float\",\"Cumulative Outlays Incurred\",${String.format(Locale.US, "%.2f", totalWorkOutlays)},\"Isolated reimbursable work spend\"\n")
                writer.write("\"Corporate Float\",\"Claims Settled by Employer\",${String.format(Locale.US, "%.2f", totalClaimsSettled)},\"Reimbursements received to date\"\n")
                if (excessAdvanceHeld > 0.0) {
                    writer.write("\"Corporate Float\",\"Company Advance Held\",${String.format(Locale.US, "%.2f", excessAdvanceHeld)},\"Ring-fenced unspent employer advance\"\n")
                } else {
                    writer.write("\"Corporate Float\",\"Pending Claims Receivable\",${String.format(Locale.US, "%.2f", netPendingClaim)},\"Outstanding balance due from employer\"\n")
                }

                writer.write("\"Fortress Engine\",\"Operating Savings Cap (Floor)\",${String.format(Locale.US, "%.2f", sweepFloor)},\"Liquid threshold maintained before auto-sweep FDs\"\n")
                writer.write("\"Fortress Engine\",\"Liquid Savings Cushion Held\",${String.format(Locale.US, "%.2f", currentCushionBalance)},\"Available liquid reserve in Fortress\"\n")
                writer.write("\"Fortress Engine\",\"Emergency FDs (Sweep Corpus)\",${String.format(Locale.US, "%.2f", currentFdBalance)},\"High-yield sweep FDs booked\"\n")
                writer.write("\"Fortress Engine\",\"Emergency Safety Net Target\",${String.format(Locale.US, "%.2f", emergencyTarget)},\"Runway target based on $emergencyMonths months burn\"\n")
                writer.write("\"Fortress Engine\",\"Safety Net Target Funding\",${fortressFundedPct}%,${if (fortressFundedPct >= 100) "\"Fully Funded (100%)\"" else "\"Accumulating Emergency Corpus\""}\n")
                writer.write("\n\n")

                // ============================================================
                // SECTION 1: VAULT ACCOUNTS OVERVIEW
                // ============================================================
                writer.write("=== VAULT ACCOUNTS & BALANCES ===\n")
                writer.write("Account Name,Strategic Role,Starting Balance ($currencySymbol),Current Balance ($currencySymbol),Minimum Balance Floor (MAB),Status\n")
                allAccounts.forEach { acc ->
                    val status = if (acc.isArchived) "Archived" else "Active"
                    val startBal = String.format(Locale.US, "%.2f", acc.startingBalance)
                    val curBal = String.format(Locale.US, "%.2f", acc.currentBalance)
                    val minBal = String.format(Locale.US, "%.2f", acc.minBalance)
                    writer.write("\"${sanitizeCsv(acc.accountName)}\",\"${sanitizeCsv(acc.accountType)}\",$startBal,$curBal,$minBal,\"$status\"\n")
                }
                writer.write("\n\n")

                // ============================================================
                // SECTION 2: AUTOPAY & FIXED COMMITMENTS
                // ============================================================
                writer.write("=== RECURRING AUTOPAY COMMITMENTS ===\n")
                writer.write("Primary Commitment,Category,Subcategory,Taxonomy Status,Custom Note / Title,Flow Type,Planned Amount ($currencySymbol),Source Vault,Destination Vault,Due Day,Status,Month,Year\n")
                allFixedBills.forEach { bill ->
                    val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
                        when (bill.subcategory.trim()) {
                            "WEALTH_ALLOCATION" -> "Fortress Sweep"
                            "BILL_FUNDING" -> "Bill Funding"
                            "REBALANCE" -> "Vault Rebalance"
                            "CASH_WITHDRAWAL" -> "Cash ATM Withdrawal"
                            else -> bill.subcategory.trim().ifBlank { "Vault Sweep" }
                        }
                    } else bill.subcategory.trim()

                    val cleanTitle = bill.title.trim()
                    val displayPrimary = when {
                        cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> friendlySubcat
                        cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
                            val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                            if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
                        }
                        friendlySubcat.isBlank() -> cleanTitle
                        else -> "$friendlySubcat ($cleanTitle)"
                    }

                    val distinctNote = when {
                        cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> ""
                        cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                        else -> cleanTitle
                    }

                    val isLegacyCat = masterCategories["${bill.type.name}_${bill.category.trim().lowercase()}"]?.isLegacy == true

                    val flowTypeLabel = when (bill.type) {
                        TransactionType.CORPORATE -> {
                            if (bill.category.equals("Reimbursements & Claims", ignoreCase = true)) "CORPORATE (INFLOW)" else "CORPORATE (OUTFLOW)"
                        }
                        else -> bill.type.name
                    }

                    val status = if (bill.isPaid) "Settled" else "Pending"
                    val dueDayStr = bill.dueDay?.let { "Day $it" } ?: "Not Set"
                    val destVault = bill.toAccountName ?: ""
                    val formattedAmt = String.format(Locale.US, "%.2f", bill.amount)
                    val taxonomyStatus = if (isLegacyCat) "Legacy (Retired)" else "Active"

                    writer.write(
                        "\"${sanitizeCsv(displayPrimary)}\",\"${sanitizeCsv(bill.category)}\",\"${sanitizeCsv(friendlySubcat)}\",\"$taxonomyStatus\",\"${sanitizeCsv(distinctNote)}\",\"$flowTypeLabel\",$formattedAmt,\"${sanitizeCsv(bill.accountName)}\",\"${sanitizeCsv(destVault)}\",\"$dueDayStr\",\"$status\",${bill.month},${bill.year}\n"
                    )
                }
                writer.write("\n\n")

                // ============================================================
                // SECTION 3: TRANSACTION LEDGER
                // ============================================================
                writer.write("=== COMPLETE TRANSACTION LEDGER ===\n")
                writer.write("Timestamp,Primary Entry,Category,Subcategory,Taxonomy Status,Note / Merchant,Flow Type,Cashflow Impact ($currencySymbol),Raw Amount ($currencySymbol),Source Vault,Destination Vault,Transfer Classification,Month,Year\n")
                allTransactions.forEach { tx ->
                    val friendlySubcat = if (tx.type == TransactionType.TRANSFER) {
                        when (tx.subcategory.trim()) {
                            "WEALTH_ALLOCATION" -> "Fortress Sweep"
                            "BILL_FUNDING" -> "Bill Funding"
                            "REBALANCE" -> "Vault Rebalance"
                            "CASH_WITHDRAWAL" -> "Cash ATM Withdrawal"
                            else -> tx.subcategory.trim().ifBlank { "Vault Sweep" }
                        }
                    } else tx.subcategory.trim()

                    val cleanTitle = tx.title.trim()
                    val displayPrimary = when {
                        cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> friendlySubcat
                        cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
                            val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                            if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
                        }
                        friendlySubcat.isBlank() -> cleanTitle
                        else -> "$friendlySubcat ($cleanTitle)"
                    }

                    val distinctNote = when {
                        cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> ""
                        cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                        else -> cleanTitle
                    }

                    val isCorporateInflowTx = (tx.type == TransactionType.CORPORATE && tx.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
                            (tx.type == TransactionType.INCOME && tx.category.equals("Reimbursements & Corporate Inflow", ignoreCase = true))

                    val isCorporateOutflowTx = (tx.type == TransactionType.CORPORATE && !tx.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
                            (tx.type == TransactionType.EXPENSE && tx.category.equals("Work & Professional", ignoreCase = true))

                    val flowTypeLabel = when {
                        isCorporateInflowTx -> "CORPORATE (INFLOW)"
                        isCorporateOutflowTx -> "CORPORATE (OUTFLOW)"
                        else -> tx.type.name
                    }

                    val signedImpact = when {
                        isCorporateInflowTx -> tx.amount
                        isCorporateOutflowTx -> -tx.amount
                        tx.type == TransactionType.EXPENSE || tx.type == TransactionType.ASSET -> -tx.amount
                        tx.type == TransactionType.INCOME -> tx.amount
                        tx.type == TransactionType.TRANSFER -> 0.0
                        else -> 0.0
                    }

                    val isLegacyCat = masterCategories["${tx.type.name}_${tx.category.trim().lowercase()}"]?.isLegacy == true
                    val taxonomyStatus = if (isLegacyCat) "Legacy (Retired)" else "Active"

                    val dateStr = dateFormat.format(Date(tx.date))
                    val destVault = tx.toAccountName ?: ""
                    val subtypeLabel = when (tx.transferSubtype) {
                        TransferSubtype.BILL_FUNDING -> "Bill Funding"
                        TransferSubtype.WEALTH_ALLOCATION -> "Fortress Sweep"
                        TransferSubtype.REBALANCE -> "Vault Rebalance"
                        TransferSubtype.CASH_WITHDRAWAL -> "Cash ATM Withdrawal"
                        TransferSubtype.NONE -> ""
                    }

                    val formattedSignedImpact = String.format(Locale.US, "%.2f", signedImpact)
                    val formattedRawAmt = String.format(Locale.US, "%.2f", tx.amount)

                    writer.write(
                        "\"$dateStr\",\"${sanitizeCsv(displayPrimary)}\",\"${sanitizeCsv(tx.category)}\",\"${sanitizeCsv(friendlySubcat)}\",\"$taxonomyStatus\",\"${sanitizeCsv(distinctNote)}\",\"$flowTypeLabel\",$formattedSignedImpact,$formattedRawAmt,\"${sanitizeCsv(tx.accountName)}\",\"${sanitizeCsv(destVault)}\",\"$subtypeLabel\",${tx.month},${tx.year}\n"
                    )
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeCsv(value: String): String {
        var clean = value.replace("\"", "\"\"")
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@")) {
            clean = "\t$clean"
        }
        return clean
    }
}
