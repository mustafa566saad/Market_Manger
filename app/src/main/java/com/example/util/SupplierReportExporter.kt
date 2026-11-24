package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.SupplierMonthlySummary
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ExportFormat {
    CSV,
    PDF
}

data class ExportedReportFile(
    val file: File,
    val mimeType: String,
    val title: String,
    val format: ExportFormat
)

object SupplierReportExporter {

    private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a", Locale("ar"))
    private val fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.US)

    private fun formatMoney(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        return formatter.format(amount)
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    /**
     * Generates a UTF-8 CSV string with BOM for full Arabic Excel/WhatsApp compatibility.
     */
    fun buildCsvContent(
        suppliers: List<SupplierMonthlySummary>,
        yearMonth: YearMonth,
        generatedAt: LocalDateTime = LocalDateTime.now()
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM
        sb.append('\uFEFF')

        val monthLabel = yearMonth.format(arabicMonthFormatter)
        sb.appendLine(escapeCsv("محسن ماركت - تقرير إجمالي حسابات الموردين"))
        sb.appendLine("${escapeCsv("الشهر")},${escapeCsv(monthLabel)}")
        sb.appendLine("${escapeCsv("تاريخ التصدير")},${escapeCsv(generatedAt.format(timestampFormatter))}")
        sb.appendLine()

        // Column headers
        sb.appendLine(
            listOf(
                "اسم المورد",
                "مشتريات الشهر (ج.م)",
                "المدفوع خلال الشهر (ج.م)",
                "صافي مديونية الشهر (ج.م)",
                "إجمالي المديونية التراكمية (ج.م)"
            ).joinToString(",") { escapeCsv(it) }
        )

        // Rows
        suppliers.forEach { item ->
            sb.appendLine(
                listOf(
                    escapeCsv(item.supplierName),
                    String.format(Locale.US, "%.2f", item.monthlyPurchases),
                    String.format(Locale.US, "%.2f", item.monthlyPaid),
                    String.format(Locale.US, "%.2f", item.monthlyNewDebt),
                    String.format(Locale.US, "%.2f", item.cumulativeDebt)
                ).joinToString(",")
            )
        }

        // Totals Row
        val totalMonthlyPurchases = suppliers.sumOf { it.monthlyPurchases }
        val totalMonthlyPaid = suppliers.sumOf { it.monthlyPaid }
        val totalMonthlyDebt = suppliers.sumOf { it.monthlyNewDebt }
        val totalGrandDebt = suppliers.sumOf { it.cumulativeDebt }

        sb.appendLine()
        sb.appendLine(
            listOf(
                escapeCsv("الإجمالي الكلي (${suppliers.size} مورد)"),
                String.format(Locale.US, "%.2f", totalMonthlyPurchases),
                String.format(Locale.US, "%.2f", totalMonthlyPaid),
                String.format(Locale.US, "%.2f", totalMonthlyDebt),
                String.format(Locale.US, "%.2f", totalGrandDebt)
            ).joinToString(",")
        )

        return sb.toString()
    }

    fun exportSuppliersSummaryCsv(
        cacheDir: File,
        suppliers: List<SupplierMonthlySummary>,
        yearMonth: YearMonth
    ): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val fileName = "mohsen_suppliers_${yearMonth.year}_${yearMonth.monthValue}_${LocalDateTime.now().format(fileTimestampFormatter)}.csv"
        val file = File(exportDir, fileName)
        val csvContent = buildCsvContent(suppliers, yearMonth)
        file.writeText(csvContent, Charsets.UTF_8)
        return file
    }

    fun exportSuppliersSummaryPdf(
        cacheDir: File,
        suppliers: List<SupplierMonthlySummary>,
        yearMonth: YearMonth
    ): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val fileName = "mohsen_suppliers_${yearMonth.year}_${yearMonth.monthValue}_${LocalDateTime.now().format(fileTimestampFormatter)}.pdf"
        val file = File(exportDir, fileName)

        val pageWidth = 595
        val pageHeight = 842
        val marginX = 28f
        val contentRight = pageWidth - marginX
        val contentLeft = marginX

        val pdf = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(30, 17, 46)
            style = Paint.Style.FILL
        }
        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val totalsBgPaint = Paint().apply {
            color = Color.rgb(243, 232, 255)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val colHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val boldCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val debtCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 18, 60)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val paidCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }

        // RTL Column X positions (Right-aligned anchor points from right to left)
        val colSupplierX = contentRight - 8f
        val colPurchasesX = 390f
        val colPaidX = 300f
        val colMonthDebtX = 210f
        val colCumDebtX = 115f

        val monthLabel = yearMonth.format(arabicMonthFormatter)
        val generatedLabel = LocalDateTime.now().format(timestampFormatter)

        val rowsPerPage = 24
        val pagesCount = ((suppliers.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

        var currentIndex = 0
        for (pageIndex in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            var y = 26f

            // Top Banner
            canvas.drawRect(contentLeft, y, contentRight, y + 58f, headerBgPaint)
            canvas.drawText("محسن ماركت — تقرير إجمالي حسابات الموردين", contentRight - 14f, y + 24f, titlePaint)
            canvas.drawText("الشهر: $monthLabel   |   تاريخ الإصدار: $generatedLabel", contentRight - 14f, y + 44f, subtitlePaint)
            y += 72f

            // Table Header
            canvas.drawRect(contentLeft, y, contentRight, y + 26f, tableHeaderBgPaint)
            canvas.drawRect(contentLeft, y, contentRight, y + 26f, borderPaint)
            val headerTextY = y + 17f
            canvas.drawText("اسم المورد", colSupplierX, headerTextY, colHeaderPaint)
            canvas.drawText("مشتريات الشهر", colPurchasesX, headerTextY, colHeaderPaint)
            canvas.drawText("المدفوع بالشهر", colPaidX, headerTextY, colHeaderPaint)
            canvas.drawText("مديونية الشهر", colMonthDebtX, headerTextY, colHeaderPaint)
            canvas.drawText("المديونية التراكمية", colCumDebtX, headerTextY, colHeaderPaint)
            y += 26f

            val endIndex = (currentIndex + rowsPerPage).coerceAtMost(suppliers.size)
            for (i in currentIndex until endIndex) {
                val item = suppliers[i]
                val rowHeight = 24f
                if ((i - currentIndex) % 2 == 1) {
                    canvas.drawRect(contentLeft, y, contentRight, y + rowHeight, zebraBgPaint)
                }
                canvas.drawRect(contentLeft, y, contentRight, y + rowHeight, borderPaint)

                val textY = y + 16f
                val nameClipped = if (item.supplierName.length > 22) item.supplierName.take(20) + ".." else item.supplierName
                canvas.drawText("${i + 1}. $nameClipped", colSupplierX, textY, boldCellPaint)
                canvas.drawText(formatMoney(item.monthlyPurchases), colPurchasesX, textY, cellPaint)
                canvas.drawText(formatMoney(item.monthlyPaid), colPaidX, textY, paidCellPaint)
                canvas.drawText(
                    formatMoney(item.monthlyNewDebt),
                    colMonthDebtX,
                    textY,
                    if (item.monthlyNewDebt > 0.01) debtCellPaint else cellPaint
                )
                canvas.drawText(
                    formatMoney(item.cumulativeDebt),
                    colCumDebtX,
                    textY,
                    if (item.cumulativeDebt > 0.01) debtCellPaint else paidCellPaint
                )
                y += rowHeight
            }
            currentIndex = endIndex

            // On last page, draw Totals Row
            if (pageIndex == pagesCount - 1) {
                val totalMonthlyPurchases = suppliers.sumOf { it.monthlyPurchases }
                val totalMonthlyPaid = suppliers.sumOf { it.monthlyPaid }
                val totalMonthlyDebt = suppliers.sumOf { it.monthlyNewDebt }
                val totalGrandDebt = suppliers.sumOf { it.cumulativeDebt }

                y += 8f
                canvas.drawRect(contentLeft, y, contentRight, y + 30f, totalsBgPaint)
                canvas.drawRect(contentLeft, y, contentRight, y + 30f, borderPaint)
                val totY = y + 19f
                canvas.drawText("الإجمالي (${suppliers.size} مورد)", colSupplierX, totY, boldCellPaint)
                canvas.drawText(formatMoney(totalMonthlyPurchases), colPurchasesX, totY, boldCellPaint)
                canvas.drawText(formatMoney(totalMonthlyPaid), colPaidX, totY, paidCellPaint)
                canvas.drawText(formatMoney(totalMonthlyDebt), colMonthDebtX, totY, boldCellPaint)
                canvas.drawText(formatMoney(totalGrandDebt), colCumDebtX, totY, debtCellPaint)
            }

            // Page Footer
            canvas.drawText(
                "صفحة ${pageIndex + 1} من $pagesCount — نظام إدارة محسن ماركت",
                pageWidth / 2f,
                pageHeight - 20f,
                footerPaint
            )

            pdf.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        return file
    }

    fun shareReportFile(
        context: Context,
        report: ExportedReportFile
    ) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, report.file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = report.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, report.title)
            putExtra(Intent.EXTRA_TEXT, "${report.title} — محسن ماركت")
            clipData = ClipData.newRawUri(report.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "مشاركة التقرير عبر...")
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
