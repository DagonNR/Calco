package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.SaleProductItem
import java.text.NumberFormat
import java.util.Locale

class SaleProductsAdapter(
    private var items: List<SaleProductItem>
) : RecyclerView.Adapter<SaleProductsAdapter.VH>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvProductName: TextView = v.findViewById(R.id.tvProductName)
        val tvQtyPrice: TextView = v.findViewById(R.id.tvQtyPrice)
        val tvSubtotal: TextView = v.findViewById(R.id.tvSubtotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sale_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvProductName.text = it.productName
        holder.tvQtyPrice.text = "${it.quantity} x ${moneyFmt.format(it.unitPrice)}"
        holder.tvSubtotal.text = moneyFmt.format(it.subtotal)
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<SaleProductItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}