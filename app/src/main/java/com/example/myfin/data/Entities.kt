package com.example.myfin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val linkedFixedBillId: Long? = null
)

@Entity(tableName = "fixed_bills")
data class FixedBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val subcategory: String = "General",
    val accountName: String,
    val toAccountName: String? = null,
    val type: TransactionType,
    val isPaid: Boolean = false,
    val dueDay: Int? = null,
    val month: Int,
    val year: Int
)

@Entity(tableName = "budget_plans")
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val plannedAmount: Double,
    val type: TransactionType,
    val month: Int,
    val year: Int
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val accountName: String,
    val startingBalance: Double = 0.0,
    val accountType: String = "Bank",
    val sortOrder: Int = 0
)

@Entity(tableName = "categories", primaryKeys = ["name", "type"])
data class CategoryEntity(
    val name: String,
    val type: TransactionType
)

@Entity(tableName = "subcategories", primaryKeys = ["parentCategory", "name", "type"])
data class SubcategoryEntity(
    val parentCategory: String,
    val name: String,
    val type: TransactionType
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val displayName: String = "Sushant",
    val email: String = "",
    val dateOfBirth: String = "2000-03-21",
    val baseMonthlyIncome: Double = 0.0,
    val currencySymbol: String = "₹",
    val profileImageUri: String? = null,
    val coverImageUri: String? = null,
    val fortressThreshold: Double = 100000.0,
    val isOnboardingCompleted: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isScreenCaptureAllowed: Boolean = false,
    val isAutoPayReminderEnabled: Boolean = true,
    val isOverrunWarningEnabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)
