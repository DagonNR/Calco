package com.eq6.calco

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class RegisterStoreAdminActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_store_admin)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreate)

        btnCreate.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Ingresa el nombre"
                return@setOnClickListener
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Ingresa un correo válido"
                return@setOnClickListener
            }

            btnCreate.isEnabled = false
            btnCreate.text = "Creando..."

            val tempPassword = generateTempPassword()

            auth.createUserWithEmailAndPassword(email, tempPassword)
                .addOnSuccessListener { result ->
                    val user = result.user ?: run {
                        btnCreate.isEnabled = true
                        btnCreate.text = "Crear cuenta"
                        Toast.makeText(this, "No se pudo obtener usuario", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val uid = user.uid

                    val indexData: MutableMap<String, Any> = mutableMapOf(
                        "role" to "admin",
                        "name" to name,
                        "email" to email,
                        "storeId" to "",
                        "needsStoreSetup" to true,
                        "createdAt" to Timestamp.now()
                    )

                    db.collection("usersIndex").document(uid).set(indexData)
                        .addOnSuccessListener {
                            sendVerificationAndPasswordEmail(email, btnCreate)
                        }
                        .addOnFailureListener { e ->
                            btnCreate.isEnabled = true
                            btnCreate.text = "Crear cuenta"
                            Toast.makeText(this, "Error guardando índice: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnCreate.isEnabled = true
                    btnCreate.text = "Crear cuenta"
                    Toast.makeText(this, "Error creando cuenta: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun sendVerificationAndPasswordEmail(
        email: String,
        btnCreate: MaterialButton
    ) {
        val user = auth.currentUser

        user?.sendEmailVerification()
            ?.addOnSuccessListener {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        auth.signOut()
                        btnCreate.isEnabled = true
                        btnCreate.text = "Crear cuenta"

                        Toast.makeText(
                            this,
                            "Cuenta creada. Revisa tu correo para verificarlo y crear contraseña.",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                    .addOnFailureListener { e ->
                        auth.signOut()
                        btnCreate.isEnabled = true
                        btnCreate.text = "Crear cuenta"
                        Toast.makeText(
                            this,
                            "Cuenta creada, pero error enviando contraseña: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
            }
            ?.addOnFailureListener { e ->
                auth.signOut()
                btnCreate.isEnabled = true
                btnCreate.text = "Crear cuenta"
                Toast.makeText(
                    this,
                    "Cuenta creada, pero error enviando verificación: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
    }

    private fun generateTempPassword(): String {
        val token = UUID.randomUUID().toString().replace("-", "").take(10)
        return "Aa1!$token"
    }
}