package com.lanmusicsync.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object NetworkUtils {
    
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    fun getWifiIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return if (ipInt != 0) {
            String.format(
                Locale.getDefault(),
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } else {
            null
        }
    }
    
    fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }
    
    fun isValidIpAddress(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun isPortAvailable(port: Int): Boolean {
        return try {
            java.net.ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getSubnetBroadcastAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo
        
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        return String.format(
            Locale.getDefault(),
            "%d.%d.%d.%d",
            broadcast and 0xff,
            broadcast shr 8 and 0xff,
            broadcast shr 16 and 0xff,
            broadcast shr 24 and 0xff
        )
    }
}

