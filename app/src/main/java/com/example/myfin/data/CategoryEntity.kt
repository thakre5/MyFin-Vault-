package com.example.myfin.data

import androidx.room.Entity

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
            // 1. EXPENSE CATEGORIES (Personal Living Burn)
            CategoryEntity("Utilities & Living Bills", TransactionType.EXPENSE),
            CategoryEntity("Everyday Living", TransactionType.EXPENSE),
            CategoryEntity("Leisure, Trips & Media", TransactionType.EXPENSE),
            CategoryEntity("Health & Medical", TransactionType.EXPENSE),
            CategoryEntity("Family & Home Support", TransactionType.EXPENSE),
            CategoryEntity("Debt & Financial Obligations", TransactionType.EXPENSE),
            CategoryEntity("General", TransactionType.EXPENSE),

            // 2. INCOME CATEGORIES (True Personal Inflows)
            CategoryEntity("Salary & Professional Inflow", TransactionType.INCOME),
            CategoryEntity("Passive & Capital Drawdowns", TransactionType.INCOME),
            CategoryEntity("Refunds & Recoveries", TransactionType.INCOME),

            // 3. ASSET / SIP CATEGORIES (Wealth Creation & Capital Stock)
            CategoryEntity("Investments & Wealth", TransactionType.ASSET),
            CategoryEntity("Liquid Reserves & Receivables", TransactionType.ASSET),

            // 4. CORPORATE CATEGORIES (Isolated Employer Float)
            CategoryEntity("Work & Professional", TransactionType.CORPORATE),
            CategoryEntity("Reimbursements & Claims", TransactionType.CORPORATE)
        )
    }
}
