package com.example.localplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.localplayer.R
import com.example.localplayer.data.QueueRow
import com.example.localplayer.databinding.ItemQueueBinding

class QueueAdapter : RecyclerView.Adapter<QueueAdapter.VH>() {

    private val items = mutableListOf<QueueRow>()
    private var currentIndex: Int = -1

    var onItemClick: ((Int) -> Unit)? = null
    var onRemoveClick: ((Int) -> Unit)? = null

    fun setItems(newItems: List<QueueRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setCurrentIndex(idx: Int) {
        val old = currentIndex
        currentIndex = idx
        if (old != -1) notifyItemChanged(old)
        if (currentIndex != -1) notifyItemChanged(currentIndex)
    }

    fun moveItem(from: Int, to: Int) {
        if (from !in  items.indices || to !in items.indices) return
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)

        if (currentIndex == from) currentIndex = to
        else if (from < currentIndex && to >= currentIndex) currentIndex -= 1
        else if (from > currentIndex && to <= currentIndex) currentIndex += 1
    }

    class VH(val binding: ItemQueueBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val b = holder.binding
        b.tvQueueTitle.text = row.title
        b.tvQueueSub.text = row.sub
        if (row.albumId > 0) {
            b.ivQueueArt.load(AlbumArt.uri(row.albumId)) {
                placeholder(R.mipmap.ic_launcher)
                error(R.mipmap.ic_launcher)
                crossfade(true)
            }
        } else {
            b.ivQueueArt.setImageResource(R.mipmap.ic_launcher)
        }

        val isCurrent = position == currentIndex
        b.root.isSelected = isCurrent
        b.root.alpha = if (isCurrent) 1.0f else 0.85f

        b.root.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) onItemClick?.invoke(pos)
        }
        b.btnQueueRemove.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) onRemoveClick?.invoke(pos)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}