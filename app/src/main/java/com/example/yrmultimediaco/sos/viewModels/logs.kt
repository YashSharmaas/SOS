package com.example.yrmultimediaco.sos.viewModels

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yrmultimediaco.sos.R

class LogsAdapter : RecyclerView.Adapter<LogsAdapter.VH>() {

    private var data: List<String> = emptyList()

    fun submit(list: List<String>) {
        data = list
        notifyDataSetChanged()
    }

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false) as TextView
        return VH(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = data[position]
    }
}
