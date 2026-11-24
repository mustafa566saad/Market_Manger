package com.example.data.repository

import com.example.data.model.Attendance
import com.example.data.model.ConnectionStatus
import com.example.data.model.DailyClosingPreview
import com.example.data.model.DailyTreasury
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategorySummary
import com.example.data.model.MonthSummaryData
import com.example.data.model.MonthlyFinancialAnalysis
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierDebtSummary
import com.example.data.model.SupplierMonthlySummary
import com.example.data.model.TodayQuickStats
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Repository coordinating all business and financial operations for Mohsen Market.
 * Delegates directly to MohsenMarketRepository and manages model conversions.
 */
class MarketRepository(
    private val repo: MohsenMarketRepository = MohsenMarketRepository()
) {

    val connectionStatus: StateFlow<ConnectionStatus> = repo.connectionStatus
    val lastErrorMessage: StateFlow<String?> = repo.lastErrorMessage

    suspend fun testConnection(): Boolean = repo.testConnection()

    private fun Date.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun Date.toLocalDateTime(): LocalDateTime =
        Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDateTime()

    // =========================================================================
    // Employees
    // =========================================================================

    suspend fun getEmployees(includeInactive: Boolean = true): Result<List<Employee>> =
        repo.getAllEmployees(includeInactive)

    suspend fun addEmployee(employee: Employee): Result<Int> =
        repo.insertEmployee(employee)

    suspend fun updateEmployee(employee: Employee): Result<Boolean> =
        repo.updateEmployee(employee)

    suspend fun deleteEmployee(employeeId: Int): Result<Boolean> =
        repo.deleteEmployee(employeeId)

    // =========================================================================
    // Attendances
    // =========================================================================

    suspend fun getAttendances(dateFilter: LocalDate? = null, employeeId: Int? = null): Result<List<Attendance>> =
        repo.getAttendances(dateFilter, employeeId)

    suspend fun getAttendances(dateFilter: Date, employeeId: Int? = null): Result<List<Attendance>> =
        repo.getAttendances(dateFilter.toLocalDate(), employeeId)

    suspend fun getAttendancesForMonth(year: Int, month: Int, employeeId: Int? = null): Result<List<Attendance>> =
        repo.getAttendancesForMonth(year, month, employeeId)

    suspend fun getActiveAttendances(): Result<List<Attendance>> =
        repo.getActiveAttendances()

    suspend fun checkIn(employeeId: Int, checkInTime: Date = Date()): Result<Int> =
        repo.recordCheckIn(employeeId, checkInTime.toLocalDateTime())

    suspend fun checkIn(employeeId: Int, checkInTime: LocalDateTime): Result<Int> =
        repo.recordCheckIn(employeeId, checkInTime)

    suspend fun checkOut(attendanceId: Int, checkOutTime: Date = Date()): Result<Boolean> =
        repo.recordCheckOut(attendanceId, checkOutTime.toLocalDateTime())

    suspend fun checkOut(attendanceId: Int, checkOutTime: LocalDateTime): Result<Boolean> =
        repo.recordCheckOut(attendanceId, checkOutTime)

    suspend fun saveAttendance(attendance: Attendance): Result<Boolean> =
        repo.insertOrUpdateAttendance(attendance)

    suspend fun deleteAttendance(attendanceId: Int): Result<Boolean> =
        repo.deleteAttendance(attendanceId)

    // =========================================================================
    // Supplier Bills
    // =========================================================================

    suspend fun getSupplierBills(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        supplierName: String? = null
    ): Result<List<SupplierBill>> =
        repo.getSupplierBills(startDate, endDate, supplierName)

    suspend fun getSupplierBills(
        startDate: Date,
        endDate: Date? = null,
        supplierName: String? = null
    ): Result<List<SupplierBill>> =
        repo.getSupplierBills(startDate.toLocalDate(), endDate?.toLocalDate(), supplierName)

    suspend fun saveSupplierBill(bill: SupplierBill, isFawry: Boolean): Result<Int> {
        return if (bill.id == 0) {
            repo.insertSupplierBill(bill, isFawry)
        } else {
            repo.updateSupplierBill(bill, isFawry).map { bill.id }
        }
    }

    suspend fun deleteSupplierBill(billId: Int, billDate: LocalDate): Result<Boolean> =
        repo.deleteSupplierBill(billId, billDate)

    suspend fun deleteSupplierBill(billId: Int, billDate: Date): Result<Boolean> =
        repo.deleteSupplierBill(billId, billDate.toLocalDate())

    suspend fun getCumulativeSupplierDebt(): Result<Double> =
        repo.getCumulativeSupplierDebt()

    suspend fun getDistinctSupplierNames(): Result<List<String>> =
        repo.getDistinctSupplierNames()

    suspend fun getSupplierDebt(supplierName: String): Result<Double> =
        repo.getSupplierDebt(supplierName)

    suspend fun getSupplierBillsForMonth(
        year: Int,
        month: Int,
        supplierName: String? = null
    ): Result<List<SupplierBill>> =
        repo.getSupplierBillsForMonth(year, month, supplierName)

    // =========================================================================
    // Expenses
    // =========================================================================

    suspend fun getExpenses(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        expenseType: String? = null
    ): Result<List<Expense>> =
        repo.getExpenses(startDate, endDate, expenseType)

    suspend fun getExpenses(
        startDate: Date,
        endDate: Date? = null,
        expenseType: String? = null
    ): Result<List<Expense>> =
        repo.getExpenses(startDate.toLocalDate(), endDate?.toLocalDate(), expenseType)


    suspend fun saveExpense(expense: Expense): Result<Int> {
        return if (expense.id == 0) {
            repo.insertExpense(expense)
        } else {
            repo.updateExpense(expense).map { expense.id }
        }
    }

    suspend fun deleteExpense(expenseId: Int, expenseDate: LocalDate): Result<Boolean> =
        repo.deleteExpense(expenseId, expenseDate)

    suspend fun deleteExpense(expenseId: Int, expenseDate: Date): Result<Boolean> =
        repo.deleteExpense(expenseId, expenseDate.toLocalDate())

    suspend fun getMonthlyExpenseBreakdown(year: Int, month: Int): Result<List<ExpenseCategorySummary>> =
        repo.getMonthlyExpenseBreakdown(year, month)

    suspend fun isTreasuryDateClosed(date: LocalDate): Result<Boolean> =
        repo.isTreasuryDateClosed(date)

    // =========================================================================
    // Daily Treasury & Shift Closing
    // =========================================================================

    suspend fun getDailyClosingPreview(targetDate: Date, cashInDrawer: Double): Result<DailyClosingPreview> =
        repo.getDailyClosingPreview(targetDate.toLocalDate(), cashInDrawer)

    suspend fun getDailyClosingPreview(targetDate: LocalDate, cashInDrawer: Double): Result<DailyClosingPreview> =
        repo.getDailyClosingPreview(targetDate, cashInDrawer)

    suspend fun saveDailyClosing(targetDate: Date, cashInDrawer: Double): Result<DailyTreasury> =
        repo.saveDailyClosing(targetDate.toLocalDate(), cashInDrawer)

    suspend fun saveDailyClosing(targetDate: LocalDate, cashInDrawer: Double): Result<DailyTreasury> =
        repo.saveDailyClosing(targetDate, cashInDrawer)

    suspend fun getAllDailyTreasuries(): Result<List<DailyTreasury>> =
        repo.getAllDailyTreasuries()

    suspend fun getDailyTreasuriesForMonth(year: Int, month: Int): Result<List<DailyTreasury>> =
        repo.getDailyTreasuriesForMonth(year, month)

    suspend fun runRetroactiveAdjustment(): Result<Int> =
        repo.runRetroactiveTreasuryAdjustment()

    // =========================================================================
    // Monthly Financial Analysis (10 KPIs)
    // =========================================================================

    suspend fun getMonthlyFinancialAnalysis(year: Int, month: Int): Result<MonthSummaryData> =
        repo.getMonthSummaryData(year, month)

    // =========================================================================
    // Dashboard & Reports
    // =========================================================================

    suspend fun getTodayQuickStats(): Result<TodayQuickStats> =
        repo.getTodayQuickStats()

    suspend fun getAllSuppliersDebtSummary(): Result<List<SupplierDebtSummary>> =
        repo.getAllSuppliersDebtSummary()

    suspend fun getSuppliersMonthlySummary(year: Int, month: Int): Result<List<SupplierMonthlySummary>> =
        repo.getSuppliersMonthlySummary(year, month)
}
