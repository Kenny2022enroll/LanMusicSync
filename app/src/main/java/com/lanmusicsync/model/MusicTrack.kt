package com.lanmusicsync.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val filePath: String,
    val albumArtPath: String? = null,
    val size: Long = 0L // file size in bytes
) : Parcelable

@Parcelize
data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<MusicTrack>,
    val currentTrackIndex: Int = 0
) : Parcelable

