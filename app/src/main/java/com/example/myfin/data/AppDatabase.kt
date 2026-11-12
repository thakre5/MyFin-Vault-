package com.example.myfin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Calendar

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        FixedBillEntity::class,
        BudgetPlanEntity::class,
        UserProfile::class,
        NoteEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN minBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferSubtype TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN vaultMode TEXT NOT NULL DEFAULT '3-VAULT'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isArchived ON accounts(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_sortOrder ON accounts(sortOrder)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_linkedFixedBillId ON transactions(linkedFixedBillId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isLegacy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN isNew INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subcategories ADD COLUMN isLegacy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subcategories ADD COLUMN isNew INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE user_profile ADD COLUMN fortressSweepThreshold REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN fortressEmergencyMonths INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN fortressManualTarget REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN taxonomyGraceMonth INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN taxonomyGraceYear INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isTaxonomyBannerDismissed INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN initialReimbursementClaim REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN initialCompanyAdvance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fixed_bills_isPaid ON fixed_bills(isPaid)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL DEFAULT '',
                        `tag` TEXT NOT NULL DEFAULT 'General',
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `isChecklist` INTEGER NOT NULL DEFAULT 0,
                        `checklistJson` TEXT NOT NULL DEFAULT '[]',
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_tag` ON `notes` (`tag`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_accountName_type_category` ON `transactions` (`accountName`, `type`, `category`)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myfin_vault.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedMasterTaxonomy(db, forceSeed = true)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            try {
                                db.setForeignKeyConstraintsEnabled(true)
                            } catch (_: Exception) {
                                db.execSQL("PRAGMA foreign_keys = ON;")
                            }
                            seedMasterTaxonomy(db, forceSeed = false)
                        }

                        private fun seedMasterTaxonomy(db: SupportSQLiteDatabase, forceSeed: Boolean) {
                            db.beginTransaction()
                            try {
                                var needsInitialTaxonomySeed = forceSeed
                                if (!needsInitialTaxonomySeed) {
                                    // Check if taxonomy grace period was already initialized or if categories table is empty
                                    db.query("SELECT taxonomyGraceMonth FROM user_profile WHERE id = 1 LIMIT 1").use { cursor ->
                                        needsInitialTaxonomySeed = if (cursor.moveToFirst()) {
                                            cursor.getInt(0) == -1
                                        } else {
                                            true
                                        }
                                    }
                                    if (!needsInitialTaxonomySeed) {
                                        db.query("SELECT COUNT(*) FROM categories").use { cursor ->
                                            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                                                needsInitialTaxonomySeed = true
                                            }
                                        }
                                    }
                                }

                                if (needsInitialTaxonomySeed) {
                                    db.execSQL("""
                                        UPDATE categories 
                                        SET isLegacy = 1 
                                        WHERE (name = 'Work & Professional' AND type = 'EXPENSE')
                                           OR (name = 'Reimbursements & Corporate Inflow' AND type = 'INCOME')
                                    """)
                                    db.execSQL("""
                                        UPDATE subcategories 
                                        SET isLegacy = 1 
                                        WHERE (parentCategory = 'Work & Professional' AND type = 'EXPENSE')
                                           OR (parentCategory = 'Reimbursements & Corporate Inflow' AND type = 'INCOME')
                                    """)

                                    CategoryEntity.defaultCategories.forEach { category ->
                                        val catNameEscaped = category.name.replace("'", "''")
                                        val isNewInt = if (category.isNew) 1 else 0
                                        db.execSQL(
                                            "INSERT OR IGNORE INTO categories (name, type, isLegacy, isNew) VALUES ('$catNameEscaped', '${category.type.name}', 0, $isNewInt)"
                                        )
                                    }

                                    SubcategoryEntity.defaultSubcategories.forEach { subcategory ->
                                        val parentEscaped = subcategory.parentCategory.replace("'", "''")
                                        val subNameEscaped = subcategory.name.replace("'", "''")
                                        val isNewInt = if (subcategory.isNew) 1 else 0
                                        db.execSQL(
                                            "INSERT OR IGNORE INTO subcategories (parentCategory, name, type, isLegacy, isNew) VALUES ('$parentEscaped', '$subNameEscaped', '${subcategory.type.name}', 0, $isNewInt)"
                                        )
                                    }
                                }

                                val cal = Calendar.getInstance()
                                val curMonth = cal.get(Calendar.MONTH) + 1
                                val curYear = cal.get(Calendar.YEAR)

                                db.execSQL("""
                                    INSERT OR IGNORE INTO user_profile (
                                        id, displayName, email, dateOfBirth, baseMonthlyIncome, currencySymbol,
                                        isOnboardingCompleted, isBiometricEnabled, isScreenCaptureAllowed,
                                        isAutoPayReminderEnabled, isOverrunWarningEnabled, reminderEnabled,
                                        reminderHour, reminderMinute, vaultMode, fortressSweepThreshold,
                                        fortressEmergencyMonths, fortressManualTarget, taxonomyGraceMonth,
                                        taxonomyGraceYear, isTaxonomyBannerDismissed, initialReimbursementClaim,
                                        initialCompanyAdvance
                                    ) VALUES (
                                        1, 'Admin Vault', '', '', 0.0, '₹',
                                        0, 0, 0,
                                        1, 1, 0,
                                        20, 30, '3-VAULT', 0.0,
                                        6, 0.0, $curMonth,
                                        $curYear, 0, 0.0,
                                        0.0
                                    )
                                """)

                                db.execSQL("""
                                    UPDATE user_profile 
                                    SET taxonomyGraceMonth = $curMonth, 
                                        taxonomyGraceYear = $curYear 
                                    WHERE taxonomyGraceMonth = -1
                                """)

                                db.setTransactionSuccessful()
                            } finally {
                                db.endTransaction()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
