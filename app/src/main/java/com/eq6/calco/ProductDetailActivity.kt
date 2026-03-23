package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private var storeId: String = ""
    private var productId: String = ""

    private lateinit var etName: EditText
    private lateinit var etBarcode: EditText
    private lateinit var etPrice: EditText
    private lateinit var etStock: EditText

    private lateinit var btnEdit: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var swActive: SwitchMaterial

    private var isEditMode = false

    private var currentActive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        productId = intent.getStringExtra("productId") ?: run {
            Toast.makeText(this, "Falta productId", Toast.LENGTH_LONG).show()
            finish(); return
        }

        etName = findViewById(R.id.etName)
        etBarcode = findViewById(R.id.etBarcode)
        etPrice = findViewById(R.id.etPrice)
        etStock = findViewById(R.id.etStock)

        btnEdit = findViewById(R.id.btnEdit)
        btnSave = findViewById(R.id.btnSave)
        swActive = findViewById(R.id.swActive)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        fun openMovement(type: String) {
            val i = Intent(this, InventoryMovementActivity::class.java)
            i.putExtra("productId", productId)
            i.putExtra("type", type)
            startActivity(i)
        }

        findViewById<MaterialButton>(R.id.btnEntrada).setOnClickListener { openMovement("IN") }
        findViewById<MaterialButton>(R.id.btnAjuste).setOnClickListener { openMovement("ADJUST") }

        btnEdit.setOnClickListener { setEditMode(true) }
        btnSave.setOnClickListener { saveChanges() }

        setEditMode(false)

        StoreSession.getStoreId(
            onOk = { sid ->
                storeId = sid
                loadProduct()
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (storeId.isNotBlank()) loadProduct()
    }

    private fun productRef() =
        db.collection("stores").document(storeId).collection("products").document(productId)

    private fun loadProduct() {
        productRef().get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Producto no existe", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val name = doc.getString("name") ?: ""
                val barcode = doc.getString("barcode") ?: ""
                val price = doc.getDouble("price") ?: 0.0
                val stock = doc.getLong("stock") ?: 0L
                val active = doc.getBoolean("active") ?: true

                currentActive = active

                etName.setText(name)
                etBarcode.setText(barcode)
                etPrice.setText(price.toString())
                etStock.setText(stock.toString())
                swActive.isChecked = active

                setEditMode(isEditMode)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled

        etName.isEnabled = false
        etStock.isEnabled = false

        etBarcode.isEnabled = enabled
        etPrice.isEnabled = enabled

        swActive.visibility = if (enabled) View.VISIBLE else View.GONE
        btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
        btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun saveChanges() {
        val barcode = etBarcode.text.toString().trim()

        val price = etPrice.text.toString().trim().toDoubleOrNull()
        if (price == null || price <= 0) {
            etPrice.error = "Precio válido"
            return
        }

        val active = swActive.isChecked

        btnSave.isEnabled = false
        btnSave.text = "Guardando..."

        val data: MutableMap<String, Any> = mutableMapOf(
            "price" to price,
            "barcode" to barcode,
            "active" to active
        )

        productRef().update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnSave.text = "Guardar"
                setEditMode(false)
                loadProduct()
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                btnSave.text = "Guardar"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}