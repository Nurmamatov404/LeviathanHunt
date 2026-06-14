package com.leviathan.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.leviathan.game.boardgames.BoardGameMenuScreen
import com.leviathan.game.screens.GameScreen

class LeviathanGame : ApplicationAdapter() {
    var currentScreen: GameScreen? = null

    override fun create() {
        setScreen(BoardGameMenuScreen(this))
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        currentScreen?.render(Gdx.graphics.deltaTime)
    }

    override fun resize(width: Int, height: Int) {
        currentScreen?.resize(width, height)
    }

    override fun dispose() {
        currentScreen?.dispose()
    }

    fun setScreen(screen: GameScreen) {
        currentScreen?.dispose()
        currentScreen = screen
        screen.show()
    }
}
