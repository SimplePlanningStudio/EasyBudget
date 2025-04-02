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
package com.simplebudget.helper

import android.os.Looper
import com.simplebudget.BuildConfig

const val MD5 = "MD5"
const val PROTECTION_TYPE = "protection_type"
const val APP_PASSWORD_PROTECTION = "app_password_protection"
const val APP_PASSWORD_HASH = "app_password_hash"
const val APP_PROTECTION_TYPE = "app_protection_type"
const val DELETE_PASSWORD_HASH = "delete_password_hash"
const val LOCALE_URDU = "ur"
const val LOCALE_ENGLISH = "en"

// global intents
const val OPEN_DOCUMENT_TREE = 1000

const val ACCOUNTS_LIMIT = 6
val FREE_BUDGETS_LIMIT = 1

// security
const val WAS_PROTECTION_HANDLED = "was_protection_handled"
const val PROTECTION_PIN = 0
const val SHOW_PIN = 0
const val DATE_FORMAT_EIGHT = "dd-MM-yyyy"

const val APP_BANNER_DISPLAY_THRESHOLD = 3

const val ARG_DATE = "arg_date"

const val ARG_BUDGET_DETAILS = "budget_details"

//Notifications key
const val KEY_MONTHLY = "monthly"
const val KEY_BUDGET = "budget"
const val KEY_BUDGET_INTRO = "budget_intro"

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()
