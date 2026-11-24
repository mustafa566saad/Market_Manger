package com.example.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * [TASK 0: DATA MODELS]
 * Complete models matching remote Microsoft SQL Server database schema and business logic,
 * hardened with 2-decimal BigDecimal rounding and normalized string utilities.
 */

fun roundMoney(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return 0.0
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}

fun normalizeSupplierName(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ")

data class Employee(
    val id: Int = 0,
    val employeeName: String,
    val phone: String = "",
    val hourRate: Double = 0.0,
    val isActive: Boolean = true
) {
    val employeeId: Int get() = id
}

data class Attendance(
    val id: Int = 0,
    val employeeId: Int,
    val employeeName: String = "",
    val checkIn: LocalDateTime,
    val checkOut: LocalDateTime? = null,
    val hourRate: Double = 0.0
) {
    // Secondary constructor with legacy Date and attendanceId
    constructor(
        attendanceId: Int,
        employeeId: Int,
        employeeName: String = "",
        hourRate: Double = 0.0,
        checkInDate: Date,
        checkOutDate: Date? = null
    ) : this(
        id = attendanceId,
        employeeId = employeeId,
        employeeName = employeeName,
        checkIn = LocalDateTime.ofInstant(Instant.ofEpochMilli(checkInDate.time), ZoneId.systemDefault()),
        checkOut = checkOutDate?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it.time), ZoneId.systemDefault()) },
        hourRate = hourRate
    )

    val attendanceId: Int get() = id
    val isOngoing: Boolean get() = checkOut == null

    /**
     * Business Rule 1:
     * Shift Cost Formula: ShiftCost = TotalHours * HourlyRateAtShift.
     * Overnight Shifts: If CheckOut time is less than or equal to CheckIn time,
     * the shift automatically spans into the next day (CheckOut + 1 day).
     */
    val totalHours: Double get() {
        if (checkOut == null) return 0.0
        var adjustedCheckOut = checkOut
        if (!adjustedCheckOut.isAfter(checkIn)) {
            adjustedCheckOut = adjustedCheckOut.plusDays(1)
        }
        val duration = Duration.between(checkIn, adjustedCheckOut)
        val minutes = duration.toMinutes().coerceAtLeast(0)
        return roundMoney(minutes / 60.0)
    }

    val shiftCost: Double get() = roundMoney(totalHours * hourRate)

    val isOvernight: Boolean get() {
        if (checkOut == null) return false
        return !checkOut.isAfter(checkIn) || checkOut.toLocalDate() != checkIn.toLocalDate()
    }

    val checkInDate: Date get() = Date.from(checkIn.atZone(ZoneId.systemDefault()).toInstant())
    val checkOutDate: Date? get() = checkOut?.let { Date.from(it.atZone(ZoneId.systemDefault()).toInstant()) }
}

data class Expense(
    val id: Int = 0,
    val expenseType: String,
    val amount: Double,
    val expenseDate: LocalDate,
    val notes: String? = null
) {
    // Secondary constructor with legacy Date and expenseId
    constructor(
        expenseId: Int,
        expenseType: String,
        amount: Double,
        legacyDate: Date,
        notes: String? = null
    ) : this(
        id = expenseId,
        expenseType = expenseType,
        amount = amount,
        expenseDate = Instant.ofEpochMilli(legacyDate.time).atZone(ZoneId.systemDefault()).toLocalDate(),
        notes = notes
    )

    val expenseId: Int get() = id
    val dateAsLegacyDate: Date get() = Date.from(expenseDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

data class ExpenseCategorySummary(
    val expenseType: String,
    val totalAmount: Double,
    val count: Int = 1
)

data class SupplierBill(
    val id: Int = 0,
    val supplierName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double = roundMoney((totalAmount - paidAmount).coerceAtLeast(0.0)),
    val billDate: LocalDate,
    val notes: String? = null,
    val isFawry: Boolean = notes?.contains("[فوري]") == true
) {
    // Secondary constructor with legacy Date and billId
    constructor(
        billId: Int,
        supplierName: String,
        totalAmount: Double,
        paidAmount: Double,
        legacyDate: Date,
        notes: String? = null
    ) : this(
        id = billId,
        supplierName = supplierName,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        remainingAmount = roundMoney((totalAmount - paidAmount).coerceAtLeast(0.0)),
        billDate = Instant.ofEpochMilli(legacyDate.time).atZone(ZoneId.systemDefault()).toLocalDate(),
        notes = notes,
        isFawry = notes?.contains("[فوري]") == true
    )

    val billId: Int get() = id
    val cleanNotes: String get() = (notes ?: "").replace("[فوري]", "").trim()
    val isOldDebtPaymentOnly: Boolean get() = totalAmount <= 0.001 && paidAmount > 0.001
    val netDebtChange: Double get() = roundMoney(totalAmount - paidAmount)
    val isFullyPaid: Boolean get() = remainingAmount <= 0.01
    val isUnpaid: Boolean get() = paidAmount <= 0.01
    val isPartiallyPaid: Boolean get() = !isFullyPaid && !isUnpaid
    val dateAsLegacyDate: Date get() = Date.from(billDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

data class DailyTreasury(
    val id: Int = 0,
    val date: LocalDate,
    val totalSales: Double,
    val totalExpenses: Double,
    val totalSupplierPayments: Double,
    val netCash: Double = roundMoney(totalSales - totalExpenses - totalSupplierPayments),
    val profit: Double = roundMoney(totalSales * 0.10),
    val actualDrawerCash: Double? = null
) {
    // Secondary constructor with legacy Date and treasuryId
    constructor(
        treasuryId: Int,
        legacyDate: Date,
        totalSales: Double,
        totalExpenses: Double,
        totalSupplierPayments: Double
    ) : this(
        id = treasuryId,
        date = Instant.ofEpochMilli(legacyDate.time).atZone(ZoneId.systemDefault()).toLocalDate(),
        totalSales = totalSales,
        totalExpenses = totalExpenses,
        totalSupplierPayments = totalSupplierPayments,
        netCash = roundMoney(totalSales - totalExpenses - totalSupplierPayments),
        profit = roundMoney(totalSales * 0.10)
    )

    val treasuryId: Int get() = id
    val estimatedProfit: Double get() = profit
    val dateAsLegacyDate: Date get() = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

data class DailyClosingPreview(
    val date: LocalDate,
    val previousCash: Double,
    val cashInDrawer: Double,
    val totalExpenses: Double,
    val cashSupplierPayments: Double,
    val fawrySupplierPayments: Double,
    val totalSupplierPaymentsCombined: Double,
    val calculatedTotalSales: Double,
    val estimatedProfit: Double,
    val isAlreadyClosed: Boolean
) {
    val dateAsLegacyDate: Date get() = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

data class SupplierDebtSummary(
    val supplierName: String,
    val totalInvoiced: Double,
    val totalPaid: Double,
    val remainingDebt: Double
)

data class SupplierMonthlySummary(
    val supplierName: String,
    val monthlyPurchases: Double,
    val monthlyPaid: Double,
    val monthlyNewDebt: Double = roundMoney(monthlyPurchases - monthlyPaid),
    val cumulativeDebt: Double,
    val allTimeInvoiced: Double = 0.0,
    val allTimePaid: Double = 0.0
)

data class MonthSummaryData(
    val year: Int,
    val month: Int,
    // 10 Key Performance Indicators
    val totalSales: Double,
    val grossProfit10Percent: Double,
    val totalPurchases: Double,
    val totalSupplierPayments: Double,
    val cashSupplierPayments: Double,
    val fawrySupplierPayments: Double,
    val monthlySupplierDebt: Double,
    val cumulativeSupplierDebt: Double,
    val operatingExpenses: Double,
    val employeeWages: Double,
    val netOperatingProfit: Double,
    val netDrawerCashFlow: Double,
    // Breakdowns
    val supplierDebts: List<SupplierDebtSummary> = emptyList(),
    val expenseBreakdown: Map<String, Double> = emptyMap(),
    val closedDaysCount: Int = 0,
    // Operational Activity Metrics
    val purchaseBillsCount: Int = 0,
    val supplierCount: Int = supplierDebts.size,
    val expenseCount: Int = 0,
    val attendanceShiftsCount: Int = 0,
    val totalStaffHours: Double = 0.0
) {
    // Compatibility aliases for UI
    val supplierDebtsList: List<SupplierDebtSummary> get() = supplierDebts
    val expenseCategoryBreakdown: Map<String, Double> get() = expenseBreakdown
    val dailySalesAverage: Double
        get() = if (closedDaysCount > 0) roundMoney(totalSales / closedDaysCount) else 0.0
}

typealias MonthlyFinancialAnalysis = MonthSummaryData

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    RETRYING,
    DISCONNECTED,
    ERROR
}

data class TodayQuickStats(
    val date: LocalDate = LocalDate.now(),
    val todaySales: Double = 0.0,
    val expectedDrawerCash: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val cashSupplierPayments: Double = 0.0,
    val fawrySupplierPayments: Double = 0.0,
    val activeStaffCount: Int = 0,
    val totalEmployees: Int = 0,
    val cumulativeSupplierDebt: Double = 0.0,
    val isTodayClosed: Boolean = false,
    val monthSales: Double = 0.0,
    val monthPurchases: Double = 0.0,
    val monthClosedDaysCount: Int = 0
)

