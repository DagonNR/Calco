package com.eq6.calco

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    
    private val TAG = "FirebaseTest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        testFirebaseConnection()
    }

    private fun testFirebaseConnection() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        Log.d(TAG, "Intentando conectar a Firebase...")

        if (auth.app != null) {
            Log.d(TAG, "Firebase Auth: Conectado correctamente")
        }

        val testData = hashMapOf("status" to "connected", "timestamp" to System.currentTimeMillis())
        
        db.collection("connection_test").add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Firestore: ¡Éxito! Documento escrito con ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore: Error al conectar o escribir", e)
                Log.e(TAG, "Asegúrate de haber configurado las Reglas de Seguridad en Firebase Console.")
            }
    }
}
