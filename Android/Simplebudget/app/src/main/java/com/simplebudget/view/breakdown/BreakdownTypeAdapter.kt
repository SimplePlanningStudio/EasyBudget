/*
 *   Copyright 2024 Waheed Nazir
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
package com.simplebudget.view.breakdown

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R

class BreakdownTypeAdapter(
    private val typeList: ArrayList<Types>, val context: Context?,
    private val previousSelectedLanguageCode: String
) : RecyclerView.Adapter<LanguageViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): LanguageViewHolder {
        return LanguageViewHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false)
        )
    }

    @SuppressLint("NewApi")
    override fun onBindViewHolder(viewHolder: LanguageViewHolder, position: Int) {
        viewHolder.languageName.text = typeList[position].type
        if (previousSelectedLanguageCode == (typeList[position].type)) {
            viewHolder.languageName.setTextColor(context!!.getColor(R.color.white))
            viewHolder.itemLayout.setBackgroundColor(context.getColor(R.color.primary_dark))
        } else {
            viewHolder.languageName.setTextColor(context!!.getColor(R.color.colorBlack))
            viewHolder.itemLayout.setBackgroundColor(context.getColor(R.color.white))
        }
    }

    override fun getItemCount(): Int = typeList.size
}

class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val languageName: TextView = view.findViewById(R.id.languageName)
    val itemLayout: LinearLayout = view.findViewById(R.id.item_language_change)
}
