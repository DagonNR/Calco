package com.eq6.calco

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class CreateProductActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_product)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etName = findViewById<EditText>(R.id.etName)
        val etBarcode = findViewById<EditText>(R.id.etBarcode)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etStock = findViewById<EditText>(R.id.etStock)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val barcode = etBarcode.text.toString().trim()
            val price = etPrice.text.toString().trim().toDoubleOrNull()
            val stock = etStock.text.toString().trim().toLongOrNull() ?: 0L

            if (name.isEmpty()) { etName.error = "Ingresa nombre"; return@setOnClickListener }
            if (price == null || price <= 0) { etPrice.error = "Precio válido"; return@setOnClickListener }
            if (stock < 0) { etStock.error = "Stock no puede ser negativo"; return@setOnClickListener }

            btnSave.isEnabled = false
            btnSave.text = "Guardando..."

            StoreSession.getStoreId(
                onOk = { storeId ->
                    val data: MutableMap<String, Any> = mutableMapOf(
                        "name" to name,
                        "barcode" to barcode,
                        "price" to price,
                        "stock" to stock,
                        "active" to true,
                        "createdAt" to Timestamp.now()
                    )

                    db.collection("stores").document(storeId)
                        .collection("products")
                        .add(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Producto creado", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
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