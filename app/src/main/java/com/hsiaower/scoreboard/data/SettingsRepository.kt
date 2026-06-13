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
import com.hsiaower.scoreboard.model.MatchTimeline
import com.hsiaower.scoreboard.model.MatchState
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.RemoteMapping
import com.hsiaower.scoreboard.model.ScoreSnapshot
import com.hsiaower.scoreboard.model.SetTimeline
import com.hsiaower.scoreboard.model.Team
import com.hsiaower.scoreboard.model.TimeoutEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

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
            timerSecondsRemaining = 0,
            timerRunning = false,
        )
    }

    val remoteMappings: Flow<Map<RemoteAction, RemoteMapping>> =
        context.dataStore.data.map { preferences ->
            RemoteAction.entries.mapNotNull { action ->
                val keyCodes = preferences[keyCodesKey(action)]
                    ?.split(',')
                    ?.mapNotNull(String::toIntOrNull)
                    ?.toSet()
                    ?.takeIf { it.isNotEmpty() }
                    ?: preferences[keyCodeKey(action)]?.let(::setOf)
                    ?: return@mapNotNull null
                val displayName = preferences[keyNameKey(action)]
                    ?: keyCodes.joinToString(" + ") { "Key $it" }
                val typeName = preferences[inputTypeKey(action)] ?: InputType.SINGLE_PRESS.name
                val inputType = InputType.entries.firstOrNull { it.name == typeName }
                    ?: InputType.SINGLE_PRESS
                action to RemoteMapping(keyCodes, displayName, inputType)
            }.toMap()
        }

    val currentTimeline: Flow<MatchTimeline> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_TIMELINE]?.let(::decodeMatchTimeline) ?: MatchTimeline()
    }

    val previousMatches: Flow<List<MatchTimeline>> = context.dataStore.data.map { preferences ->
        preferences[PREVIOUS_MATCHES]?.let(::decodeMatchList) ?: emptyList()
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

    suspend fun saveCurrentTimeline(timeline: MatchTimeline) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_TIMELINE] = encodeMatchTimeline(timeline).toString()
        }
    }

    suspend fun savePreviousMatches(matches: List<MatchTimeline>) {
        context.dataStore.edit { preferences ->
            preferences[PREVIOUS_MATCHES] = JSONArray().apply {
                matches.take(50).forEach { put(encodeMatchTimeline(it)) }
            }.toString()
        }
    }

    suspend fun saveRemoteMapping(action: RemoteAction, mapping: RemoteMapping) {
        context.dataStore.edit { preferences ->
            preferences[keyCodesKey(action)] = mapping.keyCodes.sorted().joinToString(",")
            preferences[keyCodeKey(action)] = mapping.keyCodes.min()
            preferences[keyNameKey(action)] = mapping.displayName
            preferences[inputTypeKey(action)] = mapping.inputType.name
        }
    }

    suspend fun clearRemoteMapping(action: RemoteAction) {
        context.dataStore.edit { preferences ->
            preferences.remove(keyCodesKey(action))
            preferences.remove(keyCodeKey(action))
            preferences.remove(keyNameKey(action))
            preferences.remove(inputTypeKey(action))
        }
    }

    private fun keyCodesKey(action: RemoteAction): Preferences.Key<String> =
        stringPreferencesKey("remote_${action.name}_key_codes")

    private fun keyCodeKey(action: RemoteAction): Preferences.Key<Int> =
        intPreferencesKey("remote_${action.name}_key_code")

    private fun keyNameKey(action: RemoteAction): Preferences.Key<String> =
        stringPreferencesKey("remote_${action.name}_key_name")

    private fun inputTypeKey(action: RemoteAction): Preferences.Key<String> =
        stringPreferencesKey("remote_${action.name}_input_type")

    private fun normalizeTeamName(name: String?, defaultName: String): String =
        name?.trim()?.take(30)?.ifBlank { defaultName } ?: defaultName

    private fun encodeMatchTimeline(match: MatchTimeline): JSONObject = JSONObject().apply {
        put("id", match.id)
        put("startedAt", match.startedAt)
        put("endedAt", match.endedAt ?: JSONObject.NULL)
        put("team1Name", match.team1Name)
        put("team2Name", match.team2Name)
        put("team1Sets", match.team1Sets)
        put("team2Sets", match.team2Sets)
        put("completedSets", JSONArray().apply {
            match.completedSets.forEach { set ->
                put(JSONObject().apply {
                    put("number", set.number)
                    put("team1Score", set.team1Score)
                    put("team2Score", set.team2Score)
                    put("winner", set.winner.name)
                    put("events", encodeEvents(set.events))
                    put("timeoutEvents", encodeTimeoutEvents(set.timeoutEvents))
                })
            }
        })
        put("currentSetEvents", encodeEvents(match.currentSetEvents))
        put("currentSetTimeoutEvents", encodeTimeoutEvents(match.currentSetTimeoutEvents))
    }

    private fun encodeEvents(events: List<ScoreSnapshot>): JSONArray = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject().apply {
                put("team1Score", event.team1Score)
                put("team2Score", event.team2Score)
                put("timestamp", event.timestamp)
                put("isReset", event.isReset)
            })
        }
    }

    private fun encodeTimeoutEvents(events: List<TimeoutEvent>): JSONArray = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject().apply {
                put("team", event.team.name)
                put("team1Score", event.team1Score)
                put("team2Score", event.team2Score)
                put("timestamp", event.timestamp)
            })
        }
    }

    private fun decodeMatchList(value: String): List<MatchTimeline> = runCatching {
        val array = JSONArray(value)
        List(array.length()) { decodeMatchTimeline(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun decodeMatchTimeline(value: String): MatchTimeline = runCatching {
        decodeMatchTimeline(JSONObject(value))
    }.getOrDefault(MatchTimeline())

    private fun decodeMatchTimeline(json: JSONObject): MatchTimeline {
        val completedSetsJson = json.optJSONArray("completedSets") ?: JSONArray()
        val completedSets = List(completedSetsJson.length()) { index ->
            val set = completedSetsJson.getJSONObject(index)
            SetTimeline(
                number = set.optInt("number", index + 1),
                team1Score = set.optInt("team1Score"),
                team2Score = set.optInt("team2Score"),
                winner = Team.entries.firstOrNull { it.name == set.optString("winner") } ?: Team.TEAM_1,
                events = decodeEvents(set.optJSONArray("events")),
                timeoutEvents = decodeTimeoutEvents(set.optJSONArray("timeoutEvents")),
            )
        }
        return MatchTimeline(
            id = json.optLong("id", System.currentTimeMillis()),
            startedAt = json.optLong("startedAt", System.currentTimeMillis()),
            endedAt = json.optLong("endedAt").takeIf { !json.isNull("endedAt") && it > 0 },
            team1Name = normalizeTeamName(json.optString("team1Name"), "Home Team"),
            team2Name = normalizeTeamName(json.optString("team2Name"), "Away Team"),
            team1Sets = json.optInt("team1Sets"),
            team2Sets = json.optInt("team2Sets"),
            completedSets = completedSets,
            currentSetEvents = decodeEvents(json.optJSONArray("currentSetEvents"))
                .ifEmpty { listOf(ScoreSnapshot(0, 0)) },
            currentSetTimeoutEvents = decodeTimeoutEvents(json.optJSONArray("currentSetTimeoutEvents")),
        )
    }

    private fun decodeEvents(array: JSONArray?): List<ScoreSnapshot> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val event = array.getJSONObject(index)
            ScoreSnapshot(
                team1Score = event.optInt("team1Score"),
                team2Score = event.optInt("team2Score"),
                timestamp = event.optLong("timestamp", System.currentTimeMillis()),
                isReset = event.optBoolean("isReset", false),
            )
        }
    }

    private fun decodeTimeoutEvents(array: JSONArray?): List<TimeoutEvent> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val event = array.getJSONObject(index)
            TimeoutEvent(
                team = Team.entries.firstOrNull { it.name == event.optString("team") } ?: Team.TEAM_1,
                team1Score = event.optInt("team1Score"),
                team2Score = event.optInt("team2Score"),
                timestamp = event.optLong("timestamp", System.currentTimeMillis()),
            )
        }
    }

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
        val CURRENT_TIMELINE = stringPreferencesKey("current_timeline")
        val PREVIOUS_MATCHES = stringPreferencesKey("previous_matches")
    }
}
