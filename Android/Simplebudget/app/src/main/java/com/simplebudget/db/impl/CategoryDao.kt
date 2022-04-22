/*
 *   Copyright 2022 Waheed Nazir
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

import androidx.room.*
import com.simplebudget.db.impl.entity.ExpenseEntity
import com.simplebudget.db.impl.entity.RecurringExpenseEntity
import java.util.*
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery
import com.simplebudget.db.impl.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistCategory(categoryEntity: CategoryEntity): Long

    @Query("SELECT * FROM category")
    suspend fun getCategories(): List<CategoryEntity>

    @Delete
    suspend fun deleteCategory(categoryEntity: CategoryEntity)
}