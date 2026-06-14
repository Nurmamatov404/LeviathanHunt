package com.leviathan.game.boardgames

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

data class GameStats(
    val wins: Int,
    val losses: Int,
    val draws: Int
) {
    val total get() = wins + losses + draws
}

object PlayerStats {
    private const val PREFS_NAME = "board_game_stats"

    fun getCheckersStats(): GameStats = getStats("checkers")
    fun getChessStats(): GameStats = getStats("chess")

    fun recordCheckersWin() = record("checkers", "wins")
    fun recordCheckersLoss() = record("checkers", "losses")
    fun recordCheckersDraw() = record("checkers", "draws")
    fun recordChessWin() = record("chess", "wins")
    fun recordChessLoss() = record("chess", "losses")
    fun recordChessDraw() = record("chess", "draws")

    private fun getStats(game: String): GameStats {
        val prefs = prefs()
        return GameStats(
            wins = prefs.getInteger("${game}_wins", 0),
            losses = prefs.getInteger("${game}_losses", 0),
            draws = prefs.getInteger("${game}_draws", 0)
        )
    }

    private fun record(game: String, key: String) {
        val prefs = prefs()
        prefs.putInteger("${game}_${key}", prefs.getInteger("${game}_${key}", 0) + 1)
        prefs.flush()
    }

    private fun prefs(): Preferences = Gdx.app.getPreferences(PREFS_NAME)
}
