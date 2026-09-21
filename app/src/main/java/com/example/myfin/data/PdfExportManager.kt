package com.example.myfin.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object PdfExportManager {

    fun exportToUri(
        context: Context,
        uri: Uri,
        currencySymbol: String,
        timeRangeLabel: String,
        totalIncome: Double,
        totalExpenses: Double,
        netSurplus: Double,
        safeToSpend: Double
    ): Boolean {
        val pdfDocument = PdfDocument()
        return try {
            // Standard A4 dimensions: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Colors
            val colorPrimary = Color.parseColor("#6C5CE7")      // AccentPurple
            val colorPrimaryDark = Color.parseColor("#4D38C9")
            val colorTextDark = Color.parseColor("#0F172A")     // Slate 900
            val colorTextMuted = Color.parseColor("#64748B")    // Slate 500
            val colorBorder = Color.parseColor("#E2E8F0")       // Slate 200
            val colorCardBg = Color.parseColor("#F8FAFC")       // Slate 50
            val colorGreen = Color.parseColor("#10B981")        // Emerald 500
            val colorRed = Color.parseColor("#EF4444")          // Rose 500
            val colorPurpleTint = Color.parseColor("#F5F3FF")   // Purple 50

            // Common Paints
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = colorBorder
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val marginStart = 42f
            val marginEnd = 553f
            val contentWidth = marginEnd - marginStart // 511 pt

            // ================================================================
            // 1. TOP BRAND ACCENT BAR
            // ================================================================
            fillPaint.color = colorPrimary
            canvas.drawRect(0f, 0f, 595f, 6f, fillPaint)

            // ================================================================
            // 2. HEADER: BRAND & DOCUMENT METADATA
            // ================================================================
            var y = 46f

            // Logo Badge ("MF")
            val logoRect = RectF(marginStart, y, marginStart + 36f, y + 36f)
            fillPaint.color = colorPrimary
            canvas.drawRoundRect(logoRect, 8f, 8f, fillPaint)

            textPaint.color = Color.WHITE
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 15f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("MF", logoRect.centerX(), logoRect.centerY() + 5.5f, textPaint)

            // Brand Titles
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = colorTextDark
            textPaint.textSize = 18f
            canvas.drawText("MyFin", marginStart + 46f, y + 16f, textPaint)

            fillPaint.color = colorPurpleTint
            val vaultBadge = RectF(marginStart + 104f, y + 3f, marginStart + 158f, y + 19f)
            canvas.drawRoundRect(vaultBadge, 4f, 4f, fillPaint)

            textPaint.textSize = 8.5f
            textPaint.color = colorPrimary
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            canvas.drawText("VAULT", vaultBadge.left + 8f, vaultBadge.centerY() + 3f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.color = colorTextMuted
            canvas.drawText("OFFLINE 3-TIER WEALTH LEDGER", marginStart + 46f, y + 30f, textPaint)

            // Right-aligned Document Label & Date
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 14f
            textPaint.color = colorPrimaryDark
            canvas.drawText("FINANCIAL STATEMENT", marginEnd, y + 14f, textPaint)

            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8.5f
            textPaint.color = colorTextMuted
            canvas.drawText("Period: $timeRangeLabel  •  Generated: $dateStr", marginEnd, y + 28f, textPaint)

            // Divider Line
            y += 48f
            strokePaint.color = colorBorder
            canvas.drawLine(marginStart, y, marginEnd, y, strokePaint)

            // ================================================================
            // 3. EXECUTIVE KPI CARDS (2 x 2 GRID)
            // ================================================================
            y += 18f
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11.5f
            textPaint.color = colorTextDark
            canvas.drawText("EXECUTIVE AUDIT SUMMARY", marginStart, y, textPaint)

            y += 12f
            val cardGap = 11f
            val cardWidth = (contentWidth - cardGap) / 2f
            val cardHeight = 64f

            val cards = listOf(
                Triple("TOTAL PERSONAL INFLOW", totalIncome, colorGreen),
                Triple("LIFESTYLE EXPENSES", totalExpenses, colorRed),
                Triple("NET CAPITAL RETAINED", netSurplus, colorPrimary),
                Triple("SAFE-TO-SPEND RUNWAY", safeToSpend, colorPrimaryDark)
            )

            cards.forEachIndexed { index, (label, value, accentColor) ->
                val col = index % 2
                val row = index / 2
                val cX = marginStart + (col * (cardWidth + cardGap))
                val cY = y + (row * (cardHeight + cardGap))
                val cardRect = RectF(cX, cY, cX + cardWidth, cY + cardHeight)

                // Background & border
                fillPaint.color = colorCardBg
                canvas.drawRoundRect(cardRect, 10f, 10f, fillPaint)
                strokePaint.color = colorBorder
                canvas.drawRoundRect(cardRect, 10f, 10f, strokePaint)

                // Indicator pip
                fillPaint.color = accentColor
                canvas.drawCircle(cX + 14f, cY + 18f, 3.5f, fillPaint)

                // Card Title
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 8.5f
                textPaint.color = colorTextMuted
                canvas.drawText(label, cX + 24f, cY + 21f, textPaint)

                // Card Value
                textPaint.textSize = 15f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = colorTextDark
                val formattedVal = "$currencySymbol${String.format(Locale.US, "%,.2f", value)}"
                canvas.drawText(formattedVal, cX + 14f, cY + 46f, textPaint)
            }

            // ================================================================
            // 4. CASH FLOW HEALTH & SAVINGS RETENTION RATE
            // ================================================================
            y += (cardHeight * 2) + cardGap + 20f

            val savingsRate = if (totalIncome > 0.0) {
                ((netSurplus / totalIncome) * 100).toInt().coerceIn(-100, 100)
            } else 0

            val healthCardRect = RectF(marginStart, y, marginEnd, y + 48f)
            fillPaint.color = colorCardBg
            canvas.drawRoundRect(healthCardRect, 10f, 10f, fillPaint)
            strokePaint.color = colorBorder
            canvas.drawRoundRect(healthCardRect, 10f, 10f, strokePaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 9f
            textPaint.color = colorTextDark
            canvas.drawText("NET CAPITAL RETENTION RATE", marginStart + 16f, y + 19f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = if (savingsRate >= 20) colorGreen else if (savingsRate >= 0) colorPrimary else colorRed
            canvas.drawText("$savingsRate% of Inflow Retained", marginEnd - 16f, y + 19f, textPaint)

            // Progress Bar Track
            val barY = y + 28f
            val barXStart = marginStart + 16f
            val barXEnd = marginEnd - 16f
            val barWidth = barXEnd - barXStart
            val barHeight = 6f

            fillPaint.color = Color.parseColor("#E2E8F0")
            canvas.drawRoundRect(RectF(barXStart, barY, barXEnd, barY + barHeight), 3f, 3f, fillPaint)

            // Progress Bar Fill
            val fillWidth = if (savingsRate > 0) (barWidth * (savingsRate / 100f)).coerceIn(8f, barWidth) else 0f
            if (fillWidth > 0f) {
                fillPaint.color = if (savingsRate >= 20) colorGreen else colorPrimary
                canvas.drawRoundRect(RectF(barXStart, barY, barXStart + fillWidth, barY + barHeight), 3f, 3f, fillPaint)
            }

            // ================================================================
            // 5. ITEMIZED BREAKDOWN TABLE
            // ================================================================
            y += 66f
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11.5f
            textPaint.color = colorTextDark
            canvas.drawText("LEDGER BALANCE SUMMARY", marginStart, y, textPaint)

            y += 10f
            // Table Header Bar
            val tableHeaderRect = RectF(marginStart, y, marginEnd, y + 26f)
            fillPaint.color = colorPurpleTint
            canvas.drawRoundRect(tableHeaderRect, 6f, 6f, fillPaint)

            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = colorPrimaryDark
            canvas.drawText("METRIC CLASSIFICATION", marginStart + 12f, y + 16f, textPaint)
            canvas.drawText("FLOW DIRECTION", marginStart + 220f, y + 16f, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("RECORDED AMOUNT", marginEnd - 12f, y + 16f, textPaint)

            y += 26f

            data class TableRowItem(val title: String, val subtitle: String, val flow: String, val flowColor: Int, val amount: Double)
            val tableItems = listOf(
                TableRowItem("Personal Monthly Inflow", "Salary, professional & genuine income", "INFLOW (+)", colorGreen, totalIncome),
                TableRowItem("Lifestyle Living Expenses", "Essential living, bills, EMI & discretionary spend", "OUTFLOW (-)", colorRed, totalExpenses),
                TableRowItem("Net Capital Retained", "Net surplus available for investment & reserves", if (netSurplus >= 0) "SURPLUS (=)" else "DEFICIT (=)", if (netSurplus >= 0) colorPrimary else colorRed, netSurplus),
                TableRowItem("Safe-to-Spend Runway", "Ring-fenced liquid buffer above all bills & commitments", "RUNWAY", colorPrimaryDark, safeToSpend)
            )

            tableItems.forEachIndexed { idx, item ->
                val rowRect = RectF(marginStart, y, marginEnd, y + 36f)

                // Alternating row background
                if (idx % 2 == 1) {
                    fillPaint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(rowRect, fillPaint)
                }

                // Divider line below row
                strokePaint.color = Color.parseColor("#F1F5F9")
                canvas.drawLine(marginStart, y + 36f, marginEnd, y + 36f, strokePaint)

                // Title & Subtitle
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 9.5f
                textPaint.color = colorTextDark
                canvas.drawText(item.title, marginStart + 12f, y + 15f, textPaint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 8f
                textPaint.color = colorTextMuted
                canvas.drawText(item.subtitle, marginStart + 12f, y + 27f, textPaint)

                // Flow Direction Pill
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 8.5f
                textPaint.color = item.flowColor
                canvas.drawText(item.flow, marginStart + 220f, y + 20f, textPaint)

                // Amount (Right aligned)
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.textSize = 10f
                textPaint.color = colorTextDark
                val amtStr = "$currencySymbol${String.format(Locale.US, "%,.2f", item.amount)}"
                canvas.drawText(amtStr, marginEnd - 12f, y + 20f, textPaint)

                y += 36f
            }

            // ================================================================
            // 6. SECURITY & ARCHITECTURE ATTESTATION BOX
            // ================================================================
            y += 24f
            val sealRect = RectF(marginStart, y, marginEnd, y + 68f)
            fillPaint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(sealRect, 8f, 8f, fillPaint)
            strokePaint.color = colorBorder
            canvas.drawRoundRect(sealRect, 8f, 8f, strokePaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.color = colorPrimaryDark
            canvas.drawText("CRYPTOGRAPHIC AUDIT & PRIVACY ATTESTATION", marginStart + 14f, y + 18f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 7.8f
            textPaint.color = colorTextMuted
            canvas.drawText("• Compiled offline from your device hardware SQLite encrypted database (AES-256 GCM).", marginStart + 14f, y + 32f, textPaint)
            canvas.drawText("• Zero telemetry, server transmission, or third-party sync. Figures reflect local ledger state at export.", marginStart + 14f, y + 44f, textPaint)
            canvas.drawText("• Verified by MyFin 3-Tier Wealth segregation engine (Operating • Commitments • Fortress).", marginStart + 14f, y + 56f, textPaint)

            // ================================================================
            // 7. FOOTER
            // ================================================================
            val footerY = 808f
            strokePaint.color = colorBorder
            canvas.drawLine(marginStart, footerY - 14f, marginEnd, footerY - 14f, strokePaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8f
            textPaint.color = colorTextMuted
            canvas.drawText("MyFin Vault • Confidential Personal Wealth Statement", marginStart, footerY, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page 1 of 1", marginEnd, footerY, textPaint)

            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
            } ?: return false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
}
