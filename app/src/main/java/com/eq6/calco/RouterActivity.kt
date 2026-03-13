package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RouterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        val uid = user.uid

        db.collection("usersIndex").document(uid).get()
            .addOnSuccessListener { doc ->
                // Si no existe el doc, mandamos a crear tienda
                if (!doc.exists()) {
                    startActivity(Intent(this, CreateStoreActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                val role = (doc.getString("role") ?: "").lowercase().trim()
                val storeId = (doc.getString("storeId") ?: "").trim()
                val name = doc.getString("name") ?: (user.email ?: "Usuario")

                // Si existe el doc pero no hay storeId, también mandamos a crear tienda
                if (storeId.isBlank()) {
                    startActivity(Intent(this, CreateStoreActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                when (role) {
                    "admin" -> goToAdmin(name)
                    "seller" -> goToSeller(name)
                    "client" -> goToClient(name)
                    else -> {
                        Toast.makeText(this, "Rol inválido: $role", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        goToLogin()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error leyendo usersIndex: ${e.message}", Toast.LENGTH_LONG).show()
                auth.signOut()
                goToLogin()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToAdmin(name: String) {
        startActivity(Intent(this, AdminDashboardActivity::class.java).putExtra("name", name))
        finish()
    }

    private fun goToSeller(name: String) {
        startActivity(Intent(this, SellerDashboardActivity::class.java).putExtra("name", name))
        finish()
    }

    private fun goToClient(name: String) {
        startActivity(Intent(this, ClientDashboardActivity::class.java).putExtra("name", name))
        finish()
    }
}