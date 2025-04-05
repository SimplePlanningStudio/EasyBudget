/*
 *   Copyright 2025 Benoit LETONDOR, Waheed Nazir
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.simplebudget.db.impl

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplebudget.db.impl.accounts.AccountTypeDao
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.budgets.BudgetCategoryCrossRef
import com.simplebudget.db.impl.budgets.BudgetDao
import com.simplebudget.db.impl.budgets.BudgetEntity
import com.simplebudget.db.impl.budgets.RecurringBudgetEntity
import com.simplebudget.db.impl.categories.CategoryDao
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.db.impl.expenses.ExpenseDao
import com.simplebudget.db.impl.expenses.ExpenseEntity
import com.simplebudget.db.impl.profile.ProfileDao
import com.simplebudget.db.impl.profile.ProfileEntity
import com.simplebudget.db.impl.recurringexpenses.RecurringExpenseEntity
import com.simplebudget.helper.localDateFromTimestamp
import com.simplebudget.model.account.Accounts
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.model.recurringexpense.RecurringExpenseType
import java.time.LocalDate

const val DB_NAME = "easybudget.db"

@Database(
    exportSchema = false,
    version = 11,
    entities = [CategoryEntity::class,
        ExpenseEntity::class,
        RecurringExpenseEntity::class,
        AccountTypeEntity::class,
        BudgetEntity::class,
        RecurringBudgetEntity::class,
        BudgetCategoryCrossRef::class,
        ProfileEntity::class
    ]
)
@TypeConverters(TimestampConverters::class)
abstract class RoomDB : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountTypeDao(): AccountTypeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun profileDao(): ProfileDao

    companion object {
        fun create(context: Context): RoomDB =
            Room.databaseBuilder(context, RoomDB::class.java, DB_NAME).addMigrations(
                migrationFrom1To2,
                migrationFrom2To3,
                migrationToRoom,
                migrationFrom4To5,
                migrationFrom5To6,
                migrateTimestamps6To7,
                migrateTimestamps7To8,
                migrateTimestamps8To9,
                migrateTimestamps9To10,
                migrateTimestamps10To11
            ).build()
    }
}

class TimestampConverters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}

private val migrateTimestamps10To11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Adding profile table to have user info
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profile (
                id INTEGER PRIMARY KEY NOT NULL,
                userName TEXT,
                email TEXT,
                fcmToken TEXT,
                loginId TEXT,
                isPremium INTEGER NOT NULL DEFAULT 0,
                premiumType TEXT,
                appVersion TEXT
            )
            """.trimIndent()
        )
    }
}

private val migrateTimestamps9To10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // Step 1: Retrieve MISCELLANEOUS category ID
        val cursor = db.query("SELECT _category_id FROM category WHERE name = 'MISCELLANEOUS'")
        val miscellaneousCategoryId = if (cursor.moveToFirst()) cursor.getLong(0) else 1
        cursor.close()

        // Step 2: Check if categoryId column already exists
        val columnCursor = db.query("PRAGMA table_info(expense)")
        var columnExists = false
        while (columnCursor.moveToNext()) {
            val columnName = columnCursor.getString(columnCursor.getColumnIndexOrThrow("name"))
            if (columnName == "categoryId") {
                columnExists = true
                break
            }
        }
        columnCursor.close()

        // Step 3: Add categoryId column if it doesn't exist
        if (!columnExists) {
            db.execSQL("ALTER TABLE expense ADD COLUMN categoryId INTEGER NOT NULL DEFAULT $miscellaneousCategoryId")
        }

        // Step 4: Update categoryId based on category name
        db.execSQL(
            """
            UPDATE expense
            SET categoryId = COALESCE((
                SELECT _category_id FROM category WHERE category.name = expense.category
            ), $miscellaneousCategoryId)
            WHERE category IS NOT NULL;
            """.trimIndent()
        )

        // Step 5: Ensure no NULL values remain in categoryId
        db.execSQL(
            """
            UPDATE expense
            SET categoryId = $miscellaneousCategoryId
            WHERE categoryId IS NULL;
            """.trimIndent()
        )

        //Adding budget table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS budget (
                _budget_id INTEGER PRIMARY KEY AUTOINCREMENT, 
                goal TEXT NOT NULL, 
                accountId INTEGER NOT NULL,
                budgetAmount INTEGER NOT NULL, 
                remainingAmount INTEGER NOT NULL, 
                spentAmount INTEGER NOT NULL, 
                startDate INTEGER NOT NULL, 
                endDate INTEGER NOT NULL, 
                monthly_id INTEGER
            );
        """.trimIndent()
        )

        // Create indexes for budget table on date,accountId and categoryId
        db.execSQL("CREATE INDEX IF NOT EXISTS 'S_D' ON 'budget' ('startDate')")
        db.execSQL("CREATE INDEX IF NOT EXISTS 'E_D' ON 'budget' ('endDate')")

        //Adding monthly budget table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS monthlybudget(
                '_budget_id' INTEGER,
                'goal' TEXT NOT NULL,
                'accountId' INTEGER NOT NULL,
                'budgetAmount' INTEGER NOT NULL,
                'type' TEXT NOT NULL,
                'recurringDate' INTEGER NOT NULL,
                'modified' INTEGER NOT NULL,
                PRIMARY KEY('_budget_id')
            );
            """.trimIndent()
        )

        //Category cross reference table for one to many (budget to categories)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS budget_category_cross_ref (
                _budget_id INTEGER NOT NULL,
                _category_id INTEGER NOT NULL,
                PRIMARY KEY(_budget_id, _category_id),
                FOREIGN KEY(_budget_id) REFERENCES budget(_budget_id) ON DELETE CASCADE
            );
        """
        )

        //Create indexes for budget_category_cross_ref table
        db.execSQL("CREATE INDEX IF NOT EXISTS 'C_I' ON 'budget_category_cross_ref' ('_category_id')")
    }
}

private val migrateTimestamps8To9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //Update default account table only the name SAVINGS to DEFAULT_ACCOUNT
        db.execSQL("UPDATE account_type SET name = 'DEFAULT_ACCOUNT' WHERE _account_type_id = 1")
    }
}
private val migrateTimestamps7To8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Adding accountType into expenses and categories
        db.execSQL("ALTER TABLE expense ADD COLUMN accountId INTEGER not null DEFAULT '" + Accounts.DEFAULT_ACCOUNT + "'")
        db.execSQL("ALTER TABLE monthlyexpense ADD COLUMN accountId INTEGER not null DEFAULT '" + Accounts.DEFAULT_ACCOUNT + "'")
        //Add account type table
        db.execSQL("CREATE TABLE IF NOT EXISTS account_type ('_account_type_id' INTEGER, 'name' text not null DEFAULT 'DEFAULT_ACCOUNT','isActive' INTEGER not null DEFAULT 1, PRIMARY KEY('_account_type_id'))")
    }
}

private val migrateTimestamps6To7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT _expense_id,date FROM expense")
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_expense_id"))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"))

            val localDate = localDateFromTimestamp(timestamp)
            val newTimestamp = localDate.toEpochDay()

            db.execSQL("UPDATE expense SET date = $newTimestamp WHERE _expense_id = $id")
        }

        val cursorRecurring = db.query("SELECT _expense_id,recurringDate FROM monthlyexpense")
        while (cursorRecurring.moveToNext()) {
            val id = cursorRecurring.getLong(cursorRecurring.getColumnIndexOrThrow("_expense_id"))
            val timestamp =
                cursorRecurring.getLong(cursorRecurring.getColumnIndexOrThrow("recurringDate"))

            val localDate = localDateFromTimestamp(timestamp)
            val newTimestamp = localDate.toEpochDay()

            db.execSQL("UPDATE monthlyexpense SET recurringDate = $newTimestamp WHERE _expense_id = $id")
        }
    }
}

private val migrationFrom5To6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS category ('_category_id' INTEGER, 'name' text not null DEFAULT 'MISCELLANEOUS', PRIMARY KEY('_category_id'))")
    }
}

private val migrationFrom4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense ADD COLUMN category text not null DEFAULT '" + ExpenseCategoryType.MISCELLANEOUS.name + "'")
        db.execSQL("ALTER TABLE monthlyexpense ADD COLUMN category text not null DEFAULT '" + ExpenseCategoryType.MISCELLANEOUS.name + "'")
    }
}

private val migrationToRoom = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
    }
}

private val migrationFrom2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE monthlyexpense ADD COLUMN type text not null DEFAULT '" + RecurringExpenseType.MONTHLY + "'")
    }
}

private val migrationFrom1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE expense SET amount = amount * 100")
        db.execSQL("UPDATE monthlyexpense SET amount = amount * 100")
    }
}
