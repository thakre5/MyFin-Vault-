package com.example.myfin.data

data class AccountBalanceResult(
    val accountName: String,
    val startingBalance: Double,
    val accountType: String,
    val minBalance: Double = 0.0,
    val sortOrder: Int,
    val currentBalance: Double
)

data class MonthlySummary(
    val month: Int,
    val totalActualIncome: Double = 0.0,
    val totalActualExpense: Double = 0.0,
    val totalAsset: Double = 0.0
)

data class YearlyCategoryRollup(
    val category: String,
    val type: TransactionType,
    val totalActualAmount: Double
)
