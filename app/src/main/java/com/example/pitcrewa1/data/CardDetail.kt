package com.example.pitcrewa1.data

import com.google.firebase.database.IgnoreExtraProperties
import org.json.JSONObject

@IgnoreExtraProperties
data class CardDetail(
    var cardId: String = "",
    var userId: String = "",
    var cardNumber: String = "",
    var maskedNumber: String = "",
    var cardHolderName: String = "",
    var expiryDate: String = "",
    var cardType: String = "VISA",
    var createdAt: Long = System.currentTimeMillis()
) {
    // Zero-argument constructor required by Firebase
    constructor() : this("", "", "", "", "", "", "VISA", 0L)

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("cardId", cardId)
            put("userId", userId)
            put("cardNumber", cardNumber)
            put("maskedNumber", maskedNumber)
            put("cardHolderName", cardHolderName)
            put("expiryDate", expiryDate)
            put("cardType", cardType)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): CardDetail {
            return CardDetail(
                cardId = json.optString("cardId", ""),
                userId = json.optString("userId", ""),
                cardNumber = json.optString("cardNumber", ""),
                maskedNumber = json.optString("maskedNumber", ""),
                cardHolderName = json.optString("cardHolderName", ""),
                expiryDate = json.optString("expiryDate", ""),
                cardType = json.optString("cardType", "VISA"),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
