/*
 *   Copyright 2023 Benoit LETONDOR
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
import com.simplebudget.db.impl.entity.CategoryEntity
import com.simplebudget.db.impl.entity.ExpenseEntity
import com.simplebudget.db.impl.entity.RecurringExpenseEntity
import com.simplebudget.helper.localDateFromTimestamp
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.model.RecurringExpenseType
import java.time.LocalDate

const val DB_NAME = "easybudget.db"

@Database(
    exportSchema = false,
    version = 7,
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        RecurringExpenseEntity::class
    ]
)
@TypeConverters(TimestampConverters::class)
abstract class RoomDB : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        fun create(context: Context): RoomDB = Room
            .databaseBuilder(context, RoomDB::class.java, DB_NAME)
            .addMigrations(
                migrationFrom1To2,
                migrationFrom2To3,
                migrationToRoom,
                migrationFrom4To5,
                migrationFrom5To6,
                migrateTimestamps6To7
            )
            .build()
    }
}

private class TimestampConverters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}

private val migrateTimestamps6To7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val cursor = database.query("SELECT _expense_id,date FROM expense")
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_expense_id"))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"))

            val localDate = localDateFromTimestamp(timestamp)
            val newTimestamp = localDate.toEpochDay()

            database.execSQL("UPDATE expense SET date = $newTimestamp WHERE _expense_id = $id")
        }

        val cursorRecurring = database.query("SELECT _expense_id,recurringDate FROM monthlyexpense")
        while (cursorRecurring.moveToNext()) {
            val id = cursorRecurring.getLong(cursorRecurring.getColumnIndexOrThrow("_expense_id"))
            val timestamp =
                cursorRecurring.getLong(cursorRecurring.getColumnIndexOrThrow("recurringDate"))

            val localDate = localDateFromTimestamp(timestamp)
            val newTimestamp = localDate.toEpochDay()

            database.execSQL("UPDATE monthlyexpense SET recurringDate = $newTimestamp WHERE _expense_id = $id")
        }
    }
}

private val migrationFrom5To6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS category ('_category_id' INTEGER, 'name' text not null DEFAULT 'MISCELLANEOUS', PRIMARY KEY('_category_id'))")
    }
}
private val migrationFrom4To5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE expense ADD COLUMN category text not null DEFAULT '" + ExpenseCategoryType.MISCELLANEOUS + "'")
        database.execSQL("ALTER TABLE monthlyexpense ADD COLUMN category text not null DEFAULT '" + ExpenseCategoryType.MISCELLANEOUS + "'")
    }
}

private val migrationToRoom = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}

private val migrationFrom2To3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE monthlyexpense ADD COLUMN type text not null DEFAULT '" + RecurringExpenseType.MONTHLY + "'")
    }
}

private val migrationFrom1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE expense SET amount = amount * 100")
        database.execSQL("UPDATE monthlyexpense SET amount = amount * 100")
    }
}
