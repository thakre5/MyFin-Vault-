package com.example.myfin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Non-destructive Migration: Version 1 -> Version 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add minBalance and isArchived columns to accounts
                db.execSQL("ALTER TABLE accounts ADD COLUMN minBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")

                // 2. Add transferSubtype column to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferSubtype TEXT NOT NULL DEFAULT 'NONE'")

                // 3. Add vaultMode column to user_profile
                db.execSQL("ALTER TABLE user_profile ADD COLUMN vaultMode TEXT NOT NULL DEFAULT '3_VAULT'")

                // 4. Create indices for performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isArchived ON accounts(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_sortOrder ON accounts(sortOrder)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myfin_vault.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Fallback safety during testing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
