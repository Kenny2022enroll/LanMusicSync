package com.lanmusicsync.network

import android.util.Log
import com.lanmusicsync.model.NetworkMessage
import com.lanmusicsync.utils.Constants
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SocketManager {
    
    private val tag = "SocketManager"
    private val isRunning = AtomicBoolean(false)
    private val clients = ConcurrentHashMap<String, ClientConnection>()
    private var serverSocket: ServerSocket? = null
    private var messageListener: MessageListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    interface MessageListener {
        fun onMessageReceived(message: NetworkMessage, clientId: String)
        fun onClientConnected(clientId: String, address: String)
        fun onClientDisconnected(clientId: String)
        fun onError(error: Exception)
    }
    
    private data class ClientConnection(
        val socket: Socket,
        val input: DataInputStream,
        val output: DataOutputStream,
        val address: String
    )
    
    fun setMessageListener(listener: MessageListener) {
        this.messageListener = listener
    }
    
    fun startServer(port: Int = Constants.DEFAULT_PORT): Boolean {
        if (isRunning.get()) {
            Log.w(tag, "Server is already running")
            return false
        }
        
        return try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            
            scope.launch {
                acceptClients()
            }
            
            Log.i(tag, "Server started on port $port")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start server", e)
            messageListener?.onError(e)
            false
        }
    }
    
    private suspend fun acceptClients() {
        while (isRunning.get() && serverSocket?.isClosed == false) {
            try {
                val clientSocket = withContext(Dispatchers.IO) {
                    serverSocket?.accept()
                }
                
                clientSocket?.let { socket ->
                    val clientId = "${socket.inetAddress.hostAddress}:${socket.port}"
                    val input = DataInputStream(socket.getInputStream())
                    val output = DataOutputStream(socket.getOutputStream())
                    
                    val connection = ClientConnection(socket, input, output, socket.inetAddress.hostAddress)
                    clients[clientId] = connection
                    
                    messageListener?.onClientConnected(clientId, connection.address)
                    
                    // Start listening for messages from this client
                    scope.launch {
                        listenToClient(clientId, connection)
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(tag, "Error accepting client", e)
                    messageListener?.onError(e)
                }
            }
        }
    }
    
    private suspend fun listenToClient(clientId: String, connection: ClientConnection) {
        try {
            while (isRunning.get() && !connection.socket.isClosed) {
                val messageLength = withContext(Dispatchers.IO) {
                    connection.input.readInt()
                }
                
                if (messageLength > 0 && messageLength <= Constants.MAX_MESSAGE_SIZE) {
                    val messageData = ByteArray(messageLength)
                    withContext(Dispatchers.IO) {
                        connection.input.readFully(messageData)
                    }
                    
                    val message = MessageSerializer.deserializeMessage(messageData)
                    message?.let {
                        messageListener?.onMessageReceived(it, clientId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error listening to client $clientId", e)
        } finally {
            disconnectClient(clientId)
        }
    }
    
    fun connectToServer(address: String, port: Int = Constants.DEFAULT_PORT): Boolean {
        if (isRunning.get()) {
            Log.w(tag, "Already connected or running as server")
            return false
        }
        
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(address, port), Constants.CONNECTION_TIMEOUT_MS.toInt())
            
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            val connection = ClientConnection(socket, input, output, address)
            
            clients["server"] = connection
            isRunning.set(true)
            
            scope.launch {
                listenToClient("server", connection)
            }
            
            Log.i(tag, "Connected to server at $address:$port")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to server", e)
            messageListener?.onError(e)
            false
        }
    }
    
    fun sendMessage(message: NetworkMessage, clientId: String? = null): Boolean {
        val messageData = MessageSerializer.serializeMessage(message)
        
        return if (clientId != null) {
            // Send to specific client
            sendToClient(clientId, messageData)
        } else {
            // Broadcast to all clients
            var success = true
            clients.keys.forEach { id ->
                if (!sendToClient(id, messageData)) {
                    success = false
                }
            }
            success
        }
    }
    
    private fun sendToClient(clientId: String, data: ByteArray): Boolean {
        val connection = clients[clientId] ?: return false
        
        return try {
            synchronized(connection.output) {
                connection.output.writeInt(data.size)
                connection.output.write(data)
                connection.output.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message to client $clientId", e)
            disconnectClient(clientId)
            false
        }
    }
    
    fun disconnectClient(clientId: String) {
        clients[clientId]?.let { connection ->
            try {
                connection.socket.close()
            } catch (e: Exception) {
                Log.e(tag, "Error closing client socket", e)
            }
            clients.remove(clientId)
            messageListener?.onClientDisconnected(clientId)
        }
    }
    
    fun stop() {
        isRunning.set(false)
        
        // Close all client connections
        clients.keys.toList().forEach { clientId ->
            disconnectClient(clientId)
        }
        
        // Close server socket
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing server socket", e)
        }
        
        scope.cancel()
        Log.i(tag, "Socket manager stopped")
    }
    
    fun getConnectedClients(): List<String> {
        return clients.keys.toList()
    }
    
    fun isConnected(): Boolean {
        return isRunning.get() && clients.isNotEmpty()
    }
}

