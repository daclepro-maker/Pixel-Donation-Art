package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey val email: String,
    val passwordHash: String, // Stored encrypted/hashed or simple string for demo security
    val displayName: String,
    val dateRegistered: Long = System.currentTimeMillis()
)

@Entity(tableName = "pixel_purchases")
data class PixelPurchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val userName: String,
    val startX: Int,
    val startY: Int,
    val width: Int,
    val height: Int,
    val pixelCount: Int,
    val amountUsd: Double,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentBankSnippet: String = "", // Simulated bank origin identifier
    val paymentPaypalReceipt: String = "", // Simulated paypal transaction reference ID
    val colorHex: String = "#D0BCFF" // Hex representation of the custom colored pixels
)
