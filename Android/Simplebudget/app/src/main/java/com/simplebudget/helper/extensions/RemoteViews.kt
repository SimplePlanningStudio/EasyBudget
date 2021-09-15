package com.simplebudget.helper.extensions

import android.widget.RemoteViews

fun RemoteViews.setBackgroundColor(id: Int, color: Int) {
    setInt(id, "setBackgroundColor", color)
}

fun RemoteViews.setText(id: Int, text: String) {
    setTextViewText(id, text)
}
