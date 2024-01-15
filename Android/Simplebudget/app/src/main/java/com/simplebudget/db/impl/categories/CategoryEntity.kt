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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_category_id")
    val id: Long?,
    @ColumnInfo(name = "name")
    val name: String
) {

    fun toCategory(catName: String) = CategoryEntity(
        id,
        catName
    )

    fun toCategory(id: Long, catName: String) = CategoryEntity(
        id,
        catName
    )
}