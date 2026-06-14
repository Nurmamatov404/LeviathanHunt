package com.leviathan.game.boardgames

enum class ChessPieceType { EMPTY, PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

data class ChessPiece(val type: ChessPieceType, val isWhite: Boolean) {
    val isBlack get() = !isWhite
    companion object {
        val EMPTY = ChessPiece(ChessPieceType.EMPTY, true)
        val W_PAWN = ChessPiece(ChessPieceType.PAWN, true)
        val W_KNIGHT = ChessPiece(ChessPieceType.KNIGHT, true)
        val W_BISHOP = ChessPiece(ChessPieceType.BISHOP, true)
        val W_ROOK = ChessPiece(ChessPieceType.ROOK, true)
        val W_QUEEN = ChessPiece(ChessPieceType.QUEEN, true)
        val W_KING = ChessPiece(ChessPieceType.KING, true)
        val B_PAWN = ChessPiece(ChessPieceType.PAWN, false)
        val B_KNIGHT = ChessPiece(ChessPieceType.KNIGHT, false)
        val B_BISHOP = ChessPiece(ChessPieceType.BISHOP, false)
        val B_ROOK = ChessPiece(ChessPieceType.ROOK, false)
        val B_QUEEN = ChessPiece(ChessPieceType.QUEEN, false)
        val B_KING = ChessPiece(ChessPieceType.KING, false)
    }
}

data class ChessMove(
    val fromRow: Int, val fromCol: Int,
    val toRow: Int, val toCol: Int,
    val promotion: ChessPieceType = ChessPieceType.EMPTY,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val enPassantCaptureRow: Int = -1,
    val enPassantCaptureCol: Int = -1
)

data class ChessState(
    val board: Array<Array<ChessPiece>>,
    val whiteToMove: Boolean,
    val castlingK: Boolean, val castlingQ: Boolean,
    val castlingk: Boolean, val castlingq: Boolean,
    val enPassantSquare: Pair<Int, Int>?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
    val inCheck: Boolean = false,
    val gameOver: Boolean = false,
    val winner: String? = null,
    val gameResult: String? = null,
    val whiteKingPos: Pair<Int, Int> = 7 to 4,
    val blackKingPos: Pair<Int, Int> = 0 to 4,
    val moveHistory: List<ChessMove> = emptyList()
)

class ChessGame {
    companion object {
        fun createInitialBoard(): Array<Array<ChessPiece>> {
            val board = Array(8) { Array(8) { ChessPiece.EMPTY } }
            val backRank = arrayOf(
                ChessPieceType.ROOK, ChessPieceType.KNIGHT, ChessPieceType.BISHOP,
                ChessPieceType.QUEEN, ChessPieceType.KING, ChessPieceType.BISHOP,
                ChessPieceType.KNIGHT, ChessPieceType.ROOK
            )
            for (col in 0 until 8) {
                board[0][col] = ChessPiece(backRank[col], false)
                board[1][col] = ChessPiece.B_PAWN
                board[6][col] = ChessPiece.W_PAWN
                board[7][col] = ChessPiece(backRank[col], true)
            }
            return board
        }

        fun newState(): ChessState {
            val board = createInitialBoard()
            return ChessState(
                board = board,
                whiteToMove = true,
                castlingK = true, castlingQ = true,
                castlingk = true, castlingq = true,
                enPassantSquare = null,
                halfMoveClock = 0,
                fullMoveNumber = 1
            )
        }

        fun pieceToChar(piece: ChessPiece): String = when {
            piece.type == ChessPieceType.EMPTY -> "."
            piece.isWhite -> when (piece.type) {
                ChessPieceType.KING -> "K"
                ChessPieceType.QUEEN -> "Q"
                ChessPieceType.ROOK -> "R"
                ChessPieceType.BISHOP -> "B"
                ChessPieceType.KNIGHT -> "N"
                ChessPieceType.PAWN -> "P"
                else -> "?"
            }
            else -> when (piece.type) {
                ChessPieceType.KING -> "k"
                ChessPieceType.QUEEN -> "q"
                ChessPieceType.ROOK -> "r"
                ChessPieceType.BISHOP -> "b"
                ChessPieceType.KNIGHT -> "n"
                ChessPieceType.PAWN -> "p"
                else -> "?"
            }
        }
    }

    private val pawnEvalWhite = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5, 5, 10, 25, 25, 10, 5, 5),
        intArrayOf(0, 0, 0, 20, 20, 0, 0, 0),
        intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
        intArrayOf(5, 10, 10, -20, -20, 10, 10, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val knightEval = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
        intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
        intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
        intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
        intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
        intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val bishopEval = arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 10, 10, 5, 0, -10),
        intArrayOf(-10, 5, 5, 10, 10, 5, 5, -10),
        intArrayOf(-10, 0, 10, 10, 10, 10, 0, -10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10, -10),
        intArrayOf(-10, 5, 0, 0, 0, 0, 5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    private val rookEval = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(5, 10, 10, 10, 10, 10, 10, 5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(0, 0, 0, 5, 5, 0, 0, 0)
    )

    private val queenEval = arrayOf(
        intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 5, 5, 5, 0, -10),
        intArrayOf(-5, 0, 5, 5, 5, 5, 0, -5),
        intArrayOf(0, 0, 5, 5, 5, 5, 0, -5),
        intArrayOf(-10, 5, 5, 5, 5, 5, 0, -10),
        intArrayOf(-10, 0, 5, 0, 0, 0, 0, -10),
        intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20)
    )

    private val kingEval = arrayOf(
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
        intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
        intArrayOf(20, 20, 0, 0, 0, 0, 20, 20),
        intArrayOf(20, 30, 10, 0, 0, 10, 30, 20)
    )

    fun getValidMoves(state: ChessState): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val white = state.whiteToMove

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board[row][col]
                if (piece.type == ChessPieceType.EMPTY) continue
                if (piece.isWhite != white) continue

                when (piece.type) {
                    ChessPieceType.PAWN -> addPawnMoves(state, row, col, white, moves)
                    ChessPieceType.KNIGHT -> addKnightMoves(state, row, col, white, moves)
                    ChessPieceType.BISHOP -> addSlidingMoves(state, row, col, white, moves, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
                    ChessPieceType.ROOK -> addSlidingMoves(state, row, col, white, moves, 1 to 0, -1 to 0, 0 to 1, 0 to -1)
                    ChessPieceType.QUEEN -> addSlidingMoves(state, row, col, white, moves,
                        1 to 1, 1 to -1, -1 to 1, -1 to -1, 1 to 0, -1 to 0, 0 to 1, 0 to -1)
                    ChessPieceType.KING -> addKingMoves(state, row, col, white, moves)
                }
            }
        }

        return moves.filter { !wouldBeInCheck(state, it) }
    }

    private fun addPawnMoves(state: ChessState, row: Int, col: Int, white: Boolean, moves: MutableList<ChessMove>) {
        val dir = if (white) -1 else 1
        val startRow = if (white) 6 else 1
        val promoRow = if (white) 0 else 7

        if (row + dir in 0 until 8 && state.board[row + dir][col].type == ChessPieceType.EMPTY) {
            if (row + dir == promoRow) {
                for (promo in arrayOf(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT)) {
                    moves.add(ChessMove(row, col, row + dir, col, promotion = promo))
                }
            } else {
                moves.add(ChessMove(row, col, row + dir, col))
            }
            if (row == startRow && state.board[row + 2 * dir][col].type == ChessPieceType.EMPTY) {
                moves.add(ChessMove(row, col, row + 2 * dir, col))
            }
        }

        for (dc in listOf(-1, 1)) {
            val nc = col + dc
            if (nc !in 0 until 8) continue
            val nr = row + dir
            val target = state.board[nr][nc]
            if (target.type != ChessPieceType.EMPTY && target.isWhite != white) {
                if (nr == promoRow) {
                    for (promo in arrayOf(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT)) {
                        moves.add(ChessMove(row, col, nr, nc, promotion = promo))
                    }
                } else {
                    moves.add(ChessMove(row, col, nr, nc))
                }
            }

            if (state.enPassantSquare != null) {
                val (epRow, epCol) = state.enPassantSquare
                if (nr == epRow && nc == epCol) {
                    moves.add(ChessMove(row, col, nr, nc, isEnPassant = true, enPassantCaptureRow = row, enPassantCaptureCol = nc))
                }
            }
        }
    }

    private fun addKnightMoves(state: ChessState, row: Int, col: Int, white: Boolean, moves: MutableList<ChessMove>) {
        val offsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((dr, dc) in offsets) {
            val nr = row + dr
            val nc = col + dc
            if (nr in 0 until 8 && nc in 0 until 8) {
                val target = state.board[nr][nc]
                if (target.type == ChessPieceType.EMPTY || target.isWhite != white) {
                    moves.add(ChessMove(row, col, nr, nc))
                }
            }
        }
    }

    private fun addSlidingMoves(state: ChessState, row: Int, col: Int, white: Boolean,
                                 moves: MutableList<ChessMove>, vararg dirs: Pair<Int, Int>) {
        for ((dr, dc) in dirs) {
            var r = row + dr
            var c = col + dc
            while (r in 0 until 8 && c in 0 until 8) {
                val target = state.board[r][c]
                if (target.type == ChessPieceType.EMPTY) {
                    moves.add(ChessMove(row, col, r, c))
                } else {
                    if (target.isWhite != white) {
                        moves.add(ChessMove(row, col, r, c))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    private fun addKingMoves(state: ChessState, row: Int, col: Int, white: Boolean, moves: MutableList<ChessMove>) {
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr
                val nc = col + dc
                if (nr in 0 until 8 && nc in 0 until 8) {
                    val target = state.board[nr][nc]
                    if (target.type == ChessPieceType.EMPTY || target.isWhite != white) {
                        moves.add(ChessMove(row, col, nr, nc))
                    }
                }
            }
        }

        if (white && row == 7 && col == 4) {
            if (state.castlingK && state.board[7][5].type == ChessPieceType.EMPTY &&
                state.board[7][6].type == ChessPieceType.EMPTY && state.board[7][7].let { it.type == ChessPieceType.ROOK && it.isWhite }) {
                if (!isSquareAttacked(state, 7, 4, false) && !isSquareAttacked(state, 7, 5, false) && !isSquareAttacked(state, 7, 6, false)) {
                    moves.add(ChessMove(row, col, 7, 6, isCastling = true))
                }
            }
            if (state.castlingQ && state.board[7][3].type == ChessPieceType.EMPTY &&
                state.board[7][2].type == ChessPieceType.EMPTY && state.board[7][1].type == ChessPieceType.EMPTY &&
                state.board[7][0].let { it.type == ChessPieceType.ROOK && it.isWhite }) {
                if (!isSquareAttacked(state, 7, 4, false) && !isSquareAttacked(state, 7, 3, false) && !isSquareAttacked(state, 7, 2, false)) {
                    moves.add(ChessMove(row, col, 7, 2, isCastling = true))
                }
            }
        }
        if (!white && row == 0 && col == 4) {
            if (state.castlingk && state.board[0][5].type == ChessPieceType.EMPTY &&
                state.board[0][6].type == ChessPieceType.EMPTY && state.board[0][7].let { it.type == ChessPieceType.ROOK && it.isBlack }) {
                if (!isSquareAttacked(state, 0, 4, true) && !isSquareAttacked(state, 0, 5, true) && !isSquareAttacked(state, 0, 6, true)) {
                    moves.add(ChessMove(row, col, 0, 6, isCastling = true))
                }
            }
            if (state.castlingq && state.board[0][3].type == ChessPieceType.EMPTY &&
                state.board[0][2].type == ChessPieceType.EMPTY && state.board[0][1].type == ChessPieceType.EMPTY &&
                state.board[0][0].let { it.type == ChessPieceType.ROOK && it.isBlack }) {
                if (!isSquareAttacked(state, 0, 4, true) && !isSquareAttacked(state, 0, 3, true) && !isSquareAttacked(state, 0, 2, true)) {
                    moves.add(ChessMove(row, col, 0, 2, isCastling = true))
                }
            }
        }
    }

    fun isSquareAttacked(state: ChessState, row: Int, col: Int, byWhite: Boolean): Boolean {
        val opponent = if (byWhite) false else true

        for (dr in -1..1) {
            for (dc in -1..1) {
                if ((dr == 0 || dc == 0) && dr + dc != 0) {
                    var r = row + dr
                    var c = col + dc
                    while (r in 0 until 8 && c in 0 until 8) {
                        val p = state.board[r][c]
                        if (p.type != ChessPieceType.EMPTY) {
                            if (p.isWhite == byWhite && (p.type == ChessPieceType.ROOK || p.type == ChessPieceType.QUEEN)) return true
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
                if (dr != 0 && dc != 0) {
                    var r = row + dr
                    var c = col + dc
                    while (r in 0 until 8 && c in 0 until 8) {
                        val p = state.board[r][c]
                        if (p.type != ChessPieceType.EMPTY) {
                            if (p.isWhite == byWhite && (p.type == ChessPieceType.BISHOP || p.type == ChessPieceType.QUEEN)) return true
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
        }

        for ((dr, dc) in listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)) {
            val r = row + dr
            val c = col + dc
            if (r in 0 until 8 && c in 0 until 8) {
                val p = state.board[r][c]
                if (p.type == ChessPieceType.KNIGHT && p.isWhite == byWhite) return true
            }
        }

        for (dc in listOf(-1, 1)) {
            val dir = if (byWhite) -1 else 1
            val r = row + dir
            val c = col + dc
            if (r in 0 until 8 && c in 0 until 8) {
                val p = state.board[r][c]
                if (p.type == ChessPieceType.PAWN && p.isWhite == byWhite) return true
            }
        }

        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in 0 until 8 && c in 0 until 8) {
                    val p = state.board[r][c]
                    if (p.type == ChessPieceType.KING && p.isWhite == byWhite) return true
                }
            }
        }

        return false
    }

    private fun wouldBeInCheck(state: ChessState, move: ChessMove): Boolean {
        val newState = applyMoveRaw(state, move)

        val kingRow: Int
        val kingCol: Int
        if (state.whiteToMove) {
            if (state.board[move.fromRow][move.fromCol].type == ChessPieceType.KING) {
                kingRow = move.toRow
                kingCol = move.toCol
            } else {
                kingRow = state.whiteKingPos.first
                kingCol = state.whiteKingPos.second
            }
        } else {
            if (state.board[move.fromRow][move.fromCol].type == ChessPieceType.KING) {
                kingRow = move.toRow
                kingCol = move.toCol
            } else {
                kingRow = state.blackKingPos.first
                kingCol = state.blackKingPos.second
            }
        }

        return isSquareAttacked(newState, kingRow, kingCol, !state.whiteToMove)
    }

    private fun applyMoveRaw(state: ChessState, move: ChessMove): ChessState {
        val newBoard = state.board.map { it.copyOf() }.toTypedArray()
        val piece = newBoard[move.fromRow][move.fromCol]
        newBoard[move.fromRow][move.fromCol] = ChessPiece.EMPTY

        if (move.isCastling) {
            newBoard[move.toRow][move.toCol] = piece
            if (move.toCol == 6) {
                newBoard[move.toRow][5] = newBoard[move.toRow][7]
                newBoard[move.toRow][7] = ChessPiece.EMPTY
            } else {
                newBoard[move.toRow][3] = newBoard[move.toRow][0]
                newBoard[move.toRow][0] = ChessPiece.EMPTY
            }
        } else {
            if (move.promotion != ChessPieceType.EMPTY) {
                newBoard[move.toRow][move.toCol] = ChessPiece(move.promotion, piece.isWhite)
            } else {
                newBoard[move.toRow][move.toCol] = piece
            }
        }

        if (move.isEnPassant) {
            newBoard[move.enPassantCaptureRow][move.enPassantCaptureCol] = ChessPiece.EMPTY
        }

        return newState(state, newBoard, move)
    }

    private fun newState(old: ChessState, newBoard: Array<Array<ChessPiece>>, move: ChessMove): ChessState {
        val isWhite = !old.whiteToMove
        val whiteKingPos = if (isWhite) old.whiteKingPos else
            if (old.board[move.fromRow][move.fromCol].type == ChessPieceType.KING && !old.whiteToMove)
                move.toRow to move.toCol else old.whiteKingPos
        val blackKingPos = if (!isWhite) old.blackKingPos else
            if (old.board[move.fromRow][move.fromCol].type == ChessPieceType.KING && old.whiteToMove)
                move.toRow to move.toCol else old.blackKingPos

        val castlingK = old.castlingK && !(old.whiteToMove && move.fromRow == 7 && move.fromCol == 4) && !(old.whiteToMove && move.fromRow == 7 && move.fromCol == 7)
        val castlingQ = old.castlingQ && !(old.whiteToMove && move.fromRow == 7 && move.fromCol == 4) && !(old.whiteToMove && move.fromRow == 7 && move.fromCol == 0)
        val castlingk = old.castlingk && !(!old.whiteToMove && move.fromRow == 0 && move.fromCol == 4) && !(!old.whiteToMove && move.fromRow == 0 && move.fromCol == 7)
        val castlingq = old.castlingq && !(!old.whiteToMove && move.fromRow == 0 && move.fromCol == 4) && !(!old.whiteToMove && move.fromRow == 0 && move.fromCol == 0)

        var epSquare: Pair<Int, Int>? = null
        val piece = old.board[move.fromRow][move.fromCol]
        if (piece.type == ChessPieceType.PAWN && kotlin.math.abs(move.toRow - move.fromRow) == 2) {
            epSquare = (move.fromRow + move.toRow) / 2 to move.fromCol
        }

        val hmc = if (piece.type == ChessPieceType.PAWN || newBoard[move.toRow][move.toCol].type != ChessPieceType.EMPTY) 0 else old.halfMoveClock + 1
        val fmn = if (isWhite) old.fullMoveNumber + 1 else old.fullMoveNumber

        val kingPos = if (isWhite) whiteKingPos else blackKingPos
        val inCheck = isSquareAttacked(ChessState(newBoard, isWhite, castlingK, castlingQ, castlingk, castlingq, epSquare, hmc, fmn,
            whiteKingPos = whiteKingPos, blackKingPos = blackKingPos),
            kingPos.first, kingPos.second, !isWhite)

        return ChessState(newBoard, isWhite, castlingK, castlingQ, castlingk, castlingq, epSquare, hmc, fmn,
            inCheck = inCheck, whiteKingPos = whiteKingPos, blackKingPos = blackKingPos,
            moveHistory = old.moveHistory + move)
    }

    fun applyMove(state: ChessState, move: ChessMove): ChessState {
        val after = applyMoveRaw(state, move)

        val opponent = !after.whiteToMove
        val opponentKingPos = if (opponent) after.blackKingPos else after.whiteKingPos
        val opponentMoves = getValidMoves(ChessState(after.board, after.whiteToMove,
            after.castlingK, after.castlingQ, after.castlingk, after.castlingq,
            after.enPassantSquare, after.halfMoveClock, after.fullMoveNumber,
            whiteKingPos = after.whiteKingPos, blackKingPos = after.blackKingPos))

        if (opponentMoves.isEmpty()) {
            val inCheck = isSquareAttacked(after, opponentKingPos.first, opponentKingPos.second, !after.whiteToMove)
            val winner = if (inCheck) (if (!after.whiteToMove) "White" else "Black") else null
            return after.copy(
                gameOver = true,
                winner = winner,
                gameResult = if (inCheck) "Checkmate" else "Stalemate"
            )
        }

        if (after.halfMoveClock >= 100) {
            return after.copy(gameOver = true, gameResult = "Draw (50-move rule)")
        }

        val inCheck = after.inCheck
        val hasSufficientMaterial = hasSufficientMaterial(after)
        if (!hasSufficientMaterial) {
            return after.copy(gameOver = true, gameResult = "Draw (insufficient material)")
        }

        return after
    }

    private fun hasSufficientMaterial(state: ChessState): Boolean {
        var whitePieces = 0
        var blackPieces = 0
        var whiteMinor = true
        var blackMinor = true
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val p = state.board[row][col]
                when (p.type) {
                    ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.PAWN -> {
                        if (p.isWhite) whiteMinor = false else blackMinor = false
                        if (p.isWhite) whitePieces++ else blackPieces++
                    }
                    ChessPieceType.KNIGHT, ChessPieceType.BISHOP -> {
                        if (p.isWhite) whitePieces++ else blackPieces++
                    }
                    else -> {}
                }
            }
        }
        return !(whitePieces <= 1 && blackPieces <= 1)
    }

    fun getAIMove(state: ChessState, depth: Int = 4): ChessMove? {
        val moves = getValidMoves(state)
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()

        var bestMove = moves.first()
        var bestScore = Int.MIN_VALUE
        var a = Int.MIN_VALUE
        val b = Int.MAX_VALUE

        for (move in moves) {
            val newState = applyMove(state, move)
            val score = minimax(newState, depth - 1, a, b, false)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            a = maxOf(a, score)
        }
        return bestMove
    }

    private fun minimax(state: ChessState, depth: Int, alpha: Int, beta: Int, maximizing: Boolean): Int {
        if (depth == 0 || state.gameOver) {
            val eval = evaluate(state)
            return if (maximizing) eval else -eval
        }

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

    fun evaluate(state: ChessState): Int {
        if (state.gameOver) {
            return when (state.winner) {
                "White" -> 100000
                "Black" -> -100000
                else -> 0
            }
        }

        var score = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board[row][col]
                if (piece.type == ChessPieceType.EMPTY) continue

                val materialValue = when (piece.type) {
                    ChessPieceType.PAWN -> 100
                    ChessPieceType.KNIGHT -> 320
                    ChessPieceType.BISHOP -> 330
                    ChessPieceType.ROOK -> 500
                    ChessPieceType.QUEEN -> 900
                    ChessPieceType.KING -> 20000
                    else -> 0
                }

                val posValue = when (piece.type) {
                    ChessPieceType.PAWN -> if (piece.isWhite) pawnEvalWhite[row][col] else pawnEvalWhite[7 - row][7 - col]
                    ChessPieceType.KNIGHT -> if (piece.isWhite) knightEval[row][col] else knightEval[7 - row][7 - col]
                    ChessPieceType.BISHOP -> if (piece.isWhite) bishopEval[row][col] else bishopEval[7 - row][7 - col]
                    ChessPieceType.ROOK -> if (piece.isWhite) rookEval[row][col] else rookEval[7 - row][7 - col]
                    ChessPieceType.QUEEN -> if (piece.isWhite) queenEval[row][col] else queenEval[7 - row][7 - col]
                    ChessPieceType.KING -> if (piece.isWhite) kingEval[row][col] else kingEval[7 - row][7 - col]
                    else -> 0
                }

                val total = materialValue + posValue
                score += if (piece.isWhite) total else -total
            }
        }
        return score
    }
}
