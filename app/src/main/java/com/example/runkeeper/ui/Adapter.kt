package com.example.runkeeper.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.runkeeper.R
import com.example.runkeeper.TrackingUtility
import com.example.runkeeper.database.Run
import kotlinx.android.synthetic.main.item_list.view.*
import java.lang.Math.round
import java.text.SimpleDateFormat
import java.util.*

class Adapter : RecyclerView.Adapter<Adapter.AdapterViewHolder>() {

    inner class AdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    val differCallback = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    fun submitList(list: List<Run>) =
        differ.submitList(list) // add's list of runs in RV and updates

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterViewHolder {
        return AdapterViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_list,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: AdapterViewHolder, position: Int) {
        val run = differ.currentList[position]
        holder.itemView.apply {
            Glide
                .with(this)
                .load(run.image)
                .into(map_screen_iv_item)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = run.timestamp
            }
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            date_tv_item.text = dateFormat.format(calendar.time)

            val speed = "${run.speed} km/h"
            speed_tv_item.text = speed

            val distance = "${round((run.distance / 1000f) * 10) / 10f} km"
            distance_tv_item.text = distance

            time_tv_item.text = TrackingUtility.getFormattedTime(run.time)

        }
    }
}