/*
 *   Copyright 2025 Waheed Nazir
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

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.helper.RecyclerTouchListener

/**
 * enum that allows to specify the order in which the different data objects
 * for the combined-chart are drawn
 */
enum class TYPE {
    ALL, REVENUES, EXPENSE
}

/**
 *
 */
data class Types(val type: String)


/**
 *
 */
object BreakdownType {
    /**
     * Show Languages Selection Dialog
     */
    fun showTypeDialog(
        context: Context,
        currentType: String,
        onLanguageSelected: (languageCode: String) -> Unit
    ) {
        val listDialog = Dialog(context)
        listDialog.setContentView(R.layout.dialog_list_view)

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(listDialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        listDialog.window?.attributes = lp

        val recyclerView: RecyclerView =
            listDialog.findViewById(R.id.recycler_view)
        val testRecyclerAdapter =
            BreakdownTypeAdapter(getAvailableTypes(context), context, currentType)
        val mLayoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(context)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        recyclerView.adapter = testRecyclerAdapter

        recyclerView.addOnItemTouchListener(
            RecyclerTouchListener(
                context,
                recyclerView,
                object : RecyclerTouchListener.ClickListener {
                    override fun onLongClick(view: View?, position: Int) {
                        selectType(position)
                    }

                    override fun onClick(view: View, position: Int) {
                        selectType(position)
                    }

                    private fun selectType(position: Int) {
                        val type = getAvailableTypes(context)[position].type
                        onLanguageSelected.invoke(type)
                        listDialog.dismiss()
                    }
                })
        )
        listDialog.show()
    }


    /**
     *
     */
    fun getSelectedType(context: Context, type: String): String {
        return when (type) {
            context.getString(R.string.all_label) -> TYPE.ALL.name
            context.getString(R.string.revenues) -> TYPE.REVENUES.name
            context.getString(R.string.expenses) -> TYPE.EXPENSE.name
            else -> TYPE.ALL.name
        }
    }

    /**
     *
     */
    private fun getAvailableTypes(context: Context): ArrayList<Types> {
        val languages = ArrayList<Types>()
        languages.clear()
        languages.add(Types(context.getString(R.string.all_label)))
        languages.add(Types(context.getString(R.string.revenues)))
        languages.add(Types(context.getString(R.string.expenses)))
        return languages
    }
}