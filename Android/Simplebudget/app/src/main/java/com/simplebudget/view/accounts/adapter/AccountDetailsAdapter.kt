package com.simplebudget.view.accounts.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.databinding.RecyclerviewAccountDetailsCellBinding
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.model.category.Category
import com.simplebudget.prefs.AppPreferences

class AccountDetailsAdapter(
    private val allExpensesParentList: ArrayList<AccountDataModels.CustomTripleAccount.Data>,
    private val appPreferences: AppPreferences,
    private val onAccountSelected: (selectedAccountDetails: AccountDataModels.CustomTripleAccount.Data) -> Unit
) : RecyclerView.Adapter<AccountDetailsAdapter.CollectionsViewHolder>() {

    lateinit var context: Context

    class CollectionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RecyclerviewAccountDetailsCellBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_account_details_cell, parent, false)
        context = parent.context
        return CollectionsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionsViewHolder, position: Int) {
        holder.binding.apply {
            val obj = allExpensesParentList[position]
            val accountLabeling =
                if (obj.account.contains("ACCOUNT"))
                    obj.account else
                    String.format("%s %s", obj.account, "ACCOUNT")
            val amountSpend = (obj.totalCredit - obj.totalDebit)
            accountTitle.text = String.format("%s", accountLabeling)
            ivBolt.setBackgroundResource(if (amountSpend > 0.0) R.drawable.ic_bolt_green else R.drawable.ic_bolt_red)
            ivBolt.visibility = if (amountSpend == 0.0) View.GONE else View.VISIBLE
            accountBalanceAmount.text = String.format(
                "%s",
                CurrencyHelper.getFormattedCurrencyString(appPreferences, amountSpend)
            )
            incomeAmount.text = String.format(
                "%s",
                CurrencyHelper.getFormattedCurrencyString(appPreferences, obj.totalCredit)
            )
            expenseAmount.text = String.format(
                "%s",
                CurrencyHelper.getFormattedCurrencyString(appPreferences, obj.totalDebit)
            )
            headerCardView.setOnClickListener {
                onAccountSelected(obj)
            }
        }
    }

    override fun getItemCount() = allExpensesParentList.size

    fun addNewAccount(item: AccountDataModels.CustomTripleAccount.Data) {
        allExpensesParentList.add(0, item)
        notifyItemInserted(0)
    }
}