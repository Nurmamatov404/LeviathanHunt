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

    private val sw get() = Gdx.graphics.width.toFloat()
    private val sh get() = Gdx.graphics.height.toFloat()

    override fun show() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        font = BitmapFont()
        cam = OrthographicCamera(sw, sh)
        cam.setToOrtho(false)
        Gdx.input.inputProcessor = null
        rebuildUI()
    }

    private fun rebuildUI() {
        buttons.clear()
        val cx = sw / 2f
        val bw = (sw * 0.55f).coerceIn(200f, 300f)
        val bh = (sh * 0.08f).coerceIn(45f, 65f)
        val gap = bh * 1.5f
        val startY = sh * 0.65f

        buttons.add(MenuScreen.ButtonDef("PLAY AS SHIP", Rectangle(cx - bw/2, startY + 5, bw, bh)) {
            selectedRole = PlayerRole.SHIP; game.playerRole = PlayerRole.SHIP
            game.networkManager.sendPacket(RoleSelectPacket().also { it.role = PlayerRole.SHIP })
            statusText = "Role: SHIP"; rebuildUI()
        })
        buttons.add(MenuScreen.ButtonDef("PLAY AS MONSTER", Rectangle(cx - bw/2, startY - gap + 5, bw, bh)) {
            selectedRole = PlayerRole.MONSTER; game.playerRole = PlayerRole.MONSTER
            game.networkManager.sendPacket(RoleSelectPacket().also { it.role = PlayerRole.MONSTER })
            statusText = "Role: MONSTER"; rebuildUI()
        })

        val readyText = if (isReady) "UNREADY" else "READY"
        buttons.add(MenuScreen.ButtonDef(readyText, Rectangle(cx - bw/2, startY - gap * 2 + 5, bw, bh)) {
            isReady = !isReady
            game.networkManager.sendPacket(ReadyPacket().also { it.ready = isReady })
            statusText = if (isReady) "Ready!" else "Not Ready"; rebuildUI()
        })

        if (game.networkManager.isHosting()) {
            buttons.add(MenuScreen.ButtonDef("START GAME", Rectangle(cx - bw/2, startY - gap * 3 + 5, bw, bh)) {
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

        val bh = (sh * 0.08f).coerceIn(45f, 65f)

        shape.begin(ShapeRenderer.ShapeType.Filled)

        // Role indicators
        val shipSel = (selectedRole == PlayerRole.SHIP) to Color(0.2f, 0.6f, 0.2f, 0.9f)
        val monSel = (selectedRole == PlayerRole.MONSTER) to Color(0.8f, 0.2f, 0.2f, 0.9f)

        (buttons + listOf(
            menuBtnDef("ROLE: $selectedRole", Rectangle(sw/4f, sh - 120f, sw/2f, bh/1.5f)) {} // dummy for display
        )).forEach { btn ->
            val isShipRole = btn.text.contains("SHIP")
            val isMonRole = btn.text.contains("MONSTER")
            val isRoleBtn = isShipRole || isMonRole
            val isSelected = (isShipRole && selectedRole == PlayerRole.SHIP) ||
                            (isMonRole && selectedRole == PlayerRole.MONSTER)
            if (isRoleBtn && isSelected)
                shape.setColor(0.3f, 0.7f, 0.3f, 0.9f)
            else if (isRoleBtn)
                shape.setColor(0.3f, 0.3f, 0.3f, 0.7f)
            else
                shape.setColor(0.15f, 0.4f, 0.8f, 0.9f)
            shape.rect(btn.rect.x, btn.rect.y, btn.rect.width, btn.rect.height)
        }
        shape.end()

        batch.begin()
        font.data.setScale(sh / 300f)
        font.draw(batch, "LOBBY", sw / 2f - 60f, sh - 40f)
        font.data.setScale(bh / 30f)

        val host = if (game.networkManager.isHosting()) "HOST" else "CLIENT"
        font.draw(batch, "Mode: $host", 20f, sh - 40f)
        font.draw(batch, statusText, sw / 2f - 80f, sh * 0.15f)

        buttons.forEach { btn ->
            font.draw(batch, btn.text, btn.rect.x + btn.rect.width * 0.1f, btn.rect.y + btn.rect.height * 0.65f)
        }
        batch.end()

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = sh - Gdx.input.y.toFloat()
            buttons.forEach { if (it.rect.contains(mx, my)) it.action() }
        }
    }

    private fun menuBtnDef(text: String, rect: Rectangle, action: () -> Unit) =
        MenuScreen.ButtonDef(text, rect, action)

    override fun onNetworkPacket(packet: Packet) {
        when (packet) {
            is StartGamePacket -> { game.gameState = GameState.PLAYING; game.setScreen(GameScreen3D(game)) }
            is ReadyPacket -> { opponentReady = packet.ready; statusText = if (opponentReady) "Opponent ready" else "Waiting for opponent" }
            else -> {}
        }
    }

    override fun getGame() = game
    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); shape.dispose(); font.dispose() }
}
