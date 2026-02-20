package com.example.localplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localplayer.data.LocalMusicRepository
import com.example.localplayer.databinding.ActivityMainBinding
import com.example.localplayer.playback.ControllerProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var controller: MediaController? = null

    private val repo by lazy { LocalMusicRepository(this) }

    private val adapter by lazy {
        SongAdapter { song ->
            val c = controller ?: return@SongAdapter
            val item = MediaItem.Builder()
                .setUri(song.contentUri)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()

            c.setMediaItem(item)
            c.prepare()
            c.play()
            binding.tvStatus.text = "Playing: ${song.title}"
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

        requestMediaPermissionIfNeeded()
    }

    private fun requestMediaPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            initAfterPermission()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun initAfterPermission() {
        val songs = repo.loadSongs()
        adapter.submit(songs)
        binding.tvStatus.text = "Loaded ${songs.size} songs"

        ControllerProvider.buildControllerAsync(
            this,
            { c ->
                controller = c
                binding.tvStatus.text = "Ready"
            },
            { t -> binding.tvStatus.text = "Controller error: ${t.message}" }
        )
    }

    override fun onDestroy() {
        controller?.release()
        super.onDestroy()
    }
}