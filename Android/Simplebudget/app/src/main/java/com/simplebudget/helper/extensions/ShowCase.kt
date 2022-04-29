package com.simplebudget.helper.extensions

import android.content.Context
import android.view.View
import com.simplebudget.helper.showcaseviewlib.GuideView
import com.simplebudget.helper.showcaseviewlib.config.DismissType
import com.simplebudget.helper.showcaseviewlib.config.Gravity
import com.simplebudget.helper.showcaseviewlib.config.PointerType

/**
 * Show case view future expenses
 */
fun Context.showCaseView(
    targetView: View,
    title: String,
    message: String,
    handleGuideListener: () -> Unit
) {
    GuideView.Builder(this)
        .setTitle(title)
        .setContentText(message)
        .setGravity(Gravity.center) //optional
        .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
        .setTargetView(targetView)
        .setContentTextSize(14) //optional
        .setTitleTextSize(18) //optional
        .setPointerType(PointerType.arrow)
        .setGuideListener {
            handleGuideListener.invoke()
        }
        .build()
        .show()
}