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

class LobbyScreen(private val game: LeviathanGame) : GameScreen {
    private lateinit var batch: SpriteBatch
    private lateinit var shape: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var cam: OrthographicCamera

    private val buttons = mutableListOf<MenuScreen.ButtonDef>()
    private var selectedRole = PlayerRole.NONE
    private var isReady = false
    private var opponentReady = false
    private var statusText = "Select your role"

    override fun show() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        font = BitmapFont()
        font.data.setScale(1.5f)
        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.setToOrtho(false)
        rebuildUI()
    }

    private fun rebuildUI() {
        buttons.clear()
        val cx = Gdx.graphics.width / 2f
        val bw = 250f
        val bh = 50f

        buttons.add(MenuScreen.ButtonDef("PLAY AS SHIP", Rectangle(cx - bw/2, 400f, bw, bh)) {
            selectedRole = PlayerRole.SHIP
            game.playerRole = PlayerRole.SHIP
            game.networkManager.sendPacket(RoleSelectPacket().also { it.role = PlayerRole.SHIP })
            statusText = "Role: SHIP"
        })
        buttons.add(MenuScreen.ButtonDef("PLAY AS MONSTER", Rectangle(cx - bw/2, 330f, bw, bh)) {
            selectedRole = PlayerRole.MONSTER
            game.playerRole = PlayerRole.MONSTER
            game.networkManager.sendPacket(RoleSelectPacket().also { it.role = PlayerRole.MONSTER })
            statusText = "Role: MONSTER"
        })

        val readyText = if (isReady) "UNREADY" else "READY"
        buttons.add(MenuScreen.ButtonDef(readyText, Rectangle(cx - bw/2, 260f, bw, bh)) {
            isReady = !isReady
            game.networkManager.sendPacket(ReadyPacket().also { it.ready = isReady })
            statusText = if (isReady) "Ready!" else "Not Ready"
            rebuildUI()
        })

        if (game.networkManager.isHosting()) {
            buttons.add(MenuScreen.ButtonDef("START GAME", Rectangle(cx - bw/2, 190f, bw, bh)) {
                game.networkManager.sendPacket(StartGamePacket())
                game.gameState = GameState.PLAYING
                game.setScreen(GameScreen3D(game))
            })
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { btn ->
            shape.setColor(0.15f, 0.4f, 0.8f, 0.9f)
            shape.rect(btn.rect.x, btn.rect.y, btn.rect.width, btn.rect.height)
        }
        shape.end()

        batch.begin()
        font.data.setScale(2f)
        font.draw(batch, "LOBBY", Gdx.graphics.width / 2f - 50f, Gdx.graphics.height - 60f)
        font.data.setScale(1.2f)

        val host = if (game.networkManager.isHosting()) "HOST" else "CLIENT"
        font.draw(batch, "Role: ${selectedRole.name} [$host]",
            Gdx.graphics.width / 2f - 100f, Gdx.graphics.height - 100f)
        font.draw(batch, statusText, Gdx.graphics.width / 2f - 80f, 140f)

        font.data.setScale(1.5f)
        buttons.forEach { btn ->
            font.draw(batch, btn.text, btn.rect.x + 20f, btn.rect.y + 35f)
        }
        batch.end()

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = Gdx.graphics.height - Gdx.input.y.toFloat()
            buttons.forEach { if (it.rect.contains(mx, my)) it.action() }
        }
    }

    override fun onNetworkPacket(packet: Packet) {
        when (packet) {
            is StartGamePacket -> {
                game.gameState = GameState.PLAYING
                game.setScreen(GameScreen3D(game))
            }
            is ReadyPacket -> {
                opponentReady = packet.ready
                statusText = if (opponentReady) "Opponent is ready" else "Waiting for opponent"
            }
        }
    }

    override fun getGame() = game
    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); shape.dispose(); font.dispose() }
}
