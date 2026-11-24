package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.SupplierMonthlySummary
import com.example.data.repository.MarketRepository
import com.example.util.ExportFormat
import com.example.util.ExportedReportFile
import com.example.util.SupplierReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SuppliersSummaryUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val searchQuery: String = "",
    val allSuppliers: List<SupplierMonthlySummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,

    // Export state
    val showExportDialog: Boolean = false,
    val isExporting: Boolean = false,
    val exportedReportEvent: ExportedReportFile? = null
) {
    /**
     * Filtered list sorted descending by highest cumulative debt.
     */
    val filteredSuppliers: List<SupplierMonthlySummary>
        get() {
            val query = searchQuery.trim()
            val list = if (query.isBlank()) {
                allSuppliers
            } else {
                allSuppliers.filter { it.supplierName.contains(query, ignoreCase = true) }
            }
            return list.sortedWith(
                compareByDescending<SupplierMonthlySummary> { it.cumulativeDebt }
                    .thenByDescending { it.monthlyPurchases }
                    .thenBy { it.supplierName }
            )
        }

    // Bottom Totals Bar (All suppliers aggregates for the selected month + grand debt)
    val totalMonthlyPurchases: Double
        get() = allSuppliers.sumOf { it.monthlyPurchases }

    val totalMonthlyPaid: Double
        get() = allSuppliers.sumOf { it.monthlyPaid }

    val totalMonthlyDebt: Double
        get() = allSuppliers.sumOf { it.monthlyNewDebt }

    val totalGrandDebt: Double
        get() = allSuppliers.sumOf { it.cumulativeDebt }
}

class SuppliersSummaryViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersSummaryUiState())
    val uiState: StateFlow<SuppliersSummaryUiState> = _uiState.asStateFlow()

    private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))

    init {
        observeConnection()
        loadSummary()
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

    fun loadSummary() {
        val ym = _uiState.value.selectedYearMonth
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getSuppliersMonthlySummary(ym.year, ym.monthValue).fold(
                onSuccess = { list ->
                    val sorted = list.sortedWith(
                        compareByDescending<SupplierMonthlySummary> { it.cumulativeDebt }
                            .thenByDescending { it.monthlyPurchases }
                            .thenBy { it.supplierName }
                    )
                    _uiState.update {
                        it.copy(
                            allSuppliers = sorted,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "تعذر تحميل تقرير حسابات الموردين"
                        )
                    }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun prevMonth() {
        val nextYm = _uiState.value.selectedYearMonth.minusMonths(1)
        _uiState.update { it.copy(selectedYearMonth = nextYm) }
        loadSummary()
    }

    fun nextMonth() {
        val nextYm = _uiState.value.selectedYearMonth.plusMonths(1)
        _uiState.update { it.copy(selectedYearMonth = nextYm) }
        loadSummary()
    }

    fun resetToCurrentMonth() {
        val now = YearMonth.now()
        if (_uiState.value.selectedYearMonth != now) {
            _uiState.update { it.copy(selectedYearMonth = now) }
            loadSummary()
        }
    }

    fun openExportDialog() {
        if (_uiState.value.allSuppliers.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "لا توجد بيانات موردين لتصديرها") }
            return
        }
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun exportReport(context: Context, format: ExportFormat) {
        val currentState = _uiState.value
        val dataToExport = currentState.filteredSuppliers.ifEmpty { currentState.allSuppliers }
        if (dataToExport.isEmpty()) {
            _uiState.update {
                it.copy(
                    showExportDialog = false,
                    snackbarMessage = "لا توجد بيانات موردين لتصديرها"
                )
            }
            return
        }

        val appContext = context.applicationContext
        val yearMonth = currentState.selectedYearMonth
        val monthLabel = yearMonth.format(arabicMonthFormatter)

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val exported = withContext(Dispatchers.IO) {
                    when (format) {
                        ExportFormat.CSV -> {
                            val file = SupplierReportExporter.exportSuppliersSummaryCsv(
                                cacheDir = appContext.cacheDir,
                                suppliers = dataToExport,
                                yearMonth = yearMonth
                            )
                            ExportedReportFile(
                                file = file,
                                mimeType = "text/csv",
                                title = "تقرير حسابات الموردين - $monthLabel",
                                format = ExportFormat.CSV
                            )
                        }
                        ExportFormat.PDF -> {
                            val file = SupplierReportExporter.exportSuppliersSummaryPdf(
                                cacheDir = appContext.cacheDir,
                                suppliers = dataToExport,
                                yearMonth = yearMonth
                            )
                            ExportedReportFile(
                                file = file,
                                mimeType = "application/pdf",
                                title = "تقرير حسابات الموردين - $monthLabel",
                                format = ExportFormat.PDF
                            )
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        showExportDialog = false,
                        exportedReportEvent = exported
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        showExportDialog = false,
                        snackbarMessage = "فشل تصدير التقرير: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun consumeExportedReportEvent() {
        _uiState.update { it.copy(exportedReportEvent = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(snackbarMessage = null, errorMessage = null) }
    }
}
