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

    override fun show() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        font = BitmapFont()
        font.data.setScale(2f)
        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.setToOrtho(false)

        val cx = Gdx.graphics.width / 2f
        buttons.add(MenuScreen.ButtonDef("MAIN MENU", Rectangle(cx - 125f, 200f, 250f, 50f)) {
            game.networkManager.disconnect()
            game.setScreen(MenuScreen(game))
        })
        buttons.add(MenuScreen.ButtonDef("EXIT", Rectangle(cx - 125f, 130f, 250f, 50f)) {
            Gdx.app.exit()
        })
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { shape.setColor(0.15f, 0.4f, 0.8f, 0.9f); shape.rect(it.rect.x, it.rect.y, it.rect.width, it.rect.height) }
        shape.end()

        batch.begin()
        font.data.setScale(2.5f)
        val winText = if (winner == game.playerRole) "VICTORY!" else "DEFEAT"
        font.draw(batch, winText, Gdx.graphics.width / 2f - 80f, Gdx.graphics.height - 100f)

        font.data.setScale(1.5f)
        val roleText = if (winner == PlayerRole.SHIP) "SHIP WINS" else "MONSTER WINS"
        font.draw(batch, roleText, Gdx.graphics.width / 2f - 70f, Gdx.graphics.height - 150f)
        font.draw(batch, reason, Gdx.graphics.width / 2f - 70f, Gdx.graphics.height - 190f)

        font.data.setScale(1.5f)
        buttons.forEach { font.draw(batch, it.text, it.rect.x + 20f, it.rect.y + 35f) }
        batch.end()

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = Gdx.graphics.height - Gdx.input.y.toFloat()
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
