package com.hsiaower.scoreboard.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.InputType
import com.hsiaower.scoreboard.model.MatchState
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.RemoteMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "scoreboard_settings")

class SettingsRepository(private val context: Context) {
    val gameSettings: Flow<GameSettings> = context.dataStore.data.map { preferences ->
        val winByTwo = preferences[WIN_BY_TWO] ?: true
        GameSettings(
            winningScore = preferences[WINNING_SCORE] ?: 25,
            winByTwo = winByTwo,
            hardCapEnabled = winByTwo && (preferences[HARD_CAP_ENABLED] ?: false),
            hardCapScore = preferences[HARD_CAP_SCORE] ?: 30,
            setsToWin = preferences[SETS_TO_WIN] ?: 2,
            timeoutsPerSet = preferences[TIMEOUTS_PER_SET] ?: 2,
            timeoutDurationSeconds = preferences[TIMEOUT_DURATION] ?: 30,
            team1Name = normalizeTeamName(preferences[TEAM_1_NAME], "Home Team"),
            team2Name = normalizeTeamName(preferences[TEAM_2_NAME], "Away Team"),
            tutorialCompleted = preferences[TUTORIAL_COMPLETED] ?: false,
        )
    }

    val matchState: Flow<MatchState> = context.dataStore.data.map { preferences ->
        val defaultTimeouts = preferences[TIMEOUTS_PER_SET] ?: 2
        MatchState(
            team1Score = preferences[TEAM_1_SCORE] ?: 0,
            team2Score = preferences[TEAM_2_SCORE] ?: 0,
            team1Sets = preferences[TEAM_1_SETS] ?: 0,
            team2Sets = preferences[TEAM_2_SETS] ?: 0,
            team1Timeouts = preferences[TEAM_1_TIMEOUTS] ?: defaultTimeouts,
            team2Timeouts = preferences[TEAM_2_TIMEOUTS] ?: defaultTimeouts,
            team1OnLeft = preferences[TEAM_1_ON_LEFT] ?: true,
            timerSecondsRemaining = preferences[TIMER_SECONDS] ?: 0,
            timerRunning = false,
        )
    }

    val remoteMappings: Flow<Map<RemoteAction, RemoteMapping>> =
        context.dataStore.data.map { preferences ->
            RemoteAction.entries.mapNotNull { action ->
                val keyCode = preferences[keyCodeKey(action)] ?: return@mapNotNull null
                val displayName = preferences[keyNameKey(action)] ?: "Key $keyCode"
                val typeName = preferences[inputTypeKey(action)] ?: InputType.SINGLE_PRESS.name
                val inputType = InputType.entries.firstOrNull { it.name == typeName }
                    ?: InputType.SINGLE_PRESS
                action to RemoteMapping(keyCode, displayName, inputType)
            }.toMap()
        }

    suspend fun saveGameSettings(settings: GameSettings) {
        context.dataStore.edit { preferences ->
            preferences[WINNING_SCORE] = settings.winningScore.coerceAtLeast(1)
            preferences[WIN_BY_TWO] = settings.winByTwo
            preferences[HARD_CAP_ENABLED] = settings.winByTwo && settings.hardCapEnabled
            preferences[HARD_CAP_SCORE] = settings.hardCapScore.coerceAtLeast(1)
            preferences[SETS_TO_WIN] = settings.setsToWin.coerceIn(1, 9)
            preferences[TIMEOUTS_PER_SET] = settings.timeoutsPerSet.coerceIn(0, 9)
            preferences[TIMEOUT_DURATION] = settings.timeoutDurationSeconds.coerceIn(1, 600)
            preferences[TEAM_1_NAME] = normalizeTeamName(settings.team1Name, "Home Team")
            preferences[TEAM_2_NAME] = normalizeTeamName(settings.team2Name, "Away Team")
            preferences[TUTORIAL_COMPLETED] = settings.tutorialCompleted
        }
    }

    suspend fun saveMatchState(match: MatchState) {
        context.dataStore.edit { preferences ->
            preferences[TEAM_1_SCORE] = match.team1Score.coerceAtLeast(0)
            preferences[TEAM_2_SCORE] = match.team2Score.coerceAtLeast(0)
            preferences[TEAM_1_SETS] = match.team1Sets.coerceAtLeast(0)
            preferences[TEAM_2_SETS] = match.team2Sets.coerceAtLeast(0)
            preferences[TEAM_1_TIMEOUTS] = match.team1Timeouts.coerceAtLeast(0)
            preferences[TEAM_2_TIMEOUTS] = match.team2Timeouts.coerceAtLeast(0)
            preferences[TEAM_1_ON_LEFT] = match.team1OnLeft
            preferences[TIMER_SECONDS] = match.timerSecondsRemaining.coerceAtLeast(0)
        }
    }

    suspend fun saveRemoteMapping(action: RemoteAction, mapping: RemoteMapping) {
        context.dataStore.edit { preferences ->
            preferences[keyCodeKey(action)] = mapping.keyCode
            preferences[keyNameKey(action)] = mapping.displayName
            preferences[inputTypeKey(action)] = mapping.inputType.name
        }
    }

    suspend fun clearRemoteMapping(action: RemoteAction) {
        context.dataStore.edit { preferences ->
            preferences.remove(keyCodeKey(action))
            preferences.remove(keyNameKey(action))
            preferences.remove(inputTypeKey(action))
        }
    }

    private fun keyCodeKey(action: RemoteAction): Preferences.Key<Int> =
        intPreferencesKey("remote_${action.name}_key_code")

    private fun keyNameKey(action: RemoteAction): Preferences.Key<String> =
        stringPreferencesKey("remote_${action.name}_key_name")

    private fun inputTypeKey(action: RemoteAction): Preferences.Key<String> =
        stringPreferencesKey("remote_${action.name}_input_type")

    private fun normalizeTeamName(name: String?, defaultName: String): String =
        name?.trim()?.take(30)?.ifBlank { defaultName } ?: defaultName

    private companion object {
        val WINNING_SCORE = intPreferencesKey("winning_score")
        val WIN_BY_TWO = booleanPreferencesKey("win_by_two")
        val HARD_CAP_ENABLED = booleanPreferencesKey("hard_cap_enabled")
        val HARD_CAP_SCORE = intPreferencesKey("hard_cap_score")
        val SETS_TO_WIN = intPreferencesKey("sets_to_win")
        val TIMEOUTS_PER_SET = intPreferencesKey("timeouts_per_set")
        val TIMEOUT_DURATION = intPreferencesKey("timeout_duration_seconds")
        val TEAM_1_NAME = stringPreferencesKey("team_1_name")
        val TEAM_2_NAME = stringPreferencesKey("team_2_name")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val TEAM_1_SCORE = intPreferencesKey("team_1_score")
        val TEAM_2_SCORE = intPreferencesKey("team_2_score")
        val TEAM_1_SETS = intPreferencesKey("team_1_sets")
        val TEAM_2_SETS = intPreferencesKey("team_2_sets")
        val TEAM_1_TIMEOUTS = intPreferencesKey("team_1_timeouts")
        val TEAM_2_TIMEOUTS = intPreferencesKey("team_2_timeouts")
        val TEAM_1_ON_LEFT = booleanPreferencesKey("team_1_on_left")
        val TIMER_SECONDS = intPreferencesKey("timer_seconds")
    }
}
