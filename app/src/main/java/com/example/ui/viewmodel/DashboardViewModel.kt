package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.TodayQuickStats
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val stats: TodayQuickStats = TodayQuickStats(),
    val errorMessage: String? = null,
    val isRetrying: Boolean = false
)

/**
 * [TASK 1: DASHBOARD VIEW MODEL]
 * Manages remote data fetching for today's live supermarket KPIs on Dispatchers.IO,
 * exposing both sealed UiState<TodayQuickStats> and DashboardUiState with Arabic retry support.
 */
class DashboardViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _screenState = MutableStateFlow<UiState<TodayQuickStats>>(UiState.Loading)
    val screenState: StateFlow<UiState<TodayQuickStats>> = _screenState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val msg = throwable.message ?: "حدث خطأ أثناء الاتصال بقاعدة بيانات محسن ماركت"
        _screenState.value = UiState.Error(msg)
        _uiState.update {
            it.copy(
                isLoading = false,
                isRetrying = false,
                errorMessage = msg
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
        loadDashboardStats()
    }

    fun loadDashboardStats(isUserRetry: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            _screenState.value = UiState.Loading
            _uiState.update {
                it.copy(
                    isLoading = !isUserRetry,
                    isRetrying = isUserRetry,
                    errorMessage = null
                )
            }
            val result = repository.getTodayQuickStats()
            result.onSuccess { stats ->
                _screenState.value = UiState.Success(stats)
                _uiState.update {
                    it.copy(
                        stats = stats,
                        isLoading = false,
                        isRetrying = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                val msg = error.message ?: "تعذر جلب إحصائيات اليوم من سيرفر SQL Server. اضغط إعادة المحاولة 🔄"
                _screenState.value = UiState.Error(msg)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRetrying = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun retry() {
        loadDashboardStats(isUserRetry = true)
    }
}
