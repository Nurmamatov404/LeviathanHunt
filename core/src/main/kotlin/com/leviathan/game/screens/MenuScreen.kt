package com.leviathan.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.leviathan.game.LeviathanGame
import com.leviathan.game.network.*

class MenuScreen(private val game: LeviathanGame) : GameScreen {
    private lateinit var batch: SpriteBatch
    private lateinit var whiteTex: Texture
    private lateinit var font: BitmapFont
    private lateinit var cam: OrthographicCamera

    private val buttons = mutableListOf<ButtonDef>()
    private var showJoin = false
    private var ipAddress = "192.168.43.1"
    private var statusMsg = ""
    private var typing = false
    private var keyboardOpen = false

    private val sw get() = Gdx.graphics.width.toFloat()
    private val sh get() = Gdx.graphics.height.toFloat()

    data class ButtonDef(val text: String, val rect: Rectangle, val action: () -> Unit)

    override fun show() {
        batch = SpriteBatch()
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.fill()
        whiteTex = Texture(pixmap)
        pixmap.dispose()
        font = BitmapFont()
        cam = OrthographicCamera(sw, sh)
        cam.setToOrtho(false)
        Gdx.input.inputProcessor = null
        rebuildButtons()
    }

    private fun rebuildButtons() {
        buttons.clear()
        val cx = sw / 2f
        val bw = (sw * 0.6f).coerceIn(200f, 350f)
        val bh = (sh * 0.08f).coerceIn(50f, 70f)
        val gap = bh * 1.4f
        val startY = sh * 0.5f

        if (!showJoin) {
            buttons.add(ButtonDef("CREATE GAME", Rectangle(cx - bw/2, startY + gap, bw, bh)) {
                if (game.networkManager.startHost()) {
                    statusMsg = "Hosting..."; game.setScreen(LobbyScreen(game))
                } else statusMsg = "Failed to start host!"
            })
            buttons.add(ButtonDef("JOIN GAME", Rectangle(cx - bw/2, startY, bw, bh)) {
                showJoin = true; rebuildButtons()
            })
            buttons.add(ButtonDef("EXIT", Rectangle(cx - bw/2, startY - gap, bw, bh)) {
                Gdx.app.exit()
            })
        } else {
            buttons.add(ButtonDef("CONNECT", Rectangle(cx - bw/2, startY, bw, bh)) {
                if (ipAddress.isBlank()) { statusMsg = "Enter IP"; return@ButtonDef }
                statusMsg = "Connecting..."; keyboardOpen = false
                if (game.networkManager.connectToHost(ipAddress)) game.setScreen(LobbyScreen(game))
                else statusMsg = "Connection failed!"
            })
            buttons.add(ButtonDef("BACK", Rectangle(cx - bw/2, startY - gap, bw, bh)) {
                showJoin = false; statusMsg = ""; keyboardOpen = false; rebuildButtons()
            })
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        batch.projectionMatrix = cam.combined

        val bw = (sw * 0.6f).coerceIn(200f, 350f)
        val bh = (sh * 0.08f).coerceIn(50f, 70f)

        batch.begin()

        buttons.forEach { btn ->
            batch.setColor(0.15f, 0.4f, 0.8f, 0.9f)
            batch.draw(whiteTex, btn.rect.x, btn.rect.y, btn.rect.width, btn.rect.height)
        }

        font.data.setScale(sh / 300f)
        font.draw(batch, "LEVIATHAN HUNT", sw / 2f - sw * 0.22f, sh - 40f)
        font.data.setScale(sh / 500f)
        font.draw(batch, "Asymmetric Multiplayer Survival", sw / 2f - sw * 0.2f, sh - 70f)

        font.data.setScale(bh / 30f)
        buttons.forEach { btn ->
            font.draw(batch, btn.text, btn.rect.x + btn.rect.width * 0.15f, btn.rect.y + btn.rect.height * 0.65f)
        }

        if (showJoin) {
            val inputY = sh * 0.78f
            val inputH = bh * 0.8f
            font.data.setScale(bh / 35f)
            font.draw(batch, "Host IP:", sw / 4f, inputY + 20f)
            batch.setColor(1f, 1f, 1f, 0.05f)
            batch.draw(whiteTex, sw / 4f, inputY - inputH, sw / 2f, inputH)
            batch.setColor(1f, 1f, 1f, 1f)
            val lx = sw / 4f
            val ly = inputY - inputH
            val lw = sw / 2f
            val lh = inputH
            val b = 2f
            batch.draw(whiteTex, lx, ly, lw, b)
            batch.draw(whiteTex, lx, ly + lh - b, lw, b)
            batch.draw(whiteTex, lx, ly, b, lh)
            batch.draw(whiteTex, lx + lw - b, ly, b, lh)
            batch.setColor(1f, 1f, 1f, 1f)
            val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            font.draw(batch, ipAddress + cursor, sw / 4f + 10f, inputY - 5f)
        }

        if (statusMsg.isNotEmpty()) {
            font.data.setScale(bh / 30f)
            font.draw(batch, statusMsg, sw / 2f - 100f, sh * 0.2f)
        }
        batch.end()
    }

    private fun handleInput() {
        if (showJoin) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && ipAddress.isNotEmpty())
                ipAddress = ipAddress.dropLast(1)
            for (c in '0'..'9') {
                val key = try { Input.Keys.valueOf(c.toString()) } catch (_: Exception) { -1 }
                if (key > 0 && Gdx.input.isKeyJustPressed(key)) {
                    if (ipAddress.length < 15) ipAddress += c
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD) && ipAddress.length < 15) ipAddress += "."
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                keyboardOpen = false
                buttons.firstOrNull { it.text == "CONNECT" }?.action?.invoke()
            }
        }

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = sh - Gdx.input.y.toFloat()

            if (showJoin) {
                val inputY = sh * 0.78f
                val inputH = (sh * 0.08f).coerceIn(50f, 70f) * 0.8f
                val rect = Rectangle(sw / 4f, inputY - inputH, sw / 2f, inputH)
                if (rect.contains(mx, my)) {
                    keyboardOpen = true
                    return
                }
            }

            buttons.forEach { if (it.rect.contains(mx, my)) it.action() }
        }
    }

    override fun onNetworkPacket(packet: Packet) {}
    override fun getGame() = game
    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); whiteTex.dispose(); font.dispose() }
}
