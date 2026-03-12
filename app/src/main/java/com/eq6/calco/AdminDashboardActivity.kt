package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val name = intent.getStringExtra("name") ?: "Admin"

        findViewById<TextView>(R.id.tvHello).text = "Hola,\n$name\n(Admin)"

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        bottom.selectedItemId = R.id.nav_home

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    startActivity(Intent(this, UsersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> true
                R.id.nav_report -> {
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.fabAdmin)
            .setOnClickListener {
                startActivity(Intent(this, CreateUserActivity::class.java))
            }

    }
}
