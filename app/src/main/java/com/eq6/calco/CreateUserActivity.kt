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
        tvCommissionLabel = findViewById(R.id.tvCommissionLabel)
        etCommissionPercent = findViewById(R.id.etCommissionPercent)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.roles_array,
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

                if (!isSeller) {
                    etCommissionPercent.setText("")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { createUserFlow() }
    }

    private fun createUserFlow() {
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

        val isSeller = role == "seller"
        val commissionRate: Double? = if (isSeller) {
            val percentText = etCommissionPercent.text.toString().trim()
            val percent = percentText.toDoubleOrNull()

            if (percent == null || percent <= 0 || percent > 100) {
                etCommissionPercent.error = "Ingresa un % válido (1 a 100)"
                setLoading(false)
                return
            }

            percent / 100.0
        } else null

        setLoading(true)

        val secondaryAuth = getSecondaryAuth()

        val tempPassword = generateTempPassword()

        secondaryAuth.createUserWithEmailAndPassword(email, tempPassword)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    setLoading(false)
                    Toast.makeText(this, "No se pudo obtener UID", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val data: MutableMap<String, Any> = mutableMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "createdAt" to Timestamp.now(),
                    "createdBy" to (mainAuth.currentUser?.uid ?: "unknown")
                )

                if (commissionRate != null) {
                    data["commissionRate"] = commissionRate
                }

                db.collection("users").document(uid).set(data)
                    .addOnSuccessListener {

                        secondaryAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                secondaryAuth.signOut()
                                setLoading(false)
                                Toast.makeText(
                                    this,
                                    "Usuario creado. Se envió correo para crear contraseña.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                setLoading(false)
                                Toast.makeText(this, "Usuario creado, pero error enviando correo: ${e.message}", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Error guardando perfil: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Error creando usuario: ${e.message}", Toast.LENGTH_LONG).show()
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
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(this, options, "Secondary")
        }

        return FirebaseAuth.getInstance(secondaryApp)
    }
}