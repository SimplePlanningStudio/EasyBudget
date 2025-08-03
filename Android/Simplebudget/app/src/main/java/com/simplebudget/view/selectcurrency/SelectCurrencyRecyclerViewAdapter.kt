/*
 *   Copyright 2025 Benoit LETONDOR
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
package com.simplebudget.view.selectcurrency

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView

import com.simplebudget.R
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.Keyboard
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.helper.getUserCurrency
import com.simplebudget.helper.setUserCurrency
import java.util.*
import kotlin.collections.ArrayList

/**
 * View adapter for the Recycler view of the [SelectCurrencyFragment]
 *
 * @author Benoit LETONDOR
 */
class SelectCurrencyRecyclerViewAdapter(
    mainCurrencies: List<Currency>,
    secondaryCurrencies: List<Currency>,
    private val appPreferences: AppPreferences,
) : RecyclerView.Adapter<SelectCurrencyRecyclerViewAdapter.ViewHolder>(), Filterable {


    var countryFilterList = ArrayList<Currency>()
    var currencyList = ArrayList<Currency>()

    init {
        countryFilterList.addAll(mainCurrencies)
        countryFilterList.addAll(secondaryCurrencies)

        currencyList.addAll(countryFilterList)
    }

    /**
     * Get the position of the selected currency
     */
    fun selectedCurrencyPosition(): Int {
        val currency = appPreferences.getUserCurrency()
        return countryFilterList.indexOf(currency)
    }

// ---------------------------------------->

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycleview_currency_cell, parent, false)
        return ViewHolder(v, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= 0 && position < countryFilterList.size) {
            val currency = countryFilterList[position]
            val userCurrency = appPreferences.getUserCurrency() == currency

            holder.selectedIndicator?.visibility =
                if (userCurrency) View.VISIBLE else View.INVISIBLE
            holder.currencyTitle?.text = CurrencyHelper.getCurrencyDisplayName(currency)
            holder.v.setOnClickListener { v ->
                // Set the currency
                appPreferences.setUserCurrency(currency)
                // Reload date to change the checkmark
                notifyDataSetChanged()

                // Broadcast the intent
                val intent = Intent(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
                intent.putExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA, currency.currencyCode)

                LocalBroadcastManager.getInstance(v.context).sendBroadcast(intent)

                Keyboard.hideSoftKeyboard(v.context, v)
            }
        }
    }

    override fun getItemCount(): Int {
        return countryFilterList.size
    }

    /**
     *
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                countryFilterList = if (charSearch.isEmpty()) {
                    currencyList
                } else {
                    val resultList = ArrayList<Currency>()
                    for (row in currencyList) {
                        if (row.currencyCode.lowercase(Locale.ROOT)
                                .contains(charSearch.lowercase(Locale.ROOT))
                            || row.displayName.lowercase(Locale.ROOT)
                                .contains(charSearch.lowercase(Locale.ROOT))
                            || row.symbol.lowercase(Locale.ROOT)
                                .contains(charSearch.lowercase(Locale.ROOT))
                        ) {
                            resultList.add(row)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = countryFilterList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                try {
                    countryFilterList = results?.values as ArrayList<Currency>
                    notifyDataSetChanged()
                } catch (e: Exception) {
                }
            }

        }
    }
// ------------------------------------------->

    class ViewHolder(val v: View, val type: Int, val separator: Boolean = false) :
        RecyclerView.ViewHolder(v) {
        var currencyTitle: TextView? = null
        var selectedIndicator: ImageView? = null

        init {
            if (!separator) {
                currencyTitle = v.findViewById(R.id.currency_cell_title_tv)
                selectedIndicator = v.findViewById(R.id.currency_cell_selected_indicator_iv)
            }
        }
    }

}
