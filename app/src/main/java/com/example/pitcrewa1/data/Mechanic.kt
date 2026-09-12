package com.example.pitcrewa1.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Mechanic(
    val mechanicId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val phone: String = "",
    val businessName: String = "",
    val email: String = "",
    val password: String = "",
    val latitude: Double = 6.9271,
    val longitude: Double = 79.8612,
    val address: String = "",
    val isOnline: Boolean = true,
    val isVerified: Boolean = true,
    val rating: Double = 5.0,
    val totalJobsCompleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
