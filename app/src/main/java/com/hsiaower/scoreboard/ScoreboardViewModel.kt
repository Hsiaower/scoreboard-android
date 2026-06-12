package com.hsiaower.scoreboard

import android.app.Application
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hsiaower.scoreboard.data.SettingsRepository
import com.hsiaower.scoreboard.model.AppScreen
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.InputType
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.RemoteMapping
import com.hsiaower.scoreboard.model.ScoreRules
import com.hsiaower.scoreboard.model.ScoreValidationError
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScoreboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val _state = MutableStateFlow(ScoreboardState())
    val state: StateFlow<ScoreboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.gameSettings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            repository.remoteMappings.collect { mappings ->
                _state.update { it.copy(remoteMappings = mappings) }
            }
        }
    }

    fun adjustScore(team: Team, delta: Int) {
        _state.update { current ->
            val scores = ScoreRules.adjust(
                team = team,
                delta = delta,
                team1Score = current.team1Score,
                team2Score = current.team2Score,
                settings = current.settings,
            )
            current.copy(team1Score = scores.team1, team2Score = scores.team2)
        }
    }

    fun setScore(team: Team, newScore: Int): ScoreValidationError? {
        val current = _state.value
        val validationError = ScoreRules.validateManualScore(
            team = team,
            newScore = newScore,
            team1Score = current.team1Score,
            team2Score = current.team2Score,
            settings = current.settings,
        )
        if (validationError != null) return validationError

        _state.update { state ->
            val scores = ScoreRules.set(
                team = team,
                newScore = newScore,
                team1Score = state.team1Score,
                team2Score = state.team2Score,
                settings = state.settings,
            )
            state.copy(team1Score = scores.team1, team2Score = scores.team2)
        }
        return null
    }

    fun resetScores() {
        _state.update { it.copy(team1Score = 0, team2Score = 0) }
    }

    fun navigate(screen: AppScreen) {
        _state.update { it.copy(currentScreen = screen, capturingAction = null) }
    }

    fun saveSettings(settings: GameSettings) {
        viewModelScope.launch {
            repository.saveGameSettings(settings)
            navigate(AppScreen.SCOREBOARD)
        }
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

        performRemoteAction(action)
        return true
    }

    private fun performRemoteAction(action: RemoteAction) {
        when (action) {
            RemoteAction.TEAM_1_PLUS -> adjustScore(Team.TEAM_1, 1)
            RemoteAction.TEAM_1_MINUS -> adjustScore(Team.TEAM_1, -1)
            RemoteAction.TEAM_2_PLUS -> adjustScore(Team.TEAM_2, 1)
            RemoteAction.TEAM_2_MINUS -> adjustScore(Team.TEAM_2, -1)
            RemoteAction.RESET -> resetScores()
        }
    }
}
