package com.example.myfin.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfin.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

data class SubcategoryPerformance(
    val name: String,
    val amount: Double
)

data class CategoryPerformance(
    val category: String,
    val type: TransactionType,
    val plannedAmount: Double,
    val actualAmount: Double,
    val activeSubcategories: List<SubcategoryPerformance> = emptyList(),
    val isReimbursableFloat: Boolean = false
) {
    val variance: Double get() = plannedAmount - actualAmount
    val isOverBudget: Boolean get() = !isReimbursableFloat && type == TransactionType.EXPENSE && actualAmount > plannedAmount && plannedAmount > 0

    val name: String get() = category
    val categoryName: String get() = category
    val spentAmount: Double get() = actualAmount
    val budgetedAmount: Double get() = plannedAmount
}

data class CommitmentsShortfallStatus(
    val isShortfall: Boolean = false,
    val shortfallAmount: Double = 0.0,
    val unpaidBillsTotal: Double = 0.0,
    val commitmentsBalance: Double = 0.0,
    val requiredBuffer: Double = 0.0,
    val earliestDueDay: Int? = null,
    val affectedAccountName: String = "Commitments",
    val affectedAccountsCount: Int = 0
)

data class PaydayAllocationPlan(
    val salaryAmount: Double = 0.0,
    val toOperating: Double = 0.0,
    val toCommitments: Double = 0.0,
    val toFortressBase: Double = 0.0,
    val toFortressSurplus: Double = 0.0,
    val totalToFortress: Double = 0.0,
    val commitmentsShortfallCovered: Double = 0.0,
    val remainingCommitmentsShortfall: Double = 0.0,
    val isShortfallFullyFunded: Boolean = true,
    val isOperatingFullyFunded: Boolean = true,
    val pendingBillsCount: Int = 0
) {
    val toFortress: Double get() = totalToFortress
    val remainingOperating: Double get() = toOperating
}

data class MonthEndSweepPlan(
    val availableOperatingCash: Double = 0.0,
    val runwayBuffer: Double = 0.0,
    val runwayDays: Int = 0,
    val targetNextPayday: Int = 0,
    val sweepAmount: Double = 0.0
)

data class ReimbursementStatus(
    val totalWorkExpenses: Double = 0.0,
    val totalClaimsReceived: Double = 0.0,
    val pendingReimbursement: Double = 0.0,
    val isSettled: Boolean = true,
    val cumulativeWorkExpenses: Double = 0.0,
    val cumulativeClaimsReceived: Double = 0.0,
    val excessAdvanceHeld: Double = 0.0
)

data class AssetWealthMetrics(
    val grossWealth: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val liquidReserves: Double = 0.0,
    val activeReceivables: Double = 0.0,
    val npaWrittenOff: Double = 0.0,
    val realizableNetWorth: Double = 0.0
)

data class MultiYearAssetMetric(
    val year: Int,
    val totalAssets: Double = 0.0,
    val growthPercent: Double = 0.0
)

data class YearlyMonthData(
    val monthIndex: Int,
    val monthName: String,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val assets: Double = 0.0,
    val lifestyleExpenses: Double = 0.0,
    val workExpenses: Double = 0.0,
    val corporateReimbursements: Double = 0.0,
    val netSavings: Double = 0.0,
    val fixedExpenses: Double = 0.0,
    val variableExpenses: Double = 0.0,
    val isFuture: Boolean = false,
    val transactions: List<TransactionEntity> = emptyList()
)

typealias MonthDataSummary = YearlyMonthData
typealias MonthlyMetrics = DashboardMetrics
typealias CategoryProgressItem = CategoryPerformance

data class DashboardMetrics(
    val plannedIncome: Double = 0.0,
    val actualIncome: Double = 0.0,
    val plannedExpenses: Double = 0.0,
    val actualExpenses: Double = 0.0,
    val plannedAssets: Double = 0.0,
    val actualAssets: Double = 0.0,
    val fixedCommitmentsTotal: Double = 0.0,
    val safeToSpend: Double = 0.0,
    val theoreticalSafeToSpend: Double = 0.0,
    val liquidOperatingCash: Double = 0.0,
    val safeToSpendPercentage: Int = 100,
    val netSavedAfterInvest: Double = 0.0,
    val totalVaultBalance: Double = 0.0,
    val isOverBudget: Boolean = false,
    val dailyExpensePoints: List<Float> = emptyList(),
    val workExpenses: Double = 0.0,
    val corporateReimbursements: Double = 0.0,
    val pendingReimbursement: Double = 0.0,
    val lifestyleExpenses: Double = 0.0,
    val personalIncome: Double = 0.0,
    val daysUntilPayday: Int = 0,
    val nextPaydayDay: Int = 1,
    val isSalaryDelayed: Boolean = false
) {
    val totalAssetAllocated: Double get() = actualAssets
    val personalBurn: Double get() = lifestyleExpenses
}

data class MonthlyUiState(
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val accounts: List<AccountBalanceResult> = emptyList(),
    val activeAccounts: List<AccountBalanceResult> = emptyList(),
    val archivedAccounts: List<AccountBalanceResult> = emptyList(),
    val fixedBills: List<FixedBillEntity> = emptyList(),
    val categories: List<CategoryPerformance> = emptyList(),
    val masterCategories: List<CategoryEntity> = emptyList(),
    val masterSubcategories: List<SubcategoryEntity> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionEntity>> = emptyMap(),
    val budgetPlans: List<BudgetPlanEntity> = emptyList(),
    val commitmentsShortfall: CommitmentsShortfallStatus = CommitmentsShortfallStatus(),
    val paydaySuggestion: PaydayAllocationPlan? = null,
    val monthEndSweepSuggestion: MonthEndSweepPlan? = null,
    val reimbursementStatus: ReimbursementStatus = ReimbursementStatus(),
    val frequentCategories: List<CategoryEntity> = emptyList(),
    val frequentSubcategories: List<SubcategoryEntity> = emptyList(),
    val frequentAccounts: List<String> = emptyList(),
    val isRolloverBannerVisible: Boolean = false,
    val rolloverBannerMessage: String = "",
    val isTaxonomyGracePeriodActive: Boolean = false,
    val isTaxonomyBannerVisible: Boolean = false,
    val fortressTarget: Double = 0.0,
    val fortressFdBalance: Double = 0.0,
    val fortressProgressPercentage: Int = 0
) {
    val categoryBreakdowns: List<CategoryPerformance> get() = categories
    val accountList: List<String> get() = frequentAccounts.ifEmpty { activeAccounts.map { it.accountName } }
    val subcategories: List<SubcategoryEntity> get() = frequentSubcategories.ifEmpty { masterSubcategories }
}

data class YearlyUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val monthlyRollups: List<MonthlySummary> = emptyList(),
    val categoryRollups: List<YearlyCategoryRollup> = emptyList(),
    val totalYearlyIncome: Double = 0.0,
    val totalYearlyExpense: Double = 0.0,
    val totalYearlyAssets: Double = 0.0,
    val annualNetSurplus: Double = 0.0,
    val yearlyMonths: List<YearlyMonthData> = emptyList(),
    val multiYearAssets: List<MultiYearAssetMetric> = emptyList(),
    val assetWealthMetrics: AssetWealthMetrics = AssetWealthMetrics(),
    val reimbursementStatus: ReimbursementStatus = ReimbursementStatus(),
    val annualLifestyleExpenses: Double = 0.0,
    val annualPersonalIncome: Double = 0.0,
    val allYearTransactions: List<TransactionEntity> = emptyList()
) {
    val totalAnnualIncome: Double get() = totalYearlyIncome
    val totalAnnualExpense: Double get() = totalYearlyExpense
    val netAnnualSavings: Double get() = annualNetSurplus
    val monthlySummaries: List<MonthlySummary> get() = monthlyRollups
    val yearlyMonthsData: List<YearlyMonthData> get() = yearlyMonths
}

data class FilterCriteria(
    val query: String = "",
    val type: TransactionType? = null,
    val account: String = "ALL",
    val startDate: Long? = null,
    val endDate: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val dao: BudgetDao,
    val securityManager: SecurityManager
) : ViewModel() {

    private val cal = Calendar.getInstance()
    val currentYear = MutableStateFlow(cal.get(Calendar.YEAR))
    val currentMonth = MutableStateFlow(cal.get(Calendar.MONTH) + 1)
    val filterCriteria = MutableStateFlow(FilterCriteria())
    val isAppUnlocked = MutableStateFlow(false)

    val showRolloverBanner = MutableStateFlow(false)
    val rolloverBannerMessage = MutableStateFlow("")

    val protectedCategories = setOf(
        "Utilities & Living Bills",
        "Everyday Living",
        "Leisure, Trips & Media",
        "Health & Medical",
        "Family & Home Support",
        "Debt & Financial Obligations",
        "Work & Professional",
        "Reimbursements & Claims",
        "Investments & Wealth",
        "Liquid Reserves & Receivables",
        "Salary & Professional Inflow",
        "Passive & Capital Drawdowns",
        "Refunds & Recoveries",
        "General"
    )

    val userProfile: StateFlow<UserProfile> = dao.getUserProfile()
        .map { it ?: UserProfile(id = 1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile(id = 1))

    init {
        viewModelScope.launch(Dispatchers.IO) {
            seedFullExcelTaxonomyIfEmpty()
            seedDefaultAccountsIfEmpty()
            checkAndRolloverRecurringBills(currentMonth.value, currentYear.value)
            checkAndExecuteMonthEndAutoRollover()
        }
    }

    val monthlyUiState: StateFlow<MonthlyUiState> = combine(
        currentMonth, currentYear, filterCriteria, userProfile
    ) { month, year, filter, profile ->
        Tuple4(month, year, filter, profile)
    }.flatMapLatest { (month, year, filter, profile) ->
        val coreDataFlow = combine(
            dao.getTransactionsForMonth(month, year),
            dao.getFixedBillsForMonth(month, year),
            dao.getAccountBalances()
        ) { transactions, fixedBills, accounts ->
            Triple(transactions, fixedBills, accounts)
        }

        val metadataFlow = combine(
            dao.getBudgetPlansForMonth(month, year),
            dao.getAllCategories(),
            dao.getAllSubcategories()
        ) { plans, masterCats, masterSubcats ->
            Triple(plans, masterCats, masterSubcats)
        }

        val globalHistoryFlow = flow {
            emit(try { dao.getAllTransactions() } catch (_: Exception) { emptyList() })
        }

        val bannerFlow = combine(showRolloverBanner, rolloverBannerMessage) { show, msg -> show to msg }

        combine(coreDataFlow, metadataFlow, globalHistoryFlow, bannerFlow) { coreData, metaData, allTimeTxs, bannerInfo ->
            val (transactions, fixedBills, allAccounts) = coreData
            val (plans, masterCats, masterSubcats) = metaData
            val (isBannerVisible, bannerMsg) = bannerInfo

            val todayCal = Calendar.getInstance()
            val sysMonth = todayCal.get(Calendar.MONTH) + 1
            val sysYear = todayCal.get(Calendar.YEAR)

            val isGracePeriodActive = (profile.taxonomyGraceYear == sysYear && profile.taxonomyGraceMonth == sysMonth)
            val isTaxonomyBannerVisible = isGracePeriodActive && !profile.isTaxonomyBannerDismissed

            val activeTaxonomyCats = if (isGracePeriodActive) {
                masterCats
            } else {
                masterCats.filter { !it.isLegacy }
            }

            val activeTaxonomySubcats = if (isGracePeriodActive) {
                masterSubcats
            } else {
                masterSubcats.filter { !it.isLegacy }
            }

            val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000L

            val categoryScores = mutableMapOf<String, Int>()
            val subcategoryScores = mutableMapOf<String, Int>()
            val accountScores = mutableMapOf<String, Int>()

            allTimeTxs.forEach { tx ->
                val weight = if (tx.date >= ninetyDaysAgo) 3 else 1
                categoryScores[tx.category] = (categoryScores[tx.category] ?: 0) + weight

                val subKey = "${tx.category.trim().lowercase()}:::${tx.subcategory.trim().lowercase()}"
                subcategoryScores[subKey] = (subcategoryScores[subKey] ?: 0) + weight

                accountScores[tx.accountName] = (accountScores[tx.accountName] ?: 0) + weight
                if (!tx.toAccountName.isNullOrBlank()) {
                    accountScores[tx.toAccountName] = (accountScores[tx.toAccountName] ?: 0) + weight
                }
            }

            val sortedMasterCats = activeTaxonomyCats.sortedWith(
                compareByDescending<CategoryEntity> { categoryScores[it.name] ?: 0 }
                    .thenBy { it.name }
            )

            val sortedMasterSubcats = activeTaxonomySubcats.sortedWith(
                compareByDescending<SubcategoryEntity> {
                    subcategoryScores["${it.parentCategory.trim().lowercase()}:::${it.name.trim().lowercase()}"] ?: 0
                }.thenBy { it.name }
            )

            val activeAccounts = allAccounts.filter { !it.isArchived }.sortedBy { it.sortOrder }
            val archivedAccounts = allAccounts.filter { it.isArchived }.sortedBy { it.sortOrder }

            val frequentAccounts = activeAccounts.sortedWith(
                compareByDescending<AccountBalanceResult> { accountScores[it.accountName] ?: 0 }
                    .thenBy { it.sortOrder }
            ).map { it.accountName }

            val regularTxs = transactions.filter { it.type != TransactionType.TRANSFER }

            val actualIncome = regularTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val actualExpenses = regularTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val actualAssets = regularTxs.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }

            val corporateTxs = regularTxs.filter { it.type == TransactionType.CORPORATE }
            val workExpenses = corporateTxs.filter { !it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
            val corporateReimbursements = corporateTxs.filter { it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }

            val allTimeCorporateTxs = allTimeTxs.filter { it.type == TransactionType.CORPORATE }
            val allTimeWorkExpenses = allTimeCorporateTxs.filter { !it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
            val allTimeClaimsReceived = allTimeCorporateTxs.filter { it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }

            val effectiveAllTimeWorkExpenses = allTimeWorkExpenses + profile.initialReimbursementClaim
            val effectiveAllTimeClaimsReceived = allTimeClaimsReceived + profile.initialCompanyAdvance

            val cumulativePending = (effectiveAllTimeWorkExpenses - effectiveAllTimeClaimsReceived).coerceAtLeast(0.0)
            val excessAdvanceHeld = (effectiveAllTimeClaimsReceived - effectiveAllTimeWorkExpenses).coerceAtLeast(0.0)

            val monthReimbursementStatus = ReimbursementStatus(
                totalWorkExpenses = workExpenses,
                totalClaimsReceived = corporateReimbursements,
                pendingReimbursement = cumulativePending,
                isSettled = cumulativePending <= 0.0 && excessAdvanceHeld <= 0.0,
                cumulativeWorkExpenses = effectiveAllTimeWorkExpenses,
                cumulativeClaimsReceived = effectiveAllTimeClaimsReceived,
                excessAdvanceHeld = excessAdvanceHeld
            )

            val isLoanRepayment = { tx: TransactionEntity ->
                tx.type == TransactionType.INCOME &&
                (tx.subcategory.contains("Loan Paybacks Received", ignoreCase = true) ||
                 tx.title.contains("Loan Payback", ignoreCase = true))
            }

            val isTaxOrPurchaseRefund = { tx: TransactionEntity ->
                tx.type == TransactionType.INCOME &&
                (tx.subcategory.contains("Tax & Purchase Refunds", ignoreCase = true) ||
                 tx.title.contains("Refund", ignoreCase = true))
            }

            val isCapitalDrawdown = { tx: TransactionEntity ->
                tx.type == TransactionType.INCOME &&
                (tx.category.equals("Passive & Capital Drawdowns", ignoreCase = true) ||
                 tx.subcategory.contains("Capital Gains", ignoreCase = true) ||
                 tx.subcategory.contains("Realization", ignoreCase = true) ||
                 tx.subcategory.contains("Emergency Fund Drawdown", ignoreCase = true) ||
                 tx.subcategory.contains("FD / Deposit Maturity", ignoreCase = true))
            }

            val nonPersonalInflow = regularTxs.filter {
                isLoanRepayment(it) || isTaxOrPurchaseRefund(it) || isCapitalDrawdown(it)
            }.sumOf { it.amount }

            val lifestyleExpenses = actualExpenses
            val personalIncome = (actualIncome - nonPersonalInflow).coerceAtLeast(0.0)

            val isLoanGiven = { tx: TransactionEntity ->
                tx.type == TransactionType.ASSET &&
                (tx.subcategory.contains("Personal Loans", ignoreCase = true) ||
                 tx.subcategory.contains("Loaned", ignoreCase = true))
            }

            val isNpaWriteOff = { tx: TransactionEntity ->
                tx.type == TransactionType.ASSET &&
                (tx.subcategory.contains("NPA", ignoreCase = true) ||
                 tx.subcategory.contains("Bad Debt", ignoreCase = true) ||
                 tx.category.equals("NPA", ignoreCase = true))
            }

            val isGenuineSavingsOrAsset = { tx: TransactionEntity ->
                tx.type == TransactionType.ASSET && !isLoanGiven(tx) && !isNpaWriteOff(tx)
            }

            val actualSavingsAndInvestments = regularTxs.filter(isGenuineSavingsOrAsset).sumOf { it.amount }

            val allCategoryNames = (sortedMasterCats.map { it.name to it.type } +
                    plans.map { it.category to it.type } +
                    fixedBills.map { it.category to it.type } +
                    regularTxs.map { it.category to it.type }).distinct()

            val matrixList = allCategoryNames.mapNotNull { (catName, catType) ->
                if (catType == TransactionType.TRANSFER) return@mapNotNull null

                val isCorporateCat = catType == TransactionType.CORPORATE

                val isNonPersonalIncomeCat = catType == TransactionType.INCOME &&
                        (catName.equals("Passive & Capital Drawdowns", ignoreCase = true) ||
                         catName.contains("Capital Drawdown", ignoreCase = true))

                val catTxs = regularTxs.filter { tx ->
                    tx.category.equals(catName, ignoreCase = true) && tx.type == catType &&
                    !(catType == TransactionType.INCOME && (isLoanRepayment(tx) || isTaxOrPurchaseRefund(tx) || isCapitalDrawdown(tx)))
                }

                val actualTotal = catTxs.sumOf { it.amount }

                val manualPlan = plans.find { it.category.equals(catName, ignoreCase = true) && it.type == catType }?.plannedAmount ?: 0.0
                val fixedForCat = fixedBills.filter { it.category.equals(catName, ignoreCase = true) && it.type == catType }.sumOf { it.amount }
                val effectivePlanned = if (manualPlan > 0.0) max(manualPlan, fixedForCat) else fixedForCat

                if (isNonPersonalIncomeCat && actualTotal == 0.0 && effectivePlanned == 0.0) return@mapNotNull null
                if (actualTotal == 0.0 && effectivePlanned == 0.0) return@mapNotNull null

                val activeSubs = catTxs.groupBy { it.subcategory }
                    .map { (subName, txs) -> SubcategoryPerformance(subName, txs.sumOf { it.amount }) }
                    .filter { it.amount != 0.0 }

                CategoryPerformance(
                    category = catName,
                    type = catType,
                    plannedAmount = effectivePlanned,
                    actualAmount = actualTotal,
                    activeSubcategories = activeSubs,
                    isReimbursableFloat = isCorporateCat
                )
            }

            val plannedIncome = matrixList.filter { it.type == TransactionType.INCOME }.sumOf { it.plannedAmount }
            val plannedExpenses = matrixList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.plannedAmount }
            val plannedAssets = matrixList.filter { it.type == TransactionType.ASSET }.sumOf { it.plannedAmount }

            val allFixedCommitments = fixedBills.filter {
                it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER
            }
            val fixedExpenseTotal = allFixedCommitments.sumOf { it.amount }
            val pendingFixedCommitments = allFixedCommitments.filter { !it.isPaid }.sumOf { it.amount }

            val pendingAssetCommitment = (plannedAssets - actualSavingsAndInvestments).coerceAtLeast(0.0)
            val totalPendingCommitments = pendingFixedCommitments + pendingAssetCommitment

            val baseIncome = when {
                max(plannedIncome, personalIncome) > 0.0 -> max(plannedIncome, personalIncome)
                profile.baseMonthlyIncome > 0.0 -> profile.baseMonthlyIncome
                else -> 0.0
            }

            // =========================================================================
            // 6. DYNAMIC PRIMARY SALARY DETECTION & PAYDAY GRACE PERIOD ENGINE
            // =========================================================================
            val salaryTxsInCurrentMonth = regularTxs.filter {
                it.type == TransactionType.INCOME &&
                it.category.equals("Salary & Professional Inflow", ignoreCase = true)
            }
            // Pick largest salary transaction to avoid bonuses/freelance splits hijacking the date
            val currentMonthSalaryTx = salaryTxsInCurrentMonth.maxByOrNull { it.amount }

            val historicalSalaryTxs = allTimeTxs.filter {
                it.type == TransactionType.INCOME &&
                it.category.equals("Salary & Professional Inflow", ignoreCase = true)
            }
            val lastKnownSalaryTx = currentMonthSalaryTx ?: historicalSalaryTxs.maxByOrNull { it.date }

            val dynamicSalaryDay = lastKnownSalaryTx?.let {
                val c = Calendar.getInstance().apply { timeInMillis = it.date }
                c.get(Calendar.DAY_OF_MONTH)
            } ?: 1

            val todayMidnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val currentDayOfMonth = todayMidnight.get(Calendar.DAY_OF_MONTH)
            val isCurrentSystemMonth = (month == sysMonth) && (year == sysYear)
            val isPastMonth = (year < sysYear) || (year == sysYear && month < sysMonth)

            var isSalaryDelayed = false
            val daysUntilUpcomingSalary: Int

            if (isPastMonth) {
                daysUntilUpcomingSalary = 0
            } else if (!isCurrentSystemMonth) {
                // Future planned months: assume standard 30-day runway
                daysUntilUpcomingSalary = 30
            } else {
                if (currentMonthSalaryTx != null) {
                    // Salary for current month has already arrived! Target NEXT month's payday.
                    val nextPaydayCal = Calendar.getInstance().apply {
                        timeInMillis = todayMidnight.timeInMillis
                        add(Calendar.MONTH, 1)
                        val maxDayNextMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, min(dynamicSalaryDay, maxDayNextMonth))
                    }
                    val diffMillis = nextPaydayCal.timeInMillis - todayMidnight.timeInMillis
                    daysUntilUpcomingSalary = max(1L, diffMillis / (24 * 60 * 60 * 1000L)).toInt()
                } else {
                    // Salary for current month has NOT arrived yet.
                    if (currentDayOfMonth <= dynamicSalaryDay) {
                        // Payday is upcoming this month
                        daysUntilUpcomingSalary = max(1, dynamicSalaryDay - currentDayOfMonth)
                    } else if (currentDayOfMonth <= dynamicSalaryDay + 4) {
                        // 4-Day Grace Period: Weekend / Holiday payroll delay detected!
                        // Do not lock a 30-day buffer; hold only 1-day burn cushion
                        daysUntilUpcomingSalary = 1
                        isSalaryDelayed = true
                    } else {
                        // More than 4 days overdue without logged salary: target next cycle
                        val nextPaydayCal = Calendar.getInstance().apply {
                            timeInMillis = todayMidnight.timeInMillis
                            add(Calendar.MONTH, 1)
                            val maxDayNextMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                            set(Calendar.DAY_OF_MONTH, min(dynamicSalaryDay, maxDayNextMonth))
                        }
                        val diffMillis = nextPaydayCal.timeInMillis - todayMidnight.timeInMillis
                        daysUntilUpcomingSalary = max(1L, diffMillis / (24 * 60 * 60 * 1000L)).toInt()
                        isSalaryDelayed = true
                    }
                }
            }

            // =========================================================================
            // 7. HISTORICAL BASELINE BURN VELOCITY
            // =========================================================================
            val historicalMonthsSpend = allTimeTxs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { "${it.year}-${it.month}" }
                .values
                .map { it.sumOf { tx -> tx.amount } }
                .filter { it > 0.0 }

            val historicalAvgSpend = if (historicalMonthsSpend.isNotEmpty()) {
                historicalMonthsSpend.average()
            } else {
                if (plannedExpenses > 0.0) plannedExpenses else profile.baseMonthlyIncome.coerceAtLeast(0.0)
            }

            val livingBufferTarget = max(historicalAvgSpend, plannedExpenses.takeIf { it > 0.0 } ?: profile.baseMonthlyIncome.coerceAtLeast(0.0))
            val dailyBurnVelocity = livingBufferTarget / 30.0
            val runwayProtectionUntilSalary = dailyBurnVelocity * daysUntilUpcomingSalary

            // =========================================================================
            // 8. OPTION 2: PURE GUILT-FREE SAFE-TO-SPEND (LIQUID CAPITAL MINUS BUFFER)
            // =========================================================================
            val isFortressAccount = { acc: AccountBalanceResult ->
                acc.accountType.equals("Fortress", ignoreCase = true) ||
                acc.accountName.contains("FORTRESS", ignoreCase = true) ||
                acc.accountName.contains("TERTIARY", ignoreCase = true)
            }

            // Accessible liquid pool (Operating + Commitments above MAB + Cash)
            val liquidPoolAccounts = activeAccounts.filter { !isFortressAccount(it) }

            val totalLiquidAboveMab = liquidPoolAccounts.sumOf {
                (it.currentBalance - it.minBalance).coerceAtLeast(0.0)
            }.coerceAtLeast(0.0)

            val pendingFixedBills = allFixedCommitments.filter { bill ->
                !bill.isPaid &&
                bill.type != TransactionType.INCOME &&
                !(bill.type == TransactionType.CORPORATE && bill.category.equals("Reimbursements & Claims", ignoreCase = true))
            }.sumOf { it.amount }

            val option2SafeToSpend = (totalLiquidAboveMab - pendingFixedBills - excessAdvanceHeld - runwayProtectionUntilSalary)
                .coerceAtLeast(0.0)

            val personalDiscretionaryExpenses = regularTxs.filter { tx ->
                tx.type == TransactionType.EXPENSE && tx.linkedFixedBillId == null
            }.sumOf { it.amount }

            val rawTheoreticalSafeToSpend = baseIncome - totalPendingCommitments - personalDiscretionaryExpenses
            val theoreticalSafeToSpend = if (baseIncome > 0) rawTheoreticalSafeToSpend.coerceAtLeast(0.0) else 0.0

            val safeToSpendPercentage = if (totalLiquidAboveMab > 0.0) {
                ((option2SafeToSpend / totalLiquidAboveMab) * 100).toInt().coerceIn(0, 100)
            } else 0

            val isOverBudget = rawTheoreticalSafeToSpend < 0.0 || (plannedExpenses > 0 && lifestyleExpenses > plannedExpenses)
            val netSaved = (personalIncome - lifestyleExpenses) - actualSavingsAndInvestments
            val totalVault = allAccounts.sumOf { it.currentBalance }
            val dailyPoints = calculateDailySparklinePoints(regularTxs, month, year)

            // =========================================================================
            // 9. COMMITMENTS SHORTFALL ENGINE
            // =========================================================================
            val is3VaultMode = profile.vaultMode.contains("3", ignoreCase = true)
            val commitmentAccounts = activeAccounts.filter {
                it.accountType.equals("Commitments", ignoreCase = true)
            }

            var totalCommitmentsShortfall = 0.0
            var totalUnpaidCommitmentsSum = 0.0
            var totalCommitmentsBalance = 0.0
            var totalCommitmentsRequiredBuffer = 0.0
            var earliestDueDay: Int? = null
            val affectedAccountNames = mutableListOf<String>()

            commitmentAccounts.forEach { acc ->
                val accBills = fixedBills.filter { bill ->
                    !bill.isPaid &&
                    bill.type != TransactionType.INCOME &&
                    bill.accountName.equals(acc.accountName, ignoreCase = true)
                }
                val accUnpaidTotal = accBills.sumOf { it.amount }
                val accNet = acc.currentBalance - accUnpaidTotal - acc.minBalance

                totalUnpaidCommitmentsSum += accUnpaidTotal
                totalCommitmentsBalance += acc.currentBalance
                totalCommitmentsRequiredBuffer += acc.minBalance

                if (accNet < 0.0) {
                    totalCommitmentsShortfall += abs(accNet)
                    affectedAccountNames.add(acc.accountName)
                }

                val minDue = accBills.mapNotNull { it.dueDay }.minOrNull()
                if (minDue != null) {
                    earliestDueDay = if (earliestDueDay == null) minDue else min(earliestDueDay!!, minDue)
                }
            }

            val isShortfall = is3VaultMode && (totalCommitmentsShortfall > 0.0)
            val affectedAccountLabel = when {
                affectedAccountNames.isEmpty() -> if (commitmentAccounts.isNotEmpty()) commitmentAccounts.first().accountName else "Commitments"
                affectedAccountNames.size == 1 -> affectedAccountNames.first()
                else -> "${affectedAccountNames.size} Commitments Vaults"
            }

            val shortfallStatus = CommitmentsShortfallStatus(
                isShortfall = isShortfall,
                shortfallAmount = if (isShortfall) totalCommitmentsShortfall else 0.0,
                unpaidBillsTotal = totalUnpaidCommitmentsSum,
                commitmentsBalance = totalCommitmentsBalance,
                requiredBuffer = totalCommitmentsRequiredBuffer,
                earliestDueDay = earliestDueDay,
                affectedAccountName = affectedAccountLabel,
                affectedAccountsCount = affectedAccountNames.size
            )

            // =========================================================================
            // 10. PAYDAY ALLOCATION & MONTH-END SWEEP ENGINES
            // =========================================================================
            val totalDaysInCurrentMonth = todayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val salaryTx = currentMonthSalaryTx

            val isPaydayAllocated = transactions.any { tx ->
                tx.type == TransactionType.TRANSFER &&
                salaryTx != null && tx.date >= salaryTx.date &&
                (tx.transferSubtype == TransferSubtype.BILL_FUNDING || tx.title.contains("Payday Allocation", ignoreCase = true)) &&
                tx.month == month && tx.year == year
            }

            val paydaySuggestion = if (salaryTx != null && is3VaultMode && isCurrentSystemMonth && !isPaydayAllocated) {
                val salaryAmt = salaryTx.amount

                val toOperating = min(salaryAmt, livingBufferTarget)
                val remainingAfterOperating = max(0.0, salaryAmt - toOperating)

                val toCommitments = min(remainingAfterOperating, totalCommitmentsShortfall)
                val remainingAfterCommitments = max(0.0, remainingAfterOperating - toCommitments)

                val transferredToFortressThisMonth = transactions.filter { tx ->
                    tx.type == TransactionType.TRANSFER &&
                    (tx.transferSubtype == TransferSubtype.WEALTH_ALLOCATION ||
                     tx.toAccountName?.contains("Fortress", ignoreCase = true) == true) &&
                    tx.month == month && tx.year == year
                }.sumOf { it.amount }

                val dynamicFortressTargetSweep = if (profile.fortressSweepThreshold > 0.0) profile.fortressSweepThreshold else (profile.baseMonthlyIncome * 0.1)
                val pendingFortressBase = max(0.0, dynamicFortressTargetSweep - transferredToFortressThisMonth)
                val toFortressBase = min(remainingAfterCommitments, pendingFortressBase)

                val totalOperatingRetained = toOperating + max(0.0, remainingAfterCommitments - toFortressBase)

                PaydayAllocationPlan(
                    salaryAmount = salaryAmt,
                    toOperating = totalOperatingRetained,
                    toCommitments = toCommitments,
                    toFortressBase = toFortressBase,
                    toFortressSurplus = 0.0,
                    totalToFortress = toFortressBase,
                    commitmentsShortfallCovered = toCommitments,
                    remainingCommitmentsShortfall = max(0.0, totalCommitmentsShortfall - toCommitments),
                    isShortfallFullyFunded = toCommitments >= totalCommitmentsShortfall,
                    isOperatingFullyFunded = toOperating >= livingBufferTarget,
                    pendingBillsCount = fixedBills.count { !it.isPaid && it.type != TransactionType.INCOME }
                )
            } else null

            val isMonthEndWindow = currentDayOfMonth >= 28 && isCurrentSystemMonth

            val isOperatingOrCashAccount = { acc: AccountBalanceResult ->
                acc.accountType.equals("Operating", ignoreCase = true) ||
                acc.accountType.equals("Cash", ignoreCase = true) ||
                acc.accountName.contains("OPERATING", ignoreCase = true) ||
                acc.accountName.contains("PRIMARY", ignoreCase = true) ||
                acc.accountName.contains("CASH", ignoreCase = true)
            }

            val operatingAccountsList = activeAccounts.filter(isOperatingOrCashAccount)

            val operatingLiquidCash = operatingAccountsList.sumOf {
                it.currentBalance - it.minBalance
            }.coerceAtLeast(0.0)

            val unpaidOperatingBills = fixedBills.filter { bill ->
                !bill.isPaid &&
                operatingAccountsList.any { it.accountName.equals(bill.accountName, ignoreCase = true) }
            }.sumOf { it.amount }

            val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            val maxDaysInNextMonth = nextMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val targetNextPayday = min(dynamicSalaryDay, maxDaysInNextMonth)

            val daysLeftInCurrentMonth = max(0, totalDaysInCurrentMonth - currentDayOfMonth)
            val runwayDays = daysLeftInCurrentMonth + targetNextPayday
            val dailyBurn = livingBufferTarget / 30.0
            val runwayBuffer = runwayDays * dailyBurn

            val sweepableSurplus = (operatingLiquidCash - unpaidOperatingBills - excessAdvanceHeld - runwayBuffer).coerceAtLeast(0.0)

            val isMonthEndSweepDone = transactions.any { tx ->
                tx.type == TransactionType.TRANSFER &&
                tx.transferSubtype == TransferSubtype.WEALTH_ALLOCATION &&
                tx.title.contains("Month-End", ignoreCase = true) &&
                tx.month == month && tx.year == year
            }

            val monthEndSweepSuggestion = if (isMonthEndWindow && is3VaultMode && sweepableSurplus > 50.0 && !isMonthEndSweepDone) {
                MonthEndSweepPlan(
                    availableOperatingCash = operatingLiquidCash,
                    runwayBuffer = runwayBuffer,
                    runwayDays = runwayDays,
                    targetNextPayday = targetNextPayday,
                    sweepAmount = sweepableSurplus
                )
            } else null

            // =========================================================================
            // 11. FORTRESS DUAL-TARGET ENGINE
            // =========================================================================
            val fortressVaultAccount = allAccounts.find { isFortressAccount(it) }
            val fortressTotalBalance = fortressVaultAccount?.currentBalance ?: 0.0
            val currentFdReserve = max(0.0, fortressTotalBalance - profile.fortressSweepThreshold)

            val computedFortressTarget = if (profile.fortressManualTarget > 0.0) {
                profile.fortressManualTarget
            } else {
                val months = if (profile.fortressEmergencyMonths > 0) profile.fortressEmergencyMonths else 6
                historicalAvgSpend * months
            }

            val fortressProgress = if (computedFortressTarget > 0.0) {
                ((currentFdReserve / computedFortressTarget) * 100).toInt().coerceIn(0, 100)
            } else 0

            val filtered = transactions.filter { tx ->
                val matchesQuery = filter.query.isBlank() ||
                        tx.title.contains(filter.query, ignoreCase = true) ||
                        tx.category.contains(filter.query, ignoreCase = true) ||
                        tx.subcategory.contains(filter.query, ignoreCase = true)
                val matchesType = filter.type == null || tx.type == filter.type
                val matchesAccount = filter.account == "ALL" || tx.accountName == filter.account || tx.toAccountName == filter.account
                val matchesDate = (filter.startDate == null || tx.date >= filter.startDate) &&
                        (filter.endDate == null || tx.date <= filter.endDate)

                matchesQuery && matchesType && matchesAccount && matchesDate
            }

            val grouped = filtered.groupBy { formatDateHeader(it.date) }

            MonthlyUiState(
                selectedMonth = month,
                selectedYear = year,
                metrics = DashboardMetrics(
                    plannedIncome = plannedIncome,
                    actualIncome = actualIncome,
                    plannedExpenses = plannedExpenses,
                    actualExpenses = actualExpenses,
                    plannedAssets = plannedAssets,
                    actualAssets = actualAssets,
                    fixedCommitmentsTotal = fixedExpenseTotal,
                    safeToSpend = option2SafeToSpend,
                    theoreticalSafeToSpend = theoreticalSafeToSpend,
                    liquidOperatingCash = (totalLiquidAboveMab - pendingFixedBills - excessAdvanceHeld).coerceAtLeast(0.0),
                    safeToSpendPercentage = safeToSpendPercentage,
                    netSavedAfterInvest = netSaved,
                    totalVaultBalance = totalVault,
                    isOverBudget = isOverBudget,
                    dailyExpensePoints = dailyPoints,
                    workExpenses = workExpenses,
                    corporateReimbursements = corporateReimbursements,
                    pendingReimbursement = cumulativePending,
                    lifestyleExpenses = lifestyleExpenses,
                    personalIncome = personalIncome,
                    daysUntilPayday = daysUntilUpcomingSalary,
                    nextPaydayDay = dynamicSalaryDay,
                    isSalaryDelayed = isSalaryDelayed
                ),
                accounts = allAccounts,
                activeAccounts = activeAccounts,
                archivedAccounts = archivedAccounts,
                fixedBills = fixedBills,
                categories = matrixList,
                masterCategories = sortedMasterCats,
                masterSubcategories = sortedMasterSubcats,
                groupedTransactions = grouped,
                budgetPlans = plans,
                commitmentsShortfall = shortfallStatus,
                paydaySuggestion = paydaySuggestion,
                monthEndSweepSuggestion = monthEndSweepSuggestion,
                reimbursementStatus = monthReimbursementStatus,
                frequentCategories = sortedMasterCats,
                frequentSubcategories = sortedMasterSubcats,
                frequentAccounts = frequentAccounts,
                isRolloverBannerVisible = isBannerVisible,
                rolloverBannerMessage = bannerMsg,
                isTaxonomyGracePeriodActive = isGracePeriodActive,
                isTaxonomyBannerVisible = isTaxonomyBannerVisible,
                fortressTarget = computedFortressTarget,
                fortressFdBalance = currentFdReserve,
                fortressProgressPercentage = fortressProgress
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyUiState())

    val yearlyUiState: StateFlow<YearlyUiState> = combine(
        currentYear,
        dao.getAccountBalances(),
        userProfile
    ) { year, accounts, profile ->
        Triple(year, accounts, profile)
    }.flatMapLatest { (year, allAccounts, profile) ->
        combine(
            dao.getYearlySummary(year),
            dao.getYearlyCategoryBreakdown(year)
        ) { rollups, categoryRollups ->
            val allTransactions = withContext(Dispatchers.IO) {
                try { dao.getAllTransactions() } catch (_: Exception) { emptyList() }
            }

            val txCal = Calendar.getInstance()
            val nowCal = Calendar.getInstance()
            val thisYear = nowCal.get(Calendar.YEAR)
            val thisMonth = nowCal.get(Calendar.MONTH) + 1

            val isLoanRepayment = { tx: TransactionEntity ->
                tx.type == TransactionType.INCOME &&
                (tx.subcategory.contains("Loan Paybacks Received", ignoreCase = true) ||
                 tx.title.contains("Loan Payback", ignoreCase = true))
            }

            val isTaxOrPurchaseRefund = { tx: TransactionEntity ->
                tx.type == TransactionType.INCOME &&
                (tx.subcategory.contains("Tax & Purchase Refunds", ignoreCase = true) ||
                 tx.title.contains("Refund", ignoreCase = true))
            }

            val isCapitalDrawdown = { tx: TransactionEntity ->
                tx.type == TransactionType.INCOME &&
                (tx.category.equals("Passive & Capital Drawdowns", ignoreCase = true) ||
                 tx.subcategory.contains("Capital Gains", ignoreCase = true) ||
                 tx.subcategory.contains("Realization", ignoreCase = true) ||
                 tx.subcategory.contains("Emergency Fund Drawdown", ignoreCase = true) ||
                 tx.subcategory.contains("FD / Deposit Maturity", ignoreCase = true))
            }

            val isLoanGiven = { tx: TransactionEntity ->
                tx.type == TransactionType.ASSET &&
                (tx.subcategory.contains("Personal Loans", ignoreCase = true) ||
                 tx.subcategory.contains("Loaned", ignoreCase = true))
            }

            val isNpaWriteOff = { tx: TransactionEntity ->
                tx.type == TransactionType.ASSET &&
                (tx.subcategory.contains("NPA", ignoreCase = true) ||
                 tx.subcategory.contains("Bad Debt", ignoreCase = true) ||
                 tx.category.equals("NPA", ignoreCase = true))
            }

            val isGenuineSavingsOrAsset = { tx: TransactionEntity ->
                tx.type == TransactionType.ASSET && !isLoanGiven(tx) && !isNpaWriteOff(tx)
            }

            val allYearTransactions = allTransactions.filter { tx ->
                txCal.timeInMillis = tx.date
                txCal.get(Calendar.YEAR) == year && tx.type != TransactionType.TRANSFER
            }

            val yearlyMonths = (1..12).map { m ->
                val isFutureMonth = (year == thisYear && m > thisMonth) || (year > thisYear)
                val monthTxs = allYearTransactions.filter { tx ->
                    txCal.timeInMillis = tx.date
                    (txCal.get(Calendar.MONTH) + 1) == m
                }

                val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val ast = monthTxs.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }

                val fixedExp = monthTxs.filter { it.type == TransactionType.EXPENSE && it.linkedFixedBillId != null }.sumOf { it.amount }
                val varExp = exp - fixedExp
                val workExp = monthTxs.filter { it.type == TransactionType.CORPORATE && !it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
                val corpReimb = monthTxs.filter { it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
                val lifestyleExp = exp

                val monthNonPersonalInflows = monthTxs.filter {
                    isLoanRepayment(it) || isTaxOrPurchaseRefund(it) || isCapitalDrawdown(it)
                }.sumOf { it.amount }
                val monthPersonalIncome = (inc - monthNonPersonalInflows).coerceAtLeast(0.0)
                val monthGenuineAssets = monthTxs.filter(isGenuineSavingsOrAsset).sumOf { it.amount }

                val truePersonalNetSavings = monthPersonalIncome - lifestyleExp - monthGenuineAssets

                YearlyMonthData(
                    monthIndex = m,
                    monthName = MONTH_NAMES[m - 1],
                    income = inc,
                    expenses = exp,
                    assets = ast,
                    lifestyleExpenses = lifestyleExp,
                    workExpenses = workExp,
                    corporateReimbursements = corpReimb,
                    netSavings = truePersonalNetSavings,
                    fixedExpenses = fixedExp,
                    variableExpenses = varExp,
                    isFuture = isFutureMonth,
                    transactions = monthTxs
                )
            }

            val totalIncome = rollups.sumOf { it.totalActualIncome }
            val totalExpense = rollups.sumOf { it.totalActualExpense }
            val totalAssets = rollups.sumOf { it.totalAsset }

            val allTxYears = allTransactions.mapNotNull { tx ->
                txCal.timeInMillis = tx.date
                txCal.get(Calendar.YEAR)
            }.distinct()

            val minYearInHistory = allTxYears.minOrNull() ?: (year - 2)
            val startYear = min(minYearInHistory, year - 2)

            val annualNetAssetMap = mutableMapOf<Int, Double>()
            for (y in startYear..year) {
                annualNetAssetMap[y] = 0.0
            }

            allTransactions.forEach { tx ->
                txCal.timeInMillis = tx.date
                val txYear = txCal.get(Calendar.YEAR)
                if (txYear in startYear..year) {
                    if (isGenuineSavingsOrAsset(tx)) {
                        annualNetAssetMap[txYear] = (annualNetAssetMap[txYear] ?: 0.0) + tx.amount
                    } else if (isCapitalDrawdown(tx)) {
                        annualNetAssetMap[txYear] = (annualNetAssetMap[txYear] ?: 0.0) - tx.amount
                    }
                }
            }

            var runningCumulativeAssets = 0.0
            val cumulativeAssetMap = sortedMapOf<Int, Double>()
            annualNetAssetMap.toSortedMap().forEach { (y, netAmt) ->
                runningCumulativeAssets = (runningCumulativeAssets + netAmt).coerceAtLeast(0.0)
                cumulativeAssetMap[y] = runningCumulativeAssets
            }

            val displayYears = ((year - 2)..year).toList()
            val multiYearAssetList = displayYears.map { y ->
                val currentCum = cumulativeAssetMap[y] ?: 0.0
                val prevCum = cumulativeAssetMap[y - 1] ?: 0.0
                val growth = if (prevCum > 0.0) {
                    ((currentCum - prevCum) / prevCum) * 100.0
                } else 0.0
                MultiYearAssetMetric(year = y, totalAssets = currentCum, growthPercent = growth)
            }

            val allTimeInvestmentsInflow = allTransactions.filter {
                it.type == TransactionType.ASSET &&
                it.category.equals("Investments & Wealth", ignoreCase = true)
            }.sumOf { it.amount }

            val allTimeCapitalDrawdowns = allTransactions.filter(isCapitalDrawdown).sumOf { it.amount }
            val totalActiveInvestments = (allTimeInvestmentsInflow - allTimeCapitalDrawdowns).coerceAtLeast(0.0)

            val activeLoanedReceivables = allTransactions.filter(isLoanGiven).sumOf { it.amount }
            val npaWrittenOff = allTransactions.filter(isNpaWriteOff).sumOf { it.amount }
            val repaymentsReceived = allTransactions.filter(isLoanRepayment).sumOf { it.amount }

            val effectiveReceivables = (activeLoanedReceivables - repaymentsReceived - npaWrittenOff).coerceAtLeast(0.0)
            val liquidReserves = allAccounts.filter { !it.isArchived }.sumOf { it.currentBalance }

            val grossWealth = liquidReserves + totalActiveInvestments + effectiveReceivables + npaWrittenOff
            val realizableNetWorth = liquidReserves + totalActiveInvestments + effectiveReceivables

            val wealthMetrics = AssetWealthMetrics(
                grossWealth = grossWealth,
                totalInvestments = totalActiveInvestments,
                liquidReserves = liquidReserves,
                activeReceivables = effectiveReceivables,
                npaWrittenOff = npaWrittenOff,
                realizableNetWorth = realizableNetWorth
            )

            val annualWorkExpenses = allYearTransactions.filter { it.type == TransactionType.CORPORATE && !it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
            val annualReimbursements = allYearTransactions.filter { it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
            val annualLifestyleExpenses = totalExpense

            val annualNonPersonalInflows = allYearTransactions.filter {
                isLoanRepayment(it) || isTaxOrPurchaseRefund(it) || isCapitalDrawdown(it)
            }.sumOf { it.amount }
            val annualPersonalIncome = (totalIncome - annualNonPersonalInflows).coerceAtLeast(0.0)
            val annualGenuineAssets = allYearTransactions.filter(isGenuineSavingsOrAsset).sumOf { it.amount }

            val annualNetSurplus = annualPersonalIncome - annualLifestyleExpenses - annualGenuineAssets

            val allTimeWork = allTransactions.filter { it.type == TransactionType.CORPORATE && !it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }
            val allTimeReimb = allTransactions.filter { it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true) }.sumOf { it.amount }

            val effectiveAllTimeWork = allTimeWork + profile.initialReimbursementClaim
            val effectiveAllTimeReimb = allTimeReimb + profile.initialCompanyAdvance
            val allTimePending = (effectiveAllTimeWork - effectiveAllTimeReimb).coerceAtLeast(0.0)
            val allTimeExcessAdvance = (effectiveAllTimeReimb - effectiveAllTimeWork).coerceAtLeast(0.0)

            val annualReimbursementStatus = ReimbursementStatus(
                totalWorkExpenses = annualWorkExpenses,
                totalClaimsReceived = annualReimbursements,
                pendingReimbursement = allTimePending,
                isSettled = allTimePending <= 0.0 && allTimeExcessAdvance <= 0.0,
                cumulativeWorkExpenses = effectiveAllTimeWork,
                cumulativeClaimsReceived = effectiveAllTimeReimb,
                excessAdvanceHeld = allTimeExcessAdvance
            )

            YearlyUiState(
                selectedYear = year,
                monthlyRollups = rollups,
                categoryRollups = categoryRollups,
                totalYearlyIncome = totalIncome,
                totalYearlyExpense = totalExpense,
                totalYearlyAssets = totalAssets,
                annualNetSurplus = annualNetSurplus,
                yearlyMonths = yearlyMonths,
                multiYearAssets = multiYearAssetList,
                assetWealthMetrics = wealthMetrics,
                reimbursementStatus = annualReimbursementStatus,
                annualLifestyleExpenses = annualLifestyleExpenses,
                annualPersonalIncome = annualPersonalIncome,
                allYearTransactions = allYearTransactions
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearlyUiState())

    val averageMonthlySpend: StateFlow<Double> = combine(yearlyUiState, monthlyUiState) { yearly, monthly ->
        val completedHistoricalMonths = yearly.yearlyMonthsData.filter { !it.isFuture && it.lifestyleExpenses > 0 }

        if (completedHistoricalMonths.isNotEmpty()) {
            completedHistoricalMonths.map { it.lifestyleExpenses }.average()
        } else {
            val curActual = monthly.metrics.lifestyleExpenses
            val planned = monthly.metrics.plannedExpenses
            val fixed = monthly.metrics.fixedCommitmentsTotal
            max(curActual, max(planned, fixed))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun selectMonth(month: Int) {
        currentMonth.value = month
        viewModelScope.launch(Dispatchers.IO) {
            checkAndRolloverRecurringBills(month, currentYear.value)
        }
    }

    fun selectYear(year: Int) {
        currentYear.value = year
        viewModelScope.launch(Dispatchers.IO) {
            checkAndRolloverRecurringBills(currentMonth.value, year)
        }
    }

    fun setSelectedMonth(month: Int, year: Int) {
        currentMonth.value = month
        currentYear.value = year
        viewModelScope.launch(Dispatchers.IO) {
            checkAndRolloverRecurringBills(month, year)
        }
    }

    fun setYearlySelectedYear(year: Int) {
        selectYear(year)
    }

    fun updateSearchQuery(query: String) { filterCriteria.value = filterCriteria.value.copy(query = query) }
    fun updateFilter(type: TransactionType?, account: String, start: Long?, end: Long?) {
        filterCriteria.value = filterCriteria.value.copy(type = type, account = account, startDate = start, endDate = end)
    }
    fun resetFilters() { filterCriteria.value = FilterCriteria() }

    fun unlockApp() { isAppUnlocked.value = true }
    fun lockApp() { isAppUnlocked.value = false }

    fun dismissRolloverBanner() {
        showRolloverBanner.value = false
    }

    fun dismissTaxonomyBanner() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.dismissTaxonomyBanner()
        }
    }

    fun checkAndExecuteMonthEndAutoRollover() {
        viewModelScope.launch(Dispatchers.IO) {
            val nowCal = Calendar.getInstance()
            val day = nowCal.get(Calendar.DAY_OF_MONTH)
            val maxDayInMonth = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            if (day >= 28 || day == maxDayInMonth) {
                val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                val nMonth = nextMonthCal.get(Calendar.MONTH) + 1
                val nYear = nextMonthCal.get(Calendar.YEAR)
                val count = dao.getFixedBillCount(nMonth, nYear)

                if (count == 0) {
                    val clonedCount = checkAndRolloverRecurringBills(nMonth, nYear)
                    if (clonedCount > 0) {
                        rolloverBannerMessage.value = "${MONTH_NAMES[nMonth - 1]} recurring bills ($clonedCount) automatically scheduled."
                        showRolloverBanner.value = true
                    }
                }
            }
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveUserProfile(profile.copy(id = 1))
        }
    }

    fun finalizeOnboardingProfile(
        displayName: String,
        email: String,
        dob: String,
        currencySymbol: String,
        vaultMode: String,
        masterPin: String,
        isBiometricEnabled: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            securityManager.setPin(masterPin)
            if (dob.isNotBlank()) {
                securityManager.setRecoveryDob(dob)
            }
            val profile = UserProfile(
                id = 1,
                displayName = displayName.trim().ifEmpty { "Vault User" },
                email = email.trim(),
                dateOfBirth = dob.trim(),
                currencySymbol = currencySymbol,
                vaultMode = vaultMode,
                isBiometricEnabled = isBiometricEnabled,
                isScreenCaptureAllowed = false,
                isOnboardingCompleted = true
            )
            dao.saveUserProfile(profile)
            isAppUnlocked.value = true
        }
    }

    fun updateVaultMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, vaultMode = mode))
        }
    }

    fun setMasterPin(pin: String, recoveryDob: String) {
        securityManager.setPin(pin)
        securityManager.setRecoveryDob(recoveryDob)
    }

    fun savePin(pin: String) {
        securityManager.setPin(pin)
    }

    fun saveMasterPin(pin: String) {
        securityManager.setPin(pin)
    }

    fun updateProfileImageUri(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, profileImageUri = uriString))
        }
    }

    fun updateCoverImageUri(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, coverImageUri = uriString))
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, displayName = name))
        }
    }

    fun updateEmail(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, email = email.trim()))
        }
    }

    fun updateDateOfBirth(dob: String) {
        viewModelScope.launch(Dispatchers.IO) {
            securityManager.setRecoveryDob(dob)
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, dateOfBirth = dob.trim()))
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, currencySymbol = symbol))
        }
    }

    fun updateFortressSweepThreshold(threshold: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, fortressSweepThreshold = threshold))
        }
    }

    fun updateFortressEmergencyMonths(months: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, fortressEmergencyMonths = months))
        }
    }

    fun updateFortressManualTarget(target: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, fortressManualTarget = target))
        }
    }

    fun updateFortressThreshold(newThreshold: Double) {
        updateFortressSweepThreshold(newThreshold)
    }

    fun updateOpeningCorporateFloat(initialClaim: Double, initialAdvance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(
                current.copy(
                    id = 1,
                    initialReimbursementClaim = initialClaim.coerceAtLeast(0.0),
                    initialCompanyAdvance = initialAdvance.coerceAtLeast(0.0)
                )
            )
        }
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, isBiometricEnabled = enabled))
        }
    }

    fun updateScreenCaptureAllowed(allowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(id = 1, isScreenCaptureAllowed = allowed))
        }
    }

    fun updateReminderSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(
                current.copy(
                    id = 1,
                    isAutoPayReminderEnabled = enabled,
                    reminderEnabled = enabled,
                    reminderHour = hour,
                    reminderMinute = minute
                )
            )
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(context.applicationContext, hour, minute)
            } else {
                ReminderScheduler.cancelReminder(context.applicationContext)
            }
        }
    }

    fun completeOnboarding(
        displayName: String,
        dob: String,
        baseIncome: Double,
        fortressSweepThreshold: Double = 0.0,
        fortressEmergencyMonths: Int = 6,
        masterPin: String,
        isBiometricEnabled: Boolean,
        vaultMode: String = "3-VAULT"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            securityManager.setPin(masterPin)
            securityManager.setRecoveryDob(dob)
            val profile = UserProfile(
                id = 1,
                displayName = displayName,
                dateOfBirth = dob,
                baseMonthlyIncome = baseIncome,
                fortressSweepThreshold = fortressSweepThreshold,
                fortressEmergencyMonths = fortressEmergencyMonths,
                isOnboardingCompleted = true,
                isBiometricEnabled = isBiometricEnabled,
                vaultMode = vaultMode
            )
            dao.saveUserProfile(profile)
            isAppUnlocked.value = true
        }
    }

    fun replaceAllAccounts(accounts: List<AccountEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllAccounts()
            dao.insertAccounts(accounts)
        }
    }

    fun adjustAccountBalance(accountName: String, targetBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAccountByName(accountName)
            val balances = dao.getAccountBalances().first()
            val currentBal = balances.find { it.accountName.equals(accountName, ignoreCase = true) }?.currentBalance ?: 0.0
            val diff = targetBalance - currentBal
            if (diff != 0.0) {
                val txType = if (diff > 0.0) TransactionType.INCOME else TransactionType.EXPENSE
                val cal = Calendar.getInstance()
                dao.insertTransaction(
                    TransactionEntity(
                        title = "Balance Adjustment",
                        amount = abs(diff),
                        category = "General",
                        subcategory = existing?.accountType ?: "Adjustment",
                        accountName = accountName,
                        type = txType,
                        date = System.currentTimeMillis(),
                        month = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR)
                    )
                )
            }
        }
    }

    fun restoreVaultFromUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = restoreFromJsonUri(context, uri, wipeExisting = true)
            withContext(Dispatchers.Main) {
                if (success) {
                    onResult(true, "Vault restored successfully.")
                } else {
                    onResult(false, "Failed to restore backup snapshot.")
                }
            }
        }
    }

    fun backupVaultToEncryptedJson(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = exportJsonBackupToUri(context, uri)
            withContext(Dispatchers.Main) {
                onResult(success, if (success) "Full encrypted backup saved!" else "Backup failed")
            }
        }
    }

    fun restoreVaultFromEncryptedJson(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        restoreVaultFromUri(context, uri, onResult)
    }

    suspend fun exportCsvToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val transactions = dao.getAllTransactions()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val builder = StringBuilder()

            builder.append('\uFEFF')
            builder.append("Date,Title,Flow Type,Category,Subcategory,Amount,Source Vault,Destination Vault\n")

            transactions.forEach { tx ->
                val escapedTitle = tx.title.replace("\"", "\"\"")
                val escapedCat = tx.category.replace("\"", "\"\"")
                val escapedSub = tx.subcategory.replace("\"", "\"\"")
                val dateStr = dateFormat.format(Date(tx.date))
                val toAcc = tx.toAccountName ?: ""

                builder.append("\"$dateStr\",")
                builder.append("\"$escapedTitle\",")
                builder.append("\"${tx.type.name}\",")
                builder.append("\"$escapedCat\",")
                builder.append("\"$escapedSub\",")
                builder.append("${tx.amount},")
                builder.append("\"${tx.accountName}\",")
                builder.append("\"$toAcc\"\n")
            }

            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(builder.toString().toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun seedFullExcelTaxonomyIfEmpty() = withContext(Dispatchers.IO) {
        val existingCats = dao.getAllCategoriesDirect()
        if (existingCats.isEmpty()) {
            dao.insertCategories(CategoryEntity.defaultCategories)
            dao.insertSubcategories(SubcategoryEntity.defaultSubcategories)
        }
    }

    suspend fun seedDefaultAccountsIfEmpty() = withContext(Dispatchers.IO) {
        val isCompleted = dao.getUserProfileDirect()?.isOnboardingCompleted ?: false
        val count = dao.getAccountCount()
        if (count == 0 && isCompleted) {
            dao.insertAccounts(
                listOf(
                    AccountEntity(accountName = "PRIMARY BANK", startingBalance = 0.0, accountType = "Operating", minBalance = 0.0, sortOrder = 0),
                    AccountEntity(accountName = "SECONDARY BANK", startingBalance = 0.0, accountType = "Commitments", minBalance = 0.0, sortOrder = 1),
                    AccountEntity(accountName = "TERTIARY BANK", startingBalance = 0.0, accountType = "Fortress", minBalance = 0.0, sortOrder = 2),
                    AccountEntity(accountName = "CASH WALLET", startingBalance = 0.0, accountType = "Cash", minBalance = 0.0, sortOrder = 3)
                )
            )
        }
    }

    fun resetEntireVault(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllTransactions()
            dao.clearAllFixedBills()
            dao.clearAllBudgetPlans()
            dao.clearAllAccounts()
            dao.clearUserProfile()
            dao.clearAllCategories()
            dao.clearAllSubcategories()
            seedFullExcelTaxonomyIfEmpty()
            seedDefaultAccountsIfEmpty()
            securityManager.clearAll()
            isAppUnlocked.value = false
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun copyPreviousMonthBudget(onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val prevMonth = if (currentMonth.value == 1) 12 else currentMonth.value - 1
            val prevYear = if (currentMonth.value == 1) currentYear.value - 1 else currentYear.value
            val previousPlans = dao.getBudgetPlansForMonthDirect(prevMonth, prevYear)

            val activeCategories = dao.getAllCategoriesDirect().filter { !it.isLegacy }.map { it.name }.toSet()

            val clonedPlans = previousPlans.filter { activeCategories.contains(it.category) }.map { plan ->
                BudgetPlanEntity(
                    category = plan.category,
                    plannedAmount = plan.plannedAmount,
                    type = plan.type,
                    month = currentMonth.value,
                    year = currentYear.value
                )
            }
            dao.insertBudgetPlans(clonedPlans)
            withContext(Dispatchers.Main) {
                onComplete(clonedPlans.size)
            }
        }
    }

    fun updateCategoryBudget(category: String, amount: Double, type: TransactionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPlans = dao.getBudgetPlansForMonthDirect(currentMonth.value, currentYear.value)
            val existing = currentPlans.find { it.category == category && it.type == type }
            if (existing != null) {
                dao.insertBudgetPlan(existing.copy(plannedAmount = amount))
            } else {
                dao.insertBudgetPlan(
                    BudgetPlanEntity(
                        category = category,
                        plannedAmount = amount,
                        type = type,
                        month = currentMonth.value,
                        year = currentYear.value
                    )
                )
            }
        }
    }

    private suspend fun findMatchingUnpaidBill(
        txMonth: Int,
        txYear: Int,
        resolvedTitle: String,
        amount: Double,
        category: String,
        subcategory: String,
        accountName: String,
        toAccountName: String?,
        type: TransactionType
    ): FixedBillEntity? {
        if (type == TransactionType.INCOME) return null

        val unpaidBills = dao.getFixedBillsForMonthDirect(txMonth, txYear).filter { !it.isPaid }

        val candidates = unpaidBills.filter { bill ->
            if (bill.type != type) return@filter false

            if (type == TransactionType.TRANSFER) {
                val matchesSubtype = bill.subcategory.equals(subcategory, ignoreCase = true)
                val matchesDestination = !toAccountName.isNullOrBlank() &&
                        !bill.toAccountName.isNullOrBlank() &&
                        bill.toAccountName.equals(toAccountName, ignoreCase = true)
                val matchesTitle = bill.title.isNotBlank() && (
                        resolvedTitle.contains(bill.title, ignoreCase = true) ||
                        bill.title.contains(resolvedTitle, ignoreCase = true)
                )
                matchesSubtype && (matchesDestination || matchesTitle)
            } else {
                bill.category.equals(category, ignoreCase = true) &&
                bill.subcategory.equals(subcategory, ignoreCase = true)
            }
        }

        return candidates.map { bill ->
            var score = 0
            val billTitleClean = bill.title.trim()
            val hasDistinctBillTitle = billTitleClean.isNotBlank() && !billTitleClean.equals(bill.subcategory, ignoreCase = true)
            val hasDistinctTxTitle = resolvedTitle.isNotBlank() && !resolvedTitle.equals(subcategory, ignoreCase = true)

            if (hasDistinctBillTitle && hasDistinctTxTitle) {
                if (resolvedTitle.equals(billTitleClean, ignoreCase = true)) {
                    score += 100
                } else if (resolvedTitle.contains(billTitleClean, ignoreCase = true) || billTitleClean.contains(resolvedTitle, ignoreCase = true)) {
                    score += 50
                }
            }

            if (bill.accountName.equals(accountName, ignoreCase = true)) {
                score += 20
            }
            if (type == TransactionType.TRANSFER && !toAccountName.isNullOrBlank() && toAccountName.equals(bill.toAccountName, ignoreCase = true)) {
                score += 20
            }

            val diff = abs(bill.amount - amount)
            if (diff < 0.01) {
                score += 30
            } else if (diff <= bill.amount * 0.1) {
                score += 10
            }

            bill to score
        }.filter { it.second >= 50 }
         .maxByOrNull { it.second }
         ?.first
    }

    fun saveTransaction(
        id: Long = 0,
        title: String,
        amount: Double,
        category: String,
        subcategory: String,
        accountName: String,
        type: TransactionType,
        date: Long = System.currentTimeMillis(),
        toAccountName: String? = null,
        transferSubtype: TransferSubtype = TransferSubtype.NONE
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanTitle = title.trim()
            val cleanSubcat = subcategory.trim()
            val resolvedTitle = if (cleanTitle.isBlank()) cleanSubcat else cleanTitle

            val calTx = Calendar.getInstance().apply { timeInMillis = date }
            val txMonth = calTx.get(Calendar.MONTH) + 1
            val txYear = calTx.get(Calendar.YEAR)

            var resolvedLinkedBillId: Long? = null

            if (id == 0L) {
                val matchingBill = findMatchingUnpaidBill(
                    txMonth, txYear, resolvedTitle, amount, category, subcategory, accountName, toAccountName, type
                )
                if (matchingBill != null) {
                    resolvedLinkedBillId = matchingBill.id
                    dao.updateFixedBill(matchingBill.copy(isPaid = true, amount = amount))
                }
            } else {
                val existingTx = dao.getTransactionById(id)
                resolvedLinkedBillId = existingTx?.linkedFixedBillId

                if (resolvedLinkedBillId != null) {
                    val linkedBill = dao.getFixedBillById(resolvedLinkedBillId)
                    if (linkedBill != null) {
                        val stillMatches = if (type == TransactionType.TRANSFER) {
                            linkedBill.type == TransactionType.TRANSFER &&
                            linkedBill.subcategory.equals(subcategory, ignoreCase = true)
                        } else {
                            linkedBill.type == type &&
                            linkedBill.category.equals(category, ignoreCase = true)
                        }

                        if (stillMatches) {
                            dao.updateFixedBill(
                                linkedBill.copy(
                                    title = resolvedTitle,
                                    amount = amount
                                )
                            )
                        } else {
                            dao.updateFixedBill(linkedBill.copy(isPaid = false))
                            resolvedLinkedBillId = null

                            val newMatchingBill = findMatchingUnpaidBill(
                                txMonth, txYear, resolvedTitle, amount, category, subcategory, accountName, toAccountName, type
                            )
                            if (newMatchingBill != null) {
                                resolvedLinkedBillId = newMatchingBill.id
                                dao.updateFixedBill(newMatchingBill.copy(isPaid = true, amount = amount))
                            }
                        }
                    }
                } else if (type != TransactionType.INCOME) {
                    val matchingBill = findMatchingUnpaidBill(
                        txMonth, txYear, resolvedTitle, amount, category, subcategory, accountName, toAccountName, type
                    )
                    if (matchingBill != null) {
                        resolvedLinkedBillId = matchingBill.id
                        dao.updateFixedBill(matchingBill.copy(isPaid = true, amount = amount))
                    }
                }
            }

            val entity = TransactionEntity(
                id = id,
                title = resolvedTitle,
                amount = amount,
                category = category,
                subcategory = subcategory,
                accountName = accountName,
                toAccountName = toAccountName,
                type = type,
                date = date,
                month = txMonth,
                year = txYear,
                linkedFixedBillId = resolvedLinkedBillId,
                transferSubtype = transferSubtype
            )
            if (id == 0L) dao.insertTransaction(entity) else dao.updateTransaction(entity)
        }
    }

    fun executeInstantTransfer(
        fromAccount: String,
        toAccount: String,
        amount: Double,
        note: String = "",
        subtype: TransferSubtype = TransferSubtype.NONE,
        date: Long = System.currentTimeMillis()
    ) {
        val resolvedNote = if (note.isBlank()) "Vault Transfer ($fromAccount ➔ $toAccount)" else note.trim()
        saveTransaction(
            title = resolvedNote,
            amount = amount,
            category = "Transfer",
            subcategory = subtype.name,
            accountName = fromAccount,
            toAccountName = toAccount,
            type = TransactionType.TRANSFER,
            date = date,
            transferSubtype = subtype
        )
    }

    fun applyPaydayAllocation(
        plan: PaydayAllocationPlan,
        operatingAccount: String,
        commitmentsAccount: String,
        fortressAccount: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (plan.toCommitments > 0.0) {
                executeInstantTransfer(
                    fromAccount = operatingAccount,
                    toAccount = commitmentsAccount,
                    amount = plan.toCommitments,
                    note = "Payday Allocation ➔ Commitments Bill Funding",
                    subtype = TransferSubtype.BILL_FUNDING
                )
            }
            if (plan.totalToFortress > 0.0) {
                executeInstantTransfer(
                    fromAccount = operatingAccount,
                    toAccount = fortressAccount,
                    amount = plan.totalToFortress,
                    note = "Payday Allocation ➔ Fortress Emergency Base",
                    subtype = TransferSubtype.WEALTH_ALLOCATION
                )
            }
        }
    }

    fun applyMonthEndSweep(
        plan: MonthEndSweepPlan,
        operatingAccount: String,
        fortressAccount: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (plan.sweepAmount > 0.0) {
                executeInstantTransfer(
                    fromAccount = operatingAccount,
                    toAccount = fortressAccount,
                    amount = plan.sweepAmount,
                    note = "Month-End Wealth Sweep ➔ Fortress Extra",
                    subtype = TransferSubtype.WEALTH_ALLOCATION
                )
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTransaction(transaction)
            transaction.linkedFixedBillId?.let { billId ->
                val linkedBill = dao.getFixedBillById(billId)
                if (linkedBill != null) {
                    dao.updateFixedBill(linkedBill.copy(isPaid = false))
                }
            }
        }
    }

    fun addCategory(name: String, type: TransactionType) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertCategory(CategoryEntity(name = name.trim(), type = type, isLegacy = false, isNew = false)) }
    }

    fun updateCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCategoryAndCascade(category, newName)
        }
    }

    fun deleteCategory(category: CategoryEntity, onResult: (Boolean, String) -> Unit) {
        if (!category.isLegacy && protectedCategories.contains(category.name)) {
            onResult(false, "'${category.name}' is an active system default and cannot be deleted.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCategoryAndCascade(category)
            dao.deleteFutureUnpaidFixedBillsByCategory(category.name, currentMonth.value, currentYear.value)
            withContext(Dispatchers.Main) {
                onResult(true, "Category deleted. Historical logs safely preserved.")
            }
        }
    }

    fun addSubcategory(parentCategory: String, name: String, type: TransactionType = TransactionType.EXPENSE) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertSubcategory(SubcategoryEntity(parentCategory = parentCategory, name = name.trim(), type = type, isLegacy = false, isNew = false)) }
    }

    fun updateSubcategory(sub: SubcategoryEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSubcategoryAndCascade(sub, newName)
        }
    }

    fun deleteSubcategory(sub: SubcategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSubcategory(sub)
            dao.deleteFutureUnpaidFixedBillsBySubcategory(sub.parentCategory, sub.name, currentMonth.value, currentYear.value)
        }
    }

    fun addFixedBill(
        title: String,
        amount: Double,
        category: String,
        subcategory: String,
        account: String = "Primary Bank",
        toAccount: String? = null,
        type: TransactionType = TransactionType.EXPENSE,
        dueDay: Int? = null,
        isPaid: Boolean = false,
        paidDateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanTitle = title.trim()
            val cleanSubcat = subcategory.trim()
            val finalTitle = if (cleanTitle.isBlank()) cleanSubcat else cleanTitle

            val calTx = Calendar.getInstance().apply { timeInMillis = paidDateMillis }
            val targetMonth = if (isPaid) calTx.get(Calendar.MONTH) + 1 else currentMonth.value
            val targetYear = if (isPaid) calTx.get(Calendar.YEAR) else currentYear.value

            val bill = FixedBillEntity(
                title = finalTitle,
                amount = amount,
                category = category,
                subcategory = subcategory,
                accountName = account,
                toAccountName = toAccount,
                type = type,
                isPaid = isPaid,
                dueDay = dueDay,
                month = targetMonth,
                year = targetYear
            )
            val insertedId = dao.insertFixedBill(bill)

            if (isPaid) {
                val subtype = if (type == TransactionType.TRANSFER) {
                    try {
                        TransferSubtype.valueOf(subcategory)
                    } catch (_: Exception) {
                        TransferSubtype.BILL_FUNDING
                    }
                } else TransferSubtype.NONE

                dao.insertTransaction(
                    TransactionEntity(
                        title = bill.title.ifBlank { subcategory },
                        amount = amount,
                        category = category,
                        subcategory = subcategory.ifBlank { bill.title },
                        accountName = account,
                        toAccountName = toAccount,
                        type = type,
                        date = paidDateMillis,
                        month = targetMonth,
                        year = targetYear,
                        linkedFixedBillId = insertedId,
                        transferSubtype = subtype
                    )
                )
            }
        }
    }

    fun updateFixedBill(
        id: Long,
        title: String,
        amount: Double,
        category: String,
        subcategory: String,
        account: String,
        toAccount: String?,
        type: TransactionType,
        dueDay: Int?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getFixedBillById(id)
            if (existing != null) {
                val cleanTitle = title.trim()
                val cleanSubcat = subcategory.trim()
                val finalTitle = if (cleanTitle.isBlank()) cleanSubcat else cleanTitle

                val updatedBill = existing.copy(
                    title = finalTitle,
                    amount = amount,
                    category = category,
                    subcategory = subcategory,
                    accountName = account,
                    toAccountName = toAccount,
                    type = type,
                    dueDay = dueDay
                )
                dao.updateFixedBill(updatedBill)

                val linkedTx = dao.getTransactionByLinkedBill(id)
                if (linkedTx != null) {
                    dao.updateTransaction(
                        linkedTx.copy(
                            title = finalTitle.ifBlank { subcategory },
                            amount = amount,
                            category = category,
                            subcategory = subcategory,
                            accountName = account,
                            toAccountName = toAccount,
                            type = type
                        )
                    )
                }
            }
        }
    }

    fun toggleFixedBillPaid(bill: FixedBillEntity, customAmount: Double = bill.amount, customDateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedStatus = !bill.isPaid

            if (updatedStatus) {
                val calTx = Calendar.getInstance().apply { timeInMillis = customDateMillis }
                val txMonth = calTx.get(Calendar.MONTH) + 1
                val txYear = calTx.get(Calendar.YEAR)

                val alignedBill = if (bill.month != txMonth || bill.year != txYear) {
                    val targetMonthBills = dao.getFixedBillsForMonthDirect(txMonth, txYear)
                    val existingInTarget = targetMonthBills.firstOrNull { it.id == bill.id }
                        ?: targetMonthBills.firstOrNull { candidate ->
                            val bTitle = bill.title.trim()
                            val cTitle = candidate.title.trim()
                            val isTitleMatch = bTitle.equals(cTitle, ignoreCase = true) ||
                                ((bTitle.isBlank() || bTitle.equals(bill.subcategory, ignoreCase = true)) &&
                                 (cTitle.isBlank() || cTitle.equals(candidate.subcategory, ignoreCase = true)))

                            candidate.type == bill.type &&
                            candidate.category.equals(bill.category, ignoreCase = true) &&
                            candidate.subcategory.equals(bill.subcategory, ignoreCase = true) &&
                            isTitleMatch &&
                            candidate.accountName.equals(bill.accountName, ignoreCase = true)
                        }

                    if (existingInTarget != null) {
                        existingInTarget.copy(isPaid = true, amount = customAmount)
                    } else {
                        bill.copy(month = txMonth, year = txYear, isPaid = true, amount = customAmount)
                    }
                } else {
                    bill.copy(isPaid = true, amount = customAmount)
                }

                dao.updateFixedBill(alignedBill)

                val subtype = if (alignedBill.type == TransactionType.TRANSFER) {
                    try {
                        TransferSubtype.valueOf(alignedBill.subcategory)
                    } catch (_: Exception) {
                        TransferSubtype.BILL_FUNDING
                    }
                } else TransferSubtype.NONE

                dao.insertTransaction(
                    TransactionEntity(
                        title = alignedBill.title.ifBlank { alignedBill.subcategory },
                        amount = customAmount,
                        category = alignedBill.category,
                        subcategory = alignedBill.subcategory.ifBlank { alignedBill.title },
                        accountName = alignedBill.accountName,
                        toAccountName = alignedBill.toAccountName,
                        type = alignedBill.type,
                        date = customDateMillis,
                        month = txMonth,
                        year = txYear,
                        linkedFixedBillId = alignedBill.id,
                        transferSubtype = subtype
                    )
                )
            } else {
                dao.updateFixedBill(bill.copy(isPaid = false))
                dao.deleteTransactionByLinkedBill(bill.id)
            }
        }
    }

    fun deleteFixedBill(bill: FixedBillEntity, cascadeFuture: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteFixedBill(bill)
            dao.deleteTransactionByLinkedBill(bill.id)

            if (cascadeFuture) {
                val sig = getBillSignature(bill)
                val allBills = dao.getAllFixedBills()
                val futureBills = allBills.filter { candidate ->
                    !candidate.isPaid &&
                    (candidate.year > bill.year || (candidate.year == bill.year && candidate.month > bill.month)) &&
                    getBillSignature(candidate) == sig
                }
                futureBills.forEach { futureBill ->
                    dao.deleteFixedBill(futureBill)
                    dao.deleteTransactionByLinkedBill(futureBill.id)
                }
            }
        }
    }

    fun addAccount(
        name: String,
        startingBalance: Double,
        type: String = "Operating",
        minBalance: Double = 0.0,
        sortOrder: Int = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingAccounts = dao.getAllAccountsDirect()
            val effectiveOrder = if (sortOrder == 0 && existingAccounts.isNotEmpty()) {
                existingAccounts.maxOf { it.sortOrder } + 1
            } else sortOrder

            dao.insertAccount(
                AccountEntity(
                    accountName = name.trim().uppercase(),
                    startingBalance = startingBalance,
                    accountType = type,
                    minBalance = minBalance,
                    isArchived = false,
                    sortOrder = effectiveOrder
                )
            )
        }
    }

    fun updateAccountDetails(
        oldName: String,
        newName: String,
        startingBalance: Double,
        accountType: String,
        minBalance: Double? = null,
        isArchived: Boolean? = null,
        sortOrder: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAccountByName(oldName)
            val resolvedMinBal = minBalance ?: existing?.minBalance ?: 0.0
            val resolvedArchived = isArchived ?: existing?.isArchived ?: false
            val resolvedSortOrder = sortOrder ?: existing?.sortOrder ?: 0

            dao.updateAccountAndCascade(
                oldName = oldName,
                newName = newName,
                startingBalance = startingBalance,
                accountType = accountType,
                minBalance = resolvedMinBal,
                isArchived = resolvedArchived,
                sortOrder = resolvedSortOrder
            )
        }
    }

    fun archiveAccount(accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.archiveAccount(accountName)
        }
    }

    fun unarchiveAccount(accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.unarchiveAccount(accountName)
        }
    }

    fun reorderAccounts(orderedAccounts: List<AccountEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.reorderAccounts(orderedAccounts)
        }
    }

    fun deleteAccount(account: AccountEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val txCount = dao.getTransactionCountForAccount(account.accountName)
            val billCount = dao.getAllFixedBills().count {
                it.accountName.equals(account.accountName, ignoreCase = true) ||
                it.toAccountName.equals(account.accountName, ignoreCase = true)
            }

            if (txCount > 0 || billCount > 0) {
                val reasons = mutableListOf<String>()
                if (txCount > 0) reasons.add("$txCount linked transactions")
                if (billCount > 0) reasons.add("$billCount scheduled bills")
                withContext(Dispatchers.Main) {
                    onResult(false, "Cannot delete account with ${reasons.joinToString(" and ")}. Archive it instead.")
                }
            } else {
                dao.deleteAccount(account)
                withContext(Dispatchers.Main) {
                    onResult(true, "Account removed successfully.")
                }
            }
        }
    }

    suspend fun exportJsonBackupToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            val allTxs = dao.getAllTransactions()
            val allCats = dao.getAllCategoriesDirect()
            val allSubcats = dao.getAllSubcategoriesDirect()
            val allAccounts = dao.getAllAccountsDirect()
            val allFixedBills = dao.getAllFixedBills()
            val allBudgetPlans = dao.getAllBudgetPlans()
            val currentProfile = userProfile.value

            val profileObj = JSONObject().apply {
                put("displayName", currentProfile.displayName)
                put("email", currentProfile.email)
                put("dateOfBirth", currentProfile.dateOfBirth)
                put("baseMonthlyIncome", currentProfile.baseMonthlyIncome)
                put("currencySymbol", currentProfile.currencySymbol)
                put("profileImageUri", currentProfile.profileImageUri ?: JSONObject.NULL)
                put("coverImageUri", currentProfile.coverImageUri ?: JSONObject.NULL)
                put("fortressSweepThreshold", currentProfile.fortressSweepThreshold)
                put("fortressEmergencyMonths", currentProfile.fortressEmergencyMonths)
                put("fortressManualTarget", currentProfile.fortressManualTarget)
                put("taxonomyGraceMonth", currentProfile.taxonomyGraceMonth)
                put("taxonomyGraceYear", currentProfile.taxonomyGraceYear)
                put("isTaxonomyBannerDismissed", currentProfile.isTaxonomyBannerDismissed)
                put("isOnboardingCompleted", currentProfile.isOnboardingCompleted)
                put("isBiometricEnabled", currentProfile.isBiometricEnabled)
                put("isScreenCaptureAllowed", currentProfile.isScreenCaptureAllowed)
                put("isAutoPayReminderEnabled", currentProfile.isAutoPayReminderEnabled)
                put("isOverrunWarningEnabled", currentProfile.isOverrunWarningEnabled)
                put("reminderEnabled", currentProfile.reminderEnabled)
                put("reminderHour", currentProfile.reminderHour)
                put("reminderMinute", currentProfile.reminderMinute)
                put("vaultMode", currentProfile.vaultMode)
                put("initialReimbursementClaim", currentProfile.initialReimbursementClaim)
                put("initialCompanyAdvance", currentProfile.initialCompanyAdvance)
            }
            root.put("userProfile", profileObj)

            val txArray = JSONArray()
            allTxs.forEach { tx ->
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("subcategory", tx.subcategory)
                    put("accountName", tx.accountName)
                    put("toAccountName", tx.toAccountName ?: "")
                    put("type", tx.type.name)
                    put("date", tx.date)
                    put("month", tx.month)
                    put("year", tx.year)
                    put("linkedFixedBillId", tx.linkedFixedBillId ?: JSONObject.NULL)
                    put("transferSubtype", tx.transferSubtype.name)
                }
                txArray.put(obj)
            }
            root.put("transactions", txArray)

            val catArray = JSONArray()
            allCats.forEach { c ->
                catArray.put(JSONObject().apply {
                    put("name", c.name)
                    put("type", c.type.name)
                    put("isLegacy", c.isLegacy)
                    put("isNew", c.isNew)
                })
            }
            root.put("categories", catArray)

            val subArray = JSONArray()
            allSubcats.forEach { s ->
                subArray.put(JSONObject().apply {
                    put("parentCategory", s.parentCategory)
                    put("name", s.name)
                    put("type", s.type.name)
                    put("isLegacy", s.isLegacy)
                    put("isNew", s.isNew)
                })
            }
            root.put("subcategories", subArray)

            val accArray = JSONArray()
            allAccounts.forEach { a ->
                accArray.put(JSONObject().apply {
                    put("accountName", a.accountName)
                    put("startingBalance", a.startingBalance)
                    put("accountType", a.accountType)
                    put("minBalance", a.minBalance)
                    put("isArchived", a.isArchived)
                    put("sortOrder", a.sortOrder)
                })
            }
            root.put("accounts", accArray)

            val billsArray = JSONArray()
            allFixedBills.forEach { b ->
                billsArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("amount", b.amount)
                    put("category", b.category)
                    put("subcategory", b.subcategory)
                    put("accountName", b.accountName)
                    put("toAccountName", b.toAccountName ?: "")
                    put("type", b.type.name)
                    put("isPaid", b.isPaid)
                    put("dueDay", b.dueDay ?: JSONObject.NULL)
                    put("month", b.month)
                    put("year", b.year)
                })
            }
            root.put("fixedBills", billsArray)

            val plansArray = JSONArray()
            allBudgetPlans.forEach { p ->
                plansArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("category", p.category)
                    put("plannedAmount", p.plannedAmount)
                    put("type", p.type.name)
                    put("month", p.month)
                    put("year", p.year)
                })
            }
            root.put("budgetPlans", plansArray)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreFromJsonUri(context: Context, uri: Uri, wipeExisting: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            val root = JSONObject(stringBuilder.toString())

            if (wipeExisting) {
                dao.clearAllTransactions()
                dao.clearAllFixedBills()
                dao.clearAllBudgetPlans()
            }
            if (root.has("categories") && wipeExisting) {
                dao.clearAllCategories()
                dao.clearAllSubcategories()
                val catArray = root.getJSONArray("categories")
                val catList = mutableListOf<CategoryEntity>()
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    catList.add(
                        CategoryEntity(
                            name = c.getString("name"),
                            type = TransactionType.valueOf(c.getString("type")),
                            isLegacy = c.optBoolean("isLegacy", false),
                            isNew = c.optBoolean("isNew", false)
                        )
                    )
                }
                dao.insertCategories(catList)
            }
            if (root.has("subcategories") && wipeExisting) {
                val subArray = root.getJSONArray("subcategories")
                val subList = mutableListOf<SubcategoryEntity>()
                for (i in 0 until subArray.length()) {
                    val s = subArray.getJSONObject(i)
                    subList.add(
                        SubcategoryEntity(
                            parentCategory = s.getString("parentCategory"),
                            name = s.getString("name"),
                            type = TransactionType.valueOf(s.getString("type")),
                            isLegacy = s.optBoolean("isLegacy", false),
                            isNew = s.optBoolean("isNew", false)
                        )
                    )
                }
                dao.insertSubcategories(subList)
            }
            if (root.has("accounts") && wipeExisting) {
                dao.clearAllAccounts()
                val accArray = root.getJSONArray("accounts")
                val accList = mutableListOf<AccountEntity>()
                for (i in 0 until accArray.length()) {
                    val a = accArray.getJSONObject(i)
                    val accType = if (a.has("accountType")) a.getString("accountType") else a.optString("type", "Operating")
                    val sortOrder = a.optInt("sortOrder", i)
                    val minBal = a.optDouble("minBalance", 0.0)
                    val isArchived = a.optBoolean("isArchived", false)
                    accList.add(
                        AccountEntity(
                            accountName = a.getString("accountName"),
                            startingBalance = a.getDouble("startingBalance"),
                            accountType = accType,
                            minBalance = minBal,
                            isArchived = isArchived,
                            sortOrder = sortOrder
                        )
                    )
                }
                dao.insertAccounts(accList)
            }
            if (root.has("fixedBills")) {
                val billsArray = root.getJSONArray("fixedBills")
                val billsList = mutableListOf<FixedBillEntity>()
                for (i in 0 until billsArray.length()) {
                    val b = billsArray.getJSONObject(i)
                    billsList.add(
                        FixedBillEntity(
                            id = b.optLong("id", 0L),
                            title = b.getString("title"),
                            amount = b.getDouble("amount"),
                            category = b.getString("category"),
                            subcategory = b.optString("subcategory", "General"),
                            accountName = b.getString("accountName"),
                            toAccountName = if (b.optString("toAccountName").isBlank()) null else b.getString("toAccountName"),
                            type = TransactionType.valueOf(b.getString("type")),
                            isPaid = b.optBoolean("isPaid", false),
                            dueDay = if (b.isNull("dueDay")) null else b.optInt("dueDay"),
                            month = b.getInt("month"),
                            year = b.getInt("year")
                        )
                    )
                }
                dao.insertFixedBills(billsList)
            }
            if (root.has("budgetPlans")) {
                val plansArray = root.getJSONArray("budgetPlans")
                val plansList = mutableListOf<BudgetPlanEntity>()
                for (i in 0 until plansArray.length()) {
                    val p = plansArray.getJSONObject(i)
                    plansList.add(
                        BudgetPlanEntity(
                            id = p.optLong("id", 0L),
                            category = p.getString("category"),
                            plannedAmount = p.getDouble("plannedAmount"),
                            type = TransactionType.valueOf(p.getString("type")),
                            month = p.getInt("month"),
                            year = p.getInt("year")
                        )
                    )
                }
                dao.insertBudgetPlans(plansList)
            }
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val subTypeStr = obj.optString("transferSubtype", "NONE")
                    val subType = try { TransferSubtype.valueOf(subTypeStr) } catch (_: Exception) { TransferSubtype.NONE }
                    dao.insertTransaction(
                        TransactionEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            category = obj.getString("category"),
                            subcategory = obj.optString("subcategory", "General"),
                            accountName = obj.getString("accountName"),
                            toAccountName = if (obj.optString("toAccountName").isBlank()) null else obj.getString("toAccountName"),
                            type = TransactionType.valueOf(obj.getString("type")),
                            date = obj.getLong("date"),
                            month = obj.getInt("month"),
                            year = obj.getInt("year"),
                            linkedFixedBillId = if (obj.isNull("linkedFixedBillId")) null else obj.optLong("linkedFixedBillId"),
                            transferSubtype = subType
                        )
                    )
                }
            }

            var updatedProfile = userProfile.value.copy(id = 1, isOnboardingCompleted = true)
            if (root.has("userProfile")) {
                val p = root.getJSONObject("userProfile")
                val parsedProfileImg = if (p.isNull("profileImageUri")) null else p.optString("profileImageUri").takeIf { it.isNotBlank() }
                val parsedCoverImg = if (p.isNull("coverImageUri")) null else p.optString("coverImageUri").takeIf { it.isNotBlank() }
                val restoredDob = p.optString("dateOfBirth", updatedProfile.dateOfBirth)

                if (restoredDob.isNotBlank()) {
                    securityManager.setRecoveryDob(restoredDob)
                }

                updatedProfile = updatedProfile.copy(
                    id = 1,
                    displayName = p.optString("displayName", updatedProfile.displayName),
                    email = p.optString("email", updatedProfile.email),
                    dateOfBirth = restoredDob,
                    baseMonthlyIncome = p.optDouble("baseMonthlyIncome", updatedProfile.baseMonthlyIncome),
                    currencySymbol = p.optString("currencySymbol", updatedProfile.currencySymbol),
                    profileImageUri = parsedProfileImg ?: updatedProfile.profileImageUri,
                    coverImageUri = parsedCoverImg ?: updatedProfile.coverImageUri,
                    fortressSweepThreshold = p.optDouble("fortressSweepThreshold", 0.0),
                    fortressEmergencyMonths = p.optInt("fortressEmergencyMonths", 6),
                    fortressManualTarget = p.optDouble("fortressManualTarget", 0.0),
                    taxonomyGraceMonth = p.optInt("taxonomyGraceMonth", -1),
                    taxonomyGraceYear = p.optInt("taxonomyGraceYear", -1),
                    isTaxonomyBannerDismissed = p.optBoolean("isTaxonomyBannerDismissed", false),
                    isBiometricEnabled = p.optBoolean("isBiometricEnabled", updatedProfile.isBiometricEnabled),
                    isScreenCaptureAllowed = p.optBoolean("isScreenCaptureAllowed", updatedProfile.isScreenCaptureAllowed),
                    isAutoPayReminderEnabled = p.optBoolean("isAutoPayReminderEnabled", updatedProfile.isAutoPayReminderEnabled),
                    isOverrunWarningEnabled = p.optBoolean("isOverrunWarningEnabled", updatedProfile.isOverrunWarningEnabled),
                    reminderEnabled = p.optBoolean("reminderEnabled", updatedProfile.reminderEnabled),
                    reminderHour = p.optInt("reminderHour", updatedProfile.reminderHour),
                    reminderMinute = p.optInt("reminderMinute", updatedProfile.reminderMinute),
                    vaultMode = p.optString("vaultMode", updatedProfile.vaultMode),
                    initialReimbursementClaim = p.optDouble("initialReimbursementClaim", 0.0),
                    initialCompanyAdvance = p.optDouble("initialCompanyAdvance", 0.0)
                )
            }
            dao.saveUserProfile(updatedProfile)
            isAppUnlocked.value = true
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getBillSignature(b: FixedBillEntity): String {
        val cleanCat = b.category.trim().lowercase()
        val cleanSubcat = b.subcategory.trim().lowercase()
        val cleanTitle = b.title.trim().lowercase()
        val cleanAcc = b.accountName.trim().lowercase()
        val cleanToAcc = b.toAccountName?.trim()?.lowercase() ?: ""
        val cleanType = b.type.name

        return if (cleanTitle.isNotBlank() && cleanTitle != cleanSubcat) {
            "${cleanCat}_${cleanSubcat}_${cleanTitle}_${cleanAcc}_${cleanToAcc}_${cleanType}"
        } else {
            val due = b.dueDay ?: -1
            "${cleanCat}_${cleanSubcat}_${due}_${cleanAcc}_${cleanToAcc}_${cleanType}"
        }
    }

    private suspend fun checkAndRolloverRecurringBills(targetMonth: Int, targetYear: Int): Int = withContext(Dispatchers.IO) {
        val historicalBills = dao.getLatestHistoricalFixedBills(targetMonth, targetYear)
        if (historicalBills.isEmpty()) return@withContext 0

        var currentIterMonth = historicalBills.first().month
        var currentIterYear = historicalBills.first().year
        var latestKnownBills: List<FixedBillEntity> = historicalBills.filter {
            it.month == currentIterMonth && it.year == currentIterYear
        }

        var totalClonedInRun = 0

        while (currentIterYear < targetYear || (currentIterYear == targetYear && currentIterMonth < targetMonth)) {
            val prevMonth = currentIterMonth
            val prevYear = currentIterYear

            if (currentIterMonth == 12) {
                currentIterMonth = 1
                currentIterYear += 1
            } else {
                currentIterMonth += 1
            }

            val sourceBills = dao.getFixedBillsForMonthDirect(prevMonth, prevYear).ifEmpty { latestKnownBills }
            val existingInIter = dao.getFixedBillsForMonthDirect(currentIterMonth, currentIterYear)

            val existingSignatures = existingInIter.map { getBillSignature(it) }.toSet()

            val missingToClone = sourceBills.filter { source ->
                val sig = getBillSignature(source)
                !existingSignatures.contains(sig)
            }.distinctBy { getBillSignature(it) }.map {
                FixedBillEntity(
                    title = it.title,
                    amount = it.amount,
                    category = it.category,
                    subcategory = it.subcategory,
                    accountName = it.accountName,
                    toAccountName = it.toAccountName,
                    type = it.type,
                    isPaid = false,
                    dueDay = it.dueDay,
                    month = currentIterMonth,
                    year = currentIterYear
                )
            }

            if (missingToClone.isNotEmpty()) {
                dao.insertFixedBills(missingToClone)
                totalClonedInRun += missingToClone.size
            }

            latestKnownBills = dao.getFixedBillsForMonthDirect(currentIterMonth, currentIterYear)
        }

        totalClonedInRun
    }

    private fun calculateDailySparklinePoints(
        transactions: List<TransactionEntity>,
        month: Int,
        year: Int
    ): List<Float> {
        val daysInMonth = Calendar.getInstance().apply { set(year, month - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dataMap = FloatArray(daysInMonth) { 0f }
        val calTx = Calendar.getInstance()

        transactions.filter { it.type == TransactionType.EXPENSE }.forEach { tx ->
            calTx.timeInMillis = tx.date
            val day = calTx.get(Calendar.DAY_OF_MONTH)
            if (day in 1..daysInMonth) {
                dataMap[day - 1] += tx.amount.toFloat()
            }
        }

        var runningTotal = 0f
        return dataMap.map { amt ->
            runningTotal += amt
            runningTotal
        }
    }

    private fun formatDateHeader(timestamp: Long): String {
        val nowMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val txMidnight = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = (nowMidnight.timeInMillis - txMidnight.timeInMillis) / (24 * 60 * 60 * 1000L)

        return when (diffDays) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(timestamp))
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
