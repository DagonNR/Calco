package com.eq6.calco

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        val targetUid = intent.getStringExtra("uid") ?: run {
            Toast.makeText(this, "Falta uid", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etRole = findViewById<EditText>(R.id.etRole)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db.collection("usersIndex").document(currentUser.uid).get()
            .addOnSuccessListener { indexDoc ->
                val storeId = (indexDoc.getString("storeId") ?: "").trim()
                if (storeId.isBlank()) {
                    Toast.makeText(this, "No tienes tienda asignada", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val storeUserRef = db.collection("stores").document(storeId)
                    .collection("users").document(targetUid)

                storeUserRef.get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            Toast.makeText(this, "Usuario no existe en esta tienda", Toast.LENGTH_LONG).show()
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

                // Eliminar usuario, pero no asi mismo
                findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
                    val myUid = currentUser.uid
                    if (myUid == targetUid) {
                        Toast.makeText(this, "No puedes eliminar tu propia cuenta", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    AlertDialog.Builder(this)
                        .setTitle("Eliminar usuario")
                        .setMessage("Se eliminará el usuario de esta tienda. ¿Continuar?")
                        .setPositiveButton("Eliminar") { _, _ ->

                            val indexRef = db.collection("usersIndex").document(targetUid)

                            db.runBatch { batch ->
                                batch.delete(storeUserRef) // lo saca de la tienda
                                batch.delete(indexRef)     // lo deja sin acceso en router
                            }.addOnSuccessListener {
                                Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_LONG).show()
                                finish()
                            }.addOnFailureListener { e ->
                                Toast.makeText(this, "Error eliminando: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error leyendo tienda: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }
}