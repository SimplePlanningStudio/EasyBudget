/*
 *   Copyright 2024 Waheed Nazir
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
package com.simplebudget.db.impl.categories

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistCategory(categoryEntity: CategoryEntity): Long

    @Query("SELECT * FROM category")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE _category_id = :categoryId")
    suspend fun getCategory(categoryId: Long): CategoryEntity

    @Query("SELECT * FROM category WHERE name = 'MISCELLANEOUS'")
    suspend fun getMiscellaneousCategory(): CategoryEntity

    @Delete
    suspend fun deleteCategory(categoryEntity: CategoryEntity)

    @Query("DELETE FROM category WHERE name = :categoryName")
    suspend fun deleteCategory(categoryName: String)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun getRowCount(): Int
}