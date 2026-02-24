package com.example.localplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localplayer.data.LocalMusicRepository
import com.example.localplayer.data.Song
import com.example.localplayer.databinding.ActivityMainBinding
import com.example.localplayer.playback.ControllerProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var browser: MediaBrowser? = null

    private val repo by lazy { LocalMusicRepository(this) }

    private var cachedSongs: List<Song> = emptyList()

    private val adapter by lazy {
        SongAdapter { song ->
            playAt(song)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) initAfterPermission() else binding.tvStatus.text = "Permission denied"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvSongs.layoutManager = LinearLayoutManager(this)
        binding.rvSongs.adapter = adapter

        binding.rvSongs.isFocusable = true
        binding.rvSongs.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS

        binding.etSearch.addTextChangedListener { text ->
            adapter.filter(text?.toString().orEmpty())
            binding.tvEmpty.visibility =
                if (adapter.currentSize() == 0) View.VISIBLE else View.GONE
        }

        binding.btnPlayPause.setOnClickListener {
            val b = browser ?: return@setOnClickListener
            if (b.isPlaying) b.pause() else b.play()
        }
        binding.btnNext.setOnClickListener { browser?.seekToNext() }
        binding.btnPrev.setOnClickListener { browser?.seekToPrevious() }

        binding.nowPlayingBar.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        requestMediaPermissionIfNeeded()
    }

    private fun requestMediaPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        val granted =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

        if (granted) initAfterPermission() else permissionLauncher.launch(permission)
    }

    private fun initAfterPermission() {
        cachedSongs = repo.loadSongs()
        adapter.submit(cachedSongs)

        binding.tvEmpty.visibility = if (cachedSongs.isEmpty()) View.VISIBLE else View.GONE
        binding.tvStatus.text = if (cachedSongs.isEmpty()) {
            "No songs in MediaStore"
        } else {
            "Loaded ${cachedSongs.size} songs"
        }

        ControllerProvider.buildBrowserAsync(
            this,
            { b ->
                browser = b

                b.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateNowPlaying(b)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateNowPlaying(b)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updateNowPlaying(b)
                    }
                })

                updateNowPlaying(b)

                binding.tvStatus.text = if (cachedSongs.isEmpty()) {
                    "Ready (no songs)"
                } else {
                    "Ready"
                }
            },
            { t ->
                binding.tvStatus.text = "Controller error: ${t.message}"
            }
        )
    }

    private fun playAt(song: Song) {
        val b = browser ?: run {
            binding.tvStatus.text = "Controller not ready yet..."
            return
        }
        if (cachedSongs.isEmpty()) return

        val mediaItems = cachedSongs.map {
            MediaItem.Builder()
                .setUri(it.contentUri)
                .setMediaId(it.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(it.title)
                        .setArtist(it.artist)
                        .setAlbumTitle(it.album)
                        .build()
                )
                .build()
        }

        val startIndex = cachedSongs.indexOfFirst { it.id == song.id }.let { idx ->
            if (idx >= 0) idx else 0
        }

        b.setMediaItems(mediaItems, startIndex, 0L)
        b.prepare()
        b.play()

        updateNowPlaying(b)
    }

    private fun updateNowPlaying(b: MediaBrowser) {
        val md = b.currentMediaItem?.mediaMetadata
        val title = md?.title?.toString() ?: "Not playing"
        val sub = listOfNotNull(
            md?.artist?.toString(),
            md?.albumTitle?.toString()
        ).joinToString(" • ").ifEmpty { " " }

        binding.tvMiniTitle.text = title
        binding.tvMiniSub.text = sub

        binding.btnPlayPause.setImageResource(
            if (b.isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )

        binding.tvStatus.text = when {
            b.currentMediaItem == null -> "Ready"
            b.isPlaying -> "Playing"
            else -> "Paused"
        }

        binding.btnPrev.isEnabled = b.hasPreviousMediaItem()
        binding.btnNext.isEnabled = b.hasNextMediaItem()
    }

    override fun onDestroy() {
        browser?.release()
        super.onDestroy()
    }
}