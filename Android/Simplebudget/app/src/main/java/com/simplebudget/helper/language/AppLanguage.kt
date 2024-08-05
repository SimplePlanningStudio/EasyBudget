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
package com.simplebudget.helper.language

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R

data class AppLanguage(val languageName: String, val languageCode: String)


object Languages {
    /**
     * Show Languages Selection Dialog
     */
    fun showLanguagesDialog(
        context: Context,
        currentLanguage: String,
        onLanguageSelected: (languageCode: String) -> Unit
    ) {
        val listDialog = Dialog(context)
        listDialog.setContentView(R.layout.language_dialog_list_view)

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(listDialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        listDialog.window?.attributes = lp

        val recyclerView: RecyclerView =
            listDialog.findViewById(R.id.recycler_view)
        val testRecyclerAdapter = LanguagesAdapter(
            Languages.getAvailableLanguages(),
            context,
            currentLanguage
        )
        val mLayoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(context)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        recyclerView.adapter = testRecyclerAdapter

        recyclerView.addOnItemTouchListener(
            RecyclerTouchListener(
                context,
                recyclerView,
                object : RecyclerTouchListener.ClickListener {
                    override fun onLongClick(view: View?, position: Int) {
                        selectLanguage(position)
                    }

                    override fun onClick(view: View, position: Int) {
                        selectLanguage(position)
                    }

                    private fun selectLanguage(position: Int) {
                        // TODO Left undone.
                        /*val languageCode = getAvailableLanguages()[position].languageCode
                        onLanguageSelected.invoke(languageCode)
                        (context as LocalizationActivity).setLanguage(languageCode)
                        listDialog.dismiss()*/
                    }

                })
        )
        listDialog.show()
    }


    /**
     *
     */
    private fun getAvailableLanguages(): ArrayList<AppLanguage> {
        val languages = ArrayList<AppLanguage>()
        languages.clear()
        languages.add(AppLanguage("English", "en"))
        languages.add(AppLanguage("Chinese", "zh"))
        languages.add(AppLanguage("Hindi", "hi"))
        languages.add(AppLanguage("Spanish", "es"))
        languages.add(AppLanguage("French", "fr"))
        languages.add(AppLanguage("German", "de"))
        languages.add(AppLanguage("Japanese", "ja"))
        languages.add(AppLanguage("Russian", "ru"))
        languages.add(AppLanguage("Portuguese", "pt"))
        languages.add(AppLanguage("Turkish", "tr"))
        languages.add(AppLanguage("Arabic", "ar"))
        languages.add(AppLanguage("Urdu", "ur"))
        languages.add(AppLanguage("Bengali", "bn"))
        languages.add(AppLanguage("Indonesian", "in"))
        return languages
    }
}