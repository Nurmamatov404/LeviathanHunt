package com.leviathan.game.boardgames

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.leviathan.game.LeviathanGame
import com.leviathan.game.screens.GameScreen
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class BGScreen { MAIN, VS_BOT, CREATE_GAME, JOIN_GAME, GAME_LIST }

data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float,
    var cr: Float, var cg: Float, var cb: Float,
    var life: Float, var maxLife: Float
)

data class Star(
    var x: Float, var y: Float,
    var size: Float, var speed: Float,
    var phase: Float
)

class BoardGameMenuScreen(private val game: LeviathanGame) : GameScreen {
    private lateinit var batch: SpriteBatch
    private lateinit var whiteTex: Texture
    private lateinit var font: BitmapFont
    private lateinit var titleFont: BitmapFont
    private lateinit var cam: OrthographicCamera

    private var currentScreen = BGScreen.MAIN
    private var selectedGame = BoardGameType.CHECKERS
    private var playerName get() = game.playerName
        set(v) { game.playerName = v }
    private var typingName = false
    private var statusMsg = ""
    private var statusTimer = 0f
    private var foundGames = mutableListOf<BoardGameLobby>()
    private var bgNetwork = BoardGameNetwork()
    private var scanning = false

    private var sw = 0f
    private var sh = 0f
    private var time = 0f
    private val layout = GlyphLayout()

    private fun fw(text: CharSequence): Float { layout.setText(font, text); return layout.width }
    private fun tfw(text: CharSequence): Float { layout.setText(titleFont, text); return layout.width }

    private val stars = mutableListOf<Star>()
    private val particles = mutableListOf<Particle>()
    private var particleTimer = 0f
    private var buttonGlowPhase = 0f

    private var isWaitingForOpponent = false
    private var gameStarted = false

    private val c1 = Color(0.08f, 0.0f, 0.18f, 1f)
    private val c2 = Color(0.02f, 0.02f, 0.08f, 1f)
    private val c3 = Color(0.15f, 0.0f, 0.25f, 1f)
    private val gold = Color(1f, 0.78f, 0.2f, 1f)
    private val goldDim = Color(0.6f, 0.45f, 0.1f, 1f)
    private val cyan = Color(0.2f, 0.8f, 1f, 1f)
    private val green = Color(0.3f, 0.9f, 0.4f, 1f)
    private val purple = Color(0.6f, 0.3f, 0.9f, 1f)
    private val red = Color(0.9f, 0.2f, 0.2f, 1f)
    private val darkPanel = Color(0.03f, 0.03f, 0.08f, 0.8f)

    override fun show() {
        sw = Gdx.graphics.width.toFloat()
        sh = Gdx.graphics.height.toFloat()
        batch = SpriteBatch()
        val pix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pix.setColor(1f, 1f, 1f, 1f)
        pix.fill()
        whiteTex = Texture(pix)
        pix.dispose()
        font = BitmapFont()
        titleFont = BitmapFont()
        cam = OrthographicCamera(sw, sh)
        cam.setToOrtho(false)
        Gdx.input.inputProcessor = null

        initStars()
        bgNetwork = BoardGameNetwork()
        bgNetwork.onConnected = { statusMsg = "Connected!" }
        bgNetwork.onDisconnected = { reason -> statusMsg = reason }
    }

    private fun initStars() {
        stars.clear()
        repeat(60) {
            stars.add(Star(
                Random.nextFloat() * sw,
                Random.nextFloat() * sh,
                Random.nextFloat() * 2f + 0.5f,
                Random.nextFloat() * 0.3f + 0.1f,
                Random.nextFloat() * 6.28f
            ))
        }
    }

    private fun spawnParticles(x: Float, y: Float, count: Int, color: Color) {
        repeat(count) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 80f + 30f
            val life = Random.nextFloat() * 0.8f + 0.4f
            particles.add(Particle(
                x, y,
                cos(angle) * speed, sin(angle) * speed,
                Random.nextFloat() * 3f + 1f,
                color.r, color.g, color.b,
                life, life
            ))
        }
    }

    override fun render(delta: Float) {
        time += delta
        sw = Gdx.graphics.width.toFloat()
        sh = Gdx.graphics.height.toFloat()
        cam.setToOrtho(false)
        cam.update()
        batch.projectionMatrix = cam.combined

        buttonGlowPhase += delta * 2f
        statusTimer -= delta
        if (statusTimer <= 0 && statusMsg.isNotEmpty() && currentScreen == BGScreen.MAIN) statusMsg = ""

        updateParticles(delta)
        drawBackground()
        batch.begin()
        when (currentScreen) {
            BGScreen.MAIN -> drawMainMenu()
            BGScreen.VS_BOT -> drawGameSelection("PLAY VS AI")
            BGScreen.CREATE_GAME -> drawGameSelection("CREATE GAME")
            BGScreen.JOIN_GAME -> drawJoinGame()
            BGScreen.GAME_LIST -> drawGameList()
        }
        batch.end()
        handleInput()
        buttons.clear()
    }

    private fun updateParticles(delta: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * delta
            p.y += p.vy * delta
            p.life -= delta
            if (p.life <= 0) iter.remove()
        }
    }

    private fun drawBackground() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.begin()
        val steps = 40
        val barH = sh / steps
        for (i in 0 until steps) {
            val t = i.toFloat() / steps
            val pulse = sin(time * 0.3f + t * 2f) * 0.03f
            batch.setColor(
                (c1.r + (c2.r - c1.r) * t + pulse).coerceIn(0f, 1f),
                (c1.g + (c2.g - c1.g) * t + pulse * 0.5f).coerceIn(0f, 1f),
                (c1.b + (c2.b - c1.b) * t + pulse * 2f).coerceIn(0f, 1f),
                1f
            )
            batch.draw(whiteTex, 0f, i * barH, sw, barH + 2)
        }

        val gradSteps = 15
        val gradH = sh * 0.12f
        for (i in 0 until gradSteps) {
            val t = i.toFloat() / gradSteps
            val alpha = (1f - t) * 0.15f
            batch.setColor(c3.r, c3.g, c3.b, alpha)
            batch.draw(whiteTex, 0f, sh - gradH + i * (gradH / gradSteps), sw, gradH / gradSteps + 1)
        }

        for (s in stars) {
            s.y -= s.speed * 60f * Gdx.graphics.deltaTime
            s.phase += Gdx.graphics.deltaTime * 2f
            if (s.y < -5f) { s.y = sh + 5f; s.x = Random.nextFloat() * sw }
            val alpha = (sin(s.phase) * 0.3f + 0.7f).coerceIn(0.2f, 1f)
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(whiteTex, s.x, s.y, s.size, s.size)
        }

        for (p in particles) {
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            batch.setColor(p.cr, p.cg, p.cb, alpha * 0.8f)
            batch.draw(whiteTex, p.x - p.size / 2, p.y - p.size / 2, p.size, p.size)
        }
        batch.end()
    }

    private fun drawTitle(text: String, x: Float, y: Float) {
        val scale = sh / 220f
        titleFont.data.setScale(scale)
        val w = tfw(text)

        titleFont.setColor(0f, 0f, 0f, 0.3f)
        titleFont.draw(batch, text, x - w / 2 + 3f, y - 3f)

        val glow = sin(time * 2f) * 0.15f + 0.85f
        titleFont.setColor(gold.r * glow, gold.g * glow, gold.b * glow, 1f)
        titleFont.draw(batch, text, x - w / 2 + 1f, y - 1f)

        titleFont.setColor(gold.r, gold.g, gold.b, 0.6f)
        titleFont.draw(batch, text, x - w / 2, y)
    }

    private fun drawSubTitle(text: String, x: Float, y: Float) {
        font.data.setScale(sh / 500f)
        font.setColor(0.7f, 0.7f, 0.9f, 0.6f)
        val w = fw(text)
        font.draw(batch, text, x - w / 2, y)
    }

    private fun drawMainMenu() {
        val cx = sw / 2f
        val bw = (sw * 0.6f).coerceIn(200f, 340f)
        val bh = (sh * 0.085f).coerceIn(48f, 70f)
        val gap = bh * 1.6f
        val startY = sh * 0.62f

        drawTitle("BOARD GAMES", cx, sh - 50f)
        drawSubTitle("Classic Strategy Games", cx, sh - 85f)

        drawGlowButton("PLAY VS AI", cx - bw / 2, startY, bw, bh, gold, cyan) {
            currentScreen = BGScreen.VS_BOT; spawnParticles(cx, startY + bh / 2, 15, gold)
        }
        drawGlowButton("CREATE GAME", cx - bw / 2, startY - gap, bw, bh, cyan, gold) {
            if (playerName.isBlank()) { flashStatus("Enter your name!"); return@drawGlowButton }
            currentScreen = BGScreen.CREATE_GAME
        }
        drawGlowButton("JOIN GAME", cx - bw / 2, startY - gap * 2, bw, bh, green, cyan) {
            if (playerName.isBlank()) { flashStatus("Enter your name!"); return@drawGlowButton }
            scanForGames()
        }
        drawGlowButton("STATISTICS", cx - bw / 2, startY - gap * 3, bw, bh, purple, gold) {
            currentScreen = BGScreen.GAME_LIST
        }
        drawGlowButton("EXIT", cx - bw / 2, startY - gap * 4, bw, bh, red, Color(1f, 0.5f, 0.5f, 1f)) {
            Gdx.app.exit()
        }

        drawNameInput()

        if (statusMsg.isNotEmpty()) {
            font.data.setScale(bh / 32f)
            font.setColor(gold.r, gold.g, gold.b, (sin(time * 3f) * 0.2f + 0.8f))
            font.draw(batch, statusMsg, cx - fw(statusMsg) / 2, sh * 0.12f)
        }
    }

    private fun drawNameInput() {
        val bw = (sw * 0.5f).coerceIn(200f, 300f)
        val bh = (sh * 0.06f).coerceIn(38f, 50f)
        val cx = sw / 2f
        val iy = sh * 0.2f

        font.data.setScale(bh / 30f)
        font.setColor(gold.r * 0.7f, gold.g * 0.7f, gold.b * 0.7f, 0.8f)
        font.draw(batch, "PLAYER NAME", cx - fw("PLAYER NAME") / 2, iy + bh + 18f)

        batch.setColor(0f, 0f, 0f, 0.4f)
        batch.draw(whiteTex, cx - bw / 2 + 4f, iy - 4f, bw, bh)

        batch.setColor(darkPanel)
        batch.draw(whiteTex, cx - bw / 2, iy, bw, bh)

        val glowActive = if (typingName) (sin(time * 4f) * 0.3f + 0.7f) else 0.3f
        batch.setColor(gold.r * glowActive, gold.g * glowActive, gold.b * glowActive, glowActive)
        batch.draw(whiteTex, cx - bw / 2, iy, bw, 2f)
        batch.draw(whiteTex, cx - bw / 2, iy + bh - 2f, bw, 2f)
        batch.draw(whiteTex, cx - bw / 2, iy, 2f, bh)
        batch.draw(whiteTex, cx + bw / 2 - 2f, iy, 2f, bh)

        font.data.setScale(bh / 26f)
        val cursor = if (typingName && (System.currentTimeMillis() / 400) % 2 == 0L) "█" else ""
        val display = if (playerName.isEmpty() && !typingName) "Enter your name" else playerName + cursor
        if (playerName.isEmpty() && !typingName) font.setColor(0.4f, 0.4f, 0.4f, 0.6f)
        else font.setColor(1f, 1f, 1f, 1f)
        font.draw(batch, display, cx - bw / 2 + 14f, iy + bh * 0.65f)
    }

    private fun drawGameSelection(title: String) {
        val cx = sw / 2f
        val bw = (sw * 0.6f).coerceIn(200f, 340f)
        val bh = (sh * 0.085f).coerceIn(48f, 70f)
        val gap = bh * 1.6f
        val startY = sh * 0.55f

        if (isWaitingForOpponent && !gameStarted) {
            drawTitle("WAITING FOR PLAYER...", cx, sh - 50f)
            font.data.setScale(bh / 28f)
            font.setColor(0.7f, 0.7f, 0.9f, 0.7f)
            val dotCount = ((time * 2f).toInt() % 4)
            val dots = ".".repeat(dotCount)
            font.draw(batch, "Enable hotspot. Waiting$dots", cx - fw("Enable hotspot. Waiting...") / 2, sh * 0.5f)
            font.draw(batch, "Game: ${selectedGame.name}", cx - 60f, sh * 0.43f)

            val pulse = sin(time * 3f) * 0.3f + 0.7f
            batch.setColor(cyan.r * pulse, cyan.g * pulse, cyan.b * pulse, 0.15f)
            batch.draw(whiteTex, cx - 60f, sh * 0.38f, 120f, 3f)

            drawGlowButton("CANCEL", cx - bw / 2, sh * 0.18f, bw, bh, red, Color(1f, 0.5f, 0.5f, 1f)) {
                bgNetwork.disconnect(); isWaitingForOpponent = false; currentScreen = BGScreen.MAIN
            }
            return
        }

        drawTitle(title, cx, sh - 50f)
        drawSubTitle("Select a game", cx, sh - 85f)

        val checkersCol = if (selectedGame == BoardGameType.CHECKERS) gold else Color(0.3f, 0.3f, 0.45f, 0.7f)
        drawGlowButton("✦ CHECKERS", cx - bw / 2, startY, bw, bh, checkersCol, gold) {
            selectedGame = BoardGameType.CHECKERS; spawnParticles(cx, startY + bh / 2, 8, gold)
        }
        val chessCol = if (selectedGame == BoardGameType.CHESS) cyan else Color(0.3f, 0.3f, 0.45f, 0.7f)
        drawGlowButton("♚ CHESS", cx - bw / 2, startY - gap, bw, bh, chessCol, cyan) {
            selectedGame = BoardGameType.CHESS; spawnParticles(cx, startY - gap + bh / 2, 8, cyan)
        }

        if (currentScreen == BGScreen.VS_BOT) {
            drawGlowButton("▶ START GAME", cx - bw / 2, startY - gap * 2.5f, bw, bh, green, gold) {
                startVsAI(); spawnParticles(cx, startY - gap * 2.5f + bh / 2, 20, green)
            }
        } else {
            drawGlowButton("▶ START HOSTING", cx - bw / 2, startY - gap * 2.5f, bw, bh, green, cyan) {
                startHosting(); spawnParticles(cx, startY - gap * 2.5f + bh / 2, 20, green)
            }
        }

        drawGlowButton("✕ CANCEL", cx - bw / 2, startY - gap * 3.5f, bw, bh, red, Color(1f, 0.5f, 0.5f, 1f)) {
            isWaitingForOpponent = false; bgNetwork.disconnect(); currentScreen = BGScreen.MAIN
        }
    }

    private fun drawJoinGame() {
        val cx = sw / 2f
        val bw = (sw * 0.65f).coerceIn(220f, 360f)
        val bh = (sh * 0.08f).coerceIn(45f, 65f)
        val gap = bh * 1.5f

        drawTitle("JOIN GAME", cx, sh - 50f)

        if (scanning) {
            val angle = time * 3f
            font.data.setScale(bh / 28f)
            font.setColor(cyan.r, cyan.g, cyan.b, (sin(time * 4f) * 0.2f + 0.8f))
            font.draw(batch, "Scanning network...", cx - 100f, sh * 0.55f)

            batch.setColor(cyan.r, cyan.g, cyan.b, 0.3f)
            val rx = cx + cos(angle) * 40f
            val ry = sh * 0.48f + sin(angle) * 40f
            batch.draw(whiteTex, rx - 4f, ry - 4f, 8f, 8f)
            batch.draw(whiteTex, cx - 4f, sh * 0.48f - 4f, 8f, 8f)
        } else if (foundGames.isEmpty()) {
            font.data.setScale(bh / 28f)
            font.setColor(0.5f, 0.5f, 0.6f, 0.7f)
            font.draw(batch, "No games found", cx - 80f, sh * 0.58f)
            drawGlowButton("⟳ SCAN AGAIN", cx - bw / 2, sh * 0.45f, bw, bh, cyan, gold) { scanForGames() }
        } else {
            font.data.setScale(bh / 32f)
            font.setColor(gold.r * 0.8f, gold.g * 0.8f, gold.b * 0.8f, 0.8f)
            font.draw(batch, "Available Games:", cx - 90f, sh * 0.78f)

            var yPos = sh * 0.72f - bh
            foundGames.take(4).forEach { lobby ->
                val label = "${lobby.hostName} — ${lobby.gameType.name}"
                val col = if (lobby.gameType == BoardGameType.CHECKERS) gold else cyan
                drawGlowButton(label, cx - bw / 2, yPos, bw, bh, col, Color(1f, 1f, 1f, 0.8f)) {
                    joinGame(lobby); spawnParticles(cx, yPos + bh / 2, 15, col)
                }
                yPos -= gap * 0.8f
            }
        }

        drawGlowButton("✕ BACK", cx - bw / 2, sh * 0.12f, bw, bh, red, Color(1f, 0.5f, 0.5f, 1f)) {
            bgNetwork.disconnect(); currentScreen = BGScreen.MAIN
        }
    }

    private fun drawGameList() {
        val cx = sw / 2f
        val bw = (sw * 0.7f).coerceIn(250f, 420f)
        val bh = (sh * 0.07f).coerceIn(40f, 60f)
        val gap = bh * 1.4f

        drawTitle("STATISTICS", cx, sh - 50f)

        val cs = PlayerStats.getCheckersStats()
        val chs = PlayerStats.getChessStats()
        val sy = sh * 0.7f

        font.data.setScale(bh / 26f)
        font.setColor(gold.r, gold.g, gold.b, 1f)
        font.draw(batch, "✦ CHECKERS", cx - fw("✦ CHECKERS") / 2, sy)
        font.data.setScale(bh / 30f)
        font.setColor(0.9f, 0.9f, 0.9f, 0.9f)
        val cStats = "Wins: ${cs.wins}   Losses: ${cs.losses}   Draws: ${cs.draws}"
        font.draw(batch, cStats, cx - fw(cStats) / 2, sy - gap * 0.6f)

        font.data.setScale(bh / 26f)
        font.setColor(cyan.r, cyan.g, cyan.b, 1f)
        font.draw(batch, "♚ CHESS", cx - fw("♚ CHESS") / 2, sy - gap * 1.4f)
        font.data.setScale(bh / 30f)
        font.setColor(0.9f, 0.9f, 0.9f, 0.9f)
        val chStats = "Wins: ${chs.wins}   Losses: ${chs.losses}   Draws: ${chs.draws}"
        font.draw(batch, chStats, cx - fw(chStats) / 2, sy - gap * 2f)

        font.data.setScale(bh / 32f)
        font.setColor(0.4f, 0.4f, 0.5f, 0.7f)
        val total = "Total games played: ${cs.total + chs.total}"
        font.draw(batch, total, cx - fw(total) / 2, sy - gap * 3f)

        drawGlowButton("✕ BACK", cx - bw / 2, sh * 0.12f, bw, bh, red, Color(1f, 0.5f, 0.5f, 1f)) {
            currentScreen = BGScreen.MAIN
        }
    }

    private fun drawGlowButton(text: String, x: Float, y: Float, w: Float, h: Float, color: Color, glowColor: Color, action: () -> Unit) {
        val glow = sin(buttonGlowPhase) * 0.15f + 0.85f

        batch.setColor(color.r * 0.2f, color.g * 0.2f, color.b * 0.2f, 0.4f)
        batch.draw(whiteTex, x + 4f, y - 4f, w, h)

        batch.setColor(color.r * 0.15f, color.g * 0.15f, color.b * 0.15f, 0.3f)
        batch.draw(whiteTex, x - 2f, y - 2f, w + 4f, h + 4f)

        batch.setColor(color.r * glow, color.g * glow, color.b * glow, 0.9f)
        batch.draw(whiteTex, x + 2f, y + 2f, w - 4f, h - 4f)

        batch.setColor(1f, 1f, 1f, 0.08f)
        batch.draw(whiteTex, x + 2f, y + h / 2f, w - 4f, h / 2f - 2f)

        font.data.setScale(h / 28f)
        font.setColor(1f, 1f, 1f, 1f)
        val tw = fw(text)
        font.draw(batch, text, x + w / 2 - tw / 2, y + h * 0.62f)

        buttons.add(ButtonDef(text, Rectangle(x, y, w, h), action))
    }

    private val buttons = mutableListOf<ButtonDef>()

    private fun flashStatus(msg: String) {
        statusMsg = msg; statusTimer = 2f
    }

    private fun scanForGames() {
        scanning = true; statusMsg = "Scanning..."
        foundGames.clear(); currentScreen = BGScreen.JOIN_GAME
        Thread {
            val games = bgNetwork.discoverGames()
            Gdx.app.postRunnable {
                foundGames.addAll(games); scanning = false
                flashStatus(if (games.isEmpty()) "No games found" else "Found ${games.size} game(s)")
            }
        }.start()
    }

    private fun startVsAI() {
        if (playerName.isBlank()) { flashStatus("Enter your name!"); return }
        game.setScreen(
            if (selectedGame == BoardGameType.CHECKERS) CheckersScreen(game, playerName, "AI", true, true)
            else ChessScreen(game, playerName, "AI", true, true)
        )
    }

    private fun startHosting() {
        if (playerName.isBlank()) { flashStatus("Enter your name!"); return }
        isWaitingForOpponent = true; statusMsg = "Starting..."
        val hostGameType = selectedGame
        bgNetwork = BoardGameNetwork()
        bgNetwork.onPacketReceived = { packet ->
            Gdx.app.postRunnable {
                if (packet is BoardGamePacket.JoinRequest) {
                    bgNetwork.sendPacket(BoardGamePacket.JoinAccepted("white", packet.playerName))
                    game.setScreen(
                        if (hostGameType == BoardGameType.CHECKERS)
                            CheckersScreen(game, playerName, packet.playerName, false, true, bgNetwork)
                        else ChessScreen(game, playerName, packet.playerName, false, true, bgNetwork)
                    )
                }
            }
        }
        bgNetwork.onDisconnected = { reason -> Gdx.app.postRunnable { flashStatus("Disconnected: $reason") } }
        bgNetwork.startHost(playerName, hostGameType)
    }

    private fun joinGame(lobby: BoardGameLobby) {
        bgNetwork.connectToHost(lobby.ip)
        bgNetwork.onPacketReceived = { packet ->
            Gdx.app.postRunnable {
                if (packet is BoardGamePacket.JoinAccepted) {
                    game.setScreen(
                        if (lobby.gameType == BoardGameType.CHECKERS)
                            CheckersScreen(game, playerName, lobby.hostName, false, false, bgNetwork)
                        else ChessScreen(game, playerName, lobby.hostName, false, false, bgNetwork)
                    )
                }
            }
        }
        bgNetwork.sendPacket(BoardGamePacket.JoinRequest(playerName, lobby.gameType))
    }

    private fun handleInput() {
        val bw = (sw * 0.5f).coerceIn(200f, 300f)
        val bh = (sh * 0.06f).coerceIn(38f, 50f)

        if (currentScreen == BGScreen.MAIN) {
            val nameRect = Rectangle(sw / 2f - bw / 2, sh * 0.2f, bw, bh)
            if (Gdx.input.justTouched()) {
                val mx = Gdx.input.x.toFloat()
                val my = sh - Gdx.input.y.toFloat()
                val tapped = nameRect.contains(mx, my)
                if (tapped && !typingName) {
                    typingName = true
                    Gdx.input.getTextInput(object : com.badlogic.gdx.Input.TextInputListener {
                        override fun input(text: String) { playerName = text.take(15); typingName = false }
                        override fun canceled() { typingName = false }
                    }, "Enter your name", playerName, "")
                } else if (!tapped) {
                    typingName = false
                }
            }
        }

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = sh - Gdx.input.y.toFloat()
            buttons.toList().forEach { if (it.rect.contains(mx, my)) it.action() }
        }
    }

    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); whiteTex.dispose(); font.dispose(); titleFont.dispose() }
}
