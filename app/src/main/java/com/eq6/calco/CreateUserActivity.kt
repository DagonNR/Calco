package com.eq6.calco

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CreateUserActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val mainAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var spRole: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var tvCommissionLabel: TextView
    private lateinit var etCommissionPercent: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        spRole = findViewById(R.id.spRole)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        tvCommissionLabel = findViewById(R.id.tvCommissionLabel)
        etCommissionPercent = findViewById(R.id.etCommissionPercent)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.roles_array, // Admin, Vendedor, Cliente
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = adapter
        spRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val roleUi = spRole.selectedItem.toString().lowercase()
                val isSeller = roleUi == "vendedor"
                tvCommissionLabel.visibility = if (isSeller) View.VISIBLE else View.GONE
                etCommissionPercent.visibility = if (isSeller) View.VISIBLE else View.GONE
                if (!isSeller) etCommissionPercent.setText("")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { createUserFlow() }
    }

    private fun createUserFlow() {
        val admin = mainAuth.currentUser ?: run {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val roleUi = spRole.selectedItem.toString()

        if (name.isEmpty()) {
            etName.error = "Ingresa el nombre"
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un email válido"
            return
        }

        val role = when (roleUi.lowercase()) {
            "admin" -> "admin"
            "vendedor" -> "seller"
            "cliente" -> "client"
            else -> "client"
        }

        val commissionRate: Double? = if (role == "seller") {
            val percentText = etCommissionPercent.text.toString().trim()
            val percent = percentText.toDoubleOrNull()
            if (percent == null || percent <= 0 || percent > 100) {
                etCommissionPercent.error = "Ingresa un % válido (1 a 100)"
                return
            }
            percent / 100.0
        } else null

        setLoading(true)

        db.collection("usersIndex").document(admin.uid).get()
            .addOnSuccessListener { adminIndex ->
                val storeId = (adminIndex.getString("storeId") ?: "").trim()
                if (storeId.isBlank()) {
                    setLoading(false)
                    Toast.makeText(this, "No tienes tienda asignada. Crea una tienda primero.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val secondaryAuth = getSecondaryAuth()
                val tempPassword = generateTempPassword()

                secondaryAuth.createUserWithEmailAndPassword(email, tempPassword)
                    .addOnSuccessListener { result ->
                        val newUid = result.user?.uid ?: run {
                            setLoading(false)
                            Toast.makeText(this, "No se pudo obtener UID del nuevo usuario", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        val indexRef = db.collection("usersIndex").document(newUid)
                        val storeUserRef = db.collection("stores").document(storeId)
                            .collection("users").document(newUid)

                        val indexData: MutableMap<String, Any> = mutableMapOf(
                            "storeId" to storeId,
                            "role" to role,
                            "name" to name,
                            "email" to email
                        )
                        if (commissionRate != null) indexData["commissionRate"] = commissionRate

                        val storeUserData: MutableMap<String, Any> = mutableMapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role,
                            "createdAt" to Timestamp.now(),
                            "createdBy" to admin.uid
                        )
                        if (commissionRate != null) storeUserData["commissionRate"] = commissionRate

                        db.runBatch { batch ->
                            batch.set(indexRef, indexData)
                            batch.set(storeUserRef, storeUserData)
                        }.addOnSuccessListener {

                            secondaryAuth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    setLoading(false)
                                    Toast.makeText(this, "Usuario creado. Se envió correo para crear contraseña.", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    setLoading(false)
                                    Toast.makeText(this, "Usuario creado, pero error enviando correo: ${e.message}", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                        }.addOnFailureListener { e ->
                            setLoading(false)
                            Toast.makeText(this, "Error guardando usuario: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Error creando usuario: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Error leyendo tienda: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        btnSave.isEnabled = !loading
        btnSave.text = if (loading) "Guardando..." else "Guardar"
    }

    private fun generateTempPassword(): String {
        val token = UUID.randomUUID().toString().replace("-", "").take(10)
        return "Aa1!$token"
    }

    private fun getSecondaryAuth(): FirebaseAuth {
        val primaryApp = FirebaseApp.getInstance()
        val options: FirebaseOptions = primaryApp.options

        val secondaryApp = try {
            FirebaseApp.getInstance("Secondary")
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(this, options, "Secondary")
        }

        return FirebaseAuth.getInstance(secondaryApp)
    }
}