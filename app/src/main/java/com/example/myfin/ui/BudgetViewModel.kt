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
import kotlin.math.max

data class SubcategoryPerformance(
    val name: String,
    val amount: Double
)

data class CategoryPerformance(
    val category: String,
    val type: TransactionType,
    val plannedAmount: Double,
    val actualAmount: Double,
    val activeSubcategories: List<SubcategoryPerformance> = emptyList()
) {
    val variance: Double get() = plannedAmount - actualAmount
    val isOverBudget: Boolean get() = type == TransactionType.EXPENSE && actualAmount > plannedAmount && plannedAmount > 0

    val name: String get() = category
    val categoryName: String get() = category
    val spentAmount: Double get() = actualAmount
    val budgetedAmount: Double get() = plannedAmount
}

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
    val safeToSpendPercentage: Int = 100,
    val netSavedAfterInvest: Double = 0.0,
    val totalVaultBalance: Double = 0.0,
    val isOverBudget: Boolean = false,
    val dailyExpensePoints: List<Float> = emptyList()
) {
    val totalAssetAllocated: Double get() = actualAssets
}

data class MonthlyUiState(
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val accounts: List<AccountBalanceResult> = emptyList(),
    val fixedBills: List<FixedBillEntity> = emptyList(),
    val categories: List<CategoryPerformance> = emptyList(),
    val masterCategories: List<CategoryEntity> = emptyList(),
    val masterSubcategories: List<SubcategoryEntity> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionEntity>> = emptyMap(),
    val budgetPlans: List<BudgetPlanEntity> = emptyList()
) {
    val categoryBreakdowns: List<CategoryPerformance> get() = categories
    val accountList: List<String> get() = accounts.map { it.accountName }
    val subcategories: List<SubcategoryEntity> get() = masterSubcategories
}

data class YearlyUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val monthlyRollups: List<MonthlySummary> = emptyList(),
    val categoryRollups: List<YearlyCategoryRollup> = emptyList(),
    val totalYearlyIncome: Double = 0.0,
    val totalYearlyExpense: Double = 0.0,
    val totalYearlyAssets: Double = 0.0,
    val annualNetSurplus: Double = 0.0
) {
    val totalAnnualIncome: Double get() = totalYearlyIncome
    val totalAnnualExpense: Double get() = totalYearlyExpense
    val netAnnualSavings: Double get() = annualNetSurplus
    val monthlySummaries: List<MonthlySummary> get() = monthlyRollups
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
    val showRolloverPrompt = MutableStateFlow(false)

    val protectedCategories = setOf(
        "Utilities & Living Bills",
        "Everyday Living",
        "Leisure, Trips & Media",
        "Health & Medical",
        "Family & Home Support",
        "Debt & Financial Obligations",
        "Work & Professional",
        "Investments & Wealth",
        "Liquid Reserves & Receivables",
        "Salary & Professional Inflow",
        "Reimbursements & Corporate Inflow",
        "Passive & Capital Drawdowns",
        "General"
    )

    val userProfile: StateFlow<UserProfile> = dao.getUserProfile()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            seedFullExcelTaxonomyIfEmpty()
            checkAndRolloverRecurringBills(currentMonth.value, currentYear.value)
            checkIfRolloverPromptNeeded()
        }
    }

    val monthlyUiState: StateFlow<MonthlyUiState> = combine(
        currentMonth, currentYear, filterCriteria
    ) { month, year, filter -> Triple(month, year, filter) }
        .flatMapLatest { (month, year, filter) ->
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

            combine(coreDataFlow, metadataFlow) { coreData, metaData ->
                val (transactions, fixedBills, accounts) = coreData
                val (plans, masterCats, masterSubcats) = metaData

                val regularTxs = transactions.filter { it.type != TransactionType.TRANSFER }

                val actualIncome = regularTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val actualExpenses = regularTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val actualAssets = regularTxs.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }

                val allCategoryNames = (masterCats.map { it.name to it.type } +
                        plans.map { it.category to it.type } +
                        fixedBills.map { it.category to it.type } +
                        regularTxs.map { it.category to it.type }).distinct()

                val matrixList = allCategoryNames.mapNotNull { (catName, catType) ->
                    if (catType == TransactionType.TRANSFER) return@mapNotNull null

                    val catTxs = regularTxs.filter { it.category == catName && it.type == catType }
                    val actualTotal = catTxs.sumOf { it.amount }

                    val manualPlan = plans.find { it.category == catName && it.type == catType }?.plannedAmount ?: 0.0
                    val fixedForCat = fixedBills.filter { it.category == catName && it.type == catType }.sumOf { it.amount }
                    val effectivePlanned = if (manualPlan > 0.0) max(manualPlan, fixedForCat) else fixedForCat

                    if (actualTotal == 0.0 && effectivePlanned == 0.0) return@mapNotNull null

                    val activeSubs = catTxs.groupBy { it.subcategory }
                        .map { (subName, txs) -> SubcategoryPerformance(subName, txs.sumOf { it.amount }) }
                        .filter { it.amount != 0.0 }

                    CategoryPerformance(
                        category = catName,
                        type = catType,
                        plannedAmount = effectivePlanned,
                        actualAmount = actualTotal,
                        activeSubcategories = activeSubs
                    )
                }

                val plannedIncome = matrixList.filter { it.type == TransactionType.INCOME }.sumOf { it.plannedAmount }
                val plannedExpenses = matrixList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.plannedAmount }
                val plannedAssets = matrixList.filter { it.type == TransactionType.ASSET }.sumOf { it.plannedAmount }

                val fixedExpenseTotal = fixedBills.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val baseIncome = max(plannedIncome, actualIncome)
                val commitments = fixedExpenseTotal + actualAssets
                val rawSafeToSpend = baseIncome - commitments - actualExpenses
                val safeToSpend = if (baseIncome > 0) rawSafeToSpend.coerceAtLeast(0.0) else 0.0

                val safeToSpendPercentage = if (baseIncome > 0) {
                    ((safeToSpend / baseIncome) * 100).toInt().coerceIn(0, 100)
                } else 0

                val isOverBudget = rawSafeToSpend < 0.0 || (plannedExpenses > 0 && actualExpenses > plannedExpenses)
                val netSaved = (actualIncome - actualExpenses) - actualAssets
                val totalVault = accounts.sumOf { it.currentBalance }
                val dailyPoints = calculateDailySparklinePoints(transactions, month, year)

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
                        fixedCommitmentsTotal = fixedBills.sumOf { it.amount },
                        safeToSpend = safeToSpend,
                        safeToSpendPercentage = safeToSpendPercentage,
                        netSavedAfterInvest = netSaved,
                        totalVaultBalance = totalVault,
                        isOverBudget = isOverBudget,
                        dailyExpensePoints = dailyPoints
                    ),
                    accounts = accounts,
                    fixedBills = fixedBills,
                    categories = matrixList,
                    masterCategories = masterCats,
                    masterSubcategories = masterSubcats,
                    groupedTransactions = grouped,
                    budgetPlans = plans
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyUiState())

    val yearlyUiState: StateFlow<YearlyUiState> = currentYear
        .flatMapLatest { year ->
            combine(
                dao.getYearlySummary(year),
                dao.getYearlyCategoryBreakdown(year)
            ) { rollups, categoryRollups ->
                val totalIncome = rollups.sumOf { it.totalActualIncome }
                val totalExpense = rollups.sumOf { it.totalActualExpense }
                val totalAssets = rollups.sumOf { it.totalAsset }
                val netSurplus = totalIncome - totalExpense

                YearlyUiState(
                    selectedYear = year,
                    monthlyRollups = rollups,
                    categoryRollups = categoryRollups,
                    totalYearlyIncome = totalIncome,
                    totalYearlyExpense = totalExpense,
                    totalYearlyAssets = totalAssets,
                    annualNetSurplus = netSurplus
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearlyUiState())

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

    fun checkIfRolloverPromptNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val nowCal = Calendar.getInstance()
            val day = nowCal.get(Calendar.DAY_OF_MONTH)
            if (day >= 28) {
                val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                val nMonth = nextMonthCal.get(Calendar.MONTH) + 1
                val nYear = nextMonthCal.get(Calendar.YEAR)
                val count = dao.getFixedBillCount(nMonth, nYear)
                showRolloverPrompt.value = (count == 0)
            } else {
                showRolloverPrompt.value = false
            }
        }
    }

    fun executeRolloverToNextMonth() {
        viewModelScope.launch(Dispatchers.IO) {
            val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            val nMonth = nextMonthCal.get(Calendar.MONTH) + 1
            val nYear = nextMonthCal.get(Calendar.YEAR)
            checkAndRolloverRecurringBills(nMonth, nYear)
            showRolloverPrompt.value = false
        }
    }

    fun dismissRolloverPrompt() {
        showRolloverPrompt.value = false
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveUserProfile(profile)
        }
    }

    fun setMasterPin(pin: String, recoveryDob: String) {
        securityManager.setPin(pin)
        securityManager.setRecoveryDob(recoveryDob)
    }

    fun updateProfileImageUri(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(profileImageUri = uriString))
        }
    }

    fun updateCoverImageUri(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(coverImageUri = uriString))
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(displayName = name))
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(currencySymbol = symbol))
        }
    }

    fun updateFortressThreshold(newThreshold: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(fortressThreshold = newThreshold))
        }
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(isBiometricEnabled = enabled))
        }
    }

    fun updateScreenCaptureAllowed(allowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(current.copy(isScreenCaptureAllowed = allowed))
        }
    }

    fun updateReminderSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            dao.saveUserProfile(
                current.copy(
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
        fortressThreshold: Double,
        masterPin: String,
        isBiometricEnabled: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            securityManager.setPin(masterPin)
            securityManager.setRecoveryDob(dob)
            val profile = UserProfile(
                id = 1,
                displayName = displayName,
                dateOfBirth = dob,
                baseMonthlyIncome = baseIncome,
                fortressThreshold = fortressThreshold,
                isOnboardingCompleted = true,
                isBiometricEnabled = isBiometricEnabled
            )
            dao.saveUserProfile(profile)
            isAppUnlocked.value = true
        }
    }

    suspend fun seedFullExcelTaxonomyIfEmpty() = withContext(Dispatchers.IO) {
        val existingCats = dao.getAllCategories().first()
        if (existingCats.isEmpty()) {
            dao.insertCategories(CategoryEntity.defaultCategories)
            dao.insertSubcategories(SubcategoryEntity.defaultSubcategories)
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
            securityManager.setPin("")
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
            val previousPlans = dao.getBudgetPlansForMonth(prevMonth, prevYear).first()

            previousPlans.forEach { plan ->
                dao.insertBudgetPlan(
                    BudgetPlanEntity(
                        category = plan.category,
                        plannedAmount = plan.plannedAmount,
                        type = plan.type,
                        month = currentMonth.value,
                        year = currentYear.value
                    )
                )
            }
            withContext(Dispatchers.Main) {
                onComplete(previousPlans.size)
            }
        }
    }

    fun updateCategoryBudget(category: String, amount: Double, type: TransactionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPlans = dao.getBudgetPlansForMonth(currentMonth.value, currentYear.value).first()
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

    fun saveBudgetPlan(category: String, amount: Double, type: TransactionType) {
        updateCategoryBudget(category, amount, type)
    }

    fun saveBudgetPlan(plan: BudgetPlanEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertBudgetPlan(
                plan.copy(month = currentMonth.value, year = currentYear.value)
            )
        }
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
        toAccountName: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedTitle = if (title.isBlank()) subcategory else title.trim()
            val calTx = Calendar.getInstance().apply { timeInMillis = date }
            val txMonth = calTx.get(Calendar.MONTH) + 1
            val txYear = calTx.get(Calendar.YEAR)

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
                year = txYear
            )
            if (id == 0L) dao.insertTransaction(entity) else dao.updateTransaction(entity)
        }
    }

    fun addTransaction(tx: TransactionEntity) {
        saveTransaction(tx.id, tx.title, tx.amount, tx.category, tx.subcategory, tx.accountName, tx.type, tx.date, tx.toAccountName)
    }

    fun updateTransaction(tx: TransactionEntity) {
        saveTransaction(tx.id, tx.title, tx.amount, tx.category, tx.subcategory, tx.accountName, tx.type, tx.date, tx.toAccountName)
    }

    fun executeInstantTransfer(fromAccount: String, toAccount: String, amount: Double, note: String = "") {
        val resolvedNote = if (note.isBlank()) "Vault Transfer ($fromAccount ➔ $toAccount)" else note.trim()
        saveTransaction(
            title = resolvedNote,
            amount = amount,
            category = "Transfer",
            subcategory = "Internal",
            accountName = fromAccount,
            toAccountName = toAccount,
            type = TransactionType.TRANSFER
        )
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
        viewModelScope.launch(Dispatchers.IO) { dao.insertCategory(CategoryEntity(name = name.trim(), type = type)) }
    }

    fun addCategory(category: CategoryEntity) {
        addCategory(category.name, category.type)
    }

    fun updateCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldName = category.name
            val trimmed = newName.trim()
            dao.updateCategory(category.copy(name = trimmed))
            dao.cascadeRenameCategoryInTransactions(oldName, trimmed)
            dao.cascadeRenameCategoryInBudgetPlans(oldName, trimmed)
            dao.cascadeRenameCategoryInFixedBills(oldName, trimmed)
            dao.cascadeRenameCategoryInSubcategories(oldName, trimmed)
        }
    }

    fun deleteCategory(category: CategoryEntity, onResult: (Boolean, String) -> Unit) {
        if (protectedCategories.contains(category.name)) {
            onResult(false, "'${category.name}' is a protected system category and cannot be deleted.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCategory(category)
            dao.deleteSubcategoriesForParent(category.name)
            dao.deleteBudgetPlansForCategory(category.name)
            dao.reassignOrphanedTransactionsToGeneral(category.name)
            dao.deleteFutureUnpaidFixedBillsByCategory(category.name, currentMonth.value, currentYear.value)
            withContext(Dispatchers.Main) {
                onResult(true, "Category deleted. Historical entries safely reassigned to 'General'.")
            }
        }
    }

    fun addSubcategory(parentCategory: String, name: String, type: TransactionType) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertSubcategory(SubcategoryEntity(parentCategory = parentCategory, name = name.trim(), type = type)) }
    }

    fun addSubcategory(sub: SubcategoryEntity) {
        addSubcategory(sub.parentCategory, sub.name, sub.type)
    }

    fun updateSubcategory(sub: SubcategoryEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldName = sub.name
            val trimmed = newName.trim()
            dao.updateSubcategory(sub.copy(name = trimmed))
            dao.cascadeRenameSubcategoryInTransactions(sub.parentCategory, oldName, trimmed)
            dao.cascadeRenameSubcategoryInFixedBills(sub.parentCategory, oldName, trimmed)
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
        dueDay: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertFixedBill(
                FixedBillEntity(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    subcategory = subcategory,
                    accountName = account,
                    toAccountName = toAccount,
                    type = type,
                    isPaid = false,
                    dueDay = dueDay,
                    month = currentMonth.value,
                    year = currentYear.value
                )
            )
        }
    }

    fun insertFixedBillDirect(bill: FixedBillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertFixedBill(bill)
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
                dao.updateFixedBill(
                    existing.copy(
                        title = title.trim(),
                        amount = amount,
                        category = category,
                        subcategory = subcategory,
                        accountName = account,
                        toAccountName = toAccount,
                        type = type,
                        dueDay = dueDay
                    )
                )
            }
        }
    }

    fun toggleFixedBillPaid(bill: FixedBillEntity, customAmount: Double = bill.amount, customDateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedStatus = !bill.isPaid
            dao.updateFixedBill(bill.copy(isPaid = updatedStatus))

            if (updatedStatus) {
                val calTx = Calendar.getInstance().apply { timeInMillis = customDateMillis }
                val txMonth = calTx.get(Calendar.MONTH) + 1
                val txYear = calTx.get(Calendar.YEAR)

                dao.insertTransaction(
                    TransactionEntity(
                        title = bill.title,
                        amount = customAmount,
                        category = bill.category,
                        subcategory = bill.subcategory.ifBlank { bill.title },
                        accountName = bill.accountName,
                        toAccountName = bill.toAccountName,
                        type = bill.type,
                        date = customDateMillis,
                        month = txMonth,
                        year = txYear,
                        linkedFixedBillId = bill.id
                    )
                )
            } else {
                dao.deleteTransactionByLinkedBill(bill.id)
            }
        }
    }

    fun deleteFixedBill(bill: FixedBillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteFixedBill(bill)
            dao.deleteTransactionByLinkedBill(bill.id)
        }
    }

    fun addAccount(name: String, startingBalance: Double, type: String = "Bank", sortOrder: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingAccounts = dao.getAllAccounts().first()
            val effectiveOrder = if (sortOrder == 0 && existingAccounts.isNotEmpty()) {
                existingAccounts.maxOf { it.sortOrder } + 1
            } else sortOrder

            dao.insertAccount(
                AccountEntity(
                    accountName = name.trim(),
                    startingBalance = startingBalance,
                    accountType = type,
                    sortOrder = effectiveOrder
                )
            )
        }
    }

    fun updateAccountStartingBalance(accountName: String, startingBalance: Double, type: String = "Bank") {
        viewModelScope.launch(Dispatchers.IO) { dao.insertAccount(AccountEntity(accountName, startingBalance, type)) }
    }

    fun updateAccountDetails(
        oldName: String,
        newName: String,
        startingBalance: Double,
        accountType: String,
        sortOrder: Int = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateAccountAndCascade(
                oldName = oldName,
                newName = newName,
                startingBalance = startingBalance,
                accountType = accountType,
                sortOrder = sortOrder
            )
        }
    }

    fun reorderAccounts(orderedAccounts: List<AccountEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.reorderAccounts(orderedAccounts)
        }
    }

    fun deleteAccount(account: AccountEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = dao.getTransactionCountForAccount(account.accountName)
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    onResult(false, "Cannot delete account with $count linked transactions.")
                } else {
                    dao.deleteAccount(account)
                    onResult(true, "Account removed successfully.")
                }
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
                onResult(success, if (success) "" else "Backup failed")
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

    suspend fun exportJsonBackupToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            val allTxs = dao.getAllTransactions()
            val allCats = dao.getAllCategories().first()
            val allSubcats = dao.getAllSubcategories().first()
            val allAccounts = dao.getAllAccounts().first()
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
                put("fortressThreshold", currentProfile.fortressThreshold)
                put("isOnboardingCompleted", currentProfile.isOnboardingCompleted)
                put("isBiometricEnabled", currentProfile.isBiometricEnabled)
                put("isScreenCaptureAllowed", currentProfile.isScreenCaptureAllowed)
                put("isAutoPayReminderEnabled", currentProfile.isAutoPayReminderEnabled)
                put("isOverrunWarningEnabled", currentProfile.isOverrunWarningEnabled)
                put("reminderEnabled", currentProfile.reminderEnabled)
                put("reminderHour", currentProfile.reminderHour)
                put("reminderMinute", currentProfile.reminderMinute)
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
                }
                txArray.put(obj)
            }
            root.put("transactions", txArray)

            val catArray = JSONArray()
            allCats.forEach { c ->
                catArray.put(JSONObject().apply {
                    put("name", c.name)
                    put("type", c.type.name)
                })
            }
            root.put("categories", catArray)

            val subArray = JSONArray()
            allSubcats.forEach { s ->
                subArray.put(JSONObject().apply {
                    put("parentCategory", s.parentCategory)
                    put("name", s.name)
                    put("type", s.type.name)
                })
            }
            root.put("subcategories", subArray)

            val accArray = JSONArray()
            allAccounts.forEach { a ->
                accArray.put(JSONObject().apply {
                    put("accountName", a.accountName)
                    put("startingBalance", a.startingBalance)
                    put("accountType", a.accountType)
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
            val jsonContent = stringBuilder.toString()
            val root = JSONObject(jsonContent)

            if (wipeExisting) {
                dao.clearAllTransactions()
                dao.clearAllFixedBills()
                dao.clearAllBudgetPlans()
            }
            if (root.has("categories") && wipeExisting) {
                dao.clearAllCategories()
                dao.clearAllSubcategories()
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    dao.insertCategory(CategoryEntity(name = c.getString("name"), type = TransactionType.valueOf(c.getString("type"))))
                }
            }
            if (root.has("subcategories") && wipeExisting) {
                val subArray = root.getJSONArray("subcategories")
                for (i in 0 until subArray.length()) {
                    val s = subArray.getJSONObject(i)
                    dao.insertSubcategory(SubcategoryEntity(parentCategory = s.getString("parentCategory"), name = s.getString("name"), type = TransactionType.valueOf(s.getString("type"))))
                }
            }
            if (root.has("accounts") && wipeExisting) {
                dao.clearAllAccounts()
                val accArray = root.getJSONArray("accounts")
                for (i in 0 until accArray.length()) {
                    val a = accArray.getJSONObject(i)
                    val accType = if (a.has("accountType")) a.getString("accountType") else a.optString("type", "Bank")
                    val sortOrder = a.optInt("sortOrder", i)
                    dao.insertAccount(
                        AccountEntity(
                            accountName = a.getString("accountName"),
                            startingBalance = a.getDouble("startingBalance"),
                            accountType = accType,
                            sortOrder = sortOrder
                        )
                    )
                }
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
                            linkedFixedBillId = if (obj.isNull("linkedFixedBillId")) null else obj.optLong("linkedFixedBillId")
                        )
                    )
                }
            }

            var updatedProfile = userProfile.value.copy(isOnboardingCompleted = true)
            if (root.has("userProfile")) {
                val p = root.getJSONObject("userProfile")
                val parsedProfileImg = if (p.isNull("profileImageUri")) null else p.optString("profileImageUri").takeIf { it.isNotBlank() }
                val parsedCoverImg = if (p.isNull("coverImageUri")) null else p.optString("coverImageUri").takeIf { it.isNotBlank() }

                updatedProfile = updatedProfile.copy(
                    displayName = p.optString("displayName", updatedProfile.displayName),
                    email = p.optString("email", updatedProfile.email),
                    dateOfBirth = p.optString("dateOfBirth", updatedProfile.dateOfBirth),
                    baseMonthlyIncome = p.optDouble("baseMonthlyIncome", updatedProfile.baseMonthlyIncome),
                    currencySymbol = p.optString("currencySymbol", updatedProfile.currencySymbol),
                    profileImageUri = parsedProfileImg ?: updatedProfile.profileImageUri,
                    coverImageUri = parsedCoverImg ?: updatedProfile.coverImageUri,
                    fortressThreshold = p.optDouble("fortressThreshold", updatedProfile.fortressThreshold),
                    isBiometricEnabled = p.optBoolean("isBiometricEnabled", updatedProfile.isBiometricEnabled),
                    isScreenCaptureAllowed = p.optBoolean("isScreenCaptureAllowed", updatedProfile.isScreenCaptureAllowed),
                    isAutoPayReminderEnabled = p.optBoolean("isAutoPayReminderEnabled", updatedProfile.isAutoPayReminderEnabled),
                    isOverrunWarningEnabled = p.optBoolean("isOverrunWarningEnabled", updatedProfile.isOverrunWarningEnabled),
                    reminderEnabled = p.optBoolean("reminderEnabled", updatedProfile.reminderEnabled),
                    reminderHour = p.optInt("reminderHour", updatedProfile.reminderHour),
                    reminderMinute = p.optInt("reminderMinute", updatedProfile.reminderMinute)
                )
            }
            dao.saveUserProfile(updatedProfile)
            isAppUnlocked.value = true
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkAndRolloverRecurringBills(month: Int, year: Int) = withContext(Dispatchers.IO) {
        val count = dao.getFixedBillCount(month, year)
        if (count == 0) {
            val historicalBills = dao.getLatestHistoricalFixedBills(month, year)
            if (historicalBills.isNotEmpty()) {
                val latestMonth = historicalBills.first().month
                val latestYear = historicalBills.first().year
                val billsToClone = historicalBills.filter { it.month == latestMonth && it.year == latestYear }

                val cloned = billsToClone.distinctBy { it.title }.map {
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
                        month = month,
                        year = year
                    )
                }
                dao.insertFixedBills(cloned)
            }
        }
    }

    private fun calculateDailySparklinePoints(transactions: List<TransactionEntity>, month: Int, year: Int): List<Float> {
        val daysInMonth = Calendar.getInstance().apply { set(year, month - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyMap = FloatArray(daysInMonth) { 0f }
        val calTx = Calendar.getInstance()

        transactions.filter { it.type == TransactionType.EXPENSE }.forEach { tx ->
            calTx.timeInMillis = tx.date
            val day = calTx.get(Calendar.DAY_OF_MONTH)
            if (day in 1..daysInMonth) {
                dailyMap[day - 1] += tx.amount.toFloat()
            }
        }

        var runningTotal = 0f
        return dailyMap.map { amt ->
            runningTotal += amt
            runningTotal
        }
    }

    private fun formatDateHeader(timestamp: Long): String {
        val dateCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        return when {
            dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) -> "Today"
            dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(timestamp))
        }
    }
}
