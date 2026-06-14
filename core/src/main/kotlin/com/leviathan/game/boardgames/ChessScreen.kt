package com.leviathan.game.boardgames

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.leviathan.game.LeviathanGame
import com.leviathan.game.screens.GameScreen

class ChessScreen(
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

    private val chessGame = ChessGame()
    private var state = ChessGame.newState()
    private var selectedRow = -1
    private var selectedCol = -1
    private var validMoves = listOf<ChessMove>()
    private var message = ""
    private var gameResult = ""
    private var showRematch = false
    private var aiThinking = false
    private var lastMove: ChessMove? = null
    private var time = 0f

    private var sw = 0f
    private var sh = 0f

    private val woodLight = Color(0.93f, 0.85f, 0.7f, 1f)
    private val woodDark = Color(0.45f, 0.28f, 0.1f, 1f)
    private val woodBorder = Color(0.3f, 0.15f, 0.05f, 1f)
    private val bgTop = Color(0.06f, 0.08f, 0.14f, 1f)
    private val bgBot = Color(0.02f, 0.02f, 0.05f, 1f)
    private val gold = Color(1f, 0.82f, 0.25f, 1f)
    private val panelBg = Color(0.03f, 0.03f, 0.07f, 0.85f)
    private val whitePieceCol = Color(1f, 1f, 1f, 1f)
    private val blackPieceCol = Color(0.08f, 0.08f, 0.08f, 1f)

    private val pieceChars = mapOf(
        ChessPieceType.KING to '♚', ChessPieceType.QUEEN to '♛',
        ChessPieceType.ROOK to '♜', ChessPieceType.BISHOP to '♝',
        ChessPieceType.KNIGHT to '♞', ChessPieceType.PAWN to '♟'
    )

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

        if (isVsAI && !isWhite) doAIMove()
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
        batch.setColor(1f, 1f, 1f, 0.008f)
        for (i in 0 until 6) {
            val x = ((time * 15f + i * 200f) % (sw + 300f)) - 150f
            batch.draw(whiteTex, x, 0f, 2f, sh)
        }
        batch.end()
    }

    private fun drawPanel(boardX: Float, boardY: Float, boardSize: Float) {
        val ph = 52f
        batch.setColor(panelBg)
        batch.draw(whiteTex, 0f, sh - ph, sw, ph)
        batch.setColor(gold.r * 0.5f, gold.g * 0.5f, gold.b * 0.5f, 0.25f)
        batch.draw(whiteTex, 0f, sh - ph, sw, 1f)

        font.data.setScale(ph / 30f)
        font.setColor(gold)
        font.draw(batch, playerName, 20f, sh - ph * 0.3f)
        font.setColor(0.5f, 0.5f, 0.6f, 0.6f)
        font.data.setScale(ph / 34f)
        font.draw(batch, if (isVsAI) "vs AI" else "vs", sw / 2f - 15f, sh - ph * 0.3f)
        font.setColor(0.7f, 0.7f, 0.8f, 1f)
        font.data.setScale(ph / 30f)
        font.draw(batch, opponentName, sw - 140f, sh - ph * 0.3f)

        val inCheckStr = if (state.inCheck) " ⚠ CHECK!" else ""
        val turnText = when {
            state.gameOver -> gameResult.ifEmpty { "Game Over" }
            state.whiteToMove == isWhite -> "Your Turn$inCheckStr"
            else -> if (isVsAI) "AI Thinking...$inCheckStr" else "$opponentName's Turn$inCheckStr"
        }
        font.data.setScale(ph / 32f)
        val turnColor = when {
            state.gameOver -> gold
            state.inCheck -> Color(1f, 0.2f, 0.2f, 1f)
            state.whiteToMove == isWhite -> Color(0.3f, 1f, 0.4f, 1f)
            else -> Color(0.7f, 0.7f, 0.8f, 0.7f)
        }
        font.setColor(turnColor)
        font.draw(batch, turnText, sw / 2f - 80f, sh - ph * 0.72f)

        font.data.setScale(ph / 36f)
        font.setColor(0.6f, 0.6f, 0.6f, 0.5f)
        font.draw(batch, "Round ${state.fullMoveNumber}", sw * 0.05f, sh - ph * 0.72f)
    }

    private fun drawBoard(boardX: Float, boardY: Float, boardSize: Float) {
        val cellSize = boardSize / 8f

        batch.setColor(0f, 0f, 0f, 0.35f)
        batch.draw(whiteTex, boardX + 5f, boardY - 5f, boardSize + 10f, boardSize + 10f)
        batch.setColor(woodBorder)
        batch.draw(whiteTex, boardX - 6f, boardY - 6f, boardSize + 12f, boardSize + 12f)

        val cs = 12f
        batch.setColor(woodBorder.r * 0.7f, woodBorder.g * 0.7f, woodBorder.b * 0.7f, 1f)
        batch.draw(whiteTex, boardX - 8f, boardY - 8f, cs, cs)
        batch.draw(whiteTex, boardX + boardSize - 4f, boardY - 8f, cs, cs)
        batch.draw(whiteTex, boardX - 8f, boardY + boardSize - 4f, cs, cs)
        batch.draw(whiteTex, boardX + boardSize - 4f, boardY + boardSize - 4f, cs, cs)

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val isLight = (row + col) % 2 == 0
                val x = boardX + col * cellSize
                val y = boardY + (7 - row) * cellSize
                batch.setColor(if (isLight) woodLight else woodDark)
                batch.draw(whiteTex, x, y, cellSize, cellSize)
            }
        }

        for (move in validMoves) {
            val x = boardX + move.toCol * cellSize
            val y = boardY + (7 - move.toRow) * cellSize
            val pulse = (kotlin.math.sin(time * 3f) * 0.2f + 0.8f).toFloat()
            if (state.board[move.toRow][move.toCol].type != ChessPieceType.EMPTY) {
                batch.setColor(1f, 0.15f, 0.15f, pulse * 0.45f)
                batch.draw(whiteTex, x, y, cellSize, cellSize)
            } else {
                batch.setColor(0.2f, 0.85f, 0.3f, pulse * 0.45f)
                val ds = cellSize * 0.22f
                batch.draw(whiteTex, x + cellSize / 2 - ds / 2, y + cellSize / 2 - ds / 2, ds, ds)
            }
        }

        if (lastMove != null) {
            val lm = lastMove!!
            batch.setColor(1f, 1f, 0.3f, 0.12f)
            batch.draw(whiteTex, boardX + lm.fromCol * cellSize, boardY + (7 - lm.fromRow) * cellSize, cellSize, cellSize)
            batch.draw(whiteTex, boardX + lm.toCol * cellSize, boardY + (7 - lm.toRow) * cellSize, cellSize, cellSize)
        }

        if (selectedRow >= 0) {
            val sx = boardX + selectedCol * cellSize
            val sy = boardY + (7 - selectedRow) * cellSize
            val p = (kotlin.math.sin(time * 4f) * 0.3f + 0.7f).toFloat()
            batch.setColor(0.3f, 0.7f, 1f, p * 0.35f)
            batch.draw(whiteTex, sx - 2f, sy - 2f, cellSize + 4f, cellSize + 4f)
        }
    }

    private fun drawPieces(boardX: Float, boardY: Float, boardSize: Float) {
        val cellSize = boardSize / 8f

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board[row][col]
                if (piece.type == ChessPieceType.EMPTY) continue
                val cx = boardX + col * cellSize + cellSize / 2f
                val cy = boardY + (7 - row) * cellSize + cellSize / 2f
                val char = pieceChars[piece.type] ?: '?'
                val size = when (piece.type) {
                    ChessPieceType.PAWN -> cellSize * 0.52f
                    ChessPieceType.KNIGHT -> cellSize * 0.58f
                    else -> cellSize * 0.62f
                }

                val isWhiteP = piece.isWhite
                val floatOffset = kotlin.math.sin(time * 1.2f + row * 2.5f + col * 1.7f).toFloat() * 1f
                val shadowOff = 2f

                font.data.setScale(size)
                batch.setColor(0f, 0f, 0f, 0.2f)
                font.draw(batch, char.toString(), cx - size * 0.28f + shadowOff, cy - size * 0.4f - shadowOff + floatOffset)

                if (isWhiteP) {
                    batch.setColor(0.9f, 0.9f, 0.9f, 1f)
                    font.draw(batch, char.toString(), cx - size * 0.28f - 1f, cy - size * 0.4f + 1f + floatOffset)
                    font.draw(batch, char.toString(), cx - size * 0.28f + 1f, cy - size * 0.4f - 1f + floatOffset)
                    batch.setColor(whitePieceCol)
                    font.draw(batch, char.toString(), cx - size * 0.28f, cy - size * 0.4f + floatOffset)
                } else {
                    batch.setColor(0.3f, 0.3f, 0.3f, 1f)
                    font.draw(batch, char.toString(), cx - size * 0.28f - 1f, cy - size * 0.4f + 1f + floatOffset)
                    font.draw(batch, char.toString(), cx - size * 0.28f + 1f, cy - size * 0.4f - 1f + floatOffset)
                    batch.setColor(blackPieceCol)
                    font.draw(batch, char.toString(), cx - size * 0.28f, cy - size * 0.4f + floatOffset)
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

            val whiteWon = state.winner == "White"
            val draw = state.winner == null
            val playerWon = (whiteWon && isWhite) || (!whiteWon && !isWhite)
            val resultText = when {
                draw -> "─ DRAW ─"
                playerWon -> "✦ VICTORY! ✦"
                else -> "✕ DEFEAT ✕"
            }
            val resultColor = when {
                draw -> gold
                playerWon -> Color(0.3f, 1f, 0.4f, 1f)
                else -> Color(1f, 0.3f, 0.3f, 1f)
            }

            font.data.setScale(sh / 140f)
            val glow = (kotlin.math.sin(time * 3f) * 0.15f + 0.85f).toFloat()
            font.setColor(resultColor.r * glow, resultColor.g * glow, resultColor.b * glow, 1f)
            font.draw(batch, resultText, cx - font.getBounds(resultText).width / 2, sh * 0.6f)

            if (state.gameResult != null) {
                font.data.setScale(sh / 300f)
                font.setColor(0.8f, 0.8f, 0.8f, 0.7f)
                font.draw(batch, state.gameResult, cx - font.getBounds(state.gameResult!!).width / 2, sh * 0.53f)
            }

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
        val tw = font.getBounds(text).width
        font.draw(batch, text, x + w / 2 - tw / 2, y + h * 0.62f)
        buttons.add(ButtonDef(text, Rectangle(x, y, w, h), {
            if (text.contains("PLAY AGAIN")) rematch() else game.setScreen(BoardGameMenuScreen(game))
        }))
    }

    private val buttons = mutableListOf<ButtonDef>()

    private fun handleInput(boardX: Float, boardY: Float, boardSize: Float) {
        buttons.clear()
        if (state.gameOver || state.whiteToMove != isWhite || aiThinking) return

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
            if (piece.type != ChessPieceType.EMPTY && piece.isWhite == isWhite) {
                val moves = chessGame.getValidMoves(state)
                val pm = moves.filter { it.fromRow == row && it.fromCol == col }
                if (pm.isNotEmpty()) { selectedRow = row; selectedCol = col; validMoves = pm }
            }
        } else {
            val move = validMoves.find { it.toRow == row && it.toCol == col }
            if (move != null) {
                val piece = state.board[selectedRow][selectedCol]
                if (piece.type == ChessPieceType.PAWN && (row == 0 || row == 7)) {
                    val queenMove = move.copy(promotion = ChessPieceType.QUEEN)
                    executeMove(queenMove)
                } else {
                    executeMove(move)
                }
                selectedRow = -1; selectedCol = -1; validMoves = emptyList()
            } else {
                val piece = state.board[row][col]
                if (piece.type != ChessPieceType.EMPTY && piece.isWhite == isWhite) {
                    val moves = chessGame.getValidMoves(state)
                    val pm = moves.filter { it.fromRow == row && it.fromCol == col }
                    if (pm.isNotEmpty()) { selectedRow = row; selectedCol = col; validMoves = pm }
                    else { selectedRow = -1; selectedCol = -1; validMoves = emptyList() }
                } else { selectedRow = -1; selectedCol = -1; validMoves = emptyList() }
            }
        }
    }

    private fun executeMove(move: ChessMove) {
        state = chessGame.applyMove(state, move)
        lastMove = move
        network?.sendPacket(BoardGamePacket.Move(move.fromRow, move.fromCol, move.toRow, move.toCol,
            promotion = move.promotion.name))
        if (state.gameOver) { onGameOver(); return }
        if (isVsAI && state.whiteToMove != isWhite) doAIMove()
    }

    private fun doAIMove() {
        aiThinking = true
        Thread {
            try {
                Thread.sleep(200)
                val move = chessGame.getAIMove(state, 4)
                Gdx.app.postRunnable {
                    if (move != null) { state = chessGame.applyMove(state, move); lastMove = move }
                    aiThinking = false
                    if (state.gameOver) onGameOver()
                }
            } catch (_: Exception) { aiThinking = false }
        }.start()
    }

    private fun onGameOver() {
        val whiteWon = state.winner == "White"
        val playerWon = (whiteWon && isWhite) || (!whiteWon && !isWhite)
        gameResult = when { state.winner == null -> "Draw"; playerWon -> "Victory!"; else -> "Defeat" }
        when { playerWon -> PlayerStats.recordChessWin()
            state.winner == null -> PlayerStats.recordChessDraw()
            else -> PlayerStats.recordChessLoss()
        }
    }

    private fun rematch() {
        state = ChessGame.newState(); selectedRow = -1; selectedCol = -1
        validMoves = emptyList(); gameResult = ""; lastMove = null
        if (isVsAI && !isWhite) doAIMove()
    }

    private fun handleNetworkPacket(packet: BoardGamePacket) {
        if (packet is BoardGamePacket.Move) {
            val promo = if (packet.promotion.isNotEmpty()) ChessPieceType.valueOf(packet.promotion) else ChessPieceType.EMPTY
            val move = ChessMove(packet.fromRow, packet.fromCol, packet.toRow, packet.toCol, promotion = promo)
            state = chessGame.applyMove(state, move); lastMove = move
            if (state.gameOver) onGameOver()
        }
    }

    override fun resize(w: Int, h: Int) { cam.setToOrtho(false) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { batch.dispose(); whiteTex.dispose(); font.dispose() }
}
