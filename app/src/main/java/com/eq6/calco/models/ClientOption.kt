package com.eq6.calco.models

data class ClientOption(val uid: String, val name: String) {
    override fun toString(): String = name
}
