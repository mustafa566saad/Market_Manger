package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategorySummary
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

val EXPENSE_TYPE_PRESETS = listOf(
    "كهرباء",
    "مياه وغاز",
    "إيجار",
    "مرتبات عمال",
    "نثريات"
)

const val CUSTOM_EXPENSE_TYPE_OPTION = "تصنيف مخصص (كتابة يدوية)"

data class ExpensesUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,

    // 1. Expense Recording Form
    val selectedPresetType: String = EXPENSE_TYPE_PRESETS.first(),
    val isCustomType: Boolean = false,
    val customTypeInput: String = "",
    val amountInput: String = "",
    val formDate: LocalDate = LocalDate.now(),
    val notesInput: String = "",
    val isFormDateClosedInTreasury: Boolean = false,
    val isSavingExpense: Boolean = false,

    // 2. Daily Expenses Ledger
    val ledgerDate: LocalDate = LocalDate.now(),
    val dailyExpenses: List<Expense> = emptyList(),
    val isLoadingDaily: Boolean = false,
    val expenseToDelete: Expense? = null,
    val isDeletingExpense: Boolean = false,

    // 3. Monthly Category Breakdown
    val breakdownYearMonth: YearMonth = YearMonth.now(),
    val monthlyBreakdown: List<ExpenseCategorySummary> = emptyList(),
    val isLoadingMonthly: Boolean = false
) {
    val effectiveExpenseType: String
        get() = if (isCustomType) customTypeInput.trim() else selectedPresetType

    val parsedAmount: Double
        get() = amountInput.toDoubleOrNull() ?: 0.0

    val isFormValid: Boolean
        get() = effectiveExpenseType.isNotBlank() && parsedAmount > 0.0

    val dailyTotalAmount: Double
        get() = dailyExpenses.sumOf { it.amount }

    val monthlyTotalAmount: Double
        get() = monthlyBreakdown.sumOf { it.totalAmount }
}

class ExpensesViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
        refreshAll()
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

    fun refreshAll() {
        loadDailyExpenses()
        loadMonthlyBreakdown()
        checkClosedStatusForDate(_uiState.value.formDate)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, snackbarMessage = null) }
    }

    // =========================================================================
    // 1. Form Inputs & Recording
    // =========================================================================

    fun selectPresetType(type: String) {
        if (type == CUSTOM_EXPENSE_TYPE_OPTION) {
            _uiState.update { it.copy(isCustomType = true) }
        } else {
            _uiState.update {
                it.copy(
                    selectedPresetType = type,
                    isCustomType = false,
                    customTypeInput = ""
                )
            }
        }
    }

    fun onCustomTypeChanged(text: String) {
        _uiState.update { it.copy(customTypeInput = text) }
    }

    fun onAmountChanged(text: String) {
        _uiState.update { it.copy(amountInput = text) }
    }

    fun onFormDateChanged(date: LocalDate) {
        _uiState.update {
            it.copy(
                formDate = date,
                ledgerDate = date,
                breakdownYearMonth = YearMonth.from(date)
            )
        }
        checkClosedStatusForDate(date)
        loadDailyExpenses()
        loadMonthlyBreakdown()
    }

    fun onNotesChanged(text: String) {
        _uiState.update { it.copy(notesInput = text) }
    }

    private fun checkClosedStatusForDate(date: LocalDate) {
        viewModelScope.launch {
            repository.isTreasuryDateClosed(date).fold(
                onSuccess = { isClosed ->
                    _uiState.update { it.copy(isFormDateClosedInTreasury = isClosed) }
                },
                onFailure = { /* non-fatal */ }
            )
        }
    }

    fun submitExpense() {
        val state = _uiState.value
        val type = state.effectiveExpenseType
        val amount = state.parsedAmount

        if (type.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "يرجى اختيار أو إدخال نوع المصروف") }
            return
        }
        if (amount <= 0.0) {
            _uiState.update { it.copy(snackbarMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر") }
            return
        }

        val targetDate = state.formDate
        val wasClosed = state.isFormDateClosedInTreasury

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingExpense = true, errorMessage = null) }
            val expense = Expense(
                id = 0,
                expenseType = type,
                amount = amount,
                expenseDate = targetDate,
                notes = state.notesInput.trim()
            )

            repository.saveExpense(expense).fold(
                onSuccess = {
                    val syncNote = if (wasClosed) " وتم تحديث الخزينة المقفولة تلقائياً" else ""
                    _uiState.update {
                        it.copy(
                            isSavingExpense = false,
                            amountInput = "",
                            notesInput = "",
                            ledgerDate = targetDate,
                            breakdownYearMonth = YearMonth.from(targetDate),
                            snackbarMessage = "تم تسجيل مصروف ($type) بنجاح$syncNote"
                        )
                    }
                    loadDailyExpenses()
                    loadMonthlyBreakdown()
                    checkClosedStatusForDate(targetDate)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSavingExpense = false,
                            errorMessage = err.localizedMessage ?: "تعذر حفظ المصروف في قاعدة البيانات",
                            snackbarMessage = "فشل حفظ المصروف: ${err.localizedMessage ?: err.message}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // 2. Daily Expenses Ledger & Delete (with Treasury Sync)
    // =========================================================================

    fun setLedgerDate(date: LocalDate) {
        _uiState.update { it.copy(ledgerDate = date) }
        loadDailyExpenses()
    }

    fun prevLedgerDay() {
        setLedgerDate(_uiState.value.ledgerDate.minusDays(1))
    }

    fun nextLedgerDay() {
        setLedgerDate(_uiState.value.ledgerDate.plusDays(1))
    }

    fun loadDailyExpenses() {
        val date = _uiState.value.ledgerDate
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDaily = true, errorMessage = null) }
            repository.getExpenses(startDate = date, endDate = date).fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            dailyExpenses = list,
                            isLoadingDaily = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingDaily = false,
                            errorMessage = err.localizedMessage ?: "تعذر تحميل مصروفات اليوم"
                        )
                    }
                }
            )
        }
    }

    fun requestDeleteExpense(expense: Expense) {
        _uiState.update { it.copy(expenseToDelete = expense) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(expenseToDelete = null) }
    }

    fun confirmDeleteExpense() {
        val expense = _uiState.value.expenseToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingExpense = true, errorMessage = null) }
            val wasClosed = repository.isTreasuryDateClosed(expense.expenseDate).getOrDefault(false)

            repository.deleteExpense(expense.id, expense.expenseDate).fold(
                onSuccess = {
                    val syncNote = if (wasClosed) " وتحديث الخزينة تلقائياً" else ""
                    _uiState.update {
                        it.copy(
                            isDeletingExpense = false,
                            expenseToDelete = null,
                            snackbarMessage = "تم حذف مصروف (${expense.expenseType})$syncNote"
                        )
                    }
                    loadDailyExpenses()
                    loadMonthlyBreakdown()
                    checkClosedStatusForDate(_uiState.value.formDate)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isDeletingExpense = false,
                            errorMessage = err.localizedMessage ?: "فشل حذف المصروف",
                            snackbarMessage = "تعذر حذف المصروف: ${err.localizedMessage ?: err.message}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // 3. Monthly Category Breakdown
    // =========================================================================

    fun prevBreakdownMonth() {
        _uiState.update { it.copy(breakdownYearMonth = it.breakdownYearMonth.minusMonths(1)) }
        loadMonthlyBreakdown()
    }

    fun nextBreakdownMonth() {
        _uiState.update { it.copy(breakdownYearMonth = it.breakdownYearMonth.plusMonths(1)) }
        loadMonthlyBreakdown()
    }

    fun loadMonthlyBreakdown() {
        val ym = _uiState.value.breakdownYearMonth
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMonthly = true) }
            repository.getMonthlyExpenseBreakdown(ym.year, ym.monthValue).fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            monthlyBreakdown = list,
                            isLoadingMonthly = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingMonthly = false,
                            errorMessage = err.localizedMessage ?: "تعذر تحميل ملخص مصاريف الشهر"
                        )
                    }
                }
            )
        }
    }
}
