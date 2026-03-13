package com.eq6.calco.models

data class SaleProductItem(
    val productName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 0L,
    val subtotal: Double = 0.0
)