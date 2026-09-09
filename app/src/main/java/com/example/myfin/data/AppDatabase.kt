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
        UserProfile::class
    ],
    version = 4,
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
                db.execSQL("ALTER TABLE user_profile ADD COLUMN fortressThreshold REAL NOT NULL DEFAULT 0.0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isArchived ON accounts(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_sortOrder ON accounts(sortOrder)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_linkedFixedBillId ON transactions(linkedFixedBillId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Categories & Subcategories lifecycle flags
                db.execSQL("ALTER TABLE categories ADD COLUMN isLegacy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN isNew INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subcategories ADD COLUMN isLegacy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subcategories ADD COLUMN isNew INTEGER NOT NULL DEFAULT 0")

                // 2. UserProfile: Fortress dual-target & taxonomy transition flags
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
                // 1. Corporate float reconciliation fields
                db.execSQL("ALTER TABLE user_profile ADD COLUMN initialReimbursementClaim REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN initialCompanyAdvance REAL NOT NULL DEFAULT 0.0")

                // 2. Index on FixedBillEntity.isPaid
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fixed_bills_isPaid ON fixed_bills(isPaid)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myfin_vault.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedMasterTaxonomy(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            try {
                                db.setForeignKeyConstraintsEnabled(true)
                            } catch (_: Exception) {
                                db.execSQL("PRAGMA foreign_keys = ON;")
                            }
                            seedMasterTaxonomy(db)
                        }

                        private fun seedMasterTaxonomy(db: SupportSQLiteDatabase) {
                            db.beginTransaction()
                            try {
                                // 1. Flag known obsolete default categories as legacy
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

                                // 2. Upsert standard default categories
                                CategoryEntity.defaultCategories.forEach { category ->
                                    val catNameEscaped = category.name.replace("'", "''")
                                    val isNewInt = if (category.isNew) 1 else 0
                                    db.execSQL(
                                        "INSERT OR IGNORE INTO categories (name, type, isLegacy, isNew) VALUES ('$catNameEscaped', '${category.type.name}', 0, $isNewInt)"
                                    )
                                }

                                // 3. Upsert standard default subcategories
                                SubcategoryEntity.defaultSubcategories.forEach { subcategory ->
                                    val parentEscaped = subcategory.parentCategory.replace("'", "''")
                                    val subNameEscaped = subcategory.name.replace("'", "''")
                                    val isNewInt = if (subcategory.isNew) 1 else 0
                                    db.execSQL(
                                        "INSERT OR IGNORE INTO subcategories (parentCategory, name, type, isLegacy, isNew) VALUES ('$parentEscaped', '$subNameEscaped', '${subcategory.type.name}', 0, $isNewInt)"
                                    )
                                }

                                // 4. Initialize default user profile if absent and stamp transition window
                                val cal = Calendar.getInstance()
                                val curMonth = cal.get(Calendar.MONTH) + 1
                                val curYear = cal.get(Calendar.YEAR)

                                db.execSQL("""
                                    INSERT OR IGNORE INTO user_profile (id, displayName, taxonomyGraceMonth, taxonomyGraceYear)
                                    VALUES (1, 'Admin Vault', $curMonth, $curYear)
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
