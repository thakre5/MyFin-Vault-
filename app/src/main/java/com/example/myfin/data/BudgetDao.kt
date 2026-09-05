package com.example.myfin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // ========================================================================
    // 1. User Profile & Settings
    // ========================================================================
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // ========================================================================
    // 2. Liquid Vault Accounts
    // ========================================================================
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, sortOrder ASC, accountName ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, sortOrder ASC, accountName ASC")
    suspend fun getAllAccountsDirect(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, accountName ASC")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 1 ORDER BY sortOrder ASC, accountName ASC")
    fun getArchivedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE accountName = :name COLLATE NOCASE LIMIT 1")
    suspend fun getAccountByName(name: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE accountName = :name COLLATE NOCASE")
    suspend fun deleteAccountByName(name: String)

    @Query("UPDATE accounts SET isArchived = 1 WHERE accountName = :name COLLATE NOCASE")
    suspend fun archiveAccount(name: String)

    @Query("UPDATE accounts SET isArchived = 0 WHERE accountName = :name COLLATE NOCASE")
    suspend fun unarchiveAccount(name: String)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    @Query("SELECT COUNT(*) FROM transactions WHERE accountName = :accName COLLATE NOCASE OR toAccountName = :accName COLLATE NOCASE")
    suspend fun getTransactionCountForAccount(accName: String): Int

    @Query("SELECT COUNT(*) FROM fixed_bills WHERE accountName = :accName COLLATE NOCASE OR toAccountName = :accName COLLATE NOCASE")
    suspend fun getLinkedBillCountForAccount(accName: String): Int

    @Query("""
        SELECT 
            a.accountName, 
            a.startingBalance, 
            a.accountType,
            COALESCE(a.minBalance, 0.0) AS minBalance,
            COALESCE(a.isArchived, 0) AS isArchived,
            a.sortOrder,
            (a.startingBalance + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'INCOME' AND t.accountName = a.accountName COLLATE NOCASE), 0.0) - 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' OR t.type = 'ASSET') AND t.accountName = a.accountName COLLATE NOCASE), 0.0) + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.toAccountName = a.accountName COLLATE NOCASE), 0.0) - 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.accountName = a.accountName COLLATE NOCASE), 0.0)
            ) AS currentBalance,
            (COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'INCOME' AND t.accountName = a.accountName COLLATE NOCASE), 0.0) + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.toAccountName = a.accountName COLLATE NOCASE), 0.0)
            ) AS totalInflow,
            (COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' OR t.type = 'ASSET') AND t.accountName = a.accountName COLLATE NOCASE), 0.0) + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.accountName = a.accountName COLLATE NOCASE), 0.0)
            ) AS totalOutflow
        FROM accounts a
        ORDER BY a.isArchived ASC, a.sortOrder ASC, a.accountName ASC
    """)
    fun getAccountBalances(): Flow<List<AccountBalanceResult>>

    @Query("""
        SELECT 
            a.accountName, 
            a.startingBalance, 
            a.accountType,
            COALESCE(a.minBalance, 0.0) AS minBalance,
            COALESCE(a.isArchived, 0) AS isArchived,
            a.sortOrder,
            (a.startingBalance + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'INCOME' AND t.accountName = a.accountName COLLATE NOCASE), 0.0) - 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' OR t.type = 'ASSET') AND t.accountName = a.accountName COLLATE NOCASE), 0.0) + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.toAccountName = a.accountName COLLATE NOCASE), 0.0) - 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.accountName = a.accountName COLLATE NOCASE), 0.0)
            ) AS currentBalance,
            (COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'INCOME' AND t.accountName = a.accountName COLLATE NOCASE), 0.0) + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.toAccountName = a.accountName COLLATE NOCASE), 0.0)
            ) AS totalInflow,
            (COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE (t.type = 'EXPENSE' OR t.type = 'ASSET') AND t.accountName = a.accountName COLLATE NOCASE), 0.0) + 
             COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'TRANSFER' AND t.accountName = a.accountName COLLATE NOCASE), 0.0)
            ) AS totalOutflow
        FROM accounts a
        WHERE a.isArchived = 0
        ORDER BY a.sortOrder ASC, a.accountName ASC
    """)
    fun getActiveAccountBalances(): Flow<List<AccountBalanceResult>>

    // ========================================================================
    // 3. Categories & Subcategories
    // ========================================================================
    @Query("SELECT * FROM categories ORDER BY type ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, name ASC")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE name = :name AND type = :type")
    suspend fun deleteCategoryByNameAndType(name: String, type: TransactionType)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Query("SELECT * FROM subcategories ORDER BY parentCategory ASC, name ASC")
    fun getAllSubcategories(): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategories ORDER BY parentCategory ASC, name ASC")
    suspend fun getAllSubcategoriesDirect(): List<SubcategoryEntity>

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

    @Query("DELETE FROM subcategories WHERE parentCategory = :parentCategory AND name = :name AND type = :type")
    suspend fun deleteSubcategoryByKeys(parentCategory: String, name: String, type: TransactionType)

    @Query("DELETE FROM subcategories")
    suspend fun clearAllSubcategories()

    // ========================================================================
    // 4. Fixed Bills / Commitments
    // ========================================================================
    @Query("SELECT * FROM fixed_bills WHERE month = :month AND year = :year ORDER BY isPaid ASC, dueDay ASC, title ASC")
    fun getFixedBillsForMonth(month: Int, year: Int): Flow<List<FixedBillEntity>>

    @Query("SELECT * FROM fixed_bills WHERE month = :month AND year = :year ORDER BY isPaid ASC, dueDay ASC, title ASC")
    suspend fun getFixedBillsForMonthDirect(month: Int, year: Int): List<FixedBillEntity>

    @Query("SELECT * FROM fixed_bills ORDER BY year DESC, month DESC, id ASC")
    suspend fun getAllFixedBills(): List<FixedBillEntity>

    @Query("SELECT * FROM fixed_bills WHERE id = :id LIMIT 1")
    suspend fun getFixedBillById(id: Long): FixedBillEntity?

    @Query("SELECT * FROM fixed_bills WHERE month = :month AND year = :year AND type = :type AND category = :category AND subcategory = :subcategory LIMIT 1")
    suspend fun getFixedBillByKeys(month: Int, year: Int, type: TransactionType, category: String, subcategory: String): FixedBillEntity?

    @Query("""
        SELECT * FROM fixed_bills 
        WHERE month = :month AND year = :year 
          AND type = :type 
          AND category = :category 
          AND subcategory = :subcategory 
          AND (title = :title OR (:title = '' AND (title = subcategory OR title = '')))
          AND accountName = :accountName 
        LIMIT 1
    """)
    suspend fun getFixedBillByTemplateKeys(
        month: Int,
        year: Int,
        type: TransactionType,
        category: String,
        subcategory: String,
        title: String,
        accountName: String
    ): FixedBillEntity?

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

    @Query("DELETE FROM fixed_bills")
    suspend fun clearAllFixedBills()

    @Query("DELETE FROM fixed_bills WHERE category = :category AND isPaid = 0 AND ((year > :currentYear) OR (year = :currentYear AND month >= :currentMonth))")
    suspend fun deleteFutureUnpaidFixedBillsByCategory(category: String, currentMonth: Int, currentYear: Int)

    @Query("DELETE FROM fixed_bills WHERE category = :category AND subcategory = :subcategory AND isPaid = 0 AND ((year > :currentYear) OR (year = :currentYear AND month >= :currentMonth))")
    suspend fun deleteFutureUnpaidFixedBillsBySubcategory(category: String, subcategory: String, currentMonth: Int, currentYear: Int)

    // ========================================================================
    // 5. Budget Plans
    // ========================================================================
    @Query("SELECT * FROM budget_plans WHERE month = :month AND year = :year ORDER BY category ASC")
    fun getBudgetPlansForMonth(month: Int, year: Int): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans WHERE month = :month AND year = :year ORDER BY category ASC")
    suspend fun getBudgetPlansForMonthDirect(month: Int, year: Int): List<BudgetPlanEntity>

    @Query("SELECT * FROM budget_plans ORDER BY year DESC, month DESC, category ASC")
    suspend fun getAllBudgetPlans(): List<BudgetPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPlan(plan: BudgetPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPlans(plans: List<BudgetPlanEntity>)

    @Query("DELETE FROM budget_plans WHERE category = :category")
    suspend fun deleteBudgetPlansForCategory(category: String)

    @Query("DELETE FROM budget_plans")
    suspend fun clearAllBudgetPlans()

    // ========================================================================
    // 6. Transactions
    // ========================================================================
    @Query("SELECT * FROM transactions WHERE month = :month AND year = :year ORDER BY date DESC, id DESC")
    fun getTransactionsForMonth(month: Int, year: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE linkedFixedBillId = :billId LIMIT 1")
    suspend fun getTransactionByLinkedBill(billId: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE linkedFixedBillId = :billId")
    suspend fun deleteTransactionByLinkedBill(billId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // ========================================================================
    // 7. Cascading Rename & Cleanup Operations
    // ========================================================================
    @Query("UPDATE transactions SET accountName = :newName WHERE accountName = :oldName COLLATE NOCASE")
    suspend fun cascadeRenameAccountInTransactions(oldName: String, newName: String)

    @Query("UPDATE transactions SET toAccountName = :newName WHERE toAccountName = :oldName COLLATE NOCASE")
    suspend fun cascadeRenameToAccountInTransactions(oldName: String, newName: String)

    @Query("UPDATE fixed_bills SET accountName = :newName WHERE accountName = :oldName COLLATE NOCASE")
    suspend fun cascadeRenameAccountInFixedBills(oldName: String, newName: String)

    @Query("UPDATE fixed_bills SET toAccountName = :newName WHERE toAccountName = :oldName COLLATE NOCASE")
    suspend fun cascadeRenameToAccountInFixedBills(oldName: String, newName: String)

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

    @Query("UPDATE fixed_bills SET category = 'General', subcategory = 'General' WHERE category = :oldCategory")
    suspend fun reassignOrphanedFixedBillsToGeneral(oldCategory: String)

    // ========================================================================
    // 8. Atomic Transactions for Safe Migrations & Reordering
    // ========================================================================
    @Transaction
    suspend fun updateAccountAndCascade(
        oldName: String,
        newName: String,
        startingBalance: Double,
        accountType: String,
        minBalance: Double = 0.0,
        isArchived: Boolean = false,
        sortOrder: Int = 0
    ) {
        val cleanOldName = oldName.trim()
        val cleanNewName = newName.trim().uppercase()

        val updatedAccount = AccountEntity(
            accountName = cleanNewName,
            startingBalance = startingBalance,
            accountType = accountType,
            minBalance = minBalance,
            isArchived = isArchived,
            sortOrder = sortOrder
        )

        if (cleanOldName == cleanNewName) {
            insertAccount(updatedAccount)
        } else {
            insertAccount(updatedAccount)
            cascadeRenameAccountInTransactions(cleanOldName, cleanNewName)
            cascadeRenameToAccountInTransactions(cleanOldName, cleanNewName)
            cascadeRenameAccountInFixedBills(cleanOldName, cleanNewName)
            cascadeRenameToAccountInFixedBills(cleanOldName, cleanNewName)
            deleteAccountByName(cleanOldName)
        }
    }

    @Transaction
    suspend fun updateCategoryAndCascade(
        oldCategory: CategoryEntity,
        newName: String
    ) {
        val trimmedNew = newName.trim()
        if (oldCategory.name == trimmedNew) return

        insertCategory(CategoryEntity(name = trimmedNew, type = oldCategory.type))
        cascadeRenameCategoryInTransactions(oldCategory.name, trimmedNew)
        cascadeRenameCategoryInBudgetPlans(oldCategory.name, trimmedNew)
        cascadeRenameCategoryInFixedBills(oldCategory.name, trimmedNew)
        cascadeRenameCategoryInSubcategories(oldCategory.name, trimmedNew)
        deleteCategory(oldCategory)
    }

    @Transaction
    suspend fun deleteCategoryAndCascade(category: CategoryEntity) {
        reassignOrphanedTransactionsToGeneral(category.name)
        reassignOrphanedFixedBillsToGeneral(category.name)
        deleteSubcategoriesForParent(category.name)
        deleteBudgetPlansForCategory(category.name)
        deleteCategory(category)
    }

    @Transaction
    suspend fun updateSubcategoryAndCascade(
        oldSubcategory: SubcategoryEntity,
        newName: String
    ) {
        val trimmedNew = newName.trim()
        if (oldSubcategory.name == trimmedNew) return

        insertSubcategory(
            SubcategoryEntity(
                parentCategory = oldSubcategory.parentCategory,
                name = trimmedNew,
                type = oldSubcategory.type
            )
        )
        cascadeRenameSubcategoryInTransactions(oldSubcategory.parentCategory, oldSubcategory.name, trimmedNew)
        cascadeRenameSubcategoryInFixedBills(oldSubcategory.parentCategory, oldSubcategory.name, trimmedNew)
        deleteSubcategory(oldSubcategory)
    }

    @Transaction
    suspend fun reorderAccounts(orderedAccounts: List<AccountEntity>) {
        val updated = orderedAccounts.mapIndexed { index, account ->
            account.copy(sortOrder = index)
        }
        insertAccounts(updated)
    }

    // ========================================================================
    // 9. Yearly Analytics Aggregations
    // ========================================================================
    @Query("""
        SELECT 
            m.month,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0.0) AS totalActualIncome,
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0.0) AS totalActualExpense,
            COALESCE(SUM(CASE WHEN t.type = 'ASSET' THEN t.amount ELSE 0 END), 0.0) AS totalAsset
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
        SELECT category, type, SUM(amount) AS totalActualAmount, SUM(amount) AS totalAmount 
        FROM transactions 
        WHERE year = :year AND type != 'TRANSFER' 
        GROUP BY category, type 
        ORDER BY totalActualAmount DESC
    """)
    fun getYearlyCategoryBreakdown(year: Int): Flow<List<YearlyCategoryRollup>>
}
