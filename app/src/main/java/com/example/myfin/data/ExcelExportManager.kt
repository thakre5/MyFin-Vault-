package com.example.myfin.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

object ExcelExportManager {

    private sealed class XlsxCell(val styleIndex: Int) {
        class Text(val value: String, styleIndex: Int = STYLE_TEXT) : XlsxCell(styleIndex)
        class Number(val value: Double, styleIndex: Int = STYLE_CURRENCY) : XlsxCell(styleIndex)
        class Integer(val value: Int, styleIndex: Int = STYLE_INTEGER) : XlsxCell(styleIndex)
    }

    private data class XlsxSheet(
        val name: String,
        val colWidths: List<Int>,
        val rows: List<List<XlsxCell>>
    )

    private const val STYLE_TEXT = 0
    private const val STYLE_HEADER = 1
    private const val STYLE_CURRENCY = 2
    private const val STYLE_INTEGER = 3
    private const val STYLE_BOLD_TEXT = 4

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

            // ============================================================
            // 1. EXECUTIVE RECONCILIATION & 3-VAULT AUDIT SUMMARY
            // ============================================================
            val activeAccounts = allAccounts.filter { !it.isArchived }
            val totalLiquid = activeAccounts.sumOf { it.currentBalance }

            val isCorporateOutlay = { tx: TransactionEntity ->
                (tx.type == TransactionType.CORPORATE && !tx.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
                (tx.type == TransactionType.EXPENSE && tx.category.equals("Work & Professional", ignoreCase = true))
            }
            val isCorporateInflow = { tx: TransactionEntity ->
                (tx.type == TransactionType.CORPORATE && tx.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
                (tx.type == TransactionType.INCOME && tx.category.equals("Reimbursements & Corporate Inflow", ignoreCase = true))
            }

            val totalWorkOutlays = allTransactions.filter(isCorporateOutlay).sumOf { it.amount } + userProfile.initialReimbursementClaim
            val totalClaimsSettled = allTransactions.filter(isCorporateInflow).sumOf { it.amount } + userProfile.initialCompanyAdvance
            val netPendingClaim = (totalWorkOutlays - totalClaimsSettled).coerceAtLeast(0.0)
            val excessAdvanceHeld = (totalClaimsSettled - totalWorkOutlays).coerceAtLeast(0.0)

            // Aggregate across all active Fortress accounts
            val fortressTotal = activeAccounts.filter {
                it.accountType.equals("Fortress", ignoreCase = true) ||
                it.accountName.contains("FORTRESS", ignoreCase = true) ||
                it.accountName.contains("TERTIARY", ignoreCase = true)
            }.sumOf { it.currentBalance }

            val sweepFloor = userProfile.fortressSweepThreshold
            val currentFdBalance = if (sweepFloor > 0.0) max(0.0, fortressTotal - sweepFloor) else 0.0
            val currentCushionBalance = if (sweepFloor > 0.0) min(fortressTotal, sweepFloor) else fortressTotal

            val personalExpensesList = allTransactions.filter {
                it.type == TransactionType.EXPENSE && !it.category.equals("Work & Professional", ignoreCase = true)
            }
            val monthlyBurnGroups = personalExpensesList.groupBy { "${it.year}-${it.month}" }.values.map { it.sumOf { tx -> tx.amount } }
            val avgMonthlyBurn = if (monthlyBurnGroups.isNotEmpty()) {
                monthlyBurnGroups.average()
            } else {
                max(userProfile.baseMonthlyIncome, 1000.0)
            }

            val emergencyMonths = if (userProfile.fortressEmergencyMonths > 0) userProfile.fortressEmergencyMonths else 6
            val emergencyTarget = if (userProfile.fortressManualTarget > 0.0) {
                userProfile.fortressManualTarget
            } else {
                avgMonthlyBurn * emergencyMonths
            }
            val fortressFundedPct = if (emergencyTarget > 0.0) ((currentFdBalance / emergencyTarget) * 100).toInt() else 0

            val summaryRows = mutableListOf<List<XlsxCell>>()
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Audit Category", STYLE_HEADER),
                    XlsxCell.Text("Metric", STYLE_HEADER),
                    XlsxCell.Text("Value ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Status / Notes", STYLE_HEADER)
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Liquidity", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Total Active Liquid Reserves"),
                    XlsxCell.Number(totalLiquid),
                    XlsxCell.Text("Across all active vault accounts")
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Corporate Float", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Cumulative Outlays Incurred"),
                    XlsxCell.Number(totalWorkOutlays),
                    XlsxCell.Text("Isolated reimbursable work spend (includes opening float)")
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Corporate Float", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Claims Settled by Employer"),
                    XlsxCell.Number(totalClaimsSettled),
                    XlsxCell.Text("Reimbursements received to date (includes opening float)")
                )
            )
            if (excessAdvanceHeld > 0.0) {
                summaryRows.add(
                    listOf(
                        XlsxCell.Text("Corporate Float", STYLE_BOLD_TEXT),
                        XlsxCell.Text("Company Advance Held"),
                        XlsxCell.Number(excessAdvanceHeld),
                        XlsxCell.Text("Ring-fenced unspent employer advance")
                    )
                )
            } else {
                summaryRows.add(
                    listOf(
                        XlsxCell.Text("Corporate Float", STYLE_BOLD_TEXT),
                        XlsxCell.Text("Pending Claims Receivable"),
                        XlsxCell.Number(netPendingClaim),
                        XlsxCell.Text("Outstanding balance due from employer")
                    )
                )
            }
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Fortress Engine", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Operating Savings Cap (Floor)"),
                    XlsxCell.Number(sweepFloor),
                    XlsxCell.Text("Liquid threshold maintained before auto-sweep FDs")
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Fortress Engine", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Liquid Savings Cushion Held"),
                    XlsxCell.Number(currentCushionBalance),
                    XlsxCell.Text("Available liquid reserve in Fortress")
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Fortress Engine", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Emergency FDs (Sweep Corpus)"),
                    XlsxCell.Number(currentFdBalance),
                    XlsxCell.Text("High-yield sweep FDs booked")
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Fortress Engine", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Emergency Safety Net Target"),
                    XlsxCell.Number(emergencyTarget),
                    XlsxCell.Text("Runway target based on $emergencyMonths months burn")
                )
            )
            summaryRows.add(
                listOf(
                    XlsxCell.Text("Fortress Engine", STYLE_BOLD_TEXT),
                    XlsxCell.Text("Safety Net Target Funding (%)"),
                    XlsxCell.Integer(fortressFundedPct),
                    XlsxCell.Text(if (fortressFundedPct >= 100) "Fully Funded (100%)" else "Accumulating Emergency Corpus")
                )
            )

            // ============================================================
            // 2. VAULT ACCOUNTS OVERVIEW SHEET
            // ============================================================
            val accountRows = mutableListOf<List<XlsxCell>>()
            accountRows.add(
                listOf(
                    XlsxCell.Text("Account Name", STYLE_HEADER),
                    XlsxCell.Text("Strategic Role", STYLE_HEADER),
                    XlsxCell.Text("Starting Balance ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Current Balance ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Minimum Balance Floor (MAB)", STYLE_HEADER),
                    XlsxCell.Text("Spendable Surplus ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Status", STYLE_HEADER)
                )
            )
            allAccounts.forEach { acc ->
                val status = if (acc.isArchived) "Archived" else if (acc.isMabBreached) "MAB Shortfall" else "Active"
                accountRows.add(
                    listOf(
                        XlsxCell.Text(sanitizeCell(acc.accountName), STYLE_BOLD_TEXT),
                        XlsxCell.Text(sanitizeCell(acc.accountType)),
                        XlsxCell.Number(acc.startingBalance),
                        XlsxCell.Number(acc.currentBalance),
                        XlsxCell.Number(acc.minBalance),
                        XlsxCell.Number(acc.spendableBalance),
                        XlsxCell.Text(status)
                    )
                )
            }

            // ============================================================
            // 3. AUTOPAY & FIXED COMMITMENTS SHEET
            // ============================================================
            val billRows = mutableListOf<List<XlsxCell>>()
            billRows.add(
                listOf(
                    XlsxCell.Text("Primary Commitment", STYLE_HEADER),
                    XlsxCell.Text("Category", STYLE_HEADER),
                    XlsxCell.Text("Subcategory", STYLE_HEADER),
                    XlsxCell.Text("Taxonomy Status", STYLE_HEADER),
                    XlsxCell.Text("Custom Note / Title", STYLE_HEADER),
                    XlsxCell.Text("Flow Type", STYLE_HEADER),
                    XlsxCell.Text("Planned Amount ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Source Vault", STYLE_HEADER),
                    XlsxCell.Text("Destination Vault", STYLE_HEADER),
                    XlsxCell.Text("Due Day", STYLE_HEADER),
                    XlsxCell.Text("Status", STYLE_HEADER),
                    XlsxCell.Text("Month", STYLE_HEADER),
                    XlsxCell.Text("Year", STYLE_HEADER)
                )
            )
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
                val taxonomyStatus = if (isLegacyCat) "Legacy (Retired)" else "Active"

                billRows.add(
                    listOf(
                        XlsxCell.Text(sanitizeCell(displayPrimary), STYLE_BOLD_TEXT),
                        XlsxCell.Text(sanitizeCell(bill.category)),
                        XlsxCell.Text(sanitizeCell(friendlySubcat)),
                        XlsxCell.Text(taxonomyStatus),
                        XlsxCell.Text(sanitizeCell(distinctNote)),
                        XlsxCell.Text(flowTypeLabel),
                        XlsxCell.Number(bill.amount),
                        XlsxCell.Text(sanitizeCell(bill.accountName)),
                        XlsxCell.Text(sanitizeCell(destVault)),
                        XlsxCell.Text(dueDayStr),
                        XlsxCell.Text(status),
                        XlsxCell.Integer(bill.month),
                        XlsxCell.Integer(bill.year)
                    )
                )
            }

            // ============================================================
            // 4. COMPLETE TRANSACTION LEDGER SHEET
            // ============================================================
            val txRows = mutableListOf<List<XlsxCell>>()
            txRows.add(
                listOf(
                    XlsxCell.Text("Timestamp", STYLE_HEADER),
                    XlsxCell.Text("Primary Entry", STYLE_HEADER),
                    XlsxCell.Text("Category", STYLE_HEADER),
                    XlsxCell.Text("Subcategory", STYLE_HEADER),
                    XlsxCell.Text("Taxonomy Status", STYLE_HEADER),
                    XlsxCell.Text("Note / Merchant", STYLE_HEADER),
                    XlsxCell.Text("Flow Type", STYLE_HEADER),
                    XlsxCell.Text("Cashflow Impact ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Raw Amount ($currencySymbol)", STYLE_HEADER),
                    XlsxCell.Text("Source Vault", STYLE_HEADER),
                    XlsxCell.Text("Destination Vault", STYLE_HEADER),
                    XlsxCell.Text("Transfer Classification", STYLE_HEADER),
                    XlsxCell.Text("Month", STYLE_HEADER),
                    XlsxCell.Text("Year", STYLE_HEADER)
                )
            )
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

                txRows.add(
                    listOf(
                        XlsxCell.Text(dateStr),
                        XlsxCell.Text(sanitizeCell(displayPrimary), STYLE_BOLD_TEXT),
                        XlsxCell.Text(sanitizeCell(tx.category)),
                        XlsxCell.Text(sanitizeCell(friendlySubcat)),
                        XlsxCell.Text(taxonomyStatus),
                        XlsxCell.Text(sanitizeCell(distinctNote)),
                        XlsxCell.Text(flowTypeLabel),
                        XlsxCell.Number(signedImpact),
                        XlsxCell.Number(tx.amount),
                        XlsxCell.Text(sanitizeCell(tx.accountName)),
                        XlsxCell.Text(sanitizeCell(destVault)),
                        XlsxCell.Text(subtypeLabel),
                        XlsxCell.Integer(tx.month),
                        XlsxCell.Integer(tx.year)
                    )
                )
            }

            val sheets = listOf(
                XlsxSheet("Executive Summary", listOf(22, 34, 20, 48), summaryRows),
                XlsxSheet("Vault Accounts", listOf(24, 18, 22, 22, 24, 22, 16), accountRows),
                XlsxSheet("AutoPay Commitments", listOf(28, 24, 24, 18, 26, 20, 20, 18, 18, 12, 14, 10, 10), billRows),
                XlsxSheet("Transaction Ledger", listOf(22, 28, 24, 24, 18, 28, 20, 20, 18, 18, 18, 20, 10, 10), txRows)
            )

            val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size))
                writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
                writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets))
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheets.size))
                writeZipEntry(zip, "xl/styles.xml", buildStylesXml())

                sheets.forEachIndexed { index, sheet ->
                    writeZipEntry(zip, "xl/worksheets/sheet${index + 1}.xml", buildWorksheetXml(sheet))
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zip.putNextEntry(entry)
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val sheetOverrides = (1..sheetCount).joinToString("\n") { i ->
            """  <Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$sheetOverrides
</Types>"""
    }

    private fun buildRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildWorkbookXml(sheets: List<XlsxSheet>): String {
        val sheetTags = sheets.mapIndexed { idx, s ->
            val sheetNum = idx + 1
            """    <sheet name="${escapeXml(s.name)}" sheetId="$sheetNum" r:id="rId$sheetNum"/>"""
        }.joinToString("\n")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
$sheetTags
  </sheets>
</workbook>"""
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val sheetRels = (1..sheetCount).joinToString("\n") { i ->
            """  <Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>"""
        }
        val stylesId = sheetCount + 1
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$sheetRels
  <Relationship Id="rId$stylesId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00;[Red]-#,##0.00"/>
  </numFmts>
  <fonts count="3">
    <font><sz val="10"/><color rgb="FF0F172A"/><name val="Calibri"/></font>
    <font><b/><sz val="10.5"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FF0F172A"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF2563EB"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFE2E8F0"/></left>
      <right style="thin"><color rgb="FFE2E8F0"/></right>
      <top style="thin"><color rgb="FFE2E8F0"/></top>
      <bottom style="thin"><color rgb="FFE2E8F0"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="5">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyFont="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right"/></xf>
    <xf numFmtId="1" fontId="0" fillId="0" borderId="1" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="1" applyFont="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>"""

    private fun buildWorksheetXml(sheet: XlsxSheet): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""").append('\n')
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""").append('\n')

        if (sheet.colWidths.isNotEmpty()) {
            sb.append("  <cols>\n")
            sheet.colWidths.forEachIndexed { idx, w ->
                val colIndex = idx + 1
                sb.append("""    <col min="$colIndex" max="$colIndex" width="$w" customWidth="1"/>""").append('\n')
            }
            sb.append("  </cols>\n")
        }

        sb.append("  <sheetData>\n")
        sheet.rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            val rowHeightAttr = if (rowNum == 1) """ ht="22" customHeight="1"""" else ""
            sb.append("""    <row r="$rowNum"$rowHeightAttr>""").append('\n')
            row.forEachIndexed { cIdx, cell ->
                val cellRef = "${columnLetter(cIdx)}$rowNum"
                when (cell) {
                    is XlsxCell.Text -> {
                        sb.append("""      <c r="$cellRef" s="${cell.styleIndex}" t="inlineStr"><is><t>${escapeXml(cell.value)}</t></is></c>""").append('\n')
                    }
                    is XlsxCell.Number -> {
                        val safeNum = if (cell.value.isNaN() || cell.value.isInfinite()) 0.0 else cell.value
                        sb.append("""      <c r="$cellRef" s="${cell.styleIndex}"><v>${String.format(Locale.US, "%.2f", safeNum)}</v></c>""").append('\n')
                    }
                    is XlsxCell.Integer -> {
                        sb.append("""      <c r="$cellRef" s="${cell.styleIndex}"><v>${cell.value}</v></c>""").append('\n')
                    }
                }
            }
            sb.append("    </row>\n")
        }
        sb.append("  </sheetData>\n")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun columnLetter(colIndex: Int): String {
        var idx = colIndex
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.insert(0, ('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.toString()
    }

    private fun sanitizeCell(value: String): String {
        val singleLine = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim()
        if (singleLine.startsWith("=") || singleLine.startsWith("+") || singleLine.startsWith("-") || singleLine.startsWith("@")) {
            return "'$singleLine"
        }
        return singleLine
    }

    private fun escapeXml(value: String): String {
        return buildString(value.length) {
            for (ch in value) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> if (ch.code >= 0x20 || ch == '\t') append(ch)
                }
            }
        }
    }
}
