package com.simplebudget.db.impl.profile

import androidx.room.*

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    suspend fun getUser(): ProfileEntity?

    @Query("DELETE FROM profile")
    suspend fun deleteUser()
}
