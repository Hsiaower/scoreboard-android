package com.hsiaower.scoreboard

import android.app.Application
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hsiaower.scoreboard.data.SettingsRepository
import com.hsiaower.scoreboard.model.AppScreen
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.InputType
import com.hsiaower.scoreboard.model.MatchState
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.RemoteMapping
import com.hsiaower.scoreboard.model.ScoreRules
import com.hsiaower.scoreboard.model.ScoreValidationError
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.Team
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScoreboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val _state = MutableStateFlow(ScoreboardState())
    val state: StateFlow<ScoreboardState> = _state.asStateFlow()
    private val undoStack = ArrayDeque<MatchState>()
    private val redoStack = ArrayDeque<MatchState>()
    private var settingsLoaded = false

    init {
        viewModelScope.launch {
            repository.gameSettings.collect { settings ->
                _state.update { current ->
                    val firstScreen = if (!settingsLoaded && !settings.tutorialCompleted) {
                        AppScreen.TUTORIAL
                    } else {
                        current.currentScreen
                    }
                    settingsLoaded = true
                    current.copy(settings = settings, currentScreen = firstScreen)
                }
            }
        }
        viewModelScope.launch {
            repository.matchState.collect { match ->
                _state.update { it.copy(match = match) }
            }
        }
        viewModelScope.launch {
            repository.remoteMappings.collect { mappings ->
                _state.update { it.copy(remoteMappings = mappings) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val match = _state.value.match
                if (!match.timerRunning) continue
                val remaining = (match.timerSecondsRemaining - 1).coerceAtLeast(0)
                updateMatch(recordHistory = false) {
                    it.copy(
                        timerSecondsRemaining = remaining,
                        timerRunning = remaining > 0,
                    )
                }
            }
        }
    }

    fun adjustScore(team: Team, delta: Int) {
        updateMatch { current ->
            val scores = ScoreRules.adjust(
                team = team,
                delta = delta,
                team1Score = current.team1Score,
                team2Score = current.team2Score,
                settings = _state.value.settings,
            )
            current.copy(team1Score = scores.team1, team2Score = scores.team2)
        }
    }

    fun setScore(team: Team, newScore: Int): ScoreValidationError? {
        val current = _state.value
        val validationError = ScoreRules.validateManualScore(
            team = team,
            newScore = newScore,
            team1Score = current.match.team1Score,
            team2Score = current.match.team2Score,
            settings = current.settings,
        )
        if (validationError != null) return validationError

        updateMatch { match ->
            val scores = ScoreRules.set(
                team = team,
                newScore = newScore,
                team1Score = match.team1Score,
                team2Score = match.team2Score,
                settings = _state.value.settings,
            )
            match.copy(team1Score = scores.team1, team2Score = scores.team2)
        }
        return null
    }

    fun setSets(team: Team, value: Int) {
        updateMatch { match ->
            when (team) {
                Team.TEAM_1 -> match.copy(team1Sets = value.coerceIn(0, 99))
                Team.TEAM_2 -> match.copy(team2Sets = value.coerceIn(0, 99))
            }
        }
    }

    fun setTimeouts(team: Team, value: Int) {
        updateMatch { match ->
            when (team) {
                Team.TEAM_1 -> match.copy(team1Timeouts = value.coerceIn(0, 9))
                Team.TEAM_2 -> match.copy(team2Timeouts = value.coerceIn(0, 9))
            }
        }
    }

    fun startTimeout(team: Team) {
        val match = _state.value.match
        val remaining = if (team == Team.TEAM_1) match.team1Timeouts else match.team2Timeouts
        if (remaining <= 0) return
        updateMatch { current ->
            when (team) {
                Team.TEAM_1 -> current.copy(
                    team1Timeouts = current.team1Timeouts - 1,
                    timerSecondsRemaining = _state.value.settings.timeoutDurationSeconds,
                    timerRunning = true,
                )
                Team.TEAM_2 -> current.copy(
                    team2Timeouts = current.team2Timeouts - 1,
                    timerSecondsRemaining = _state.value.settings.timeoutDurationSeconds,
                    timerRunning = true,
                )
            }
        }
    }

    fun toggleTimer() {
        updateMatch(recordHistory = false) { match ->
            if (match.timerSecondsRemaining <= 0) {
                match.copy(
                    timerSecondsRemaining = _state.value.settings.timeoutDurationSeconds,
                    timerRunning = true,
                )
            } else {
                match.copy(timerRunning = !match.timerRunning)
            }
        }
    }

    fun clearScore() {
        updateMatch {
            it.copy(
                team1Score = 0,
                team2Score = 0,
                timerSecondsRemaining = 0,
                timerRunning = false,
            )
        }
    }

    fun newMatch() {
        undoStack.clear()
        redoStack.clear()
        val settings = _state.value.settings
        setMatch(
            MatchState(
                team1Timeouts = settings.timeoutsPerSet,
                team2Timeouts = settings.timeoutsPerSet,
                team1OnLeft = _state.value.match.team1OnLeft,
            ),
        )
    }

    fun switchSides() {
        updateMatch { it.copy(team1OnLeft = !it.team1OnLeft) }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.match)
        setMatch(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.match)
        setMatch(next)
    }

    fun showRotationPlaceholder() {
        _state.update { it.copy(rotationMessageVisible = true) }
        viewModelScope.launch {
            delay(2_000)
            _state.update { it.copy(rotationMessageVisible = false) }
        }
    }

    fun navigate(screen: AppScreen) {
        _state.update { it.copy(currentScreen = screen, capturingAction = null) }
    }

    fun saveSettings(settings: GameSettings) {
        viewModelScope.launch {
            repository.saveGameSettings(settings)
            _state.update { it.copy(settings = settings, currentScreen = AppScreen.SCOREBOARD) }
        }
    }

    fun completeTutorial() {
        val settings = _state.value.settings.copy(tutorialCompleted = true)
        _state.update { it.copy(settings = settings, currentScreen = AppScreen.SCOREBOARD) }
        viewModelScope.launch { repository.saveGameSettings(settings) }
    }

    fun restartTutorial() {
        navigate(AppScreen.TUTORIAL)
    }

    fun setTeamName(team: Team, name: String) {
        val normalizedName = name.trim().take(30)
        if (normalizedName.isBlank()) return
        val updatedSettings = when (team) {
            Team.TEAM_1 -> _state.value.settings.copy(team1Name = normalizedName)
            Team.TEAM_2 -> _state.value.settings.copy(team2Name = normalizedName)
        }
        _state.update { it.copy(settings = updatedSettings) }
        viewModelScope.launch { repository.saveGameSettings(updatedSettings) }
    }

    fun beginInputCapture(action: RemoteAction) {
        _state.update { it.copy(capturingAction = action) }
    }

    fun cancelInputCapture() {
        _state.update { it.copy(capturingAction = null) }
    }

    fun clearMapping(action: RemoteAction) {
        viewModelScope.launch { repository.clearRemoteMapping(action) }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false

        val capturingAction = _state.value.capturingAction
        if (capturingAction != null) {
            val mapping = RemoteMapping(
                keyCode = event.keyCode,
                displayName = KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_"),
                inputType = InputType.SINGLE_PRESS,
            )
            _state.update { it.copy(capturingAction = null) }
            viewModelScope.launch { repository.saveRemoteMapping(capturingAction, mapping) }
            return true
        }

        val action = _state.value.remoteMappings.entries.firstOrNull { (_, mapping) ->
            mapping.inputType == InputType.SINGLE_PRESS && mapping.keyCode == event.keyCode
        }?.key ?: return false

        when (action) {
            RemoteAction.TEAM_1_PLUS -> adjustScore(Team.TEAM_1, 1)
            RemoteAction.TEAM_1_MINUS -> adjustScore(Team.TEAM_1, -1)
            RemoteAction.TEAM_2_PLUS -> adjustScore(Team.TEAM_2, 1)
            RemoteAction.TEAM_2_MINUS -> adjustScore(Team.TEAM_2, -1)
            RemoteAction.RESET -> clearScore()
        }
        return true
    }

    private fun updateMatch(
        recordHistory: Boolean = true,
        transform: (MatchState) -> MatchState,
    ) {
        val before = _state.value.match
        val after = transform(before)
        if (after == before) return
        if (recordHistory) {
            undoStack.addLast(before)
            while (undoStack.size > 50) undoStack.removeFirst()
            redoStack.clear()
        }
        setMatch(after)
    }

    private fun setMatch(match: MatchState) {
        _state.update {
            it.copy(
                match = match,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
        viewModelScope.launch { repository.saveMatchState(match) }
    }
}
