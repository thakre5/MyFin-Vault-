package com.example.myfin.data

data class AccountBalanceResult(
    val accountName: String,
    val startingBalance: Double,
    val accountType: String,
    val minBalance: Double = 0.0,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val currentBalance: Double = 0.0,
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0
)

data class MonthlySummary(
    val month: Int,
    val totalActualIncome: Double = 0.0,
    val totalActualExpense: Double = 0.0,
    val totalAsset: Double = 0.0
) {
    val netSurplus: Double
        get() = totalActualIncome - totalActualExpense
}

data class YearlyCategoryRollup(
    val category: String,
    val type: TransactionType,
    val totalActualAmount: Double = 0.0,
    val totalAmount: Double = totalActualAmount
)
