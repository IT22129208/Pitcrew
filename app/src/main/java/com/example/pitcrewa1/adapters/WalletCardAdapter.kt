package com.example.pitcrewa1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pitcrewa1.R
import com.example.pitcrewa1.data.CardDetail

class WalletCardAdapter(
    private var cards: List<CardDetail> = emptyList(),
    private val onCardClick: ((CardDetail) -> Unit)? = null
) : RecyclerView.Adapter<WalletCardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMaskedNumber: TextView = itemView.findViewById(R.id.tvCardMaskedNumber)
        val tvHolderName: TextView = itemView.findViewById(R.id.tvCardHolderName)
        val tvExpiry: TextView = itemView.findViewById(R.id.tvCardExpiry)
        val tvBadge: TextView = itemView.findViewById(R.id.tvCardTypeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        holder.tvMaskedNumber.text = card.maskedNumber
        holder.tvHolderName.text = card.cardHolderName
        holder.tvExpiry.text = card.expiryDate
        holder.tvBadge.text = card.cardType.uppercase()

        holder.itemView.setOnClickListener {
            onCardClick?.invoke(card)
        }
    }

    override fun getItemCount(): Int = cards.size

    fun updateData(newCards: List<CardDetail>) {
        cards = newCards
        notifyDataSetChanged()
    }
}
