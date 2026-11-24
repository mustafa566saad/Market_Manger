package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.MonthSummaryData
import com.example.data.repository.MarketRepository
import com.example.util.ExportFormat
import com.example.util.ExportedReportFile
import com.example.util.MonthlyReportExporter
import kotlinx.coroutines.CoroutineExceptionHandler
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

data class MonthClosingUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val summary: MonthSummaryData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,

    // Export & Share State
    val showExportDialog: Boolean = false,
    val isExporting: Boolean = false,
    val exportedReportEvent: ExportedReportFile? = null
)

class MonthClosingViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthClosingUiState())
    val uiState: StateFlow<MonthClosingUiState> = _uiState.asStateFlow()

    private val _screenState = MutableStateFlow<UiState<MonthSummaryData>>(UiState.Loading)
    val screenState: StateFlow<UiState<MonthSummaryData>> = _screenState.asStateFlow()

    private val arabicMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val msg = throwable.message ?: "تعذر تحميل التحليل المالي للشهر. اضغط إعادة المحاولة 🔄"
        _screenState.value = UiState.Error(msg)
        _uiState.update {
            it.copy(
                isLoading = false,
                isExporting = false,
                errorMessage = msg
            )
        }
    }

    init {
        observeConnection()
        loadMonthlySummary()
    }

    private fun observeConnection() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.lastErrorMessage.collect { err ->
                if (err != null) {
                    _uiState.update { it.copy(errorMessage = err) }
                }
            }
        }
    }

    fun loadMonthlySummary() {
        val ym = _uiState.value.selectedYearMonth
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            _screenState.value = UiState.Loading
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getMonthlyFinancialAnalysis(ym.year, ym.monthValue).fold(
                onSuccess = { data ->
                    _screenState.value = UiState.Success(data)
                    _uiState.update {
                        it.copy(
                            summary = data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    val msg = err.message ?: "تعذر تحميل التحليل المالي للشهر"
                    _screenState.value = UiState.Error(msg)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = msg
                        )
                    }
                }
            )
        }
    }

    fun prevMonth() {
        _uiState.update {
            it.copy(selectedYearMonth = it.selectedYearMonth.minusMonths(1))
        }
        loadMonthlySummary()
    }

    fun nextMonth() {
        _uiState.update {
            it.copy(selectedYearMonth = it.selectedYearMonth.plusMonths(1))
        }
        loadMonthlySummary()
    }

    fun resetToCurrentMonth() {
        _uiState.update {
            it.copy(selectedYearMonth = YearMonth.now())
        }
        loadMonthlySummary()
    }

    fun openExportDialog() {
        if (_uiState.value.summary == null) {
            _uiState.update {
                it.copy(snackbarMessage = "يرجى انتظار تحميل بيانات الشهر قبل التصدير")
            }
            return
        }
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun exportReport(context: Context, format: ExportFormat) {
        val summary = _uiState.value.summary
        if (summary == null) {
            _uiState.update {
                it.copy(
                    showExportDialog = false,
                    snackbarMessage = "لا توجد بيانات مالية جاهزة للتصدير"
                )
            }
            return
        }

        val monthLabel = _uiState.value.selectedYearMonth.format(arabicMonthFormatter)

        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            _uiState.update { it.copy(isExporting = true, showExportDialog = false, errorMessage = null) }
            try {
                val exported = withContext(Dispatchers.IO) {
                    when (format) {
                        ExportFormat.CSV -> {
                            val file = MonthlyReportExporter.exportMonthlyReportCsv(
                                cacheDir = context.cacheDir,
                                summary = summary
                            )
                            ExportedReportFile(
                                file = file,
                                mimeType = "text/csv",
                                title = "التقرير المالي الشهري الشامل - $monthLabel",
                                format = ExportFormat.CSV
                            )
                        }
                        ExportFormat.PDF -> {
                            val file = MonthlyReportExporter.exportMonthlyReportPdf(
                                cacheDir = context.cacheDir,
                                summary = summary
                            )
                            ExportedReportFile(
                                file = file,
                                mimeType = "application/pdf",
                                title = "التقرير المالي الشهري الشامل - $monthLabel",
                                format = ExportFormat.PDF
                            )
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportedReportEvent = exported,
                        snackbarMessage = "تم تجهيز التقرير المالي الشامل بنجاح"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "فشل تصدير التقرير المالي: ${e.message ?: "خطأ غير متوقع"}"
                    )
                }
            }
        }
    }

    fun consumeExportedReportEvent() {
        _uiState.update { it.copy(exportedReportEvent = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, snackbarMessage = null) }
    }
}
