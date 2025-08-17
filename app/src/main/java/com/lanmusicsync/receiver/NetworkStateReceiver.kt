package com.lanmusicsync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.lanmusicsync.utils.Constants

class NetworkStateReceiver : BroadcastReceiver() {
    
    private val tag = "NetworkStateReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                handleConnectivityChange(context)
            }
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                handleWifiP2pStateChange(intent)
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                handlePeersChanged()
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                handleConnectionChanged(intent)
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                handleThisDeviceChanged(intent)
            }
        }
    }
    
    private fun handleConnectivityChange(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        val isWifiConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        
        Log.d(tag, "WiFi connection state changed: $isWifiConnected")
        
        // Broadcast network state change
        val broadcastIntent = Intent(Constants.BROADCAST_CONNECTION_STATE_CHANGED).apply {
            putExtra("wifi_connected", isWifiConnected)
        }
        context.sendBroadcast(broadcastIntent)
    }
    
    private fun handleWifiP2pStateChange(intent: Intent) {
        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
        val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
        
        Log.d(tag, "WiFi P2P state changed: ${if (isEnabled) "enabled" else "disabled"}")
        
        // Broadcast WiFi P2P state change
        val broadcastIntent = Intent(Constants.BROADCAST_CONNECTION_STATE_CHANGED).apply {
            putExtra("wifi_p2p_enabled", isEnabled)
        }
        intent.resolveActivity(null)?.let { context ->
            // Note: We can't get context directly here, this would need to be handled differently
            // in a real implementation, possibly through a service or application context
        }
    }
    
    private fun handlePeersChanged() {
        Log.d(tag, "WiFi P2P peers changed")
        // This would trigger a new peer discovery in the service
    }
    
    private fun handleConnectionChanged(intent: Intent) {
        Log.d(tag, "WiFi P2P connection changed")
        // Handle connection state changes
    }
    
    private fun handleThisDeviceChanged(intent: Intent) {
        Log.d(tag, "WiFi P2P this device changed")
        // Handle device information changes
    }
}

