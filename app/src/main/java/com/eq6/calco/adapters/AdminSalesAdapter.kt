package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.AdminSaleItem
import java.text.NumberFormat
import java.util.Locale

class AdminSalesAdapter(
    private var items: List<AdminSaleItem>,
    private val onClick: (AdminSaleItem) -> Unit
) : RecyclerView.Adapter<AdminSalesAdapter.VH>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvSaleNumber: TextView = v.findViewById(R.id.tvSaleNumber)
        val tvClient: TextView = v.findViewById(R.id.tvClient)
        val tvSeller: TextView = v.findViewById(R.id.tvSeller)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val tvMonth: TextView = v.findViewById(R.id.tvMonth)
        val btnGo: ImageButton = v.findViewById(R.id.btnGo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_sale, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sale = items[position]

        holder.tvSaleNumber.text = "#${sale.saleNumber}"
        holder.tvClient.text = "Cliente: ${sale.clientName}"
        holder.tvSeller.text = "Vendedor: ${sale.sellerName}"
        holder.tvAmount.text = moneyFmt.format(sale.amount)
        holder.tvMonth.text = monthKeyToLabel(sale.monthKey)

        holder.itemView.setOnClickListener { onClick(sale) }
        holder.btnGo.setOnClickListener { onClick(sale) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<AdminSaleItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun monthKeyToLabel(mk: String): String {
        val parts = mk.split("-")
        if (parts.size != 2) return mk

        val year = parts[0]
        val monthNumber = parts[1].toIntOrNull() ?: return mk

        val month = when (monthNumber) {
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

        return "$month $year"
    }
}