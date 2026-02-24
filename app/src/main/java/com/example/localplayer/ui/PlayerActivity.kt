package com.example.localplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import com.example.localplayer.databinding.ActivityPlayerBinding
import com.example.localplayer.playback.ControllerProvider
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var browser: MediaBrowser? = null

    private var progressJob: Job? = null
    private var userSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playerToolbar.setNavigationOnClickListener { finish() }
        binding.playerToolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)

        binding.btnPlayPauseLarge.setOnClickListener {
            val b = browser ?: return@setOnClickListener
            if (b.isPlaying) b.pause() else b.play()
        }
        binding.btnNextLarge.setOnClickListener { browser?.seekToNext() }
        binding.btnPrevLarge.setOnClickListener { browser?.seekToPrevious() }

        binding.btnShuffle.setOnClickListener {
            val b = browser ?: return@setOnClickListener
            b.shuffleModeEnabled = !b.shuffleModeEnabled
            updateButtons(b)
        }

        binding.btnRepeat.setOnClickListener {
            val b = browser ?: return@setOnClickListener
            b.repeatMode = when (b.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            updateButtons(b)
        }

        binding.sliderSeek.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            userSeeking = true
            binding.tvPos.text = formatMs(value.toLong())
        }
        binding.sliderSeek.addOnSliderTouchListener(object :
            Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(p0: Slider) {
                userSeeking = true
            }

            override fun onStopTrackingTouch(p0: Slider) {
                val b = browser ?: return
                val seekPos = p0.value.toLong()
                b.seekTo(seekPos)
                userSeeking = false
            }
        })

        connectBrowser()
    }

    private fun connectBrowser() {
        ControllerProvider.buildBrowserAsync(
            this,
            { b ->
                browser = b

                b.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateAll(b)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateAll(b)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updateAll(b)
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        updateButtons(b)
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        updateButtons(b)
                    }
                })

                updateAll(b)
                startProgressLoop()
            },
            { t ->
                binding.tvPlayerStatus.text = "Controller error: ${t.message}"
            }
        )
    }

    private fun updateAll(b: MediaBrowser) {
        val md = b.currentMediaItem?.mediaMetadata
        binding.tvTitleLarge.text = md?.title?.toString() ?: "Not playing"
        binding.tvSubLarge.text = listOfNotNull(
            md?.artist?.toString(),
            md?.albumTitle?.toString()
        ).joinToString(" • ").ifEmpty { " " }

        binding.btnPlayPauseLarge.setImageResource(
            if (b.isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )

        binding.tvPlayerStatus.text = when {
            b.currentMediaItem == null -> "Ready"
            b.isPlaying -> "Playing"
            else -> "Paused"
        }

        val dur = max(0L, b.duration)
        binding.sliderSeek.valueFrom = 0f
        binding.sliderSeek.valueTo = max(1f, dur.toFloat())
        binding.tvDur.text = formatMs(dur)

        binding.btnPrevLarge.isEnabled = b.hasPreviousMediaItem()
        binding.btnNextLarge.isEnabled = b.hasNextMediaItem()

        updateButtons(b)

        if (!userSeeking) {
            val pos = max(0L, b.currentPosition)
            binding.sliderSeek.value = pos.toFloat().coerceIn(
                binding.sliderSeek.valueFrom, binding.sliderSeek.valueTo
            )
            binding.tvPos.text = formatMs(pos)
        }
    }

    private fun updateButtons(b: MediaBrowser) {
        binding.btnShuffle.alpha = if (b.shuffleModeEnabled) 1.0f else 0.4f
        binding.btnRepeat.alpha = if (b.repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1.0f

        binding.btnRepeat.setImageResource(
            when (b.repeatMode) {
                Player.REPEAT_MODE_ONE -> android.R.drawable.ic_menu_recent_history
                Player.REPEAT_MODE_ALL -> android.R.drawable.ic_menu_rotate
                else -> android.R.drawable.ic_menu_revert
            }
        )
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                val b = browser
                if (b != null && !userSeeking) {
                    val dur = max(0L, b.duration)
                    val pos = max(0L, b.currentPosition)

                    if (dur > 0) {
                        binding.sliderSeek.valueTo = max(1f, dur.toFloat())
                        binding.sliderSeek.value = pos.toFloat().coerceIn(
                            binding.sliderSeek.valueFrom, binding.sliderSeek.valueTo
                        )
                    }
                    binding.tvPos.text = formatMs(pos)
                    binding.tvDur.text = formatMs(dur)
                }
                delay(500)
            }
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    override fun onDestroy() {
        progressJob?.cancel()
        browser?.release()
        super.onDestroy()
    }
}