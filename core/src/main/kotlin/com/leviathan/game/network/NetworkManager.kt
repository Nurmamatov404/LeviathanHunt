package com.leviathan.game.network

import com.leviathan.game.utils.Constants
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class NetworkManager {
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var connected = false
    private var isHost = false

    var onPacketReceived: ((Packet) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: ((String) -> Unit)? = null

    fun startHost(): Boolean {
        return try {
            isHost = true
            serverSocket = ServerSocket(Constants.PORT)
            serverSocket?.soTimeout = 0
            thread(name = "Server-Listen") {
                try {
                    socket = serverSocket?.accept()
                    socket?.let { setupConnection(it) }
                } catch (e: Exception) {
                    onDisconnected?.invoke("Connection failed: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            onDisconnected?.invoke("Could not start host: ${e.message}")
            false
        }
    }

    fun connectToHost(ipAddress: String): Boolean {
        return try {
            isHost = false
            socket = Socket()
            socket?.connect(InetSocketAddress(ipAddress, Constants.PORT), 5000)
            socket?.let { setupConnection(it) }
            true
        } catch (e: Exception) {
            onDisconnected?.invoke("Could not connect: ${e.message}")
            false
        }
    }

    private fun setupConnection(sock: Socket) {
        try {
            writer = OutputStreamWriter(sock.getOutputStream())
            reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            connected = true
            onConnected?.invoke()

            thread(name = "Network-Read") {
                try {
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        val packet = PacketSerializer.deserialize<Packet>(line!!)
                        if (packet != null) {
                            onPacketReceived?.invoke(packet)
                        }
                    }
                } catch (e: Exception) {
                    if (connected) {
                        onDisconnected?.invoke("Connection lost")
                    }
                }
            }
        } catch (e: Exception) {
            onDisconnected?.invoke("Setup error: ${e.message}")
        }
    }

    fun sendPacket(packet: Packet) {
        try {
            val json = PacketSerializer.serialize(packet)
            writer?.write(json)
            writer?.flush()
        } catch (e: Exception) {
            onDisconnected?.invoke("Send error: ${e.message}")
        }
    }

    fun disconnect() {
        connected = false
        try {
            writer?.close()
            reader?.close()
            socket?.close()
            serverSocket?.close()
        } catch (_: Exception) {}
        writer = null
        reader = null
        socket = null
        serverSocket = null
    }

    fun isConnected(): Boolean = connected
    fun isHosting(): Boolean = isHost
    fun isClient(): Boolean = !isHost && connected
}
