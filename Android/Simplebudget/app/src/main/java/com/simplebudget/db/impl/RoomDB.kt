/*
 *   Copyright 2022 Benoit LETONDOR
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
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.model.RecurringExpenseType
import java.util.*

const val DB_NAME = "easybudget.db"

@Database(
    exportSchema = false,
    version = 6,
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
                migrationFrom5To6
            )
            .build()
    }
}

private class TimestampConverters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
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
