package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eq6.calco.adapters.DashboardSalesAdapter
import com.eq6.calco.models.DashboardSaleItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ClientDashboardActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    private lateinit var tvHello: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvMonth: TextView
    private lateinit var tvPurchasesTitle: TextView

    private lateinit var adapter: DashboardSalesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        tvHello = findViewById(R.id.tvHello)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvMonth = findViewById(R.id.tvMonth)
        tvPurchasesTitle = findViewById(R.id.tvPurchasesTitle)

        adapter = DashboardSalesAdapter(emptyList()) { sale ->
            val i = Intent(this, SaleDetailActivity::class.java)
            i.putExtra("saleId", sale.id)
            startActivity(i)
        }

        val rv = findViewById<RecyclerView>(R.id.rvLastPurchases)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavClient)
        bottom.selectedItemId = R.id.nav_home
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> {
                    startActivity(Intent(this, ClientHistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        loadDashboard()
    }

    private fun loadDashboard() {
        val user = auth.currentUser ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val monthKey = currentMonthKey()
        tvMonth.text = monthKeyToLabel(monthKey)
        tvPurchasesTitle.text = "Compras ${monthKeyToLabel(monthKey)}"

        StoreSession.getStoreId(
            onOk = { storeId ->
                val storeRef = db.collection("stores").document(storeId)

                // Nombre del cliente
                storeRef.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: (user.email ?: "Cliente")
                        tvHello.text = "Hola, $name\n(Cliente)"

                        // Compras del mes
                        storeRef.collection("sales")
                            .whereEqualTo("clientId", user.uid)
                            .whereEqualTo("monthKey", monthKey)
                            .get()
                            .addOnSuccessListener { snap ->
                                val docs = snap.documents

                                val total = docs.sumOf { it.getDouble("amount") ?: 0.0 }
                                tvTotalAmount.text = moneyFmt.format(total)

                                val last3 = docs
                                    .sortedByDescending { it.getTimestamp("date")?.toDate()?.time ?: 0L }
                                    .take(3)
                                    .map { d ->
                                        DashboardSaleItem(
                                            id = d.id,
                                            saleNumber = d.getString("saleNumber") ?: "",
                                            monthKey = d.getString("monthKey") ?: monthKey,
                                            amount = d.getDouble("amount") ?: 0.0
                                        )
                                    }

                                adapter.update(last3)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error compras: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error perfil: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        )
    }

    private fun currentMonthKey(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.getDefault(), "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
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