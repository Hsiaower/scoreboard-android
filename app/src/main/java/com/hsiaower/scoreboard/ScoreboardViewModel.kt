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
            when (team) {
                Team.TEAM_1 -> current.copy(team1Score = (current.team1Score + delta).coerceAtLeast(0))
                Team.TEAM_2 -> current.copy(team2Score = (current.team2Score + delta).coerceAtLeast(0))
            }
        }
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
