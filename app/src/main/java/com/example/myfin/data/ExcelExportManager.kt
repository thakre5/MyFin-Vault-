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
                // Write UTF-8 Byte Order Mark (BOM) so Microsoft Excel renders formatting/currencies accurately
                writer.write("\uFEFF")

                // SECTION 1: VAULT ACCOUNTS OVERVIEW
                writer.write("=== VAULT ACCOUNTS & BALANCES ===\n")
                writer.write("Account Name,Type,Starting Balance ($currencySymbol),Current Balance ($currencySymbol),Minimum Balance (MAB),Status\n")
                allAccounts.forEach { acc ->
                    val status = if (acc.isArchived) "Archived" else "Active"
                    writer.write("\"${escapeCsv(acc.accountName)}\",\"${acc.accountType}\",${acc.startingBalance},${acc.currentBalance},${acc.minBalance},\"$status\"\n")
                }
                writer.write("\n\n")

                // SECTION 2: AUTOPAY & FIXED COMMITMENTS
                writer.write("=== RECURRING AUTOPAY COMMITMENTS ===\n")
                writer.write("Title,Flow Type,Category,Subcategory,Planned Amount ($currencySymbol),Deduction Vault,Due Day,Status,Month,Year\n")
                allFixedBills.forEach { bill ->
                    val status = if (bill.isPaid) "Settled" else "Pending"
                    val dueDayStr = bill.dueDay?.let { "Day $it" } ?: "Not Set"
                    writer.write(
                        "\"${escapeCsv(bill.title)}\",\"${bill.type.name}\",\"${escapeCsv(bill.category)}\",\"${escapeCsv(bill.subcategory)}\",${bill.amount},\"${escapeCsv(bill.accountName)}\",\"$dueDayStr\",\"$status\",${bill.month},${bill.year}\n"
                    )
                }
                writer.write("\n\n")

                // SECTION 3: TRANSACTION LEDGER
                writer.write("=== COMPLETE TRANSACTION LEDGER ===\n")
                writer.write("Timestamp,Title,Flow Type,Category,Subcategory,Amount ($currencySymbol),Source Vault,Destination Vault,Transfer Subtype,Month,Year\n")
                allTransactions.forEach { tx ->
                    val dateStr = dateFormat.format(Date(tx.date))
                    val dest = tx.toAccountName ?: ""
                    val subtype = if (tx.transferSubtype != TransferSubtype.NONE) tx.transferSubtype.name else ""
                    writer.write(
                        "\"$dateStr\",\"${escapeCsv(tx.title)}\",\"${tx.type.name}\",\"${escapeCsv(tx.category)}\",\"${escapeCsv(tx.subcategory)}\",${tx.amount},\"${escapeCsv(tx.accountName)}\",\"${escapeCsv(dest)}\",\"$subtype\",${tx.month},${tx.year}\n"
                    )
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
