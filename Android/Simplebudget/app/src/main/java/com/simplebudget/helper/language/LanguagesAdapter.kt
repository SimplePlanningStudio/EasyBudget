package com.simplebudget.helper.language

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R

class LanguagesAdapter(
    private val languageList: ArrayList<AppLanguage>, val context: Context?,
    private val previousSelectedLanguageCode: String
) : RecyclerView.Adapter<LanguageViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): LanguageViewHolder {
        return LanguageViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_change_language, viewGroup, false)
        )
    }

    @SuppressLint("NewApi")
    override fun onBindViewHolder(viewHolder: LanguageViewHolder, position: Int) {
        viewHolder.languageName.text = languageList[position].languageName
        if (previousSelectedLanguageCode == (languageList[position].languageCode)) {
            viewHolder.languageName.setTextColor(context!!.getColor(R.color.white))
            viewHolder.itemLayout.setBackgroundColor(context.getColor(R.color.primary_dark))
        } else {
            viewHolder.languageName.setTextColor(context!!.getColor(R.color.colorBlack))
            viewHolder.itemLayout.setBackgroundColor(context.getColor(R.color.white))
        }
    }

    override fun getItemCount(): Int = languageList.size
}

class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val languageName: TextView = view.findViewById(R.id.languageName)
    val itemLayout: LinearLayout = view.findViewById(R.id.item_language_change)
}
