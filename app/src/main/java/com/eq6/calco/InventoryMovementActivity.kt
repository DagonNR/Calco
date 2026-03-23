package com.eq6.calco

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InventoryMovementActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_movement)

        val productId = intent.getStringExtra("productId") ?: run {
            Toast.makeText(this, "Falta productId", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val type = intent.getStringExtra("type") ?: "IN" // IN o ADJUST

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)

        val tvUnitCostLabel = findViewById<TextView>(R.id.tvUnitCostLabel)
        val etUnitCost = findViewById<EditText>(R.id.etUnitCost)

        val tvAdjustTypeLabel = findViewById<TextView>(R.id.tvAdjustTypeLabel)
        val spAdjustDirection = findViewById<Spinner>(R.id.spAdjustDirection)
        val tvReasonLabel = findViewById<TextView>(R.id.tvReasonLabel)
        val etReason = findViewById<EditText>(R.id.etReason)

        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnBack.setOnClickListener { finish() }

        if (type == "IN") {
            tvTitle.text = "Entrada"
            tvUnitCostLabel.visibility = View.VISIBLE
            etUnitCost.visibility = View.VISIBLE

            tvAdjustTypeLabel.visibility = View.GONE
            spAdjustDirection.visibility = View.GONE
            tvReasonLabel.visibility = View.GONE
            etReason.visibility = View.GONE
        } else {
            tvTitle.text = "Ajuste"
            tvUnitCostLabel.visibility = View.GONE
            etUnitCost.visibility = View.GONE

            tvAdjustTypeLabel.visibility = View.VISIBLE
            spAdjustDirection.visibility = View.VISIBLE
            tvReasonLabel.visibility = View.VISIBLE
            etReason.visibility = View.VISIBLE

            val options = listOf("Sumar (error a favor)", "Restar (merma/roto)")
            spAdjustDirection.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        btnSave.setOnClickListener {
            val user = auth.currentUser ?: run {
                Toast.makeText(this, "Sesión no válida", Toast.LENGTH_LONG).show()
                finish(); return@setOnClickListener
            }

            val quantity = etQuantity.text.toString().trim().toLongOrNull()
            if (quantity == null || quantity <= 0) {
                etQuantity.error = "Cantidad válida"
                return@setOnClickListener
            }

            val note = etNote.text.toString().trim()

            val unitCost: Double? = if (type == "IN") {
                val v = etUnitCost.text.toString().trim().toDoubleOrNull()
                if (v == null || v <= 0) {
                    etUnitCost.error = "Costo válido"
                    return@setOnClickListener
                }
                v
            } else null

            val direction: String? = if (type == "ADJUST") {
                if (spAdjustDirection.selectedItemPosition == 0) "ADD" else "SUB"
            } else null

            val reason: String? = if (type == "ADJUST") {
                val r = etReason.text.toString().trim()
                if (r.isEmpty()) {
                    etReason.error = "Motivo requerido"
                    return@setOnClickListener
                }
                r
            } else null

            btnSave.isEnabled = false
            btnSave.text = "Guardando..."

            StoreSession.getStoreId(
                onOk = { storeId ->
                    val storeRef = db.collection("stores").document(storeId)
                    val productRef = storeRef.collection("products").document(productId)
                    val movementRef = storeRef.collection("inventory_movements").document()

                    db.runTransaction { tx ->
                        val pSnap = tx.get(productRef)
                        if (!pSnap.exists()) throw Exception("Producto no existe")

                        val currentStock = (pSnap.getLong("stock") ?: 0L)
                        val delta: Long = when (type) {
                            "IN" -> quantity
                            "ADJUST" -> if (direction == "ADD") quantity else -quantity
                            else -> 0L
                        }

                        val newStock = currentStock + delta
                        if (newStock < 0) throw Exception("Stock insuficiente para restar")

                        tx.update(productRef, "stock", newStock)

                        val movementData: MutableMap<String, Any> = mutableMapOf(
                            "productId" to productId,
                            "type" to type,
                            "quantity" to quantity,
                            "note" to note,
                            "createdById" to user.uid,
                            "createdAt" to Timestamp.now(),
                            "delta" to delta,
                            "stockBefore" to currentStock,
                            "stockAfter" to newStock
                        )

                        if (type == "IN" && unitCost != null) movementData["unitCost"] = unitCost
                        if (type == "ADJUST") {
                            movementData["direction"] = direction ?: "SUB"
                            movementData["reason"] = reason ?: ""
                        }

                        tx.set(movementRef, movementData)
                        true
                    }.addOnSuccessListener {
                        Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_LONG).show()
                        finish()
                    }.addOnFailureListener { e ->
                        btnSave.isEnabled = true
                        btnSave.text = "Guardar"
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onFail = { msg ->
                    btnSave.isEnabled = true
                    btnSave.text = "Guardar"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}