package com.eq6.calco.models

import com.google.firebase.Timestamp

data class SaleItem(
    val id: String = "",
    val saleNumber: String = "",
    val amount: Double = 0.0,
    val monthKey: String = "",
    val date: Timestamp? = null,
    val clientId: String = "",
    val clientName: String = ""
)