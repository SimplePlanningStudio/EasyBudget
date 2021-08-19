package com.simplebudget.helper.stickytimelineview

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.helper.stickytimelineview.callback.SectionCallback
import com.simplebudget.helper.stickytimelineview.decoration.HorizontalSectionItemDecoration
import com.simplebudget.helper.stickytimelineview.decoration.VerticalSectionItemDecoration
import com.simplebudget.helper.stickytimelineview.model.RecyclerViewAttr

class TimeLineRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    private var recyclerViewAttr: RecyclerViewAttr? = null

    companion object {
        private const val MODE_VERTICAL = 0x00
        private const val MODE_HORIZONTAL = 0x01

        const val MODE_FULL = 0x00
        const val MODE_TO_TIME_LINE = 0x01
        const val MODE_TO_DOT = 0x02
    }

    init {
        attrs?.let {
            val a = context.theme?.obtainStyledAttributes(
                attrs,
                R.styleable.TimeLineRecyclerView,
                0, 0
            )

            a?.let {
                recyclerViewAttr =
                    RecyclerViewAttr(
                        it.getColor(
                            R.styleable.TimeLineRecyclerView_sectionBackgroundColor,
                            ContextCompat.getColor(context, R.color.colorDefaultBackground)
                        ),
                        it.getColor(
                            R.styleable.TimeLineRecyclerView_sectionTitleTextColor,
                            ContextCompat.getColor(context, R.color.colorDefaultTitle)
                        ),
                        it.getColor(
                            R.styleable.TimeLineRecyclerView_sectionSubTitleTextColor,
                            ContextCompat.getColor(context, R.color.colorDefaultSubTitle)
                        ),
                        it.getColor(
                            R.styleable.TimeLineRecyclerView_timeLineColor,
                            ContextCompat.getColor(context, R.color.colorDefaultTitle)
                        ),
                        it.getColor(
                            R.styleable.TimeLineRecyclerView_timeLineDotColor,
                            ContextCompat.getColor(context, R.color.colorDefaultTitle)
                        ),
                        it.getColor(
                            R.styleable.TimeLineRecyclerView_timeLineDotStrokeColor,
                            ContextCompat.getColor(context, R.color.colorDefaultStroke)
                        ),
                        it.getDimension(
                            R.styleable.TimeLineRecyclerView_sectionTitleTextSize,
                            context.resources.getDimension(R.dimen.title_text_size)
                        ),
                        it.getDimension(
                            R.styleable.TimeLineRecyclerView_sectionSubTitleTextSize,
                            context.resources.getDimension(R.dimen.sub_title_text_size)
                        ),
                        it.getDimension(
                            R.styleable.TimeLineRecyclerView_timeLineWidth,
                            context.resources.getDimension(R.dimen.line_width)
                        ),
                        it.getBoolean(R.styleable.TimeLineRecyclerView_isSticky, true),
                        it.getDrawable(R.styleable.TimeLineRecyclerView_customDotDrawable),
                        it.getInt(R.styleable.TimeLineRecyclerView_timeLineMode, MODE_VERTICAL),
                        it.getInt(
                            R.styleable.TimeLineRecyclerView_sectionBackgroundColorMode,
                            MODE_FULL
                        ), it.getDimension(
                            R.styleable.TimeLineRecyclerView_timeLineDotRadius,
                            context.resources.getDimension(R.dimen.dot_radius)
                        ),
                        it.getDimension(
                            R.styleable.TimeLineRecyclerView_timeLineDotStrokeSize,
                            context.resources.getDimension(R.dimen.dot_stroke_width)
                        )

                    )
            }

            a?.recycle()
        }
    }

    /**
     * Add VerticalSectionItemDecoration for Sticky TimeLineView
     *
     * @param callback SectionCallback
     * if you'd like to know more mode , look at res/values/attrs.xml
     */
    fun addItemDecoration(callback: SectionCallback) {
        recyclerViewAttr?.let {
            val decoration: ItemDecoration =
                when (it.timeLineMode) {
                    MODE_VERTICAL -> {
                        VerticalSectionItemDecoration(
                            context,
                            callback,
                            it
                        )
                    }
                    MODE_HORIZONTAL -> {
                        HorizontalSectionItemDecoration(
                            context,
                            callback,
                            it
                        )
                    }
                    else -> {
                        VerticalSectionItemDecoration(
                            context,
                            callback,
                            it
                        )
                    }
                }

            this.addItemDecoration(decoration)
        }
    }
}