package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.ConnectionStatus
import com.example.data.model.TodayQuickStats
import com.example.ui.components.ConnectionBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.navigation.MohsenDestination
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.DashboardViewModel

import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDate

// Dark Theme Color accents and backgrounds for the 6 cards (pure dark theme with vibrant accents)
private val SkyBlueAccent = Color(0xFF38BDF8)
private val SkyBlueBg = Color(0xFF0E1F30)
private val SkyBlueBorder = Color(0xFF0284C7).copy(alpha = 0.6f)

private val LightGreenAccent = Color(0xFF4ADE80)
private val LightGreenBg = Color(0xFF0D2517)
private val LightGreenBorder = Color(0xFF16A34A).copy(alpha = 0.6f)

private val PlumAccent = Color(0xFFC084FC)
private val PlumBg = Color(0xFF201335)
private val PlumBorder = Color(0xFF9333EA).copy(alpha = 0.6f)

private val LightCoralAccent = Color(0xFFFB7185)
private val LightCoralBg = Color(0xFF2B121A)
private val LightCoralBorder = Color(0xFFE11D48).copy(alpha = 0.6f)

private val GoldAccent = Color(0xFFFBBF24)
private val GoldBg = Color(0xFF291E0D)
private val GoldBorder = Color(0xFFD97706).copy(alpha = 0.6f)

private val DarkForestGreenAccent = Color(0xFF34D399)
private val DarkForestGreenBg = Color(0xFF0D271F)
private val DarkForestGreenBorder = Color(0xFF059669).copy(alpha = 0.6f)

private fun formatArabicMonthYear(date: LocalDate): String {
    val months = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val monthName = months.getOrElse(date.monthValue - 1) { "" }
    return "$monthName ${date.year}"
}

/**
 * [TASK 1: DASHBOARD SCREEN]
 * MainForm equivalent for Mohsen Market with live monthly stats card, 6 color-coded navigation cards,
 * and system designer attribution for Eng.Mustafa Saad.
 */
@Composable
fun DashboardScreen(
    onNavigate: (MohsenDestination) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeveloperDialog by remember { mutableStateOf(false) }

    if (showDeveloperDialog) {
        DeveloperInfoDialog(onDismiss = { showDeveloperDialog = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Store Title & Branding
        item {
            DashboardHeader(
                connectionStatus = state.connectionStatus,
                isLoading = state.isLoading || state.isRetrying,
                onRefresh = { viewModel.retry() },
                onDeveloperClick = { showDeveloperDialog = true }
            )
        }

        // 2. Error Banner with Retry Button
        if (state.errorMessage != null) {
            item {
                DashboardErrorBanner(
                    errorMessage = state.errorMessage ?: "",
                    onRetry = { viewModel.retry() }
                )
            }
        }

        // 3. Top Live Summary Card: Monthly Live Stats (Sales, Purchases, Total Debts)
        item {
            TodayLiveSummaryCard(
                stats = state.stats,
                isLoading = state.isLoading,
                onRefresh = { viewModel.retry() }
            )
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .background(Color(0xFF60A5FA), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الأقسام والوظائف الرئيسية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                )
            }
        }

        // 4. 6 Distinct Navigation Cards
        // Card 1: Staff & Attendance (Sky Blue)
        item {
            DashboardNavigationCard(
                title = "إدارة الموظفين والحضور",
                subtitle = "تسجيل الحضور والانصراف، حساب تكلفة الورديات، والورديات الليلية",
                badgeText = "${state.stats.activeStaffCount} موظف حاضر الآن",
                icon = Icons.Default.Badge,
                accentColor = SkyBlueAccent,
                backgroundColor = SkyBlueBg,
                borderColor = SkyBlueBorder,
                testTag = "dashboard_card_staff",
                onClick = { onNavigate(MohsenDestination.StaffAttendance) }
            )
        }

        // Card 2: Supplier Bills (Light Green)
        item {
            DashboardNavigationCard(
                title = "فواتير الموردين والتجار",
                subtitle = "تسجيل فواتير البضائع، فصل مدفوعات فوري عن الدرج، ومتابعة السداد",
                badgeText = "مشتريات الشهر: ${formatCurrency(state.stats.monthPurchases)}",
                icon = Icons.Default.ReceiptLong,
                accentColor = LightGreenAccent,
                backgroundColor = LightGreenBg,
                borderColor = LightGreenBorder,
                testTag = "dashboard_card_bills",
                onClick = { onNavigate(MohsenDestination.SupplierBills) }
            )
        }

        // Card 3: Suppliers Debt Summary (Plum)
        item {
            DashboardNavigationCard(
                title = "تقرير إجمالي حسابات الموردين",
                subtitle = "كشف مديونيات الشركات والتجار، الفواتير المتبقية، والديون التراكمية",
                badgeText = "إجمالي الديون: ${formatCurrency(state.stats.cumulativeSupplierDebt)}",
                icon = Icons.Default.AccountBalance,
                accentColor = PlumAccent,
                backgroundColor = PlumBg,
                borderColor = PlumBorder,
                testTag = "dashboard_card_debts",
                onClick = { onNavigate(MohsenDestination.SupplierDebtSummary) }
            )
        }

        // Card 4: Daily Expenses (Light Coral)
        item {
            DashboardNavigationCard(
                title = "المصروفات والخوارج",
                subtitle = "يومية المحل، إيجار، كهرباء، صيانة، سلف، وتحديث الخزينة تلقائياً",
                badgeText = "مصروفات اليوم: ${formatCurrency(state.stats.todayExpenses)}",
                icon = Icons.Default.MoneyOff,
                accentColor = LightCoralAccent,
                backgroundColor = LightCoralBg,
                borderColor = LightCoralBorder,
                testTag = "dashboard_card_expenses",
                onClick = { onNavigate(MohsenDestination.DailyExpenses) }
            )
        }

        // Card 5: Daily Treasury Closing (Gold)
        item {
            DashboardNavigationCard(
                title = "الخزنة وتقفيل اليومية",
                subtitle = "تقفيل الوردية اليومية، احتساب المبيعات من الدرج، والضبط بأثر رجعي",
                badgeText = if (state.stats.isTodayClosed) "اليوم مقفل بالخزينة ✓" else "اليوم قيد التشغيل ⏳",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = GoldAccent,
                backgroundColor = GoldBg,
                borderColor = GoldBorder,
                testTag = "dashboard_card_treasury",
                onClick = { onNavigate(MohsenDestination.DailyTreasury) }
            )
        }

        // Card 6: Monthly Financial Closing (Dark Forest Green)
        item {
            DashboardNavigationCard(
                title = "تقفيل الشهر والتحليل المالي 📊",
                subtitle = "المؤشرات الـ 10 الدقيقة، مجمل الربح 10%، وصافي التدفق النقدي للدرج",
                badgeText = "10 مؤشرات مالية معتمدة",
                icon = Icons.Default.Assessment,
                accentColor = DarkForestGreenAccent,
                backgroundColor = DarkForestGreenBg,
                borderColor = DarkForestGreenBorder,
                testTag = "dashboard_card_monthly",
                onClick = { onNavigate(MohsenDestination.MonthlyFinancialClosing) }
            )
        }

        // 5. System Designer & Developer Signature Card (Eng.Mustafa Saad)
        item {
            DeveloperCreditCard(
                onClick = { showDeveloperDialog = true }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Top Header with Market Branding, Designer Badge & Live Status
 */
@Composable
private fun DashboardHeader(
    connectionStatus: ConnectionStatus,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDeveloperClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141418)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF27272E), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E1E24),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .border(1.dp, Color(0xFF3F3F46), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_market_logo),
                        contentDescription = "قاصد كريم - Mohsen Ayman",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "محسن ماركت - لوحة التحكم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF60A5FA)
                    )
                    Text(
                        text = "قاصد كريم • Mohsen Ayman",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFB923C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF1E1E28),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onDeveloperClick)
                            .testTag("header_developer_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Designed by Eng.Mustafa Saad",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                ConnectionBadge(
                    status = connectionStatus,
                    onRetry = onRefresh
                )
                Spacer(modifier = Modifier.height(6.dp))
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("dashboard_refresh_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Live Summary Card displaying Monthly Live Indicators fetched directly from SQL Server:
 * 1. مبيعات الشهر (Month Sales)
 * 2. إجمالي المشتريات (Total Monthly Purchases)
 * 3. إجمالي الديون (Total Cumulative Supplier Debts)
 */
@Composable
private fun TodayLiveSummaryCard(
    stats: TodayQuickStats,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141418)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF27272E), RoundedCornerShape(20.dp))
            .testTag("today_live_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header of Summary Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مؤشرات الشهر الحية (${formatArabicMonthYear(stats.date)})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                }

                Surface(
                    color = if (stats.isTodayClosed) ProfitGreen.copy(alpha = 0.2f) else GoldAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${stats.monthClosedDaysCount} يوم مقفل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.isTodayClosed) ProfitGreen else GoldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 3 Primary Monthly Metrics Grid:
            // 1. مبيعات الشهر | 2. إجمالي المشتريات | 3. إجمالي الديون
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Month Sales
                QuickMetricTile(
                    label = "مبيعات الشهر",
                    value = formatCurrency(stats.monthSales),
                    valueColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Total Monthly Purchases
                QuickMetricTile(
                    label = "إجمالي المشتريات",
                    value = formatCurrency(stats.monthPurchases),
                    valueColor = Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f)
                )

                // Metric 3: Total Cumulative Supplier Debts
                QuickMetricTile(
                    label = "إجمالي الديون",
                    value = formatCurrency(stats.cumulativeSupplierDebt),
                    valueColor = Color(0xFFFB7185),
                    modifier = Modifier.weight(1f)
                )
            }

            // Secondary Info Strip (Today's Staff Attendance & Drawer Cash)
            Surface(
                color = Color(0xFF1E1E24),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = SkyBlueAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حضور الموظفين: ${stats.activeStaffCount} / ${stats.totalEmployees}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نقدية الدرج اليوم: ${formatCurrency(stats.expectedDrawerCash)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Developer & System Designer Signature Card at the bottom of the Dashboard
 */
@Composable
private fun DeveloperCreditCard(
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12151E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.6f),
                        Color(0xFFC084FC).copy(alpha = 0.6f),
                        Color(0xFFFB923C).copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .testTag("developer_credit_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Developer",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "تصميم وتطوير البرنامج",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Eng.Mustafa Saad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "System Architect & Software Designer",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Surface(
                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "المطور",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Dialog displaying full system & developer attribution for Eng.Mustafa Saad
 */
@Composable
private fun DeveloperInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141A),
        titleContentColor = Color(0xFFF8FAFC),
        textContentColor = Color(0xFFE2E8F0),
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Eng.Mustafa Saad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF38BDF8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مصمم ومطور النظام المالي والإداري",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFB923C),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF1E1E26),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "منظومة محسن ماركت المتكاملة (قاصد كريم)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "نظام محاسبي وإداري سحابي مربوط بقاعدة بيانات SQL Server المركزية لإدارة المبيعات، الخزينة، فواتير الموردين، المصروفات، وشؤون الموظفين.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Text(
                    text = "Designed & Engineered by Eng.Mustafa Saad ©",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun QuickMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E1E24),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = modifier.border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reusable M3 Navigation Card with custom color accents, icon, and badge
 */
@Composable
private fun DashboardNavigationCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with solid accent color
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor,
                shadowElevation = 2.dp,
                modifier = Modifier.size(52.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title, Subtitle, and Badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Navigation Arrow with dark theme circular background
            Surface(
                shape = CircleShape,
                color = Color(0xFF1C1C24),
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "فتح",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Error Banner with Retry button
 */
@Composable
private fun DashboardErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Surface(
        color = Color(0xFF241014),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ExpenseRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "خطأ في اتصال السيرفر",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        maxLines = 2
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إعادة المحاولة", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
