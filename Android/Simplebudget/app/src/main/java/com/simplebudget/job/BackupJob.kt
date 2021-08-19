/*
 *   Copyright 2021 Benoit LETONDOR
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
package com.simplebudget.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.simplebudget.auth.Auth
import com.simplebudget.cloudstorage.CloudStorage
import com.simplebudget.db.DB
import com.simplebudget.helper.backupDB
import com.simplebudget.prefs.AppPreferences
import org.koin.java.KoinJavaComponent.get

class BackupJob(private val context: Context,
                workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val db: DB = get(DB::class.java)
    private val cloudStorage: CloudStorage = get(CloudStorage::class.java)
    private val auth: Auth = get(Auth::class.java)
    private val appPreferences: AppPreferences = get(AppPreferences::class.java)

    override suspend fun doWork(): Result {
        return backupDB(context, db, cloudStorage, auth, appPreferences)
    }

}