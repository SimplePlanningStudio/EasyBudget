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
package com.simplebudget.injection

import androidx.collection.ArrayMap
import com.simplebudget.iab.Iab
import com.simplebudget.auth.Auth
import com.simplebudget.auth.FirebaseAuth
import com.simplebudget.cloudstorage.CloudStorage
import com.simplebudget.cloudstorage.FirebaseStorage
import com.simplebudget.db.DB
import com.simplebudget.db.impl.CacheDBStorage
import com.simplebudget.db.impl.CachedDBImpl
import com.simplebudget.db.impl.DBImpl
import com.simplebudget.db.impl.RoomDB
import com.simplebudget.iab.IabImpl
import com.simplebudget.model.Expense
import com.simplebudget.prefs.AppPreferences
import org.koin.dsl.module
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val appModule = module {

    single { AppPreferences(get()) }

    single<Iab> { IabImpl(get(), get()) }

    single<CacheDBStorage> {
        object : CacheDBStorage {
            override val expenses: MutableMap<LocalDate, List<Expense>> = ArrayMap()
            override val balances: MutableMap<LocalDate, Double> = ArrayMap()
        }
    }

    single<Executor> { Executors.newSingleThreadExecutor() }

    single<Auth> { FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance()) }

    single<CloudStorage> {
        FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance().apply {
            maxOperationRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
            maxDownloadRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
            maxUploadRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
        })
    }

    factory<DB> { CachedDBImpl(DBImpl(RoomDB.create(get())), get(), get()) }

    factory { CachedDBImpl(DBImpl(RoomDB.create(get())), get(), get()) }
}