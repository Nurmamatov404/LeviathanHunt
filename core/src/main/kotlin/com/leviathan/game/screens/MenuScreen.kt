package com.leviathan.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.leviathan.game.LeviathanGame
import com.leviathan.game.network.*

class MenuScreen(private val game: LeviathanGame) : GameScreen {
    private lateinit var batch: SpriteBatch
    private lateinit var shape: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var cam: OrthographicCamera

    private val buttons = mutableListOf<ButtonDef>()
    private var showJoin = false
    private var ipAddress = "192.168.43.1"
    private var statusMsg = ""
    private var typing = false

    data class ButtonDef(val text: String, val rect: Rectangle, val action: () -> Unit)

    override fun show() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        font = BitmapFont()
        font.data.setScale(1.5f)
        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.setToOrtho(false)
        Gdx.input.inputProcessor = null
        rebuildButtons()
    }

    private fun rebuildButtons() {
        buttons.clear()
        val cx = Gdx.graphics.width / 2f
        val bw = 250f
        val bh = 50f

        if (!showJoin) {
            buttons.add(ButtonDef("CREATE GAME", Rectangle(cx - bw/2, 380f, bw, bh)) {
                if (game.networkManager.startHost()) {
                    statusMsg = "Hosting... Waiting for player"
                    game.setScreen(LobbyScreen(game))
                } else statusMsg = "Failed to start host!"
            })
            buttons.add(ButtonDef("JOIN GAME", Rectangle(cx - bw/2, 310f, bw, bh)) {
                showJoin = true; rebuildButtons()
            })
            buttons.add(ButtonDef("EXIT", Rectangle(cx - bw/2, 240f, bw, bh)) {
                Gdx.app.exit()
            })
        } else {
            buttons.add(ButtonDef("CONNECT", Rectangle(cx - bw/2, 310f, bw, bh)) {
                if (ipAddress.isBlank()) { statusMsg = "Enter IP"; return@ButtonDef }
                statusMsg = "Connecting to $ipAddress..."
                if (game.networkManager.connectToHost(ipAddress)) {
                    game.setScreen(LobbyScreen(game))
                } else statusMsg = "Connection failed!"
            })
            buttons.add(ButtonDef("BACK", Rectangle(cx - bw/2, 240f, bw, bh)) {
                showJoin = false; statusMsg = ""; rebuildButtons()
            })
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        shape.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { btn ->
            shape.setColor(0.15f, 0.4f, 0.8f, 0.9f)
            shape.rect(btn.rect.x, btn.rect.y, btn.rect.width, btn.rect.height)
        }
        shape.end()

        batch.begin()
        // Title
        font.data.setScale(2.5f)
        font.draw(batch, "LEVIATHAN HUNT", Gdx.graphics.width / 2f - 140f, Gdx.graphics.height - 60f)
        font.data.setScale(1f)
        font.draw(batch, "Asymmetric Multiplayer Survival", Gdx.graphics.width / 2f - 120f, Gdx.graphics.height - 90f)

        font.data.setScale(1.5f)
        buttons.forEach { btn ->
            font.draw(batch, btn.text, btn.rect.x + 20f, btn.rect.y + 35f)
        }

        if (showJoin) {
            font.data.setScale(1.2f)
            font.draw(batch, "Host IP:", Gdx.graphics.width / 2f - 180f, 450f)
            shape.begin(ShapeRenderer.ShapeType.Line)
            shape.setColor(1f, 1f, 1f, 1f)
            shape.rect(Gdx.graphics.width / 2f - 150f, 400f, 300f, 40f)
            shape.end()
            font.draw(batch, ipAddress + if (typing) "|" else "",
                Gdx.graphics.width / 2f - 140f, 430f)
        }

        if (statusMsg.isNotEmpty()) {
            font.data.setScale(1f)
            font.draw(batch, statusMsg, Gdx.graphics.width / 2f - 100f, 150f)
        }
        batch.end()
    }

    private fun handleInput() {
        if (showJoin) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && ipAddress.isNotEmpty())
                ipAddress = ipAddress.dropLast(1)
            for (c in '0'..'9') {
                if (Gdx.input.isKeyJustPressed(Input.Keys.valueOf(c.toString())) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.valueOf("NUMPAD_$c"))) {
                    if (ipAddress.length < 15) ipAddress += c
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD) && ipAddress.length < 15) ipAddress += "."
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                rebuildButtons()
                buttons.firstOrNull()?.action?.invoke()
            }
        }

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
