package com.lanmusicsync.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val isHost: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastSeen: Long = System.currentTimeMillis()
) : Parcelable

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

@Parcelize
data class NetworkMessage(
    val type: MessageType,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: String? = null
) : Parcelable

enum class MessageType {
    // Connection messages
    DEVICE_DISCOVERY,
    DEVICE_DISCOVERY_RESPONSE,
    CONNECTION_REQUEST,
    CONNECTION_ACCEPTED,
    CONNECTION_REJECTED,
    DISCONNECT,
    
    // Music control messages
    PLAY,
    PAUSE,
    STOP,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    SEEK_TO,
    VOLUME_CHANGE,
    
    // Sync messages
    PLAYLIST_UPDATE,
    TRACK_CHANGE,
    PLAYBACK_STATE,
    TIME_SYNC,
    
    // Audio streaming
    AUDIO_DATA,
    AUDIO_BUFFER_STATUS,
    
    // Status messages
    HEARTBEAT,
    ERROR
}

