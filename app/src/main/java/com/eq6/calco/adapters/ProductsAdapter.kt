package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.ProductItem
import java.text.NumberFormat
import java.util.Locale

class ProductsAdapter(
    private var items: List<ProductItem>,
    private val onClick: (ProductItem) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.VH>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvBarcode: TextView = v.findViewById(R.id.tvBarcode)
        val tvStock: TextView = v.findViewById(R.id.tvStock)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val btnGo: ImageButton = v.findViewById(R.id.btnGo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.tvName.text = p.name
        holder.tvStock.text = "Stock: ${p.stock}"
        holder.tvPrice.text = moneyFmt.format(p.price)

        holder.tvBarcode.text = if (p.barcode.isBlank()) "Barcode: —" else "Barcode: ${p.barcode}"
        holder.btnGo.setOnClickListener { onClick(p) }
        holder.itemView.setOnClickListener { onClick(p) }
        holder.itemView.alpha = if (p.active) 1.0f else 0.5f
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ProductItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}