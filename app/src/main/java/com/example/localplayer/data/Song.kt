package com.example.localplayer.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val contentUri: Uri,
    val durationMs: Long
)
