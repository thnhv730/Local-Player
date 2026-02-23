package com.example.localplayer.playback

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.localplayer.data.LocalMusicRepository
import com.example.localplayer.data.Song

class PlaybackService : MediaLibraryService() {
    private lateinit var librarySession: MediaLibrarySession

    private val songs: List<Song> by lazy { LocalMusicRepository(this).loadSongs() }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        librarySession = MediaLibrarySession.Builder(this, player, LibraryCallback(songs))
            .build()
    }

    override fun onDestroy() {
        librarySession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return librarySession
    }
}