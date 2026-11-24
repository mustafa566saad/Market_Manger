package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.DailyTreasury
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class TreasuryUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,

    // 1. Shift Closing Section
    val selectedDate: LocalDate = LocalDate.now(),
    val actualCashInput: String = "",
    val previousCash: Double = 0.0,
    val totalDailyExpenses: Double = 0.0,
    val totalSupplierCashPayments: Double = 0.0,
    val fawrySupplierPayments: Double = 0.0,
    val isDayAlreadyClosed: Boolean = false,
    val isLoadingPreview: Boolean = false,
    val isSavingClosing: Boolean = false,
    val showUpdateConfirmDialog: Boolean = false,

    // Retroactive Recalculation
    val showRetroactiveConfirmDialog: Boolean = false,
    val isRunningRetroactive: Boolean = false,

    // 2. Monthly History & Sales Records
    val historyYearMonth: YearMonth = YearMonth.now(),
    val monthlyHistory: List<DailyTreasury> = emptyList(),
    val isLoadingHistory: Boolean = false
) {
    val parsedActualCash: Double?
        get() = actualCashInput.trim().toDoubleOrNull()

    val hasValidCashInput: Boolean
        get() = parsedActualCash != null && parsedActualCash!! >= 0.0

    /**
     * Formula: TotalDailySales = (ActualCash - PreviousCash) + TotalDailyExpenses + TotalSupplierCashPayments
     */
    val calculatedDailySales: Double
        get() {
            val actual = parsedActualCash ?: 0.0
            return (actual - previousCash) + totalDailyExpenses + totalSupplierCashPayments
        }

    /**
     * Formula: NetProfit (10%) = TotalDailySales * 0.10
     */
    val calculatedNetProfit: Double
        get() = calculatedDailySales * 0.10

    // Monthly History Bottom Statistics
    val totalMonthlySales: Double
        get() = monthlyHistory.sumOf { it.totalSales }

    val totalMonthlyProfit: Double
        get() = monthlyHistory.sumOf { it.profit }
}

class TreasuryViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreasuryUiState())
    val uiState: StateFlow<TreasuryUiState> = _uiState.asStateFlow()

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
        loadDayPreview(_uiState.value.selectedDate)
        loadMonthlyHistory()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, snackbarMessage = null) }
    }

    // =========================================================================
    // 1. Shift Closing Inputs & Preview Calculations
    // =========================================================================

    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                historyYearMonth = YearMonth.from(date)
            )
        }
        loadDayPreview(date)
        loadMonthlyHistory()
    }

    fun onActualCashInputChanged(text: String) {
        _uiState.update { it.copy(actualCashInput = text) }
    }

    fun selectClosedDayForEditing(treasury: DailyTreasury) {
        _uiState.update { it.copy(selectedDate = treasury.date) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPreview = true, errorMessage = null) }
            repository.getDailyClosingPreview(treasury.date, 0.0).fold(
                onSuccess = { preview ->
                    val savedPhysicalCash = (preview.previousCash + treasury.netCash).coerceAtLeast(0.0)
                    val formattedCash = if (savedPhysicalCash % 1.0 == 0.0) {
                        savedPhysicalCash.toLong().toString()
                    } else {
                        String.format(java.util.Locale.US, "%.2f", savedPhysicalCash)
                    }
                    _uiState.update {
                        it.copy(
                            previousCash = preview.previousCash,
                            totalDailyExpenses = preview.totalExpenses,
                            totalSupplierCashPayments = preview.cashSupplierPayments,
                            fawrySupplierPayments = preview.fawrySupplierPayments,
                            isDayAlreadyClosed = preview.isAlreadyClosed,
                            actualCashInput = formattedCash,
                            isLoadingPreview = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            errorMessage = err.localizedMessage ?: "تعذر تحميل بيانات اليومية"
                        )
                    }
                }
            )
        }
    }

    fun loadDayPreview(date: LocalDate = _uiState.value.selectedDate) {
        val currentCash = _uiState.value.parsedActualCash ?: 0.0
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPreview = true, errorMessage = null) }
            repository.getDailyClosingPreview(date, currentCash).fold(
                onSuccess = { preview ->
                    _uiState.update {
                        it.copy(
                            previousCash = preview.previousCash,
                            totalDailyExpenses = preview.totalExpenses,
                            totalSupplierCashPayments = preview.cashSupplierPayments,
                            fawrySupplierPayments = preview.fawrySupplierPayments,
                            isDayAlreadyClosed = preview.isAlreadyClosed,
                            isLoadingPreview = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            errorMessage = err.localizedMessage ?: "تعذر حساب معاينة الخزينة"
                        )
                    }
                }
            )
        }
    }

    fun onSaveOrUpdateClicked() {
        val state = _uiState.value
        val actualCash = state.parsedActualCash
        if (actualCash == null || actualCash < 0.0) {
            _uiState.update {
                it.copy(snackbarMessage = "يرجى إدخال قيمة صحيحة للكاش الفعلي الموجود بالدرج")
            }
            return
        }

        if (state.isDayAlreadyClosed) {
            _uiState.update { it.copy(showUpdateConfirmDialog = true) }
        } else {
            executeSaveDailyClosing()
        }
    }

    fun dismissUpdateConfirmDialog() {
        _uiState.update { it.copy(showUpdateConfirmDialog = false) }
    }

    fun confirmUpdateClosedDay() {
        _uiState.update { it.copy(showUpdateConfirmDialog = false) }
        executeSaveDailyClosing()
    }

    private fun executeSaveDailyClosing() {
        val state = _uiState.value
        val actualCash = state.parsedActualCash ?: return
        val targetDate = state.selectedDate

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingClosing = true, errorMessage = null) }
            repository.saveDailyClosing(targetDate, actualCash).fold(
                onSuccess = { saved ->
                    val actionWord = if (state.isDayAlreadyClosed) "تعديل" else "حفظ وتقفيل"
                    val salesStr = String.format(java.util.Locale.US, "%.2f", saved.totalSales)
                    _uiState.update {
                        it.copy(
                            isSavingClosing = false,
                            isDayAlreadyClosed = true,
                            snackbarMessage = "تم $actionWord اليومية بنجاح — إجمالي المبيعات: $salesStr ج.م"
                        )
                    }
                    loadDayPreview(targetDate)
                    loadMonthlyHistory()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSavingClosing = false,
                            errorMessage = err.localizedMessage ?: "فشل حفظ اليومية في السيرفر",
                            snackbarMessage = "فشل الحفظ: ${err.localizedMessage ?: err.message}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // Retroactive Recalculation
    // =========================================================================

    fun requestRetroactiveRecalculation() {
        _uiState.update { it.copy(showRetroactiveConfirmDialog = true) }
    }

    fun dismissRetroactiveConfirmDialog() {
        _uiState.update { it.copy(showRetroactiveConfirmDialog = false) }
    }

    fun confirmRetroactiveRecalculation() {
        _uiState.update { it.copy(showRetroactiveConfirmDialog = false, isRunningRetroactive = true, errorMessage = null) }
        viewModelScope.launch {
            repository.runRetroactiveAdjustment().fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isRunningRetroactive = false,
                            snackbarMessage = "تمت تسوية وإعادة حساب $count يومية بنجاح مع الحفاظ على رصيد الدرج"
                        )
                    }
                    refreshAll()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isRunningRetroactive = false,
                            errorMessage = err.localizedMessage ?: "فشلت التسوية بأثر رجعي",
                            snackbarMessage = "فشلت التسوية: ${err.localizedMessage ?: err.message}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // 2. Monthly History & Sales Records
    // =========================================================================

    fun prevHistoryMonth() {
        _uiState.update { it.copy(historyYearMonth = it.historyYearMonth.minusMonths(1)) }
        loadMonthlyHistory()
    }

    fun nextHistoryMonth() {
        _uiState.update { it.copy(historyYearMonth = it.historyYearMonth.plusMonths(1)) }
        loadMonthlyHistory()
    }

    fun loadMonthlyHistory() {
        val ym = _uiState.value.historyYearMonth
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            repository.getDailyTreasuriesForMonth(ym.year, ym.monthValue).fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            monthlyHistory = list,
                            isLoadingHistory = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = err.localizedMessage ?: "تعذر تحميل سجل اليوميات الشهرية"
                        )
                    }
                }
            )
        }
    }
}
