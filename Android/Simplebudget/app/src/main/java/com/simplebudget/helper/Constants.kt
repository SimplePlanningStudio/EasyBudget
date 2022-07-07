package com.simplebudget.helper

import android.os.Looper

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

// security
const val WAS_PROTECTION_HANDLED = "was_protection_handled"
const val PROTECTION_PIN = 0
const val SHOW_PIN = 0
const val DATE_FORMAT_EIGHT = "dd-MM-yyyy"

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()
