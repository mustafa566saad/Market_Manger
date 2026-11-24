package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Attendance
import com.example.data.model.ConnectionStatus
import com.example.data.model.Employee
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

enum class EmployeeTab(val titleAr: String) {
    STAFF_DIRECTORY("دليل الموظفين"),
    LOG_SHIFT("تسجيل وردية عمل"),
    MONTHLY_TIMESHEET("كشف الورديات والأجور")
}

data class EmployeeUiState(
    val selectedTab: EmployeeTab = EmployeeTab.STAFF_DIRECTORY,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Employees Directory (Tab 1)
    val employees: List<Employee> = emptyList(),
    val showAddEmployeeDialog: Boolean = false,
    val editingEmployee: Employee? = null,
    val isSavingEmployee: Boolean = false,

    // Log Work Shift (Tab 2)
    val shiftSelectedEmployee: Employee? = null,
    val shiftDate: LocalDate = LocalDate.now(),
    val checkInTime: LocalTime = LocalTime.of(9, 0),
    val checkOutTime: LocalTime = LocalTime.of(17, 0),
    val isSubmittingShift: Boolean = false,

    // Monthly Timesheet & Payroll (Tab 3)
    val timesheetYearMonth: YearMonth = YearMonth.now(),
    val timesheetEmployeeFilter: Employee? = null, // null means all employees
    val timesheetShifts: List<Attendance> = emptyList(),
    val isLoadingTimesheet: Boolean = false,
    val shiftToEdit: Attendance? = null,
    val shiftToDelete: Attendance? = null,
    val isSavingShiftEdit: Boolean = false,
    val isDeletingShift: Boolean = false
) {
    // Calculated overnight shift details for Tab 2
    val isOvernightShift: Boolean
        get() = checkOutTime <= checkInTime

    val calculatedCheckInDateTime: LocalDateTime
        get() = LocalDateTime.of(shiftDate, checkInTime)

    val calculatedCheckOutDateTime: LocalDateTime
        get() = if (isOvernightShift) {
            LocalDateTime.of(shiftDate.plusDays(1), checkOutTime)
        } else {
            LocalDateTime.of(shiftDate, checkOutTime)
        }

    val calculatedTotalHours: Double
        get() {
            val minutes = Duration.between(calculatedCheckInDateTime, calculatedCheckOutDateTime).toMinutes()
            return (minutes.coerceAtLeast(0) / 60.0)
        }

    val calculatedShiftCost: Double
        get() {
            val rate = shiftSelectedEmployee?.hourRate ?: 0.0
            return calculatedTotalHours * rate
        }

    // Aggregates for Tab 3
    val timesheetTotalHours: Double
        get() = timesheetShifts.sumOf { it.totalHours }

    val timesheetTotalCost: Double
        get() = timesheetShifts.sumOf { it.shiftCost }
}

class EmployeeViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeUiState())
    val uiState: StateFlow<EmployeeUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
        loadEmployees()
        loadMonthlyTimesheet()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            repository.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
        viewModelScope.launch {
            repository.lastErrorMessage.collect { err ->
                if (err != null) {
                    _uiState.update { it.copy(errorMessage = err) }
                }
            }
        }
    }

    fun selectTab(tab: EmployeeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == EmployeeTab.MONTHLY_TIMESHEET) {
            loadMonthlyTimesheet()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // =========================================================================
    // Tab 1: Staff Directory & Add Employee
    // =========================================================================

    fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getEmployees(includeInactive = true).fold(
                onSuccess = { list ->
                    _uiState.update { state ->
                        // Automatically pick first active employee for Tab 2 if not selected yet
                        val active = list.filter { it.isActive }
                        val selected = state.shiftSelectedEmployee ?: active.firstOrNull()
                        state.copy(
                            employees = list,
                            shiftSelectedEmployee = selected,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "تعذر تحميل الموظفين: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun openAddEmployeeDialog() {
        _uiState.update { it.copy(showAddEmployeeDialog = true, editingEmployee = null) }
    }

    fun openEditEmployeeDialog(employee: Employee) {
        _uiState.update { it.copy(showAddEmployeeDialog = true, editingEmployee = employee) }
    }

    fun dismissEmployeeDialog() {
        _uiState.update { it.copy(showAddEmployeeDialog = false, editingEmployee = null) }
    }

    fun saveEmployee(name: String, hourRate: Double, phone: String, isActive: Boolean) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم الموظف") }
            return
        }
        if (hourRate <= 0.0) {
            _uiState.update { it.copy(errorMessage = "سعر الساعة يجب أن يكون أكبر من صفر") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEmployee = true, errorMessage = null) }
            val currentEditing = _uiState.value.editingEmployee
            if (currentEditing == null) {
                // Add new employee
                val newEmp = Employee(
                    id = 0,
                    employeeName = name.trim(),
                    hourRate = hourRate,
                    phone = phone.trim(),
                    isActive = isActive
                )
                repository.addEmployee(newEmp).fold(
                    onSuccess = { newId ->
                        _uiState.update {
                            it.copy(
                                isSavingEmployee = false,
                                showAddEmployeeDialog = false,
                                successMessage = "تمت إضافة الموظف \"$name\" بنجاح"
                            )
                        }
                        loadEmployees()
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSavingEmployee = false,
                                errorMessage = "فشل حفظ الموظف: ${err.localizedMessage}"
                            )
                        }
                    }
                )
            } else {
                // Update existing employee
                val updated = currentEditing.copy(
                    employeeName = name.trim(),
                    hourRate = hourRate,
                    phone = phone.trim(),
                    isActive = isActive
                )
                repository.updateEmployee(updated).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isSavingEmployee = false,
                                showAddEmployeeDialog = false,
                                editingEmployee = null,
                                successMessage = "تم تحديث بيانات الموظف بنجاح"
                            )
                        }
                        loadEmployees()
                        loadMonthlyTimesheet()
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSavingEmployee = false,
                                errorMessage = "فشل تعديل الموظف: ${err.localizedMessage}"
                            )
                        }
                    }
                )
            }
        }
    }

    fun toggleEmployeeActive(employee: Employee) {
        viewModelScope.launch {
            val updated = employee.copy(isActive = !employee.isActive)
            repository.updateEmployee(updated).fold(
                onSuccess = {
                    val statusText = if (updated.isActive) "تنشيط" else "إيقاف"
                    _uiState.update {
                        it.copy(successMessage = "تم $statusText الموظف ${employee.employeeName}")
                    }
                    loadEmployees()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(errorMessage = "تعذر تغيير حالة الموظف: ${err.localizedMessage}")
                    }
                }
            )
        }
    }

    // =========================================================================
    // Tab 2: Log Work Shift (Attendance)
    // =========================================================================

    fun selectShiftEmployee(employee: Employee) {
        _uiState.update { it.copy(shiftSelectedEmployee = employee) }
    }

    fun updateShiftDate(date: LocalDate) {
        _uiState.update { it.copy(shiftDate = date) }
    }

    fun updateCheckInTime(time: LocalTime) {
        _uiState.update { it.copy(checkInTime = time) }
    }

    fun updateCheckOutTime(time: LocalTime) {
        _uiState.update { it.copy(checkOutTime = time) }
    }

    fun submitWorkShift() {
        val state = _uiState.value
        val emp = state.shiftSelectedEmployee
        if (emp == null) {
            _uiState.update { it.copy(errorMessage = "يرجى اختيار الموظف أولاً") }
            return
        }

        val checkIn = state.calculatedCheckInDateTime
        val checkOut = state.calculatedCheckOutDateTime

        if (state.calculatedTotalHours <= 0.0) {
            _uiState.update { it.copy(errorMessage = "مدة الوردية غير صحيحة") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingShift = true, errorMessage = null) }

            val attendance = Attendance(
                id = 0,
                employeeId = emp.id,
                employeeName = emp.employeeName,
                hourRate = emp.hourRate,
                checkIn = checkIn,
                checkOut = checkOut
            )

            repository.saveAttendance(attendance).fold(
                onSuccess = {
                    val hoursStr = String.format("%.2f", state.calculatedTotalHours)
                    val costStr = String.format("%.2f", state.calculatedShiftCost)
                    val overnightText = if (state.isOvernightShift) " (وردية ليلية)" else ""

                    _uiState.update {
                        it.copy(
                            isSubmittingShift = false,
                            successMessage = "تم تسجيل وردية ${emp.employeeName} بنجاح: $hoursStr ساعة ($costStr ج.م)$overnightText"
                        )
                    }
                    // Refresh timesheet
                    loadMonthlyTimesheet()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmittingShift = false,
                            errorMessage = "تعذر تسجيل الوردية في قاعدة البيانات: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // Tab 3: Monthly Staff Timesheet & Payroll
    // =========================================================================

    fun setTimesheetMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(timesheetYearMonth = yearMonth) }
        loadMonthlyTimesheet()
    }

    fun nextTimesheetMonth() {
        setTimesheetMonth(_uiState.value.timesheetYearMonth.plusMonths(1))
    }

    fun prevTimesheetMonth() {
        setTimesheetMonth(_uiState.value.timesheetYearMonth.minusMonths(1))
    }

    fun filterTimesheetByEmployee(employee: Employee?) {
        _uiState.update { it.copy(timesheetEmployeeFilter = employee) }
        loadMonthlyTimesheet()
    }

    fun loadMonthlyTimesheet() {
        val ym = _uiState.value.timesheetYearMonth
        val emp = _uiState.value.timesheetEmployeeFilter
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTimesheet = true) }
            repository.getAttendancesForMonth(
                year = ym.year,
                month = ym.monthValue,
                employeeId = emp?.id
            ).fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            timesheetShifts = list,
                            isLoadingTimesheet = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingTimesheet = false,
                            errorMessage = "تعذر تحميل كشف الورديات: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun openEditShiftDialog(attendance: Attendance) {
        _uiState.update { it.copy(shiftToEdit = attendance) }
    }

    fun dismissEditShiftDialog() {
        _uiState.update { it.copy(shiftToEdit = null) }
    }

    fun saveEditedShift(attendanceId: Int, employeeId: Int, checkIn: LocalDateTime, checkOut: LocalDateTime) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingShiftEdit = true, errorMessage = null) }
            val att = Attendance(
                id = attendanceId,
                employeeId = employeeId,
                checkIn = checkIn,
                checkOut = checkOut
            )
            repository.saveAttendance(att).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSavingShiftEdit = false,
                            shiftToEdit = null,
                            successMessage = "تم تعديل الوردية بنجاح"
                        )
                    }
                    loadMonthlyTimesheet()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSavingShiftEdit = false,
                            errorMessage = "فشل تعديل الوردية: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun requestDeleteShift(attendance: Attendance) {
        _uiState.update { it.copy(shiftToDelete = attendance) }
    }

    fun dismissDeleteShiftDialog() {
        _uiState.update { it.copy(shiftToDelete = null) }
    }

    fun confirmDeleteShift() {
        val shift = _uiState.value.shiftToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingShift = true, errorMessage = null) }
            repository.deleteAttendance(shift.id).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeletingShift = false,
                            shiftToDelete = null,
                            successMessage = "تم حذف الوردية بنجاح"
                        )
                    }
                    loadMonthlyTimesheet()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isDeletingShift = false,
                            errorMessage = "فشل حذف الوردية: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }
}
