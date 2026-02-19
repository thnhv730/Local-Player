package com.example.localplayer.data

import android.net.Uri

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val contentUri: Uri,
    val durationMs: Long
)
