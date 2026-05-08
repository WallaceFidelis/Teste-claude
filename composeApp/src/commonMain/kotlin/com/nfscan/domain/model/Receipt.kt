package com.nfscan.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Receipt(
    val id: Long = 0,
    val supermarket: String,
    val date: String,
    val items: List<ReceiptItem>,
    val total: Double,
    val rawText: String,
)

@Serializable
data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
)
