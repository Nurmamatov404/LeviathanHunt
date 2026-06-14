package com.leviathan.game.boardgames

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

enum class BoardGameType { CHECKERS, CHESS }

data class BoardGameLobby(
    val gameId: String,
    val hostName: String,
    val gameType: BoardGameType,
    val ip: String,
    val port: Int,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class BoardGamePacket {
    data class JoinRequest(val playerName: String, val gameType: BoardGameType) : BoardGamePacket()
    data class JoinAccepted(val yourColor: String, val opponentName: String) : BoardGamePacket()
    data class Move(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int,
                    val promotion: String = "", val captures: String = "") : BoardGamePacket()
    data class GameOver(val winner: String, val reason: String) : BoardGamePacket()
    data class Rematch(val accepted: Boolean) : BoardGamePacket()
    data class ChatMessage(val message: String) : BoardGamePacket()
}

class BoardGameNetwork {
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var connected = false
    private var isHost = false
    private var discoverySocket: DatagramSocket? = null
    private var isRunning = false

    var onPacketReceived: ((BoardGamePacket) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: ((String) -> Unit)? = null
    var onGameFound: ((BoardGameLobby) -> Unit)? = null

    private val gson = Gson()
    private val DISCOVERY_PORT = 7788
    private val GAME_PORT = 7779
    private val BROADCAST_ADDR = "255.255.255.255"

    fun startHost(hostName: String, gameType: BoardGameType): Boolean {
        return try {
            isHost = true
            serverSocket = ServerSocket(GAME_PORT)
            thread(name = "BG-Server") {
                try {
                    socket = serverSocket?.accept()
                    socket?.let { setupConnection(it) }
                } catch (e: Exception) {
                    onDisconnected?.invoke("Connection failed")
                }
            }
            startDiscoveryBroadcast(hostName, gameType)
            true
        } catch (e: Exception) {
            onDisconnected?.invoke("Could not start host")
            false
        }
    }

    private fun startDiscoveryBroadcast(hostName: String, gameType: BoardGameType) {
        isRunning = true
        thread(name = "BG-Discovery") {
            try {
                discoverySocket = DatagramSocket(DISCOVERY_PORT)
                discoverySocket?.broadcast = true
                val buffer = ByteArray(1024)

                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    discoverySocket?.soTimeout = 2000
                    try {
                        discoverySocket?.receive(packet)
                        val msg = String(packet.data, 0, packet.length)
                        if (msg == "BOARD_GAME_DISCOVER") {
                            val response = gson.toJson(BoardGameLobby(
                                gameId = hostName + System.currentTimeMillis(),
                                hostName = hostName,
                                gameType = gameType,
                                ip = getLocalIpAddress(),
                                port = GAME_PORT
                            ))
                            val respPacket = DatagramPacket(
                                response.toByteArray(), response.length,
                                packet.address, packet.port
                            )
                            discoverySocket?.send(respPacket)
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // continue
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun discoverGames(): List<BoardGameLobby> {
        val games = mutableListOf<BoardGameLobby>()
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 2000
            val msg = "BOARD_GAME_DISCOVER"
            val packet = DatagramPacket(msg.toByteArray(), msg.length,
                InetAddress.getByName(BROADCAST_ADDR), DISCOVERY_PORT)
            socket.send(packet)

            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 2000) {
                try {
                    val resp = DatagramPacket(buffer, buffer.size)
                    socket.receive(resp)
                    val json = String(resp.data, 0, resp.length)
                    val lobby = gson.fromJson(json, BoardGameLobby::class.java)
                    if (lobby != null) {
                        games.add(lobby.copy(ip = resp.address.hostAddress ?: lobby.ip))
                    }
                } catch (_: Exception) { break }
            }
            socket.close()
        } catch (_: Exception) {}
        return games.distinctBy { it.gameId }
    }

    fun connectToHost(ip: String): Boolean {
        return try {
            isHost = false
            socket = Socket()
            socket?.connect(InetSocketAddress(ip, GAME_PORT), 5000)
            socket?.let { setupConnection(it) }
            true
        } catch (e: Exception) {
            onDisconnected?.invoke("Could not connect")
            false
        }
    }

    private fun setupConnection(sock: Socket) {
        try {
            writer = OutputStreamWriter(sock.getOutputStream())
            reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            connected = true
            onConnected?.invoke()

            thread(name = "BG-NetRead") {
                try {
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        val packet = deserializePacket(line!!)
                        if (packet != null) {
                            onPacketReceived?.invoke(packet)
                        }
                    }
                } catch (_: Exception) {
                    if (connected) onDisconnected?.invoke("Connection lost")
                }
            }
        } catch (_: Exception) {
            onDisconnected?.invoke("Setup error")
        }
    }

    fun sendPacket(packet: BoardGamePacket) {
        try {
            val json = serializePacket(packet) + "\n"
            writer?.write(json)
            writer?.flush()
        } catch (_: Exception) {
            onDisconnected?.invoke("Send error")
        }
    }

    private fun serializePacket(packet: BoardGamePacket): String {
        return when (packet) {
            is BoardGamePacket.JoinRequest -> gson.toJson(mapOf("type" to "join_req", "data" to packet))
            is BoardGamePacket.JoinAccepted -> gson.toJson(mapOf("type" to "join_acc", "data" to packet))
            is BoardGamePacket.Move -> gson.toJson(mapOf("type" to "move", "data" to packet))
            is BoardGamePacket.GameOver -> gson.toJson(mapOf("type" to "game_over", "data" to packet))
            is BoardGamePacket.Rematch -> gson.toJson(mapOf("type" to "rematch", "data" to packet))
            is BoardGamePacket.ChatMessage -> gson.toJson(mapOf("type" to "chat", "data" to packet))
        }
    }

    private fun deserializePacket(json: String): BoardGamePacket? {
        return try {
            val map = gson.fromJson(json, Map::class.java)
            val type = map["type"] as String
            val data = gson.toJson(map["data"])
            when (type) {
                "join_req" -> gson.fromJson(data, BoardGamePacket.JoinRequest::class.java)
                "join_acc" -> gson.fromJson(data, BoardGamePacket.JoinAccepted::class.java)
                "move" -> gson.fromJson(data, BoardGamePacket.Move::class.java)
                "game_over" -> gson.fromJson(data, BoardGamePacket.GameOver::class.java)
                "rematch" -> gson.fromJson(data, BoardGamePacket.Rematch::class.java)
                "chat" -> gson.fromJson(data, BoardGamePacket.ChatMessage::class.java)
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            val ip = socket.localAddress.hostAddress ?: "127.0.0.1"
            socket.close()
            ip
        } catch (_: Exception) { "127.0.0.1" }
    }

    fun disconnect() {
        isRunning = false
        connected = false
        try { discoverySocket?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    fun isConnected() = connected
    fun isHosting() = isHost
}
