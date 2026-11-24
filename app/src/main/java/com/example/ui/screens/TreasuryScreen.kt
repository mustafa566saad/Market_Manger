package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DailyTreasury
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.FawryBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.TreasuryUiState
import com.example.ui.viewmodel.TreasuryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Gold accent theme matching Card 5 on Dashboard
private val GoldPrimary = Color(0xFFFBBF24)
private val GoldStrong = Color(0xFFD97706)
private val GoldDarkBg = Color(0xFF241B0A)
private val DarkCardBg = Color(0xFF141418)
private val DarkCardElevated = Color(0xFF1E1E24)
private val DarkCardBorder = Color(0xFF2B2B36)
private val SkyAccent = Color(0xFF60A5FA)

private val arabicDateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ar"))
private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))

/**
 * [TASK 6: DAILY TREASURY & SHIFT CLOSING SCREEN]
 * Implements Shift Closing (Actual Cash Counted, live SQL calculations, Fawry exclusion,
 * save/update with confirmation, retroactive recalculation) and Monthly History & Sales Records
 * with bottom monthly totals.
 */
@Composable
fun TreasuryScreen(
    viewModel: TreasuryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color.Black,
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                MonthlyTreasuryBottomStatsBar(
                    monthLabel = state.historyYearMonth.format(arabicMonthFormatter),
                    totalMonthlySales = state.totalMonthlySales,
                    totalMonthlyProfit = state.totalMonthlyProfit,
                    closedDaysCount = state.monthlyHistory.size,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black)
                    .testTag("treasury_screen"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.errorMessage != null) {
                    item {
                        TreasuryErrorBanner(
                            message = state.errorMessage ?: "",
                            onRetry = { viewModel.refreshAll() }
                        )
                    }
                }

                // 1. Shift Closing Card
                item {
                    ShiftClosingSectionCard(
                        state = state,
                        onDatePicked = { viewModel.onDateSelected(it) },
                        onCashChanged = { viewModel.onActualCashInputChanged(it) },
                        onSaveClick = { viewModel.onSaveOrUpdateClicked() },
                        onRetroactiveClick = { viewModel.requestRetroactiveRecalculation() }
                    )
                }

                // 2. Monthly History & Sales Records Header
                item {
                    MonthlyHistoryFilterCard(
                        monthLabel = state.historyYearMonth.format(arabicMonthFormatter),
                        count = state.monthlyHistory.size,
                        isLoading = state.isLoadingHistory,
                        onPrevMonth = { viewModel.prevHistoryMonth() },
                        onNextMonth = { viewModel.nextHistoryMonth() }
                    )
                }

                // Closed Days History List
                if (state.isLoadingHistory && state.monthlyHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldPrimary)
                        }
                    }
                } else if (state.monthlyHistory.isEmpty()) {
                    item {
                        EmptyTreasuryHistoryCard(
                            monthLabel = state.historyYearMonth.format(arabicMonthFormatter)
                        )
                    }
                } else {
                    items(state.monthlyHistory, key = { it.id }) { treasury ->
                        ClosedDayHistoryCard(
                            treasury = treasury,
                            isSelected = treasury.date == state.selectedDate,
                            onSelectDay = { viewModel.selectClosedDayForEditing(treasury) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    // Update Closed Day Confirmation Dialog
    if (state.showUpdateConfirmDialog) {
        UpdateClosedDayConfirmDialog(
            date = state.selectedDate,
            newSales = state.calculatedDailySales,
            newProfit = state.calculatedNetProfit,
            onDismiss = { viewModel.dismissUpdateConfirmDialog() },
            onConfirm = { viewModel.confirmUpdateClosedDay() }
        )
    }

    // Retroactive Recalculation Confirmation Dialog
    if (state.showRetroactiveConfirmDialog) {
        RetroactiveAdjustmentDialog(
            onDismiss = { viewModel.dismissRetroactiveConfirmDialog() },
            onConfirm = { viewModel.confirmRetroactiveRecalculation() }
        )
    }
}

// =============================================================================
// 1. SHIFT CLOSING SECTION
// =============================================================================

@Composable
private fun ShiftClosingSectionCard(
    state: TreasuryUiState,
    onDatePicked: (LocalDate) -> Unit,
    onCashChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRetroactiveClick: () -> Unit
) {
    val context = LocalContext.current
    val datePickerDialog = remember(state.selectedDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDatePicked(LocalDate.of(year, month + 1, dayOfMonth))
            },
            state.selectedDate.year,
            state.selectedDate.monthValue - 1,
            state.selectedDate.dayOfMonth
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.55f), DarkCardBorder)),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header + Closed Day Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تقفيل الوردية والخزنة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "احتساب المبيعات تلقائياً من كاش الدرج",
                            fontSize = 11.5.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }

                if (state.isDayAlreadyClosed) {
                    Surface(
                        color = ProfitGreen.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.border(1.dp, ProfitGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مقفل مسبقاً",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }

            // Date Picker Trigger
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { datePickerDialog.show() }
                    .testTag("pick_date_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "تاريخ الوردية / اليومية",
                                fontSize = 10.5.sp,
                                color = Color(0xFFA1A1AA)
                            )
                            Text(
                                text = state.selectedDate.format(arabicDateFormatter),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "تغيير",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }

            // Input Field: Actual Physical Cash Counted
            OutlinedTextField(
                value = state.actualCashInput,
                onValueChange = onCashChanged,
                label = { Text("الكاش الفعلي الموجود في الدرج الآن (ج.م)") },
                placeholder = { Text("أدخل المبلغ المعدود في الدرج...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = GoldPrimary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCardElevated,
                    unfocusedContainerColor = DarkCardElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cash_in_drawer_input")
            )

            // Real-Time Calculated Equation Card
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معادلة احتساب المبيعات والأرباح",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        if (state.isLoadingPreview) {
                            CircularProgressIndicator(
                                color = GoldPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // 1. Previous Cash (-)
                    FormulaRow(
                        label = "كاش الأمس المرحل (-)",
                        value = "- ${formatCurrency(state.previousCash)}",
                        valueColor = SkyAccent
                    )

                    // 2. Actual Cash Counted (+)
                    FormulaRow(
                        label = "الكاش الفعلي في الدرج (+)",
                        value = formatCurrency(state.parsedActualCash ?: 0.0),
                        valueColor = Color.White
                    )

                    // 3. Total Daily Expenses (+)
                    FormulaRow(
                        label = "إجمالي مصروفات اليوم (+)",
                        value = "+ ${formatCurrency(state.totalDailyExpenses)}",
                        valueColor = ExpenseRed
                    )

                    // 4. Total Supplier Cash Payments (+) (Strictly excluding Fawry)
                    FormulaRow(
                        label = "مدفوعات الموردين كاش (+)",
                        value = "+ ${formatCurrency(state.totalSupplierCashPayments)}",
                        valueColor = AmberWarning
                    )

                    if (state.fawrySupplierPayments > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مدفوعات فوري / إلكتروني (مستبعدة من الدرج)",
                                fontSize = 11.sp,
                                color = FawryBlue
                            )
                            Text(
                                text = formatCurrency(state.fawrySupplierPayments),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FawryBlue
                            )
                        }
                    }

                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 2.dp))

                    // 5. Total Daily Sales (=) & Net Profit (10%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Daily Sales Result Box
                        Surface(
                            color = Color(0xFF121822),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, SkyAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "إجمالي مبيعات اليوم (=)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA1A1AA)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = formatCurrency(state.calculatedDailySales),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SkyAccent
                                )
                            }
                        }

                        // Net Profit 10% Result Box
                        Surface(
                            color = Color(0xFF0E2116),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, ProfitGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "صافي الربح (10%)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA1A1AA)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = formatCurrency(state.calculatedNetProfit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }
                }
            }

            // Primary Action Button: "حفظ / تعديل اليومية"
            Button(
                onClick = onSaveClick,
                enabled = state.hasValidCashInput && !state.isSavingClosing && !state.isRunningRetroactive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF24242C),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_treasury_button")
            ) {
                if (state.isSavingClosing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ / تعديل اليومية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Secondary Action Button: "تسوية وإعادة حساب اليوميات القديمة 🔄"
            OutlinedButton(
                onClick = onRetroactiveClick,
                enabled = !state.isSavingClosing && !state.isRunningRetroactive,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldStrong.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retroactive_adjustment_button")
            ) {
                if (state.isRunningRetroactive) {
                    CircularProgressIndicator(color = GoldPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري التسوية بأثر رجعي...", fontSize = 13.5.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تسوية وإعادة حساب اليوميات القديمة 🔄",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FormulaRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = Color(0xFFD4D4D8)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// =============================================================================
// 2. MONTHLY HISTORY & SALES RECORDS
// =============================================================================

@Composable
private fun MonthlyHistoryFilterCard(
    monthLabel: String,
    count: Int,
    isLoading: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سجل اليوميات والمبيعات الشهرية ($count يوم)",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = GoldPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Month/Year Switcher
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
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
                            .testTag("treasury_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الشهر السابق",
                            tint = GoldPrimary
                        )
                    }

                    Text(
                        text = monthLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("treasury_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "الشهر التالي",
                            tint = GoldPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedDayHistoryCard(
    treasury: DailyTreasury,
    isSelected: Boolean,
    onSelectDay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) GoldPrimary else DarkCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelectDay() }
            .testTag("treasury_history_item_${treasury.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Date + 10% Profit Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = treasury.date.dayOfMonth.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = treasury.date.format(shortDateFormatter),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = treasury.date.format(DateTimeFormatter.ofPattern("EEEE", Locale("ar"))),
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E98)
                        )
                    }
                }

                // 10% Profit Badge
                Surface(
                    color = ProfitGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ربح: ${formatCurrency(treasury.profit)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                }
            }

            // Metrics Row: Total Sales, Expenses, Supplier Payments
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryMiniMetric(
                        label = "إجمالي المبيعات",
                        value = formatCurrency(treasury.totalSales),
                        color = SkyAccent,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkCardBorder))
                    HistoryMiniMetric(
                        label = "المصروفات",
                        value = formatCurrency(treasury.totalExpenses),
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkCardBorder))
                    HistoryMiniMetric(
                        label = "مدفوعات الموردين",
                        value = formatCurrency(treasury.totalSupplierPayments),
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryMiniMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFA1A1AA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =============================================================================
// BOTTOM STATISTICS BAR
// =============================================================================

@Composable
private fun MonthlyTreasuryBottomStatsBar(
    monthLabel: String,
    totalMonthlySales: Double,
    totalMonthlyProfit: Double,
    closedDaysCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkCardBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = GoldPrimary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .testTag("treasury_bottom_stats_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SkyAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي مبيعات الشهر",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(totalMonthlySales),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SkyAccent
                        )
                    }
                }

                Surface(
                    color = GoldDarkBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ProfitGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي أرباح الشهر (10%)",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(totalMonthlyProfit),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ProfitGreen
                        )
                    }
                }
            }

            Text(
                text = "شهر $monthLabel — $closedDaysCount يومية مقفولة",
                fontSize = 10.5.sp,
                color = Color(0xFF8E8E98),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =============================================================================
// CONFIRMATION DIALOGS & BANNERS
// =============================================================================

@Composable
private fun UpdateClosedDayConfirmDialog(
    date: LocalDate,
    newSales: Double,
    newProfit: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = GoldPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد تعديل يومية مقفولة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "تم تقفيل يوم ${date.format(shortDateFormatter)} مسبقاً. هل تريد تحديث بيانات هذا اليوم بالقيم الجديدة؟",
                    color = Color.White,
                    fontSize = 13.5.sp
                )
                Text(
                    text = "• المبيعات الجديدة: ${formatCurrency(newSales)}\n• صافي الربح (10%): ${formatCurrency(newProfit)}",
                    color = GoldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_update_treasury_button")
            ) {
                Text("تأكيد التحديث", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFFA1A1AA))
            }
        }
    )
}

@Composable
private fun RetroactiveAdjustmentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = GoldPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسوية اليوميات القديمة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ستقوم هذه العملية بمراجعة كافة اليوميات المقفولة في قاعدة البيانات ومطابقتها مع فواتير الموردين النقدية والمصروفات الفعلية المسجلة لكل يوم.",
                    color = Color.White,
                    fontSize = 13.5.sp
                )
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, ProfitGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "يتم الحفاظ على صافي رصيد الدرج الفعلي كما هو لمنع أي عجز وهمي في الحسابات.",
                            color = Color(0xFFBBF7D0),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_retroactive_button")
            ) {
                Text("بدء التسوية الآن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFFA1A1AA))
            }
        }
    )
}

@Composable
private fun EmptyTreasuryHistoryCard(monthLabel: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لا توجد يوميات مقفولة خلال شهر $monthLabel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TreasuryErrorBanner(
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 12.5.sp,
                color = Color(0xFFFECDD3),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
            ) {
                Text("إعادة", fontSize = 12.sp)
            }
        }
    }
}
