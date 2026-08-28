package com.example.myfin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        FixedBillEntity::class,
        BudgetPlanEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myfin_vault.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed the 4 baseline strategic vault accounts directly into SQLite
                db.execSQL("INSERT OR IGNORE INTO accounts (accountName, startingBalance, accountType, sortOrder) VALUES ('Operating Account', 0.0, 'Operating', 0)")
                db.execSQL("INSERT OR IGNORE INTO accounts (accountName, startingBalance, accountType, sortOrder) VALUES ('Commitments Account', 0.0, 'Commitments', 1)")
                db.execSQL("INSERT OR IGNORE INTO accounts (accountName, startingBalance, accountType, sortOrder) VALUES ('Fortress Account', 0.0, 'Fortress', 2)")
                db.execSQL("INSERT OR IGNORE INTO accounts (accountName, startingBalance, accountType, sortOrder) VALUES ('Cash Wallet', 0.0, 'Cash', 3)")
            }
        }
    }
}
