package com.hsiaower.scoreboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hsiaower.scoreboard.ScoreboardViewModel
import com.hsiaower.scoreboard.model.AppScreen
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.Team
import kotlin.math.abs

private val Team1Blue = Color(0xFF1D4ED8)
private val Team2Red = Color(0xFFB91C1C)
private val AppBackground = Color(0xFF111827)

@Composable
fun ScoreboardApp(viewModel: ScoreboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            when (state.currentScreen) {
                AppScreen.SCOREBOARD -> ScoreboardScreen(
                    state = state,
                    onAdjust = viewModel::adjustScore,
                    onReset = viewModel::resetScores,
                    onSettings = { viewModel.navigate(AppScreen.SETTINGS) },
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    onSave = viewModel::saveSettings,
                    onBack = { viewModel.navigate(AppScreen.SCOREBOARD) },
                    onRemoteMappings = { viewModel.navigate(AppScreen.REMOTE_MAPPING) },
                )

                AppScreen.REMOTE_MAPPING -> RemoteMappingScreen(
                    state = state,
                    onSetInput = viewModel::beginInputCapture,
                    onClear = viewModel::clearMapping,
                    onCancelCapture = viewModel::cancelInputCapture,
                    onBack = { viewModel.navigate(AppScreen.SETTINGS) },
                )
            }
        }
    }
}

@Composable
private fun ScoreboardScreen(
    state: ScoreboardState,
    onAdjust: (Team, Int) -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    teamName = "Team 1",
                    score = state.team1Score,
                    color = Team1Blue,
                    isWinner = state.winner == Team.TEAM_1,
                    onAdjust = { onAdjust(Team.TEAM_1, it) },
                    landscape = true,
                )
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    teamName = "Team 2",
                    score = state.team2Score,
                    color = Team2Red,
                    isWinner = state.winner == Team.TEAM_2,
                    onAdjust = { onAdjust(Team.TEAM_2, it) },
                    landscape = true,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    teamName = "Team 1",
                    score = state.team1Score,
                    color = Team1Blue,
                    isWinner = state.winner == Team.TEAM_1,
                    onAdjust = { onAdjust(Team.TEAM_1, it) },
                    landscape = false,
                )
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    teamName = "Team 2",
                    score = state.team2Score,
                    color = Team2Red,
                    isWinner = state.winner == Team.TEAM_2,
                    onAdjust = { onAdjust(Team.TEAM_2, it) },
                    landscape = false,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ControlButton(text = "Reset", onClick = onReset)
            ControlButton(text = "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun TeamZone(
    modifier: Modifier,
    teamName: String,
    score: Int,
    color: Color,
    isWinner: Boolean,
    onAdjust: (Int) -> Unit,
    landscape: Boolean,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val winnerBorder = if (isWinner) Modifier.border(10.dp, Color(0xFFFFD54F)) else Modifier

    Box(
        modifier = modifier
            .then(winnerBorder)
            .background(color)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragDistance += dragAmount
                    },
                    onDragEnd = {
                        if (abs(dragDistance) >= 48f) {
                            onAdjust(if (dragDistance < 0f) 1 else -1)
                        }
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = teamName,
                color = Color.White,
                fontSize = if (landscape) 34.sp else 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = score.toString(),
                color = Color.White,
                fontSize = if (landscape) 150.sp else 110.sp,
                lineHeight = if (landscape) 160.sp else 120.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = if (isWinner) "WINNER" else "Swipe up +1  /  down -1",
                color = if (isWinner) Color(0xFFFFE082) else Color.White.copy(alpha = 0.82f),
                fontSize = if (landscape) 22.sp else 16.sp,
                fontWeight = if (isWinner) FontWeight.Black else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(
    settings: GameSettings,
    onSave: (GameSettings) -> Unit,
    onBack: () -> Unit,
    onRemoteMappings: () -> Unit,
) {
    var winningScore by remember(settings) { mutableStateOf(settings.winningScore.toString()) }
    var winByTwo by remember(settings) { mutableStateOf(settings.winByTwo) }
    var hardCapEnabled by remember(settings) { mutableStateOf(settings.hardCapEnabled) }
    var hardCapScore by remember(settings) { mutableStateOf(settings.hardCapScore.toString()) }
    val parsedWinningScore = winningScore.toIntOrNull()?.takeIf { it > 0 }
    val parsedHardCapScore = hardCapScore.toIntOrNull()?.takeIf { it > 0 }
    val canSave = parsedWinningScore != null && parsedHardCapScore != null

    SettingsPage(title = "Game Settings", onBack = onBack) {
        NumberField(
            label = "Winning score",
            value = winningScore,
            onValueChange = { winningScore = it.filter(Char::isDigit) },
        )
        CheckRow(
            label = "Win by 2",
            checked = winByTwo,
            onCheckedChange = { winByTwo = it },
        )
        CheckRow(
            label = "Hard cap enabled",
            checked = hardCapEnabled,
            onCheckedChange = { hardCapEnabled = it },
        )
        NumberField(
            label = "Hard cap score",
            value = hardCapScore,
            enabled = hardCapEnabled,
            onValueChange = { hardCapScore = it.filter(Char::isDigit) },
        )
        Button(
            onClick = {
                onSave(
                    GameSettings(
                        winningScore = parsedWinningScore ?: 25,
                        winByTwo = winByTwo,
                        hardCapEnabled = hardCapEnabled,
                        hardCapScore = parsedHardCapScore ?: 30,
                    ),
                )
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save settings")
        }
        OutlinedButton(onClick = onRemoteMappings, modifier = Modifier.fillMaxWidth()) {
            Text("Remote Mapping")
        }
        FutureFeaturesCard()
    }
}

@Composable
private fun RemoteMappingScreen(
    state: ScoreboardState,
    onSetInput: (RemoteAction) -> Unit,
    onClear: (RemoteAction) -> Unit,
    onCancelCapture: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsPage(title = "Remote Mapping", onBack = onBack) {
        Text(
            "Map keyboard or gamepad buttons. Single press is active; double and long press are model placeholders.",
            color = Color(0xFF374151),
        )
        RemoteAction.entries.forEach { action ->
            val mapping = state.remoteMappings[action]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(action.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        mapping?.let { "${it.displayName} - ${it.inputType.label}" } ?: "Not mapped",
                        color = Color(0xFF4B5563),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSetInput(action) }) {
                            Text("Set input")
                        }
                        if (mapping != null) {
                            OutlinedButton(onClick = { onClear(action) }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }
    }

    state.capturingAction?.let { action ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(modifier = Modifier.padding(24.dp).widthIn(max = 420.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Press a remote button", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Listening for ${action.label}",
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(onClick = onCancelCapture) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
        content()
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 18.sp)
    }
}

@Composable
private fun FutureFeaturesCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Future features", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Switch sides - Match/set history - Rename teams - Change colours - " +
                    "Share live scores - Undo last action",
                color = Color(0xFF6B7280),
            )
        }
    }
}
