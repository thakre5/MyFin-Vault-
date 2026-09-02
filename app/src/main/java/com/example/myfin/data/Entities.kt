package com.example.myfin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

@Entity(
    tableName = "fixed_bills",
    indices = [
        Index(value = ["title", "month", "year"]),
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
    val vaultMode: String = "3-VAULT"
)
