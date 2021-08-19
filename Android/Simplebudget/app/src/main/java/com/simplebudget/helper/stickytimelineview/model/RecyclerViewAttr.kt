package com.simplebudget.helper.stickytimelineview.model

import android.graphics.drawable.Drawable

/**
 */
data class RecyclerViewAttr(
    val sectionBackgroundColor: Int,
    val sectionTitleTextColor: Int,
    val sectionSubTitleTextColor: Int,
    val sectionLineColor: Int,
    val sectionDotColor: Int,
    val sectionDotStrokeColor: Int,
    val sectionTitleTextSize: Float,
    val sectionSubTitleTextSize: Float,
    val sectionLineWidth: Float,
    val isSticky: Boolean,
    val customDotDrawable: Drawable?,
    val timeLineMode: Int,
    val sectionBackgroundColorMode: Int,
    val sectionDotSize: Float,
    val sectionDotStrokeSize: Float
)