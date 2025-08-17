package com.lanmusicsync.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.lanmusicsync.R
import com.lanmusicsync.model.*
import com.lanmusicsync.network.*
import com.lanmusicsync.utils.Constants
import com.lanmusicsync.utils.NetworkUtils
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class MusicSyncService : Service(), SocketManager.MessageListener, DeviceDiscovery.DiscoveryListener {
    
    private val tag = "MusicSyncService"
    private val binder = MusicSyncBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Core components
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var socketManager: SocketManager
    private lateinit var deviceDiscovery: DeviceDiscovery
    
    // State management
    private var isHost = AtomicBoolean(false)
    private var currentDevice: Device? = null
    private var currentPlaybackState = PlaybackState()
    private var connectedDevices = mutableListOf<Device>()
    
    // Listeners
    private var serviceListener: ServiceListener? = null
    
    interface ServiceListener {
        fun onPlaybackStateChanged(state: PlaybackState)
        fun onDeviceConnected(device: Device)
        fun onDeviceDisconnected(deviceId: String)
        fun onDeviceDiscovered(device: Device)
        fun onSyncError(error: String)
    }
    
    inner class MusicSyncBinder : Binder() {
        fun getService(): MusicSyncService = this@MusicSyncService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service created")
        
        initializeComponents()
        createNotificationChannel()
    }
    
    private fun initializeComponents() {
        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updatePlaybackState()
            }
        })
        
        // Initialize network components
        socketManager = SocketManager()
        socketManager.setMessageListener(this)
        
        deviceDiscovery = DeviceDiscovery(this)
        deviceDiscovery.setDiscoveryListener(this)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.music_sync_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.music_sync_channel_description)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "Service started")
        
        when (intent?.action) {
            Constants.ACTION_PLAY -> play()
            Constants.ACTION_PAUSE -> pause()
            Constants.ACTION_NEXT -> nextTrack()
            Constants.ACTION_PREVIOUS -> previousTrack()
            Constants.ACTION_STOP -> stop()
        }
        
        startForeground(Constants.NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    fun setServiceListener(listener: ServiceListener) {
        this.serviceListener = listener
    }
    
    // Host functionality
    fun startAsHost(deviceName: String): Boolean {
        if (isHost.get()) {
            Log.w(tag, "Already running as host")
            return false
        }
        
        val localIp = NetworkUtils.getLocalIpAddress()
        if (localIp == null) {
            Log.e(tag, "Cannot get local IP address")
            return false
        }
        
        currentDevice = Device(
            id = NetworkUtils.generateDeviceId(),
            name = deviceName,
            ipAddress = localIp,
            port = Constants.DEFAULT_PORT,
            isHost = true
        )
        
        val success = socketManager.startServer(Constants.DEFAULT_PORT) &&
                deviceDiscovery.startDiscovery(deviceName, true)
        
        if (success) {
            isHost.set(true)
            Log.i(tag, "Started as host: $deviceName")
        }
        
        return success
    }
    
    // Client functionality
    fun startAsClient(deviceName: String): Boolean {
        if (isHost.get()) {
            Log.w(tag, "Cannot start as client while running as host")
            return false
        }
        
        val localIp = NetworkUtils.getLocalIpAddress()
        if (localIp == null) {
            Log.e(tag, "Cannot get local IP address")
            return false
        }
        
        currentDevice = Device(
            id = NetworkUtils.generateDeviceId(),
            name = deviceName,
            ipAddress = localIp,
            port = 0,
            isHost = false
        )
        
        return deviceDiscovery.startDiscovery(deviceName, false)
    }
    
    fun connectToHost(hostDevice: Device): Boolean {
        if (isHost.get()) {
            Log.w(tag, "Cannot connect to host while running as host")
            return false
        }
        
        return socketManager.connectToServer(hostDevice.ipAddress, hostDevice.port)
    }
    
    // Music control
    fun play() {
        if (isHost.get()) {
            exoPlayer.play()
            broadcastPlaybackCommand(MessageType.PLAY)
        } else {
            // Send play request to host
            currentDevice?.let { device ->
                val message = MessageSerializer.createMessage(MessageType.PLAY, device.id)
                socketManager.sendMessage(message)
            }
        }
    }
    
    fun pause() {
        if (isHost.get()) {
            exoPlayer.pause()
            broadcastPlaybackCommand(MessageType.PAUSE)
        } else {
            currentDevice?.let { device ->
                val message = MessageSerializer.createMessage(MessageType.PAUSE, device.id)
                socketManager.sendMessage(message)
            }
        }
    }
    
    fun stop() {
        if (isHost.get()) {
            exoPlayer.stop()
            broadcastPlaybackCommand(MessageType.STOP)
        } else {
            currentDevice?.let { device ->
                val message = MessageSerializer.createMessage(MessageType.STOP, device.id)
                socketManager.sendMessage(message)
            }
        }
    }
    
    fun nextTrack() {
        if (isHost.get()) {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNext()
                broadcastPlaybackCommand(MessageType.NEXT_TRACK)
            }
        } else {
            currentDevice?.let { device ->
                val message = MessageSerializer.createMessage(MessageType.NEXT_TRACK, device.id)
                socketManager.sendMessage(message)
            }
        }
    }
    
    fun previousTrack() {
        if (isHost.get()) {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPrevious()
                broadcastPlaybackCommand(MessageType.PREVIOUS_TRACK)
            }
        } else {
            currentDevice?.let { device ->
                val message = MessageSerializer.createMessage(MessageType.PREVIOUS_TRACK, device.id)
                socketManager.sendMessage(message)
            }
        }
    }
    
    fun seekTo(positionMs: Long) {
        if (isHost.get()) {
            exoPlayer.seekTo(positionMs)
            val message = MessageSerializer.createMessage(
                MessageType.SEEK_TO,
                currentDevice?.id ?: "",
                positionMs.toString()
            )
            socketManager.sendMessage(message)
        }
    }
    
    fun setVolume(volume: Float) {
        exoPlayer.volume = volume
        currentPlaybackState = currentPlaybackState.copy(volume = volume)
        
        if (isHost.get()) {
            val message = MessageSerializer.createMessage(
                MessageType.VOLUME_CHANGE,
                currentDevice?.id ?: "",
                volume.toString()
            )
            socketManager.sendMessage(message)
        }
    }
    
    fun setPlaylist(playlist: Playlist) {
        if (!isHost.get()) {
            Log.w(tag, "Only host can set playlist")
            return
        }
        
        // Clear current playlist
        exoPlayer.clearMediaItems()
        
        // Add new tracks
        playlist.tracks.forEach { track ->
            val mediaItem = MediaItem.fromUri(track.filePath)
            exoPlayer.addMediaItem(mediaItem)
        }
        
        // Prepare player
        exoPlayer.prepare()
        
        // Broadcast playlist to clients
        val message = MessageSerializer.createMessage(
            MessageType.PLAYLIST_UPDATE,
            currentDevice?.id ?: "",
            playlist
        )
        socketManager.sendMessage(message)
        
        updatePlaybackState()
    }
    
    private fun broadcastPlaybackCommand(command: MessageType) {
        currentDevice?.let { device ->
            val message = MessageSerializer.createMessage(command, device.id)
            socketManager.sendMessage(message)
        }
    }
    
    private fun updatePlaybackState() {
        val currentTrack = getCurrentTrack()
        val newState = PlaybackState(
            isPlaying = exoPlayer.isPlaying,
            currentPosition = exoPlayer.currentPosition,
            currentTrack = currentTrack,
            volume = exoPlayer.volume,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        currentPlaybackState = newState
        serviceListener?.onPlaybackStateChanged(newState)
        
        // Broadcast state to clients if host
        if (isHost.get()) {
            val syncData = SyncData(
                playbackState = newState,
                serverTimestamp = System.currentTimeMillis(),
                expectedPlayPosition = exoPlayer.currentPosition
            )
            
            val message = MessageSerializer.createMessage(
                MessageType.PLAYBACK_STATE,
                currentDevice?.id ?: "",
                syncData
            )
            socketManager.sendMessage(message)
        }
        
        updateNotification()
    }
    
    private fun getCurrentTrack(): MusicTrack? {
        // This would need to be implemented based on how you store track metadata
        // For now, return null
        return null
    }
    
    // SocketManager.MessageListener implementation
    override fun onMessageReceived(message: NetworkMessage, clientId: String) {
        scope.launch {
            handleNetworkMessage(message, clientId)
        }
    }
    
    override fun onClientConnected(clientId: String, address: String) {
        Log.i(tag, "Client connected: $clientId from $address")
        // You might want to create a Device object and add to connectedDevices
    }
    
    override fun onClientDisconnected(clientId: String) {
        Log.i(tag, "Client disconnected: $clientId")
        serviceListener?.onDeviceDisconnected(clientId)
    }
    
    override fun onError(error: Exception) {
        Log.e(tag, "Socket error", error)
        serviceListener?.onSyncError(error.message ?: "Network error")
    }
    
    // DeviceDiscovery.DiscoveryListener implementation
    override fun onDeviceDiscovered(device: Device) {
        Log.i(tag, "Device discovered: ${device.name}")
        serviceListener?.onDeviceDiscovered(device)
    }
    
    override fun onDeviceLost(deviceId: String) {
        Log.i(tag, "Device lost: $deviceId")
        serviceListener?.onDeviceDisconnected(deviceId)
    }
    
    override fun onDiscoveryError(error: Exception) {
        Log.e(tag, "Discovery error", error)
        serviceListener?.onSyncError(error.message ?: "Discovery error")
    }
    
    private suspend fun handleNetworkMessage(message: NetworkMessage, clientId: String) {
        when (message.type) {
            MessageType.PLAY -> if (!isHost.get()) exoPlayer.play()
            MessageType.PAUSE -> if (!isHost.get()) exoPlayer.pause()
            MessageType.STOP -> if (!isHost.get()) exoPlayer.stop()
            MessageType.NEXT_TRACK -> if (!isHost.get()) exoPlayer.seekToNext()
            MessageType.PREVIOUS_TRACK -> if (!isHost.get()) exoPlayer.seekToPrevious()
            MessageType.SEEK_TO -> {
                if (!isHost.get()) {
                    message.data?.toLongOrNull()?.let { position ->
                        exoPlayer.seekTo(position)
                    }
                }
            }
            MessageType.VOLUME_CHANGE -> {
                message.data?.toFloatOrNull()?.let { volume ->
                    exoPlayer.volume = volume
                }
            }
            MessageType.PLAYLIST_UPDATE -> {
                if (!isHost.get()) {
                    message.data?.let { playlistJson ->
                        MessageSerializer.deserializePlaylist(playlistJson)?.let { playlist ->
                            // Handle playlist update for client
                            // Implementation depends on how you want to handle this
                        }
                    }
                }
            }
            MessageType.PLAYBACK_STATE -> {
                if (!isHost.get()) {
                    message.data?.let { syncDataJson ->
                        MessageSerializer.deserializeSyncData(syncDataJson)?.let { syncData ->
                            // Handle playback state sync for client
                            handlePlaybackSync(syncData)
                        }
                    }
                }
            }
            else -> {
                Log.d(tag, "Unhandled message type: ${message.type}")
            }
        }
    }
    
    private fun handlePlaybackSync(syncData: SyncData) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - syncData.serverTimestamp
        val expectedPosition = syncData.expectedPlayPosition + timeDiff
        
        val currentPosition = exoPlayer.currentPosition
        val positionDiff = expectedPosition - currentPosition
        
        // Only adjust if difference is significant
        if (kotlin.math.abs(positionDiff) > Constants.SYNC_TOLERANCE_MS) {
            exoPlayer.seekTo(expectedPosition)
        }
        
        // Update playback state
        if (syncData.playbackState.isPlaying != exoPlayer.isPlaying) {
            if (syncData.playbackState.isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, Class.forName("com.lanmusicsync.ui.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (currentPlaybackState.isPlaying) 
                    getString(R.string.music_playing) 
                else 
                    getString(R.string.music_syncing)
            )
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }
    
    fun getCurrentPlaybackState(): PlaybackState = currentPlaybackState
    
    fun getConnectedDevices(): List<Device> = connectedDevices.toList()
    
    fun isRunningAsHost(): Boolean = isHost.get()
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "Service destroyed")
        
        exoPlayer.release()
        socketManager.stop()
        deviceDiscovery.stopDiscovery()
        scope.cancel()
    }
}

