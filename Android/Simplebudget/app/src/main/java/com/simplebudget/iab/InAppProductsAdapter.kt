package com.simplebudget.iab

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.simplebudget.R

class InAppProductsAdapter(
    productList: List<ProductDetails>,
    private val onSelectProduct: (ProductDetails) -> Unit
) :
    RecyclerView.Adapter<InAppProductsAdapter.ProductViewHolder>() {
    private val productList: List<ProductDetails>
    private var selectedPosition = 0 //etc. RecyclerView.NO_POSITION

    init {
        this.productList = productList
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Declare your views here
        val productTitle: TextView
        val productDescription: TextView
        val productAmount: TextView
        val isSelected: ImageView
        val itemBackground: CardView

        init {
            productTitle = itemView.findViewById(R.id.productTitle)
            productDescription = itemView.findViewById(R.id.productDescription)
            productAmount = itemView.findViewById(R.id.productAmount)
            isSelected = itemView.findViewById(R.id.isSelected)
            itemBackground = itemView.findViewById(R.id.productCardView)
            itemView.setOnClickListener {
                selectedPosition = adapterPosition
                onSelectProduct(productList[selectedPosition])
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_inapp_products, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product: ProductDetails = productList[position]
        holder.productTitle.text = product.name
        if (product.productType == BillingClient.ProductType.INAPP) {
            //inapp
            holder.productDescription.text = product.description ?: ""
            product.oneTimePurchaseOfferDetails?.let { oneTimePurchaseOfferDetails ->
                holder.productAmount.text = oneTimePurchaseOfferDetails.formattedPrice
            }
        } else {
            // Subscriptions
            product.subscriptionOfferDetails?.let { subscriptionOfferDetails ->
                holder.productDescription.text =
                    BillingHelpers.billingPeriod(subscriptionOfferDetails.first()?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod)
            }
            product.subscriptionOfferDetails?.let { subscriptionOfferDetails ->
                holder.productAmount.text =
                    subscriptionOfferDetails.first()?.pricingPhases?.pricingPhaseList?.first()?.formattedPrice
            }
        }
        if (position == selectedPosition) {
            holder.itemBackground.setCardBackgroundColor(
                holder.itemBackground.context.resources.getColor(
                    R.color.buttons_green,
                    null
                )
            )
            holder.isSelected.visibility = View.VISIBLE
        } else {
            holder.itemBackground.setCardBackgroundColor(
                holder.itemBackground.context.resources.getColor(
                    R.color.colorBlackLight,
                    null
                )
            )
            holder.isSelected.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    object BillingHelpers {
        // P1W equates to one week, P1M equates to one month, P3M equates to three months, P6M equates to six months, and P1Y equates to one year
        fun billingPeriod(billingPeriod: String?): String {
            return when (billingPeriod) {
                "P1W" -> "Auto-renews per week until canceled"
                "P1M" -> "Auto-renews per month until canceled"
                "P3M" -> "Auto-renews after three months until canceled"
                "P6M" -> "Auto-renews after six months until canceled"
                "P1Y" -> "Auto-renews after one year until canceled"
                else -> ""
            }
        }
    }
}