package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.CartItem
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var items: MutableList<CartItem>,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvQtyPrice: TextView = v.findViewById(R.id.tvQtyPrice)
        val tvSubtotal: TextView = v.findViewById(R.id.tvSubtotal)
        val btnRemove: ImageButton = v.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cartItem = items[position]
        holder.tvName.text = cartItem.productName
        holder.tvQtyPrice.text = "${cartItem.quantity} x ${moneyFmt.format(cartItem.unitPrice)}"
        holder.tvSubtotal.text = moneyFmt.format(cartItem.subtotal())
        holder.btnRemove.setOnClickListener { onRemove(cartItem) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: MutableList<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
