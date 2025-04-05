package com.simplebudget.db.impl.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simplebudget.model.profile.Profile

/**
 * User Profile Entity used ID 1 for singleton pattern
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val userName: String? = null,
    val email: String? = null,
    val fcmToken: String? = null,
    val loginId: String? = null,
    val isPremium: Boolean = false,
    val premiumType: String? = null,
    val appVersion: String? = null,
) {
    fun toProfile() = Profile(
        id,
        userName,
        email,
        fcmToken,
        loginId,
        isPremium,
        premiumType,
        appVersion
    )
}
