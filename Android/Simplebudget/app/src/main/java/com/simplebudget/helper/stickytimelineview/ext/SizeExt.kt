package com.simplebudget.helper.stickytimelineview.ext

import android.content.Context

/**
 */
fun Int.DP(context: Context): Float = (this * context.resources.displayMetrics.density)

fun Float.DP(context: Context): Float = (this * context.resources.displayMetrics.density)