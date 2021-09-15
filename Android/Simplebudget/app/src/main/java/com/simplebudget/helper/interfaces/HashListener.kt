package com.simplebudget.helper.interfaces

interface HashListener {
    fun receivedHash(hash: String, type: Int)
    fun error(error: String)
}
