package com.hsiaower.scoreboard.model

enum class ScoreValidationError {
    NEGATIVE,
    ABOVE_HARD_CAP,
}

data class Scores(
    val team1: Int,
    val team2: Int,
)

object ScoreRules {
    fun adjust(
        team: Team,
        delta: Int,
        team1Score: Int,
        team2Score: Int,
        settings: GameSettings,
    ): Scores {
        val currentWinner = WinnerRules.determineWinner(team1Score, team2Score, settings)
        if (delta > 0 && currentWinner == team) {
            return Scores(team1Score, team2Score)
        }

        val currentScore = if (team == Team.TEAM_1) team1Score else team2Score
        var updatedScore = (currentScore + delta).coerceAtLeast(0)
        if (delta > 0 && settings.winByTwo && settings.hardCapEnabled) {
            updatedScore = updatedScore.coerceAtMost(settings.hardCapScore)
        }

        return when (team) {
            Team.TEAM_1 -> Scores(updatedScore, team2Score)
            Team.TEAM_2 -> Scores(team1Score, updatedScore)
        }
    }

    fun validateManualScore(
        team: Team,
        newScore: Int,
        team1Score: Int,
        team2Score: Int,
        settings: GameSettings,
    ): ScoreValidationError? {
        if (newScore < 0) return ScoreValidationError.NEGATIVE
        if (settings.winByTwo && settings.hardCapEnabled && newScore > settings.hardCapScore) {
            return ScoreValidationError.ABOVE_HARD_CAP
        }

        return null
    }

    fun set(
        team: Team,
        newScore: Int,
        team1Score: Int,
        team2Score: Int,
        settings: GameSettings,
    ): Scores {
        require(
            validateManualScore(team, newScore, team1Score, team2Score, settings) == null,
        )

        return when (team) {
            Team.TEAM_1 -> Scores(newScore, team2Score)
            Team.TEAM_2 -> Scores(team1Score, newScore)
        }
    }
}
