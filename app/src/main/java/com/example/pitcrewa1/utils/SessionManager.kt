package com.example.pitcrewa1.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pitcrew_user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_REMEMBERED_EMAIL = "remembered_email"
        private const val KEY_REMEMBERED_PASSWORD = "remembered_password"
        private const val KEY_VEHICLE_MODEL = "vehicle_model"
        private const val KEY_VEHICLE_NUMBER = "vehicle_number"
        private const val KEY_VEHICLE_TYPE = "vehicle_type"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_SHOP_LAT = "shop_lat"
        private const val KEY_SHOP_LNG = "shop_lng"
        private const val KEY_SHOP_ADDRESS = "shop_address"
    }

    fun saveUserSession(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        vehicleModel: String = "",
        vehicleNumber: String = "",
        vehicleType: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            if (vehicleModel.isNotEmpty()) putString(KEY_VEHICLE_MODEL, vehicleModel)
            if (vehicleNumber.isNotEmpty()) putString(KEY_VEHICLE_NUMBER, vehicleNumber)
            if (vehicleType.isNotEmpty()) putString(KEY_VEHICLE_TYPE, vehicleType)
            apply()
        }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        vehicleModel: String,
        vehicleNumber: String,
        vehicleType: String
    ) {
        prefs.edit().apply {
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_VEHICLE_MODEL, vehicleModel)
            putString(KEY_VEHICLE_NUMBER, vehicleNumber)
            putString(KEY_VEHICLE_TYPE, vehicleType)
            apply()
        }
    }

    fun saveVehicleDetails(vehicleModel: String, vehicleNumber: String, vehicleType: String) {
        prefs.edit().apply {
            putString(KEY_VEHICLE_MODEL, vehicleModel)
            putString(KEY_VEHICLE_NUMBER, vehicleNumber)
            putString(KEY_VEHICLE_TYPE, vehicleType)
            apply()
        }
    }

    fun saveRememberCredentials(email: String, password: String, isRemembered: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, isRemembered)
            if (isRemembered) {
                putString(KEY_REMEMBERED_EMAIL, email)
                putString(KEY_REMEMBERED_PASSWORD, password)
            } else {
                remove(KEY_REMEMBERED_EMAIL)
                remove(KEY_REMEMBERED_PASSWORD)
            }
            apply()
        }
    }

    fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)
    fun getRememberedEmail(): String = prefs.getString(KEY_REMEMBERED_EMAIL, "") ?: ""
    fun getRememberedPassword(): String = prefs.getString(KEY_REMEMBERED_PASSWORD, "") ?: ""

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getFirstName(): String = prefs.getString(KEY_FIRST_NAME, "") ?: ""
    fun getLastName(): String = prefs.getString(KEY_LAST_NAME, "") ?: ""
    fun getFullName(): String = "${getFirstName()} ${getLastName()}".trim()
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""
    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getVehicleModel(): String = prefs.getString(KEY_VEHICLE_MODEL, "") ?: ""
    fun getVehicleNumber(): String = prefs.getString(KEY_VEHICLE_NUMBER, "") ?: ""
    fun getVehicleType(): String = prefs.getString(KEY_VEHICLE_TYPE, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "USER") ?: "USER"

    fun saveUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun saveMechanicSession(
        mechanicId: String,
        firstName: String,
        lastName: String,
        businessName: String,
        email: String,
        phone: String,
        latitude: Double = 6.9271,
        longitude: Double = 79.8612,
        address: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ROLE, "MECHANIC")
            putString(KEY_USER_ID, mechanicId)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_BUSINESS_NAME, businessName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_SHOP_LAT, latitude.toString())
            putString(KEY_SHOP_LNG, longitude.toString())
            putString(KEY_SHOP_ADDRESS, address)
            apply()
        }
    }

    fun getBusinessName(): String = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
    fun getShopLatitude(): Double = prefs.getString(KEY_SHOP_LAT, "6.9271")?.toDoubleOrNull() ?: 6.9271
    fun getShopLongitude(): Double = prefs.getString(KEY_SHOP_LNG, "79.8612")?.toDoubleOrNull() ?: 79.8612
    fun getShopAddress(): String = prefs.getString(KEY_SHOP_ADDRESS, "") ?: ""
    fun isMechanic(): Boolean = getUserRole() == "MECHANIC"

    fun updateMechanicProfile(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        businessName: String,
        address: String
    ) {
        prefs.edit().apply {
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_BUSINESS_NAME, businessName)
            putString(KEY_SHOP_ADDRESS, address)
            apply()
        }
    }

    fun clearSession() {
        val remember = isRememberMeEnabled()
        val remEmail = getRememberedEmail()
        val remPass = getRememberedPassword()
        prefs.edit().clear().apply()
        if (remember) {
            saveRememberCredentials(remEmail, remPass, true)
        }
    }
}
