package com.eq6.calco

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportSaleActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_sale)

        val saleId = intent.getStringExtra("saleId") ?: run {
            Toast.makeText(this, "Falta saleId", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val saleNumber = intent.getStringExtra("saleNumber") ?: ""

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val spReason = findViewById<Spinner>(R.id.spReason)
        val etDesc = findViewById<EditText>(R.id.etDescription)
        val btnSend = findViewById<Button>(R.id.btnSend)

        btnBack.setOnClickListener { finish() }

        val reasonsAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.report_reasons_array,
            android.R.layout.simple_spinner_item
        )
        reasonsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spReason.adapter = reasonsAdapter

        btnSend.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Sesión no válida", Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            btnSend.text = "Enviando..."

            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { reporterDoc ->
                    val reporterName = reporterDoc.getString("name") ?: (user.email ?: "Usuario")
                    val reporterRole = reporterDoc.getString("role") ?: "unknown"

                    val reportData = hashMapOf(
                        "saleId" to saleId,
                        "saleNumber" to saleNumber,
                        "reason" to spReason.selectedItem.toString(),
                        "description" to etDesc.text.toString().trim(),
                        "status" to "pending",
                        "createdAt" to Timestamp.now(),
                        "createdById" to user.uid,
                        "createdByName" to reporterName,
                        "createdByRole" to reporterRole
                    )

                    db.collection("reports").add(reportData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reporte enviado", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnSend.isEnabled = true
                            btnSend.text = "Enviar"
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnSend.isEnabled = true
                    btnSend.text = "Enviar"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavSeller)
        bottom.selectedItemId = R.id.nav_history
    }
}