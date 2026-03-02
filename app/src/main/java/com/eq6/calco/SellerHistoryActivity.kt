package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.eq6.calco.adapters.SalesAdapter
import com.eq6.calco.models.SaleItem
import java.util.Locale

data class FilterClientOption(val id: String, val name: String) {
    override fun toString(): String = name
}

class SellerHistoryActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var ivFilter: ImageView
    private lateinit var filterPanel: LinearLayout
    private lateinit var spMonth: Spinner
    private lateinit var spClient: Spinner
    private lateinit var btnApply: Button

    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SalesAdapter

    private var allSales: List<SaleItem> = emptyList()
    private var months: List<String> = emptyList()
    private var clients: List<FilterClientOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_history)

        ivFilter = findViewById(R.id.ivFilter)
        filterPanel = findViewById(R.id.filterPanel)
        spMonth = findViewById(R.id.spMonth)
        spClient = findViewById(R.id.spClient)
        btnApply = findViewById(R.id.btnApply)
        rv = findViewById(R.id.rvSales)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = SalesAdapter(emptyList()) { sale ->
            val i = Intent(this, SaleDetailActivity::class.java)
            i.putExtra("saleId", sale.id)
            startActivity(i)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        rv.setHasFixedSize(true)

        ivFilter.setOnClickListener {
            filterPanel.visibility = if (filterPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnApply.setOnClickListener {
            applyFilters()
            filterPanel.visibility = View.GONE
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavSeller)
        bottom.selectedItemId = R.id.nav_history
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, SellerDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        loadSales()
    }

    private fun loadSales() {
        val user = auth.currentUser ?: run {
            finish()
            return
        }

        db.collection("sales")
            .whereEqualTo("sellerId", user.uid)
            .get()
            .addOnSuccessListener { snap ->
                allSales = snap.documents.map { doc ->
                    SaleItem(
                        id = doc.id,
                        saleNumber = doc.getString("saleNumber") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        monthKey = doc.getString("monthKey") ?: "",
                        date = doc.getTimestamp("date") ?: Timestamp.now(),
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: ""
                    )
                }.sortedByDescending { it.date?.toDate()?.time ?: 0L }

                buildFilters()
                applyFilters()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cargando ventas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun buildFilters() {
        months = allSales.mapNotNull {
            it.date?.toDate()
        }.map {
            String.format(Locale.getDefault(), "%04d-%02d", it.year + 1900, it.month + 1)
        }.distinct().sortedDescending()

        val monthOptions = mutableListOf("Todos")
        monthOptions.addAll(months)

        spMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monthOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        clients = allSales
            .map { FilterClientOption(it.clientId, it.clientName.ifBlank { it.clientId }) }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        val clientOptions = mutableListOf(FilterClientOption("", "Todos"))
        clientOptions.addAll(clients)

        spClient.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, clientOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun applyFilters() {
        val monthSelected = spMonth.selectedItem.toString() // "Todos" o "YYYY-MM"
        val clientSelected = spClient.selectedItem as FilterClientOption

        val filtered = allSales.filter { sale ->
            val okClient = clientSelected.id.isEmpty() || sale.clientId == clientSelected.id

            val okMonth = if (monthSelected == "Todos") true else {
                val d = sale.date?.toDate()
                if (d == null) false else {
                    val mk = String.format(Locale.getDefault(), "%04d-%02d", d.year + 1900, d.month + 1)
                    mk == monthSelected
                }
            }

            okClient && okMonth
        }

        adapter.update(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}