package com.hsiaower.scoreboard

import android.app.Application
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hsiaower.scoreboard.data.SettingsRepository
import com.hsiaower.scoreboard.model.AppScreen
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.InputType
import com.hsiaower.scoreboard.model.MatchTimeline
import com.hsiaower.scoreboard.model.MatchState
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.RemoteMapping
import com.hsiaower.scoreboard.model.ScoreRules
import com.hsiaower.scoreboard.model.ScoreSnapshot
import com.hsiaower.scoreboard.model.ScoreValidationError
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.SetTimeline
import com.hsiaower.scoreboard.model.Team
import com.hsiaower.scoreboard.model.TimeoutEvent
import com.hsiaower.scoreboard.model.WinnerRules
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScoreboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val _state = MutableStateFlow(ScoreboardState())
    val state: StateFlow<ScoreboardState> = _state.asStateFlow()
    private val undoStack = ArrayDeque<MatchState>()
    private val redoStack = ArrayDeque<MatchState>()
    private val pressedRemoteKeys = mutableMapOf<Int, Long>()
    private val consumedRemoteKeys = mutableSetOf<Int>()
    private val activeMultiButtonActions = mutableSetOf<RemoteAction>()
    private val longPressJobs = mutableMapOf<RemoteAction, Job>()
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
            val match = repository.matchState.first()
            _state.update { it.copy(match = match) }
        }
        viewModelScope.launch {
            repository.remoteMappings.collect { mappings ->
                _state.update { it.copy(remoteMappings = mappings) }
            }
        }
        viewModelScope.launch {
            repository.currentTimeline.collect { timeline ->
                _state.update { current ->
                    val normalized = if (!timeline.hasActivity) {
                        timeline.copy(
                            team1Name = current.settings.team1Name,
                            team2Name = current.settings.team2Name,
                        )
                    } else {
                        timeline
                    }
                    current.copy(currentTimeline = normalized)
                }
            }
        }
        viewModelScope.launch {
            repository.previousMatches.collect { matches ->
                _state.update { it.copy(previousMatches = matches) }
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
        if (_state.value.match.timerSecondsRemaining > 0) return
        updateMatch { current ->
            val scores = ScoreRules.adjust(
                team = team,
                delta = delta,
                team1Score = current.team1Score,
                team2Score = current.team2Score,
                settings = _state.value.settings,
            )
            val updated = current.copy(team1Score = scores.team1, team2Score = scores.team2)
            if (updated != current) recordScore(updated.team1Score, updated.team2Score)
            updated
        }
    }

    fun setScore(team: Team, newScore: Int): ScoreValidationError? {
        val current = _state.value
        if (current.match.timerSecondsRemaining > 0) return ScoreValidationError.TIMEOUT_ACTIVE
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
            val updated = match.copy(team1Score = scores.team1, team2Score = scores.team2)
            if (updated != match) recordScore(updated.team1Score, updated.team2Score)
            updated
        }
        return null
    }

    fun setSets(team: Team, value: Int) {
        updateMatch { match ->
            val updated = when (team) {
                Team.TEAM_1 -> match.copy(team1Sets = value.coerceIn(0, 99))
                Team.TEAM_2 -> match.copy(team2Sets = value.coerceIn(0, 99))
            }
            if (updated != match) {
                saveTimeline(
                    _state.value.currentTimeline.copy(
                        team1Sets = updated.team1Sets,
                        team2Sets = updated.team2Sets,
                    ),
                )
            }
            updated
        }
    }

    fun awardSet(team: Team) {
        val state = _state.value
        if (state.winner != team || state.matchWinner != null) return

        val finalMatch = state.match
        updateMatch { match ->
            val updatedSets = when (team) {
                Team.TEAM_1 -> match.copy(team1Sets = match.team1Sets + 1)
                Team.TEAM_2 -> match.copy(team2Sets = match.team2Sets + 1)
            }
            updatedSets.copy(
                team1Score = 0,
                team2Score = 0,
                team1Timeouts = state.settings.timeoutsPerSet,
                team2Timeouts = state.settings.timeoutsPerSet,
                timerSecondsRemaining = 0,
                timerRunning = false,
            )
        }
        val updatedState = _state.value
        val timeline = state.currentTimeline
        val recordedEvents = timeline.currentSetEvents.let { events ->
            if (events.lastOrNull()?.let {
                    it.team1Score == finalMatch.team1Score && it.team2Score == finalMatch.team2Score
                } == true
            ) {
                events
            } else {
                events + ScoreSnapshot(finalMatch.team1Score, finalMatch.team2Score)
            }
        }
        val winningIndex = recordedEvents.indexOfFirst { snapshot ->
            WinnerRules.determineWinner(
                team1Score = snapshot.team1Score,
                team2Score = snapshot.team2Score,
                settings = state.settings,
            ) == team
        }
        val finalEvents = if (winningIndex >= 0) recordedEvents.take(winningIndex + 1) else recordedEvents
        val winningScore = finalEvents.last()
        saveTimeline(
            timeline.copy(
                team1Name = state.settings.team1Name,
                team2Name = state.settings.team2Name,
                team1Sets = updatedState.match.team1Sets,
                team2Sets = updatedState.match.team2Sets,
                completedSets = timeline.completedSets + SetTimeline(
                    number = timeline.completedSets.size + 1,
                    team1Score = winningScore.team1Score,
                    team2Score = winningScore.team2Score,
                    winner = team,
                    events = finalEvents,
                    timeoutEvents = timeline.currentSetTimeoutEvents.filter {
                        it.timestamp <= winningScore.timestamp
                    },
                ),
                currentSetEvents = listOf(ScoreSnapshot(0, 0)),
                currentSetTimeoutEvents = emptyList(),
            ),
        )
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
        if (match.timerRunning || match.timerSecondsRemaining > 0) return
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
        recordTimeout(team, match.team1Score, match.team2Score)
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
            val updated = it.copy(
                team1Score = 0,
                team2Score = 0,
                timerSecondsRemaining = 0,
                timerRunning = false,
            )
            if (updated != it) recordScore(0, 0)
            updated
        }
    }

    fun newMatch() {
        archiveCurrentMatch()
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
        saveTimeline(
            MatchTimeline(
                team1Name = settings.team1Name,
                team2Name = settings.team2Name,
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
        recordScore(previous.team1Score, previous.team2Score)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.match)
        setMatch(next)
        recordScore(next.team1Score, next.team2Score)
    }

    fun showRotationPlaceholder() {
        _state.update { it.copy(rotationMessageVisible = true) }
        viewModelScope.launch {
            delay(2_000)
            _state.update { it.copy(rotationMessageVisible = false) }
        }
    }

    fun navigate(screen: AppScreen) {
        cancelInputCapture()
        releaseRemoteInputs()
        _state.update { it.copy(currentScreen = screen) }
    }

    fun openMatchHistory(matchId: Long) {
        _state.update {
            it.copy(
                selectedMatchId = matchId,
                currentScreen = AppScreen.MATCH_HISTORY,
            )
        }
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
        saveTimeline(
            _state.value.currentTimeline.copy(
                team1Name = updatedSettings.team1Name,
                team2Name = updatedSettings.team2Name,
            ),
        )
        viewModelScope.launch { repository.saveGameSettings(updatedSettings) }
    }

    fun beginInputCapture(action: RemoteAction, inputType: InputType) {
        _state.update {
            it.copy(
                capturingAction = action,
                capturingInputType = inputType,
                capturedKeyCodes = emptySet(),
            )
        }
    }

    fun cancelInputCapture() {
        _state.update {
            it.copy(
                capturingAction = null,
                capturingInputType = null,
                capturedKeyCodes = emptySet(),
            )
        }
    }

    fun clearMapping(action: RemoteAction) {
        viewModelScope.launch { repository.clearRemoteMapping(action) }
    }

    fun releaseRemoteInputs() {
        pressedRemoteKeys.clear()
        consumedRemoteKeys.clear()
        activeMultiButtonActions.clear()
        longPressJobs.values.forEach(Job::cancel)
        longPressJobs.clear()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false

        if (_state.value.capturingAction != null) {
            return captureRemoteInput(event)
        }
        if (_state.value.currentScreen != AppScreen.SCOREBOARD) return false

        val relevantMappings = _state.value.remoteMappings.filterValues {
            event.keyCode in it.keyCodes
        }
        if (relevantMappings.isEmpty()) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return true
                pressedRemoteKeys[event.keyCode] = event.eventTime

                relevantMappings.filterValues { it.inputType == InputType.LONG_PRESS }
                    .forEach { (action, mapping) ->
                        scheduleHeldAction(action, mapping)
                    }

                val completedCombination = _state.value.remoteMappings
                    .asSequence()
                    .filter { (action, mapping) ->
                        mapping.inputType in MULTI_BUTTON_INPUT_TYPES &&
                            action !in activeMultiButtonActions &&
                            mapping.keyCodes.all(pressedRemoteKeys::containsKey) &&
                            mapping.keyCodes.none(consumedRemoteKeys::contains) &&
                            isWithinCombinationWindow(mapping.keyCodes)
                    }
                    .maxByOrNull { (_, mapping) -> mapping.keyCodes.size }
                if (completedCombination != null) {
                    val (action, mapping) = completedCombination
                    activeMultiButtonActions += action
                    consumedRemoteKeys += mapping.keyCodes
                    cancelLongPressesFor(mapping.keyCodes)
                    if (mapping.inputType == InputType.MULTI_BUTTON_HOLD) {
                        scheduleHeldAction(action, mapping)
                    } else {
                        performRemoteAction(action)
                    }
                }
            }

            KeyEvent.ACTION_UP -> {
                // Resolve taps on release so a hold or chord can claim the keys first.
                val wasConsumed = event.keyCode in consumedRemoteKeys
                if (!wasConsumed) {
                    relevantMappings
                        .filterValues { it.inputType == InputType.SINGLE_PRESS }
                        .keys
                        .firstOrNull()
                        ?.let(::performRemoteAction)
                }

                relevantMappings.filterValues {
                    it.inputType == InputType.LONG_PRESS ||
                        it.inputType == InputType.MULTI_BUTTON_HOLD
                }
                    .keys
                    .forEach { action -> longPressJobs.remove(action)?.cancel() }
                pressedRemoteKeys -= event.keyCode
                consumedRemoteKeys -= event.keyCode
                activeMultiButtonActions.removeAll { action ->
                    _state.value.remoteMappings[action]
                        ?.keyCodes
                        ?.all(pressedRemoteKeys::containsKey) != true
                }
            }

            else -> return false
        }
        return true
    }

    private fun scheduleHeldAction(action: RemoteAction, mapping: RemoteMapping) {
        longPressJobs[action]?.cancel()
        longPressJobs[action] = viewModelScope.launch {
            delay(LONG_PRESS_DURATION_MS)
            if (
                mapping.keyCodes.all(pressedRemoteKeys::containsKey) &&
                (
                    mapping.inputType == InputType.MULTI_BUTTON_HOLD ||
                        mapping.keyCodes.none(consumedRemoteKeys::contains)
                    )
            ) {
                consumedRemoteKeys += mapping.keyCodes
                cancelLongPressesFor(mapping.keyCodes, except = action)
                performRemoteAction(action)
            }
        }
    }

    private fun isWithinCombinationWindow(keyCodes: Set<Int>): Boolean {
        val times = keyCodes.mapNotNull(pressedRemoteKeys::get)
        return times.size == keyCodes.size &&
            (times.max() - times.min()) <= MULTI_BUTTON_WINDOW_MS
    }

    private fun cancelLongPressesFor(
        keyCodes: Set<Int>,
        except: RemoteAction? = null,
    ) {
        val affectedActions = longPressJobs.keys.filter { action ->
            action != except &&
                _state.value.remoteMappings[action]?.keyCodes?.any(keyCodes::contains) == true
        }
        affectedActions.forEach { action -> longPressJobs.remove(action)?.cancel() }
    }

    private fun captureRemoteInput(event: KeyEvent): Boolean {
        val state = _state.value
        val action = state.capturingAction ?: return false
        val inputType = state.capturingInputType ?: InputType.SINGLE_PRESS
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return true
                if (inputType in MULTI_BUTTON_INPUT_TYPES) {
                    _state.update { it.copy(capturedKeyCodes = it.capturedKeyCodes + event.keyCode) }
                } else {
                    saveRemoteMapping(action, inputType, setOf(event.keyCode))
                }
            }

            KeyEvent.ACTION_UP -> {
                if (inputType in MULTI_BUTTON_INPUT_TYPES) {
                    val capturedKeys = _state.value.capturedKeyCodes
                    if (capturedKeys.size >= 2) {
                        saveRemoteMapping(action, inputType, capturedKeys)
                    } else {
                        _state.update { it.copy(capturedKeyCodes = emptySet()) }
                    }
                }
            }

            else -> return false
        }
        return true
    }

    private fun saveRemoteMapping(
        action: RemoteAction,
        inputType: InputType,
        keyCodes: Set<Int>,
    ) {
        val orderedCodes = keyCodes.sorted()
        val mapping = RemoteMapping(
            keyCodes = orderedCodes.toSet(),
            displayName = orderedCodes.joinToString(" + ") { keyCode ->
                KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            },
            inputType = inputType,
        )
        cancelInputCapture()
        viewModelScope.launch { repository.saveRemoteMapping(action, mapping) }
    }

    private fun performRemoteAction(action: RemoteAction) {
        if (_state.value.currentScreen != AppScreen.SCOREBOARD) return
        val team1OnLeft = _state.value.match.team1OnLeft
        val leftTeam = if (team1OnLeft) Team.TEAM_1 else Team.TEAM_2
        val rightTeam = if (team1OnLeft) Team.TEAM_2 else Team.TEAM_1
        when (action) {
            RemoteAction.TEAM_1_PLUS -> adjustScore(leftTeam, 1)
            RemoteAction.TEAM_1_MINUS -> adjustScore(leftTeam, -1)
            RemoteAction.TEAM_2_PLUS -> adjustScore(rightTeam, 1)
            RemoteAction.TEAM_2_MINUS -> adjustScore(rightTeam, -1)
            RemoteAction.RESET -> clearScore()
        }
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

    private fun recordScore(team1Score: Int, team2Score: Int) {
        val timeline = _state.value.currentTimeline
        val last = timeline.currentSetEvents.lastOrNull()
        if (last?.team1Score == team1Score && last.team2Score == team2Score) return
        saveTimeline(
            timeline.copy(
                team1Name = _state.value.settings.team1Name,
                team2Name = _state.value.settings.team2Name,
                currentSetEvents = timeline.currentSetEvents + ScoreSnapshot(team1Score, team2Score),
            ),
        )
    }

    private companion object {
        const val LONG_PRESS_DURATION_MS = 600L
        const val MULTI_BUTTON_WINDOW_MS = 350L
        val MULTI_BUTTON_INPUT_TYPES = setOf(
            InputType.MULTI_BUTTON,
            InputType.MULTI_BUTTON_HOLD,
        )
    }

    private fun recordTimeout(team: Team, team1Score: Int, team2Score: Int) {
        val timeline = _state.value.currentTimeline
        saveTimeline(
            timeline.copy(
                team1Name = _state.value.settings.team1Name,
                team2Name = _state.value.settings.team2Name,
                currentSetTimeoutEvents = timeline.currentSetTimeoutEvents + TimeoutEvent(
                    team = team,
                    team1Score = team1Score,
                    team2Score = team2Score,
                ),
            ),
        )
    }

    private fun archiveCurrentMatch() {
        val state = _state.value
        val timeline = state.currentTimeline
        if (!timeline.hasActivity) return
        val archived = timeline.copy(
            endedAt = System.currentTimeMillis(),
            team1Name = state.settings.team1Name,
            team2Name = state.settings.team2Name,
            team1Sets = state.match.team1Sets,
            team2Sets = state.match.team2Sets,
        )
        val matches = listOf(archived) + state.previousMatches.filterNot { it.id == archived.id }
        _state.update { it.copy(previousMatches = matches) }
        viewModelScope.launch { repository.savePreviousMatches(matches) }
    }

    private fun saveTimeline(timeline: MatchTimeline) {
        _state.update { it.copy(currentTimeline = timeline) }
        viewModelScope.launch { repository.saveCurrentTimeline(timeline) }
    }
}
