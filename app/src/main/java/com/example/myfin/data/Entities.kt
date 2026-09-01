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
    val currentBalance: Double = 0.0,
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0
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
    val totalActualAmount: Double = 0.0,
    val totalAmount: Double = totalActualAmount
)

@Entity(
    tableName = "categories",
    primaryKeys = ["name", "type"]
)
data class CategoryEntity(
    val name: String,
    val type: TransactionType
) {
    companion object {
        val defaultCategories = listOf(
            // Expense Categories
            CategoryEntity("Utilities & Living Bills", TransactionType.EXPENSE),
            CategoryEntity("Everyday Living", TransactionType.EXPENSE),
            CategoryEntity("Leisure, Trips & Media", TransactionType.EXPENSE),
            CategoryEntity("Health & Medical", TransactionType.EXPENSE),
            CategoryEntity("Family & Home Support", TransactionType.EXPENSE),
            CategoryEntity("Debt & Financial Obligations", TransactionType.EXPENSE),
            CategoryEntity("Work & Professional", TransactionType.EXPENSE),
            CategoryEntity("General", TransactionType.EXPENSE),

            // Income Categories
            CategoryEntity("Salary & Professional Inflow", TransactionType.INCOME),
            CategoryEntity("Reimbursements & Corporate Inflow", TransactionType.INCOME),
            CategoryEntity("Passive & Capital Drawdowns", TransactionType.INCOME),

            // Asset / SIP Categories
            CategoryEntity("Investments & Wealth", TransactionType.ASSET),
            CategoryEntity("Liquid Reserves & Receivables", TransactionType.ASSET)
        )
    }
}

@Entity(
    tableName = "subcategories",
    primaryKeys = ["parentCategory", "name", "type"],
    indices = [
        Index(value = ["parentCategory"])
    ]
)
data class SubcategoryEntity(
    val parentCategory: String,
    val name: String,
    val type: TransactionType
) {
    companion object {
        val defaultSubcategories = listOf(
            // Utilities & Living Bills (Expense)
            SubcategoryEntity("Utilities & Living Bills", "Rent & Housing", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "PG Rent", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "Electricity & Water", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "Phone & Internet", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "Subscriptions & Cloud", TransactionType.EXPENSE),
            SubcategoryEntity("Utilities & Living Bills", "General", TransactionType.EXPENSE),

            // Everyday Living (Expense)
            SubcategoryEntity("Everyday Living", "Groceries", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Restaurants & Dining", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Daily Transit & Fuel", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Personal Care & Grooming", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Clothing & Apparel", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "Coffee, Tea & Snacks", TransactionType.EXPENSE),
            SubcategoryEntity("Everyday Living", "General", TransactionType.EXPENSE),

            // Leisure, Trips & Media (Expense)
            SubcategoryEntity("Leisure, Trips & Media", "Trips & Stays", TransactionType.EXPENSE),
            SubcategoryEntity("Leisure, Trips & Media", "Events & Outings", TransactionType.EXPENSE),
            SubcategoryEntity("Leisure, Trips & Media", "Media & Gaming", TransactionType.EXPENSE),
            SubcategoryEntity("Leisure, Trips & Media", "Books & Learning", TransactionType.EXPENSE),
            SubcategoryEntity("Leisure, Trips & Media", "General", TransactionType.EXPENSE),

            // Health & Medical (Expense)
            SubcategoryEntity("Health & Medical", "Doctors & Specialist Care", TransactionType.EXPENSE),
            SubcategoryEntity("Health & Medical", "Pharmacy & Medicines", TransactionType.EXPENSE),
            SubcategoryEntity("Health & Medical", "Emergency Care", TransactionType.EXPENSE),
            SubcategoryEntity("Health & Medical", "Health Insurance", TransactionType.EXPENSE),
            SubcategoryEntity("Health & Medical", "General", TransactionType.EXPENSE),

            // Family & Home Support (Expense)
            SubcategoryEntity("Family & Home Support", "Parents Support", TransactionType.EXPENSE),
            SubcategoryEntity("Family & Home Support", "Mom", TransactionType.EXPENSE),
            SubcategoryEntity("Family & Home Support", "Grandma", TransactionType.EXPENSE),
            SubcategoryEntity("Family & Home Support", "Life & General Insurance", TransactionType.EXPENSE),
            SubcategoryEntity("Family & Home Support", "Gifts & Donations", TransactionType.EXPENSE),
            SubcategoryEntity("Family & Home Support", "General", TransactionType.EXPENSE),

            // Debt & Financial Obligations (Expense)
            SubcategoryEntity("Debt & Financial Obligations", "Credit Cards & EMI", TransactionType.EXPENSE),
            SubcategoryEntity("Debt & Financial Obligations", "Pay Later", TransactionType.EXPENSE),
            SubcategoryEntity("Debt & Financial Obligations", "Taxes", TransactionType.EXPENSE),
            SubcategoryEntity("Debt & Financial Obligations", "Other Debts", TransactionType.EXPENSE),
            SubcategoryEntity("Debt & Financial Obligations", "General", TransactionType.EXPENSE),

            // Work & Professional (Expense)
            SubcategoryEntity("Work & Professional", "Work Travel", TransactionType.EXPENSE),
            SubcategoryEntity("Work & Professional", "Tools & Subscriptions", TransactionType.EXPENSE),
            SubcategoryEntity("Work & Professional", "Courier & Logistics", TransactionType.EXPENSE),
            SubcategoryEntity("Work & Professional", "General", TransactionType.EXPENSE),

            // General (Expense)
            SubcategoryEntity("General", "Miscellaneous", TransactionType.EXPENSE),
            SubcategoryEntity("General", "Cash Out", TransactionType.EXPENSE),

            // Salary & Professional Inflow (Income)
            SubcategoryEntity("Salary & Professional Inflow", "Base Salary (Pay Slip)", TransactionType.INCOME),
            SubcategoryEntity("Salary & Professional Inflow", "Bonus & Incentives", TransactionType.INCOME),
            SubcategoryEntity("Salary & Professional Inflow", "Commission & Freelance", TransactionType.INCOME),
            SubcategoryEntity("Salary & Professional Inflow", "General", TransactionType.INCOME),

            // Reimbursements & Corporate Inflow (Income)
            SubcategoryEntity("Reimbursements & Corporate Inflow", "Travel Advances & Claims", TransactionType.INCOME),
            SubcategoryEntity("Reimbursements & Corporate Inflow", "Loan Paybacks Received", TransactionType.INCOME),
            SubcategoryEntity("Reimbursements & Corporate Inflow", "Tax & Purchase Refunds", TransactionType.INCOME),
            SubcategoryEntity("Reimbursements & Corporate Inflow", "General", TransactionType.INCOME),

            // Passive & Capital Drawdowns (Income)
            SubcategoryEntity("Passive & Capital Drawdowns", "Interest & Dividends", TransactionType.INCOME),
            SubcategoryEntity("Passive & Capital Drawdowns", "Capital Gains / Realization", TransactionType.INCOME),
            SubcategoryEntity("Passive & Capital Drawdowns", "Emergency Fund Drawdown", TransactionType.INCOME),
            SubcategoryEntity("Passive & Capital Drawdowns", "FD / Deposit Maturity", TransactionType.INCOME),
            SubcategoryEntity("Passive & Capital Drawdowns", "General", TransactionType.INCOME),

            // Investments & Wealth (Asset / SIP)
            SubcategoryEntity("Investments & Wealth", "Mutual Funds (MF)", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "Fixed Deposits (FD)", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "Recurring Deposits (RD)", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "Digital Gold & Commodities", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "Bonds & Securities", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "PMS & Equity", TransactionType.ASSET),
            SubcategoryEntity("Investments & Wealth", "General", TransactionType.ASSET),

            // Liquid Reserves & Receivables (Asset / SIP)
            SubcategoryEntity("Liquid Reserves & Receivables", "Emergency Fund", TransactionType.ASSET),
            SubcategoryEntity("Liquid Reserves & Receivables", "Bank Savings", TransactionType.ASSET),
            SubcategoryEntity("Liquid Reserves & Receivables", "Personal Loans (Friends & Family)", TransactionType.ASSET),
            SubcategoryEntity("Liquid Reserves & Receivables", "NPA / Bad Debt Write-off", TransactionType.ASSET),
            SubcategoryEntity("Liquid Reserves & Receivables", "General", TransactionType.ASSET)
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
