package com.example.myfin.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

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
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
            }

            val titlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#6C5CE7")
                textSize = 22f
                isBoldText = true
            }

            val subtitlePaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 12f
            }

            var y = 50f
            canvas.drawText("MyFin Financial Statement", 50f, y, titlePaint)
            y += 25f
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
            canvas.drawText("Generated on: $dateStr | Period: $timeRangeLabel", 50f, y, subtitlePaint)

            y += 40f
            paint.isBoldText = true
            canvas.drawText("Executive Summary", 50f, y, paint)

            y += 25f
            paint.isBoldText = false
            paint.textSize = 13f

            val rows = listOf(
                "Personal Inflow" to "$currencySymbol${String.format(Locale.US, "%,.2f", totalIncome)}",
                "Lifestyle Expenses" to "$currencySymbol${String.format(Locale.US, "%,.2f", totalExpenses)}",
                "Net Capital Retained" to "$currencySymbol${String.format(Locale.US, "%,.2f", netSurplus)}",
                "Safe-to-Spend Reserve" to "$currencySymbol${String.format(Locale.US, "%,.2f", safeToSpend)}"
            )

            rows.forEach { (label, value) ->
                canvas.drawText(label, 50f, y, paint)
                canvas.drawText(value, 350f, y, paint)
                y += 22f
            }

            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
