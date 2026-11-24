package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategorySummary
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.CUSTOM_EXPENSE_TYPE_OPTION
import com.example.ui.viewmodel.EXPENSE_TYPE_PRESETS
import com.example.ui.viewmodel.ExpensesUiState
import com.example.ui.viewmodel.ExpensesViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Light Coral accent theme matching Card 4 on Dashboard
private val CoralPrimary = Color(0xFFFB7185)
private val CoralStrong = Color(0xFFE11D48)
private val CoralDarkBg = Color(0xFF281018)
private val DarkCardBg = Color(0xFF141418)
private val DarkCardElevated = Color(0xFF1E1E24)
private val DarkCardBorder = Color(0xFF2A2A34)

private val arabicDateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ar"))
private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))

private fun formatEgpFull(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return "${formatter.format(amount)} جنيه"
}

/**
 * [TASK 5: DAILY EXPENSES & MONTHLY EXPENSES BREAKDOWN]
 * Dark AMOLED RTL interface with Expense Recording Form (presets + custom category),
 * Daily Expenses Ledger with Delete + Treasury Auto-Sync, and Monthly Category Breakdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel = viewModel(),
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
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black)
                    .testTag("expenses_screen"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Highlighted Monthly Expenses Total Badge
                item {
                    MonthlyTotalHighlightedBadge(
                        monthLabel = state.breakdownYearMonth.format(arabicMonthFormatter),
                        monthlyTotal = state.monthlyTotalAmount,
                        dailyTotal = state.dailyTotalAmount,
                        ledgerDate = state.ledgerDate
                    )
                }

                // Error Banner if any
                if (state.errorMessage != null) {
                    item {
                        ExpenseErrorBanner(
                            message = state.errorMessage ?: "",
                            onRetry = { viewModel.refreshAll() }
                        )
                    }
                }

                // 1. Expense Recording Section
                item {
                    ExpenseRecordingCard(
                        state = state,
                        onPresetSelected = { viewModel.selectPresetType(it) },
                        onCustomTypeChanged = { viewModel.onCustomTypeChanged(it) },
                        onAmountChanged = { viewModel.onAmountChanged(it) },
                        onDateChanged = { viewModel.onFormDateChanged(it) },
                        onNotesChanged = { viewModel.onNotesChanged(it) },
                        onSubmit = { viewModel.submitExpense() }
                    )
                }

                // 2. Daily Expenses Ledger Header
                item {
                    DailyLedgerHeader(
                        ledgerDate = state.ledgerDate,
                        dailyTotal = state.dailyTotalAmount,
                        count = state.dailyExpenses.size,
                        onPrevDay = { viewModel.prevLedgerDay() },
                        onNextDay = { viewModel.nextLedgerDay() },
                        onDatePicked = { viewModel.setLedgerDate(it) }
                    )
                }

                // Daily Expenses List Items
                if (state.isLoadingDaily && state.dailyExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CoralPrimary)
                        }
                    }
                } else if (state.dailyExpenses.isEmpty()) {
                    item {
                        EmptyDailyExpensesCard(date = state.ledgerDate)
                    }
                } else {
                    items(state.dailyExpenses, key = { it.id }) { expense ->
                        DailyExpenseItemCard(
                            expense = expense,
                            onDelete = { viewModel.requestDeleteExpense(expense) }
                        )
                    }
                }

                // 3. Monthly Category Breakdown Section
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    MonthlyBreakdownHeaderCard(
                        monthLabel = state.breakdownYearMonth.format(arabicMonthFormatter),
                        monthlyTotal = state.monthlyTotalAmount,
                        isLoading = state.isLoadingMonthly,
                        onPrevMonth = { viewModel.prevBreakdownMonth() },
                        onNextMonth = { viewModel.nextBreakdownMonth() }
                    )
                }

                if (state.monthlyBreakdown.isEmpty() && !state.isLoadingMonthly) {
                    item {
                        EmptyMonthlyBreakdownCard(
                            monthLabel = state.breakdownYearMonth.format(arabicMonthFormatter)
                        )
                    }
                } else {
                    items(
                        items = state.monthlyBreakdown,
                        key = { "cat_${it.expenseType}" }
                    ) { categorySummary ->
                        MonthlyCategoryBreakdownCard(
                            summary = categorySummary,
                            monthlyTotal = state.monthlyTotalAmount
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Delete Expense Confirmation Dialog
    state.expenseToDelete?.let { expense ->
        DeleteExpenseConfirmDialog(
            expense = expense,
            isDeleting = state.isDeletingExpense,
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = { viewModel.confirmDeleteExpense() }
        )
    }
}

// =============================================================================
// HIGHLIGHTED BADGE: "إجمالي مصاريف الشهر: X.XX جنيه"
// =============================================================================

@Composable
private fun MonthlyTotalHighlightedBadge(
    monthLabel: String,
    monthlyTotal: Double,
    dailyTotal: Double,
    ledgerDate: LocalDate
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CoralDarkBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(listOf(CoralPrimary, CoralStrong.copy(alpha = 0.45f))),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("monthly_expenses_badge")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CoralPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = null,
                            tint = CoralPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي مصاريف الشهر: ${formatEgpFull(monthlyTotal)}",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "ملخص مصروفات شهر $monthLabel",
                            fontSize = 11.5.sp,
                            color = Color(0xFFFDA4AF)
                        )
                    }
                }
            }

            // Secondary row: Selected day's total
            Surface(
                color = Color(0xFF141013),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CoralStrong.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مصاريف يوم ${ledgerDate.format(shortDateFormatter)}:",
                        fontSize = 12.sp,
                        color = Color(0xFFD4D4D8)
                    )
                    Text(
                        text = formatEgpFull(dailyTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralPrimary
                    )
                }
            }
        }
    }
}

// =============================================================================
// 1. EXPENSE RECORDING SECTION
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseRecordingCard(
    state: ExpensesUiState,
    onPresetSelected: (String) -> Unit,
    onCustomTypeChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val datePickerDialog = remember(state.formDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
            },
            state.formDate.year,
            state.formDate.monthValue - 1,
            state.formDate.dayOfMonth
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = CoralPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "تسجيل مصروف جديد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Expense Type Dropdown (Presets + Custom Category)
            Column {
                Text(
                    text = "نوع المصروف *",
                    fontSize = 12.5.sp,
                    color = Color(0xFFA1A1AA),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayLabel = if (state.isCustomType) {
                        CUSTOM_EXPENSE_TYPE_OPTION
                    } else {
                        state.selectedPresetType
                    }

                    OutlinedTextField(
                        value = displayLabel,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = expenseIconForType(state.effectiveExpenseType),
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkCardElevated,
                            unfocusedContainerColor = DarkCardElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CoralPrimary,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("expense_type_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.background(DarkCardElevated)
                    ) {
                        EXPENSE_TYPE_PRESETS.forEach { preset ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = expenseIconForType(preset),
                                        contentDescription = null,
                                        tint = CoralPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = preset,
                                        color = Color.White,
                                        fontWeight = if (!state.isCustomType && state.selectedPresetType == preset) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onPresetSelected(preset)
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = CUSTOM_EXPENSE_TYPE_OPTION,
                                    color = AmberWarning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                onPresetSelected(CUSTOM_EXPENSE_TYPE_OPTION)
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Custom Category Text Field (when user selects custom)
            AnimatedVisibility(visible = state.isCustomType) {
                OutlinedTextField(
                    value = state.customTypeInput,
                    onValueChange = onCustomTypeChanged,
                    label = { Text("اكتب التصنيف المخصص *") },
                    placeholder = { Text("مثال: صيانة ثلاجات، أكياس تغليف...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardElevated,
                        unfocusedContainerColor = DarkCardElevated,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CoralPrimary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_custom_type_input")
                )
            }

            // Amount + Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text("المبلغ (جنيه) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardElevated,
                        unfocusedContainerColor = DarkCardElevated,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CoralPrimary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("expense_amount_input")
                )

                // Date Picker Trigger Box
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { datePickerDialog.show() }
                        .testTag("expense_date_picker_trigger")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التاريخ",
                                fontSize = 10.5.sp,
                                color = Color(0xFFA1A1AA)
                            )
                            Text(
                                text = state.formDate.format(shortDateFormatter),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "تغيير التاريخ",
                            tint = CoralPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Background Treasury Sync Notice when selected date is already closed
            AnimatedVisibility(visible = state.isFormDateClosedInTreasury) {
                Surface(
                    color = Color(0xFF281E0C),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmberWarning.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "هذا اليوم مقفول في الخزينة — سيتم تحديث مبيعات وصافي الخزينة تلقائياً للحفاظ على مطابقة الدرج.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }

            // Notes Input
            OutlinedTextField(
                value = state.notesInput,
                onValueChange = onNotesChanged,
                label = { Text("ملاحظات (اختياري)") },
                placeholder = { Text("تفاصيل الفاتورة أو سبب الصرف...") },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCardElevated,
                    unfocusedContainerColor = DarkCardElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CoralPrimary,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_notes_input")
            )

            // Submit Button: "تسجيل المصروف"
            Button(
                onClick = onSubmit,
                enabled = state.isFormValid && !state.isSavingExpense,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralStrong,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF24242C),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_expense_button")
            ) {
                if (state.isSavingExpense) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ والمزامنة...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل المصروف",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
// 2. DAILY EXPENSES LEDGER
// =============================================================================

@Composable
private fun DailyLedgerHeader(
    ledgerDate: LocalDate,
    dailyTotal: Double,
    count: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onDatePicked: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val datePicker = remember(ledgerDate) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onDatePicked(LocalDate.of(year, month + 1, day)) },
            ledgerDate.year,
            ledgerDate.monthValue - 1,
            ledgerDate.dayOfMonth
        )
    }

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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل مصروفات اليوم ($count)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = formatCurrency(dailyTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoralPrimary
                )
            }

            // Day Navigator
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
                        onClick = onPrevDay,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("expenses_prev_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "اليوم السابق",
                            tint = CoralPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { datePicker.show() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CoralPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ledgerDate.format(arabicDateFormatter),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("expenses_next_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "اليوم التالي",
                            tint = CoralPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyExpenseItemCard(
    expense: Expense,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .testTag("expense_item_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CoralPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = expenseIconForType(expense.expenseType),
                    contentDescription = null,
                    tint = CoralPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.expenseType,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!expense.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.notes,
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatCurrency(expense.amount),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CoralPrimary
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_expense_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف المصروف",
                    tint = ExpenseRed,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

// =============================================================================
// 3. MONTHLY CATEGORY BREAKDOWN SECTION
// =============================================================================

@Composable
private fun MonthlyBreakdownHeaderCard(
    monthLabel: String,
    monthlyTotal: Double,
    isLoading: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CoralPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
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
                    text = "توزيع مصروفات الشهر حسب التصنيف",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isLoading) {
                    CircularProgressIndicator(color = CoralPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = formatEgpFull(monthlyTotal),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralPrimary
                    )
                }
            }

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
                            .testTag("expenses_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الشهر السابق",
                            tint = CoralPrimary
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
                            .testTag("expenses_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "الشهر التالي",
                            tint = CoralPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyCategoryBreakdownCard(
    summary: ExpenseCategorySummary,
    monthlyTotal: Double
) {
    val ratio = if (monthlyTotal > 0.01) {
        (summary.totalAmount / monthlyTotal).toFloat().coerceIn(0f, 1f)
    } else 0f

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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = expenseIconForType(summary.expenseType),
                        contentDescription = null,
                        tint = CoralPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.expenseType,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = DarkCardElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${summary.count} عملية",
                            fontSize = 10.5.sp,
                            color = Color(0xFFA1A1AA),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = formatEgpFull(summary.totalAmount),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoralPrimary
                )
            }

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CoralPrimary,
                trackColor = Color(0xFF272733)
            )

            Text(
                text = "النسبة من مصاريف الشهر: ${String.format(Locale.US, "%.1f", ratio * 100f)}%",
                fontSize = 11.sp,
                color = Color(0xFF8E8E98)
            )
        }
    }
}

// =============================================================================
// DIALOGS & HELPERS
// =============================================================================

@Composable
private fun DeleteExpenseConfirmDialog(
    expense: Expense,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = DarkCardElevated,
        titleContentColor = ExpenseRed,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد حذف المصروف", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "هل أنت متأكد من حذف مصروف \"${expense.expenseType}\" بقيمة ${formatEgpFull(expense.amount)}؟",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "⚠️ إذا كان هذا اليوم مقفولاً في الخزينة، سيتم تحديث صافي الخزينة والمبيعات تلقائياً.",
                    color = AmberWarning,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delete_expense_button")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("إلغاء", color = Color(0xFFA1A1AA))
            }
        }
    )
}

@Composable
private fun EmptyDailyExpensesCard(date: LocalDate) {
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
                imageVector = Icons.Default.MoneyOff,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لا توجد مصروفات مسجلة في يوم ${date.format(shortDateFormatter)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "استخدم نموذج التسجيل أعلاه لإضافة أي مصروف لهذا اليوم",
                fontSize = 12.sp,
                color = Color(0xFF8E8E98),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyMonthlyBreakdownCard(monthLabel: String) {
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
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لا توجد مصروفات مسجلة خلال شهر $monthLabel",
                fontSize = 13.5.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExpenseErrorBanner(
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

private fun expenseIconForType(type: String): ImageVector {
    return when {
        type.contains("كهرباء") -> Icons.Default.Bolt
        type.contains("مياه") || type.contains("غاز") -> Icons.Default.WaterDrop
        type.contains("إيجار") -> Icons.Default.Home
        type.contains("مرتبات") || type.contains("عمال") -> Icons.Default.People
        type.contains("نثريات") -> Icons.Default.Payments
        else -> Icons.Default.Category
    }
}
