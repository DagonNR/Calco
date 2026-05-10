package com.eq6.calco.models

import com.google.firebase.Timestamp

data class AdminSaleItem(
    val id: String,
    val saleNumber: String,
    val clientName: String,
    val sellerName: String,
    val amount: Double,
    val monthKey: String,
    val date: Timestamp?
)