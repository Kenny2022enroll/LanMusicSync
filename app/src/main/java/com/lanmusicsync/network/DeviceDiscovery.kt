package com.lanmusicsync.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.lanmusicsync.model.Device
import com.lanmusicsync.model.MessageType
import com.lanmusicsync.utils.Constants
import com.lanmusicsync.utils.NetworkUtils
import kotlinx.coroutines.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class DeviceDiscovery(private val context: Context) {
    
    private val tag = "DeviceDiscovery"
    private val isDiscovering = AtomicBoolean(false)
    private val discoveredDevices = ConcurrentHashMap<String, Device>()
    private var discoveryListener: DiscoveryListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var multicastSocket: MulticastSocket? = null
    
    interface DiscoveryListener {
        fun onDeviceDiscovered(device: Device)
        fun onDeviceLost(deviceId: String)
        fun onDiscoveryError(error: Exception)
    }
    
    fun setDiscoveryListener(listener: DiscoveryListener) {
        this.discoveryListener = listener
    }
    
    fun startDiscovery(deviceName: String, isHost: Boolean = false): Boolean {
        if (isDiscovering.get()) {
            Log.w(tag, "Discovery is already running")
            return false
        }
        
        if (!NetworkUtils.isWifiConnected(context)) {
            Log.e(tag, "WiFi is not connected")
            return false
        }
        
        return try {
            isDiscovering.set(true)
            
            if (isHost) {
                startHostAnnouncement(deviceName)
            } else {
                startClientDiscovery()
            }
            
            Log.i(tag, "Discovery started as ${if (isHost) "host" else "client"}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start discovery", e)
            discoveryListener?.onDiscoveryError(e)
            isDiscovering.set(false)
            false
        }
    }
    
    private fun startHostAnnouncement(deviceName: String) {
        scope.launch {
            try {
                val socket = DatagramSocket(Constants.DISCOVERY_PORT)
                val localIp = NetworkUtils.getLocalIpAddress() ?: return@launch
                
                val device = Device(
                    id = NetworkUtils.generateDeviceId(),
                    name = deviceName,
                    ipAddress = localIp,
                    port = Constants.DEFAULT_PORT,
                    isHost = true
                )
                
                val message = MessageSerializer.createMessage(
                    MessageType.DEVICE_DISCOVERY_RESPONSE,
                    device.id,
                    device
                )
                
                val messageData = MessageSerializer.serializeMessage(message)
                val broadcastAddress = NetworkUtils.getSubnetBroadcastAddress(context)
                    ?: "255.255.255.255"
                
                while (isDiscovering.get()) {
                    try {
                        val packet = DatagramPacket(
                            messageData,
                            messageData.size,
                            InetAddress.getByName(broadcastAddress),
                            Constants.DISCOVERY_PORT
                        )
                        socket.send(packet)
                        
                        delay(Constants.HEARTBEAT_INTERVAL_MS)
                    } catch (e: Exception) {
                        Log.e(tag, "Error sending discovery announcement", e)
                    }
                }
                
                socket.close()
            } catch (e: Exception) {
                Log.e(tag, "Error in host announcement", e)
                discoveryListener?.onDiscoveryError(e)
            }
        }
    }
    
    private fun startClientDiscovery() {
        scope.launch {
            try {
                val socket = DatagramSocket(Constants.DISCOVERY_PORT)
                socket.broadcast = true
                socket.soTimeout = 5000 // 5 second timeout
                
                // Send discovery request
                sendDiscoveryRequest(socket)
                
                // Listen for responses
                val buffer = ByteArray(Constants.MAX_MESSAGE_SIZE)
                
                while (isDiscovering.get()) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        
                        val receivedData = packet.data.copyOf(packet.length)
                        val message = MessageSerializer.deserializeMessage(receivedData)
                        
                        if (message?.type == MessageType.DEVICE_DISCOVERY_RESPONSE) {
                            message.data?.let { deviceJson ->
                                val device = MessageSerializer.deserializeDevice(deviceJson)
                                device?.let { dev ->
                                    if (dev.isHost && !discoveredDevices.containsKey(dev.id)) {
                                        discoveredDevices[dev.id] = dev
                                        discoveryListener?.onDeviceDiscovered(dev)
                                    }
                                }
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // Timeout is expected, continue listening
                        sendDiscoveryRequest(socket) // Send periodic discovery requests
                    } catch (e: Exception) {
                        if (isDiscovering.get()) {
                            Log.e(tag, "Error receiving discovery response", e)
                        }
                    }
                }
                
                socket.close()
            } catch (e: Exception) {
                Log.e(tag, "Error in client discovery", e)
                discoveryListener?.onDiscoveryError(e)
            }
        }
    }
    
    private fun sendDiscoveryRequest(socket: DatagramSocket) {
        try {
            val message = MessageSerializer.createMessage(
                MessageType.DEVICE_DISCOVERY,
                NetworkUtils.generateDeviceId()
            )
            
            val messageData = MessageSerializer.serializeMessage(message)
            val broadcastAddress = NetworkUtils.getSubnetBroadcastAddress(context)
                ?: "255.255.255.255"
            
            val packet = DatagramPacket(
                messageData,
                messageData.size,
                InetAddress.getByName(broadcastAddress),
                Constants.DISCOVERY_PORT
            )
            
            socket.send(packet)
        } catch (e: Exception) {
            Log.e(tag, "Error sending discovery request", e)
        }
    }
    
    fun stopDiscovery() {
        isDiscovering.set(false)
        
        try {
            multicastSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing multicast socket", e)
        }
        
        scope.cancel()
        discoveredDevices.clear()
        Log.i(tag, "Discovery stopped")
    }
    
    fun getDiscoveredDevices(): List<Device> {
        return discoveredDevices.values.toList()
    }
    
    fun clearDiscoveredDevices() {
        discoveredDevices.clear()
    }
    
    fun isRunning(): Boolean {
        return isDiscovering.get()
    }
}

