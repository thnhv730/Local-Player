package com.example.localplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaBrowser
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.localplayer.data.QueueRow
import com.example.localplayer.databinding.ActivityQueueBinding
import com.example.localplayer.playback.ControllerProvider
import kotlin.math.max

class QueueActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQueueBinding
    private var browser: MediaBrowser? = null
    private val adapter by lazy { QueueAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.queueToolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        binding.queueToolbar.setNavigationOnClickListener { finish() }

        binding.rvQueue.layoutManager = LinearLayoutManager(this)
        binding.rvQueue.adapter = adapter

        attachTouchHelper()

        connectBrowser()
    }

    private fun connectBrowser() {
        ControllerProvider.buildBrowserAsync(
            this,
            { b ->
                browser = b

                adapter.onItemClick = { position ->
                    b.seekTo(position, 0L)
                    b.play()
                }

                adapter.onRemoveClick = { position ->
                    if (position in 0 until b.mediaItemCount) {
                        b.removeMediaItem(position)
                    }
                }

                b.addListener(object : Player.Listener {
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        updateQueue(b)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrent(b)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updateCurrent(b)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateCurrent(b)
                    }
                })

                updateQueue(b)
            },
            { t ->
                binding.tvQueueStatus.text = "Controller error: ${t.message}"
                binding.tvQueueStatus.isVisible = true
            }
        )
    }

    private fun updateQueue(b: MediaBrowser) {
        val items = buildQueueItems(b)
        adapter.setItems(items)
        adapter.setCurrentIndex(safeIndex(b.currentMediaItemIndex, items.size))

        binding.tvEmpty.isVisible = items.isEmpty()
        binding.tvQueueStatus.isVisible = items.isNotEmpty()
        binding.tvQueueStatus.text = if (items.isEmpty()) "Empty queue" else "Queue: ${items.size} songs"
    }

    private fun updateCurrent(b: MediaBrowser) {
        adapter.setCurrentIndex(safeIndex(b.currentMediaItemIndex, adapter.itemCount))
    }

    private fun buildQueueItems(b: MediaBrowser): MutableList<QueueRow> {
        val list = mutableListOf<QueueRow>()
        val count = max(0, b.mediaItemCount)
        for (i in 0 until count) {
            val mi = b.getMediaItemAt(i)
            val md = mi.mediaMetadata
            val albumId = md.extras?.getLong("albumId", -1L) ?: -1L

            val title = md.title?.toString()?.takeIf { it.isNotBlank() } ?: "(Unknown title)"
            val sub = listOfNotNull(
                md.artist?.toString()?.takeIf { it.isNotBlank() },
                md.albumTitle?.toString()?.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifEmpty { " " }

            list.add(
                QueueRow(
                    mediaId = mi.mediaId,
                    title = title,
                    sub = sub,
                    albumId = albumId
                )
            )
        }
        return list
    }

    private fun attachTouchHelper() {
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val b = browser ?: return false
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                if (from !in 0 until b.mediaItemCount || to !in 0 until b.mediaItemCount) return false

                adapter.moveItem(from, to)

                b.moveMediaItem(from, to)

                adapter.setCurrentIndex(safeIndex(b.currentMediaItemIndex, adapter.itemCount))
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val b = browser ?: return
                val pos = viewHolder.adapterPosition
                if (pos in 0 until b.mediaItemCount) {
                    b.removeMediaItem(pos)
                } else {
                    adapter.notifyItemChanged(pos)
                }
            }
        })

        touchHelper.attachToRecyclerView(binding.rvQueue)
    }

    private fun safeIndex(idx: Int, size: Int): Int {
        if (size <= 0) return -1
        return idx.coerceIn(0, size - 1)
    }

    override fun onDestroy() {
        browser?.release()
        super.onDestroy()
    }
}