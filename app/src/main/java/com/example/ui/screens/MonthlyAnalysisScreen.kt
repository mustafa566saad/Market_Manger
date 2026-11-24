package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MonthSummaryData
import com.example.data.model.SupplierDebtSummary
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.MonthClosingViewModel
import com.example.util.ExportFormat
import com.example.util.MonthlyReportExporter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Dark AMOLED Theme Palette for Monthly Closing (Dark Forest Green Accent)
private val ScreenBgBlack = Color(0xFF000000)
private val CardDarkSurface = Color(0xFF141418)
private val CardDarkElevated = Color(0xFF1E1E24)
private val CardBorderSubtle = Color(0xFF2A2A35)
private val TextPrimaryLight = Color(0xFFF8FAFC)
private val TextSecondaryMuted = Color(0xFFA1A1AA)

private val ForestGreenPrimary = Color(0xFF34D399)
private val ForestGreenDeep = Color(0xFF059669)
private val ForestGreenDarkSurface = Color(0xFF0D271F)
private val SkyAccent = Color(0xFF38BDF8)
private val FawryBlueAccent = Color(0xFF60A5FA)
private val CoralRedAccent = Color(0xFFF87171)
private val AmberGoldAccent = Color(0xFFFBBF24)
private val PlumPurpleAccent = Color(0xFFC084FC)

@Composable
fun MonthlyAnalysisScreen(
    modifier: Modifier = Modifier,
    viewModel: MonthClosingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.exportedReportEvent) {
        uiState.exportedReportEvent?.let { report ->
            MonthlyReportExporter.shareMonthlyReportFile(context, report)
            viewModel.consumeExportedReportEvent()
        }
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar")) }
    val selectedMonthLabel = remember(uiState.selectedYearMonth) {
        uiState.selectedYearMonth.format(monthFormatter)
    }
    val isCurrentMonth = uiState.selectedYearMonth == YearMonth.now()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = ScreenBgBlack,
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { innerPadding ->
            val analysis = uiState.summary

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBgBlack)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Month/Year Selector & Refresh Bar
                item {
                    MonthSelectorHeaderCard(
                        monthLabel = selectedMonthLabel,
                        isCurrentMonth = isCurrentMonth,
                        isLoading = uiState.isLoading,
                        onPrevMonth = { viewModel.prevMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onResetCurrent = { viewModel.resetToCurrentMonth() },
                        onRefresh = { viewModel.loadMonthlySummary() }
                    )
                }

                // 2. Comprehensive Share / Export CTA
                item {
                    Button(
                        onClick = { viewModel.openExportDialog() },
                        enabled = analysis != null && !uiState.isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("export_monthly_report_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestGreenDeep,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1F2937),
                            disabledContentColor = TextSecondaryMuted
                        )
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جاري تجهيز التقرير الشامل...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة التقرير المالي الشامل",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشاركة التقرير المالي الشامل 📊",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Error Banner if any
                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1215)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CoralRedAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CoralRedAccent
                                )
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    color = Color(0xFFFECACA),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.loadMonthlySummary() }) {
                                    Text("إعادة المحاولة", color = CoralRedAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Loading / Empty vs Data
                if (uiState.isLoading && analysis == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = ForestGreenPrimary)
                                Text(
                                    text = "جاري تجميع المؤشرات المالية الـ 10 من قاعدة البيانات...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (analysis != null) {
                    // 3. Executive Verdict Banner
                    item {
                        ExecutiveSummaryHeroCard(analysis = analysis, monthLabel = selectedMonthLabel)
                    }

                    // 4. Section Header: 10 Financial KPIs
                    item {
                        SectionTitleRow(
                            title = "المؤشرات المالية التنفيذية الـ 10",
                            subtitle = "تحليل شامل لحركة المبيعات والموردين والمصروفات والربحية",
                            accentColor = ForestGreenPrimary
                        )
                    }

                    // KPI 1 & KPI 2
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                number = 1,
                                title = "إجمالي مبيعات المحل",
                                value = formatCurrency(analysis.totalSales),
                                subtitle = "من ${analysis.closedDaysCount} يومية مقفلة",
                                icon = Icons.Default.TrendingUp,
                                accentColor = SkyAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_1_total_sales")
                            )
                            KpiMetricCard(
                                number = 2,
                                title = "المكسب التقديري 10%",
                                value = formatCurrency(analysis.grossProfit10Percent),
                                subtitle = "إجمالي هامش الربح",
                                icon = Icons.Default.PriceCheck,
                                accentColor = ForestGreenPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_2_gross_profit")
                            )
                        }
                    }

                    // KPI 3 & KPI 4 (with Cash vs Fawry breakdown)
                    item {
                        KpiMetricCard(
                            number = 3,
                            title = "إجمالي مشتريات البضاعة",
                            value = formatCurrency(analysis.totalPurchases),
                            subtitle = "إجمالي فواتير الموردين (${analysis.purchaseBillsCount} فاتورة) خلال الشهر",
                            icon = Icons.Default.ShoppingCart,
                            accentColor = AmberGoldAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kpi_3_total_purchases")
                        )
                    }

                    item {
                        SupplierPaymentsBreakdownKpiCard(
                            analysis = analysis,
                            modifier = Modifier.testTag("kpi_4_supplier_payments")
                        )
                    }

                    // KPI 5 & KPI 6
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                number = 5,
                                title = "ديون الشهر الجديدة للموردين",
                                value = formatCurrency(analysis.monthlySupplierDebt),
                                subtitle = "المشتريات - المسدد",
                                icon = Icons.Default.ReceiptLong,
                                accentColor = CoralRedAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_5_monthly_debt")
                            )
                            KpiMetricCard(
                                number = 6,
                                title = "الديون التراكمية للموردين",
                                value = formatCurrency(analysis.cumulativeSupplierDebt),
                                subtitle = "كامل المديونية حتى الآن",
                                icon = Icons.Default.AccountBalance,
                                accentColor = PlumPurpleAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_6_cumulative_debt")
                            )
                        }
                    }

                    // KPI 7 & KPI 8
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                number = 7,
                                title = "إجمالي المصروفات التشغيلية",
                                value = formatCurrency(analysis.operatingExpenses),
                                subtitle = "${analysis.expenseCount} قيد مصروفات",
                                icon = Icons.Default.MoneyOff,
                                accentColor = CoralRedAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_7_operating_expenses")
                            )
                            KpiMetricCard(
                                number = 8,
                                title = "أجور وساعات الموظفين",
                                value = formatCurrency(analysis.employeeWages),
                                subtitle = "${analysis.attendanceShiftsCount} وردية • ${String.format(Locale.US, "%.1f", analysis.totalStaffHours)} س",
                                icon = Icons.Default.Badge,
                                accentColor = SkyAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_8_employee_wages")
                            )
                        }
                    }

                    // KPI 9 & KPI 10 (Net Operating Profit & Net Cash Flow)
                    item {
                        val isProfitable = analysis.netOperatingProfit >= 0.0
                        val profitColor = if (isProfitable) ForestGreenPrimary else CoralRedAccent
                        KpiMetricCard(
                            number = 9,
                            title = "صافي الربح التشغيلي",
                            value = formatCurrency(analysis.netOperatingProfit),
                            subtitle = "المعادلة: المكسب 10% (${formatCurrency(analysis.grossProfit10Percent)}) - المصروفات (${formatCurrency(analysis.operatingExpenses)}) - الأجور (${formatCurrency(analysis.employeeWages)})",
                            icon = if (isProfitable) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            accentColor = profitColor,
                            highlighted = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kpi_9_net_operating_profit")
                        )
                    }

                    item {
                        val isPositiveFlow = analysis.netDrawerCashFlow >= 0.0
                        val flowColor = if (isPositiveFlow) ForestGreenPrimary else CoralRedAccent
                        KpiMetricCard(
                            number = 10,
                            title = "صافي التدفق النقدي بالدرج",
                            value = formatCurrency(analysis.netDrawerCashFlow),
                            subtitle = "المعادلة: المبيعات (${formatCurrency(analysis.totalSales)}) - المصروفات (${formatCurrency(analysis.operatingExpenses)}) - مسدد كاش (${formatCurrency(analysis.cashSupplierPayments)})",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = flowColor,
                            highlighted = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kpi_10_net_cash_flow")
                        )
                    }

                    // 5. Operational Activity Metrics
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitleRow(
                            title = "مؤشرات النشاط التشغيلي",
                            subtitle = "معدلات التشغيل وحجم المعاملات المسجلة خلال الشهر",
                            accentColor = SkyAccent
                        )
                    }

                    item {
                        OperationalActivityGridCard(
                            analysis = analysis,
                            modifier = Modifier.testTag("operational_activity_card")
                        )
                    }

                    // 6. Monthly Expense Breakdown
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitleRow(
                            title = "تفصيل المصروفات حسب البند",
                            subtitle = "توزيع المصروفات التشغيلية خلال الشهر",
                            accentColor = CoralRedAccent
                        )
                    }

                    if (analysis.expenseBreakdown.isEmpty()) {
                        item {
                            EmptySectionCard(text = "لا توجد مصروفات مسجلة لهذا الشهر")
                        }
                    } else {
                        val sortedExpenses = analysis.expenseBreakdown.entries.sortedByDescending { it.value }
                        itemsIndexed(sortedExpenses, key = { _, item -> "exp_${item.key}" }) { _, entry ->
                            ExpenseBreakdownRowCard(
                                category = entry.key,
                                amount = entry.value,
                                totalExpenses = analysis.operatingExpenses
                            )
                        }
                    }

                    // 7. Supplier Debts Summary for the Month
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitleRow(
                            title = "ملخص حسابات الموردين خلال الشهر",
                            subtitle = "مرتبة حسب المتبقي الأعلى خلال هذا الشهر",
                            accentColor = PlumPurpleAccent
                        )
                    }

                    if (analysis.supplierDebts.isEmpty()) {
                        item {
                            EmptySectionCard(text = "لا توجد فواتير مشتريات مسجلة لهذا الشهر")
                        }
                    } else {
                        itemsIndexed(analysis.supplierDebts, key = { idx, item -> "sup_${idx}_${item.supplierName}" }) { idx, debt ->
                            MonthlySupplierDebtRowCard(index = idx + 1, debt = debt)
                        }
                    }
                }
            }
        }

        // Export Format Selection Dialog (PDF vs CSV)
        if (uiState.showExportDialog) {
            ExportMonthlyReportDialog(
                monthLabel = selectedMonthLabel,
                onDismiss = { viewModel.dismissExportDialog() },
                onExportPdf = { viewModel.exportReport(context, ExportFormat.PDF) },
                onExportCsv = { viewModel.exportReport(context, ExportFormat.CSV) }
            )
        }
    }
}

@Composable
private fun MonthSelectorHeaderCard(
    monthLabel: String,
    isCurrentMonth: Boolean,
    isLoading: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrent: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(CardDarkElevated)
                        .testTag("month_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "الشهر السابق",
                        tint = TextPrimaryLight
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    }
                    Text(
                        text = if (isCurrentMonth) "الشهر الحالي" else "أرشيف التقفيل الشهري",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentMonth) ForestGreenPrimary else TextSecondaryMuted
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(CardDarkElevated)
                        .testTag("month_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الشهر التالي",
                        tint = TextPrimaryLight
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("refresh_monthly_summary_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreenPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimary.copy(alpha = 0.45f))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ForestGreenPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري التحديث...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث البيانات",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث البيانات", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (!isCurrentMonth) {
                    OutlinedButton(
                        onClick = onResetCurrent,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("reset_current_month_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SkyAccent.copy(alpha = 0.4f))
                    ) {
                        Text("الشهر الحالي", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveSummaryHeroCard(
    analysis: MonthSummaryData,
    monthLabel: String
) {
    val isProfitable = analysis.netOperatingProfit >= 0.0
    val accent = if (isProfitable) ForestGreenPrimary else CoralRedAccent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isProfitable) {
                        listOf(Color(0xFF0A2E23), Color(0xFF10201C), CardDarkSurface)
                    } else {
                        listOf(Color(0xFF2D1216), Color(0xFF1E1316), CardDarkSurface)
                    }
                )
            )
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "الخلاصة التنفيذية — $monthLabel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = if (isProfitable) "أداء مالي تشغيلي رابح" else "تنبيه: صافي التشغيل بالسالب",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }

                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isProfitable) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isProfitable) "فائض ربح" else "عجز تشغيلي",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صافي الربح التشغيلي",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted
                    )
                    Text(
                        text = formatCurrency(analysis.netOperatingProfit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "صافي التدفق النقدي",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted
                    )
                    Text(
                        text = formatCurrency(analysis.netDrawerCashFlow),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (analysis.netDrawerCashFlow >= 0) SkyAccent else CoralRedAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitleRow(
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    number: Int,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val bgColor = if (highlighted) accentColor.copy(alpha = 0.08f) else CardDarkSurface
    val borderColor = if (highlighted) accentColor.copy(alpha = 0.5f) else CardBorderSubtle

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#$number",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                style = if (highlighted) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlighted) accentColor else TextPrimaryLight
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryMuted,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SupplierPaymentsBreakdownKpiCard(
    analysis: MonthSummaryData,
    modifier: Modifier = Modifier
) {
    val totalPaid = analysis.totalSupplierPayments.coerceAtLeast(0.0)
    val cashRatio = if (totalPaid > 0.0) {
        (analysis.cashSupplierPayments / totalPaid).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = FawryBlueAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#4",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = FawryBlueAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "المسدد للتجار (كاش مقابل فوري)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                }

                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = FawryBlueAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = formatCurrency(analysis.totalSupplierPayments),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimaryLight
            )

            LinearProgressIndicator(
                progress = { cashRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AmberGoldAccent,
                trackColor = FawryBlueAccent.copy(alpha = 0.35f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = AmberGoldAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AmberGoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "كاش (من الدرج)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryMuted
                            )
                            Text(
                                text = formatCurrency(analysis.cashSupplierPayments),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberGoldAccent
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = FawryBlueAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = FawryBlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "فوري / إلكتروني",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryMuted
                            )
                            Text(
                                text = formatCurrency(analysis.fawrySupplierPayments),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FawryBlueAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationalActivityGridCard(
    analysis: MonthSummaryData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OperationalStatBox(
                    label = "الأيام المسجلة",
                    value = "${analysis.closedDaysCount} يوم",
                    icon = Icons.Default.EventAvailable,
                    color = ForestGreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                OperationalStatBox(
                    label = "متوسط المبيعات اليومي",
                    value = formatCurrency(analysis.dailySalesAverage),
                    icon = Icons.Default.TrendingUp,
                    color = SkyAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OperationalStatBox(
                    label = "فواتير المشتريات",
                    value = "${analysis.purchaseBillsCount} فاتورة",
                    icon = Icons.Default.Description,
                    color = AmberGoldAccent,
                    modifier = Modifier.weight(1f)
                )
                OperationalStatBox(
                    label = "عدد الموردين",
                    value = "${analysis.supplierCount} مورد",
                    icon = Icons.Default.Groups,
                    color = PlumPurpleAccent,
                    modifier = Modifier.weight(1f)
                )
                OperationalStatBox(
                    label = "قيود المصروفات",
                    value = "${analysis.expenseCount} قيد",
                    icon = Icons.Default.Inventory2,
                    color = CoralRedAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OperationalStatBox(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CardDarkElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
        }
    }
}

@Composable
private fun ExpenseBreakdownRowCard(
    category: String,
    amount: Double,
    totalExpenses: Double
) {
    val ratio = if (totalExpenses > 0.0) (amount / totalExpenses).toFloat().coerceIn(0f, 1f) else 0f
    val pct = ratio * 100f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", pct),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted
                    )
                    Text(
                        text = formatCurrency(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralRedAccent
                    )
                }
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CoralRedAccent,
                trackColor = CardDarkElevated
            )
        }
    }
}

@Composable
private fun MonthlySupplierDebtRowCard(
    index: Int,
    debt: SupplierDebtSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$index. ${debt.supplierName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    color = if (debt.remainingDebt > 0.01) {
                        CoralRedAccent.copy(alpha = 0.14f)
                    } else {
                        ForestGreenPrimary.copy(alpha = 0.14f)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (debt.remainingDebt > 0.01) {
                            "متبقي: ${formatCurrency(debt.remainingDebt)}"
                        } else {
                            "خالص"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (debt.remainingDebt > 0.01) CoralRedAccent else ForestGreenPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مشتريات الشهر: ${formatCurrency(debt.totalInvoiced)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryMuted
                )
                Text(
                    text = "المسدد: ${formatCurrency(debt.totalPaid)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForestGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                .padding(20.dp)
        )
    }
}

@Composable
private fun ExportMonthlyReportDialog(
    monthLabel: String,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDarkSurface,
        titleContentColor = TextPrimaryLight,
        textContentColor = TextSecondaryMuted,
        title = {
            Text(
                text = "مشاركة التقرير المالي الشامل 📊",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "اختر صيغة التقرير المالي لشهر ($monthLabel) لمشاركته عبر واتساب أو البريد الإلكتروني أو الطباعة:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_monthly_pdf_option"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreenDeep,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير ومشاركة PDF شامل للطباعة", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_monthly_csv_option"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyAccent.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير ومشاركة جدول Excel (CSV)", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryMuted)
            }
        }
    )
}
