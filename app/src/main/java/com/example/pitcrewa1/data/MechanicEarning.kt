package com.example.pitcrewa1.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MechanicEarning(
    val id: String = "",
    val requestId: String = "",
    val date: String = "",              // e.g. "12 Sep 2026" or "July 29, 2026"
    val timestamp: Long = System.currentTimeMillis(),
    val amount: String = "LKR 6,580.00",
    val numericAmount: Double = 6580.0,
    val location: String = "Near Katunayake Exit",
    val cause: String = "Low fuel · Vehicle Import",
    val customerOrStation: String = "IOC Gas Station",
    val mechanicId: String = ""
)
