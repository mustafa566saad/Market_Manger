package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.SupplierMonthlySummary
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.SuppliersSummaryUiState
import com.example.ui.viewmodel.SuppliersSummaryViewModel
import com.example.util.ExportFormat
import com.example.util.SupplierReportExporter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PlumAccent = Color(0xFFC084FC)
private val PlumStrong = Color(0xFF9333EA)
private val PlumBg = Color(0xFF1E112E)
private val PlumBorder = Color(0xFF9333EA).copy(alpha = 0.5f)
private val DarkCardBg = Color(0xFF141418)
private val DarkCardElevated = Color(0xFF1E1E24)
private val DarkCardBorder = Color(0xFF272733)
private val SkyAccent = Color(0xFF60A5FA)

private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))

/**
 * [TASK 4: SUPPLIERS SUMMARY & AGGREGATE DEBT REPORT SCREEN]
 * Displays monthly purchases, monthly payments, new monthly debt, and all-time cumulative debt
 * for each supplier sorted descending by highest cumulative debt, with search, month selector,
 * bottom totals bar, and CSV/PDF export via Android Share Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersDebtSummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SuppliersSummaryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.exportedReportEvent) {
        state.exportedReportEvent?.let { report ->
            viewModel.consumeExportedReportEvent()
            try {
                SupplierReportExporter.shareReportFile(context, report)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("تعذر فتح قائمة المشاركة: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color.Black,
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = PlumAccent.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, PlumAccent.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = PlumAccent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تقرير إجمالي حسابات الموردين",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "تحليل المشتريات الشهرية والمديونيات التراكمية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("debt_summary_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color(0xFFF8FAFC)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.loadSummary() },
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("debt_summary_refresh_button")
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = PlumAccent,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث",
                                    tint = PlumAccent
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkCardBg
                    )
                )
            },
            bottomBar = {
                BottomSummaryTotalsBar(
                    state = state,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black)
                    .testTag("suppliers_debt_summary_screen")
            ) {
                // Sticky Header Controls: Month Selector + Search Bar + Export Button
                SummaryFilterAndExportHeader(
                    state = state,
                    onPrevMonth = { viewModel.prevMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onResetMonth = { viewModel.resetToCurrentMonth() },
                    onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                    onExportClick = { viewModel.openExportDialog() }
                )

                // Supplier Data List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val filtered = state.filteredSuppliers

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.errorMessage != null) {
                            item {
                                ErrorBannerCard(
                                    message = state.errorMessage ?: "",
                                    onRetry = { viewModel.loadSummary() }
                                )
                            }
                        }

                        if (state.isLoading && state.allSuppliers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = PlumAccent)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "جاري تجميع حسابات الموردين من السيرفر...",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else if (filtered.isEmpty() && !state.isLoading) {
                            item {
                                EmptySuppliersSummaryCard(
                                    hasSearch = state.searchQuery.isNotBlank()
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = filtered,
                                key = { _, item -> item.supplierName }
                            ) { index, supplier ->
                                SupplierMonthlySummaryCard(
                                    rank = index + 1,
                                    supplier = supplier
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    // Export Format Chooser Dialog (CSV or PDF -> Share Sheet)
    if (state.showExportDialog) {
        ExportFormatChooserDialog(
            monthLabel = state.selectedYearMonth.format(arabicMonthFormatter),
            suppliersCount = state.filteredSuppliers.size,
            isExporting = state.isExporting,
            onDismiss = { viewModel.dismissExportDialog() },
            onSelectFormat = { format -> viewModel.exportReport(context, format) }
        )
    }
}

// =============================================================================
// HEADER CONTROLS: MONTH/YEAR SELECTOR, SEARCH BAR, EXPORT BUTTON
// =============================================================================

@Composable
private fun SummaryFilterAndExportHeader(
    state: SuppliersSummaryUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onExportClick: () -> Unit
) {
    val isCurrentMonth = state.selectedYearMonth == YearMonth.now()

    Surface(
        color = DarkCardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Month / Year Selector Row + Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Switcher Pill
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevMonth,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("summary_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "الشهر السابق",
                                tint = PlumAccent
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isCurrentMonth) { onResetMonth() }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PlumAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.selectedYearMonth.format(arabicMonthFormatter),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("summary_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "الشهر التالي",
                                tint = PlumAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Export & Share Button
                Button(
                    onClick = onExportClick,
                    enabled = !state.isExporting && state.allSuppliers.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlumStrong,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF282034),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_report_button")
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تصدير التقرير 📊",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. Search Bar + Sort Indicator
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChanged,
                placeholder = {
                    Text(
                        text = "ابحث باسم المورد أو الشركة...",
                        color = Color(0xFF8E8E98),
                        fontSize = 13.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PlumAccent
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChanged("") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث",
                                tint = Color(0xFFA1A1AA)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCardElevated,
                    unfocusedContainerColor = DarkCardElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PlumAccent,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_search_field")
            )

            // Info Strip: Count & Sort Rule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عدد الموردين: ${state.filteredSuppliers.size} من ${state.allSuppliers.size}",
                    fontSize = 11.5.sp,
                    color = Color(0xFFA1A1AA),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "مرتب تنازلياً حسب أعلى مديونية تراكمية ↓",
                    fontSize = 11.5.sp,
                    color = PlumAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =============================================================================
// SUPPLIER ROW / CARD
// =============================================================================

@Composable
private fun SupplierMonthlySummaryCard(
    rank: Int,
    supplier: SupplierMonthlySummary
) {
    val isSettled = supplier.cumulativeDebt <= 0.01
    val hasMonthlyActivity = supplier.monthlyPurchases > 0.01 || supplier.monthlyPaid > 0.01
    val paidRatio = if (supplier.allTimeInvoiced > 0.01) {
        (supplier.allTimePaid / supplier.allTimeInvoiced).toFloat().coerceIn(0f, 1f)
    } else 1f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        if (!isSettled) PlumBorder else ProfitGreen.copy(alpha = 0.3f),
                        DarkCardBorder
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("supplier_summary_card_$rank")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Rank + Supplier Name + All-Time Cumulative Debt Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (!isSettled) PlumAccent.copy(alpha = 0.16f)
                                else ProfitGreen.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$rank",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isSettled) PlumAccent else ProfitGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = supplier.supplierName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasMonthlyActivity) "نشط خلال هذا الشهر" else "رصيد من أشهر سابقة",
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E98)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // All-Time Cumulative Debt Badge
                Surface(
                    color = if (isSettled) ProfitGreen.copy(alpha = 0.16f) else ExpenseRed.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(
                        1.dp,
                        if (isSettled) ProfitGreen.copy(alpha = 0.35f) else ExpenseRed.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "المديونية التراكمية",
                            fontSize = 10.sp,
                            color = if (isSettled) ProfitGreen else Color(0xFFFDA4AF)
                        )
                        Text(
                            text = if (isSettled) "خالص ✓" else formatCurrency(supplier.cumulativeDebt),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSettled) ProfitGreen else Color(0xFFFB7185)
                        )
                    }
                }
            }

            // Monthly Breakdown Grid (Purchases, Paid, New Debt = Purchases - Paid)
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Current Month Purchases
                    MetricCell(
                        label = "مشتريات الشهر",
                        value = formatCurrency(supplier.monthlyPurchases),
                        valueColor = SkyAccent,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(DarkCardBorder)
                    )

                    // 2. Current Month Paid
                    MetricCell(
                        label = "المدفوع بالشهر",
                        value = formatCurrency(supplier.monthlyPaid),
                        valueColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(DarkCardBorder)
                    )

                    // 3. Current Month New Debt (Purchases - Paid)
                    val newDebtColor = when {
                        supplier.monthlyNewDebt > 0.01 -> AmberWarning
                        supplier.monthlyNewDebt < -0.01 -> ProfitGreen
                        else -> Color(0xFFA1A1AA)
                    }
                    MetricCell(
                        label = "مديونية الشهر",
                        value = formatCurrency(supplier.monthlyNewDebt),
                        valueColor = newDebtColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Overall Settlement Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { paidRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isSettled) ProfitGreen else PlumAccent,
                    trackColor = Color(0xFF272733)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إجمالي تعاملات المورد: ${formatCurrency(supplier.allTimeInvoiced)}",
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E98)
                    )
                    Text(
                        text = "نسبة السداد ${String.format(Locale.US, "%.1f", paidRatio * 100f)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) ProfitGreen else PlumAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = Color(0xFFA1A1AA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =============================================================================
// BOTTOM TOTALS BAR (4 KEY AGGREGATES)
// =============================================================================

@Composable
private fun BottomSummaryTotalsBar(
    state: SuppliersSummaryUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkCardBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = PlumBorder,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .testTag("bottom_totals_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Total Monthly Purchases
                TotalMiniTile(
                    label = "إجمالي مشتريات الشهر",
                    value = formatCurrency(state.totalMonthlyPurchases),
                    accentColor = SkyAccent,
                    modifier = Modifier.weight(1f)
                )

                // 2. Total Monthly Paid
                TotalMiniTile(
                    label = "إجمالي المدفوع بالشهر",
                    value = formatCurrency(state.totalMonthlyPaid),
                    accentColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3. Total Monthly Debt
                TotalMiniTile(
                    label = "صافي مديونية الشهر",
                    value = formatCurrency(state.totalMonthlyDebt),
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )

                // 4. Total Grand Debt across all suppliers
                TotalMiniTile(
                    label = "إجمالي الديون الكلي (التراكمي)",
                    value = formatCurrency(state.totalGrandDebt),
                    accentColor = ExpenseRed,
                    highlighted = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TotalMiniTile(
    label: String,
    value: String,
    accentColor: Color,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlighted) PlumBg else DarkCardElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(
            width = 1.dp,
            color = if (highlighted) accentColor.copy(alpha = 0.55f) else DarkCardBorder,
            shape = RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = Color(0xFFA1A1AA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================================
// EXPORT FORMAT CHOOSER DIALOG
// =============================================================================

@Composable
private fun ExportFormatChooserDialog(
    monthLabel: String,
    suppliersCount: Int,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onSelectFormat: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        containerColor = DarkCardElevated,
        titleContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = PlumAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصدير ومشاركة التقرير", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اختر صيغة التقرير لشهر $monthLabel ($suppliersCount مورد):",
                    color = Color(0xFFD4D4D8),
                    fontSize = 13.5.sp
                )

                // PDF Option
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PlumBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isExporting) { onSelectFormat(ExportFormat.PDF) }
                        .testTag("export_pdf_option")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ExpenseRed.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ملف PDF منسق",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "مناسب للطباعة أو المشاركة السريعة عبر واتساب",
                                fontSize = 11.5.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }
                }

                // CSV Option
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ProfitGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isExporting) { onSelectFormat(ExportFormat.CSV) }
                        .testTag("export_csv_option")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ProfitGreen.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "جدول بيانات CSV (Excel)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "متوافق مع Excel وجوجل شيت بترميز عربي كامل",
                                fontSize = 11.5.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }
                }

                if (isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = PlumAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري إنشاء الملف...", color = PlumAccent, fontSize = 12.5.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("إغلاق", color = Color(0xFFA1A1AA))
            }
        }
    )
}

@Composable
private fun ErrorBannerCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        color = ExpenseRed.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ExpenseRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFECDD3),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
            ) {
                Text("إعادة", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptySuppliersSummaryCard(hasSearch: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (hasSearch) "لا يوجد مورد مطابق لبحثك" else "لا توجد فواتير أو حسابات موردين مسجلة",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasSearch) "جرب تعديل كلمات البحث" else "أضف فواتير موردين من شاشة فواتير الموردين لعرض الملخص هنا",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E98),
                textAlign = TextAlign.Center
            )
        }
    }
}
