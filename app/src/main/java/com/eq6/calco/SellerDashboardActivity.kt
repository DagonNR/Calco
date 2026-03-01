package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SellerDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_dashboard)

        val name = intent.getStringExtra("name") ?: "Vendedor"
        findViewById<TextView>(R.id.tvHello).text = "Hola,\n$name\n(Vendedor)"

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavSeller)
        bottom.selectedItemId = R.id.nav_home

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> {
                    true
                }
                R.id.nav_home -> true
                R.id.nav_profile -> {
                    true
                }
                else -> false
            }
        }

        findViewById<FloatingActionButton>(R.id.fabSeller).setOnClickListener {
            startActivity(Intent(this, RegisterSaleActivity::class.java))
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRegister).setOnClickListener {
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHistory).setOnClickListener {
        }
    }
}