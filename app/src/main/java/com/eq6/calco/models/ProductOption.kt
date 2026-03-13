package com.eq6.calco.models

data class ProductOption(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Double,
    val stock: Long,
    val active: Boolean
) {
    override fun toString(): String = name
}