package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView

class ClientDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        val name = intent.getStringExtra("name") ?: "Cliente"
        findViewById<TextView>(R.id.tvHello).text = "Hola,\n$name\n(Cliente)"

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavClient)
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
    }
}