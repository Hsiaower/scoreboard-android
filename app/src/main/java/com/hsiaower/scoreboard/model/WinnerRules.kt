package com.hsiaower.scoreboard.model

object WinnerRules {
    fun determineWinner(
        team1Score: Int,
        team2Score: Int,
        settings: GameSettings,
    ): Team? {
        if (settings.winByTwo && settings.hardCapEnabled) {
            if (team1Score >= settings.hardCapScore && team1Score > team2Score) return Team.TEAM_1
            if (team2Score >= settings.hardCapScore && team2Score > team1Score) return Team.TEAM_2
        }

        if (!settings.winByTwo) {
            if (team1Score >= settings.winningScore && team1Score > team2Score) return Team.TEAM_1
            if (team2Score >= settings.winningScore && team2Score > team1Score) return Team.TEAM_2
            return null
        }

        if (team1Score >= settings.winningScore && team1Score - team2Score >= 2) return Team.TEAM_1
        if (team2Score >= settings.winningScore && team2Score - team1Score >= 2) return Team.TEAM_2
        return null
    }
}
