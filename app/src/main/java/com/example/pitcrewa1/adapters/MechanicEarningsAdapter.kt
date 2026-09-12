package com.example.pitcrewa1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pitcrewa1.R
import com.example.pitcrewa1.data.MechanicEarning

class MechanicEarningsAdapter(
    private var items: List<MechanicEarning> = emptyList(),
    private val onItemClick: ((MechanicEarning) -> Unit)? = null
) : RecyclerView.Adapter<MechanicEarningsAdapter.EarningViewHolder>() {

    class EarningViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvEarningItemDate)
        val tvLocation: TextView = itemView.findViewById(R.id.tvEarningLocation)
        val tvCause: TextView = itemView.findViewById(R.id.tvEarningCause)
        val tvAmount: TextView = itemView.findViewById(R.id.tvEarningAmount)
        val tvCustomerOrStation: TextView = itemView.findViewById(R.id.tvEarningCustomerOrStation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EarningViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mechanic_earning, parent, false)
        return EarningViewHolder(view)
    }

    override fun onBindViewHolder(holder: EarningViewHolder, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.date
        holder.tvLocation.text = item.location
        holder.tvCause.text = item.cause
        holder.tvAmount.text = item.amount
        holder.tvCustomerOrStation.text = item.customerOrStation

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MechanicEarning>) {
        items = newItems
        notifyDataSetChanged()
    }
}
