package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.eq6.calco.adapters.UsersAdapter
import com.eq6.calco.models.UserItem

class UsersActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var spFilter: Spinner
    private lateinit var btnApply: Button
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: UsersAdapter
    private lateinit var ivFilter: ImageView
    private lateinit var filterPanel: LinearLayout
    private var allUsers: List<UserItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        spFilter = findViewById(R.id.spFilterRole)
        btnApply = findViewById(R.id.btnApply)
        rv = findViewById(R.id.rvUsers)
        tvEmpty = findViewById(R.id.tvEmpty)
        ivFilter = findViewById(R.id.ivFilter)
        filterPanel = findViewById(R.id.filterPanel)

        ivFilter.setOnClickListener {
            filterPanel.visibility = if (filterPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val spAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.user_type_filter_array,
            android.R.layout.simple_spinner_item
        )
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFilter.adapter = spAdapter

        adapter = UsersAdapter(emptyList()) { user ->
            val i = Intent(this, UserDetailActivity::class.java)
            i.putExtra("uid", user.uid)
            startActivity(i)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnApply.setOnClickListener {
            applyFilter()
            filterPanel.visibility = View.GONE
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        bottom.selectedItemId = R.id.nav_users
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_report -> {
                    true
                }
                else -> false
            }
        }

        findViewById<FloatingActionButton>(R.id.fabAdmin).setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users").get()
            .addOnSuccessListener { snap ->
                allUsers = snap.documents.map { doc ->
                    UserItem(
                        uid = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        role = (doc.getString("role") ?: "").lowercase().trim()
                    )
                }.sortedBy { it.name.lowercase() }

                applyFilter()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando usuarios: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun applyFilter() {
        val selected = spFilter.selectedItem.toString()

        val roleFilter = when (selected.lowercase()) {
            "admin" -> "admin"
            "vendedor" -> "seller"
            "cliente" -> "client"
            else -> ""
        }

        val filtered = if (roleFilter.isEmpty()) allUsers else allUsers.filter { it.role == roleFilter }

        adapter.update(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }
}