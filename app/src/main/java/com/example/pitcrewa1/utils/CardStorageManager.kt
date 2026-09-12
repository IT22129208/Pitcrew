package com.example.pitcrewa1.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.pitcrewa1.data.CardDetail
import org.json.JSONArray

class CardStorageManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pitcrew_cards_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CARDS_JSON = "saved_cards_json"
    }

    fun getCards(): MutableList<CardDetail> {
        val jsonStr = prefs.getString(KEY_CARDS_JSON, null) ?: return getDefaultCards()
        val list = mutableListOf<CardDetail>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(CardDetail.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return getDefaultCards()
        }
        return if (list.isNotEmpty()) list else getDefaultCards()
    }

    fun saveCards(cards: List<CardDetail>) {
        try {
            val jsonArray = JSONArray()
            for (card in cards) {
                jsonArray.put(card.toJsonObject())
            }
            prefs.edit().putString(KEY_CARDS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateCard(card: CardDetail) {
        val current = getCards()
        val index = current.indexOfFirst { it.cardId == card.cardId }
        if (index != -1) {
            current[index] = card
        } else {
            current.add(card)
        }
        saveCards(current)
    }

    fun deleteCard(cardId: String) {
        val current = getCards()
        current.removeAll { it.cardId == cardId }
        saveCards(current)
    }

    private fun getDefaultCards(): MutableList<CardDetail> {
        return mutableListOf(
            CardDetail(
                cardId = "card_sample_1",
                userId = "sample",
                cardNumber = "1024 5588 9912 3499",
                maskedNumber = "10xx xxxx xxxx xx99",
                cardHolderName = "Silva HSN",
                expiryDate = "29/09",
                cardType = "VISA"
            ),
            CardDetail(
                cardId = "card_sample_2",
                userId = "sample",
                cardNumber = "1024 5588 9912 3499",
                maskedNumber = "10xx xxxx xxxx xx99",
                cardHolderName = "Silva HSN",
                expiryDate = "29/09",
                cardType = "VISA"
            )
        )
    }
}
