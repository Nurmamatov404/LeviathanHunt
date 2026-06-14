package com.leviathan.game

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Leviathan Hunt")
            setWindowedMode(1024, 600)
            setForegroundFPS(60)
            useVsync(true)
        }
        Lwjgl3Application(LeviathanGame(), config)
    }
}
