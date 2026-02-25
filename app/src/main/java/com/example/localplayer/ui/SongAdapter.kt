package com.example.localplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.localplayer.R
import com.example.localplayer.data.Song
import com.example.localplayer.databinding.ItemSongBinding

class SongAdapter(
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.VH>() {

    private val allItems = mutableListOf<Song>()
    private val items = mutableListOf<Song>()

    fun submit(list: List<Song>) {
        allItems.clear()
        allItems.addAll(list)

        items.clear()
        items.addAll(list)

        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        items.clear()
        if (q.isEmpty()) {
            items.addAll(allItems)
        } else {
            items.addAll(
                allItems.filter {
                    it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
                }
            )
        }
        notifyDataSetChanged()
    }

    fun currentSize(): Int = items.size

    class VH(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.binding.tvTitle.text = s.title
        holder.binding.tvSub.text = "${s.artist} - ${s.album}"

        holder.binding.ivArt.load(AlbumArt.uri(s.albumId)) {
            crossfade(true)
            placeholder(R.mipmap.ic_launcher)
            error(R.mipmap.ic_launcher)
        }

        holder.binding.root.setOnClickListener { onClick(s) }
    }
}