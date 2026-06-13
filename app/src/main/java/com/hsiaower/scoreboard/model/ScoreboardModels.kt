package com.hsiaower.scoreboard.model

enum class Team {
    TEAM_1,
    TEAM_2,
}

enum class RemoteAction(val label: String) {
    TEAM_1_PLUS("Left side +1"),
    TEAM_1_MINUS("Left side -1"),
    TEAM_2_PLUS("Right side +1"),
    TEAM_2_MINUS("Right side -1"),
    RESET("New match"),
}

enum class InputType(val label: String) {
    SINGLE_PRESS("Single press"),
    LONG_PRESS("Press and hold"),
    MULTI_BUTTON("Multi-button combination"),
    MULTI_BUTTON_HOLD("Hold multi-button combination"),
}

data class RemoteMapping(
    val keyCodes: Set<Int>,
    val displayName: String,
    val inputType: InputType = InputType.SINGLE_PRESS,
) {
    constructor(
        keyCode: Int,
        displayName: String,
        inputType: InputType = InputType.SINGLE_PRESS,
    ) : this(setOf(keyCode), displayName, inputType)
}

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

data class ScoreSnapshot(
    val team1Score: Int,
    val team2Score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isReset: Boolean = false,
)

data class TimeoutEvent(
    val team: Team,
    val team1Score: Int,
    val team2Score: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

data class SetTimeline(
    val number: Int,
    val team1Score: Int,
    val team2Score: Int,
    val winner: Team,
    val events: List<ScoreSnapshot>,
    val timeoutEvents: List<TimeoutEvent> = emptyList(),
)

data class MatchTimeline(
    val id: Long = System.currentTimeMillis(),
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val team1Name: String = "Home Team",
    val team2Name: String = "Away Team",
    val team1Sets: Int = 0,
    val team2Sets: Int = 0,
    val matchWinner: Team? = null,
    val completedSets: List<SetTimeline> = emptyList(),
    val currentSetEvents: List<ScoreSnapshot> = listOf(ScoreSnapshot(0, 0)),
    val currentSetTimeoutEvents: List<TimeoutEvent> = emptyList(),
) {
    val hasActivity: Boolean
        get() = completedSets.isNotEmpty() ||
            currentSetEvents.any { it.team1Score != 0 || it.team2Score != 0 } ||
            currentSetTimeoutEvents.isNotEmpty()
}

object MatchFinalizer {
    fun winnerFromScore(team1Score: Int, team2Score: Int): Team? = when {
        team1Score > team2Score -> Team.TEAM_1
        team2Score > team1Score -> Team.TEAM_2
        else -> null
    }

    fun setsAfterAwardingCurrentSet(
        winner: Team?,
        team1Sets: Int,
        team2Sets: Int,
    ): Pair<Int, Int> = when (winner) {
        Team.TEAM_1 -> team1Sets + 1 to team2Sets
        Team.TEAM_2 -> team1Sets to team2Sets + 1
        null -> team1Sets to team2Sets
    }
}

data class ScoreboardState(
    val settings: GameSettings = GameSettings(),
    val match: MatchState = MatchState(),
    val remoteMappings: Map<RemoteAction, RemoteMapping> = emptyMap(),
    val capturingAction: RemoteAction? = null,
    val capturingInputType: InputType? = null,
    val capturedKeyCodes: Set<Int> = emptySet(),
    val currentScreen: AppScreen = AppScreen.SCOREBOARD,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val rotationMessageVisible: Boolean = false,
    val currentTimeline: MatchTimeline = MatchTimeline(),
    val previousMatches: List<MatchTimeline> = emptyList(),
    val selectedMatchId: Long? = null,
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
    HISTORY,
    PREVIOUS_MATCHES,
    MATCH_HISTORY,
}
