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
)

data class ScoreboardState(
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val settings: GameSettings = GameSettings(),
    val remoteMappings: Map<RemoteAction, RemoteMapping> = emptyMap(),
    val capturingAction: RemoteAction? = null,
    val currentScreen: AppScreen = AppScreen.SCOREBOARD,
) {
    val winner: Team?
        get() = WinnerRules.determineWinner(team1Score, team2Score, settings)
}

enum class AppScreen {
    SCOREBOARD,
    SETTINGS,
    REMOTE_MAPPING,
}
