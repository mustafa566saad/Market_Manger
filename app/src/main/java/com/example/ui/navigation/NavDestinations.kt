package com.example.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for Mohsen Market.
 */
sealed interface MohsenDestination {
    @Serializable
    data object Dashboard : MohsenDestination

    @Serializable
    data object StaffAttendance : MohsenDestination

    @Serializable
    data object SupplierBills : MohsenDestination

    @Serializable
    data object SupplierDebtSummary : MohsenDestination

    @Serializable
    data object DailyExpenses : MohsenDestination

    @Serializable
    data object DailyTreasury : MohsenDestination

    @Serializable
    data object MonthlyFinancialClosing : MohsenDestination
}
