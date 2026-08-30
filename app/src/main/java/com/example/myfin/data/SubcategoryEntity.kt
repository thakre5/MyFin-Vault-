package com.example.myfin.data

import androidx.room.Entity

@Entity(
    tableName = "subcategories",
    primaryKeys = ["parentCategory", "name", "type"]
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
