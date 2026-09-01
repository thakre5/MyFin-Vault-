package com.example.myfin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    ASSET,
    TRANSFER
}

enum class TransferSubtype {
    NONE,
    BILL_FUNDING,
    WEALTH_ALLOCATION,
    REBALANCE,
    CASH_WITHDRAWAL
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["month", "year"]),
        Index(value = ["date"]),
        Index(value = ["accountName"]),
        Index(value = ["toAccountName"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["linkedFixedBillId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val subcategory: String = "General",
    val accountName: String,
    val toAccountName: String? = null,
    val type: TransactionType,
    val date: Long = System.currentTimeMillis(),
    val month: Int,
    val year: Int,
    val linkedFixedBillId: Long? = null,
    val transferSubtype: TransferSubtype = TransferSubtype.NONE
)

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["sortOrder"])
    ]
)
data class AccountEntity(
    @PrimaryKey val accountName: String,
    val startingBalance: Double,
    val accountType: String = "Operating",
    val minBalance: Double = 0.0,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

data class AccountBalanceResult(
    val accountName: String,
    val startingBalance: Double,
    val accountType: String,
    val minBalance: Double = 0.0,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val currentBalance: Double = 0.0
)

data class MonthlySummary(
    val month: Int,
    val totalActualIncome: Double,
    val totalActualExpense: Double,
    val totalAsset: Double,
    val netSurplus: Double = totalActualIncome - totalActualExpense
)

data class YearlyCategoryRollup(
    val category: String,
    val type: TransactionType,
    val totalAmount: Double
)

@Entity(tableName = "master_categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val type: TransactionType
) {
    companion object {
        val defaultCategories = listOf(
            CategoryEntity("Utilities & Living Bills", TransactionType.EXPENSE),
            CategoryEntity("Everyday Living", TransactionType.EXPENSE),
            CategoryEntity("Leisure, Trips & Media", TransactionType.EXPENSE),
            CategoryEntity("Health & Medical", TransactionType.EXPENSE),
            CategoryEntity("Family & Home Support", TransactionType.EXPENSE),
            CategoryEntity("Debt & Financial Obligations", TransactionType.EXPENSE),
            CategoryEntity("Work & Professional", TransactionType.EXPENSE),
            CategoryEntity("General", TransactionType.EXPENSE),
            CategoryEntity("Investments & Wealth", TransactionType.ASSET),
            CategoryEntity("Liquid Reserves & Receivables", TransactionType.ASSET),
            CategoryEntity("Salary & Professional Inflow", TransactionType.INCOME),
            CategoryEntity("Reimbursements & Corporate Inflow", TransactionType.INCOME),
            CategoryEntity("Passive & Capital Drawdowns", TransactionType.INCOME)
        )
    }
}

@Entity(
    tableName = "master_subcategories",
    indices = [
        Index(value = ["parentCategory", "name", "type"], unique = true),
        Index(value = ["parentCategory"])
    ]
)
data class SubcategoryEntity(
    val parentCategory: String,
    val name: String,
    val type: TransactionType,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) {
    companion object {
        val defaultSubcategories = listOf(
            // Utilities & Living Bills
            SubcategoryEntity("Utilities & Living Bills", "Electricity & Water", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "PG Rent", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "House Rent", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "Broadband & WiFi", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "Mobile Recharges", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "Gas Cylinder / Pipeline", TransactionType.EXPENSE),
            // Everyday Living
            SubcategoryEntity("Everyday Living", "Groceries & Supermarket", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Dining Out & Cafes", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Fuel & Vehicle Maintenance", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Daily Commute & Cabs", TransactionType.EXPENSE),
            // Debt & Financial Obligations
            SubcategoryEntity("Debt & Financial Obligations", "Credit Cards & EMI", TransactionType.EXPENSE),
            SubcategoryEntity("Debt & Financial Obligations", "Personal Loan EMI", TransactionType.EXPENSE),
            // Investments & Wealth
            SubcategoryEntity("Investments & Wealth", "Mutual Funds (MF)", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "Stock Market (Equities)", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "Fixed Deposits (FD)", TransactionType.ASSET),
            // Liquid Reserves & Receivables
            SubcategoryEntity("Liquid Reserves & Receivables", "Emergency Fund", TransactionType.ASSET),
            // Salary & Professional Inflow
            SubcategoryEntity("Salary & Professional Inflow", "Base Salary (Pay Slip)", TransactionType.INCOME),
            SubcategoryEntity("Salary & Professional Inflow", "Freelance / Consulting", TransactionType.INCOME),
            SubcategoryEntity("Salary & Professional Inflow", "Bonus / Incentive", TransactionType.INCOME),
            // General
            SubcategoryEntity("General", "Adjustment", TransactionType.EXPENSE)
        )
    }
}

@Entity(
    tableName = "fixed_bills",
    indices = [
        Index(value = ["title", "month", "year"], unique = true),
        Index(value = ["month", "year"]),
        Index(value = ["accountName"]),
        Index(value = ["category"])
    ]
)
data class FixedBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val subcategory: String = "General",
    val accountName: String,
    val toAccountName: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val isPaid: Boolean = false,
    val dueDay: Int? = null,
    val month: Int,
    val year: Int
)

@Entity(
    tableName = "budget_plans",
    indices = [
        Index(value = ["category", "month", "year", "type"], unique = true),
        Index(value = ["month", "year"])
    ]
)
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val plannedAmount: Double,
    val type: TransactionType,
    val month: Int,
    val year: Int
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Admin Vault",
    val email: String = "",
    val dateOfBirth: String = "",
    val baseMonthlyIncome: Double = 0.0,
    val currencySymbol: String = "₹",
    val profileImageUri: String? = null,
    val coverImageUri: String? = null,
    val isOnboardingCompleted: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isScreenCaptureAllowed: Boolean = false,
    val isAutoPayReminderEnabled: Boolean = true,
    val isOverrunWarningEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 30,
    val fortressThreshold: Double = 25000.0,
    val vaultMode: String = "3_VAULT"
)
