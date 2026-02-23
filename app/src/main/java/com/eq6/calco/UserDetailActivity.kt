package com.eq6.calco

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class UserDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        val uid = intent.getStringExtra("uid") ?: run {
            Toast.makeText(this, "Falta uid", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etRole = findViewById<EditText>(R.id.etRole)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Usuario no existe", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                etName.setText(doc.getString("name") ?: "")
                etEmail.setText(doc.getString("email") ?: "")

                val role = (doc.getString("role") ?: "").lowercase().trim()
                val roleEs = when (role) {
                    "admin" -> "Admin"
                    "seller" -> "Vendedor"
                    "client" -> "Cliente"
                    else -> role
                }
                etRole.setText(roleEs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }

        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {

            val myUid = FirebaseAuth.getInstance().currentUser?.uid
            if (myUid != null && myUid == uid) {
                Toast.makeText(this, "No puedes eliminar tu propia cuenta", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("Se eliminará el perfil del usuario. ¿Continuar?")
                .setPositiveButton("Eliminar") { _, _ ->
                    db.collection("users").document(uid).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}