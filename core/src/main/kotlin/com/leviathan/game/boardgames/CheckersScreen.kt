package com.leviathan.game.boardgames

import com.badlogic.gdx.Gdx
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

class CheckersScreen(
    private val game: LeviathanGame,
    private val playerName: String,
    private val opponentName: String,
    private val isVsAI: Boolean,
    private val isWhite: Boolean,
    private val network: BoardGameNetwork? = null
) : GameScreen {
    private lateinit var batch: SpriteBatch
    private lateinit var whiteTex: Texture
    private lateinit var font: BitmapFont
    private lateinit var cam: OrthographicCamera
    private val layout = GlyphLayout()

    private fun fw(text: CharSequence): Float { layout.setText(font, text); return layout.width }

    private val checkersGame = CheckersGame()
    private var state = CheckersGame.newState()
    private var selectedRow = -1
    private var selectedCol = -1
    private var validMoves = listOf<CheckersMove>()
    private var message = ""
    private var gameResult = ""
    private var showRematch = false
    private var aiThinking = false
    private var lastMove: CheckersMove? = null
    private var playerColor = if (isWhite) CheckersPiece.RED else CheckersPiece.BLACK
    private var circleTex: Texture? = null
    private var time = 0f

    private var sw = 0f
    private var sh = 0f

    private val woodLight = Color(0.94f, 0.82f, 0.65f, 1f)
    private val woodDark = Color(0.54f, 0.27f, 0.12f, 1f)
    private val woodBorder = Color(0.35f, 0.18f, 0.08f, 1f)
    private val bgTop = Color(0.1f, 0.05f, 0.15f, 1f)
    private val bgBot = Color(0.03f, 0.02f, 0.06f, 1f)
    private val gold = Color(1f, 0.78f, 0.2f, 1f)
    private val panelBg = Color(0.04f, 0.04f, 0.08f, 0.85f)
    private val pieceRed1 = Color(0.85f, 0.12f, 0.12f, 1f)
    private val pieceRed2 = Color(0.55f, 0.05f, 0.05f, 1f)
    private val pieceBlack1 = Color(0.25f, 0.25f, 0.25f, 1f)
    private val pieceBlack2 = Color(0.08f, 0.08f, 0.08f, 1f)
    private val pieceRedShine = Color(1f, 0.5f, 0.5f, 0.4f)
    private val pieceBlackShine = Color(0.6f, 0.6f, 0.6f, 0.4f)

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
        cam = OrthographicCamera(sw, sh)
        cam.setToOrtho(false)
        Gdx.input.inputProcessor = null

        network?.onPacketReceived = { packet ->
            Gdx.app.postRunnable { handleNetworkPacket(packet) }
        }

        if (isVsAI && playerColor == CheckersPiece.BLACK) doAIMove()
    }

    override fun render(delta: Float) {
        time += delta
        sw = Gdx.graphics.width.toFloat()
        sh = Gdx.graphics.height.toFloat()
        cam.setToOrtho(false)
        cam.update()
        batch.projectionMatrix = cam.combined

        drawBackground()
        batch.begin()

        val boardSize = minOf(sw * 0.88f, sh * 0.7f)
        val boardX = (sw - boardSize) / 2f
        val boardY = (sh - boardSize) / 2f + 25f

        drawPanel(boardX, boardY, boardSize)
        drawBoard(boardX, boardY, boardSize)
        drawPieces(boardX, boardY, boardSize)
        drawOverlay()
        batch.end()
        handleInput(boardX, boardY, boardSize)
    }

    private fun drawBackground() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.begin()
        val steps = 30
        val barH = sh / steps
        for (i in 0 until steps) {
            val t = i.toFloat() / steps
            batch.setColor(
                bgTop.r + (bgBot.r - bgTop.r) * t,
                bgTop.g + (bgBot.g - bgTop.g) * t,
                bgTop.b + (bgBot.b - bgTop.b) * t, 1f
            )
            batch.draw(whiteTex, 0f, i * barH, sw, barH + 2)
        }

        batch.setColor(1f, 1f, 1f, 0.012f)
        for (i in 0 until 8) {
            val x = ((time * 20f + i * 150f) % (sw + 200f)) - 100f
            batch.draw(whiteTex, x, 0f, 1f, sh)
        }
        batch.end()
    }

    private fun drawPanel(boardX: Float, boardY: Float, boardSize: Float) {
        val ph = 52f
        batch.setColor(panelBg)
        batch.draw(whiteTex, 0f, sh - ph, sw, ph)

        batch.setColor(gold.r * 0.6f, gold.g * 0.6f, gold.b * 0.6f, 0.3f)
        batch.draw(whiteTex, 0f, sh - ph, sw, 1f)

        font.data.setScale(ph / 30f)
        font.setColor(gold)
        font.draw(batch, playerName, 20f, sh - ph * 0.3f)

        font.setColor(0.5f, 0.5f, 0.6f, 0.6f)
        font.data.setScale(ph / 34f)
        val vs = if (isVsAI) "vs AI" else "vs"
        font.draw(batch, vs, sw / 2f - 15f, sh - ph * 0.3f)

        font.setColor(0.7f, 0.7f, 0.8f, 1f)
        font.data.setScale(ph / 30f)
        font.draw(batch, opponentName, sw - 140f, sh - ph * 0.3f)

        val turnText = when {
            state.gameOver -> gameResult.ifEmpty { "Game Over" }
            state.currentPlayer == playerColor -> "Your Turn"
            else -> if (isVsAI) "AI Thinking..." else "$opponentName's Turn"
        }
        font.data.setScale(ph / 32f)
        val turnColor = when {
            state.gameOver -> gold
            state.currentPlayer == playerColor -> Color(0.3f, 1f, 0.4f, 1f)
            else -> Color(0.7f, 0.7f, 0.8f, 0.7f)
        }
        font.setColor(turnColor)
        font.draw(batch, turnText, sw / 2f - 70f, sh - ph * 0.72f)

        val rCount = state.board.sumOf { row -> row.count { CheckersGame.isRed(it) } }
        val bCount = state.board.sumOf { row -> row.count { CheckersGame.isBlack(it) } }
        font.data.setScale(ph / 36f)
        font.setColor(pieceRed1)
        font.draw(batch, "◉ $rCount", sw * 0.12f, sh - ph * 0.72f)
        font.setColor(pieceBlack1)
        font.draw(batch, "◉ $bCount", sw * 0.75f, sh - ph * 0.72f)
    }

    private fun drawBoard(boardX: Float, boardY: Float, boardSize: Float) {
        val cellSize = boardSize / 8f

        batch.setColor(0f, 0f, 0f, 0.35f)
        batch.draw(whiteTex, boardX + 5f, boardY - 5f, boardSize + 10f, boardSize + 10f)

        batch.setColor(woodBorder)
        batch.draw(whiteTex, boardX - 6f, boardY - 6f, boardSize + 12f, boardSize + 12f)

        val cornerSize = 12f
        batch.setColor(woodBorder.r * 0.7f, woodBorder.g * 0.7f, woodBorder.b * 0.7f, 1f)
        batch.draw(whiteTex, boardX - 8f, boardY - 8f, cornerSize, cornerSize)
        batch.draw(whiteTex, boardX + boardSize - 4f, boardY - 8f, cornerSize, cornerSize)
        batch.draw(whiteTex, boardX - 8f, boardY + boardSize - 4f, cornerSize, cornerSize)
        batch.draw(whiteTex, boardX + boardSize - 4f, boardY + boardSize - 4f, cornerSize, cornerSize)

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val isDark = (row + col) % 2 == 1
                val x = boardX + col * cellSize
                val y = boardY + (7 - row) * cellSize

                if (isDark) {
                    val shade = sin(time * 0.5f + row + col) * 0.03f + 1f
                    batch.setColor(woodDark.r * shade, woodDark.g * shade, woodDark.b * shade, 1f)
                } else {
                    val shade = sin(time * 0.5f + row + col + 2f) * 0.02f + 1f
                    batch.setColor(woodLight.r * shade, woodLight.g * shade, woodLight.b * shade, 1f)
                }
                batch.draw(whiteTex, x, y, cellSize, cellSize)
            }
        }

        if (selectedRow >= 0) {
            val sx = boardX + selectedCol * cellSize
            val sy = boardY + (7 - selectedRow) * cellSize
            val pulse = sin(time * 4f) * 0.3f + 0.7f
            batch.setColor(0.3f, 0.8f, 1f, pulse * 0.4f)
            batch.draw(whiteTex, sx - 2f, sy - 2f, cellSize + 4f, cellSize + 4f)
            batch.setColor(0.3f, 0.8f, 1f, pulse * 0.25f)
            batch.draw(whiteTex, sx - 4f, sy - 4f, cellSize + 8f, cellSize + 8f)
        }

        for (move in validMoves) {
            val x = boardX + move.toCol * cellSize
            val y = boardY + (7 - move.toRow) * cellSize
            val pulse = sin(time * 3f) * 0.2f + 0.8f
            if (state.board[move.toRow][move.toCol] != CheckersPiece.EMPTY) {
                batch.setColor(1f, 0.2f, 0.2f, pulse * 0.5f)
                batch.draw(whiteTex, x, y, cellSize, cellSize)
            } else {
                batch.setColor(0.3f, 0.9f, 0.3f, pulse * 0.5f)
                val dotSize = cellSize * 0.28f
                batch.draw(whiteTex, x + cellSize / 2 - dotSize / 2, y + cellSize / 2 - dotSize / 2, dotSize, dotSize)
            }
        }

        if (lastMove != null) {
            val lm = lastMove!!
            batch.setColor(1f, 1f, 0.3f, 0.15f)
            val fx = boardX + lm.fromCol * cellSize
            val fy = boardY + (7 - lm.fromRow) * cellSize
            batch.draw(whiteTex, fx, fy, cellSize, cellSize)
            val tx = boardX + lm.toCol * cellSize
            val ty = boardY + (7 - lm.toRow) * cellSize
            batch.draw(whiteTex, tx, ty, cellSize, cellSize)
        }
    }

    private fun ensureCircleTex(radius: Float) {
        if (circleTex == null) {
            val size = (radius * 2.8f).toInt().coerceAtLeast(20)
            val pix = Pixmap(size, size, Pixmap.Format.RGBA8888)
            pix.setColor(1f, 1f, 1f, 1f)
            pix.fillCircle(size / 2, size / 2, size / 2 - 1)
            circleTex = Texture(pix)
            pix.dispose()
        }
    }

    private fun drawPieces(boardX: Float, boardY: Float, boardSize: Float) {
        val cellSize = boardSize / 8f
        val radius = cellSize * 0.38f
        ensureCircleTex(radius)
        val tex = circleTex!!
        val ds = radius * 2.4f

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board[row][col]
                if (piece == CheckersPiece.EMPTY) continue
                val cx = boardX + col * cellSize + cellSize / 2f
                val cy = boardY + (7 - row) * cellSize + cellSize / 2f

                val isRed = CheckersGame.isRed(piece)
                val isKing = CheckersGame.isKing(piece)

                val floatOffset = sin(time * 1.5f + row * 2f + col * 1.3f) * 1.2f

                batch.setColor(0f, 0f, 0f, 0.25f)
                batch.draw(tex, cx - ds / 2 + 3f, cy - ds / 2 - 3f + floatOffset, ds, ds)

                val main = if (isRed) pieceRed1 else pieceBlack1
                val dark = if (isRed) pieceRed2 else pieceBlack2
                val shine = if (isRed) pieceRedShine else pieceBlackShine

                batch.setColor(dark)
                batch.draw(tex, cx - ds / 2, cy - ds / 2 + floatOffset, ds, ds)
                batch.setColor(main)
                batch.draw(tex, cx - ds / 2, cy - ds / 2 + ds * 0.06f + floatOffset, ds, ds * 0.9f)
                batch.setColor(shine)
                batch.draw(tex, cx - ds / 2 + ds * 0.12f, cy - ds / 2 + ds * 0.15f + floatOffset, ds * 0.4f, ds * 0.35f)

                if (isKing) {
                    val crownScale = ds * 0.55f / font.capHeight
                    font.data.setScale(crownScale)
                    val pulse = sin(time * 3f) * 0.15f + 0.85f
                    font.setColor(gold.r * pulse, gold.g * pulse, gold.b * pulse, 1f)
                    font.draw(batch, "★", cx - ds * 0.2f, cy + ds * 0.25f + floatOffset)
                    font.data.setScale(crownScale * 0.7f)
                    font.setColor(1f, 1f, 1f, 0.6f * pulse)
                    font.draw(batch, "★", cx - ds * 0.18f, cy + ds * 0.22f + floatOffset)
                }
            }
        }
    }

    private fun drawOverlay() {
        val bw = (sw * 0.45f).coerceIn(160f, 280f)
        val bh = (sh * 0.075f).coerceIn(42f, 58f)
        val gap = bh * 1.4f
        val cx = sw / 2f

        if (state.gameOver) {
            batch.setColor(0f, 0f, 0f, 0.65f)
            batch.draw(whiteTex, 0f, 0f, sw, sh)

            val win = state.winner == playerColor
            val resultText = when {
                win -> "✦ VICTORY! ✦"
                state.winner == null -> "─ DRAW ─"
                else -> "✕ DEFEAT ✕"
            }
            val resultColor = when {
                win -> Color(0.3f, 1f, 0.4f, 1f)
                state.winner == null -> gold
                else -> Color(1f, 0.3f, 0.3f, 1f)
            }

            font.data.setScale(sh / 140f)
            val glow = sin(time * 3f) * 0.15f + 0.85f
            font.setColor(resultColor.r * glow, resultColor.g * glow, resultColor.b * glow, 1f)
            font.draw(batch, resultText, cx - fw(resultText) / 2, sh * 0.6f)

            if (gameResult.isNotEmpty()) {
                font.data.setScale(sh / 300f)
                font.setColor(0.8f, 0.8f, 0.8f, 0.7f)
                font.draw(batch, gameResult, cx - 80f, sh * 0.53f)
            }

            val rCnt = state.board.sumOf { r -> r.count { CheckersGame.isRed(it) } }
            val bCnt = state.board.sumOf { r -> r.count { CheckersGame.isBlack(it) } }
            font.data.setScale(sh / 320f)
            font.setColor(0.6f, 0.6f, 0.7f, 0.7f)
            val statsText = "Remaining — Red: $rCnt  Black: $bCnt"
            font.draw(batch, statsText, cx - fw(statsText) / 2, sh * 0.47f)

            drawBtn("▶ PLAY AGAIN", cx - bw / 2, sh * 0.33f, bw, bh, Color(0.2f, 0.75f, 0.3f, 0.95f))
            drawBtn("← MAIN MENU", cx - bw / 2, sh * 0.33f - gap, bw, bh, Color(0.7f, 0.2f, 0.2f, 0.95f))
        }
    }

    private fun drawBtn(text: String, x: Float, y: Float, w: Float, h: Float, color: Color) {
        batch.setColor(0f, 0f, 0f, 0.3f)
        batch.draw(whiteTex, x + 3f, y - 3f, w, h)
        batch.setColor(color)
        batch.draw(whiteTex, x, y, w, h)
        batch.setColor(1f, 1f, 1f, 0.08f)
        batch.draw(whiteTex, x, y + h / 2f, w, h / 2f)
        font.data.setScale(h / 28f)
        font.setColor(1f, 1f, 1f, 1f)
        val tw = fw(text)
        font.draw(batch, text, x + w / 2 - tw / 2, y + h * 0.62f)

        val isPlayAgain = text.contains("PLAY AGAIN")
        buttons.add(ButtonDef(text, Rectangle(x, y, w, h), {
            if (isPlayAgain) rematch() else game.setScreen(BoardGameMenuScreen(game))
        }))
    }

    private val buttons = mutableListOf<ButtonDef>()

    private fun handleInput(boardX: Float, boardY: Float, boardSize: Float) {
        if (state.gameOver) {
            if (Gdx.input.justTouched()) {
                val mx = Gdx.input.x.toFloat()
                val my = sh - Gdx.input.y.toFloat()
                buttons.toList().forEach { if (it.rect.contains(mx, my)) it.action() }
            }
            buttons.clear()
            return
        }
        buttons.clear()
        if (state.currentPlayer != playerColor) return
        if (aiThinking) return

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x.toFloat()
            val my = sh - Gdx.input.y.toFloat()
            if (mx in boardX..(boardX + boardSize) && my in boardY..(boardY + boardSize)) {
                val cellSize = boardSize / 8f
                val col = ((mx - boardX) / cellSize).toInt()
                val row = 7 - ((my - boardY) / cellSize).toInt()
                if (row in 0 until 8 && col in 0 until 8) onCellClicked(row, col)
            }
        }
    }

    private fun onCellClicked(row: Int, col: Int) {
        if (selectedRow == -1) {
            val piece = state.board[row][col]
            if (CheckersGame.isPlayerPiece(piece, playerColor)) {
                val moves = checkersGame.getValidMoves(state)
                val pm = moves.filter { it.fromRow == row && it.fromCol == col }
                if (pm.isNotEmpty()) { selectedRow = row; selectedCol = col; validMoves = pm }
            }
        } else {
            val move = validMoves.find { it.toRow == row && it.toCol == col }
            if (move != null) {
                executeMove(move)
                selectedRow = -1; selectedCol = -1; validMoves = emptyList()
            } else {
                val piece = state.board[row][col]
                if (CheckersGame.isPlayerPiece(piece, playerColor)) {
                    val moves = checkersGame.getValidMoves(state)
                    val pm = moves.filter { it.fromRow == row && it.fromCol == col }
                    if (pm.isNotEmpty()) { selectedRow = row; selectedCol = col; validMoves = pm }
                    else { selectedRow = -1; selectedCol = -1; validMoves = emptyList() }
                } else { selectedRow = -1; selectedCol = -1; validMoves = emptyList() }
            }
        }
    }

    private fun executeMove(move: CheckersMove) {
        val newState = checkersGame.applyMove(state, move)
        lastMove = move; state = newState
        network?.sendPacket(BoardGamePacket.Move(move.fromRow, move.fromCol, move.toRow, move.toCol,
            captures = move.captures.joinToString(",") { "${it.first},${it.second}" }))
        if (state.gameOver) { onGameOver(); return }
        if (isVsAI && state.currentPlayer != playerColor) doAIMove()
    }

    private fun doAIMove() {
        aiThinking = true
        Thread {
            try {
                Thread.sleep(250)
                val move = checkersGame.getAIMove(state, 8)
                Gdx.app.postRunnable {
                    if (move != null) { state = checkersGame.applyMove(state, move); lastMove = move }
                    aiThinking = false
                    if (state.gameOver) onGameOver()
                }
            } catch (_: Exception) { aiThinking = false }
        }.start()
    }

    private fun onGameOver() {
        val win = state.winner == playerColor
        gameResult = when { state.winner == null -> "Draw"; win -> "Victory!"; else -> "Defeat" }
        when { win -> PlayerStats.recordCheckersWin()
            state.winner == null -> PlayerStats.recordCheckersDraw()
            else -> PlayerStats.recordCheckersLoss()
        }
    }

    private fun rematch() {
        state = CheckersGame.newState(); selectedRow = -1; selectedCol = -1
        validMoves = emptyList(); gameResult = ""; lastMove = null
        playerColor = if (playerColor == CheckersPiece.RED) CheckersPiece.BLACK else CheckersPiece.RED
        if (isVsAI && playerColor == CheckersPiece.BLACK) doAIMove()
    }

    private fun handleNetworkPacket(packet: BoardGamePacket) {
        if (packet is BoardGamePacket.Move) {
            val captures = if (packet.captures.isNotEmpty())
                packet.captures.split(",").chunked(2).map { it[0].toInt() to it[1].toInt() } else emptyList()
            val move = CheckersMove(packet.fromRow, packet.fromCol, packet.toRow, packet.toCol, captures)
            state = checkersGame.applyMove(state, move); lastMove = move
            if (state.gameOver) onGameOver()
        }
    }

    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); whiteTex.dispose(); font.dispose(); circleTex?.dispose(); network?.disconnect() }
}
