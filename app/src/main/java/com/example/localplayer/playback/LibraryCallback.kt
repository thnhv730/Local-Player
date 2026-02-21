package com.example.localplayer.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.example.localplayer.data.Song
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class LibraryCallback(
    private val songs: List<Song>
) : MediaLibraryService.MediaLibrarySession.Callback {

    companion object {
        private const val ROOT_ID = "root"
        private const val SONGS_ID = "songs"
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {

        val root = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Local Player")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        return Futures.immediateFuture(LibraryResult.ofItem(root, params))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

        val items: ImmutableList<MediaItem> = when (parentId) {
            ROOT_ID -> {
                ImmutableList.of(
                    MediaItem.Builder()
                        .setMediaId(SONGS_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Songs")
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .build()
                        )
                        .build()
                )
            }

            SONGS_ID -> {
                val list = songs.map { it.toMediaItem() }
                val from = (page * pageSize).coerceAtMost(list.size)
                val to = (from + pageSize).coerceAtMost(list.size)
                ImmutableList.copyOf(list.subList(from, to))
            }

            else -> ImmutableList.of()
        }

        return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {

        val song = songs.firstOrNull { it.id.toString() == mediaId }
        val item = song?.toMediaItem()

        return Futures.immediateFuture(
            if (item != null) LibraryResult.ofItem(item, null)
            else LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        )
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
}