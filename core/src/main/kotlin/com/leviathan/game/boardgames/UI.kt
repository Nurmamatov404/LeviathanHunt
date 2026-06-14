package com.leviathan.game.boardgames

import com.badlogic.gdx.math.Rectangle

data class ButtonDef(val text: String, val rect: Rectangle, val action: () -> Unit)
