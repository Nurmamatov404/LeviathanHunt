package com.leviathan.game.screens

import com.badlogic.gdx.Screen
import com.leviathan.game.LeviathanGame
import com.leviathan.game.network.Packet

interface GameScreen : Screen {
    fun onNetworkPacket(packet: Packet)
    fun getGame(): LeviathanGame
}
