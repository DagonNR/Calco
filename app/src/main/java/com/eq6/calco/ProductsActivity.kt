package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.eq6.calco.adapters.ProductsAdapter
import com.eq6.calco.models.ProductItem

class ProductsActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etSearch: EditText
    private lateinit var spFilter: Spinner
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ProductsAdapter
    private lateinit var btnBack: ImageButton
    private var allProducts: List<ProductItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        etSearch = findViewById(R.id.etSearch)
        spFilter = findViewById(R.id.spFilter)
        rv = findViewById(R.id.rvProducts)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        val filterOptions = listOf("Todos", "Activos", "Desactivados")
        spFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        adapter = ProductsAdapter(emptyList()) { p ->
            val i = Intent(this, ProductDetailActivity::class.java)
            i.putExtra("productId", p.id)
            startActivity(i)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.fabAdd)
            .setOnClickListener {
                startActivity(Intent(this, CreateProductActivity::class.java))
            }

        etSearch.doAfterTextChanged { applyFilters() }
        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun loadProducts() {
        StoreSession.getStoreId(
            onOk = { storeId ->
                db.collection("stores").document(storeId)
                    .collection("products")
                    .get()
                    .addOnSuccessListener { snap ->
                        allProducts = snap.documents.map { doc ->
                            ProductItem(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                barcode = doc.getString("barcode") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                stock = doc.getLong("stock") ?: 0L,
                                active = doc.getBoolean("active") ?: true
                            )
                        }.sortedBy { it.name.lowercase() }

                        applyFilters()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            onFail = { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        )
    }

    private fun applyFilters() {
        val q = etSearch.text.toString().trim().lowercase()
        val filter = spFilter.selectedItem.toString()

        val filtered = allProducts.filter { p ->
            val matchesQuery = q.isBlank() ||
                    p.name.lowercase().contains(q) ||
                    p.barcode.lowercase().contains(q)

            val matchesFilter = when (filter) {
                "Activos" -> p.active
                "Desactivados" -> !p.active
                else -> true
            }

            matchesQuery && matchesFilter
        }

        adapter.update(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        rv.visibility = if (filtered.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }
}
