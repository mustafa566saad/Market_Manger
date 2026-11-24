package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Attendance
import com.example.data.model.ConnectionStatus
import com.example.data.model.DailyClosingPreview
import com.example.data.model.DailyTreasury
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.MonthlyFinancialAnalysis
import com.example.data.model.SupplierBill
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

enum class AppTab(val titleAr: String) {
    TREASURY("الخزينة اليومية"),
    SUPPLIERS("فواتير الموردين"),
    EXPENSES("المصروفات"),
    ATTENDANCE("الموظفين والورديات"),
    MONTHLY("التحليل المالي")
}

data class MarketUiState(
    val currentTab: AppTab = AppTab.TREASURY,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val isGlobalLoading: Boolean = false,

    // Treasury Tab
    val treasuryDate: Date = Date(),
    val cashInDrawerText: String = "",
    val closingPreview: DailyClosingPreview? = null,
    val isCalculatingPreview: Boolean = false,
    val isSavingTreasury: Boolean = false,
    val treasuryList: List<DailyTreasury> = emptyList(),
    val isRetroactiveRunning: Boolean = false,

    // Supplier Bills Tab
    val supplierBills: List<SupplierBill> = emptyList(),
    val supplierSearchQuery: String = "",
    val supplierFawryFilter: Boolean? = null, // null: all, true: Fawry, false: Cash
    val cumulativeSupplierDebt: Double = 0.0,
    val isSavingSupplierBill: Boolean = false,

    // Expenses Tab
    val expenses: List<Expense> = emptyList(),
    val expenseTypeFilter: String = "",
    val isSavingExpense: Boolean = false,

    // Attendance Tab
    val employees: List<Employee> = emptyList(),
    val attendances: List<Attendance> = emptyList(),
    val activeAttendances: List<Attendance> = emptyList(),
    val selectedEmployeeFilter: Int? = null,
    val isSavingAttendance: Boolean = false,

    // Monthly Analysis Tab
    val analysisYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val analysisMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val monthlyAnalysis: MonthlyFinancialAnalysis? = null,
    val isLoadingAnalysis: Boolean = false
)

class MarketViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("MarketViewModel", "Unhandled coroutine error", throwable)
        _uiState.update { it.copy(errorMessage = throwable.localizedMessage ?: throwable.message, isGlobalLoading = false) }
    }

    init {
        observeConnection()
        loadInitialData()
    }

    private fun observeConnection() {
        viewModelScope.launch(exceptionHandler) {
            repository.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
        viewModelScope.launch(exceptionHandler) {
            repository.lastErrorMessage.collect { error ->
                if (error != null) {
                    _uiState.update { it.copy(errorMessage = error) }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        when (tab) {
            AppTab.TREASURY -> loadTreasuryData()
            AppTab.SUPPLIERS -> loadSupplierBills()
            AppTab.EXPENSES -> loadExpenses()
            AppTab.ATTENDANCE -> loadAttendanceData()
            AppTab.MONTHLY -> loadMonthlyAnalysis()
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refreshCurrentTab() {
        selectTab(_uiState.value.currentTab)
    }

    private fun loadInitialData() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isGlobalLoading = true) }
            loadTreasuryData()
            loadSupplierBills()
            loadExpenses()
            loadAttendanceData()
            _uiState.update { it.copy(isGlobalLoading = false) }
        }
    }

    // =========================================================================
    // TREASURY & SHIFT CLOSING
    // =========================================================================

    fun setTreasuryDate(date: Date) {
        _uiState.update { it.copy(treasuryDate = date) }
        calculateClosingPreview()
    }

    fun setTreasuryDate(date: LocalDate) {
        val legacy = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        _uiState.update { it.copy(treasuryDate = legacy) }
        calculateClosingPreview()
    }

    fun setCashInDrawerText(text: String) {
        _uiState.update { it.copy(cashInDrawerText = text) }
        calculateClosingPreview()
    }

    fun calculateClosingPreview() {
        val state = _uiState.value
        val cash = state.cashInDrawerText.toDoubleOrNull() ?: 0.0
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isCalculatingPreview = true) }
            val res = repository.getDailyClosingPreview(state.treasuryDate, cash)
            res.onSuccess { preview ->
                _uiState.update { it.copy(closingPreview = preview, isCalculatingPreview = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isCalculatingPreview = false, errorMessage = err.message) }
            }
        }
    }

    fun loadTreasuryData() {
        viewModelScope.launch(exceptionHandler) {
            val listRes = repository.getAllDailyTreasuries()
            listRes.onSuccess { list ->
                _uiState.update { it.copy(treasuryList = list) }
            }
            calculateClosingPreview()
        }
    }

    fun saveDailyClosing() {
        val state = _uiState.value
        val cash = state.cashInDrawerText.toDoubleOrNull() ?: 0.0
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSavingTreasury = true) }
            val res = repository.saveDailyClosing(state.treasuryDate, cash)
            res.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        isSavingTreasury = false,
                        snackbarMessage = "تم حفظ وتقفيل الخزينة بنجاح! مبيعات: ${String.format("%.2f", saved.totalSales)} ج.م"
                    )
                }
                loadTreasuryData()
            }.onFailure { err ->
                _uiState.update { it.copy(isSavingTreasury = false, errorMessage = err.message) }
            }
        }
    }

    fun runRetroactiveAdjustment() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isRetroactiveRunning = true) }
            val res = repository.runRetroactiveAdjustment()
            res.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        isRetroactiveRunning = false,
                        snackbarMessage = "تمت إعادة ضبط $count يوم بأثر رجعي بنجاح وحساب المبيعات بدقة!"
                    )
                }
                loadTreasuryData()
            }.onFailure { err ->
                _uiState.update { it.copy(isRetroactiveRunning = false, errorMessage = err.message) }
            }
        }
    }

    // =========================================================================
    // SUPPLIER BILLS & FAWRY
    // =========================================================================

    fun setSupplierSearch(query: String) {
        _uiState.update { it.copy(supplierSearchQuery = query) }
    }

    fun setSupplierFawryFilter(fawry: Boolean?) {
        _uiState.update { it.copy(supplierFawryFilter = fawry) }
    }

    fun loadSupplierBills() {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.getSupplierBills()
            res.onSuccess { bills ->
                _uiState.update { it.copy(supplierBills = bills) }
            }
            val debtRes = repository.getCumulativeSupplierDebt()
            debtRes.onSuccess { debt ->
                _uiState.update { it.copy(cumulativeSupplierDebt = debt) }
            }
        }
    }

    fun saveSupplierBill(bill: SupplierBill, isFawry: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSavingSupplierBill = true) }
            val res = repository.saveSupplierBill(bill, isFawry)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavingSupplierBill = false,
                        snackbarMessage = if (isFawry) "تم حفظ فاتورة مورد (دفع فوري رقمي - لم تُخصم من الدرج)" else "تم حفظ فاتورة مورد (سداد نقدي من الدرج)"
                    )
                }
                loadSupplierBills()
                calculateClosingPreview()
            }.onFailure { err ->
                _uiState.update { it.copy(isSavingSupplierBill = false, errorMessage = err.message) }
            }
        }
    }

    fun deleteSupplierBill(bill: SupplierBill) {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.deleteSupplierBill(bill.billId, bill.billDate)
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم حذف فاتورة ${bill.supplierName} وتحديث الخزينة تلقائياً") }
                loadSupplierBills()
                calculateClosingPreview()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    // =========================================================================
    // EXPENSES
    // =========================================================================

    fun setExpenseTypeFilter(type: String) {
        _uiState.update { it.copy(expenseTypeFilter = type) }
    }

    fun loadExpenses() {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.getExpenses()
            res.onSuccess { expenses ->
                _uiState.update { it.copy(expenses = expenses) }
            }
        }
    }

    fun saveExpense(expense: Expense) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSavingExpense = true) }
            val res = repository.saveExpense(expense)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavingExpense = false,
                        snackbarMessage = "تم حفظ المصروف بنجاح ومزامنة الخزينة تلقائياً"
                    )
                }
                loadExpenses()
                calculateClosingPreview()
            }.onFailure { err ->
                _uiState.update { it.copy(isSavingExpense = false, errorMessage = err.message) }
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.deleteExpense(expense.expenseId, expense.expenseDate)
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم حذف المصروف بنجاح ومزامنة الخزينة") }
                loadExpenses()
                calculateClosingPreview()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    // =========================================================================
    // EMPLOYEES & WORK SHIFTS
    // =========================================================================

    fun loadAttendanceData() {
        viewModelScope.launch(exceptionHandler) {
            val empRes = repository.getEmployees(includeInactive = true)
            empRes.onSuccess { emps ->
                _uiState.update { it.copy(employees = emps) }
            }
            val attRes = repository.getAttendances()
            attRes.onSuccess { atts ->
                _uiState.update { it.copy(attendances = atts) }
            }
            val activeRes = repository.getActiveAttendances()
            activeRes.onSuccess { actives ->
                _uiState.update { it.copy(activeAttendances = actives) }
            }
        }
    }

    fun checkIn(employeeId: Int, checkInTime: Date = Date()) {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.checkIn(employeeId, checkInTime)
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم تسجيل بدء الوردية بنجاح") }
                loadAttendanceData()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun checkOut(attendanceId: Int, checkOutTime: Date = Date()) {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.checkOut(attendanceId, checkOutTime)
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم تسجيل إنهاء الوردية وحساب الأجر تلقائياً") }
                loadAttendanceData()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun saveAttendance(attendance: Attendance) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSavingAttendance = true) }
            val res = repository.saveAttendance(attendance)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavingAttendance = false,
                        snackbarMessage = "تم حفظ سجل الحضور والوردية بنجاح"
                    )
                }
                loadAttendanceData()
            }.onFailure { err ->
                _uiState.update { it.copy(isSavingAttendance = false, errorMessage = err.message) }
            }
        }
    }

    fun deleteAttendance(attendanceId: Int) {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.deleteAttendance(attendanceId)
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم حذف سجل الوردية") }
                loadAttendanceData()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun saveEmployee(employee: Employee) {
        viewModelScope.launch(exceptionHandler) {
            val res = if (employee.employeeId == 0) {
                repository.addEmployee(employee)
            } else {
                repository.updateEmployee(employee)
            }
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم حفظ بيانات الموظف بنجاح") }
                loadAttendanceData()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun deleteEmployee(employeeId: Int) {
        viewModelScope.launch(exceptionHandler) {
            val res = repository.deleteEmployee(employeeId)
            res.onSuccess {
                _uiState.update { it.copy(snackbarMessage = "تم حذف/تعطيل الموظف بنجاح") }
                loadAttendanceData()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    // =========================================================================
    // MONTHLY CLOSING & FINANCIAL ANALYSIS (10 KPIs)
    // =========================================================================

    fun setAnalysisMonth(year: Int, month: Int) {
        _uiState.update { it.copy(analysisYear = year, analysisMonth = month) }
        loadMonthlyAnalysis()
    }

    fun loadMonthlyAnalysis() {
        val state = _uiState.value
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoadingAnalysis = true) }
            val res = repository.getMonthlyFinancialAnalysis(state.analysisYear, state.analysisMonth)
            res.onSuccess { analysis ->
                _uiState.update { it.copy(monthlyAnalysis = analysis, isLoadingAnalysis = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoadingAnalysis = false, errorMessage = err.message) }
            }
        }
    }
}
