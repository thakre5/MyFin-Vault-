package com.example.myfin.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object ExcelExportManager {

    suspend fun exportToUri(context: Context, uri: Uri, currencySymbol: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val dao = database.budgetDao()

            val allTransactions = dao.getAllTransactions()
            val allAccounts = dao.getAccountBalances().first()
            val allFixedBills = dao.getAllFixedBills()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false

            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                // UTF-8 Byte Order Mark (BOM) so Excel renders currency symbols correctly
                writer.write("\uFEFF")

                // ============================================================
                // SECTION 1: VAULT ACCOUNTS OVERVIEW
                // ============================================================
                writer.write("=== VAULT ACCOUNTS & BALANCES ===\n")
                writer.write("Account Name,Type,Starting Balance ($currencySymbol),Current Balance ($currencySymbol),Minimum Balance (MAB),Status\n")
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
                writer.write("Primary Commitment,Category,Subcategory,Custom Note / Title,Flow Type,Planned Amount ($currencySymbol),Source Vault,Destination Vault,Due Day,Status,Month,Year\n")
                allFixedBills.forEach { bill ->
                    val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
                        when (bill.subcategory.trim()) {
                            "WEALTH_ALLOCATION" -> "Fortress Sweep"
                            "BILL_FUNDING" -> "Bill Funding"
                            "REBALANCE" -> "Vault Rebalance"
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

                    val status = if (bill.isPaid) "Settled" else "Pending"
                    val dueDayStr = bill.dueDay?.let { "Day $it" } ?: "Not Set"
                    val destVault = bill.toAccountName ?: ""
                    val formattedAmt = String.format(Locale.US, "%.2f", bill.amount)

                    writer.write(
                        "\"${sanitizeCsv(displayPrimary)}\",\"${sanitizeCsv(bill.category)}\",\"${sanitizeCsv(friendlySubcat)}\",\"${sanitizeCsv(distinctNote)}\",\"${bill.type.name}\",$formattedAmt,\"${sanitizeCsv(bill.accountName)}\",\"${sanitizeCsv(destVault)}\",\"$dueDayStr\",\"$status\",${bill.month},${bill.year}\n"
                    )
                }
                writer.write("\n\n")

                // ============================================================
                // SECTION 3: TRANSACTION LEDGER
                // ============================================================
                writer.write("=== COMPLETE TRANSACTION LEDGER ===\n")
                writer.write("Timestamp,Primary Entry,Category,Subcategory,Note / Merchant,Flow Type,Amount ($currencySymbol),Source Vault,Destination Vault,Transfer Classification,Month,Year\n")
                allTransactions.forEach { tx ->
                    val friendlySubcat = if (tx.type == TransactionType.TRANSFER) {
                        when (tx.subcategory.trim()) {
                            "WEALTH_ALLOCATION" -> "Fortress Sweep"
                            "BILL_FUNDING" -> "Bill Funding"
                            "REBALANCE" -> "Vault Rebalance"
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

                    val dateStr = dateFormat.format(Date(tx.date))
                    val destVault = tx.toAccountName ?: ""
                    val subtypeLabel = when (tx.transferSubtype) {
                        TransferSubtype.BILL_FUNDING -> "Bill Funding"
                        TransferSubtype.WEALTH_ALLOCATION -> "Fortress Sweep"
                        TransferSubtype.REBALANCE -> "Vault Rebalance"
                        TransferSubtype.CASH_WITHDRAWAL -> "Cash ATM Withdrawal"
                        TransferSubtype.NONE -> ""
                    }
                    val formattedAmt = String.format(Locale.US, "%.2f", tx.amount)

                    writer.write(
                        "\"$dateStr\",\"${sanitizeCsv(displayPrimary)}\",\"${sanitizeCsv(tx.category)}\",\"${sanitizeCsv(friendlySubcat)}\",\"${sanitizeCsv(distinctNote)}\",\"${tx.type.name}\",$formattedAmt,\"${sanitizeCsv(tx.accountName)}\",\"${sanitizeCsv(destVault)}\",\"$subtypeLabel\",${tx.month},${tx.year}\n"
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
        // Neutralize formula injection in Excel and Google Sheets
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@")) {
            clean = "\t$clean"
        }
        return clean
    }
}
