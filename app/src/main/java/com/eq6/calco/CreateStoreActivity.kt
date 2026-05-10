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
            val storeName = etStoreName.text.toString().trim()

            if (storeName.isEmpty()) {
                etStoreName.error = "Ingresa el nombre de la tienda"
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

            val indexRef = db.collection("usersIndex").document(user.uid)

            indexRef.get()
                .addOnSuccessListener { indexDoc ->
                    if (!indexDoc.exists()) {
                        btn.isEnabled = true
                        btn.text = "Crear"
                        Toast.makeText(this, "Cuenta no registrada", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                        return@addOnSuccessListener
                    }

                    val role = (indexDoc.getString("role") ?: "").lowercase().trim()
                    if (role != "admin") {
                        btn.isEnabled = true
                        btn.text = "Crear"
                        Toast.makeText(this, "Solo un admin puede crear tienda", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val adminName = indexDoc.getString("name") ?: (user.email ?: "Admin")
                    val adminEmail = indexDoc.getString("email") ?: (user.email ?: "")

                    val storeRef = db.collection("stores").document()
                    val storeId = storeRef.id

                    val storeData: MutableMap<String, Any> = mutableMapOf(
                        "name" to storeName,
                        "createdAt" to Timestamp.now(),
                        "ownerId" to user.uid
                    )

                    val indexUpdate: MutableMap<String, Any> = mutableMapOf(
                        "storeId" to storeId,
                        "needsStoreSetup" to false
                    )

                    db.runBatch { batch ->
                        batch.set(storeRef, storeData)
                        batch.update(indexRef, indexUpdate)
                    }.addOnSuccessListener {

                        val storeUserRef = db.collection("stores").document(storeId)
                            .collection("users").document(user.uid)

                        val counterRef = db.collection("stores").document(storeId)
                            .collection("counters").document("sales")

                        val storeUserData: MutableMap<String, Any> = mutableMapOf(
                            "name" to adminName,
                            "email" to adminEmail,
                            "role" to "admin",
                            "createdAt" to Timestamp.now()
                        )

                        db.runBatch { batch ->
                            batch.set(storeUserRef, storeUserData)
                            batch.set(counterRef, mapOf("lastNumber" to 0L))
                        }.addOnSuccessListener {
                            Toast.makeText(this, "Tienda creada", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, RouterActivity::class.java))
                            finish()
                        }.addOnFailureListener { e ->
                            btn.isEnabled = true
                            btn.text = "Crear"
                            Toast.makeText(this, "Error creando datos de tienda: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    }.addOnFailureListener { e ->
                        btn.isEnabled = true
                        btn.text = "Crear"
                        Toast.makeText(this, "Error creando tienda: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    btn.isEnabled = true
                    btn.text = "Crear"
                    Toast.makeText(this, "Error leyendo cuenta: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}