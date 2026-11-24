package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import com.example.data.model.Attendance
import com.example.data.model.Employee
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatTime
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.EmployeeTab
import com.example.ui.viewmodel.EmployeeUiState
import com.example.ui.viewmodel.EmployeeViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Sky Blue color accents matching the Dashboard Staff Card
private val SkyBluePrimary = Color(0xFF38BDF8)
private val SkyBlueDark = Color(0xFF0284C7)
private val DarkCardBg = Color(0xFF141418)
private val DarkCardElevated = Color(0xFF1E1E24)
private val DarkCardBorder = Color(0xFF2A2A32)

private val arabicTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))
private val arabicDateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale("ar"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: EmployeeViewModel = viewModel(),
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
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
            ) {
                // Header Tabs
                PrimaryTabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    containerColor = Color.Black,
                    contentColor = SkyBluePrimary,
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DarkCardBorder)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_tabs")
                ) {
                    EmployeeTab.entries.forEach { tab ->
                        val isSelected = state.selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = tab.titleAr,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SkyBluePrimary else Color(0xFF9E9EA8),
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (state.selectedTab) {
                        EmployeeTab.STAFF_DIRECTORY -> {
                            StaffDirectoryTab(
                                state = state,
                                viewModel = viewModel
                            )
                        }
                        EmployeeTab.LOG_SHIFT -> {
                            LogShiftTab(
                                state = state,
                                viewModel = viewModel
                            )
                        }
                        EmployeeTab.MONTHLY_TIMESHEET -> {
                            MonthlyTimesheetTab(
                                state = state,
                                viewModel = viewModel
                            )
                        }
                    }

                    if (state.isLoading && state.employees.isEmpty()) {
                        CircularProgressIndicator(
                            color = SkyBluePrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Employee Dialog
    if (state.showAddEmployeeDialog) {
        AddEditEmployeeDialog(
            employee = state.editingEmployee,
            isSaving = state.isSavingEmployee,
            onDismiss = { viewModel.dismissEmployeeDialog() },
            onSave = { name, rate, phone, isActive ->
                viewModel.saveEmployee(name, rate, phone, isActive)
            }
        )
    }

    // Edit Shift Dialog
    state.shiftToEdit?.let { shift ->
        EditShiftDialog(
            shift = shift,
            employees = state.employees,
            isSaving = state.isSavingShiftEdit,
            onDismiss = { viewModel.dismissEditShiftDialog() },
            onSave = { id, empId, checkIn, checkOut ->
                viewModel.saveEditedShift(id, empId, checkIn, checkOut)
            }
        )
    }

    // Delete Shift Confirmation Dialog
    state.shiftToDelete?.let { shift ->
        DeleteShiftConfirmDialog(
            shift = shift,
            isDeleting = state.isDeletingShift,
            onDismiss = { viewModel.dismissDeleteShiftDialog() },
            onConfirm = { viewModel.confirmDeleteShift() }
        )
    }
}

// =============================================================================
// TAB 1: STAFF DIRECTORY & ADD EMPLOYEE
// =============================================================================

@Composable
private fun StaffDirectoryTab(
    state: EmployeeUiState,
    viewModel: EmployeeViewModel
) {
    val activeCount = state.employees.count { it.isActive }
    val totalCount = state.employees.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Actions & Summary Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SkyBluePrimary.copy(alpha = 0.5f), DarkCardBorder))),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "فريق العمل والموظفين",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$activeCount موظف نشط من إجمالي $totalCount",
                            fontSize = 13.sp,
                            color = Color(0xFFA0A0AB)
                        )
                    }

                    Button(
                        onClick = { viewModel.openAddEmployeeDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBluePrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_employee_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة موظف", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Employee Cards List
        if (state.employees.isEmpty() && !state.isLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا يوجد موظفين مسجلين حالياً",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على زر \"إضافة موظف\" لبدء تسجيل بيانات الموظفين وسعر الساعة",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.employees, key = { it.id }) { employee ->
                EmployeeItemCard(
                    employee = employee,
                    onEdit = { viewModel.openEditEmployeeDialog(employee) },
                    onToggleActive = { viewModel.toggleEmployeeActive(employee) }
                )
            }
        }
    }
}

@Composable
private fun EmployeeItemCard(
    employee: Employee,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (employee.isActive) SkyBluePrimary.copy(alpha = 0.35f) else Color.Transparent,
                    DarkCardBorder
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_card_${employee.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (employee.isActive) SkyBluePrimary.copy(alpha = 0.2f)
                        else Color(0xFF2A2A32)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.employeeName.take(1),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (employee.isActive) SkyBluePrimary else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = employee.employeeName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (employee.isActive) ProfitGreen.copy(alpha = 0.2f) else Color(0xFF33333E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (employee.isActive) "نشط" else "متوقف",
                            color = if (employee.isActive) ProfitGreen else Color(0xFFA0A0AB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سعر الساعة: ${formatCurrency(employee.hourRate)}",
                        fontSize = 13.sp,
                        color = SkyBluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (employee.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFFA0A0AB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = employee.phone,
                                fontSize = 12.sp,
                                color = Color(0xFFA0A0AB)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_employee_${employee.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = SkyBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier.testTag("toggle_employee_${employee.id}")
                ) {
                    Icon(
                        imageVector = if (employee.isActive) Icons.Default.Close else Icons.Default.CheckCircle,
                        contentDescription = if (employee.isActive) "إيقاف" else "تنشيط",
                        tint = if (employee.isActive) AmberWarning else ProfitGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB 2: LOG WORK SHIFT (ATTENDANCE)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogShiftTab(
    state: EmployeeUiState,
    viewModel: EmployeeViewModel
) {
    val context = LocalContext.current
    var employeeMenuExpanded by remember { mutableStateOf(false) }

    val activeEmployees = state.employees.filter { it.isActive }

    // Date Picker Dialog setup
    val datePickerDialog = remember(state.shiftDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.updateShiftDate(LocalDate.of(year, month + 1, dayOfMonth))
            },
            state.shiftDate.year,
            state.shiftDate.monthValue - 1,
            state.shiftDate.dayOfMonth
        )
    }

    // CheckIn Time Picker Dialog setup
    val checkInTimeDialog = remember(state.checkInTime) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                viewModel.updateCheckInTime(LocalTime.of(hourOfDay, minute))
            },
            state.checkInTime.hour,
            state.checkInTime.minute,
            false
        )
    }

    // CheckOut Time Picker Dialog setup
    val checkOutTimeDialog = remember(state.checkOutTime) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                viewModel.updateCheckOutTime(LocalTime.of(hourOfDay, minute))
            },
            state.checkOutTime.hour,
            state.checkOutTime.minute,
            false
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkCardBorder, Color.Black))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "تسجيل وردية عمل جديدة",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // 1. Employee Selector Dropdown
                    Column {
                        Text(
                            text = "الموظف *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA0A0AB)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = employeeMenuExpanded,
                            onExpandedChange = { employeeMenuExpanded = !employeeMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state.shiftSelectedEmployee?.let { "${it.employeeName} (${formatCurrency(it.hourRate)}/ساعة)" }
                                    ?: "اختر الموظف...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkCardElevated,
                                    unfocusedContainerColor = DarkCardElevated,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SkyBluePrimary,
                                    unfocusedBorderColor = DarkCardBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("employee_dropdown")
                            )

                            ExposedDropdownMenu(
                                expanded = employeeMenuExpanded,
                                onDismissRequest = { employeeMenuExpanded = false },
                                modifier = Modifier.background(DarkCardElevated)
                            ) {
                                if (activeEmployees.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("لا يوجد موظفين نشطين", color = Color.Gray) },
                                        onClick = { employeeMenuExpanded = false }
                                    )
                                } else {
                                    activeEmployees.forEach { emp ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(emp.employeeName, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(formatCurrency(emp.hourRate), color = SkyBluePrimary)
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectShiftEmployee(emp)
                                                employeeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Date Picker Selector
                    Column {
                        Text(
                            text = "تاريخ الوردية *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA0A0AB)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = DarkCardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                                .testTag("shift_date_picker_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = SkyBluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = state.shiftDate.format(arabicDateFormatter),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "تغيير",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBluePrimary
                                )
                            }
                        }
                    }

                    // 3. Time Pickers (CheckIn & CheckOut side-by-side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Check In
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "وقت الحضور (Check-In)",
                                fontSize = 12.5.sp,
                                color = Color(0xFFA0A0AB)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = DarkCardElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkInTimeDialog.show() }
                                    .testTag("check_in_time_picker")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = state.checkInTime.format(arabicTimeFormatter),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Check Out
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "وقت الانصراف (Check-Out)",
                                fontSize = 12.5.sp,
                                color = Color(0xFFA0A0AB)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = DarkCardElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkOutTimeDialog.show() }
                                    .testTag("check_out_time_picker")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = state.checkOutTime.format(arabicTimeFormatter),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Overnight Shift Alert / Info Badge
                    AnimatedVisibility(visible = state.isOvernightShift) {
                        Surface(
                            color = Color(0xFF281E0C),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(AmberWarning.copy(alpha = 0.5f), AmberWarning.copy(alpha = 0.2f)))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "وردية ليلية ممتدة لليوم التالي",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                    Text(
                                        text = "وقت الانصراف قبل أو يساوي وقت الحضور، لذا تم احتساب الانصراف تلقائياً في اليوم التالي (+1 يوم).",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFE0C496)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Calculation Preview Card (معاينة الوردية الحية قبل الحفظ)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SkyBluePrimary, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة الحساب التلقائي للوردية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyBluePrimary
                        )

                        Text(
                            text = state.shiftSelectedEmployee?.employeeName ?: "لم يتم الاختيار",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Hours Stat
                        Surface(
                            color = DarkCardElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "إجمالي الساعات",
                                    fontSize = 12.sp,
                                    color = Color(0xFFA0A0AB)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2f س", state.calculatedTotalHours),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Total Shift Cost Stat
                        Surface(
                            color = DarkCardElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تكلفة الوردية (المستحق)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFA0A0AB)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(state.calculatedShiftCost),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "معادلة الاحتساب: الساعات (${String.format(Locale.US, "%.2f", state.calculatedTotalHours)}) × سعر الساعة (${formatCurrency(state.shiftSelectedEmployee?.hourRate ?: 0.0)})",
                        fontSize = 11.5.sp,
                        color = Color(0xFF8E8E98),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = { viewModel.submitWorkShift() },
                enabled = state.shiftSelectedEmployee != null && state.calculatedTotalHours > 0.0 && !state.isSubmittingShift,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyBluePrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF24242C),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_shift_button")
            ) {
                if (state.isSubmittingShift) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("جاري الحفظ في السيرفر...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل وحفظ الوردية في قاعدة البيانات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB 3: MONTHLY STAFF TIMESHEET & PAYROLL
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyTimesheetTab(
    state: EmployeeUiState,
    viewModel: EmployeeViewModel
) {
    var employeeFilterMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Filter Bar: Month Selector + Employee Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            shape = RoundedCornerShape(0.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.prevTimesheetMonth() },
                        modifier = Modifier.testTag("prev_month_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الشهر السابق", tint = SkyBluePrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.timesheetYearMonth.format(arabicMonthFormatter),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextTimesheetMonth() },
                        modifier = Modifier.testTag("next_month_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الشهر التالي", tint = SkyBluePrimary)
                    }
                }

                // Employee Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = employeeFilterMenuExpanded,
                        onExpandedChange = { employeeFilterMenuExpanded = !employeeFilterMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = state.timesheetEmployeeFilter?.employeeName ?: "جميع الموظفين (الكل)",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeFilterMenuExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkCardElevated,
                                unfocusedContainerColor = DarkCardElevated,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("timesheet_employee_filter")
                        )

                        ExposedDropdownMenu(
                            expanded = employeeFilterMenuExpanded,
                            onDismissRequest = { employeeFilterMenuExpanded = false },
                            modifier = Modifier.background(DarkCardElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("جميع الموظفين (الكل)", fontWeight = FontWeight.Bold, color = SkyBluePrimary) },
                                onClick = {
                                    viewModel.filterTimesheetByEmployee(null)
                                    employeeFilterMenuExpanded = false
                                }
                            )

                            state.employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text(emp.employeeName, color = Color.White) },
                                    onClick = {
                                        viewModel.filterTimesheetByEmployee(emp)
                                        employeeFilterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.loadMonthlyTimesheet() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCardElevated)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = SkyBluePrimary)
                    }
                }
            }
        }

        // Shifts List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.isLoadingTimesheet) {
                CircularProgressIndicator(
                    color = SkyBluePrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.timesheetShifts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد ورديات مسجلة في هذا الشهر",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "استخدم تبويب \"تسجيل وردية عمل\" لتسجيل ساعات الموظفين لهذا الشهر",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.timesheetShifts, key = { it.id }) { shift ->
                        ShiftTimesheetCard(
                            shift = shift,
                            onEdit = { viewModel.openEditShiftDialog(shift) },
                            onDelete = { viewModel.requestDeleteShift(shift) }
                        )
                    }
                }
            }
        }

        // Bottom Summary Cards Bar (إجمالي الساعات المسجلة & إجمالي المستحق المالي)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkCardBorder, Color.Black))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary 1: Total Hours
                    Surface(
                        color = DarkCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SkyBluePrimary.copy(alpha = 0.3f), DarkCardBorder))),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "إجمالي الساعات المسجلة",
                                fontSize = 11.5.sp,
                                color = Color(0xFFA0A0AB)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f ساعة", state.timesheetTotalHours),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SkyBluePrimary
                            )
                        }
                    }

                    // Summary 2: Total Wages
                    Surface(
                        color = DarkCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ProfitGreen.copy(alpha = 0.3f), DarkCardBorder))),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "إجمالي المستحق المالي (جنيه)",
                                fontSize = 11.5.sp,
                                color = Color(0xFFA0A0AB)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(state.timesheetTotalCost),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${state.timesheetShifts.size} وردية مسجلة خلال ${state.timesheetYearMonth.format(arabicMonthFormatter)}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ShiftTimesheetCard(
    shift: Attendance,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val checkIn = shift.checkIn
    val checkOut = shift.checkOut
    val isOvernight = checkOut != null && checkOut.toLocalDate().isAfter(checkIn.toLocalDate())

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shift_card_${shift.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day circle badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = checkIn.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = checkIn.format(DateTimeFormatter.ofPattern("EEE", Locale("ar"))),
                        fontSize = 10.sp,
                        color = SkyBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shift.employeeName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isOvernight) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = "وردية ليلية",
                            tint = AmberWarning,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val inStr = checkIn.format(arabicTimeFormatter)
                val outStr = checkOut?.format(arabicTimeFormatter) ?: "مستمر..."
                val nextDayTag = if (isOvernight) " (+1 يوم)" else ""

                Text(
                    text = "$inStr  ➔  $outStr$nextDayTag",
                    fontSize = 12.5.sp,
                    color = Color(0xFFA0A0AB)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", shift.totalHours)} ساعة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SkyBluePrimary
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formatCurrency(shift.shiftCost),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }

            // Edit & Delete Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = SkyBluePrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = ExpenseRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// DIALOGS: ADD/EDIT EMPLOYEE, EDIT SHIFT, DELETE CONFIRMATION
// =============================================================================

@Composable
private fun AddEditEmployeeDialog(
    employee: Employee?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, hourRate: Double, phone: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(employee?.employeeName ?: "") }
    var hourRateStr by remember { mutableStateOf(employee?.let { if (it.hourRate % 1.0 == 0.0) it.hourRate.toLong().toString() else it.hourRate.toString() } ?: "25") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var isActive by remember { mutableStateOf(employee?.isActive ?: true) }

    val isEditing = employee != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = Color.White,
        textContentColor = Color(0xFFA0A0AB),
        title = {
            Text(
                text = if (isEditing) "تعديل بيانات الموظف" else "إضافة موظف جديد",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الموظف *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardBg,
                        unfocusedContainerColor = DarkCardBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input")
                )

                OutlinedTextField(
                    value = hourRateStr,
                    onValueChange = { hourRateStr = it },
                    label = { Text("سعر الساعة (ج.م) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardBg,
                        unfocusedContainerColor = DarkCardBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_rate_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCardBg,
                        unfocusedContainerColor = DarkCardBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_phone_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isActive = !isActive }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isActive) "حالة الموظف: نشط ويعمل" else "حالة الموظف: غير نشط (متوقف)",
                        color = if (isActive) ProfitGreen else Color.Gray,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ProfitGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = hourRateStr.toDoubleOrNull() ?: 0.0
                    onSave(name, rate, phone, isActive)
                },
                enabled = name.isNotBlank() && (hourRateStr.toDoubleOrNull() ?: 0.0) > 0.0 && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyBluePrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_employee_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                    Text(if (isEditing) "حفظ التعديلات" else "إضافة الموظف", fontWeight = FontWeight.Bold)
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

@Composable
private fun EditShiftDialog(
    shift: Attendance,
    employees: List<Employee>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (id: Int, employeeId: Int, checkIn: LocalDateTime, checkOut: LocalDateTime) -> Unit
) {
    val context = LocalContext.current
    var shiftDate by remember { mutableStateOf(shift.checkIn.toLocalDate()) }
    var inTime by remember { mutableStateOf(shift.checkIn.toLocalTime()) }
    var outTime by remember { mutableStateOf(shift.checkOut?.toLocalTime() ?: shift.checkIn.toLocalTime().plusHours(8)) }

    val isOvernight = outTime <= inTime
    val calculatedIn = LocalDateTime.of(shiftDate, inTime)
    val calculatedOut = if (isOvernight) LocalDateTime.of(shiftDate.plusDays(1), outTime) else LocalDateTime.of(shiftDate, outTime)
    val totalHours = java.time.Duration.between(calculatedIn, calculatedOut).toMinutes() / 60.0

    val datePicker = remember(shiftDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth -> shiftDate = LocalDate.of(year, month + 1, dayOfMonth) },
            shiftDate.year,
            shiftDate.monthValue - 1,
            shiftDate.dayOfMonth
        )
    }

    val inTimePicker = remember(inTime) {
        TimePickerDialog(
            context,
            { _, hour, min -> inTime = LocalTime.of(hour, min) },
            inTime.hour,
            inTime.minute,
            false
        )
    }

    val outTimePicker = remember(outTime) {
        TimePickerDialog(
            context,
            { _, hour, min -> outTime = LocalTime.of(hour, min) },
            outTime.hour,
            outTime.minute,
            false
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardElevated,
        titleContentColor = Color.White,
        title = { Text("تعديل الوردية: ${shift.employeeName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date picker trigger
                Surface(
                    color = DarkCardBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePicker.show() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تاريخ الوردية:", fontSize = 13.sp, color = Color.Gray)
                        Text(shiftDate.format(arabicDateFormatter), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // In & Out Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = DarkCardBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { inTimePicker.show() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الحضور", fontSize = 11.5.sp, color = ProfitGreen)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(inTime.format(arabicTimeFormatter), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Surface(
                        color = DarkCardBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { outTimePicker.show() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الانصراف", fontSize = 11.5.sp, color = SkyBluePrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(outTime.format(arabicTimeFormatter), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (isOvernight) {
                    Text(
                        text = "🌙 وردية ليلية: الانصراف باليوم التالي تلقائياً (+1 يوم)",
                        fontSize = 11.5.sp,
                        color = AmberWarning
                    )
                }

                Text(
                    text = "إجمالي الساعات المحسوبة: ${String.format(Locale.US, "%.2f", totalHours)} ساعة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBluePrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(shift.id, shift.employeeId, calculatedIn, calculatedOut) },
                enabled = totalHours > 0.0 && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                    Text("حفظ التعديل", fontWeight = FontWeight.Bold)
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

@Composable
private fun DeleteShiftConfirmDialog(
    shift: Attendance,
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
                Text("تأكيد حذف الوردية", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "هل أنت متأكد من حذف وردية الموظف \"${shift.employeeName}\"؟",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "تاريخ الوردية: ${shift.checkIn.format(shortDateFormatter)} - مدة الوردية: ${String.format(Locale.US, "%.2f", shift.totalHours)} ساعة",
                    color = Color(0xFFA0A0AB),
                    fontSize = 12.5.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delete_shift_button")
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
