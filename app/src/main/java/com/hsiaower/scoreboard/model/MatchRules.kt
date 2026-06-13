package com.hsiaower.scoreboard.model

object MatchRules {
    fun determineWinner(
        team1Sets: Int,
        team2Sets: Int,
        setsToWin: Int,
    ): Team? {
        val target = setsToWin.coerceAtLeast(1)
        if (team1Sets >= target && team1Sets > team2Sets) return Team.TEAM_1
        if (team2Sets >= target && team2Sets > team1Sets) return Team.TEAM_2
        return null
    }
}
