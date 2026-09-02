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
) {
    /**
     * True if the account has an active MAB threshold and the current balance is below it.
     */
    val isMabBreached: Boolean
        get() = minBalance > 0.0 && currentBalance < minBalance

    /**
     * The shortfall amount required to restore the account to its MAB floor.
     */
    val mabDeficit: Double
        get() = if (isMabBreached) minBalance - currentBalance else 0.0

    /**
     * Available liquid funds above the MAB floor that are safe to spend or sweep.
     */
    val spendableBalance: Double
        get() = (currentBalance - minBalance).coerceAtLeast(0.0)
}

data class MonthlySummary(
    val month: Int,
    val totalActualIncome: Double = 0.0,
    val totalActualExpense: Double = 0.0,
    val totalAsset: Double = 0.0
) {
    val netSurplus: Double
        get() = totalActualIncome - totalActualExpense

    val savingsRate: Int
        get() = if (totalActualIncome > 0.0) {
            (((totalActualIncome - totalActualExpense) / totalActualIncome) * 100).toInt()
        } else 0
}

data class YearlyCategoryRollup(
    val category: String,
    val type: TransactionType,
    val totalActualAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)
