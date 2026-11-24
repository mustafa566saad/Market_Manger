package com.example.data.repository

import android.util.Log
import com.example.data.db.DatabaseClient
import com.example.data.db.SqlServerConnectionProvider
import com.example.data.model.Attendance
import com.example.data.model.ConnectionStatus
import com.example.data.model.DailyClosingPreview
import com.example.data.model.DailyTreasury
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategorySummary
import com.example.data.model.MonthSummaryData
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierDebtSummary
import com.example.data.model.SupplierMonthlySummary
import com.example.data.model.TodayQuickStats
import com.example.data.model.normalizeSupplierName
import com.example.data.model.roundMoney
import kotlinx.coroutines.flow.StateFlow
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * [MOHSEN MARKET REPOSITORY — CRASH-PROOF & 100% SQL SERVER COMPATIBLE]
 * Executes all queries strictly on Dispatchers.IO via DatabaseClient.safeQuery / safeTransaction.
 * Matches the shared Desktop C# / .NET & Android SQL Server schema on db79344.
 */
class MohsenMarketRepository(
    private val client: SqlServerConnectionProvider = SqlServerConnectionProvider
) {
    private val tag = "MohsenMarketRepo"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val connectionStatus: StateFlow<ConnectionStatus> = DatabaseClient.connectionStatus
    val lastErrorMessage: StateFlow<String?> = DatabaseClient.lastErrorMessage

    private fun Timestamp.toLocalDateTimeSafe(): LocalDateTime =
        Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun Timestamp.toLocalDateSafe(): LocalDate =
        toLocalDateTimeSafe().toLocalDate()

    private fun LocalDateTime.toTimestampSafe(): Timestamp =
        Timestamp(this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())

    private fun LocalDate.toTimestampSafe(): Timestamp =
        Timestamp(this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

    suspend fun testConnection(): Boolean = DatabaseClient.testConnection()

    // =========================================================================
    // 1. EMPLOYEES CRUD
    // =========================================================================

    suspend fun getAllEmployees(includeInactive: Boolean = true): Result<List<Employee>> {
        return DatabaseClient.safeQuery { conn ->
            val sql = if (includeInactive) {
                "SELECT EmployeeId, EmployeeName, Phone, HourRate, IsActive FROM Employees ORDER BY IsActive DESC, EmployeeName ASC"
            } else {
                "SELECT EmployeeId, EmployeeName, Phone, HourRate, IsActive FROM Employees WHERE IsActive = 1 ORDER BY EmployeeName ASC"
            }
            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Employee>()
                    while (rs.next()) {
                        list.add(
                            Employee(
                                id = rs.getInt("EmployeeId"),
                                employeeName = rs.getString("EmployeeName")?.trim() ?: "",
                                phone = rs.getString("Phone")?.trim() ?: "",
                                hourRate = roundMoney(rs.getDouble("HourRate")),
                                isActive = rs.getBoolean("IsActive")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getEmployeeById(id: Int): Result<Employee?> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "SELECT EmployeeId, EmployeeName, Phone, HourRate, IsActive FROM Employees WHERE EmployeeId = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        Employee(
                            id = rs.getInt("EmployeeId"),
                            employeeName = rs.getString("EmployeeName")?.trim() ?: "",
                            phone = rs.getString("Phone")?.trim() ?: "",
                            hourRate = roundMoney(rs.getDouble("HourRate")),
                            isActive = rs.getBoolean("IsActive")
                        )
                    } else null
                }
            }
        }
    }

    suspend fun insertEmployee(employee: Employee): Result<Int> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "INSERT INTO Employees (EmployeeName, Phone, HourRate, IsActive) VALUES (?, ?, ?, ?)"
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                ps.setString(1, employee.employeeName.trim())
                ps.setString(2, employee.phone.trim())
                ps.setDouble(3, roundMoney(employee.hourRate))
                ps.setBoolean(4, employee.isActive)
                ps.executeUpdate()

                var generatedId = 0
                ps.generatedKeys.use { rs ->
                    if (rs.next()) generatedId = rs.getInt(1)
                }
                generatedId
            }
        }
    }

    suspend fun updateEmployee(employee: Employee): Result<Boolean> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "UPDATE Employees SET EmployeeName = ?, Phone = ?, HourRate = ?, IsActive = ? WHERE EmployeeId = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, employee.employeeName.trim())
                ps.setString(2, employee.phone.trim())
                ps.setDouble(3, roundMoney(employee.hourRate))
                ps.setBoolean(4, employee.isActive)
                ps.setInt(5, employee.id)
                ps.executeUpdate() > 0
            }
        }
    }

    suspend fun deleteEmployee(id: Int): Result<Boolean> {
        return DatabaseClient.safeTransaction { conn ->
            val countSql = "SELECT COUNT(*) FROM Attendances WHERE EmployeeId = ?"
            val hasShifts = conn.prepareStatement(countSql).use { ps ->
                ps.setInt(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) > 0 else false
                }
            }

            if (hasShifts) {
                val softSql = "UPDATE Employees SET IsActive = 0 WHERE EmployeeId = ?"
                conn.prepareStatement(softSql).use { ps ->
                    ps.setInt(1, id)
                    ps.executeUpdate() > 0
                }
            } else {
                val delSql = "DELETE FROM Employees WHERE EmployeeId = ?"
                conn.prepareStatement(delSql).use { ps ->
                    ps.setInt(1, id)
                    ps.executeUpdate() > 0
                }
            }
        }
    }

    // =========================================================================
    // 2. ATTENDANCES & SHIFTS CRUD
    // =========================================================================

    suspend fun getAttendances(dateFilter: LocalDate? = null, employeeId: Int? = null): Result<List<Attendance>> {
        return DatabaseClient.safeQuery { conn ->
            val sb = StringBuilder(
                """
                SELECT a.AttendanceId, a.EmployeeId, e.EmployeeName, e.HourRate, a.CheckIn, a.CheckOut 
                FROM Attendances a
                INNER JOIN Employees e ON a.EmployeeId = e.EmployeeId
                WHERE 1=1
                """.trimIndent()
            )

            if (dateFilter != null) {
                sb.append(" AND CAST(a.CheckIn AS DATE) = ?")
            }
            if (employeeId != null) {
                sb.append(" AND a.EmployeeId = ?")
            }
            sb.append(" ORDER BY a.CheckIn DESC")

            conn.prepareStatement(sb.toString()).use { ps ->
                var pIdx = 1
                if (dateFilter != null) {
                    ps.setString(pIdx++, dateFilter.format(dateFormatter))
                }
                if (employeeId != null) {
                    ps.setInt(pIdx, employeeId)
                }

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Attendance>()
                    while (rs.next()) {
                        val checkInTs = rs.getTimestamp("CheckIn")
                        val checkOutTs = rs.getTimestamp("CheckOut")
                        list.add(
                            Attendance(
                                id = rs.getInt("AttendanceId"),
                                employeeId = rs.getInt("EmployeeId"),
                                employeeName = rs.getString("EmployeeName") ?: "",
                                hourRate = roundMoney(rs.getDouble("HourRate")),
                                checkIn = checkInTs.toLocalDateTimeSafe(),
                                checkOut = checkOutTs?.toLocalDateTimeSafe()
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getAttendancesLegacy(dateFilter: Date? = null, employeeId: Int? = null): Result<List<Attendance>> {
        val localDate = dateFilter?.let { Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() }
        return getAttendances(localDate, employeeId)
    }

    suspend fun getAttendancesForMonth(year: Int, month: Int, employeeId: Int? = null): Result<List<Attendance>> {
        return DatabaseClient.safeQuery { conn ->
            val startDate = LocalDate.of(year, month, 1)
            val endDate = startDate.plusMonths(1).minusDays(1)
            val sb = StringBuilder(
                """
                SELECT a.AttendanceId, a.EmployeeId, e.EmployeeName, e.HourRate, a.CheckIn, a.CheckOut 
                FROM Attendances a
                INNER JOIN Employees e ON a.EmployeeId = e.EmployeeId
                WHERE CAST(a.CheckIn AS DATE) >= ? AND CAST(a.CheckIn AS DATE) <= ?
                """.trimIndent()
            )
            if (employeeId != null) {
                sb.append(" AND a.EmployeeId = ?")
            }
            sb.append(" ORDER BY a.CheckIn DESC")

            conn.prepareStatement(sb.toString()).use { ps ->
                ps.setString(1, startDate.format(dateFormatter))
                ps.setString(2, endDate.format(dateFormatter))
                if (employeeId != null) {
                    ps.setInt(3, employeeId)
                }

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Attendance>()
                    while (rs.next()) {
                        val checkInTs = rs.getTimestamp("CheckIn")
                        val checkOutTs = rs.getTimestamp("CheckOut")
                        list.add(
                            Attendance(
                                id = rs.getInt("AttendanceId"),
                                employeeId = rs.getInt("EmployeeId"),
                                employeeName = rs.getString("EmployeeName") ?: "",
                                hourRate = roundMoney(rs.getDouble("HourRate")),
                                checkIn = checkInTs.toLocalDateTimeSafe(),
                                checkOut = checkOutTs?.toLocalDateTimeSafe()
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getActiveAttendances(): Result<List<Attendance>> {
        return DatabaseClient.safeQuery { conn ->
            val sql = """
                SELECT a.AttendanceId, a.EmployeeId, e.EmployeeName, e.HourRate, a.CheckIn, a.CheckOut 
                FROM Attendances a
                INNER JOIN Employees e ON a.EmployeeId = e.EmployeeId
                WHERE a.CheckOut IS NULL
                ORDER BY a.CheckIn ASC
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Attendance>()
                    while (rs.next()) {
                        val checkInTs = rs.getTimestamp("CheckIn")
                        list.add(
                            Attendance(
                                id = rs.getInt("AttendanceId"),
                                employeeId = rs.getInt("EmployeeId"),
                                employeeName = rs.getString("EmployeeName") ?: "",
                                hourRate = roundMoney(rs.getDouble("HourRate")),
                                checkIn = checkInTs.toLocalDateTimeSafe(),
                                checkOut = null
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun recordCheckIn(employeeId: Int, checkIn: LocalDateTime = LocalDateTime.now()): Result<Int> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "INSERT INTO Attendances (EmployeeId, CheckIn, CheckOut) VALUES (?, ?, NULL)"
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                ps.setInt(1, employeeId)
                ps.setTimestamp(2, checkIn.toTimestampSafe())
                ps.executeUpdate()

                var id = 0
                ps.generatedKeys.use { rs ->
                    if (rs.next()) id = rs.getInt(1)
                }
                id
            }
        }
    }

    suspend fun recordCheckOut(attendanceId: Int, checkOut: LocalDateTime = LocalDateTime.now()): Result<Boolean> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "UPDATE Attendances SET CheckOut = ? WHERE AttendanceId = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setTimestamp(1, checkOut.toTimestampSafe())
                ps.setInt(2, attendanceId)
                ps.executeUpdate() > 0
            }
        }
    }

    suspend fun insertOrUpdateAttendance(attendance: Attendance): Result<Boolean> {
        return DatabaseClient.safeQuery { conn ->
            // Overnight shift rule: if (checkOut <= checkIn) checkOut + 1 day
            val effectiveCheckOut = if (attendance.checkOut != null && !attendance.checkOut.isAfter(attendance.checkIn)) {
                attendance.checkOut.plusDays(1)
            } else {
                attendance.checkOut
            }

            if (attendance.id == 0) {
                val sql = "INSERT INTO Attendances (EmployeeId, CheckIn, CheckOut) VALUES (?, ?, ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setInt(1, attendance.employeeId)
                    ps.setTimestamp(2, attendance.checkIn.toTimestampSafe())
                    if (effectiveCheckOut != null) {
                        ps.setTimestamp(3, effectiveCheckOut.toTimestampSafe())
                    } else {
                        ps.setNull(3, java.sql.Types.TIMESTAMP)
                    }
                    ps.executeUpdate() > 0
                }
            } else {
                val sql = "UPDATE Attendances SET EmployeeId = ?, CheckIn = ?, CheckOut = ? WHERE AttendanceId = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setInt(1, attendance.employeeId)
                    ps.setTimestamp(2, attendance.checkIn.toTimestampSafe())
                    if (effectiveCheckOut != null) {
                        ps.setTimestamp(3, effectiveCheckOut.toTimestampSafe())
                    } else {
                        ps.setNull(3, java.sql.Types.TIMESTAMP)
                    }
                    ps.setInt(4, attendance.id)
                    ps.executeUpdate() > 0
                }
            }
        }
    }

    suspend fun deleteAttendance(id: Int): Result<Boolean> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "DELETE FROM Attendances WHERE AttendanceId = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, id)
                ps.executeUpdate() > 0
            }
        }
    }

    // =========================================================================
    // 3. EXPENSES CRUD
    // =========================================================================

    suspend fun getExpenses(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        expenseType: String? = null
    ): Result<List<Expense>> {
        return DatabaseClient.safeQuery { conn ->
            val sb = StringBuilder("SELECT ExpenseId, ExpenseType, Amount, ExpenseDate, Notes FROM Expenses WHERE 1=1")
            if (startDate != null) {
                sb.append(" AND CAST(ExpenseDate AS DATE) >= ?")
            }
            if (endDate != null) {
                sb.append(" AND CAST(ExpenseDate AS DATE) <= ?")
            }
            if (!expenseType.isNullOrBlank()) {
                sb.append(" AND ExpenseType LIKE ?")
            }
            sb.append(" ORDER BY ExpenseDate DESC, ExpenseId DESC")

            conn.prepareStatement(sb.toString()).use { ps ->
                var pIdx = 1
                if (startDate != null) {
                    ps.setString(pIdx++, startDate.format(dateFormatter))
                }
                if (endDate != null) {
                    ps.setString(pIdx++, endDate.format(dateFormatter))
                }
                if (!expenseType.isNullOrBlank()) {
                    ps.setString(pIdx, "%${expenseType.trim()}%")
                }

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Expense>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("ExpenseDate")
                        list.add(
                            Expense(
                                id = rs.getInt("ExpenseId"),
                                expenseType = rs.getString("ExpenseType")?.trim() ?: "",
                                amount = roundMoney(rs.getDouble("Amount")),
                                expenseDate = ts.toLocalDateSafe(),
                                notes = rs.getString("Notes")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun insertExpense(expense: Expense): Result<Int> {
        return DatabaseClient.safeTransaction { conn ->
            val sql = "INSERT INTO Expenses (ExpenseType, Amount, ExpenseDate, Notes) VALUES (?, ?, ?, ?)"
            val id = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                ps.setString(1, expense.expenseType.trim())
                ps.setDouble(2, roundMoney(expense.amount))
                ps.setTimestamp(3, expense.expenseDate.toTimestampSafe())
                ps.setString(4, expense.notes ?: "")
                ps.executeUpdate()

                var genId = 0
                ps.generatedKeys.use { rs ->
                    if (rs.next()) genId = rs.getInt(1)
                }
                genId
            }
            syncTreasuryForDateInternal(conn, expense.expenseDate)
            id
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Boolean> {
        return DatabaseClient.safeTransaction { conn ->
            val sql = "UPDATE Expenses SET ExpenseType = ?, Amount = ?, ExpenseDate = ?, Notes = ? WHERE ExpenseId = ?"
            val ok = conn.prepareStatement(sql).use { ps ->
                ps.setString(1, expense.expenseType.trim())
                ps.setDouble(2, roundMoney(expense.amount))
                ps.setTimestamp(3, expense.expenseDate.toTimestampSafe())
                ps.setString(4, expense.notes ?: "")
                ps.setInt(5, expense.id)
                ps.executeUpdate() > 0
            }
            syncTreasuryForDateInternal(conn, expense.expenseDate)
            ok
        }
    }

    suspend fun deleteExpense(id: Int, expenseDate: LocalDate): Result<Boolean> {
        return DatabaseClient.safeTransaction { conn ->
            val sql = "DELETE FROM Expenses WHERE ExpenseId = ?"
            val ok = conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, id)
                ps.executeUpdate() > 0
            }
            syncTreasuryForDateInternal(conn, expenseDate)
            ok
        }
    }

    suspend fun getMonthlyExpenseBreakdown(year: Int, month: Int): Result<List<ExpenseCategorySummary>> {
        return DatabaseClient.safeQuery { conn ->
            val startDate = LocalDate.of(year, month, 1)
            val endDate = startDate.plusMonths(1).minusDays(1)
            val sql = """
                SELECT ExpenseType,
                       ISNULL(SUM(Amount), 0.0) AS TotalAmount,
                       COUNT(*) AS ItemsCount
                FROM Expenses
                WHERE CAST(ExpenseDate AS DATE) >= ? AND CAST(ExpenseDate AS DATE) <= ?
                GROUP BY ExpenseType
                ORDER BY TotalAmount DESC, ExpenseType ASC
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, startDate.format(dateFormatter))
                ps.setString(2, endDate.format(dateFormatter))
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ExpenseCategorySummary>()
                    while (rs.next()) {
                        list.add(
                            ExpenseCategorySummary(
                                expenseType = rs.getString("ExpenseType")?.trim()?.ifBlank { "نثريات" } ?: "نثريات",
                                totalAmount = roundMoney(rs.getDouble("TotalAmount")),
                                count = rs.getInt("ItemsCount")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun isTreasuryDateClosed(date: LocalDate): Result<Boolean> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "SELECT COUNT(*) FROM DailyTreasuries WHERE CAST(Date AS DATE) = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, date.format(dateFormatter))
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) > 0 else false
                }
            }
        }
    }

    // =========================================================================
    // 4. SUPPLIER BILLS CRUD & FAWRY PROTECTION
    // =========================================================================

    suspend fun getSupplierBills(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        supplierName: String? = null
    ): Result<List<SupplierBill>> {
        return DatabaseClient.safeQuery { conn ->
            val sb = StringBuilder("SELECT BillId, SupplierName, TotalAmount, PaidAmount, BillDate, Notes FROM SupplierBills WHERE 1=1")
            if (startDate != null) {
                sb.append(" AND CAST(BillDate AS DATE) >= ?")
            }
            if (endDate != null) {
                sb.append(" AND CAST(BillDate AS DATE) <= ?")
            }
            if (!supplierName.isNullOrBlank()) {
                sb.append(" AND SupplierName LIKE ?")
            }
            sb.append(" ORDER BY BillDate DESC, BillId DESC")

            conn.prepareStatement(sb.toString()).use { ps ->
                var pIdx = 1
                if (startDate != null) {
                    ps.setString(pIdx++, startDate.format(dateFormatter))
                }
                if (endDate != null) {
                    ps.setString(pIdx++, endDate.format(dateFormatter))
                }
                if (!supplierName.isNullOrBlank()) {
                    ps.setString(pIdx, "%${normalizeSupplierName(supplierName)}%")
                }

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SupplierBill>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("BillDate")
                        val notes = rs.getString("Notes")
                        list.add(
                            SupplierBill(
                                id = rs.getInt("BillId"),
                                supplierName = normalizeSupplierName(rs.getString("SupplierName") ?: ""),
                                totalAmount = roundMoney(rs.getDouble("TotalAmount")),
                                paidAmount = roundMoney(rs.getDouble("PaidAmount")),
                                billDate = ts.toLocalDateSafe(),
                                notes = notes,
                                isFawry = notes?.contains("[فوري]") == true
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun insertSupplierBill(bill: SupplierBill, isFawry: Boolean): Result<Int> {
        val notesToSave = formatFawryNotes(bill.notes, isFawry)
        val cleanName = normalizeSupplierName(bill.supplierName)
        return DatabaseClient.safeTransaction { conn ->
            val sql = "INSERT INTO SupplierBills (SupplierName, TotalAmount, PaidAmount, BillDate, Notes) VALUES (?, ?, ?, ?, ?)"
            val id = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                ps.setString(1, cleanName)
                ps.setDouble(2, roundMoney(bill.totalAmount))
                ps.setDouble(3, roundMoney(bill.paidAmount))
                ps.setTimestamp(4, bill.billDate.toTimestampSafe())
                ps.setString(5, notesToSave)
                ps.executeUpdate()

                var genId = 0
                ps.generatedKeys.use { rs ->
                    if (rs.next()) genId = rs.getInt(1)
                }
                genId
            }
            syncTreasuryForDateInternal(conn, bill.billDate)
            id
        }
    }

    suspend fun updateSupplierBill(bill: SupplierBill, isFawry: Boolean): Result<Boolean> {
        val notesToSave = formatFawryNotes(bill.notes, isFawry)
        val cleanName = normalizeSupplierName(bill.supplierName)
        return DatabaseClient.safeTransaction { conn ->
            val sql = "UPDATE SupplierBills SET SupplierName = ?, TotalAmount = ?, PaidAmount = ?, BillDate = ?, Notes = ? WHERE BillId = ?"
            val ok = conn.prepareStatement(sql).use { ps ->
                ps.setString(1, cleanName)
                ps.setDouble(2, roundMoney(bill.totalAmount))
                ps.setDouble(3, roundMoney(bill.paidAmount))
                ps.setTimestamp(4, bill.billDate.toTimestampSafe())
                ps.setString(5, notesToSave)
                ps.setInt(6, bill.id)
                ps.executeUpdate() > 0
            }
            syncTreasuryForDateInternal(conn, bill.billDate)
            ok
        }
    }

    suspend fun deleteSupplierBill(id: Int, billDate: LocalDate): Result<Boolean> {
        return DatabaseClient.safeTransaction { conn ->
            val sql = "DELETE FROM SupplierBills WHERE BillId = ?"
            val ok = conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, id)
                ps.executeUpdate() > 0
            }
            syncTreasuryForDateInternal(conn, billDate)
            ok
        }
    }

    private fun formatFawryNotes(notes: String?, isFawry: Boolean): String {
        val clean = (notes ?: "").replace("[فوري]", "").trim()
        return if (isFawry) {
            if (clean.isEmpty()) "[فوري]" else "[فوري] $clean"
        } else {
            clean
        }
    }

    suspend fun getCumulativeSupplierDebt(): Result<Double> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "SELECT ISNULL(SUM(TotalAmount - PaidAmount), 0.0) FROM SupplierBills"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs ->
                    if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0
                }
            }
        }
    }

    suspend fun getDistinctSupplierNames(): Result<List<String>> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "SELECT DISTINCT SupplierName FROM SupplierBills WHERE SupplierName IS NOT NULL AND RTRIM(LTRIM(SupplierName)) <> '' ORDER BY SupplierName ASC"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs ->
                    val list = mutableListOf<String>()
                    while (rs.next()) {
                        rs.getString(1)?.let { normalizeSupplierName(it) }?.let {
                            if (it.isNotEmpty() && !list.contains(it)) list.add(it)
                        }
                    }
                    list
                }
            }
        }
    }

    suspend fun getSupplierDebt(supplierName: String): Result<Double> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "SELECT ISNULL(SUM(TotalAmount - PaidAmount), 0.0) FROM SupplierBills WHERE LTRIM(RTRIM(SupplierName)) = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, normalizeSupplierName(supplierName))
                ps.executeQuery().use { rs ->
                    if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0
                }
            }
        }
    }

    suspend fun getSupplierBillsForMonth(
        year: Int,
        month: Int,
        supplierName: String? = null
    ): Result<List<SupplierBill>> {
        return DatabaseClient.safeQuery { conn ->
            val startDate = LocalDate.of(year, month, 1)
            val endDate = startDate.plusMonths(1).minusDays(1)
            val sb = StringBuilder(
                "SELECT BillId, SupplierName, TotalAmount, PaidAmount, BillDate, Notes FROM SupplierBills WHERE CAST(BillDate AS DATE) >= ? AND CAST(BillDate AS DATE) <= ?"
            )
            if (!supplierName.isNullOrBlank()) {
                sb.append(" AND LTRIM(RTRIM(SupplierName)) = ?")
            }
            sb.append(" ORDER BY BillDate DESC, BillId DESC")

            conn.prepareStatement(sb.toString()).use { ps ->
                ps.setString(1, startDate.format(dateFormatter))
                ps.setString(2, endDate.format(dateFormatter))
                if (!supplierName.isNullOrBlank()) {
                    ps.setString(3, normalizeSupplierName(supplierName))
                }

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SupplierBill>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("BillDate")
                        val notes = rs.getString("Notes")
                        list.add(
                            SupplierBill(
                                id = rs.getInt("BillId"),
                                supplierName = normalizeSupplierName(rs.getString("SupplierName") ?: ""),
                                totalAmount = roundMoney(rs.getDouble("TotalAmount")),
                                paidAmount = roundMoney(rs.getDouble("PaidAmount")),
                                billDate = ts.toLocalDateSafe(),
                                notes = notes,
                                isFawry = notes?.contains("[فوري]") == true
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getAllSuppliersDebtSummary(): Result<List<SupplierDebtSummary>> {
        return DatabaseClient.safeQuery { conn ->
            val sql = """
                SELECT SupplierName,
                       ISNULL(SUM(TotalAmount), 0.0) AS TotalInvoiced,
                       ISNULL(SUM(PaidAmount), 0.0) AS TotalPaid
                FROM SupplierBills
                GROUP BY SupplierName
                ORDER BY (ISNULL(SUM(TotalAmount), 0.0) - ISNULL(SUM(PaidAmount), 0.0)) DESC, SupplierName ASC
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SupplierDebtSummary>()
                    while (rs.next()) {
                        val name = normalizeSupplierName(rs.getString("SupplierName") ?: "")
                        val invoiced = roundMoney(rs.getDouble("TotalInvoiced"))
                        val paid = roundMoney(rs.getDouble("TotalPaid"))
                        val debt = roundMoney((invoiced - paid).coerceAtLeast(0.0))
                        list.add(
                            SupplierDebtSummary(
                                supplierName = name,
                                totalInvoiced = invoiced,
                                totalPaid = paid,
                                remainingDebt = debt
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getSuppliersMonthlySummary(year: Int, month: Int): Result<List<SupplierMonthlySummary>> {
        return DatabaseClient.safeQuery { conn ->
            val startDate = LocalDate.of(year, month, 1).format(dateFormatter)
            val endDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).format(dateFormatter)

            val sql = """
                SELECT SupplierName,
                       ISNULL(SUM(CASE WHEN CAST(BillDate AS DATE) >= ? AND CAST(BillDate AS DATE) <= ? THEN TotalAmount ELSE 0.0 END), 0.0) AS MonthlyPurchases,
                       ISNULL(SUM(CASE WHEN CAST(BillDate AS DATE) >= ? AND CAST(BillDate AS DATE) <= ? THEN PaidAmount ELSE 0.0 END), 0.0) AS MonthlyPaid,
                       ISNULL(SUM(TotalAmount), 0.0) AS AllTimeInvoiced,
                       ISNULL(SUM(PaidAmount), 0.0) AS AllTimePaid
                FROM SupplierBills
                WHERE SupplierName IS NOT NULL AND RTRIM(LTRIM(SupplierName)) <> ''
                GROUP BY SupplierName
                ORDER BY (ISNULL(SUM(TotalAmount), 0.0) - ISNULL(SUM(PaidAmount), 0.0)) DESC, MonthlyPurchases DESC, SupplierName ASC
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, startDate)
                ps.setString(2, endDate)
                ps.setString(3, startDate)
                ps.setString(4, endDate)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SupplierMonthlySummary>()
                    while (rs.next()) {
                        val name = normalizeSupplierName(rs.getString("SupplierName") ?: "")
                        if (name.isEmpty()) continue
                        val monthlyPurchases = roundMoney(rs.getDouble("MonthlyPurchases"))
                        val monthlyPaid = roundMoney(rs.getDouble("MonthlyPaid"))
                        val allTimeInvoiced = roundMoney(rs.getDouble("AllTimeInvoiced"))
                        val allTimePaid = roundMoney(rs.getDouble("AllTimePaid"))
                        val cumulativeDebt = roundMoney((allTimeInvoiced - allTimePaid).coerceAtLeast(0.0))

                        list.add(
                            SupplierMonthlySummary(
                                supplierName = name,
                                monthlyPurchases = monthlyPurchases,
                                monthlyPaid = monthlyPaid,
                                monthlyNewDebt = roundMoney(monthlyPurchases - monthlyPaid),
                                cumulativeDebt = cumulativeDebt,
                                allTimeInvoiced = allTimeInvoiced,
                                allTimePaid = allTimePaid
                            )
                        )
                    }
                    list.sortedByDescending { it.cumulativeDebt }
                }
            }
        }
    }

    suspend fun getTodayQuickStats(): Result<TodayQuickStats> {
        return DatabaseClient.safeQuery { conn ->
            val today = LocalDate.now()
            val todayStr = today.format(dateFormatter)

            // 1. Check if today is closed in DailyTreasuries
            val closedSql = "SELECT TotalSales, TotalExpenses, TotalSupplierPayments FROM DailyTreasuries WHERE CAST(Date AS DATE) = ?"
            var isTodayClosed = false
            var todaySales = 0.0
            var closedExpenses = 0.0
            var closedSupplierPayments = 0.0

            conn.prepareStatement(closedSql).use { ps ->
                ps.setString(1, todayStr)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        isTodayClosed = true
                        todaySales = roundMoney(rs.getDouble("TotalSales"))
                        closedExpenses = roundMoney(rs.getDouble("TotalExpenses"))
                        closedSupplierPayments = roundMoney(rs.getDouble("TotalSupplierPayments"))
                    }
                }
            }

            // 2. Previous cash (cumulative net flow prior to today)
            val prevSql = """
                SELECT ISNULL(SUM(TotalSales - TotalExpenses - TotalSupplierPayments), 0.0)
                FROM DailyTreasuries
                WHERE CAST(Date AS DATE) < ?
            """.trimIndent()
            val previousCash = conn.prepareStatement(prevSql).use { ps ->
                ps.setString(1, todayStr)
                ps.executeQuery().use { rs -> if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0 }
            }

            // 3. Today's actual logged expenses
            val expSql = "SELECT ISNULL(SUM(Amount), 0.0) FROM Expenses WHERE CAST(ExpenseDate AS DATE) = ?"
            val actualExpenses = conn.prepareStatement(expSql).use { ps ->
                ps.setString(1, todayStr)
                ps.executeQuery().use { rs -> if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0 }
            }

            // 4. Today's supplier payments: Cash vs Fawry
            val billSql = "SELECT PaidAmount, Notes FROM SupplierBills WHERE CAST(BillDate AS DATE) = ?"
            var cashSupplierPayments = 0.0
            var fawrySupplierPayments = 0.0
            conn.prepareStatement(billSql).use { ps ->
                ps.setString(1, todayStr)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val paid = rs.getDouble("PaidAmount")
                        val notes = rs.getString("Notes") ?: ""
                        if (notes.contains("[فوري]")) {
                            fawrySupplierPayments = roundMoney(fawrySupplierPayments + paid)
                        } else {
                            cashSupplierPayments = roundMoney(cashSupplierPayments + paid)
                        }
                    }
                }
            }

            // 5. Active staff count (attendances where CheckOut is null)
            val activeStaffSql = "SELECT COUNT(*) FROM Attendances WHERE CheckOut IS NULL"
            val activeStaffCount = conn.prepareStatement(activeStaffSql).use { ps ->
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }

            // 6. Total active employees
            val totalEmpSql = "SELECT COUNT(*) FROM Employees WHERE IsActive = 1"
            val totalEmployees = conn.prepareStatement(totalEmpSql).use { ps ->
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }

            // 7. Cumulative supplier debt (All-Time)
            val debtSql = "SELECT ISNULL(SUM(TotalAmount - PaidAmount), 0.0) FROM SupplierBills"
            val cumulativeDebt = conn.prepareStatement(debtSql).use { ps ->
                ps.executeQuery().use { rs -> if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0 }
            }

            // 8. Current Month Sales & Closed Days Count (DailyTreasuries)
            val monthStartStr = today.withDayOfMonth(1).format(dateFormatter)
            val monthEndStr = today.withDayOfMonth(today.lengthOfMonth()).format(dateFormatter)

            val monthSalesSql = """
                SELECT ISNULL(SUM(TotalSales), 0.0) AS MonthSales, COUNT(*) AS ClosedDays
                FROM DailyTreasuries
                WHERE CAST(Date AS DATE) >= ? AND CAST(Date AS DATE) <= ?
            """.trimIndent()
            var monthSales = 0.0
            var monthClosedDaysCount = 0
            conn.prepareStatement(monthSalesSql).use { ps ->
                ps.setString(1, monthStartStr)
                ps.setString(2, monthEndStr)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        monthSales = roundMoney(rs.getDouble("MonthSales"))
                        monthClosedDaysCount = rs.getInt("ClosedDays")
                    }
                }
            }

            // 9. Current Month Purchases (SupplierBills TotalAmount in current month)
            val monthPurchasesSql = """
                SELECT ISNULL(SUM(TotalAmount), 0.0)
                FROM SupplierBills
                WHERE CAST(BillDate AS DATE) >= ? AND CAST(BillDate AS DATE) <= ?
            """.trimIndent()
            val monthPurchases = conn.prepareStatement(monthPurchasesSql).use { ps ->
                ps.setString(1, monthStartStr)
                ps.setString(2, monthEndStr)
                ps.executeQuery().use { rs -> if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0 }
            }

            // Expected drawer cash
            val expectedDrawerCash = roundMoney(
                if (isTodayClosed) {
                    (previousCash + todaySales) - closedExpenses - closedSupplierPayments
                } else {
                    (previousCash - actualExpenses - cashSupplierPayments).coerceAtLeast(0.0)
                }
            )

            val finalExpenses = if (isTodayClosed) closedExpenses else actualExpenses

            TodayQuickStats(
                date = today,
                todaySales = todaySales,
                expectedDrawerCash = expectedDrawerCash,
                todayExpenses = finalExpenses,
                cashSupplierPayments = cashSupplierPayments,
                fawrySupplierPayments = fawrySupplierPayments,
                activeStaffCount = activeStaffCount,
                totalEmployees = totalEmployees,
                cumulativeSupplierDebt = cumulativeDebt,
                isTodayClosed = isTodayClosed,
                monthSales = monthSales,
                monthPurchases = monthPurchases,
                monthClosedDaysCount = monthClosedDaysCount
            )
        }
    }

    // =========================================================================
    // 5. DAILY TREASURY & SHIFT CLOSING CRUD
    // =========================================================================

    suspend fun getAllDailyTreasuries(): Result<List<DailyTreasury>> {
        return DatabaseClient.safeQuery { conn ->
            val sql = "SELECT TreasuryId, Date, TotalSales, TotalExpenses, TotalSupplierPayments FROM DailyTreasuries ORDER BY Date DESC"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs ->
                    val list = mutableListOf<DailyTreasury>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("Date")
                        val sales = roundMoney(rs.getDouble("TotalSales"))
                        val expenses = roundMoney(rs.getDouble("TotalExpenses"))
                        val supplier = roundMoney(rs.getDouble("TotalSupplierPayments"))
                        list.add(
                            DailyTreasury(
                                id = rs.getInt("TreasuryId"),
                                date = ts.toLocalDateSafe(),
                                totalSales = sales,
                                totalExpenses = expenses,
                                totalSupplierPayments = supplier,
                                netCash = roundMoney(sales - expenses - supplier),
                                profit = roundMoney(sales * 0.10)
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getDailyTreasuriesForMonth(year: Int, month: Int): Result<List<DailyTreasury>> {
        return DatabaseClient.safeQuery { conn ->
            val startDate = LocalDate.of(year, month, 1).format(dateFormatter)
            val endDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).format(dateFormatter)
            val sql = """
                SELECT TreasuryId, Date, TotalSales, TotalExpenses, TotalSupplierPayments
                FROM DailyTreasuries
                WHERE CAST(Date AS DATE) >= ? AND CAST(Date AS DATE) <= ?
                ORDER BY Date DESC
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, startDate)
                ps.setString(2, endDate)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<DailyTreasury>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("Date")
                        val sales = roundMoney(rs.getDouble("TotalSales"))
                        val expenses = roundMoney(rs.getDouble("TotalExpenses"))
                        val supplier = roundMoney(rs.getDouble("TotalSupplierPayments"))
                        list.add(
                            DailyTreasury(
                                id = rs.getInt("TreasuryId"),
                                date = ts.toLocalDateSafe(),
                                totalSales = sales,
                                totalExpenses = expenses,
                                totalSupplierPayments = supplier,
                                netCash = roundMoney(sales - expenses - supplier),
                                profit = roundMoney(sales * 0.10)
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getDailyClosingPreview(date: LocalDate, cashInDrawer: Double): Result<DailyClosingPreview> {
        return DatabaseClient.safeQuery { conn ->
            calculateDailyPreviewInternal(conn, date, cashInDrawer)
        }
    }

    private fun calculateDailyPreviewInternal(conn: Connection, date: LocalDate, cashInDrawer: Double): DailyClosingPreview {
        val dateStr = date.format(dateFormatter)

        // Previous cash: sum of net drawer flow of closed days before this date
        val prevSql = """
            SELECT ISNULL(SUM(TotalSales - TotalExpenses - TotalSupplierPayments), 0.0)
            FROM DailyTreasuries
            WHERE CAST(Date AS DATE) < ?
        """.trimIndent()
        val previousCash = conn.prepareStatement(prevSql).use { ps ->
            ps.setString(1, dateStr)
            ps.executeQuery().use { rs ->
                if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0
            }
        }

        // Total actual expenses for that day
        val expSql = "SELECT ISNULL(SUM(Amount), 0.0) FROM Expenses WHERE CAST(ExpenseDate AS DATE) = ?"
        val totalExpenses = conn.prepareStatement(expSql).use { ps ->
            ps.setString(1, dateStr)
            ps.executeQuery().use { rs ->
                if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0
            }
        }

        // Supplier payments: Cash vs Fawry breakdown
        // Rule: Fawry payments contain "[فوري]" and MUST NOT be deducted from drawer cash!
        val billSql = "SELECT PaidAmount, Notes FROM SupplierBills WHERE CAST(BillDate AS DATE) = ?"
        var cashSupplierPayments = 0.0
        var fawrySupplierPayments = 0.0
        conn.prepareStatement(billSql).use { ps ->
            ps.setString(1, dateStr)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val paid = rs.getDouble("PaidAmount")
                    val notes = rs.getString("Notes") ?: ""
                    if (notes.contains("[فوري]")) {
                        fawrySupplierPayments = roundMoney(fawrySupplierPayments + paid)
                    } else {
                        cashSupplierPayments = roundMoney(cashSupplierPayments + paid)
                    }
                }
            }
        }

        // Check if already closed
        val closedSql = "SELECT TreasuryId FROM DailyTreasuries WHERE CAST(Date AS DATE) = ?"
        val isAlreadyClosed = conn.prepareStatement(closedSql).use { ps ->
            ps.setString(1, dateStr)
            ps.executeQuery().use { rs -> rs.next() }
        }

        // Formula: TotalSales = (CashInDrawer - PreviousCash) + TotalExpenses + TotalSupplierCashPayments
        val calculatedTotalSales = roundMoney((cashInDrawer - previousCash) + totalExpenses + cashSupplierPayments)
        val estimatedProfit = roundMoney(calculatedTotalSales * 0.10)

        return DailyClosingPreview(
            date = date,
            previousCash = previousCash,
            cashInDrawer = roundMoney(cashInDrawer),
            totalExpenses = totalExpenses,
            cashSupplierPayments = cashSupplierPayments,
            fawrySupplierPayments = fawrySupplierPayments,
            totalSupplierPaymentsCombined = roundMoney(cashSupplierPayments + fawrySupplierPayments),
            calculatedTotalSales = calculatedTotalSales,
            estimatedProfit = estimatedProfit,
            isAlreadyClosed = isAlreadyClosed
        )
    }

    suspend fun saveDailyClosing(date: LocalDate, cashInDrawer: Double): Result<DailyTreasury> {
        return DatabaseClient.safeTransaction { conn ->
            val preview = calculateDailyPreviewInternal(conn, date, cashInDrawer)
            val dateStr = date.format(dateFormatter)

            val checkSql = "SELECT TreasuryId FROM DailyTreasuries WHERE CAST(Date AS DATE) = ?"
            var existingId = 0
            conn.prepareStatement(checkSql).use { ps ->
                ps.setString(1, dateStr)
                ps.executeQuery().use { rs ->
                    if (rs.next()) existingId = rs.getInt(1)
                }
            }

            if (existingId == 0) {
                val insertSql = "INSERT INTO DailyTreasuries (Date, TotalSales, TotalExpenses, TotalSupplierPayments) VALUES (?, ?, ?, ?)"
                conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                    ps.setTimestamp(1, date.toTimestampSafe())
                    ps.setDouble(2, preview.calculatedTotalSales)
                    ps.setDouble(3, preview.totalExpenses)
                    ps.setDouble(4, preview.cashSupplierPayments)
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        if (rs.next()) existingId = rs.getInt(1)
                    }
                }
            } else {
                val updateSql = "UPDATE DailyTreasuries SET TotalSales = ?, TotalExpenses = ?, TotalSupplierPayments = ? WHERE TreasuryId = ?"
                conn.prepareStatement(updateSql).use { ps ->
                    ps.setDouble(1, preview.calculatedTotalSales)
                    ps.setDouble(2, preview.totalExpenses)
                    ps.setDouble(3, preview.cashSupplierPayments)
                    ps.setInt(4, existingId)
                    ps.executeUpdate()
                }
            }

            DailyTreasury(
                id = existingId,
                date = date,
                totalSales = preview.calculatedTotalSales,
                totalExpenses = preview.totalExpenses,
                totalSupplierPayments = preview.cashSupplierPayments,
                netCash = roundMoney(preview.calculatedTotalSales - preview.totalExpenses - preview.cashSupplierPayments),
                profit = roundMoney(preview.calculatedTotalSales * 0.10)
            )
        }
    }

    /**
     * Auto Treasury Sync:
     * Adding, modifying, or deleting an expense or supplier bill in a closed day triggers
     * automatic recalculation of sales while keeping the physical net drawer cash intact.
     */
    private fun syncTreasuryForDateInternal(conn: Connection, date: LocalDate) {
        try {
            val dateStr = date.format(dateFormatter)
            val selectSql = "SELECT TreasuryId, TotalSales, TotalExpenses, TotalSupplierPayments FROM DailyTreasuries WHERE CAST(Date AS DATE) = ?"
            var treasuryId = 0
            var oldSales = 0.0
            var oldExpenses = 0.0
            var oldSupplier = 0.0

            conn.prepareStatement(selectSql).use { ps ->
                ps.setString(1, dateStr)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        treasuryId = rs.getInt("TreasuryId")
                        oldSales = rs.getDouble("TotalSales")
                        oldExpenses = rs.getDouble("TotalExpenses")
                        oldSupplier = rs.getDouble("TotalSupplierPayments")
                    } else {
                        return // Not closed, no sync needed
                    }
                }
            }

            val prevSql = "SELECT ISNULL(SUM(TotalSales - TotalExpenses - TotalSupplierPayments), 0.0) FROM DailyTreasuries WHERE CAST(Date AS DATE) < ?"
            val previousCash = conn.prepareStatement(prevSql).use { ps ->
                ps.setString(1, dateStr)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1) else 0.0 }
            }

            // Preserved physical drawer cash:
            val preservedDrawer = (oldSales - oldExpenses - oldSupplier) + previousCash

            // Query new actual expenses
            val expSql = "SELECT ISNULL(SUM(Amount), 0.0) FROM Expenses WHERE CAST(ExpenseDate AS DATE) = ?"
            val newExpenses = conn.prepareStatement(expSql).use { ps ->
                ps.setString(1, dateStr)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1) else 0.0 }
            }

            // Query new actual cash supplier payments (strictly excluding Fawry)
            val billSql = "SELECT PaidAmount, Notes FROM SupplierBills WHERE CAST(BillDate AS DATE) = ?"
            var newCashSupplier = 0.0
            conn.prepareStatement(billSql).use { ps ->
                ps.setString(1, dateStr)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val paid = rs.getDouble("PaidAmount")
                        val notes = rs.getString("Notes") ?: ""
                        if (!notes.contains("[فوري]")) {
                            newCashSupplier += paid
                        }
                    }
                }
            }

            val newSales = roundMoney((preservedDrawer - previousCash) + newExpenses + newCashSupplier)

            val updateSql = "UPDATE DailyTreasuries SET TotalSales = ?, TotalExpenses = ?, TotalSupplierPayments = ? WHERE TreasuryId = ?"
            conn.prepareStatement(updateSql).use { ps ->
                ps.setDouble(1, newSales)
                ps.setDouble(2, roundMoney(newExpenses))
                ps.setDouble(3, roundMoney(newCashSupplier))
                ps.setInt(4, treasuryId)
                ps.executeUpdate()
            }
            Log.i(tag, "Auto-synced treasury #$treasuryId for date $dateStr. New sales: $newSales")
        } catch (e: Exception) {
            Log.e(tag, "Failed auto treasury sync", e)
        }
    }

    /**
     * Retroactive Adjustment:
     * Recalculates historical daily treasuries against actual expenses and supplier bills.
     */
    suspend fun runRetroactiveTreasuryAdjustment(): Result<Int> {
        return DatabaseClient.safeTransaction { conn ->
            val treasuries = mutableListOf<DailyTreasury>()
            val selectSql = "SELECT TreasuryId, Date, TotalSales, TotalExpenses, TotalSupplierPayments FROM DailyTreasuries ORDER BY Date ASC"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(selectSql).use { rs ->
                    while (rs.next()) {
                        val ts = rs.getTimestamp("Date")
                        treasuries.add(
                            DailyTreasury(
                                id = rs.getInt("TreasuryId"),
                                date = ts.toLocalDateSafe(),
                                totalSales = rs.getDouble("TotalSales"),
                                totalExpenses = rs.getDouble("TotalExpenses"),
                                totalSupplierPayments = rs.getDouble("TotalSupplierPayments")
                            )
                        )
                    }
                }
            }

            var updatedCount = 0
            var cumulativeDrawerCash = 0.0

            for (tr in treasuries) {
                val dateStr = tr.date.format(dateFormatter)
                val prevCash = cumulativeDrawerCash
                val preservedDrawerCash = (tr.totalSales - tr.totalExpenses - tr.totalSupplierPayments) + prevCash

                val expSql = "SELECT ISNULL(SUM(Amount), 0.0) FROM Expenses WHERE CAST(ExpenseDate AS DATE) = ?"
                val actualExpenses = conn.prepareStatement(expSql).use { ps ->
                    ps.setString(1, dateStr)
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1) else 0.0 }
                }

                val billSql = "SELECT PaidAmount, Notes FROM SupplierBills WHERE CAST(BillDate AS DATE) = ?"
                var actualCashSupplier = 0.0
                conn.prepareStatement(billSql).use { ps ->
                    ps.setString(1, dateStr)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val paid = rs.getDouble("PaidAmount")
                            val notes = rs.getString("Notes") ?: ""
                            if (!notes.contains("[فوري]")) {
                                actualCashSupplier += paid
                            }
                        }
                    }
                }

                val correctedSales = roundMoney((preservedDrawerCash - prevCash) + actualExpenses + actualCashSupplier)

                val updateSql = "UPDATE DailyTreasuries SET TotalSales = ?, TotalExpenses = ?, TotalSupplierPayments = ? WHERE TreasuryId = ?"
                conn.prepareStatement(updateSql).use { ps ->
                    ps.setDouble(1, correctedSales)
                    ps.setDouble(2, roundMoney(actualExpenses))
                    ps.setDouble(3, roundMoney(actualCashSupplier))
                    ps.setInt(4, tr.id)
                    ps.executeUpdate()
                }

                cumulativeDrawerCash = preservedDrawerCash
                updatedCount++
            }
            updatedCount
        }
    }

    // =========================================================================
    // 6. MONTHLY CLOSING & 10 FINANCIAL INDICATORS
    // =========================================================================

    suspend fun getMonthSummaryData(year: Int, month: Int): Result<MonthSummaryData> {
        return DatabaseClient.safeQuery { conn ->
            val startDate = LocalDate.of(year, month, 1)
            val endDate = startDate.plusMonths(1).minusDays(1)
            val startStr = startDate.format(dateFormatter)
            val endStr = endDate.format(dateFormatter)

            // 1 & 2: Total Sales & Closed Days
            val trSql = "SELECT ISNULL(SUM(TotalSales), 0.0), COUNT(*) FROM DailyTreasuries WHERE CAST(Date AS DATE) >= ? AND CAST(Date AS DATE) <= ?"
            var totalSales = 0.0
            var closedDaysCount = 0
            conn.prepareStatement(trSql).use { ps ->
                ps.setString(1, startStr)
                ps.setString(2, endStr)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        totalSales = roundMoney(rs.getDouble(1))
                        closedDaysCount = rs.getInt(2)
                    }
                }
            }
            val grossProfit10Percent = roundMoney(totalSales * 0.10)

            // 3 & 4: Total Purchases & Payments (Cash vs Fawry)
            val billSql = "SELECT TotalAmount, PaidAmount, Notes, SupplierName FROM SupplierBills WHERE CAST(BillDate AS DATE) >= ? AND CAST(BillDate AS DATE) <= ?"
            var totalPurchases = 0.0
            var cashSupplierPayments = 0.0
            var fawrySupplierPayments = 0.0
            var purchaseBillsCount = 0
            val supplierMap = mutableMapOf<String, Pair<Double, Double>>()

            conn.prepareStatement(billSql).use { ps ->
                ps.setString(1, startStr)
                ps.setString(2, endStr)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        purchaseBillsCount++
                        val total = rs.getDouble("TotalAmount")
                        val paid = rs.getDouble("PaidAmount")
                        val notes = rs.getString("Notes") ?: ""
                        val sName = normalizeSupplierName(rs.getString("SupplierName") ?: "").ifEmpty { "غير محدد" }

                        totalPurchases = roundMoney(totalPurchases + total)
                        if (notes.contains("[فوري]")) {
                            fawrySupplierPayments = roundMoney(fawrySupplierPayments + paid)
                        } else {
                            cashSupplierPayments = roundMoney(cashSupplierPayments + paid)
                        }

                        val cur = supplierMap.getOrDefault(sName, 0.0 to 0.0)
                        supplierMap[sName] = roundMoney(cur.first + total) to roundMoney(cur.second + paid)
                    }
                }
            }
            val totalSupplierPayments = roundMoney(cashSupplierPayments + fawrySupplierPayments)
            val monthlySupplierDebt = roundMoney((totalPurchases - totalSupplierPayments).coerceAtLeast(0.0))

            // 5: Cumulative Supplier Debt across all time
            val cumSql = "SELECT ISNULL(SUM(TotalAmount - PaidAmount), 0.0) FROM SupplierBills"
            val cumulativeSupplierDebt = conn.createStatement().use { stmt ->
                stmt.executeQuery(cumSql).use { rs ->
                    if (rs.next()) roundMoney(rs.getDouble(1)) else 0.0
                }
            }

            // 6: Operating Expenses & Breakdown
            val expSql = "SELECT ExpenseType, Amount FROM Expenses WHERE CAST(ExpenseDate AS DATE) >= ? AND CAST(ExpenseDate AS DATE) <= ?"
            var operatingExpenses = 0.0
            var expenseCount = 0
            val expenseMap = mutableMapOf<String, Double>()
            conn.prepareStatement(expSql).use { ps ->
                ps.setString(1, startStr)
                ps.setString(2, endStr)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        expenseCount++
                        val type = rs.getString("ExpenseType")?.trim()?.ifEmpty { "عام" } ?: "عام"
                        val amt = rs.getDouble("Amount")
                        operatingExpenses = roundMoney(operatingExpenses + amt)
                        expenseMap[type] = roundMoney(expenseMap.getOrDefault(type, 0.0) + amt)
                    }
                }
            }

            // 7: Employee Wages (calculated from Attendances within month)
            val attSql = """
                SELECT a.CheckIn, a.CheckOut, e.HourRate 
                FROM Attendances a
                INNER JOIN Employees e ON a.EmployeeId = e.EmployeeId
                WHERE CAST(a.CheckIn AS DATE) >= ? AND CAST(a.CheckIn AS DATE) <= ?
            """.trimIndent()
            var employeeWages = 0.0
            var attendanceShiftsCount = 0
            var totalStaffHours = 0.0
            conn.prepareStatement(attSql).use { ps ->
                ps.setString(1, startStr)
                ps.setString(2, endStr)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val inTs = rs.getTimestamp("CheckIn")
                        val outTs = rs.getTimestamp("CheckOut")
                        val rate = rs.getDouble("HourRate")
                        if (inTs != null && outTs != null) {
                            val inTime = inTs.time
                            var outTime = outTs.time
                            if (outTime <= inTime) {
                                outTime += 24 * 60 * 60 * 1000L
                            }
                            val hours = (outTime - inTime).coerceAtLeast(0) / (1000.0 * 60.0 * 60.0)
                            attendanceShiftsCount++
                            totalStaffHours = roundMoney(totalStaffHours + hours)
                            employeeWages = roundMoney(employeeWages + (hours * rate))
                        }
                    }
                }
            }

            // 8: Net Operating Profit = 10% Gross Profit - Operating Expenses - Employee Wages
            val netOperatingProfit = roundMoney(grossProfit10Percent - operatingExpenses - employeeWages)

            // 9: Net Drawer Cash Flow = Total Sales - Operating Expenses - Cash Supplier Payments
            val netDrawerCashFlow = roundMoney(totalSales - operatingExpenses - cashSupplierPayments)

            val debtsList = supplierMap.map { (name, totals) ->
                SupplierDebtSummary(
                    supplierName = name,
                    totalInvoiced = totals.first,
                    totalPaid = totals.second,
                    remainingDebt = roundMoney((totals.first - totals.second).coerceAtLeast(0.0))
                )
            }.sortedByDescending { it.remainingDebt }

            MonthSummaryData(
                year = year,
                month = month,
                totalSales = totalSales,
                grossProfit10Percent = grossProfit10Percent,
                totalPurchases = totalPurchases,
                totalSupplierPayments = totalSupplierPayments,
                cashSupplierPayments = cashSupplierPayments,
                fawrySupplierPayments = fawrySupplierPayments,
                monthlySupplierDebt = monthlySupplierDebt,
                cumulativeSupplierDebt = cumulativeSupplierDebt,
                operatingExpenses = operatingExpenses,
                employeeWages = employeeWages,
                netOperatingProfit = netOperatingProfit,
                netDrawerCashFlow = netDrawerCashFlow,
                supplierDebts = debtsList,
                expenseBreakdown = expenseMap,
                closedDaysCount = closedDaysCount,
                purchaseBillsCount = purchaseBillsCount,
                supplierCount = supplierMap.size,
                expenseCount = expenseCount,
                attendanceShiftsCount = attendanceShiftsCount,
                totalStaffHours = totalStaffHours
            )
        }
    }
}
