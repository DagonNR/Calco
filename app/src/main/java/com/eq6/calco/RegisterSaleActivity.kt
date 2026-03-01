package com.eq6.calco

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ClientOption(val uid: String, val name: String) {
    override fun toString(): String = name
}

class RegisterSaleActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var btnBack: ImageButton
    private lateinit var spClient: Spinner
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var btnPickDate: ImageButton
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button

    private val selectedCal = Calendar.getInstance()
    private val dateFmt = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    private var clients: List<ClientOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_sale)

        btnBack = findViewById(R.id.btnBack)
        spClient = findViewById(R.id.spClient)
        etAmount = findViewById(R.id.etAmount)
        etDate = findViewById(R.id.etDate)
        btnPickDate = findViewById(R.id.btnPickDate)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)

        btnBack.setOnClickListener { finish() }

        etDate.setText(dateFmt.format(selectedCal.time))
        etDate.setOnClickListener { openDatePicker() }
        btnPickDate.setOnClickListener { openDatePicker() }

        loadClients()

        btnSave.setOnClickListener { saveSale() }
    }

    private fun formatSaleNumber(n: Long): String {
        return "A" + n.toString().padStart(4, '0')
    }
    private fun loadClients() {
        btnSave.isEnabled = false

        db.collection("users")
            .whereEqualTo("role", "client")
            .get()
            .addOnSuccessListener { snap ->
                clients = snap.documents.map { doc ->
                    ClientOption(
                        uid = doc.id,
                        name = doc.getString("name") ?: "(Sin nombre)"
                    )
                }.sortedBy { it.name.lowercase() }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    clients
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spClient.adapter = adapter

                btnSave.isEnabled = clients.isNotEmpty()
                if (clients.isEmpty()) {
                    Toast.makeText(this, "No hay clientes registrados", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cargando clientes: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openDatePicker() {
        val y = selectedCal.get(Calendar.YEAR)
        val m = selectedCal.get(Calendar.MONTH)
        val d = selectedCal.get(Calendar.DAY_OF_MONTH)

        val picker = android.app.DatePickerDialog(this, { _, year, month, day ->
            selectedCal.set(Calendar.YEAR, year)
            selectedCal.set(Calendar.MONTH, month)
            selectedCal.set(Calendar.DAY_OF_MONTH, day)
            etDate.setText(dateFmt.format(selectedCal.time))
        }, y, m, d)

        picker.show()
    }

    private fun saveSale() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (clients.isEmpty()) {
            Toast.makeText(this, "No hay clientes para seleccionar", Toast.LENGTH_LONG).show()
            return
        }

        val client = spClient.selectedItem as ClientOption
        val amountText = etAmount.text.toString().trim()
        val note = etNote.text.toString().trim()

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Ingresa un monto válido"
            return
        }

        setLoading(true)

        val sellerId = user.uid
        val saleDate = Timestamp(selectedCal.time)
        val d = selectedCal.time
        val cal = Calendar.getInstance().apply { time = d }
        val monthKey = String.format(
            Locale.getDefault(),
            "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )

        db.collection("users").document(sellerId).get()
            .addOnSuccessListener { sellerDoc ->
                val sellerName = sellerDoc.getString("name") ?: (user.email ?: "Vendedor")
                val rate = sellerDoc.getDouble("commissionRate") ?: 0.03

                val commissionAmount = amount * rate

                val saleData: MutableMap<String, Any> = mutableMapOf(
                    "sellerId" to sellerId,
                    "sellerName" to sellerName,
                    "clientId" to client.uid,
                    "clientName" to client.name,
                    "amount" to amount,
                    "date" to saleDate,
                    "note" to note,
                    "monthKey" to monthKey,
                    "commissionRateUsed" to rate,
                    "commissionAmount" to commissionAmount,
                    "createdAt" to Timestamp.now()
                )

                val counterRef = db.collection("counters").document("sales")

                db.runTransaction { tx ->
                    val snap = tx.get(counterRef)
                    val last = (snap.getLong("lastNumber") ?: 0L)
                    val next = last + 1L

                    tx.update(counterRef, "lastNumber", next)

                    val saleNumber = formatSaleNumber(next)

                    saleData["saleNumber"] = saleNumber

                    val saleRef = db.collection("sales").document()
                    tx.set(saleRef, saleData)

                    saleNumber
                }.addOnSuccessListener { saleNumber ->
                    setLoading(false)
                    Toast.makeText(this, "Venta registrada: #$saleNumber", Toast.LENGTH_LONG).show()
                    finish()
                }.addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Error guardando venta: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Error leyendo vendedor: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        btnSave.isEnabled = !loading
        btnSave.text = if (loading) "Guardando..." else "Guardar"
    }
}