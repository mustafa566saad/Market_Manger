package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.MonthSummaryData
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object MonthlyReportExporter {

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
     * Generates a UTF-8 CSV report with BOM containing all 10 financial KPIs,
     * operational activity metrics, expense category breakdown, and supplier debts.
     */
    fun buildCsvContent(
        summary: MonthSummaryData,
        generatedAt: LocalDateTime = LocalDateTime.now()
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')

        val ym = YearMonth.of(summary.year, summary.month)
        val monthLabel = ym.format(arabicMonthFormatter)

        sb.appendLine(escapeCsv("محسن ماركت - التقرير المالي الشهري الشامل"))
        sb.appendLine("${escapeCsv("الشهر")},${escapeCsv(monthLabel)}")
        sb.appendLine("${escapeCsv("تاريخ التصدير")},${escapeCsv(generatedAt.format(timestampFormatter))}")
        sb.appendLine()

        // Section 1: 10 Financial KPIs
        sb.appendLine(escapeCsv("=== المؤشرات المالية الـ 10 ==="))
        sb.appendLine("${escapeCsv("المؤشر")},${escapeCsv("القيمة (ج.م)")},${escapeCsv("ملاحظات")}")
        val kpis = listOf(
            Triple("1. إجمالي مبيعات المحل", summary.totalSales, "${summary.closedDaysCount} يوم مقفل"),
            Triple("2. المكسب التقديري (10%)", summary.grossProfit10Percent, "10% من إجمالي المبيعات"),
            Triple("3. إجمالي مشتريات البضاعة", summary.totalPurchases, "${summary.purchaseBillsCount} فاتورة مشتريات"),
            Triple("4. المسدد للتجار (الإجمالي)", summary.totalSupplierPayments, "كاش + فوري"),
            Triple("   - مسدد للتجار كاش (من الدرج)", summary.cashSupplierPayments, "مخصوم من نقدية الدرج"),
            Triple("   - مسدد للتجار فوري / إلكتروني", summary.fawrySupplierPayments, "غير مخصوم من الدرج"),
            Triple("5. ديون الشهر الجديدة للموردين", summary.monthlySupplierDebt, "المشتريات - المسدد خلال الشهر"),
            Triple("6. الديون التراكمية للموردين", summary.cumulativeSupplierDebt, "الرصيد التراكمي الكلي"),
            Triple("7. إجمالي المصروفات التشغيلية", summary.operatingExpenses, "${summary.expenseCount} قيد مصروفات"),
            Triple(
                "8. أجور وساعات الموظفين",
                summary.employeeWages,
                "${summary.attendanceShiftsCount} وردية / ${String.format(Locale.US, "%.1f", summary.totalStaffHours)} ساعة"
            ),
            Triple("9. صافي الربح التشغيلي", summary.netOperatingProfit, "المكسب 10% - المصروفات - الأجور"),
            Triple("10. صافي التدفق النقدي بالدرج", summary.netDrawerCashFlow, "المبيعات - المصروفات - مسدد كاش")
        )
        kpis.forEach { (label, value, note) ->
            sb.appendLine(
                "${escapeCsv(label)},${String.format(Locale.US, "%.2f", value)},${escapeCsv(note)}"
            )
        }

        sb.appendLine()

        // Section 2: Operational Activity Metrics
        sb.appendLine(escapeCsv("=== مؤشرات النشاط التشغيلي ==="))
        sb.appendLine("${escapeCsv("المؤشر التشغيلي")},${escapeCsv("القيمة")}")
        sb.appendLine("${escapeCsv("عدد الأيام المسجلة (المقفلة)")},${summary.closedDaysCount}")
        sb.appendLine("${escapeCsv("متوسط المبيعات اليومي (ج.م)")},${String.format(Locale.US, "%.2f", summary.dailySalesAverage)}")
        sb.appendLine("${escapeCsv("عدد فواتير المشتريات")},${summary.purchaseBillsCount}")
        sb.appendLine("${escapeCsv("عدد الموردين")},${summary.supplierCount}")
        sb.appendLine("${escapeCsv("عدد قيود المصروفات")},${summary.expenseCount}")
        sb.appendLine("${escapeCsv("عدد ورديات الحضور")},${summary.attendanceShiftsCount}")
        sb.appendLine("${escapeCsv("إجمالي ساعات العمل")},${String.format(Locale.US, "%.2f", summary.totalStaffHours)}")

        sb.appendLine()

        // Section 3: Expense Category Breakdown
        sb.appendLine(escapeCsv("=== تفصيل المصروفات التشغيلية حسب البند ==="))
        sb.appendLine("${escapeCsv("البند")},${escapeCsv("الإجمالي (ج.م)")},${escapeCsv("النسبة (%)")}")
        val sortedExpenses = summary.expenseBreakdown.entries.sortedByDescending { it.value }
        if (sortedExpenses.isEmpty()) {
            sb.appendLine("${escapeCsv("لا توجد مصروفات مسجلة لهذا الشهر")},0.00,0.0%")
        } else {
            val totalExp = summary.operatingExpenses.coerceAtLeast(0.01)
            sortedExpenses.forEach { (type, amount) ->
                val pct = (amount / totalExp) * 100.0
                sb.appendLine(
                    "${escapeCsv(type)},${String.format(Locale.US, "%.2f", amount)},${String.format(Locale.US, "%.1f%%", pct)}"
                )
            }
        }

        sb.appendLine()

        // Section 4: Supplier Breakdown
        sb.appendLine(escapeCsv("=== ملخص حسابات الموردين خلال الشهر ==="))
        sb.appendLine(
            listOf(
                "اسم المورد",
                "المشتريات (ج.م)",
                "المسدد (ج.م)",
                "المتبقي للشهر (ج.م)"
            ).joinToString(",") { escapeCsv(it) }
        )
        if (summary.supplierDebts.isEmpty()) {
            sb.appendLine("${escapeCsv("لا توجد فواتير موردين مسجلة لهذا الشهر")},0.00,0.00,0.00")
        } else {
            summary.supplierDebts.forEach { debt ->
                sb.appendLine(
                    listOf(
                        escapeCsv(debt.supplierName),
                        String.format(Locale.US, "%.2f", debt.totalInvoiced),
                        String.format(Locale.US, "%.2f", debt.totalPaid),
                        String.format(Locale.US, "%.2f", debt.remainingDebt)
                    ).joinToString(",")
                )
            }
        }

        return sb.toString()
    }

    fun exportMonthlyReportCsv(
        cacheDir: File,
        summary: MonthSummaryData
    ): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val fileName = "mohsen_monthly_report_${summary.year}_${summary.month}_${LocalDateTime.now().format(fileTimestampFormatter)}.csv"
        val file = File(exportDir, fileName)
        file.writeText(buildCsvContent(summary), Charsets.UTF_8)
        return file
    }

    fun exportMonthlyReportPdf(
        cacheDir: File,
        summary: MonthSummaryData
    ): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val fileName = "mohsen_monthly_report_${summary.year}_${summary.month}_${LocalDateTime.now().format(fileTimestampFormatter)}.pdf"
        val file = File(exportDir, fileName)

        val pageWidth = 595
        val pageHeight = 842
        val marginX = 28f
        val contentLeft = marginX
        val contentRight = pageWidth - marginX
        val bottomLimit = pageHeight - 44f

        val pdf = PdfDocument()

        // Paints
        val bannerBgPaint = Paint().apply {
            color = Color.rgb(6, 55, 42) // Dark Forest Green
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 250, 229)
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(6, 78, 59)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(236, 253, 245)
            style = Paint.Style.FILL
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val highlightProfitBgPaint = Paint().apply {
            color = Color.rgb(220, 252, 231)
            style = Paint.Style.FILL
        }
        val highlightLossBgPaint = Paint().apply {
            color = Color.rgb(254, 226, 226)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val colHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val boldCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val positivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 18, 60)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }

        val monthLabel = YearMonth.of(summary.year, summary.month).format(arabicMonthFormatter)
        val generatedLabel = LocalDateTime.now().format(timestampFormatter)

        val pages = mutableListOf<PdfDocument.Page>()
        var pageNumber = 0
        lateinit var currentPage: PdfDocument.Page
        lateinit var canvas: android.graphics.Canvas
        var y = 0f

        fun startNewPage(withMainBanner: Boolean) {
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdf.startPage(pageInfo)
            pages.add(currentPage)
            canvas = currentPage.canvas
            y = 24f

            if (withMainBanner) {
                canvas.drawRect(contentLeft, y, contentRight, y + 60f, bannerBgPaint)
                canvas.drawText("محسن ماركت — التقرير المالي الشهري الشامل", contentRight - 14f, y + 24f, titlePaint)
                canvas.drawText("الفترة: $monthLabel   |   تاريخ الإصدار: $generatedLabel", contentRight - 14f, y + 44f, subtitlePaint)
                y += 74f
            } else {
                canvas.drawRect(contentLeft, y, contentRight, y + 32f, bannerBgPaint)
                canvas.drawText("محسن ماركت — تابع التقرير المالي ($monthLabel)", contentRight - 14f, y + 20f, subtitlePaint)
                y += 46f
            }
        }

        fun ensureSpace(neededHeight: Float, repeatHeader: (() -> Unit)? = null) {
            if (y + neededHeight > bottomLimit) {
                pdf.finishPage(currentPage)
                startNewPage(withMainBanner = false)
                repeatHeader?.invoke()
            }
        }

        // Start Page 1
        startNewPage(withMainBanner = true)

        // Section 1: 10 Financial KPIs
        canvas.drawText("أولاً: المؤشرات المالية الـ 10", contentRight, y + 12f, sectionTitlePaint)
        y += 20f

        val colLabelX = contentRight - 10f
        val colValueX = 235f
        val colDetailX = 145f

        fun drawKpiHeader() {
            canvas.drawRect(contentLeft, y, contentRight, y + 24f, tableHeaderBgPaint)
            canvas.drawRect(contentLeft, y, contentRight, y + 24f, borderPaint)
            canvas.drawText("المؤشر المالي", colLabelX, y + 16f, colHeaderPaint)
            canvas.drawText("القيمة (ج.م)", colValueX, y + 16f, colHeaderPaint)
            canvas.drawText("التفاصيل", colDetailX, y + 16f, colHeaderPaint)
            y += 24f
        }
        drawKpiHeader()

        data class PdfKpiRow(
            val label: String,
            val value: Double,
            val detail: String,
            val styleType: Int = 0 // 0=normal, 1=positive, 2=negative, 3=profitHighlight
        )

        val kpiRows = listOf(
            PdfKpiRow("1. إجمالي مبيعات المحل", summary.totalSales, "${summary.closedDaysCount} يوم مقفل", 1),
            PdfKpiRow("2. المكسب التقديري (10%)", summary.grossProfit10Percent, "هامش ربح تقديري", 1),
            PdfKpiRow("3. إجمالي مشتريات البضاعة", summary.totalPurchases, "${summary.purchaseBillsCount} فاتورة", 0),
            PdfKpiRow(
                "4. المسدد للتجار (كاش + فوري)",
                summary.totalSupplierPayments,
                "كاش: ${formatMoney(summary.cashSupplierPayments)} | فوري: ${formatMoney(summary.fawrySupplierPayments)}",
                0
            ),
            PdfKpiRow("5. ديون الشهر الجديدة للموردين", summary.monthlySupplierDebt, "المتبقي من فواتير الشهر", 2),
            PdfKpiRow("6. الديون التراكمية للموردين", summary.cumulativeSupplierDebt, "إجمالي المديونية الكلية", 2),
            PdfKpiRow("7. إجمالي المصروفات التشغيلية", summary.operatingExpenses, "${summary.expenseCount} قيد مصروفات", 2),
            PdfKpiRow(
                "8. أجور وساعات الموظفين",
                summary.employeeWages,
                "${summary.attendanceShiftsCount} وردية (${String.format(Locale.US, "%.1f", summary.totalStaffHours)} س)",
                0
            ),
            PdfKpiRow(
                "9. صافي الربح التشغيلي",
                summary.netOperatingProfit,
                "المكسب - المصروفات - الأجور",
                if (summary.netOperatingProfit >= 0) 3 else 2
            ),
            PdfKpiRow(
                "10. صافي التدفق النقدي بالدرج",
                summary.netDrawerCashFlow,
                "المبيعات - المصروفات - كاش الموردين",
                if (summary.netDrawerCashFlow >= 0) 1 else 2
            )
        )

        kpiRows.forEachIndexed { idx, row ->
            val rowH = 24f
            ensureSpace(rowH) { drawKpiHeader() }
            val bgPaint = when {
                row.styleType == 3 -> highlightProfitBgPaint
                row.label.startsWith("9.") && summary.netOperatingProfit < 0 -> highlightLossBgPaint
                idx % 2 == 1 -> zebraBgPaint
                else -> null
            }
            if (bgPaint != null) {
                canvas.drawRect(contentLeft, y, contentRight, y + rowH, bgPaint)
            }
            canvas.drawRect(contentLeft, y, contentRight, y + rowH, borderPaint)

            val valuePaint = when (row.styleType) {
                1, 3 -> positivePaint
                2 -> negativePaint
                else -> boldCellPaint
            }
            canvas.drawText(row.label, colLabelX, y + 16f, boldCellPaint)
            canvas.drawText(formatMoney(row.value), colValueX, y + 16f, valuePaint)
            val clippedDetail = if (row.detail.length > 34) row.detail.take(32) + ".." else row.detail
            canvas.drawText(clippedDetail, colDetailX, y + 16f, subNotePaint)
            y += rowH
        }

        y += 16f

        // Section 2: Operational Activity Metrics
        ensureSpace(95f)
        canvas.drawText("ثانياً: مؤشرات النشاط التشغيلي", contentRight, y + 12f, sectionTitlePaint)
        y += 20f

        val opMetrics = listOf(
            "عدد الأيام المسجلة (المقفلة)" to "${summary.closedDaysCount} يوم",
            "متوسط المبيعات اليومي" to "${formatMoney(summary.dailySalesAverage)} ج.م",
            "عدد فواتير المشتريات" to "${summary.purchaseBillsCount} فاتورة",
            "عدد الموردين المتعامل معهم" to "${summary.supplierCount} مورد",
            "عدد قيود المصروفات" to "${summary.expenseCount} قيد"
        )
        opMetrics.forEachIndexed { idx, (label, valText) ->
            val rowH = 21f
            ensureSpace(rowH)
            if (idx % 2 == 1) {
                canvas.drawRect(contentLeft, y, contentRight, y + rowH, zebraBgPaint)
            }
            canvas.drawRect(contentLeft, y, contentRight, y + rowH, borderPaint)
            canvas.drawText(label, contentRight - 10f, y + 14f, cellPaint)
            canvas.drawText(valText, 190f, y + 14f, boldCellPaint)
            y += rowH
        }

        y += 16f

        // Section 3: Expense Breakdown
        val sortedExpenses = summary.expenseBreakdown.entries.sortedByDescending { it.value }
        ensureSpace(56f)
        canvas.drawText("ثالثاً: تفصيل المصروفات التشغيلية حسب البند", contentRight, y + 12f, sectionTitlePaint)
        y += 20f

        fun drawExpenseHeader() {
            canvas.drawRect(contentLeft, y, contentRight, y + 22f, tableHeaderBgPaint)
            canvas.drawRect(contentLeft, y, contentRight, y + 22f, borderPaint)
            canvas.drawText("بند المصروف", contentRight - 10f, y + 15f, colHeaderPaint)
            canvas.drawText("المبلغ (ج.م)", 235f, y + 15f, colHeaderPaint)
            canvas.drawText("النسبة من المصروفات", 110f, y + 15f, colHeaderPaint)
            y += 22f
        }
        drawExpenseHeader()

        if (sortedExpenses.isEmpty()) {
            ensureSpace(22f)
            canvas.drawRect(contentLeft, y, contentRight, y + 22f, borderPaint)
            canvas.drawText("لا توجد مصروفات مسجلة خلال هذا الشهر", contentRight - 10f, y + 15f, subNotePaint)
            y += 22f
        } else {
            val totalExp = summary.operatingExpenses.coerceAtLeast(0.01)
            sortedExpenses.forEachIndexed { idx, (type, amount) ->
                val rowH = 21f
                ensureSpace(rowH) { drawExpenseHeader() }
                if (idx % 2 == 1) {
                    canvas.drawRect(contentLeft, y, contentRight, y + rowH, zebraBgPaint)
                }
                canvas.drawRect(contentLeft, y, contentRight, y + rowH, borderPaint)
                val pct = (amount / totalExp) * 100.0
                canvas.drawText(type, contentRight - 10f, y + 14f, boldCellPaint)
                canvas.drawText(formatMoney(amount), 235f, y + 14f, negativePaint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", pct), 110f, y + 14f, cellPaint)
                y += rowH
            }
        }

        y += 16f

        // Section 4: Supplier Debts Breakdown
        ensureSpace(56f)
        canvas.drawText("رابعاً: ملخص تعاملات الموردين خلال الشهر", contentRight, y + 12f, sectionTitlePaint)
        y += 20f

        fun drawSupplierHeader() {
            canvas.drawRect(contentLeft, y, contentRight, y + 22f, tableHeaderBgPaint)
            canvas.drawRect(contentLeft, y, contentRight, y + 22f, borderPaint)
            canvas.drawText("اسم المورد", contentRight - 10f, y + 15f, colHeaderPaint)
            canvas.drawText("المشتريات", 310f, y + 15f, colHeaderPaint)
            canvas.drawText("المسدد", 205f, y + 15f, colHeaderPaint)
            canvas.drawText("المتبقي للشهر", 105f, y + 15f, colHeaderPaint)
            y += 22f
        }
        drawSupplierHeader()

        if (summary.supplierDebts.isEmpty()) {
            ensureSpace(22f)
            canvas.drawRect(contentLeft, y, contentRight, y + 22f, borderPaint)
            canvas.drawText("لا توجد فواتير موردين مسجلة خلال هذا الشهر", contentRight - 10f, y + 15f, subNotePaint)
            y += 22f
        } else {
            summary.supplierDebts.forEachIndexed { idx, s ->
                val rowH = 21f
                ensureSpace(rowH) { drawSupplierHeader() }
                if (idx % 2 == 1) {
                    canvas.drawRect(contentLeft, y, contentRight, y + rowH, zebraBgPaint)
                }
                canvas.drawRect(contentLeft, y, contentRight, y + rowH, borderPaint)
                val nameClipped = if (s.supplierName.length > 26) s.supplierName.take(24) + ".." else s.supplierName
                canvas.drawText("${idx + 1}. $nameClipped", contentRight - 10f, y + 14f, boldCellPaint)
                canvas.drawText(formatMoney(s.totalInvoiced), 310f, y + 14f, cellPaint)
                canvas.drawText(formatMoney(s.totalPaid), 205f, y + 14f, positivePaint)
                canvas.drawText(
                    formatMoney(s.remainingDebt),
                    105f,
                    y + 14f,
                    if (s.remainingDebt > 0.01) negativePaint else positivePaint
                )
                y += rowH
            }
        }

        // Draw footer on the final page (note: PdfDocument pages cannot be edited after finishPage,
        // so let's draw footer on currentPage right before finishing; earlier pages already finished
        // had enough bottom margin). Wait — let's draw page number on currentPage!
        canvas.drawText(
            "صفحة $pageNumber — نظام إدارة محسن ماركت",
            pageWidth / 2f,
            pageHeight - 20f,
            footerPaint
        )
        pdf.finishPage(currentPage)

        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        return file
    }

    fun shareMonthlyReportFile(
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

        val chooser = Intent.createChooser(shareIntent, "مشاركة التقرير المالي الشامل عبر...")
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
