package com.example.myfin.data

import kotlin.math.max

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
    val isMabBreached: Boolean
        get() = minBalance > 0.0 && currentBalance < minBalance

    val mabDeficit: Double
        get() = if (isMabBreached) (minBalance - currentBalance).coerceAtLeast(0.0) else 0.0

    val spendableBalance: Double
        get() = (currentBalance - minBalance).coerceAtLeast(0.0)

    fun effectiveBalance(pendingBills: Double = 0.0): Double =
        currentBalance - pendingBills.coerceAtLeast(0.0)

    fun isEffectiveMabBreached(pendingBills: Double = 0.0): Boolean =
        minBalance > 0.0 && effectiveBalance(pendingBills) < minBalance

    fun effectiveMabDeficit(pendingBills: Double = 0.0): Double =
        if (isEffectiveMabBreached(pendingBills)) {
            (minBalance - effectiveBalance(pendingBills)).coerceAtLeast(0.0)
        } else 0.0

    fun effectiveSpendableBalance(pendingBills: Double = 0.0): Double =
        (effectiveBalance(pendingBills) - minBalance).coerceAtLeast(0.0)
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
            (((totalActualIncome - totalActualExpense) / totalActualIncome) * 100)
                .toInt()
                .coerceIn(-100, 100)
        } else 0
}

data class YearlyCategoryRollup(
    val category: String,
    val type: TransactionType,
    val totalActualAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

// ============================================================================
// Phase 2 Domain Projections (Investments, Goals, Payday Sweep, Bill Variance)
// ============================================================================

data class InvestmentHoldingUiModel(
    val entity: InvestmentEntity,
    val linkedSipTotal: Double = 0.0
) {
    val totalInvested: Double
        get() = entity.openingInvestedAmount + linkedSipTotal

    val currentValue: Double
        get() = if (entity.currentValuation > 0.0) entity.currentValuation else totalInvested

    val unrealizedPnL: Double
        get() = currentValue - totalInvested

    val returnPct: Double
        get() = if (totalInvested > 0.0) ((unrealizedPnL / totalInvested) * 100.0) else 0.0
}

data class GoalProgressUiModel(
    val entity: GoalEntity,
    val syncedAmount: Double = 0.0,
    val monthsRemaining: Int = 1
) {
    val currentSaved: Double
        get() = (entity.initialSavedAmount + syncedAmount).coerceAtLeast(0.0)

    val remainingAmount: Double
        get() = (entity.targetAmount - currentSaved).coerceAtLeast(0.0)

    val progressFraction: Float
        get() = if (entity.targetAmount > 0.0) {
            (currentSaved / entity.targetAmount).toFloat().coerceIn(0f, 1f)
        } else 0f

    val requiredMonthlyContribution: Double
        get() = if (remainingAmount <= 0.0) 0.0 else remainingAmount / max(1, monthsRemaining)
}

data class PaydaySweepPlan(
    val operatingAccountName: String,
    val commitmentsAccountName: String?,
    val fortressAccountName: String?,
    val toCommitmentsAmount: Double = 0.0,
    val toFortressAmount: Double = 0.0,
    val operatingSpendableAvailable: Double = 0.0
) {
    val totalSweepAmount: Double
        get() = toCommitmentsAmount + toFortressAmount

    val isActionable: Boolean
        get() = totalSweepAmount > 0.0 && operatingSpendableAvailable >= 100.0
}

data class BillVarianceAlert(
    val billTitle: String,
    val enteredAmount: Double,
    val baselineAmount: Double,
    val variancePct: Int
)
