package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgot = findViewById(R.id.tvForgot)

        btnLogin.setOnClickListener { doLogin() }
        tvForgot.setOnClickListener { showResetPasswordDialog() }
    }

    private fun doLogin() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu email"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            return
        }
        if (pass.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                startActivity(Intent(this, RouterActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showResetPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Email"
            setText(etEmail.text.toString().trim())
        }

        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Te enviaremos un enlace para restablecer tu contraseña.")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Ingresa un email válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Revisa tu correo", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "Cargando..." else "Iniciar sesión"
    }
}