package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.MohsenDestination
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.MonthlyAnalysisScreen
import com.example.ui.screens.SupplierBillsScreen
import com.example.ui.screens.SuppliersDebtSummaryScreen
import com.example.ui.screens.TreasuryScreen
import com.example.ui.theme.MohsenMarketTheme

/**
 * [TASK 1: ROOT NAVIGATION & ENTRY POINT]
 * Root Activity initializing Jetpack Compose Material 3 theme, Arabic RTL layout direction,
 * and Jetpack Navigation Graph with type-safe destinations.
 * Each destination scopes its own dedicated ViewModel so startup only loads Dashboard quick stats.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MohsenMarketTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MohsenMarketRootApp()
                }
            }
        }
    }
}

@Composable
fun MohsenMarketRootApp() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = MohsenDestination.Dashboard
            ) {
                // 1. Dashboard (MainForm equivalent)
                composable<MohsenDestination.Dashboard> {
                    DashboardScreen(
                        onNavigate = { destination ->
                            navController.navigate(destination)
                        }
                    )
                }

                // 2. Staff & Attendance (إدارة الموظفين والحضور)
                composable<MohsenDestination.StaffAttendance> {
                    val employeeViewModel: com.example.ui.viewmodel.EmployeeViewModel = viewModel()
                    SubScreenScaffold(
                        title = "إدارة الموظفين والحضور",
                        subtitle = "حساب ساعات العمل وسعر الساعة وتكلفة الورديات",
                        onNavigateBack = { navController.popBackStack() },
                        onRefresh = {
                            employeeViewModel.loadEmployees()
                            employeeViewModel.loadMonthlyTimesheet()
                        }
                    ) {
                        AttendanceScreen(
                            viewModel = employeeViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }

                // 3. Supplier Bills (فواتير الموردين والتجار)
                composable<MohsenDestination.SupplierBills> {
                    val supplierBillsViewModel: com.example.ui.viewmodel.SupplierBillsViewModel = viewModel()
                    SubScreenScaffold(
                        title = "فواتير الموردين والتجار",
                        subtitle = "تسجيل فواتير البضائع وفصل مدفوعات فوري",
                        onNavigateBack = { navController.popBackStack() },
                        onRefresh = {
                            supplierBillsViewModel.loadBills()
                            supplierBillsViewModel.loadCumulativeDebt()
                            supplierBillsViewModel.loadDistinctSuppliers()
                        }
                    ) {
                        SupplierBillsScreen(
                            viewModel = supplierBillsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }

                // 4. Suppliers Debt Summary (تقرير إجمالي حسابات الموردين)
                composable<MohsenDestination.SupplierDebtSummary> {
                    val summaryViewModel: com.example.ui.viewmodel.SuppliersSummaryViewModel = viewModel()
                    SuppliersDebtSummaryScreen(
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = summaryViewModel
                    )
                }

                // 5. Daily Expenses (المصروفات والخوارج)
                composable<MohsenDestination.DailyExpenses> {
                    val expensesViewModel: com.example.ui.viewmodel.ExpensesViewModel = viewModel()
                    SubScreenScaffold(
                        title = "المصروفات والخوارج",
                        subtitle = "يومية المحل والإيجار والمصروفات النثرية",
                        onNavigateBack = { navController.popBackStack() },
                        onRefresh = { expensesViewModel.refreshAll() }
                    ) {
                        ExpensesScreen(viewModel = expensesViewModel)
                    }
                }

                // 6. Daily Treasury (الخزنة وتقفيل اليومية)
                composable<MohsenDestination.DailyTreasury> {
                    val treasuryViewModel: com.example.ui.viewmodel.TreasuryViewModel = viewModel()
                    SubScreenScaffold(
                        title = "الخزنة وتقفيل اليومية",
                        subtitle = "تقفيل الوردية واحتساب المبيعات من نقدية الدرج",
                        onNavigateBack = { navController.popBackStack() },
                        onRefresh = { treasuryViewModel.refreshAll() }
                    ) {
                        TreasuryScreen(viewModel = treasuryViewModel)
                    }
                }

                // 7. Monthly Financial Closing (تقفيل الشهر والتحليل المالي 📊)
                composable<MohsenDestination.MonthlyFinancialClosing> {
                    val monthClosingViewModel: com.example.ui.viewmodel.MonthClosingViewModel = viewModel()
                    SubScreenScaffold(
                        title = "تقفيل الشهر والتحليل المالي 📊",
                        subtitle = "المؤشرات المالية الـ 10 وصافي الربح التشغيلي",
                        onNavigateBack = { navController.popBackStack() },
                        onRefresh = { monthClosingViewModel.loadMonthlySummary() }
                    ) {
                        MonthlyAnalysisScreen(viewModel = monthClosingViewModel)
                    }
                }
            }
        }
    }
}

/**
 * Reusable wrapper for secondary screens with TopAppBar and back button in dark theme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreenScaffold(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("subscreen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع للوحة التحكم",
                            tint = Color(0xFFF8FAFC)
                        )
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث البيانات", tint = Color(0xFF60A5FA))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141418)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
