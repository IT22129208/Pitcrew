package com.example.pitcrewa1.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val password: String = "",
    val vehicleModel: String = "",
    val vehicleNumber: String = "",
    val vehicleType: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFullName(): String = "$firstName $lastName".trim()
}
