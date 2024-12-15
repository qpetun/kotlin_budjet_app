package com.example.test5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.test5.models.Purchase

class PurchaseAdapter : RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder>() {

    private var purchases: List<Purchase> = emptyList()

    class PurchaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tv_name)
        val priceTextView: TextView = view.findViewById(R.id.tv_price)
        val categoryTextView: TextView = view.findViewById(R.id.tv_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_purchase_adapter, parent, false)
        return PurchaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        val purchase = purchases[position]
        holder.nameTextView.text = purchase.name
        holder.priceTextView.text = "Price: $${purchase.price}"
        holder.categoryTextView.text = "Category: ${purchase.category}"
    }

    override fun getItemCount(): Int = purchases.size

    fun submitList(purchases: List<Purchase>) {
        this.purchases = purchases
        notifyDataSetChanged()
    }
}

