package com.lanmusicsync.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L, // in milliseconds
    val currentTrack: MusicTrack? = null,
    val playlist: Playlist? = null,
    val volume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val shuffleEnabled: Boolean = false,
    val lastUpdateTime: Long = System.currentTimeMillis()
) : Parcelable

enum class RepeatMode {
    NONE,
    ONE,
    ALL
}

@Parcelize
data class SyncData(
    val playbackState: PlaybackState,
    val serverTimestamp: Long,
    val expectedPlayPosition: Long, // Expected position at serverTimestamp
    val bufferHealth: Float = 1.0f // 0.0 to 1.0, buffer status
) : Parcelable

@Parcelize
data class AudioStreamInfo(
    val sampleRate: Int,
    val channelCount: Int,
    val bitRate: Int,
    val encoding: String, // e.g., "AAC", "MP3", "PCM"
    val bufferSizeMs: Int = 1000 // Buffer size in milliseconds
) : Parcelable

