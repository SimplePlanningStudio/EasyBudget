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
package com.simplebudget.model.account

import java.util.ArrayList

enum class AccountType {
    SAVINGS
}


object Accounts {

    const val SAVINGS = 1L // Default account has ID 1

    fun getAccountsList(): List<Account> {
        val accounts: ArrayList<Account> = ArrayList()
        AccountType.values().forEach {
            accounts.add(Account(it.name))
        }
        return accounts
    }
}

