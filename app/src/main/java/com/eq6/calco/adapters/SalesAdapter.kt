package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.SaleItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SalesAdapter(
    private var items: List<SaleItem>,
    private val onClick: (SaleItem) -> Unit
) : RecyclerView.Adapter<SalesAdapter.VH>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNumber: TextView = v.findViewById(R.id.tvNumber)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val btnGo: ImageButton = v.findViewById(R.id.btnGo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNumber.text = "#${item.saleNumber}"
        holder.tvAmount.text = moneyFmt.format(item.amount)
        holder.tvDate.text = item.date?.toDate()?.let { dateFmt.format(it) } ?: "-"

        holder.btnGo.setOnClickListener { onClick(item) }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<SaleItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}