package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.SupplierBill
import com.example.data.model.roundMoney
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class SupplierBillsUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Form inputs (New Bill Creation)
    val showCreateBillDialog: Boolean = false,
    val supplierNameInput: String = "",
    val supplierSuggestions: List<String> = emptyList(),
    val filteredSuggestions: List<String> = emptyList(),
    val supplierOldDebt: Double? = null,
    val isLoadingSupplierDebt: Boolean = false,
    val totalAmountInput: String = "",
    val paidAmountInput: String = "",
    val billDate: LocalDate = LocalDate.now(),
    val notesInput: String = "",
    val isFawry: Boolean = false, // تم الدفع إلكترونياً / فوري (لا يُخصم من درج الكاش)
    val isSavingBill: Boolean = false,

    // Ledger / List & Filters
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedSupplierFilter: String? = null, // null = All
    val bills: List<SupplierBill> = emptyList(),
    val isLoadingBills: Boolean = false,
    val billToDelete: SupplierBill? = null,
    val isDeletingBill: Boolean = false,

    // Persistent Bottom Stats Bar
    val allTimeCumulativeDebt: Double = 0.0,
    val filteredSupplierCumulativeDebt: Double? = null,
    val monthlyPurchasesTotal: Double = 0.0,
    val selectedSupplierMonthlyPurchases: Double = 0.0
) {
    val totalAmount: Double
        get() = totalAmountInput.trim().toDoubleOrNull() ?: 0.0

    val paidAmount: Double
        get() = paidAmountInput.trim().toDoubleOrNull() ?: 0.0

    val isPaymentOnlyMode: Boolean
        get() = totalAmount <= 0.001 && paidAmount > 0.001

    val remainingAmount: Double
        get() = roundMoney((totalAmount - paidAmount).coerceAtLeast(0.0))

    val netDebtChange: Double
        get() = roundMoney(totalAmount - paidAmount)

    val isFormValid: Boolean
        get() {
            val rawTotal = totalAmountInput.trim()
            val rawPaid = paidAmountInput.trim()
            val totalValid = rawTotal.isEmpty() || (rawTotal.toDoubleOrNull()?.let { it >= 0.0 } == true)
            val paidValid = rawPaid.isEmpty() || (rawPaid.toDoubleOrNull()?.let { it >= 0.0 } == true)
            return supplierNameInput.isNotBlank() &&
                totalValid &&
                paidValid &&
                (totalAmount > 0.0 || paidAmount > 0.0)
        }
}

class SupplierBillsViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierBillsUiState())
    val uiState: StateFlow<SupplierBillsUiState> = _uiState.asStateFlow()

    private var debtLookupJob: Job? = null

    init {
        observeConnection()
        loadDistinctSuppliers()
        loadBills()
        loadCumulativeDebt()
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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun openCreateBillDialog() {
        _uiState.update {
            it.copy(
                showCreateBillDialog = true,
                supplierNameInput = "",
                totalAmountInput = "",
                paidAmountInput = "",
                billDate = LocalDate.now(),
                notesInput = "",
                isFawry = false,
                supplierOldDebt = null
            )
        }
    }

    fun dismissCreateBillDialog() {
        _uiState.update { it.copy(showCreateBillDialog = false) }
    }

    fun loadDistinctSuppliers() {
        viewModelScope.launch {
            repository.getDistinctSupplierNames().fold(
                onSuccess = { names ->
                    _uiState.update { it.copy(supplierSuggestions = names) }
                },
                onFailure = { /* non-fatal */ }
            )
        }
    }

    fun onSupplierNameChanged(name: String) {
        val allSuggestions = _uiState.value.supplierSuggestions
        val filtered = if (name.isBlank()) {
            emptyList()
        } else {
            allSuggestions.filter { it.contains(name.trim(), ignoreCase = true) }
        }

        _uiState.update {
            it.copy(
                supplierNameInput = name,
                filteredSuggestions = filtered
            )
        }

        lookupSupplierDebt(name.trim())
    }

    fun selectSupplierSuggestion(name: String) {
        _uiState.update {
            it.copy(
                supplierNameInput = name,
                filteredSuggestions = emptyList()
            )
        }
        lookupSupplierDebt(name.trim())
    }

    private fun lookupSupplierDebt(supplierName: String) {
        debtLookupJob?.cancel()
        if (supplierName.isBlank()) {
            _uiState.update { it.copy(supplierOldDebt = null, isLoadingSupplierDebt = false) }
            return
        }

        debtLookupJob = viewModelScope.launch {
            delay(250) // debounce
            _uiState.update { it.copy(isLoadingSupplierDebt = true) }
            repository.getSupplierDebt(supplierName).fold(
                onSuccess = { debt ->
                    _uiState.update { it.copy(supplierOldDebt = debt, isLoadingSupplierDebt = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(supplierOldDebt = 0.0, isLoadingSupplierDebt = false) }
                }
            )
        }
    }

    fun onTotalAmountChanged(text: String) {
        _uiState.update { it.copy(totalAmountInput = text) }
    }

    fun onPaidAmountChanged(text: String) {
        _uiState.update { it.copy(paidAmountInput = text) }
    }

    fun setPaidInFull() {
        val total = _uiState.value.totalAmountInput
        _uiState.update { it.copy(paidAmountInput = total) }
    }

    fun setOldDebtPaymentMode() {
        _uiState.update { it.copy(totalAmountInput = "0") }
    }

    fun onBillDateChanged(date: LocalDate) {
        _uiState.update { it.copy(billDate = date) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun onIsFawryChanged(isFawry: Boolean) {
        _uiState.update { it.copy(isFawry = isFawry) }
    }

    fun saveBill() {
        val state = _uiState.value
        val supplierName = state.supplierNameInput.trim()
        val rawTotal = state.totalAmountInput.trim()
        val rawPaid = state.paidAmountInput.trim()
        val total = if (rawTotal.isEmpty()) 0.0 else rawTotal.toDoubleOrNull()
        val paid = if (rawPaid.isEmpty()) 0.0 else rawPaid.toDoubleOrNull()

        if (supplierName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المورد أو التاجر") }
            return
        }
        if (total == null || total < 0.0) {
            _uiState.update { it.copy(errorMessage = "إجمالي الفاتورة غير صالح أو سالب") }
            return
        }
        if (paid == null || paid < 0.0) {
            _uiState.update { it.copy(errorMessage = "المبلغ المدفوع غير صالح أو سالب") }
            return
        }
        if (total == 0.0 && paid == 0.0) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال قيمة الفاتورة أو مبلغ الدفعة المدفوعة") }
            return
        }

        val isPaymentOnly = total <= 0.001 && paid > 0.001

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingBill = true, errorMessage = null) }

            val bill = SupplierBill(
                id = 0,
                supplierName = supplierName,
                totalAmount = total,
                paidAmount = paid,
                billDate = state.billDate,
                notes = state.notesInput.trim(),
                isFawry = state.isFawry
            )

            repository.saveSupplierBill(bill, state.isFawry).fold(
                onSuccess = { _ ->
                    val fawryMsg = if (state.isFawry) " (سداد فوري - لم يُخصم من درج الكاش)" else " (سداد نقدي من الدرج)"
                    val successMsg = if (isPaymentOnly) {
                        "تم تسجيل دفعة سداد لـ $supplierName وخصمها من الحساب القديم$fawryMsg"
                    } else {
                        "تم حفظ فاتورة $supplierName بنجاح$fawryMsg"
                    }
                    _uiState.update {
                        it.copy(
                            isSavingBill = false,
                            showCreateBillDialog = false,
                            successMessage = successMsg
                        )
                    }
                    // Refresh data
                    loadDistinctSuppliers()
                    loadBills()
                    loadCumulativeDebt()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSavingBill = false,
                            errorMessage = "تعذر حفظ الفاتورة: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    // =========================================================================
    // Ledger & Filtering
    // =========================================================================

    fun setYearMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedYearMonth = yearMonth) }
        loadBills()
    }

    fun nextMonth() {
        setYearMonth(_uiState.value.selectedYearMonth.plusMonths(1))
    }

    fun prevMonth() {
        setYearMonth(_uiState.value.selectedYearMonth.minusMonths(1))
    }

    fun setSupplierFilter(supplier: String?) {
        _uiState.update { it.copy(selectedSupplierFilter = supplier) }
        loadBills()
        if (supplier != null) {
            viewModelScope.launch {
                repository.getSupplierDebt(supplier).fold(
                    onSuccess = { d -> _uiState.update { it.copy(filteredSupplierCumulativeDebt = d) } },
                    onFailure = { _uiState.update { it.copy(filteredSupplierCumulativeDebt = null) } }
                )
            }
        } else {
            _uiState.update { it.copy(filteredSupplierCumulativeDebt = null) }
        }
    }

    fun loadBills() {
        val ym = _uiState.value.selectedYearMonth
        val supplierFilter = _uiState.value.selectedSupplierFilter
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBills = true) }
            repository.getSupplierBillsForMonth(
                year = ym.year,
                month = ym.monthValue,
                supplierName = supplierFilter
            ).fold(
                onSuccess = { list ->
                    val monthlyTotal = list.sumOf { it.totalAmount }
                    val selectedSupplierTotal = if (supplierFilter != null) {
                        list.filter { it.supplierName == supplierFilter }.sumOf { it.totalAmount }
                    } else 0.0

                    _uiState.update {
                        it.copy(
                            bills = list,
                            monthlyPurchasesTotal = monthlyTotal,
                            selectedSupplierMonthlyPurchases = selectedSupplierTotal,
                            isLoadingBills = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingBills = false,
                            errorMessage = "تعذر تحميل فواتير الموردين: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun loadCumulativeDebt() {
        viewModelScope.launch {
            repository.getCumulativeSupplierDebt().fold(
                onSuccess = { debt ->
                    _uiState.update { it.copy(allTimeCumulativeDebt = debt) }
                },
                onFailure = { /* non-fatal */ }
            )
        }
    }

    fun requestDeleteBill(bill: SupplierBill) {
        _uiState.update { it.copy(billToDelete = bill) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(billToDelete = null) }
    }

    fun confirmDeleteBill() {
        val bill = _uiState.value.billToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingBill = true, errorMessage = null) }
            repository.deleteSupplierBill(bill.id, bill.billDate).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeletingBill = false,
                            billToDelete = null,
                            successMessage = "تم حذف فاتورة ${bill.supplierName} وتحديث اليومية تلقائياً"
                        )
                    }
                    loadBills()
                    loadCumulativeDebt()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isDeletingBill = false,
                            errorMessage = "فشل حذف الفاتورة: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }
}
