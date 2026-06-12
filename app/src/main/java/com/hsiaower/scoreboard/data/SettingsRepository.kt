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

    private companion object {
        val WINNING_SCORE = intPreferencesKey("winning_score")
        val WIN_BY_TWO = booleanPreferencesKey("win_by_two")
        val HARD_CAP_ENABLED = booleanPreferencesKey("hard_cap_enabled")
        val HARD_CAP_SCORE = intPreferencesKey("hard_cap_score")
    }
}
