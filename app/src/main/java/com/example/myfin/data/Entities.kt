package com.example.myfin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["month", "year"]),
        Index(value = ["accountName"]),
        Index(value = ["toAccountName"]),
        Index(value = ["linkedFixedBillId"])
    ]
)
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

@Entity(
    tableName = "fixed_bills",
    indices = [
        Index(value = ["month", "year"])
    ]
)
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

@Entity(
    tableName = "budget_plans",
    indices = [
        Index(value = ["category", "type", "month", "year"], unique = true)
    ]
)
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
    val accountType: String = "Operating",
    val sortOrder: Int = 0
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val displayName: String = "Vault User",
    val email: String = "",
    val dateOfBirth: String = "",
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
    val reminderMinute: Int = 0,
    val vaultMode: String = "3-VAULT" // "3-VAULT" or "SIMPLE"
)
