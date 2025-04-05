/*
 *   Copyright 2025 Waheed Nazir
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
package com.simplebudget.model.profile

import android.os.Parcel
import android.os.Parcelable

data class Profile(
    val id: Long = 1,
    val userName: String?,
    val email: String?,
    val fcmToken: String?,
    val loginId: String?,
    val isPremium: Boolean = false,
    val premiumType: String?,
    val appVersion: String?,
    val updateTime: String? = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as Long,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(userName)
        parcel.writeString(email)
        parcel.writeString(fcmToken)
        parcel.writeString(loginId)
        parcel.writeByte(if (isPremium) 1 else 0)
        parcel.writeString(premiumType)
        parcel.writeString(appVersion)
        parcel.writeString(updateTime)
    }

    companion object CREATOR : Parcelable.Creator<Profile> {
        override fun createFromParcel(parcel: Parcel): Profile {
            return Profile(parcel)
        }

        override fun newArray(size: Int): Array<Profile?> {
            return arrayOfNulls(size)
        }
    }


    override fun toString(): String {
        return "Profile(id=$id, userName='$userName', email='$email', fcmToken='$fcmToken', loginId='$loginId', isPremium=$isPremium, premiumType='$premiumType', appVersion='$appVersion')"
    }

}