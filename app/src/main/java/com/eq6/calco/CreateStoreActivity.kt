package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateStoreActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_store)

        val etStoreName = findViewById<EditText>(R.id.etStoreName)
        val btn = findViewById<Button>(R.id.btnCreateStore)

        btn.setOnClickListener {
            val name = etStoreName.text.toString().trim()
            if (name.isEmpty()) {
                etStoreName.error = "Ingresa el nombre"
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            btn.isEnabled = false
            btn.text = "Creando..."

            val storeRef = db.collection("stores").document()
            val storeId = storeRef.id

            val storeData: MutableMap<String, Any> = mutableMapOf(
                "name" to name,
                "createdAt" to Timestamp.now(),
                "ownerId" to user.uid
            )

            db.runBatch { batch ->
                batch.set(storeRef, storeData)

                val indexRef = db.collection("usersIndex").document(user.uid)
                val indexData: MutableMap<String, Any> = mutableMapOf(
                    "storeId" to storeId,
                    "role" to "admin",
                    "name" to (user.displayName ?: "Admin"),
                    "email" to (user.email ?: "")
                )
                batch.set(indexRef, indexData)

                val storeUserRef = db.collection("stores").document(storeId)
                    .collection("users").document(user.uid)

                val storeUserData: MutableMap<String, Any> = mutableMapOf(
                    "name" to (user.displayName ?: "Admin"),
                    "email" to (user.email ?: ""),
                    "role" to "admin",
                    "createdAt" to Timestamp.now()
                )
                batch.set(storeUserRef, storeUserData)

                val counterRef = db.collection("stores").document(storeId)
                    .collection("counters").document("sales")
                batch.set(counterRef, mapOf("lastNumber" to 0L))
            }.addOnSuccessListener {
                Toast.makeText(this, "Tienda creada", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, RouterActivity::class.java))
                finish()
            }.addOnFailureListener { e ->
                btn.isEnabled = true
                btn.text = "Crear"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}