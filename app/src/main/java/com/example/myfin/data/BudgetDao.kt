package com.example.myfin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions WHERE month = :month AND year = :year ORDER BY date DESC")
    fun getTransactionsForMonth(month: Int, year: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE linkedFixedBillId = :fixedBillId")
    suspend fun deleteTransactionByLinkedBill(fixedBillId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions WHERE accountName = :accountName OR toAccountName = :accountName")
    suspend fun getTransactionCountForAccount(accountName: String): Int

    // --- FIXED BILLS / AUTOPAY ---
    @Query("SELECT * FROM fixed_bills WHERE month = :month AND year = :year ORDER BY dueDay ASC, id ASC")
    fun getFixedBillsForMonth(month: Int, year: Int): Flow<List<FixedBillEntity>>

    @Query("SELECT * FROM fixed_bills")
    suspend fun getAllFixedBills(): List<FixedBillEntity>

    @Query("SELECT * FROM fixed_bills WHERE id = :id LIMIT 1")
    suspend fun getFixedBillById(id: Long): FixedBillEntity?

    @Query("SELECT COUNT(*) FROM fixed_bills WHERE month = :month AND year = :year")
    suspend fun getFixedBillCount(month: Int, year: Int): Int

    @Query("SELECT * FROM fixed_bills WHERE (year < :year) OR (year = :year AND month < :month) ORDER BY year DESC, month DESC")
    suspend fun getLatestHistoricalFixedBills(month: Int, year: Int): List<FixedBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedBill(bill: FixedBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedBills(bills: List<FixedBillEntity>)

    @Update
    suspend fun updateFixedBill(bill: FixedBillEntity)

    @Delete
    suspend fun deleteFixedBill(bill: FixedBillEntity)

    @Query("DELETE FROM fixed_bills WHERE category = :category AND isPaid = 0 AND ((year > :currentYear) OR (year = :currentYear AND month >= :currentMonth))")
    suspend fun deleteFutureUnpaidFixedBillsByCategory(category: String, currentMonth: Int, currentYear: Int)

    @Query("DELETE FROM fixed_bills WHERE category = :parentCategory AND subcategory = :subcategory AND isPaid = 0 AND ((year > :currentYear) OR (year = :currentYear AND month >= :currentMonth))")
    suspend fun deleteFutureUnpaidFixedBillsBySubcategory(parentCategory: String, subcategory: String, currentMonth: Int, currentYear: Int)

    @Query("DELETE FROM fixed_bills")
    suspend fun clearAllFixedBills()

    // --- BUDGET PLANS ---
    @Query("SELECT * FROM budget_plans WHERE month = :month AND year = :year")
    fun getBudgetPlansForMonth(month: Int, year: Int): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans")
    suspend fun getAllBudgetPlans(): List<BudgetPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPlan(plan: BudgetPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPlans(plans: List<BudgetPlanEntity>)

    @Query("DELETE FROM budget_plans WHERE category = :category")
    suspend fun deleteBudgetPlansForCategory(category: String)

    @Query("DELETE FROM budget_plans")
    suspend fun clearAllBudgetPlans()

    // --- ACCOUNTS & BALANCES ---
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE accountName = :name LIMIT 1")
    suspend fun getAccountByName(name: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Query("""
        SELECT 
            a.accountName, 
            a.startingBalance, 
            a.accountType, 
            a.sortOrder,
            (a.startingBalance 
             + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.accountName = a.accountName AND t.type = 'INCOME') OR (t.toAccountName = a.accountName AND t.type = 'TRANSFER')), 0.0)
             - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.accountName = a.accountName AND (t.type = 'EXPENSE' OR t.type = 'ASSET' OR t.type = 'TRANSFER'))), 0.0)
            ) AS currentBalance
        FROM accounts a
        ORDER BY a.sortOrder ASC
    """)
    fun getAccountBalances(): Flow<List<AccountBalanceResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Update
    suspend fun updateAccounts(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET startingBalance = :startingBalance WHERE accountName = :accountName")
    suspend fun updateAccountStartingBalance(accountName: String, startingBalance: Double)

    @Query("UPDATE accounts SET accountType = :accountType WHERE accountName = :accountName")
    suspend fun updateAccountType(accountName: String, accountType: String)

    @Transaction
    suspend fun reorderAccounts(orderedAccounts: List<AccountEntity>) {
        orderedAccounts.forEachIndexed { index, account ->
            insertAccount(account.copy(sortOrder = index))
        }
    }

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    // --- CATEGORIES & TAXONOMY ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    // --- SUBCATEGORIES ---
    @Query("SELECT * FROM subcategories ORDER BY name ASC")
    fun getAllSubcategories(): Flow<List<SubcategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: SubcategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategories(subcategories: List<SubcategoryEntity>)

    @Update
    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    @Delete
    suspend fun deleteSubcategory(subcategory: SubcategoryEntity)

    @Query("DELETE FROM subcategories WHERE parentCategory = :parentCategory")
    suspend fun deleteSubcategoriesForParent(parentCategory: String)

    @Query("DELETE FROM subcategories")
    suspend fun clearAllSubcategories()

    // --- CASCADING RENAMES & ADJUSTMENTS ---
    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun cascadeRenameCategoryInTransactions(oldName: String, newName: String)

    @Query("UPDATE budget_plans SET category = :newName WHERE category = :oldName")
    suspend fun cascadeRenameCategoryInBudgetPlans(oldName: String, newName: String)

    @Query("UPDATE fixed_bills SET category = :newName WHERE category = :oldName")
    suspend fun cascadeRenameCategoryInFixedBills(oldName: String, newName: String)

    @Query("UPDATE subcategories SET parentCategory = :newName WHERE parentCategory = :oldName")
    suspend fun cascadeRenameCategoryInSubcategories(oldName: String, newName: String)

    @Query("UPDATE transactions SET subcategory = :newName WHERE category = :parentCat AND subcategory = :oldName")
    suspend fun cascadeRenameSubcategoryInTransactions(parentCat: String, oldName: String, newName: String)

    @Query("UPDATE fixed_bills SET subcategory = :newName WHERE category = :parentCat AND subcategory = :oldName")
    suspend fun cascadeRenameSubcategoryInFixedBills(parentCat: String, oldName: String, newName: String)

    @Query("UPDATE transactions SET category = 'General', subcategory = 'General' WHERE category = :oldCategory")
    suspend fun reassignOrphanedTransactionsToGeneral(oldCategory: String)

    @Transaction
    suspend fun updateAccountAndCascade(
        oldName: String,
        newName: String,
        startingBalance: Double,
        accountType: String,
        sortOrder: Int
    ) {
        if (oldName != newName) {
            deleteAccount(AccountEntity(oldName))
            insertAccount(AccountEntity(newName, startingBalance, accountType, sortOrder))
            cascadeRenameAccountInTransactions(oldName, newName)
            cascadeRenameToAccountInTransactions(oldName, newName)
            cascadeRenameAccountInFixedBills(oldName, newName)
            cascadeRenameToAccountInFixedBills(oldName, newName)
        } else {
            insertAccount(AccountEntity(newName, startingBalance, accountType, sortOrder))
        }
    }

    @Query("UPDATE transactions SET accountName = :newName WHERE accountName = :oldName")
    suspend fun cascadeRenameAccountInTransactions(oldName: String, newName: String)

    @Query("UPDATE transactions SET toAccountName = :newName WHERE toAccountName = :oldName")
    suspend fun cascadeRenameToAccountInTransactions(oldName: String, newName: String)

    @Query("UPDATE fixed_bills SET accountName = :newName WHERE accountName = :oldName")
    suspend fun cascadeRenameAccountInFixedBills(oldName: String, newName: String)

    @Query("UPDATE fixed_bills SET toAccountName = :newName WHERE toAccountName = :oldName")
    suspend fun cascadeRenameToAccountInFixedBills(oldName: String, newName: String)

    // --- YEARLY AGGREGATIONS ---
    @Query("""
        SELECT 
            m.month,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0.0 END), 0.0) AS totalActualIncome,
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0.0 END), 0.0) AS totalActualExpense,
            COALESCE(SUM(CASE WHEN t.type = 'ASSET' THEN t.amount ELSE 0.0 END), 0.0) AS totalAsset
        FROM (
            SELECT 1 AS month UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 
            UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 
            UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
        ) m
        LEFT JOIN transactions t ON m.month = t.month AND t.year = :year AND t.type != 'TRANSFER'
        GROUP BY m.month
        ORDER BY m.month ASC
    """)
    fun getYearlySummary(year: Int): Flow<List<MonthlySummary>>

    @Query("""
        SELECT 
            category,
            type,
            SUM(amount) AS totalActualAmount
        FROM transactions
        WHERE year = :year AND type != 'TRANSFER'
        GROUP BY category, type
        ORDER BY totalActualAmount DESC
    """)
    fun getYearlyCategoryBreakdown(year: Int): Flow<List<YearlyCategoryRollup>>

    // --- USER PROFILE ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}
