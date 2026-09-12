package com.example.pitcrewa1.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pitcrewa1.R
import com.example.pitcrewa1.data.RecentActivity

class RecentActivityAdapter(
    private var items: List<RecentActivity> = emptyList(),
    private val onItemClick: ((RecentActivity) -> Unit)? = null
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvActivityDate)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvActivityStatusBadge)
        val ivStatusDot: ImageView = itemView.findViewById(R.id.ivActivityStatusDot)
        val tvLocation: TextView = itemView.findViewById(R.id.tvActivityLocation)
        val tvCause: TextView = itemView.findViewById(R.id.tvActivityCause)
        val tvAmount: TextView = itemView.findViewById(R.id.tvActivityAmount)
        val tvProvider: TextView = itemView.findViewById(R.id.tvActivityProvider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.date
        holder.tvLocation.text = item.location
        holder.tvCause.text = item.cause
        holder.tvAmount.text = item.amount
        holder.tvProvider.text = item.provider

        // Bind Status Badge and Dot Color
        val rawStatus = item.status.trim().uppercase()
        when {
            rawStatus.contains("CANCEL") -> {
                holder.tvStatusBadge.visibility = View.VISIBLE
                val isCancelledByUser = rawStatus.contains("USER") || item.provider.contains("User", ignoreCase = true)
                holder.tvStatusBadge.text = if (isCancelledByUser) "CANCELLED BY USER" else "CANCELLED"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_rose)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#C5221F"))
                holder.ivStatusDot.setColorFilter(Color.parseColor("#EA4335"))
            }
            rawStatus.contains("ACCEPT") || rawStatus.contains("ROUTE") || rawStatus.contains("PROGRESS") -> {
                holder.tvStatusBadge.visibility = View.VISIBLE
                holder.tvStatusBadge.text = "ACCEPTED"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_green)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#137333"))
                holder.ivStatusDot.setColorFilter(Color.parseColor("#34A853"))
            }
            rawStatus.contains("SEARCH") || rawStatus.contains("DISPATCH") || rawStatus.contains("PEND") -> {
                holder.tvStatusBadge.visibility = View.VISIBLE
                holder.tvStatusBadge.text = "SEARCHING"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_amber)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#B06000"))
                holder.ivStatusDot.setColorFilter(Color.parseColor("#FBBC04"))
            }
            rawStatus.contains("COMPLETE") || rawStatus.contains("DONE") -> {
                holder.tvStatusBadge.visibility = View.VISIBLE
                holder.tvStatusBadge.text = "COMPLETED"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_blue)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#1A73E8"))
                holder.ivStatusDot.setColorFilter(Color.parseColor("#1A73E8"))
            }
            else -> {
                // Fallback default
                holder.tvStatusBadge.visibility = View.VISIBLE
                holder.tvStatusBadge.text = if (rawStatus.isNotEmpty()) rawStatus else "COMPLETED"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_green)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#137333"))
                holder.ivStatusDot.setColorFilter(Color.parseColor("#34A853"))
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<RecentActivity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
