package com.simplebudget.view.accounts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.simplebudget.R
import com.simplebudget.model.account.Account

class AccountsSpinnerAdapter(
    context: Context, resource: Int, val objects: List<Account>
) :
    ArrayAdapter<Account>(context, resource, objects) {

    private class ViewHolder {
        var textView: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomViewForDropDown(position, convertView, parent)
    }

    private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        val viewHolder: ViewHolder
        if (row == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            row = inflater.inflate(R.layout.spinner_item, parent, false)

            viewHolder = ViewHolder()
            viewHolder.textView = row.findViewById(R.id.textItem)
            row.tag = viewHolder
        } else {
            viewHolder = row.tag as ViewHolder
        }

        // Assuming YourCustomObject has a method getName() to get the text to display
        val account = getItem(position)
        viewHolder.textView?.text = account?.name
        return row!!
    }

    private fun getCustomViewForDropDown(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var row = convertView
        val viewHolder: ViewHolder
        if (row == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            row = inflater.inflate(R.layout.spinner_item_dropdown, parent, false)

            viewHolder = ViewHolder()
            viewHolder.textView = row.findViewById(R.id.textItem)
            row.tag = viewHolder
        } else {
            viewHolder = row.tag as ViewHolder
        }

        // Assuming YourCustomObject has a method getName() to get the text to display
        val account = getItem(position)
        viewHolder.textView?.text = account?.name
        return row!!
    }
}