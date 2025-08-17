package com.lanmusicsync.utils

object Constants {
    // Network configuration
    const val DEFAULT_PORT = 8888
    const val DISCOVERY_PORT = 8889
    const val AUDIO_STREAM_PORT = 8890
    const val MULTICAST_ADDRESS = "224.0.0.1"
    
    // Service discovery
    const val SERVICE_TYPE = "_lanmusicsync._tcp"
    const val SERVICE_NAME = "LanMusicSync"
    
    // Connection timeouts
    const val CONNECTION_TIMEOUT_MS = 10000L
    const val HEARTBEAT_INTERVAL_MS = 5000L
    const val DISCOVERY_TIMEOUT_MS = 15000L
    
    // Audio configuration
    const val AUDIO_BUFFER_SIZE_MS = 2000
    const val SYNC_TOLERANCE_MS = 50L // Acceptable sync difference
    const val MAX_SYNC_ADJUSTMENT_MS = 500L
    
    // Message sizes
    const val MAX_MESSAGE_SIZE = 8192
    const val AUDIO_CHUNK_SIZE = 4096
    
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "music_sync_channel"
    const val NOTIFICATION_ID = 1001
    
    // Shared preferences
    const val PREFS_NAME = "lan_music_sync_prefs"
    const val PREF_DEVICE_NAME = "device_name"
    const val PREF_AUTO_CONNECT = "auto_connect"
    const val PREF_AUDIO_QUALITY = "audio_quality"
    
    // Intent actions
    const val ACTION_PLAY = "com.lanmusicsync.ACTION_PLAY"
    const val ACTION_PAUSE = "com.lanmusicsync.ACTION_PAUSE"
    const val ACTION_NEXT = "com.lanmusicsync.ACTION_NEXT"
    const val ACTION_PREVIOUS = "com.lanmusicsync.ACTION_PREVIOUS"
    const val ACTION_STOP = "com.lanmusicsync.ACTION_STOP"
    
    // Broadcast actions
    const val BROADCAST_CONNECTION_STATE_CHANGED = "com.lanmusicsync.CONNECTION_STATE_CHANGED"
    const val BROADCAST_PLAYBACK_STATE_CHANGED = "com.lanmusicsync.PLAYBACK_STATE_CHANGED"
    const val BROADCAST_DEVICE_DISCOVERED = "com.lanmusicsync.DEVICE_DISCOVERED"
    const val BROADCAST_SYNC_ERROR = "com.lanmusicsync.SYNC_ERROR"
}

