package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.DashboardSaleItem
import java.text.NumberFormat
import java.util.Locale

class DashboardSalesAdapter(
    private var items: List<DashboardSaleItem>,
    private val onClick: (DashboardSaleItem) -> Unit
) : RecyclerView.Adapter<DashboardSalesAdapter.VH>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNumber: TextView = v.findViewById(R.id.tvNumber)
        val tvMonth: TextView = v.findViewById(R.id.tvMonth)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val btnGo: ImageButton = v.findViewById(R.id.btnGo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_sale, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNumber.text = "#${item.saleNumber}"
        holder.tvMonth.text = monthKeyToSpanish(item.monthKey)
        holder.tvAmount.text = moneyFmt.format(item.amount)
        holder.btnGo.setOnClickListener { onClick(item) }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<DashboardSaleItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun monthKeyToSpanish(mk: String): String {
        val parts = mk.split("-")
        if (parts.size != 2) return mk
        val m = parts[1].toIntOrNull() ?: return mk
        return when (m) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            12 -> "Diciembre"
            else -> mk
        }
    }
}