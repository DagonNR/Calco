package com.eq6.calco

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.eq6.calco.adapters.CartAdapter
import com.eq6.calco.models.CartItem
import com.eq6.calco.models.ProductOption
import com.eq6.calco.models.ClientOption
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class RegisterSaleCartActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    private lateinit var spClient: Spinner
    private lateinit var spProduct: Spinner
    private lateinit var etQty: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnFinish: Button
    private lateinit var tvTotal: TextView
    private lateinit var rvCart: androidx.recyclerview.widget.RecyclerView

    private var clients: List<ClientOption> = emptyList()
    private var products: List<ProductOption> = emptyList()
    private var cart: MutableList<CartItem> = mutableListOf()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_sale_cart)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        spClient = findViewById(R.id.spClient)
        spProduct = findViewById(R.id.spProduct)
        etQty = findViewById(R.id.etQty)
        btnAdd = findViewById(R.id.btnAdd)
        btnFinish = findViewById(R.id.btnFinish)
        tvTotal = findViewById(R.id.tvTotal)
        rvCart = findViewById(R.id.rvCart)

        cartAdapter = CartAdapter(cart) { item ->
            cart.removeIf { it.productId == item.productId }
            cartAdapter.setItems(cart)
            updateTotal()
        }

        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.adapter = cartAdapter

        loadClientsAndProducts()

        btnAdd.setOnClickListener { addToCart() }
        btnFinish.setOnClickListener { finishSale() }

        updateTotal()
    }

    private fun loadClientsAndProducts() {
        btnFinish.isEnabled = false

        StoreSession.getStoreId(
            onOk = { storeId ->
                db.collection("stores").document(storeId).collection("users")
                    .whereEqualTo("role", "client")
                    .get()
                    .addOnSuccessListener { snap ->
                        clients = snap.documents.map { doc ->
                            ClientOption(doc.id, doc.getString("name") ?: "(Sin nombre)")
                        }.sortedBy { it.name.lowercase() }

                        spClient.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, clients).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }

                        db.collection("stores").document(storeId).collection("products")
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener { psnap ->
                                products = psnap.documents.map { doc ->
                                    ProductOption(
                                        id = doc.id,
                                        name = doc.getString("name") ?: "",
                                        barcode = doc.getString("barcode") ?: "",
                                        price = doc.getDouble("price") ?: 0.0,
                                        stock = doc.getLong("stock") ?: 0L,
                                        active = doc.getBoolean("active") ?: true
                                    )
                                }.sortedBy { it.name.lowercase() }

                                spProduct.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, products).apply {
                                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                }

                                btnFinish.isEnabled = true

                                if (clients.isEmpty()) Toast.makeText(this, "No hay clientes", Toast.LENGTH_LONG).show()
                                if (products.isEmpty()) Toast.makeText(this, "No hay productos", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error productos: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error clientes: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun addToCart() {
        if (products.isEmpty()) {
            Toast.makeText(this, "No hay productos", Toast.LENGTH_LONG).show()
            return
        }

        val p = spProduct.selectedItem as ProductOption
        val qty = etQty.text.toString().trim().toLongOrNull()

        if (qty == null || qty <= 0) {
            etQty.error = "Cantidad válida"
            return
        }

        if (qty > p.stock) {
            Toast.makeText(this, "Stock insuficiente (stock: ${p.stock})", Toast.LENGTH_LONG).show()
            return
        }

        val existing = cart.find { it.productId == p.id }
        if (existing != null) {
            val newQty = existing.quantity + qty
            if (newQty > p.stock) {
                Toast.makeText(this, "Stock insuficiente para acumular (stock: ${p.stock})", Toast.LENGTH_LONG).show()
                return
            }
            existing.quantity = newQty
        } else {
            cart.add(CartItem(p.id, p.name, p.barcode, p.price, qty))
        }

        etQty.setText("")
        cartAdapter.setItems(cart)
        updateTotal()
    }

    private fun updateTotal() {
        val total = cart.sumOf { it.subtotal() }
        tvTotal.text = "Total: ${moneyFmt.format(total)}"
        btnFinish.isEnabled = cart.isNotEmpty()
    }

    private fun finishSale() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (clients.isEmpty()) {
            Toast.makeText(this, "No hay clientes", Toast.LENGTH_LONG).show()
            return
        }
        if (cart.isEmpty()) {
            Toast.makeText(this, "Carrito vacío", Toast.LENGTH_LONG).show()
            return
        }

        val client = spClient.selectedItem as ClientOption
        val total = cart.sumOf { it.subtotal() }

        btnFinish.isEnabled = false
        btnFinish.text = "Procesando..."

        StoreSession.getStoreId(
            onOk = { storeId ->
                val storeRef = db.collection("stores").document(storeId)
                val salesCol = storeRef.collection("sales")
                val counterRef = storeRef.collection("counters").document("sales")
                val sellerRef = storeRef.collection("users").document(user.uid)

                val cal = Calendar.getInstance()
                val monthKey = String.format(Locale.getDefault(), "%04d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1
                )
                val saleDate = Timestamp.now()

                db.runTransaction { tx ->
                    val cSnap = tx.get(counterRef)
                    val last = cSnap.getLong("lastNumber") ?: 0L
                    val next = last + 1L
                    val saleNumber = "A" + next.toString().padStart(4, '0')

                    val sSnap = tx.get(sellerRef)
                    val sellerName = sSnap.getString("name") ?: (user.email ?: "Vendedor")
                    val rate = sSnap.getDouble("commissionRate") ?: 0.03
                    val commissionAmount = total * rate

                    val productRefs = cart.map { item ->
                        item.productId to storeRef.collection("products").document(item.productId)
                    }

                    val productSnaps = productRefs.associate { (pid, ref) ->
                        pid to tx.get(ref)
                    }

                    cart.forEach { item ->
                        val pSnap = productSnaps[item.productId] ?: throw Exception("Producto no existe: ${item.productName}")
                        if (!pSnap.exists()) throw Exception("Producto no existe: ${item.productName}")
                        val currentStock = pSnap.getLong("stock") ?: 0L
                        if (currentStock < item.quantity) {
                            throw Exception("Stock insuficiente para ${item.productName} (stock: $currentStock)")
                        }
                    }

                    tx.update(counterRef, "lastNumber", next)

                    val saleRef = salesCol.document()
                    val saleData: MutableMap<String, Any> = mutableMapOf(
                        "saleNumber" to saleNumber,
                        "sellerId" to user.uid,
                        "sellerName" to sellerName,
                        "clientId" to client.uid,
                        "clientName" to client.name,
                        "amount" to total,
                        "total" to total,
                        "date" to saleDate,
                        "monthKey" to monthKey,
                        "commissionRateUsed" to rate,
                        "commissionAmount" to commissionAmount,
                        "createdAt" to Timestamp.now()
                    )
                    tx.set(saleRef, saleData)

                    cart.forEach { item ->
                        val pSnap = productSnaps[item.productId]!!
                        val currentStock = pSnap.getLong("stock") ?: 0L
                        val newStock = currentStock - item.quantity

                        val productRef = storeRef.collection("products").document(item.productId)
                        tx.update(productRef, "stock", newStock)

                        val itemRef = saleRef.collection("items").document()
                        val itemData: MutableMap<String, Any> = mutableMapOf(
                            "productId" to item.productId,
                            "productName" to item.productName,
                            "barcode" to item.barcode,
                            "unitPrice" to item.unitPrice,
                            "quantity" to item.quantity,
                            "subtotal" to item.subtotal()
                        )
                        tx.set(itemRef, itemData)

                        val movRef = storeRef.collection("inventory_movements").document()
                        val movData: MutableMap<String, Any> = mutableMapOf(
                            "type" to "OUT",
                            "productId" to item.productId,
                            "quantity" to item.quantity,
                            "delta" to (-item.quantity),
                            "saleId" to saleRef.id,
                            "saleNumber" to saleNumber,
                            "createdById" to user.uid,
                            "createdAt" to Timestamp.now(),
                            "stockBefore" to currentStock,
                            "stockAfter" to newStock
                        )
                        tx.set(movRef, movData)
                    }

                    saleNumber
                }.addOnSuccessListener { saleNumber ->
                    Toast.makeText(this, "Venta registrada: #$saleNumber", Toast.LENGTH_LONG).show()
                    finish()
                }.addOnFailureListener { e ->
                    btnFinish.isEnabled = true
                    btnFinish.text = "Finalizar venta"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            onFail = { msg ->
                btnFinish.isEnabled = true
                btnFinish.text = "Finalizar venta"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        )
    }
}