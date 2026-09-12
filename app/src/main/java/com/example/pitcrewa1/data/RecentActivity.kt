package com.example.pitcrewa1.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class RecentActivity(
    val id: String = "",
    val requestId: String = "",
    val date: String = "",
    val location: String = "",
    val cause: String = "",
    val amount: String = "",
    val provider: String = "",
    val status: String = "SEARCHING", // SEARCHING, ACCEPTED, CANCELLED, COMPLETED
    val timestamp: Long = System.currentTimeMillis()
)
