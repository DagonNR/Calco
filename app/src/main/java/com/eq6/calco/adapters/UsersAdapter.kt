package com.eq6.calco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.R
import com.eq6.calco.models.UserItem

class UsersAdapter(
    private var items: List<UserItem>,
    private val onClick: (UserItem) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvRole: TextView = v.findViewById(R.id.tvRole)
        val btnGo: ImageButton = v.findViewById(R.id.btnGo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvRole.text = when (item.role) {
            "admin" -> "Admin"
            "seller" -> "Vendedor"
            "client" -> "Cliente"
            else -> item.role
        }
        holder.btnGo.setOnClickListener { onClick(item) }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<UserItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}