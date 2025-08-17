package com.lanmusicsync.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lanmusicsync.model.*
import java.nio.charset.StandardCharsets

object MessageSerializer {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    fun serializeMessage(message: NetworkMessage): ByteArray {
        val json = gson.toJson(message)
        return json.toByteArray(StandardCharsets.UTF_8)
    }
    
    fun deserializeMessage(data: ByteArray): NetworkMessage? {
        return try {
            val json = String(data, StandardCharsets.UTF_8)
            gson.fromJson(json, NetworkMessage::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun serializePlaybackState(state: PlaybackState): String {
        return gson.toJson(state)
    }
    
    fun deserializePlaybackState(json: String): PlaybackState? {
        return try {
            gson.fromJson(json, PlaybackState::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun serializeDevice(device: Device): String {
        return gson.toJson(device)
    }
    
    fun deserializeDevice(json: String): Device? {
        return try {
            gson.fromJson(json, Device::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun serializePlaylist(playlist: Playlist): String {
        return gson.toJson(playlist)
    }
    
    fun deserializePlaylist(json: String): Playlist? {
        return try {
            gson.fromJson(json, Playlist::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun serializeSyncData(syncData: SyncData): String {
        return gson.toJson(syncData)
    }
    
    fun deserializeSyncData(json: String): SyncData? {
        return try {
            gson.fromJson(json, SyncData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun createMessage(
        type: MessageType,
        senderId: String,
        data: Any? = null
    ): NetworkMessage {
        val dataString = when (data) {
            is String -> data
            null -> null
            else -> gson.toJson(data)
        }
        
        return NetworkMessage(
            type = type,
            senderId = senderId,
            data = dataString
        )
    }
}

