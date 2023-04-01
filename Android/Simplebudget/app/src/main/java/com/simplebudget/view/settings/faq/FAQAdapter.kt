package com.simplebudget.view.settings.faq

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R

class FAQAdapter(private val faqList: List<Question>, private val allExpanded: Boolean = false) :
    RecyclerView.Adapter<FAQAdapter.ViewHolder>() {
    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_faq, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = faqList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val faq = faqList[position]
        holder.bind(faq)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTextView: TextView = itemView.findViewById(R.id.question)
        private val answerTextView: TextView = itemView.findViewById(R.id.answer)

        @SuppressLint("NotifyDataSetChanged")
        fun bind(faq: Question) {
            questionTextView.text = faq.question
            answerTextView.text = faq.answer

            val isExpanded = position == expandedPosition
            answerTextView.visibility = if (allExpanded) View.VISIBLE else if (isExpanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                expandedPosition = if (isExpanded) -1 else position
                notifyDataSetChanged()
            }
        }
    }
}
