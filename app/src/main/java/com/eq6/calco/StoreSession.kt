package com.eq6.calco

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object StoreSession {

    fun getStoreId(
        onOk: (String) -> Unit,
        onFail: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onFail("Sesión no válida")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("usersIndex")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val storeId = (doc.getString("storeId") ?: "").trim()
                if (storeId.isBlank()) onFail("No hay tienda asignada")
                else onOk(storeId)
            }
            .addOnFailureListener { e ->
                onFail(e.message ?: "Error leyendo tienda")
            }
    }
}