package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etRole = findViewById<EditText>(R.id.etRole)
        val tvCommissionLabel = findViewById<View>(R.id.tvCommissionLabel)
        val etCommission = findViewById<EditText>(R.id.etCommission)

        btnBack.setOnClickListener { finish() }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        val user = auth.currentUser ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        StoreSession.getStoreId(
            onOk = { storeId ->
                db.collection("stores").document(storeId)
                    .collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            Toast.makeText(this, "Perfil no encontrado", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                            return@addOnSuccessListener
                        }

                        val name = doc.getString("name") ?: ""
                        val email = doc.getString("email") ?: (user.email ?: "")
                        val role = (doc.getString("role") ?: "").lowercase().trim()

                        etName.setText(name)
                        etEmail.setText(email)

                        val roleEs = when (role) {
                            "admin" -> "Admin"
                            "seller" -> "Vendedor"
                            "client" -> "Cliente"
                            else -> role
                        }
                        etRole.setText(roleEs)

                        if (role == "seller") {
                            tvCommissionLabel.visibility = View.VISIBLE
                            etCommission.visibility = View.VISIBLE

                            val rate = doc.getDouble("commissionRate") ?: 0.03
                            val percent = rate * 100.0
                            etCommission.setText("${String.format("%.0f", percent)}%")
                        } else {
                            tvCommissionLabel.visibility = View.GONE
                            etCommission.visibility = View.GONE
                        }

                        setupBottomNav(role)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
                }
        )
    }

    private fun setupBottomNav(role: String) {
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavGeneric)

        val menuRes = when (role) {
            "admin" -> R.menu.menu_admin_bottom
            "seller" -> R.menu.menu_seller_bottom
            "client" -> R.menu.menu_client_bottom
            else -> R.menu.menu_seller_bottom
        }
        bottom.menu.clear()
        bottom.inflateMenu(menuRes)

        bottom.selectedItemId = when (role) {
            "seller", "client" -> R.id.nav_profile
            else -> R.id.nav_home
        }

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Vendedor/Cliente
                R.id.nav_history -> {
                    if (role == "seller") startActivity(Intent(this, SellerHistoryActivity::class.java))
                    //else startActivity(Intent(this, ClientHistoryActivity::class.java)) QUITAR CUANDO SE IMPLEMENTE EL CLIENTE
                    finish()
                    true
                }
                R.id.nav_home -> {
                    when (role) {
                        "admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                        "seller" -> startActivity(Intent(this, SellerDashboardActivity::class.java))
                        "client" -> startActivity(Intent(this, ClientDashboardActivity::class.java))
                    }
                    finish()
                    true
                }
                R.id.nav_profile -> true

                // Admin
                R.id.nav_users -> {
                    startActivity(Intent(this, UsersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_report -> {
                    // Reportes de admins
                    true
                }
                else -> false
            }
        }
    }
}