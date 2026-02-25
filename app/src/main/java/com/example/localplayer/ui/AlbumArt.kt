package com.example.localplayer.ui

import android.content.ContentUris
import android.net.Uri

object AlbumArt {
    private val base: Uri = Uri.parse("content://media/external/audio/albumart")

    fun uri(albumId: Long): Uri = ContentUris.withAppendedId(base, albumId)
}