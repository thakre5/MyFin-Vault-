    // --- BUDGET PLANS ---
    @Query("SELECT * FROM budget_plans WHERE month = :month AND year = :year")
    fun getBudgetPlansForMonth(month: Int, year: Int): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans WHERE month = :month AND year = :year AND category = :category LIMIT 1")
    suspend fun getBudgetPlan(month: Int, year: Int, category: String): BudgetPlanEntity?

    @Query("SELECT * FROM budget_plans")
    suspend fun getAllBudgetPlans(): List<BudgetPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPlan(plan: BudgetPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPlans(plans: List<BudgetPlanEntity>)

    @Update
    suspend fun updateBudgetPlan(plan: BudgetPlanEntity)

    @Query("DELETE FROM budget_plans WHERE category = :category")
    suspend fun deleteBudgetPlansForCategory(category: String)

    @Query("DELETE FROM budget_plans")
    suspend fun clearAllBudgetPlans()
