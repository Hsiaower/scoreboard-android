package com.hsiaower.scoreboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import com.hsiaower.scoreboard.ScoreboardViewModel
import com.hsiaower.scoreboard.R
import com.hsiaower.scoreboard.model.AppScreen
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.ScoreValidationError
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.Team
import kotlinx.coroutines.delay
import kotlin.math.abs

private val Team1Blue = Color(0xFF1D4ED8)
private val Team2Red = Color(0xFFB91C1C)
private val AppBackground = Color(0xFF111827)

@Composable
fun ScoreboardApp(viewModel: ScoreboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showOnboardingHint by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2_000)
        showOnboardingHint = false
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            when (state.currentScreen) {
                AppScreen.SCOREBOARD -> ScoreboardScreen(
                    state = state,
                    onAdjust = viewModel::adjustScore,
                    onSetScore = viewModel::setScore,
                    onSetTeamName = viewModel::setTeamName,
                    onReset = viewModel::resetScores,
                    onSettings = { viewModel.navigate(AppScreen.SETTINGS) },
                    showOnboardingHint = showOnboardingHint,
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
    onSetScore: (Team, Int) -> ScoreValidationError?,
    onSetTeamName: (Team, String) -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
    showOnboardingHint: Boolean,
) {
    var editingScoreTeam by remember { mutableStateOf<Team?>(null) }
    var editingNameTeam by remember { mutableStateOf<Team?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    teamName = state.settings.team1Name,
                    score = state.team1Score,
                    color = Team1Blue,
                    isWinner = state.winner == Team.TEAM_1,
                    onAdjust = { onAdjust(Team.TEAM_1, it) },
                    onEditScore = { editingScoreTeam = Team.TEAM_1 },
                    onEditName = { editingNameTeam = Team.TEAM_1 },
                )
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    teamName = state.settings.team2Name,
                    score = state.team2Score,
                    color = Team2Red,
                    isWinner = state.winner == Team.TEAM_2,
                    onAdjust = { onAdjust(Team.TEAM_2, it) },
                    onEditScore = { editingScoreTeam = Team.TEAM_2 },
                    onEditName = { editingNameTeam = Team.TEAM_2 },
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    teamName = state.settings.team1Name,
                    score = state.team1Score,
                    color = Team1Blue,
                    isWinner = state.winner == Team.TEAM_1,
                    onAdjust = { onAdjust(Team.TEAM_1, it) },
                    onEditScore = { editingScoreTeam = Team.TEAM_1 },
                    onEditName = { editingNameTeam = Team.TEAM_1 },
                )
                TeamZone(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    teamName = state.settings.team2Name,
                    score = state.team2Score,
                    color = Team2Red,
                    isWinner = state.winner == Team.TEAM_2,
                    onAdjust = { onAdjust(Team.TEAM_2, it) },
                    onEditScore = { editingScoreTeam = Team.TEAM_2 },
                    onEditName = { editingNameTeam = Team.TEAM_2 },
                )
            }
        }

        MinimalIconButton(
            iconResource = R.drawable.ic_refresh,
            contentDescription = "Reset scores",
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(4.dp),
        )
        MinimalIconButton(
            iconResource = R.drawable.ic_settings,
            contentDescription = "Open settings",
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(4.dp),
        )

        AnimatedVisibility(
            visible = showOnboardingHint,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = "Swipe up to add • Swipe down to subtract • Long press score or name to edit",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }

    editingScoreTeam?.let { team ->
        val currentScore = if (team == Team.TEAM_1) state.team1Score else state.team2Score
        val teamName = if (team == Team.TEAM_1) {
            state.settings.team1Name
        } else {
            state.settings.team2Name
        }
        EditScoreDialog(
            teamName = teamName,
            currentScore = currentScore,
            hardCap = state.settings.hardCapScore.takeIf { state.settings.hardCapEnabled },
            onDismiss = { editingScoreTeam = null },
            onSave = { newScore ->
                onSetScore(team, newScore).also { error ->
                    if (error == null) editingScoreTeam = null
                }
            },
        )
    }

    editingNameTeam?.let { team ->
        val currentName = if (team == Team.TEAM_1) {
            state.settings.team1Name
        } else {
            state.settings.team2Name
        }
        EditTeamNameDialog(
            currentName = currentName,
            onDismiss = { editingNameTeam = null },
            onSave = { newName ->
                onSetTeamName(team, newName)
                editingNameTeam = null
            },
        )
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
    onEditScore: () -> Unit,
    onEditName: () -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
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
            val scoreText = score.toString()
            val scoreWidthSize = maxWidth.value / (scoreText.length.coerceAtLeast(2) * 0.58f)
            val nameWidthSize = maxWidth.value / (teamName.length.coerceAtLeast(6) * 0.60f)
            val nameFontSize = minOf(
                maxWidth.value * 0.11f,
                maxHeight.value * 0.12f,
                nameWidthSize,
            )
                .coerceIn(16f, 54f)
                .sp
            val headerHeight = (maxHeight.value * 0.25f).coerceIn(64f, 132f).dp
            val crownSize = minOf(maxWidth.value * 0.10f, maxHeight.value * 0.12f)
                .coerceIn(28f, 46f)
                .dp
            val crownAreaHeight = maxOf(
                (maxHeight.value * 0.13f).coerceIn(36f, 58f).dp,
                crownSize + 12.dp,
            )
            val scoreAreaHeight = maxHeight - headerHeight
            val scoreHeightSize = scoreAreaHeight.value * 0.78f
            val scoreFontSize = minOf(scoreWidthSize, scoreHeightSize).coerceIn(46f, 320f).sp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = scoreText,
                    color = Color.White,
                    fontSize = scoreFontSize,
                    lineHeight = scoreFontSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    modifier = Modifier.pointerInput(teamName, score) {
                        detectTapGestures(onLongPress = { onEditScore() })
                    },
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(headerHeight)
                    .padding(horizontal = 52.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(crownAreaHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isWinner) {
                        Icon(
                            painter = painterResource(R.drawable.ic_crown),
                            contentDescription = "$teamName winner",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(crownSize + 12.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD54F).copy(alpha = 0.48f),
                                                Color(0xFFFFD54F).copy(alpha = 0.16f),
                                                Color.Transparent,
                                            ),
                                        ),
                                        radius = size.minDimension / 2f,
                                    )
                                }
                                .padding(6.dp),
                        )
                    }
                }
                Text(
                    text = teamName,
                    color = Color.White,
                    fontSize = nameFontSize,
                    lineHeight = nameFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(teamName) {
                            detectTapGestures(onLongPress = { onEditName() })
                        },
                )
            }
    }
}

@Composable
private fun EditTeamNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var nameText by remember(currentName) { mutableStateOf(currentName) }
    val normalizedName = nameText.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit team name") },
        text = {
            Column {
                Text("Current name: $currentName")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(30) },
                    label = { Text("New team name") },
                    supportingText = { Text("${nameText.length}/30") },
                    singleLine = true,
                    isError = normalizedName.isEmpty(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(normalizedName) },
                enabled = normalizedName.isNotEmpty(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EditScoreDialog(
    teamName: String,
    currentScore: Int,
    hardCap: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> ScoreValidationError?,
) {
    var scoreText by remember(teamName, currentScore) { mutableStateOf(currentScore.toString()) }
    var errorMessage by remember(teamName, currentScore) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $teamName score") },
        text = {
            Column {
                Text("Current score: $currentScore")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = {
                        scoreText = it.filter(Char::isDigit)
                        errorMessage = null
                    },
                    label = { Text("New score") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newScore = scoreText.toIntOrNull()
                    if (newScore == null) {
                        errorMessage = "Enter a non-negative whole number."
                        return@TextButton
                    }

                    errorMessage = when (onSave(newScore)) {
                        ScoreValidationError.NEGATIVE -> "Score cannot be negative."
                        ScoreValidationError.ABOVE_HARD_CAP ->
                            "Score cannot exceed the hard cap of $hardCap."
                        ScoreValidationError.WINNER_CANNOT_INCREASE ->
                            "The winning team's score can only be decreased."
                        null -> null
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MinimalIconButton(
    iconResource: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(iconResource),
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.size(24.dp),
        )
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
    val canSave = parsedWinningScore != null &&
        (!winByTwo || !hardCapEnabled || parsedHardCapScore != null)

    SettingsPage(title = "Game Settings", onBack = onBack) {
        NumberField(
            label = "Winning score",
            value = winningScore,
            onValueChange = { winningScore = it.filter(Char::isDigit) },
        )
        CheckRow(
            label = "Win by 2",
            checked = winByTwo,
            onCheckedChange = {
                winByTwo = it
                if (!it) hardCapEnabled = false
            },
        )
        if (winByTwo) {
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
        }
        Button(
            onClick = {
                onSave(
                    settings.copy(
                        winningScore = parsedWinningScore ?: 25,
                        winByTwo = winByTwo,
                        hardCapEnabled = winByTwo && hardCapEnabled,
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
