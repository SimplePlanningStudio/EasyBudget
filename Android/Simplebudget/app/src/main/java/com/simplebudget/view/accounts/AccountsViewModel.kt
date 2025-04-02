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
package com.simplebudget.view.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.helper.extensions.toAccount
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.model.account.Account
import com.simplebudget.model.account.AccountType
import com.simplebudget.model.account.Accounts
import com.simplebudget.model.expense.Expense
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.setActiveAccount
import com.simplebudget.view.accounts.adapter.AccountDataModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate


class AccountsViewModel(
    private val db: DB, private val appPreferences: AppPreferences
) : ViewModel() {

    lateinit var activeAccountFlow: Flow<AccountTypeEntity?>
    lateinit var allAccountsFlow: Flow<List<AccountTypeEntity>>

    /**
     *
     */
    private val getAccountFromIdLiveData = MutableLiveData<Account>()
    val getAccountFromId: LiveData<Account> = getAccountFromIdLiveData


    /**
     *
     */
    private val progressLiveData = MutableLiveData<Boolean?>()
    val progress: LiveData<Boolean?> = progressLiveData

    /**
     *
     */
    private val deletedSuccessfullyLiveData = MutableLiveData<Boolean?>()
    val deletedSuccessfully: LiveData<Boolean?> = deletedSuccessfullyLiveData


    val monthlyReportDataLiveData = MutableLiveData<AccountDataModels.MonthlyAccountData>()
    val expenses = mutableListOf<Expense>()
    val revenues = mutableListOf<Expense>()
    val accounts = mutableListOf<Account>()
    private val allExpensesOfThisMonth = mutableListOf<AccountDataModels.SuperParentAccount>()
    private val allExpensesParentList = mutableListOf<AccountDataModels.CustomTripleAccount.Data>()
    var revenuesAmount = 0.0
    var expensesAmount = 0.0
    var balance = 0.0
    private val hashMapAccountDetails =
        hashMapOf<Long, AccountDataModels.CustomTripleAccount.Data>() // <accountId, expensesDetails>
    private val hashMapAvailableAccounts = hashMapOf<Long, Account>() // <accountId, account>


    /**
     * This function will load all expenses for all available account.
     */
    fun loadAccountDetailsWithBalance() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                expenses.clear()
                revenues.clear()
                allExpensesParentList.clear()
                allExpensesOfThisMonth.clear()
                revenuesAmount = 0.0
                expensesAmount = 0.0

                progressLiveData.postValue(true)
                val accounts = db.getAllAccounts().toAccounts()
                hashMapAvailableAccounts.clear()
                hashMapAccountDetails.clear()
                accounts.forEach { account ->
                    hashMapAvailableAccounts[account.id!!] = account
                    hashMapAccountDetails[account.id] = AccountDataModels.CustomTripleAccount.Data(
                        account.id, account.name!!, 0.0, 0.0, ArrayList()
                    )
                }
            }

            val expensesForMonth = withContext(Dispatchers.IO) {
                db.getExpensesForMonthWithoutCheckingAccount()
            }

            withContext(Dispatchers.IO) {
                for (expense in expensesForMonth) {
                    var tCredit: Double =
                        hashMapAccountDetails[expense.accountId]?.totalCredit ?: 0.0
                    var tDebit: Double = hashMapAccountDetails[expense.accountId]?.totalDebit ?: 0.0

                    if (expense.isRevenue()) {
                        revenues.add(expense)
                        revenuesAmount -= expense.amount
                        tCredit -= expense.amount
                    } else {
                        expenses.add(expense)
                        expensesAmount += expense.amount
                        tDebit += expense.amount
                    }
                    hashMapAccountDetails[expense.accountId]?.totalCredit = tCredit
                    hashMapAccountDetails[expense.accountId]?.totalDebit = tDebit
                    hashMapAccountDetails[expense.accountId]?.expenses?.add(expense)
                }
                hashMapAccountDetails.keys.forEach { key ->
                    allExpensesOfThisMonth.add(
                        AccountDataModels.ParentAccount(
                            hashMapAccountDetails[key]?.accountId!!,
                            hashMapAccountDetails[key]?.account!!,
                            hashMapAccountDetails[key]?.totalCredit ?: 0.0,
                            hashMapAccountDetails[key]?.totalDebit ?: 0.0
                        )
                    )
                    allExpensesParentList.add(
                        AccountDataModels.CustomTripleAccount.Data(
                            hashMapAccountDetails[key]?.accountId!!,
                            hashMapAccountDetails[key]?.account!!,
                            hashMapAccountDetails[key]?.totalCredit ?: 0.0,
                            hashMapAccountDetails[key]?.totalDebit ?: 0.0,
                            hashMapAccountDetails[key]?.expenses ?: ArrayList()
                        )
                    )
                    hashMapAccountDetails[key]?.expenses?.forEach { expense ->
                        allExpensesOfThisMonth.add(AccountDataModels.ChildAccount(expense))
                    }
                }
                balance = revenuesAmount - expensesAmount
                progressLiveData.postValue(false)
                monthlyReportDataLiveData.postValue(
                    AccountDataModels.MonthlyAccountData.Data(
                        expenses,
                        revenues,
                        allExpensesOfThisMonth,
                        allExpensesParentList,
                        expensesAmount,
                        revenuesAmount
                    )
                )
            }

        }
    }


    /**
     *
     */
    private fun loadActiveAccount() {
        activeAccountFlow = db.getActiveAccount()
    }

    /**
     *
     */
    private fun loadAllAccounts() {
        allAccountsFlow = db.getAccountTypes()
    }


    /**
     * Update an account and set it to active as well
     */
    fun updateActiveAccount(account: Account) {
        viewModelScope.launch {
            appPreferences.setActiveAccount(account.id, account.name)
            withContext(Dispatchers.IO) {
                progressLiveData.postValue(true)
                db.persistAccountType(account)
                db.setActiveAccount(account.id ?: Accounts.DEFAULT_ACCOUNT)
                progressLiveData.postValue(false)
            }
        }
    }

    /**
     * Delete all one time expenses / recurring expenses for given account id
     * This function mostly be called for account deletion case.
     * Set active account back to savings.
     */
    fun deleteAccount(account: Account, defaultAccount: Account) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                progressLiveData.postValue(true)
                // Delete all expenses of this account and delete this account as well.
                db.deleteAllExpensesOfAnAccount(accountId = account.id!!)
                db.deleteAccountType(account)
                updateActiveAccount(defaultAccount)
                progressLiveData.postValue(false)
            }
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val count = db.accountAlreadyExists(account.name.uppercase())
                if (count <= 0) {
                    db.persistAccountType(account)
                }
            }
        }
    }

    /**
     * Get account from accountId
     */
    fun getAccountFromId(accountId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                progressLiveData.postValue(true)
                val account: AccountTypeEntity? = db.getAccount(accountId)
                getAccountFromIdLiveData.postValue(account?.toAccount())
                progressLiveData.postValue(false)
            }
        }
    }

    init {
        loadActiveAccount()
        loadAllAccounts()
    }

    override fun onCleared() {
        hashMapAvailableAccounts.clear()
        hashMapAccountDetails.clear()
        super.onCleared()
    }
}