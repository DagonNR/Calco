package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        val etNote = findViewById<EditText>(R.id.etNote)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        db.collection("sales").document(saleId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Venta no existe", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val clientName = doc.getString("clientName") ?: ""
                val amount = doc.getDouble("amount") ?: 0.0
                val date = doc.getTimestamp("date")?.toDate()
                val note = doc.getString("note") ?: ""

                etClient.setText(clientName)
                etAmount.setText(moneyFmt.format(amount))
                etDate.setText(if (date != null) dateFmt.format(date) else "")
                etNote.setText(note.ifBlank { "" })
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }

        findViewById<MaterialButton>(R.id.btnReport).setOnClickListener {
            // Conexión a Reportes de ventas
            Toast.makeText(this, "Pendiente: pantalla Reportar", Toast.LENGTH_SHORT).show()
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
                    // Perfil
                    true
                }
                else -> false
            }
        }
    }
}