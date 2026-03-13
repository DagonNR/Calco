package com.eq6.calco.models

data class CartItem(
    val productId: String,
    val productName: String,
    val barcode: String,
    val unitPrice: Double,
    var quantity: Long
) {
    fun subtotal(): Double = unitPrice * quantity
}