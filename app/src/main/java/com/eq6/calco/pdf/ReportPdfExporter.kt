package com.eq6.calco.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.text.NumberFormat
import java.util.*

class ReportPdfExporter {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun export(
        context: Context,
        storeName: String,
        totals: MonthTotals,
        trend: List<TrendPoint>,
        top: List<TopProductRow>,
        onOk: (Uri) -> Unit,
        onFail: (String) -> Unit
    ) {
        try {
            val fileName = "Calco_Reporte_${totals.baseMonthKey}_vs_${totals.compMonthKey}.pdf"

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: run { onFail("No se pudo crear archivo"); return }

            resolver.openOutputStream(uri)?.use { out ->
                val doc = PdfDocument()

                val page1 = doc.startPage(PdfDocument.PageInfo.Builder(612, 792, 1).create())
                drawPage1(page1.canvas, storeName, totals)
                doc.finishPage(page1)

                val page2 = doc.startPage(PdfDocument.PageInfo.Builder(612, 792, 2).create())
                drawPage2(page2.canvas, totals, trend, top)
                doc.finishPage(page2)

                doc.writeTo(out)
                doc.close()
            }

            onOk(uri)
        } catch (e: Exception) {
            onFail(e.message ?: "Error exportando PDF")
        }
    }

    private fun drawPage1(canvas: Canvas, storeName: String, totals: MonthTotals) {
        val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val pSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = Color.DKGRAY }

        val left = 54f
        var y = 70f

        canvas.drawText("Calco — Reporte comparativo de ventas", left, y, pTitle)
        y += 22f
        canvas.drawText("Tienda: $storeName", left, y, pText)
        y += 16f
        canvas.drawText("Mes base: ${totals.baseMonthKey} | Mes comparación: ${totals.compMonthKey}", left, y, pText)
        y += 14f
        canvas.drawText("Generado: ${Date()}", left, y, pSmall)

        y += 28f
        val pH = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("Resumen", left, y, pH)
        y += 14f

        val baseTotal = totals.baseTotal
        val compTotal = totals.compTotal
        val diff = baseTotal - compTotal
        val pct = if (compTotal == 0.0) 0.0 else (diff / compTotal) * 100.0

        val tableTop = y + 10f
        val col1 = left
        val col2 = left + 260f
        val col3 = left + 420f
        val rowH = 22f

        val pHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pHeaderBg = Paint().apply { color = Color.parseColor("#4C0C7A") }
        canvas.drawRect(col1, tableTop, col1 + 500f, tableTop + rowH, pHeaderBg)
        canvas.drawText("Concepto", col1 + 8f, tableTop + 15f, pHeader)
        canvas.drawText("Mes", col2 + 8f, tableTop + 15f, pHeader)
        canvas.drawText("Monto", col3 + 8f, tableTop + 15f, pHeader)

        val rows = listOf(
            Triple("Mes base", totals.baseMonthKey, moneyFmt.format(baseTotal)),
            Triple("Mes comparación", totals.compMonthKey, moneyFmt.format(compTotal)),
            Triple("Diferencia (Base − Comparación)", "", moneyFmt.format(diff)),
            Triple("Variación %", "", String.format(Locale.getDefault(), "%.1f%%", pct))
        )

        val pRow = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f }
        val pGrid = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val pBg = Paint().apply { color = Color.parseColor("#F3F4F6") }

        for (i in rows.indices) {
            val top = tableTop + rowH * (i + 1)
            canvas.drawRect(col1, top, col1 + 500f, top + rowH, pBg)
            canvas.drawLine(col1, top, col1 + 500f, top, pGrid)

            val color = if (diff >= 0) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
            val rowPaintMonto = Paint(pRow)
            if (i >= 2) rowPaintMonto.color = color

            canvas.drawText(rows[i].first, col1 + 8f, top + 15f, pRow)
            canvas.drawText(rows[i].second, col2 + 8f, top + 15f, pRow)
            canvas.drawText(rows[i].third, col3 + 8f, top + 15f, rowPaintMonto)
        }
        val tableBottom = tableTop + rowH * (rows.size + 1)
        canvas.drawLine(col1, tableBottom, col1 + 500f, tableBottom, pGrid)

        y = tableBottom + 45f
        canvas.drawText("Gráfica 1: Comparación directa (Base vs Comparación)", left, y, pH)
        y += 16f

        val chartLeft = left
        val chartTop = y
        val chartW = 500f
        val chartH = 210f

        val maxVal = maxOf(baseTotal, compTotal).coerceAtLeast(1.0)
        val barMax = maxVal * 1.25

        val pBorder = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY; strokeWidth = 2f }
        canvas.drawRect(chartLeft, chartTop, chartLeft + chartW, chartTop + chartH, pBorder)

        fun barHeight(v: Double): Float = ((v / barMax) * (chartH - 40f)).toFloat()

        val baseH = barHeight(baseTotal)
        val compH = barHeight(compTotal)

        val baseX = chartLeft + 320f
        val compX = chartLeft + 160f
        val barW = 60f
        val bottom = chartTop + chartH - 30f

        val pComp = Paint().apply { color = Color.parseColor("#60A5FA") }
        val pBase = Paint().apply { color = Color.parseColor("#6D1BB3") }
        canvas.drawRect(compX, bottom - compH, compX + barW, bottom, pComp)
        canvas.drawRect(baseX, bottom - baseH, baseX + barW, bottom, pBase)

        val pLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.DKGRAY }
        canvas.drawText("Comparación", compX - 10f, bottom + 18f, pLbl)
        canvas.drawText("Base", baseX + 10f, bottom + 18f, pLbl)

        val pVal = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        pVal.color = pComp.color
        canvas.drawText(moneyFmt.format(compTotal), compX - 5f, bottom - compH - 8f, pVal)
        pVal.color = pBase.color
        canvas.drawText(moneyFmt.format(baseTotal), baseX - 5f, bottom - baseH - 8f, pVal)
    }

    private fun drawPage2(canvas: Canvas, totals: MonthTotals, trend: List<TrendPoint>, top: List<TopProductRow>) {
        val left = 54f
        var y = 70f
        val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pH = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f }

        canvas.drawText("Calco — Reporte (continuación)", left, y, pTitle)
        y += 26f

        canvas.drawText("Gráfica 2: Tendencia (últimos 6 meses)", left, y, pH)
        y += 16f

        val chartLeft = left
        val chartTop = y
        val chartW = 500f
        val chartH = 220f
        val pBorder = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY; strokeWidth = 2f }
        canvas.drawRect(chartLeft, chartTop, chartLeft + chartW, chartTop + chartH, pBorder)

        val values = trend.map { it.total }
        val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val yMax = maxVal * 1.25

        val pLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D1BB3"); strokeWidth = 4f }
        val pDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D1BB3") }
        val pLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = Color.DKGRAY }

        val n = trend.size.coerceAtLeast(2)
        val stepX = (chartW - 40f) / (n - 1)
        val baseY = chartTop + chartH - 35f

        fun yPos(v: Double): Float = baseY - ((v / yMax) * (chartH - 60f)).toFloat()

        var prevX = chartLeft + 20f
        var prevY = yPos(trend[0].total)

        for (i in trend.indices) {
            val x = chartLeft + 20f + stepX * i
            val yv = yPos(trend[i].total)

            if (i > 0) canvas.drawLine(prevX, prevY, x, yv, pLine)
            canvas.drawCircle(x, yv, 6f, pDot)

            val mk = trend[i].monthKey
            val label = mk.substring(5) + "/" + mk.substring(2,4)
            canvas.drawText(label, x - 12f, chartTop + chartH - 12f, pLbl)

            prevX = x
            prevY = yv
        }

        y = chartTop + chartH + 30f

        canvas.drawText("Top productos (comparación por meses)", left, y, pH)
        y += 18f

        val col1 = left
        val col2 = left + 300f
        val col3 = left + 420f
        val rowH = 20f

        val pHeaderBg = Paint().apply { color = Color.parseColor("#4C0C7A") }
        val pHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawRect(col1, y, col1 + 500f, y + rowH, pHeaderBg)
        canvas.drawText("Producto", col1 + 8f, y + 14f, pHeader)
        canvas.drawText("Unid. Base", col2 + 8f, y + 14f, pHeader)
        canvas.drawText("Unid. Comp", col3 + 8f, y + 14f, pHeader)

        val pRow = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val pBg = Paint().apply { color = Color.parseColor("#F3F4F6") }
        val pGrid = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var rowY = y + rowH
        val rows = top.take(8)
        for (r in rows) {
            canvas.drawRect(col1, rowY, col1 + 500f, rowY + rowH, pBg)
            canvas.drawLine(col1, rowY, col1 + 500f, rowY, pGrid)

            canvas.drawText(r.productName.take(28), col1 + 8f, rowY + 14f, pRow)
            canvas.drawText(r.baseUnits.toString(), col2 + 8f, rowY + 14f, pRow)
            canvas.drawText(r.compUnits.toString(), col3 + 8f, rowY + 14f, pRow)

            rowY += rowH
        }

        rowY += 20f
        canvas.drawText("Nota: Unidades sumadas desde sales/{saleId}/items (quantity).", left, rowY, pText)
    }
}