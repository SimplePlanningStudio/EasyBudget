/*
 *   Copyright 2023 Waheed Nazir
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
package com.simplebudget.db.impl.accounts

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountType(accountType: AccountTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccountTypes(accountTypes: List<AccountTypeEntity>)

    @Delete
    suspend fun deleteAccountType(accountType: AccountTypeEntity)

    @Update
    suspend fun updateAccountType(accountType: AccountTypeEntity)

    @Query("SELECT * FROM account_type")
    fun getAllAccountTypes(): Flow<List<AccountTypeEntity>>

    @Query("SELECT * FROM account_type")
    suspend fun getAllAccounts(): List<AccountTypeEntity>

    @Query("SELECT * FROM account_type WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<AccountTypeEntity>

    @Query("SELECT * FROM account_type WHERE _account_type_id = :accountId")
    suspend fun getAccount(accountId: Long): AccountTypeEntity

    @Query("UPDATE account_type SET isActive = 0")
    suspend fun updateAllAccountTypesInactive()

    @Query("UPDATE account_type SET isActive = CASE WHEN _account_type_id = :accountId THEN 1 ELSE 0 END WHERE _account_type_id = :accountId OR isActive = 1")
    suspend fun setActiveAccount(accountId: Long)

    @Query("UPDATE account_type SET isActive = CASE WHEN name = :accountName THEN 1 ELSE 0 END WHERE name = :accountName OR isActive = 1")
    suspend fun setActiveAccount(accountName: String)

    @Query("UPDATE account_type SET isActive = CASE WHEN name = 'SAVINGS' THEN 1 ELSE 0 END WHERE name = 'SAVINGS' OR isActive = 1")
    suspend fun resetActiveAccount()

    @Query("SELECT COUNT(*) FROM account_type")
    suspend fun getRowCount(): Int
}