package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eq6.calco.adapters.SaleProductsAdapter
import com.eq6.calco.models.SaleProductItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SaleDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFmt = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    private var saleNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_detail)

        val saleId = intent.getStringExtra("saleId") ?: run {
            Toast.makeText(this, "Falta saleId", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val etClient = findViewById<EditText>(R.id.etClient)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etDate = findViewById<EditText>(R.id.etDate)
        val rvSaleItems = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSaleItems)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val itemsAdapter = SaleProductsAdapter(emptyList())
        rvSaleItems.layoutManager = LinearLayoutManager(this)
        rvSaleItems.adapter = itemsAdapter

        StoreSession.getStoreId(
            onOk = { storeId ->
                db.collection("stores").document(storeId)
                    .collection("sales").document(saleId).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            Toast.makeText(this, "Venta no existe", Toast.LENGTH_LONG).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        // Cargar productos de la subcolección 'items'
                        db.collection("stores").document(storeId)
                            .collection("sales").document(saleId)
                            .collection("items")
                            .get()
                            .addOnSuccessListener { snap ->
                                val items = snap.documents.map { d ->
                                    SaleProductItem(
                                        productName = d.getString("productName") ?: "",
                                        unitPrice = d.get("unitPrice")?.toString()?.toDoubleOrNull() ?: 0.0,
                                        quantity = d.getLong("quantity") ?: 0L,
                                        subtotal = d.get("subtotal")?.toString()?.toDoubleOrNull() ?: 0.0
                                    )
                                }
                                itemsAdapter.update(items)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error productos: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                        val clientName = doc.getString("clientName") ?: ""
                        val amount = doc.getDouble("amount") ?: 0.0
                        val date = doc.getTimestamp("date")?.toDate()
                        saleNumber = doc.getString("saleNumber") ?: ""

                        etClient.setText(clientName)
                        etAmount.setText(moneyFmt.format(amount))
                        etDate.setText(if (date != null) dateFmt.format(date) else "")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        )

        findViewById<MaterialButton>(R.id.btnReport).setOnClickListener {
            val i = Intent(this, ReportSaleActivity::class.java)
            i.putExtra("saleId", saleId)
            i.putExtra("saleNumber", saleNumber)
            startActivity(i)
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavSeller)
        bottom.selectedItemId = R.id.nav_history
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> {
                    finish()
                    true
                }
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
    }
}
