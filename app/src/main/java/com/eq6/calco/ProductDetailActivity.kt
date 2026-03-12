package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val productId = intent.getStringExtra("productId") ?: run {
            Toast.makeText(this, "Falta productId", Toast.LENGTH_LONG).show()
            finish(); return
        }

        val etName = findViewById<EditText>(R.id.etName)
        val etBarcode = findViewById<EditText>(R.id.etBarcode)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etStock = findViewById<EditText>(R.id.etStock)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        fun openMovement(type: String) {
            val i = Intent(this, InventoryMovementActivity::class.java)
            i.putExtra("productId", productId)
            i.putExtra("type", type) // "IN" o "ADJUST"
            startActivity(i)
        }

        findViewById<MaterialButton>(R.id.btnEntrada).setOnClickListener { openMovement("IN") }
        findViewById<MaterialButton>(R.id.btnAjuste).setOnClickListener { openMovement("ADJUST") }

        StoreSession.getStoreId(
            onOk = { storeId ->
                db.collection("stores").document(storeId)
                    .collection("products").document(productId)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            Toast.makeText(this, "Producto no existe", Toast.LENGTH_LONG).show()
                            finish(); return@addOnSuccessListener
                        }

                        etName.setText(doc.getString("name") ?: "")
                        etBarcode.setText(doc.getString("barcode") ?: "")
                        val price = doc.getDouble("price") ?: 0.0
                        val stock = doc.getLong("stock") ?: 0L

                        etPrice.setText(moneyFmt.format(price))
                        etStock.setText(stock.toString())
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
    }

    override fun onResume() {
        super.onResume()
    }
}