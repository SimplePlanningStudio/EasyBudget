package com.simplebudget.view.accounts.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category

class AccountsAdapter(
    val accountList: ArrayList<Account>,
    val context: Context,
    var previousSelectedAccountType: Account?,
    private val onAccountSelected: (selectedAccount: Pair<Int, Account>) -> Unit
) : RecyclerView.Adapter<AccountsAdapter.AccountViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): AccountViewHolder {
        return AccountViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_accounts_list, viewGroup, false)
        )
    }

    @SuppressLint("NewApi,NotifyDataSetChanged")
    override fun onBindViewHolder(viewHolder: AccountViewHolder, position: Int) {
        val account = accountList[position]
        viewHolder.bind(account)
        viewHolder.itemView.setOnClickListener {
            previousSelectedAccountType = account
            onAccountSelected(Pair(position, account))
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return accountList.size
    }

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val item: ConstraintLayout = view.findViewById(R.id.item)
        private val accountTitle: TextView = view.findViewById(R.id.accountTitle)
        private val ivTickSelected: ImageView = view.findViewById(R.id.ivTickSelected)
        fun bind(account: Account) {
            if (previousSelectedAccountType == null) previousSelectedAccountType =
                accountList.first()
            accountTitle.text = account.name
            val isSelected = (accountList[layoutPosition].id == previousSelectedAccountType?.id)
            ivTickSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            item.setBackgroundColor(if (isSelected) context.resources.getColor(R.color.divider_grey_very_light) else 0)
        }
    }

    fun delete(account: Account) {
        if (accountList.contains(account)) accountList.remove(account)
        notifyDataSetChanged()
    }

    fun delete(position: Int) {
        if (accountList.size >= position) accountList.removeAt(position)
        notifyDataSetChanged()
    }
}

