package com.leviathan.game.boardgames

enum class CheckersPiece {
    EMPTY, RED, BLACK, RED_KING, BLACK_KING
}

data class CheckersMove(
    val fromRow: Int, val fromCol: Int,
    val toRow: Int, val toCol: Int,
    val captures: List<Pair<Int, Int>> = emptyList()
) {
    val isJump get() = captures.isNotEmpty()

    fun reversed() = CheckersMove(7 - fromRow, 7 - fromCol, 7 - toRow, 7 - toCol,
        captures.map { 7 - it.first to 7 - it.second })
}

data class CheckersState(
    val board: Array<Array<CheckersPiece>>,
    val currentPlayer: CheckersPiece,
    val redCount: Int,
    val blackCount: Int,
    val mustJump: Boolean = false,
    val jumpFrom: Pair<Int, Int>? = null,
    val gameOver: Boolean = false,
    val winner: CheckersPiece? = null,
    val drawByRepetition: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CheckersState) return false
        return board.contentDeepEquals(other.board) &&
               currentPlayer == other.currentPlayer &&
               mustJump == other.mustJump &&
               jumpFrom == other.jumpFrom &&
               gameOver == other.gameOver &&
               winner == other.winner
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentPlayer.hashCode()
        result = 31 * result + mustJump.hashCode()
        result = 31 * result + (jumpFrom?.hashCode() ?: 0)
        return result
    }
}

class CheckersGame {
    companion object {
        fun createInitialBoard(): Array<Array<CheckersPiece>> {
            val board = Array(8) { Array(8) { CheckersPiece.EMPTY } }
            for (row in 0 until 3) {
                for (col in 0 until 8) {
                    if ((row + col) % 2 == 1) board[row][col] = CheckersPiece.BLACK
                }
            }
            for (row in 5 until 8) {
                for (col in 0 until 8) {
                    if ((row + col) % 2 == 1) board[row][col] = CheckersPiece.RED
                }
            }
            return board
        }

        fun isRed(piece: CheckersPiece) = piece == CheckersPiece.RED || piece == CheckersPiece.RED_KING
        fun isBlack(piece: CheckersPiece) = piece == CheckersPiece.BLACK || piece == CheckersPiece.BLACK_KING
        fun isKing(piece: CheckersPiece) = piece == CheckersPiece.RED_KING || piece == CheckersPiece.BLACK_KING
        fun isPlayerPiece(piece: CheckersPiece, player: CheckersPiece) =
            (player == CheckersPiece.RED && isRed(piece)) || (player == CheckersPiece.BLACK && isBlack(piece))

        fun getOpponent(player: CheckersPiece) =
            if (player == CheckersPiece.RED) CheckersPiece.BLACK else CheckersPiece.RED

        fun newState(): CheckersState {
            val board = createInitialBoard()
            return CheckersState(board, CheckersPiece.RED, 12, 12)
        }
    }

    fun getValidMoves(state: CheckersState): List<CheckersMove> {
        if (state.gameOver) return emptyList()
        val moves = mutableListOf<CheckersMove>()
        val player = state.currentPlayer
        val forward = if (player == CheckersPiece.RED) -1 else 1

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board[row][col]
                if (!isPlayerPiece(piece, player)) continue

                if (state.mustJump && state.jumpFrom != null &&
                    (state.jumpFrom.first != row || state.jumpFrom.second != col)) continue

                val jumps = getJumps(state.board, row, col, player, forward)
                moves.addAll(jumps)

                if (!state.mustJump && jumps.isEmpty()) {
                    val dirs = getMoveDirections(piece, forward)
                    val isKing = isKing(piece)
                    for ((dr, dc) in dirs) {
                        var nr = row + dr
                        var nc = col + dc
                        while (nr in 0 until 8 && nc in 0 until 8 && state.board[nr][nc] == CheckersPiece.EMPTY) {
                            moves.add(CheckersMove(row, col, nr, nc))
                            if (!isKing) break
                            nr += dr
                            nc += dc
                        }
                    }
                }
            }
        }

        val jumps = moves.filter { it.isJump }
        return if (jumps.isNotEmpty()) jumps else moves
    }

    private fun getJumps(
        board: Array<Array<CheckersPiece>>,
        row: Int, col: Int,
        player: CheckersPiece,
        forward: Int,
        visited: Set<Pair<Int, Int>> = emptySet()
    ): List<CheckersMove> {
        val jumps = mutableListOf<CheckersMove>()
        val piece = board[row][col]
        val dirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        val opponent = getOpponent(player)

        for ((dr, dc) in dirs) {
            if (isKing(piece)) {
                var step = 1
                var foundOpp = false
                var oppRow = -1
                var oppCol = -1
                while (true) {
                    val cr = row + dr * step
                    val cc = col + dc * step
                    if (cr !in 0 until 8 || cc !in 0 until 8) break
                    val cell = board[cr][cc]
                    if (!foundOpp) {
                        if (isPlayerPiece(cell, opponent)) {
                            foundOpp = true; oppRow = cr; oppCol = cc
                        } else if (cell != CheckersPiece.EMPTY) break
                    } else {
                        if (cell == CheckersPiece.EMPTY && !visited.contains(oppRow to oppCol)) {
                            val newVisited = visited + (oppRow to oppCol)
                            val capture = oppRow to oppCol
                            val baseMove = CheckersMove(row, col, cr, cc, listOf(capture))
                            val newBoard = board.map { it.copyOf() }.toTypedArray()
                            newBoard[row][col] = CheckersPiece.EMPTY
                            newBoard[oppRow][oppCol] = CheckersPiece.EMPTY
                            newBoard[cr][cc] = piece
                            val furtherJumps = getJumps(newBoard, cr, cc, player, forward, newVisited)
                            if (furtherJumps.isNotEmpty()) {
                                for (fj in furtherJumps) {
                                    jumps.add(CheckersMove(row, col, fj.toRow, fj.toCol, listOf(capture) + fj.captures))
                                }
                            } else {
                                jumps.add(baseMove)
                            }
                        } else if (cell != CheckersPiece.EMPTY) break
                    }
                    step++
                }
            } else {
                val mr = row + dr
                val mc = col + dc
                val lr = row + 2 * dr
                val lc = col + 2 * dc

                if (mr !in 0 until 8 || mc !in 0 until 8) continue
                if (lr !in 0 until 8 || lc !in 0 until 8) continue

                val midPiece = board[mr][mc]
                if (!isPlayerPiece(midPiece, opponent)) continue
                if (board[lr][lc] != CheckersPiece.EMPTY) continue
                if (visited.contains(mr to mc)) continue

                val newVisited = visited + (mr to mc)
                val baseMove = CheckersMove(row, col, lr, lc, listOf(mr to mc))

                val newBoard = board.map { it.copyOf() }.toTypedArray()
                newBoard[row][col] = CheckersPiece.EMPTY
                var promotedPiece = player
                if (!isKing(piece)) {
                    if ((player == CheckersPiece.RED && lr == 0) || (player == CheckersPiece.BLACK && lr == 7)) {
                        promotedPiece = if (player == CheckersPiece.RED) CheckersPiece.RED_KING else CheckersPiece.BLACK_KING
                    }
                } else {
                    promotedPiece = piece
                }
                newBoard[lr][lc] = promotedPiece
                newBoard[mr][mc] = CheckersPiece.EMPTY

                if (isKing(promotedPiece) != isKing(piece) || (isKing(piece) && promotedPiece == piece)) {
                    val furtherJumps = getJumps(newBoard, lr, lc, player, forward, newVisited)
                    if (furtherJumps.isNotEmpty()) {
                        for (fj in furtherJumps) {
                            jumps.add(CheckersMove(row, col, fj.toRow, fj.toCol,
                                listOf(mr to mc) + fj.captures))
                        }
                    } else {
                        jumps.add(baseMove)
                    }
                } else {
                    jumps.add(baseMove)
                }
            }
        }
        return jumps
    }

    private fun getMoveDirections(piece: CheckersPiece, forward: Int): List<Pair<Int, Int>> {
        return when {
            piece == CheckersPiece.RED || piece == CheckersPiece.BLACK -> listOf(forward to -1, forward to 1)
            else -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        }
    }

    fun applyMove(state: CheckersState, move: CheckersMove): CheckersState {
        val newBoard = state.board.map { it.copyOf() }.toTypedArray()

        val piece = newBoard[move.fromRow][move.fromCol]
        newBoard[move.fromRow][move.fromCol] = CheckersPiece.EMPTY

        var finalPiece = piece
        if (!isKing(piece)) {
            if ((isRed(piece) && move.toRow == 0) || (isBlack(piece) && move.toRow == 7)) {
                finalPiece = if (isRed(piece)) CheckersPiece.RED_KING else CheckersPiece.BLACK_KING
            }
        }
        newBoard[move.toRow][move.toCol] = finalPiece

        var capturedRed = 0
        var capturedBlack = 0
        for ((cr, cc) in move.captures) {
            val capPiece = newBoard[cr][cc]
            if (isRed(capPiece)) capturedRed++
            else if (isBlack(capPiece)) capturedBlack++
            newBoard[cr][cc] = CheckersPiece.EMPTY
        }

        val newRedCount = state.redCount - capturedRed
        val newBlackCount = state.blackCount - capturedBlack

        if (newRedCount == 0 || newBlackCount == 0) {
            val winner = when {
                newRedCount == 0 -> CheckersPiece.BLACK
                newBlackCount == 0 -> CheckersPiece.RED
                else -> null
            }
            return CheckersState(newBoard, state.currentPlayer, newRedCount, newBlackCount,
                gameOver = true, winner = winner)
        }

        val opponent = getOpponent(state.currentPlayer)

        if (move.isJump) {
            val newState = CheckersState(newBoard, state.currentPlayer, newRedCount, newBlackCount,
                mustJump = false, jumpFrom = null)
            val furtherJumps = getJumps(newBoard, move.toRow, move.toCol, state.currentPlayer,
                if (state.currentPlayer == CheckersPiece.RED) -1 else 1)
            if (furtherJumps.isNotEmpty() && !isKing(finalPiece) == !isKing(piece)) {
                return newState.copy(mustJump = true, jumpFrom = move.toRow to move.toCol)
            }
            return newState.copy(currentPlayer = opponent)
        }

        val opponentMoves = getValidMoves(CheckersState(newBoard, opponent, newRedCount, newBlackCount))
        if (opponentMoves.isEmpty()) {
            return CheckersState(newBoard, opponent, newRedCount, newBlackCount,
                gameOver = true, winner = state.currentPlayer)
        }

        return CheckersState(newBoard, opponent, newRedCount, newBlackCount)
    }

    fun getAIMove(state: CheckersState, depth: Int = 8): CheckersMove? {
        val moves = getValidMoves(state)
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()

        var bestMove = moves.first()
        var bestScore = Int.MIN_VALUE
        val alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        for (move in moves) {
            val newState = applyMove(state, move)
            val score = minimax(newState, depth - 1, alpha, beta, false)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(state: CheckersState, depth: Int, alpha: Int, beta: Int, maximizing: Boolean): Int {
        if (depth == 0 || state.gameOver) return evaluate(state) * (if (maximizing) 1 else -1)

        val moves = getValidMoves(state)
        if (moves.isEmpty()) return evaluate(state)

        var a = alpha
        var b = beta

        if (maximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                val newState = applyMove(state, move)
                val eval = minimax(newState, depth - 1, a, b, false)
                maxEval = maxOf(maxEval, eval)
                a = maxOf(a, eval)
                if (b <= a) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                val newState = applyMove(state, move)
                val eval = minimax(newState, depth - 1, a, b, true)
                minEval = minOf(minEval, eval)
                b = minOf(b, eval)
                if (b <= a) break
            }
            return minEval
        }
    }

    fun evaluate(state: CheckersState): Int {
        if (state.gameOver) {
            return when (state.winner) {
                CheckersPiece.RED -> 100000
                CheckersPiece.BLACK -> -100000
                else -> 0
            }
        }

        var score = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board[row][col]
                when (piece) {
                    CheckersPiece.RED -> {
                        score += 100 + (7 - row) * 2
                        if (col == 0 || col == 7) score += 5
                        if (row >= 4) score += 3
                    }
                    CheckersPiece.RED_KING -> {
                        score += 160
                        if (row in 2..5 && col in 2..5) score += 10
                    }
                    CheckersPiece.BLACK -> {
                        score -= 100 + row * 2
                        if (col == 0 || col == 7) score -= 5
                        if (row <= 3) score -= 3
                    }
                    CheckersPiece.BLACK_KING -> {
                        score -= 160
                        if (row in 2..5 && col in 2..5) score -= 10
                    }
                    else -> {}
                }
            }
        }
        return score
    }
}
