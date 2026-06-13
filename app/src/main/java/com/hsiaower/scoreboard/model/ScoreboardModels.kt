package com.hsiaower.scoreboard.model

enum class Team {
    TEAM_1,
    TEAM_2,
}

enum class RemoteAction(val label: String) {
    TEAM_1_PLUS("Team 1 +1"),
    TEAM_1_MINUS("Team 1 -1"),
    TEAM_2_PLUS("Team 2 +1"),
    TEAM_2_MINUS("Team 2 -1"),
    RESET("Reset"),
}

enum class InputType(val label: String) {
    SINGLE_PRESS("Single press"),
    DOUBLE_PRESS("Double press (placeholder)"),
    LONG_PRESS("Long press (placeholder)"),
}

data class RemoteMapping(
    val keyCode: Int,
    val displayName: String,
    val inputType: InputType = InputType.SINGLE_PRESS,
)

data class GameSettings(
    val winningScore: Int = 25,
    val winByTwo: Boolean = true,
    val hardCapEnabled: Boolean = false,
    val hardCapScore: Int = 30,
    val setsToWin: Int = 2,
    val timeoutsPerSet: Int = 2,
    val timeoutDurationSeconds: Int = 30,
    val team1Name: String = "Home Team",
    val team2Name: String = "Away Team",
    val tutorialCompleted: Boolean = false,
)

data class MatchState(
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val team1Sets: Int = 0,
    val team2Sets: Int = 0,
    val team1Timeouts: Int = 2,
    val team2Timeouts: Int = 2,
    val team1OnLeft: Boolean = true,
    val timerSecondsRemaining: Int = 0,
    val timerRunning: Boolean = false,
)

data class ScoreboardState(
    val settings: GameSettings = GameSettings(),
    val match: MatchState = MatchState(),
    val remoteMappings: Map<RemoteAction, RemoteMapping> = emptyMap(),
    val capturingAction: RemoteAction? = null,
    val currentScreen: AppScreen = AppScreen.SCOREBOARD,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val rotationMessageVisible: Boolean = false,
) {
    val team1Score: Int get() = match.team1Score
    val team2Score: Int get() = match.team2Score
    val winner: Team?
        get() = WinnerRules.determineWinner(team1Score, team2Score, settings)
    val matchWinner: Team?
        get() = MatchRules.determineWinner(
            team1Sets = match.team1Sets,
            team2Sets = match.team2Sets,
            setsToWin = settings.setsToWin,
        )
}

enum class AppScreen {
    SCOREBOARD,
    SETTINGS,
    REMOTE_MAPPING,
    TUTORIAL,
}
