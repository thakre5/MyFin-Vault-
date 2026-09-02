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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add minBalance, isArchived, and sortOrder columns to accounts
                db.execSQL("ALTER TABLE accounts ADD COLUMN minBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")

                // 2. Add transferSubtype column to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferSubtype TEXT NOT NULL DEFAULT 'NONE'")

                // 3. Add vaultMode and fortressThreshold columns to user_profile
                db.execSQL("ALTER TABLE user_profile ADD COLUMN vaultMode TEXT NOT NULL DEFAULT '3-VAULT'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN fortressThreshold REAL NOT NULL DEFAULT 25000.0")

                // 4. Create indices to match Room Schema v2 requirements
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isArchived ON accounts(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_sortOrder ON accounts(sortOrder)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_linkedFixedBillId ON transactions(linkedFixedBillId)")
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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default categories
                            db.beginTransaction()
                            try {
                                CategoryEntity.defaultCategories.forEach { category ->
                                    val catNameEscaped = category.name.replace("'", "''")
                                    db.execSQL("INSERT OR IGNORE INTO categories (name, type) VALUES ('$catNameEscaped', '${category.type.name}')")
                                }
                                SubcategoryEntity.defaultSubcategories.forEach { subcategory ->
                                    val parentEscaped = subcategory.parentCategory.replace("'", "''")
                                    val subNameEscaped = subcategory.name.replace("'", "''")
                                    db.execSQL("INSERT OR IGNORE INTO subcategories (parentCategory, name, type) VALUES ('$parentEscaped', '$subNameEscaped', '${subcategory.type.name}')")
                                }
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
