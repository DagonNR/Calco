package com.eq6.calco.models

data class ProductItem(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val price: Double = 0.0,
    val stock: Long = 0L,
    val active: Boolean = true
)