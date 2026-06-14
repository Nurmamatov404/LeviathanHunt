package com.leviathan.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.leviathan.game.LeviathanGame
import com.leviathan.game.network.*

class GameOverScreen(
    private val game: LeviathanGame,
    private val winner: PlayerRole,
    private val reason: String
) : GameScreen {
    private lateinit var batch: SpriteBatch
    private lateinit var shape: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var cam: OrthographicCamera
    private val buttons = mutableListOf<MenuScreen.ButtonDef>()

    private val sw get() = Gdx.graphics.width.toFloat()
    private val sh get() = Gdx.graphics.height.toFloat()

    override fun show() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        font = BitmapFont()
        cam = OrthographicCamera(sw, sh)
        cam.setToOrtho(false)

        val bw = (sw * 0.5f).coerceIn(200f, 300f)
        val bh = (sh * 0.08f).coerceIn(50f, 65f)
        val gap = bh * 1.4f
        val cx = sw / 2f

        buttons.add(MenuScreen.ButtonDef("MAIN MENU", Rectangle(cx - bw/2, sh * 0.3f, bw, bh)) {
            game.networkManager.disconnect(); game.setScreen(MenuScreen(game))
        })
        buttons.add(MenuScreen.ButtonDef("EXIT", Rectangle(cx - bw/2, sh * 0.3f - gap, bw, bh)) {
            Gdx.app.exit()
        })
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined

        val bh = (sh * 0.08f).coerceIn(50f, 65f)

        shape.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { shape.setColor(0.15f, 0.4f, 0.8f, 0.9f); shape.rect(it.rect.x, it.rect.y, it.rect.width, it.rect.height) }
        shape.end()

        batch.begin()
        font.data.setScale(sh / 200f)
        val winText = if (winner == game.playerRole) "VICTORY!" else "DEFEAT"
        font.draw(batch, winText, sw / 2f - 90f, sh - 100f)

        font.data.setScale(sh / 350f)
        val roleText = if (winner == PlayerRole.SHIP) "SHIP WINS" else "MONSTER WINS"
        font.draw(batch, roleText, sw / 2f - 80f, sh - 150f)
        font.draw(batch, reason, sw / 2f - 80f, sh - 190f)

        font.data.setScale(bh / 30f)
        buttons.forEach { font.draw(batch, it.text, it.rect.x + it.rect.width * 0.15f, it.rect.y + it.rect.height * 0.65f) }
        batch.end()

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = sh - Gdx.input.y.toFloat()
            buttons.forEach { if (it.rect.contains(mx, my)) it.action() }
        }
    }

    override fun onNetworkPacket(packet: Packet) {}
    override fun getGame() = game
    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); shape.dispose(); font.dispose() }
}
