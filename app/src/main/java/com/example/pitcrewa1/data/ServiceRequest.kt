package com.example.pitcrewa1.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ServiceRequest(
    val requestId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val vehicleModel: String = "",
    val vehicleNumber: String = "",
    val vehicleType: String = "",
    val cause: String = "",
    val description: String = "",
    val paymentMethod: String = "",         // "CARD" or "CASH"
    val paymentCardMasked: String = "",     // e.g. "xxxx xxxx xxxx x332"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val status: String = "SEARCHING",       // "SEARCHING", "ACCEPTED", "EN_ROUTE", "ARRIVED", "COMPLETED", "CANCELLED"
    val mechanicId: String = "",            // Set by merchant/mechanic app
    val mechanicName: String = "",
    val mechanicPhone: String = "",
    val mechanicVehicle: String = "",
    val mechanicLat: Double = 0.0,
    val mechanicLng: Double = 0.0,
    val etaMinutes: Int = 0,
    val estimatedAmount: String = "LKR 6,580.00",
    val originalAmount: String = "LKR 6,580.00",
    val isCounterOffer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
