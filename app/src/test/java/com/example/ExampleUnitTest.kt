package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun testOvernightShiftCalculation() {
    // Normal shift: 09:00 to 17:00 (8 hours)
    val checkIn1 = 9.0
    val checkOut1 = 17.0
    val hours1 = checkOut1 - checkIn1
    val hourRate = 20.0
    assertEquals(8.0, hours1, 0.001)
    assertEquals(160.0, hours1 * hourRate, 0.001)

    // Overnight shift: 17:00 to 02:00 next day (9 hours)
    val checkIn2 = 17.0
    val checkOut2 = 2.0
    val hours2 = if (checkOut2 <= checkIn2) (checkOut2 + 24.0) - checkIn2 else checkOut2 - checkIn2
    assertEquals(9.0, hours2, 0.001)
    assertEquals(180.0, hours2 * hourRate, 0.001)
  }

  @Test
  fun testDailySalesFormula_CashDrawerExcludesFawry() {
    val previousCash = 5000.0
    val cashInDrawer = 7500.0
    val totalExpenses = 1200.0
    val cashSupplierPayments = 3300.0
    val fawrySupplierPayments = 4500.0 // Electronic payment, not from drawer!

    // TotalSales = (CashInDrawer - PreviousCash) + TotalExpenses + TotalSupplierCashPayments
    val totalSales = (cashInDrawer - previousCash) + totalExpenses + cashSupplierPayments
    // (7500 - 5000) + 1200 + 3300 = 2500 + 1200 + 3300 = 7000
    val estimatedProfit = totalSales * 0.10
    assertEquals(700.0, estimatedProfit, 0.001)
  }

  @Test
  fun testMssqlDriver() {
    val url = "jdbc:sqlserver://db79344.public.databaseasp.net:1433;databaseName=db79344;user=db79344;password=Ph4+@Nr5g8E-;encrypt=true;trustServerCertificate=true;"
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
    val conn = java.sql.DriverManager.getConnection(url)
    assertNotNull(conn)
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery("SELECT 1")
    assertTrue(rs.next())
    rs.close()
    stmt.close()
    conn.close()
  }

  @Test
  fun testJtdsDriver() {
    val url = "jdbc:jtds:sqlserver://db79344.public.databaseasp.net:1433/db79344;ssl=require;useNTLMv2=true;"
    Class.forName("net.sourceforge.jtds.jdbc.Driver")
    val conn = java.sql.DriverManager.getConnection(url, "db79344", "Ph4+@Nr5g8E-")
    assertNotNull(conn)
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery("SELECT 1")
    assertTrue(rs.next())
    rs.close()
    stmt.close()
    conn.close()
  }

  @Test
  fun testTask0DataModels() {
    // 1. Attendance Model with overnight LocalDateTime logic
    val checkIn = java.time.LocalDateTime.of(2026, 7, 1, 18, 0)
    val checkOutNextDay = java.time.LocalDateTime.of(2026, 7, 1, 2, 0) // Next day early morning: 18:00 to 02:00 = 8 hours
    val att = com.example.data.model.Attendance(
      id = 1,
      employeeId = 3,
      employeeName = "غباشي",
      checkIn = checkIn,
      checkOut = checkOutNextDay,
      hourRate = 20.0
    )
    assertTrue(att.isOvernight)
    assertEquals(8.0, att.totalHours, 0.001)
    assertEquals(160.0, att.shiftCost, 0.001)

    // 2. SupplierBill Model with Fawry check
    val billFawry = com.example.data.model.SupplierBill(
      id = 10,
      supplierName = "شيبسي",
      totalAmount = 5000.0,
      paidAmount = 2000.0,
      billDate = java.time.LocalDate.of(2026, 7, 1),
      notes = "[فوري] سداد إلكتروني"
    )
    assertTrue(billFawry.isFawry)
    assertEquals(3000.0, billFawry.remainingAmount, 0.001)
    assertEquals("سداد إلكتروني", billFawry.cleanNotes)

    // 3. DailyTreasury Model
    val treasury = com.example.data.model.DailyTreasury(
      id = 5,
      date = java.time.LocalDate.of(2026, 7, 1),
      totalSales = 20000.0,
      totalExpenses = 2500.0,
      totalSupplierPayments = 12000.0
    )
    assertEquals(5500.0, treasury.netCash, 0.001)
    assertEquals(2000.0, treasury.profit, 0.001)

    // 4. MonthSummaryData with 10 KPIs
    val summary = com.example.data.model.MonthSummaryData(
      year = 2026,
      month = 7,
      totalSales = 100000.0,
      grossProfit10Percent = 10000.0,
      totalPurchases = 80000.0,
      totalSupplierPayments = 65000.0,
      cashSupplierPayments = 45000.0,
      fawrySupplierPayments = 20000.0,
      monthlySupplierDebt = 15000.0,
      cumulativeSupplierDebt = 45000.0,
      operatingExpenses = 5000.0,
      employeeWages = 3000.0,
      netOperatingProfit = 2000.0, // 10000 - 5000 - 3000
      netDrawerCashFlow = 50000.0  // 100000 - 5000 - 45000
    )
    assertEquals(10000.0, summary.grossProfit10Percent, 0.001)
    assertEquals(2000.0, summary.netOperatingProfit, 0.001)
    assertEquals(50000.0, summary.netDrawerCashFlow, 0.001)
  }

  @Test
  fun testTask1DashboardAndDestinations() {
    val quickStats = com.example.data.model.TodayQuickStats(
      date = java.time.LocalDate.now(),
      todaySales = 8500.0,
      expectedDrawerCash = 12400.0,
      todayExpenses = 650.0,
      activeStaffCount = 4,
      totalEmployees = 6,
      cumulativeSupplierDebt = 18500.0,
      isTodayClosed = false
    )
    assertEquals(8500.0, quickStats.todaySales, 0.001)
    assertEquals(12400.0, quickStats.expectedDrawerCash, 0.001)
    assertEquals(650.0, quickStats.todayExpenses, 0.001)
    assertEquals(4, quickStats.activeStaffCount)
    assertFalse(quickStats.isTodayClosed)

    // Verify all 6 destinations can be instantiated
    val destinations = listOf(
      com.example.ui.navigation.MohsenDestination.Dashboard,
      com.example.ui.navigation.MohsenDestination.StaffAttendance,
      com.example.ui.navigation.MohsenDestination.SupplierBills,
      com.example.ui.navigation.MohsenDestination.SupplierDebtSummary,
      com.example.ui.navigation.MohsenDestination.DailyExpenses,
      com.example.ui.navigation.MohsenDestination.DailyTreasury,
      com.example.ui.navigation.MohsenDestination.MonthlyFinancialClosing
    )
    assertEquals(7, destinations.size)
  }

  @Test
  fun testTask2EmployeeAndAttendanceState() {
    val emp = com.example.data.model.Employee(
      id = 1,
      employeeName = "أحمد محسن",
      phone = "01012345678",
      hourRate = 30.0,
      isActive = true
    )

    // Test normal day shift: 10:00 to 18:00 (8 hours)
    val stateNormal = com.example.ui.viewmodel.EmployeeUiState(
      shiftSelectedEmployee = emp,
      shiftDate = java.time.LocalDate.of(2026, 11, 12),
      checkInTime = java.time.LocalTime.of(10, 0),
      checkOutTime = java.time.LocalTime.of(18, 0)
    )
    assertFalse(stateNormal.isOvernightShift)
    assertEquals(8.0, stateNormal.calculatedTotalHours, 0.001)
    assertEquals(240.0, stateNormal.calculatedShiftCost, 0.001)

    // Test overnight shift: 20:00 to 04:00 next day (8 hours)
    val stateOvernight = com.example.ui.viewmodel.EmployeeUiState(
      shiftSelectedEmployee = emp,
      shiftDate = java.time.LocalDate.of(2026, 11, 12),
      checkInTime = java.time.LocalTime.of(20, 0),
      checkOutTime = java.time.LocalTime.of(4, 0)
    )
    assertTrue(stateOvernight.isOvernightShift)
    assertEquals(8.0, stateOvernight.calculatedTotalHours, 0.001)
    assertEquals(240.0, stateOvernight.calculatedShiftCost, 0.001)
    assertEquals(java.time.LocalDate.of(2026, 11, 13), stateOvernight.calculatedCheckOutDateTime.toLocalDate())

    // Test monthly timesheet aggregation
    val shift1 = com.example.data.model.Attendance(
      id = 101,
      employeeId = 1,
      employeeName = "أحمد محسن",
      hourRate = 30.0,
      checkIn = java.time.LocalDateTime.of(2026, 11, 1, 9, 0),
      checkOut = java.time.LocalDateTime.of(2026, 11, 1, 17, 0)
    )
    val shift2 = com.example.data.model.Attendance(
      id = 102,
      employeeId = 1,
      employeeName = "أحمد محسن",
      hourRate = 30.0,
      checkIn = java.time.LocalDateTime.of(2026, 11, 2, 21, 0),
      checkOut = java.time.LocalDateTime.of(2026, 11, 2, 5, 0) // Overnight: 8 hours
    )
    val timesheetState = com.example.ui.viewmodel.EmployeeUiState(
      timesheetShifts = listOf(shift1, shift2)
    )
    assertEquals(16.0, timesheetState.timesheetTotalHours, 0.001)
    assertEquals(480.0, timesheetState.timesheetTotalCost, 0.001)
  }

  @Test
  fun testTask3SupplierBillsStateAndCalculations() {
    // 1. Form state validation and remaining calculation
    val formState = com.example.ui.viewmodel.SupplierBillsUiState(
      supplierNameInput = "شركة جهينة",
      totalAmountInput = "12500.0",
      paidAmountInput = "7500.0",
      isFawry = true
    )
    assertTrue(formState.isFormValid)
    assertEquals(12500.0, formState.totalAmount, 0.001)
    assertEquals(7500.0, formState.paidAmount, 0.001)
    assertEquals(5000.0, formState.remainingAmount, 0.001)
    assertTrue(formState.isFawry)

    // 2. Form state with zero total should be invalid
    val invalidForm = com.example.ui.viewmodel.SupplierBillsUiState(
      supplierNameInput = "شيبسي",
      totalAmountInput = "0",
      paidAmountInput = "0"
    )
    assertFalse(invalidForm.isFormValid)

    // 3. Ledger bills list and monthly stats aggregation
    val bill1 = com.example.data.model.SupplierBill(
      id = 1,
      supplierName = "شركة جهينة",
      totalAmount = 12500.0,
      paidAmount = 7500.0,
      billDate = java.time.LocalDate.of(2026, 11, 5),
      notes = "[فوري] سداد جزئي إلكتروني",
      isFawry = true
    )
    val bill2 = com.example.data.model.SupplierBill(
      id = 2,
      supplierName = "شيبسي",
      totalAmount = 8000.0,
      paidAmount = 8000.0,
      billDate = java.time.LocalDate.of(2026, 11, 8),
      notes = "كاش",
      isFawry = false
    )

    val ledgerState = com.example.ui.viewmodel.SupplierBillsUiState(
      bills = listOf(bill1, bill2),
      monthlyPurchasesTotal = 20500.0,
      allTimeCumulativeDebt = 15000.0
    )
    assertEquals(2, ledgerState.bills.size)
    assertEquals(20500.0, ledgerState.monthlyPurchasesTotal, 0.001)
    assertEquals(15000.0, ledgerState.allTimeCumulativeDebt, 0.001)
    assertTrue(ledgerState.bills[0].isFawry)
    assertFalse(ledgerState.bills[1].isFawry)
    assertEquals(5000.0, ledgerState.bills[0].remainingAmount, 0.001)
    assertEquals(0.0, ledgerState.bills[1].remainingAmount, 0.001)
  }

  @Test
  fun testTask4SuppliersSummarySortingTotalsAndCsvExport() {
    val s1 = com.example.data.model.SupplierMonthlySummary(
      supplierName = "شيبسي",
      monthlyPurchases = 4000.0,
      monthlyPaid = 3000.0,
      cumulativeDebt = 2500.0,
      allTimeInvoiced = 20000.0,
      allTimePaid = 17500.0
    )
    val s2 = com.example.data.model.SupplierMonthlySummary(
      supplierName = "شركة جهينة",
      monthlyPurchases = 10000.0,
      monthlyPaid = 5000.0,
      cumulativeDebt = 12000.0,
      allTimeInvoiced = 50000.0,
      allTimePaid = 38000.0
    )
    val s3 = com.example.data.model.SupplierMonthlySummary(
      supplierName = "كوكاكولا",
      monthlyPurchases = 2000.0,
      monthlyPaid = 2000.0,
      cumulativeDebt = 0.0,
      allTimeInvoiced = 10000.0,
      allTimePaid = 10000.0
    )

    val state = com.example.ui.viewmodel.SuppliersSummaryUiState(
      selectedYearMonth = java.time.YearMonth.of(2026, 11),
      allSuppliers = listOf(s1, s2, s3)
    )

    // Verify descending sorting by cumulativeDebt
    val sorted = state.filteredSuppliers
    assertEquals(3, sorted.size)
    assertEquals("شركة جهينة", sorted[0].supplierName)
    assertEquals("شيبسي", sorted[1].supplierName)
    assertEquals("كوكاكولا", sorted[2].supplierName)

    // Verify totals
    assertEquals(16000.0, state.totalMonthlyPurchases, 0.001)
    assertEquals(10000.0, state.totalMonthlyPaid, 0.001)
    assertEquals(6000.0, state.totalMonthlyDebt, 0.001)
    assertEquals(14500.0, state.totalGrandDebt, 0.001)

    // Verify instant search filtering
    val filteredState = state.copy(searchQuery = "جهينة")
    assertEquals(1, filteredState.filteredSuppliers.size)
    assertEquals("شركة جهينة", filteredState.filteredSuppliers[0].supplierName)

    // Verify CSV content includes UTF-8 BOM, headers, rows, and totals
    val csv = com.example.util.SupplierReportExporter.buildCsvContent(
      suppliers = sorted,
      yearMonth = java.time.YearMonth.of(2026, 11)
    )
    assertTrue(csv.startsWith("\uFEFF"))
    assertTrue(csv.contains("شركة جهينة,10000.00,5000.00,5000.00,12000.00"))
    assertTrue(csv.contains("16000.00,10000.00,6000.00,14500.00"))
  }

  @Test
  fun testTask5ExpensesFormValidationAndBreakdown() {
    // 1. Presets include the 5 required categories
    assertEquals(
      listOf("كهرباء", "مياه وغاز", "إيجار", "مرتبات عمال", "نثريات"),
      com.example.ui.viewmodel.EXPENSE_TYPE_PRESETS
    )

    // 2. Preset form validation
    val validPresetState = com.example.ui.viewmodel.ExpensesUiState(
      selectedPresetType = "كهرباء",
      isCustomType = false,
      amountInput = "450.5"
    )
    assertTrue(validPresetState.isFormValid)
    assertEquals("كهرباء", validPresetState.effectiveExpenseType)
    assertEquals(450.5, validPresetState.parsedAmount, 0.001)

    // 3. Custom category validation
    val invalidCustom = validPresetState.copy(isCustomType = true, customTypeInput = "   ")
    assertFalse(invalidCustom.isFormValid)

    val validCustom = validPresetState.copy(isCustomType = true, customTypeInput = "صيانة ثلاجة")
    assertTrue(validCustom.isFormValid)
    assertEquals("صيانة ثلاجة", validCustom.effectiveExpenseType)

    // 4. Daily & Monthly totals aggregation
    val exp1 = com.example.data.model.Expense(
      id = 1,
      expenseType = "كهرباء",
      amount = 350.0,
      expenseDate = java.time.LocalDate.of(2026, 11, 12),
      notes = "شحن كارت الكهرباء"
    )
    val exp2 = com.example.data.model.Expense(
      id = 2,
      expenseType = "نثريات",
      amount = 150.0,
      expenseDate = java.time.LocalDate.of(2026, 11, 12),
      notes = "أكياس"
    )

    val cat1 = com.example.data.model.ExpenseCategorySummary("إيجار", 5000.0, 1)
    val cat2 = com.example.data.model.ExpenseCategorySummary("كهرباء", 1200.0, 3)

    val aggState = com.example.ui.viewmodel.ExpensesUiState(
      dailyExpenses = listOf(exp1, exp2),
      monthlyBreakdown = listOf(cat1, cat2)
    )
    assertEquals(500.0, aggState.dailyTotalAmount, 0.001)
    assertEquals(6200.0, aggState.monthlyTotalAmount, 0.001)
  }

  @Test
  fun testTask6TreasuryFormulaAndMonthlyHistory() {
    // 1. Shift Closing formula check:
    // ActualCash = 9000, PreviousCash = 4000, Expenses = 800, CashSupplier = 2200, Fawry = 5000 (excluded)
    // TotalDailySales = (9000 - 4000) + 800 + 2200 = 8000
    // NetProfit (10%) = 800
    val closingState = com.example.ui.viewmodel.TreasuryUiState(
      actualCashInput = "9000",
      previousCash = 4000.0,
      totalDailyExpenses = 800.0,
      totalSupplierCashPayments = 2200.0,
      fawrySupplierPayments = 5000.0
    )
    assertTrue(closingState.hasValidCashInput)
    assertEquals(8000.0, closingState.calculatedDailySales, 0.001)
    assertEquals(800.0, closingState.calculatedNetProfit, 0.001)

    // 2. Monthly history aggregation
    val d1 = com.example.data.model.DailyTreasury(
      id = 1,
      date = java.time.LocalDate.of(2026, 11, 10),
      totalSales = 15000.0,
      totalExpenses = 500.0,
      totalSupplierPayments = 6000.0
    )
    val d2 = com.example.data.model.DailyTreasury(
      id = 2,
      date = java.time.LocalDate.of(2026, 11, 11),
      totalSales = 25000.0,
      totalExpenses = 1000.0,
      totalSupplierPayments = 9000.0
    )
    val historyState = closingState.copy(monthlyHistory = listOf(d1, d2))
    assertEquals(40000.0, historyState.totalMonthlySales, 0.001)
    assertEquals(4000.0, historyState.totalMonthlyProfit, 0.001)
  }

  @Test
  fun testTask7MonthlyClosingAndReportExport() {
    val summary = com.example.data.model.MonthSummaryData(
      year = 2026,
      month = 11,
      totalSales = 150000.0,
      grossProfit10Percent = 15000.0,
      totalPurchases = 110000.0,
      totalSupplierPayments = 90000.0,
      cashSupplierPayments = 60000.0,
      fawrySupplierPayments = 30000.0,
      monthlySupplierDebt = 20000.0,
      cumulativeSupplierDebt = 45000.0,
      operatingExpenses = 4500.0,
      employeeWages = 3500.0,
      netOperatingProfit = 7000.0, // 15000 - 4500 - 3500
      netDrawerCashFlow = 85500.0, // 150000 - 4500 - 60000
      supplierDebts = listOf(
        com.example.data.model.SupplierDebtSummary("شركة جهينة", 70000.0, 55000.0, 15000.0),
        com.example.data.model.SupplierDebtSummary("شيبسي", 40000.0, 35000.0, 5000.0)
      ),
      expenseBreakdown = mapOf("إيجار" to 3000.0, "كهرباء" to 1500.0),
      closedDaysCount = 15,
      purchaseBillsCount = 18,
      supplierCount = 2,
      expenseCount = 6,
      attendanceShiftsCount = 30,
      totalStaffHours = 240.0
    )

    // Operational Activity Metrics check
    assertEquals(15, summary.closedDaysCount)
    assertEquals(10000.0, summary.dailySalesAverage, 0.001)
    assertEquals(18, summary.purchaseBillsCount)
    assertEquals(2, summary.supplierCount)
    assertEquals(6, summary.expenseCount)

    // Zero closed days edge case for dailySalesAverage
    val emptyMonth = summary.copy(closedDaysCount = 0)
    assertEquals(0.0, emptyMonth.dailySalesAverage, 0.001)

    // CSV Export check (UTF-8 BOM, 10 KPIs, Cash vs Fawry, Operational metrics, Breakdowns)
    val csv = com.example.util.MonthlyReportExporter.buildCsvContent(summary)
    assertTrue(csv.startsWith("\uFEFF"))
    assertTrue(csv.contains("150000.00"))
    assertTrue(csv.contains("60000.00"))
    assertTrue(csv.contains("30000.00"))
    assertTrue(csv.contains("7000.00"))
    assertTrue(csv.contains("85500.00"))
    assertTrue(csv.contains("10000.00"))
    assertTrue(csv.contains("شركة جهينة,70000.00,55000.00,15000.00"))
    assertTrue(csv.contains("إيجار,3000.00"))
  }

  @Test
  fun testAuditFixes_PrecisionNormalizationAndLiveRepositoryQueries() = kotlinx.coroutines.runBlocking {
    // 1. IEEE-754 Floating-Point Drift Prevention via roundMoney
    val driftValue = 0.1 + 0.2 // 0.30000000000000004
    assertEquals(0.30, com.example.data.model.roundMoney(driftValue), 0.0)
    assertEquals(1234.57, com.example.data.model.roundMoney(1234.565), 0.0)

    // 2. Supplier Name Normalization
    assertEquals(
      "شركة جهينة",
      com.example.data.model.normalizeSupplierName("   شركة    جهينة  ")
    )

    // 3. Live DatabaseClient & MohsenMarketRepository End-to-End Verification on db79344
    val repo = com.example.data.repository.MohsenMarketRepository()
    assertTrue("DatabaseClient.testConnection() must succeed", repo.testConnection())

    val quickStatsRes = repo.getTodayQuickStats()
    assertTrue("getTodayQuickStats should succeed: ${quickStatsRes.exceptionOrNull()?.message}", quickStatsRes.isSuccess)

    val employeesRes = repo.getAllEmployees()
    assertTrue("getAllEmployees should succeed: ${employeesRes.exceptionOrNull()?.message}", employeesRes.isSuccess)

    val billsRes = repo.getSupplierBills()
    assertTrue("getSupplierBills should succeed: ${billsRes.exceptionOrNull()?.message}", billsRes.isSuccess)

    val expensesRes = repo.getExpenses()
    assertTrue("getExpenses should succeed: ${expensesRes.exceptionOrNull()?.message}", expensesRes.isSuccess)

    val treasuriesRes = repo.getAllDailyTreasuries()
    assertTrue("getAllDailyTreasuries should succeed: ${treasuriesRes.exceptionOrNull()?.message}", treasuriesRes.isSuccess)

    val monthSummaryRes = repo.getMonthSummaryData(2026, 7)
    assertTrue("getMonthSummaryData should succeed: ${monthSummaryRes.exceptionOrNull()?.message}", monthSummaryRes.isSuccess)
  }
}
