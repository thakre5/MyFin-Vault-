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
