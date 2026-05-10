package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.eq6.calco.adapters.AdminSalesAdapter
import com.eq6.calco.models.AdminSaleItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class AdminSalesActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    private lateinit var spMonth: Spinner
    private lateinit var etSearch: EditText
    private lateinit var rvSales: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: AdminSalesAdapter

    private var storeId: String = ""
    private var allSales: List<AdminSaleItem> = emptyList()
    private var monthOptions: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_sales)

        spMonth = findViewById(R.id.spMonth)
        etSearch = findViewById(R.id.etSearch)
        rvSales = findViewById(R.id.rvSales)
        tvTotal = findViewById(R.id.tvTotal)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = AdminSalesAdapter(emptyList()) { sale ->
            val i = Intent(this, SaleDetailActivity::class.java)
            i.putExtra("saleId", sale.id)
            startActivity(i)
        }

        rvSales.layoutManager = LinearLayoutManager(this)
        rvSales.adapter = adapter

        setupBottomNav()

        etSearch.doAfterTextChanged {
            applyFilters()
        }

        StoreSession.getStoreId(
            onOk = { sid ->
                storeId = sid
                loadSales()
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        )
    }

    private fun setupBottomNav() {
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        bottom.selectedItemId = R.id.nav_report

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    startActivity(Intent(this, UsersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_report -> {
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadSales() {
        db.collection("stores").document(storeId)
            .collection("sales")
            .get()
            .addOnSuccessListener { snap ->
                allSales = snap.documents.map { d ->
                    AdminSaleItem(
                        id = d.id,
                        saleNumber = d.getString("saleNumber") ?: "",
                        clientName = d.getString("clientName") ?: "",
                        sellerName = d.getString("sellerName") ?: "",
                        amount = d.getDouble("amount") ?: 0.0,
                        monthKey = d.getString("monthKey") ?: "",
                        date = d.getTimestamp("date")
                    )
                }.sortedByDescending { it.date?.toDate()?.time ?: 0L }

                buildMonthFilter()
                applyFilters()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cargando ventas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun buildMonthFilter() {
        val months = allSales
            .map { it.monthKey }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()

        monthOptions = listOf("Todos") + months

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            monthOptions.map { if (it == "Todos") "Todos" else monthKeyToLabel(it) }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMonth.adapter = spinnerAdapter

        val requestedMonth = intent.getStringExtra("monthKey")
        val selectedIndex = if (!requestedMonth.isNullOrBlank()) {
            monthOptions.indexOf(requestedMonth).takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        spMonth.setSelection(selectedIndex)

        spMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun applyFilters() {
        val search = etSearch.text.toString().trim().lowercase()
        val selectedMonth = monthOptions.getOrNull(spMonth.selectedItemPosition) ?: "Todos"

        val filtered = allSales.filter { sale ->
            val matchesMonth = selectedMonth == "Todos" || sale.monthKey == selectedMonth

            val matchesSearch = search.isBlank()
                    || sale.saleNumber.lowercase().contains(search)
                    || sale.clientName.lowercase().contains(search)
                    || sale.sellerName.lowercase().contains(search)

            matchesMonth && matchesSearch
        }

        adapter.update(filtered)

        val total = filtered.sumOf { it.amount }
        tvTotal.text = "Total: ${moneyFmt.format(total)}"

        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvSales.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun monthKeyToLabel(mk: String): String {
        val parts = mk.split("-")
        if (parts.size != 2) return mk

        val year = parts[0]
        val m = parts[1].toIntOrNull() ?: return mk

        val month = when (m) {
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
