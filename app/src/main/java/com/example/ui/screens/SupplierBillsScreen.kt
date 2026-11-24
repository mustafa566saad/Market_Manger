package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.SupplierBill
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.SupplierBillsUiState
import com.example.ui.viewmodel.SupplierBillsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Light Green accent theme for Supplier Bills (matching Card 2 from Dashboard)
private val LightGreenPrimary = Color(0xFF4ADE80)
private val LightGreenDark = Color(0xFF16A34A)
private val FawryBadgeColor = Color(0xFFA855F7) // Violet/Purple for Fawry electronic payments
private val DarkCardBg = Color(0xFF141418)
private val DarkCardElevated = Color(0xFF1E1E24)
private val DarkCardBorder = Color(0xFF2A2A32)

private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))
private val arabicDateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ar"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierBillsScreen(
    viewModel: SupplierBillsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color.Black,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openCreateBillDialog() },
                    containerColor = LightGreenPrimary,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_supplier_bill_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فاتورة جديدة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
            ) {
                // Top Filter Header (Month Selector + Supplier Filter)
                TopFilterSection(
                    state = state,
                    viewModel = viewModel
                )

                // Bills Ledger List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (state.isLoadingBills && state.bills.isEmpty()) {
                        CircularProgressIndicator(
                            color = LightGreenPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (state.bills.isEmpty()) {
                        EmptyBillsView(
                            monthStr = state.selectedYearMonth.format(arabicMonthFormatter),
                            onAddClick = { viewModel.openCreateBillDialog() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.bills, key = { it.id }) { bill ->
                                SupplierBillCard(
                                    bill = bill,
                                    onDelete = { viewModel.requestDeleteBill(bill) }
                                )
                            }
                            // Bottom spacer for FAB and stats bar
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Persistent Bottom Stats Bar
                PersistentBottomStatsBar(state = state)
            }
        }
    }

    // New Bill Creation Dialog / Modal
    if (state.showCreateBillDialog) {
        NewBillDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissCreateBillDialog() }
        )
    }

    // Delete Bill Confirmation Dialog
    state.billToDelete?.let { bill ->
        DeleteBillConfirmationDialog(
            bill = bill,
            isDeleting = state.isDeletingBill,
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = { viewModel.confirmDeleteBill() }
        )
    }
}

// =============================================================================
// TOP FILTER SECTION (MONTH / YEAR + SUPPLIER FILTER)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopFilterSection(
    state: SupplierBillsUiState,
    viewModel: SupplierBillsViewModel
) {
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Month Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.prevMonth() },
                    modifier = Modifier.testTag("supplier_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الشهر السابق",
                        tint = LightGreenPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = LightGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.selectedYearMonth.format(arabicMonthFormatter),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.nextMonth() },
                    modifier = Modifier.testTag("supplier_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "الشهر التالي",
                        tint = LightGreenPrimary
                    )
                }
            }

            // Supplier Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.selectedSupplierFilter ?: "جميع الموردين والتجار (الكل)",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = LightGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkCardElevated,
                            unfocusedContainerColor = DarkCardElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LightGreenPrimary,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("supplier_filter_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = supplierDropdownExpanded,
                        onDismissRequest = { supplierDropdownExpanded = false },
                        modifier = Modifier.background(DarkCardElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("جميع الموردين والتجار (الكل)", fontWeight = FontWeight.Bold, color = LightGreenPrimary) },
                            onClick = {
                                viewModel.setSupplierFilter(null)
                                supplierDropdownExpanded = false
                            }
                        )

                        state.supplierSuggestions.forEach { supplierName ->
                            DropdownMenuItem(
                                text = { Text(supplierName, color = Color.White) },
                                onClick = {
                                    viewModel.setSupplierFilter(supplierName)
                                    supplierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.loadBills()
                        viewModel.loadCumulativeDebt()
                        viewModel.loadDistinctSuppliers()
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCardElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث البيانات",
                        tint = LightGreenPrimary
                    )
                }
            }
        }
    }
}

// =============================================================================
// BILL ITEM CARD
// =============================================================================

@Composable
private fun SupplierBillCard(
    bill: SupplierBill,
    onDelete: () -> Unit
) {
    val remaining = bill.remainingAmount
    val isFullyPaid = remaining <= 0.001

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (bill.isFawry) FawryBadgeColor.copy(alpha = 0.4f) else LightGreenPrimary.copy(alpha = 0.35f),
                    DarkCardBorder
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bill_card_${bill.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Supplier Name & Fawry Badge / Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (bill.isFawry) FawryBadgeColor.copy(alpha = 0.2f)
                                else LightGreenPrimary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (bill.isFawry) Icons.Default.ElectricBolt else Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (bill.isFawry) FawryBadgeColor else LightGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bill.supplierName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = bill.billDate.format(shortDateFormatter),
                            fontSize = 11.5.sp,
                            color = Color(0xFFA0A0AB)
                        )
                    }
                }

                // Delete Button & Fawry Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bill.isFawry) {
                        Surface(
                            color = FawryBadgeColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(FawryBadgeColor, FawryBadgeColor)))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = FawryBadgeColor, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "فوري / إلكتروني",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FawryBadgeColor
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = LightGreenPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "نقدي من الدرج",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightGreenPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الفاتورة",
                            tint = ExpenseRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Grid: Total, Paid, Remaining
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "إجمالي الفاتورة", fontSize = 11.sp, color = Color(0xFFA0A0AB))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(bill.totalAmount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkCardBorder))

                    // Paid
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "المدفوع", fontSize = 11.sp, color = Color(0xFFA0A0AB))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(bill.paidAmount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.isFawry) FawryBadgeColor else ProfitGreen
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkCardBorder))

                    // Remaining
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "المتبقي (آجل)", fontSize = 11.sp, color = Color(0xFFA0A0AB))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFullyPaid) "خالص ✓" else formatCurrency(remaining),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyPaid) ProfitGreen else ExpenseRed
                        )
                    }
                }
            }

            // Notes if any
            if (!bill.cleanNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظات: ${bill.cleanNotes}",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9EA8),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =============================================================================
// NEW BILL CREATION DIALOG (FORM)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewBillDialog(
    state: SupplierBillsUiState,
    viewModel: SupplierBillsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val datePickerDialog = remember(state.billDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.onBillDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
            },
            state.billDate.year,
            state.billDate.monthValue - 1,
            state.billDate.dayOfMonth
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = LightGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "تسجيل فاتورة مورد جديدة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Supplier Name Input with Suggestions Dropdown
                item {
                    Column {
                        OutlinedTextField(
                            value = state.supplierNameInput,
                            onValueChange = { viewModel.onSupplierNameChanged(it) },
                            label = { Text("اسم المورد أو الشركة *") },
                            placeholder = { Text("مثال: شيبسي، كوكاكولا، جهينة...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkCardBg,
                                unfocusedContainerColor = DarkCardBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LightGreenPrimary,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supplier_name_input")
                        )

                        // Autocomplete suggestions chips
                        if (state.filteredSuggestions.isNotEmpty() && state.supplierNameInput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.filteredSuggestions.take(3).forEach { suggestion ->
                                    Surface(
                                        color = DarkCardBorder,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.selectSupplierSuggestion(suggestion) }
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.5.sp,
                                            color = LightGreenPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Real-time Old Debt Indicator
                item {
                    AnimatedVisibility(visible = state.supplierNameInput.isNotBlank()) {
                        Surface(
                            color = DarkCardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        if ((state.supplierOldDebt ?: 0.0) > 0) AmberWarning.copy(alpha = 0.5f) else ProfitGreen.copy(alpha = 0.5f),
                                        DarkCardBorder
                                    )
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isLoadingSupplierDebt) {
                                    CircularProgressIndicator(
                                        color = LightGreenPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جاري مراجعة ديون المورد في قاعدة البيانات...",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    val debt = state.supplierOldDebt ?: 0.0
                                    Icon(
                                        imageVector = if (debt > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (debt > 0) AmberWarning else ProfitGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (debt > 0) "ديون سابقة مستحقة لهذا المورد:" else "حساب المورد خالص:",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFA0A0AB)
                                        )
                                        Text(
                                            text = if (debt > 0) formatCurrency(debt) else "لا توجد ديون قديمة (0.00 ج.م)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (debt > 0) AmberWarning else ProfitGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Total & Paid Amounts
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.totalAmountInput,
                            onValueChange = { viewModel.onTotalAmountChanged(it) },
                            label = { Text("إجمالي الفاتورة *") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkCardBg,
                                unfocusedContainerColor = DarkCardBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LightGreenPrimary,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("total_amount_input")
                        )

                        OutlinedTextField(
                            value = state.paidAmountInput,
                            onValueChange = { viewModel.onPaidAmountChanged(it) },
                            label = { Text("المبلغ المدفوع") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkCardBg,
                                unfocusedContainerColor = DarkCardBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LightGreenPrimary,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paid_amount_input")
                        )
                    }
                }

                // Quick Pay in full chip & Calculated Remaining card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = DarkCardBorder,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { viewModel.setPaidInFull() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = LightGreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "سداد كامل الفاتورة كاش",
                                    fontSize = 11.5.sp,
                                    color = LightGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Calculated Remaining preview
                        Text(
                            text = "المتبقي الآجل: ${formatCurrency(state.remainingAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.remainingAmount <= 0) ProfitGreen else ExpenseRed
                        )
                    }
                }

                // 4. Bill Date Picker
                item {
                    Surface(
                        color = DarkCardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                            .testTag("bill_date_picker_trigger")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = LightGreenPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.billDate.format(arabicDateFormatter),
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text("تغيير", fontSize = 12.sp, color = LightGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 5. Fawry Toggle Switch (المدفوع إلكترونياً / فوري)
                item {
                    Surface(
                        color = if (state.isFawry) FawryBadgeColor.copy(alpha = 0.12f) else DarkCardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    if (state.isFawry) FawryBadgeColor.copy(alpha = 0.5f) else DarkCardBorder,
                                    DarkCardBorder
                                )
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onIsFawryChanged(!state.isFawry) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = if (state.isFawry) FawryBadgeColor else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "سداد إلكتروني / فوري (لا يُخصم من درج الكاش)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isFawry) FawryBadgeColor else Color.White
                                    )
                                    Text(
                                        text = "لن يتم خصم هذا المبلغ من كاش الدرج الفعلي عند تقفيل اليومية.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA0A0AB)
                                    )
                                }
                            }

                            Switch(
                                checked = state.isFawry,
                                onCheckedChange = { viewModel.onIsFawryChanged(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = FawryBadgeColor,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.testTag("is_fawry_switch")
                            )
                        }
                    }
                }

                // 6. Notes Input
                item {
                    OutlinedTextField(
                        value = state.notesInput,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label = { Text("ملاحظات الفاتورة (اختياري)") },
                        placeholder = { Text("أرقام الأصناف، رقم إذن الاستلام...") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkCardBg,
                            unfocusedContainerColor = DarkCardBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LightGreenPrimary,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveBill() },
                enabled = state.isFormValid && !state.isSavingBill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightGreenPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF24242C),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_bill_button")
            ) {
                if (state.isSavingBill) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري الحفظ والترحيل...")
                } else {
                    Text("حفظ الفاتورة وترحيل الحساب", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFFA0A0AB))
            }
        }
    )
}

// =============================================================================
// DELETE BILL CONFIRMATION DIALOG
// =============================================================================

@Composable
private fun DeleteBillConfirmationDialog(
    bill: SupplierBill,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = ExpenseRed,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد حذف الفاتورة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "هل أنت متأكد من حذف فاتورة المورد \"${bill.supplierName}\"؟",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "إجمالي الفاتورة: ${formatCurrency(bill.totalAmount)} - المدفوع: ${formatCurrency(bill.paidAmount)}",
                    color = Color(0xFFA0A0AB),
                    fontSize = 12.5.sp
                )
                Text(
                    text = "⚠️ سيتم إعادة احتساب اليومية والخزينة تلقائياً لهذا اليوم.",
                    color = AmberWarning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delete_bill_button")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFFA0A0AB))
            }
        }
    )
}

// =============================================================================
// PERSISTENT BOTTOM STATS BAR
// =============================================================================

@Composable
private fun PersistentBottomStatsBar(state: SupplierBillsUiState) {
    val displayDebt = state.filteredSupplierCumulativeDebt ?: state.allTimeCumulativeDebt
    val debtLabel = if (state.selectedSupplierFilter != null) {
        "ديون ${state.selectedSupplierFilter}"
    } else {
        "إجمالي ديون الموردين"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkCardBorder, Color.Black))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1: All-time / Supplier Cumulative Debt
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(ExpenseRed.copy(alpha = 0.35f), DarkCardBorder))
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = debtLabel,
                        fontSize = 11.sp,
                        color = Color(0xFFA0A0AB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrency(displayDebt),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }

            // Stat 2: Monthly Purchases Total
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(LightGreenPrimary.copy(alpha = 0.35f), DarkCardBorder))
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "مشتريات الشهر الحالي",
                        fontSize = 11.sp,
                        color = Color(0xFFA0A0AB),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrency(state.monthlyPurchasesTotal),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightGreenPrimary
                    )
                }
            }

            // Stat 3: Selected Supplier Monthly / Bills Count
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.selectedSupplierFilter != null) "مشتريات المورد شهرياً" else "عدد الفواتير",
                        fontSize = 11.sp,
                        color = Color(0xFFA0A0AB),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.selectedSupplierFilter != null) {
                            formatCurrency(state.selectedSupplierMonthlyPurchases)
                        } else {
                            "${state.bills.size} فاتورة"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBillsView(
    monthStr: String,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "لا توجد فواتير موردين مسجلة في شهر $monthStr",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "اضغط على زر \"فاتورة جديدة\" لتسجيل مشتريات البضائع وحسابات التجار",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = LightGreenPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تسجيل أول فاتورة", fontWeight = FontWeight.Bold)
        }
    }
}
