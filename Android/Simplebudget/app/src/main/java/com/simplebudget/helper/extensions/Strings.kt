package com.simplebudget.helper.extensions

import java.util.*

fun String.capital(): String {
    return this.lowercase().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}