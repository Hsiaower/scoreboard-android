package com.hsiaower.scoreboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hsiaower.scoreboard.R
import com.hsiaower.scoreboard.ScoreboardViewModel
import com.hsiaower.scoreboard.model.AppScreen
import com.hsiaower.scoreboard.model.GameSettings
import com.hsiaower.scoreboard.model.InputType
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.ScoreValidationError
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.Team
import kotlin.math.abs
import kotlin.math.min

private val AppBackground = Color(0xFF151719)
private val PanelBackground = Color(0xFF202326)
private val HomeBlue = Color(0xFF2196F3)
private val AwayRed = Color(0xFFF44336)
private val MutedText = Color(0xFFB8BDC3)
private val WinnerGold = Color(0xFFFFD54F)

private data class TeamDisplay(
    val team: Team,
    val name: String,
    val score: Int,
    val sets: Int,
    val timeouts: Int,
    val color: Color,
    val winner: Boolean,
    val matchWinner: Boolean,
)

@Composable
fun ScoreboardApp(viewModel: ScoreboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = HomeBlue,
            secondary = WinnerGold,
            background = AppBackground,
            surface = PanelBackground,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            when (state.currentScreen) {
                AppScreen.SCOREBOARD -> ScoreboardScreen(state, viewModel)
                AppScreen.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    onSave = viewModel::saveSettings,
                    onRemoteMapping = { viewModel.navigate(AppScreen.REMOTE_MAPPING) },
                    onTutorial = viewModel::restartTutorial,
                    onBack = { viewModel.navigate(AppScreen.SCOREBOARD) },
                )
                AppScreen.REMOTE_MAPPING -> RemoteMappingScreen(
                    state = state,
                    onSetInput = viewModel::beginInputCapture,
                    onClear = viewModel::clearMapping,
                    onCancelCapture = viewModel::cancelInputCapture,
                    onBack = { viewModel.navigate(AppScreen.SETTINGS) },
                )
                AppScreen.TUTORIAL -> TutorialScreen(onComplete = viewModel::completeTutorial)
            }
        }
    }
}

@Composable
private fun ScoreboardScreen(state: ScoreboardState, viewModel: ScoreboardViewModel) {
    var editingName by remember { mutableStateOf<Team?>(null) }
    var editingScore by remember { mutableStateOf<Team?>(null) }
    var editingSets by remember { mutableStateOf<Team?>(null) }
    var editingTimeouts by remember { mutableStateOf<Team?>(null) }

    val team1 = TeamDisplay(
        Team.TEAM_1,
        state.settings.team1Name,
        state.match.team1Score,
        state.match.team1Sets,
        state.match.team1Timeouts,
        HomeBlue,
        state.winner == Team.TEAM_1,
        state.match.team1Sets >= state.settings.setsToWin,
    )
    val team2 = TeamDisplay(
        Team.TEAM_2,
        state.settings.team2Name,
        state.match.team2Score,
        state.match.team2Sets,
        state.match.team2Timeouts,
        AwayRed,
        state.winner == Team.TEAM_2,
        state.match.team2Sets >= state.settings.setsToWin,
    )
    val left = if (state.match.team1OnLeft) team1 else team2
    val right = if (state.match.team1OnLeft) team2 else team1

    BoxWithConstraints(Modifier.fillMaxSize().background(AppBackground)) {
        if (maxWidth > maxHeight) {
            LandscapeScoreboard(
                left = left,
                right = right,
                state = state,
                onEditName = { editingName = it },
                onEditScore = { editingScore = it },
                onEditSets = { editingSets = it },
                onEditTimeouts = { editingTimeouts = it },
                viewModel = viewModel,
            )
        } else {
            PortraitScoreboard(
                top = left,
                bottom = right,
                state = state,
                onEditName = { editingName = it },
                onEditScore = { editingScore = it },
                onEditSets = { editingSets = it },
                onEditTimeouts = { editingTimeouts = it },
                viewModel = viewModel,
            )
        }

        if (state.rotationMessageVisible) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                color = PanelBackground,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Rotation setup is coming next", color = Color.White, modifier = Modifier.padding(16.dp))
            }
        }
    }

    editingName?.let { team ->
        EditTextDialog(
            title = "Edit team name",
            currentValue = if (team == Team.TEAM_1) state.settings.team1Name else state.settings.team2Name,
            onDismiss = { editingName = null },
            onSave = {
                viewModel.setTeamName(team, it)
                editingName = null
            },
        )
    }
    editingScore?.let { team ->
        val score = if (team == Team.TEAM_1) state.match.team1Score else state.match.team2Score
        EditNumberDialog(
            title = "Edit score",
            currentValue = score,
            onDismiss = { editingScore = null },
            onSave = {
                val error = viewModel.setScore(team, it)
                if (error == null) editingScore = null
                error
            },
        )
    }
    editingSets?.let { team ->
        val value = if (team == Team.TEAM_1) state.match.team1Sets else state.match.team2Sets
        EditNumberDialog(
            title = "Edit sets won",
            currentValue = value,
            onDismiss = { editingSets = null },
            onSave = {
                viewModel.setSets(team, it)
                editingSets = null
                null
            },
        )
    }
    editingTimeouts?.let { team ->
        val value = if (team == Team.TEAM_1) state.match.team1Timeouts else state.match.team2Timeouts
        EditNumberDialog(
            title = "Edit timeouts remaining",
            currentValue = value,
            onDismiss = { editingTimeouts = null },
            onSave = {
                viewModel.setTimeouts(team, it)
                editingTimeouts = null
                null
            },
        )
    }
}

@Composable
private fun LandscapeScoreboard(
    left: TeamDisplay,
    right: TeamDisplay,
    state: ScoreboardState,
    onEditName: (Team) -> Unit,
    onEditScore: (Team) -> Unit,
    onEditSets: (Team) -> Unit,
    onEditTimeouts: (Team) -> Unit,
    viewModel: ScoreboardViewModel,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(86.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamHeader(left, Modifier.weight(1f), onEditName, onEditTimeouts, viewModel::startTimeout)
            TimerPanel(state, Modifier.width(270.dp), viewModel::toggleTimer, viewModel::resetTimer)
            TeamHeader(right, Modifier.weight(1f), onEditName, onEditTimeouts, viewModel::startTimeout)
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            ScoreCard(
                team = left,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onAdd = { viewModel.adjustScore(left.team, 1) },
                onSubtract = { viewModel.adjustScore(left.team, -1) },
                onEdit = { onEditScore(left.team) },
            )
            CenterControls(
                left = left,
                right = right,
                state = state,
                modifier = Modifier.width(220.dp),
                onEditSets = onEditSets,
                viewModel = viewModel,
            )
            ScoreCard(
                team = right,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onAdd = { viewModel.adjustScore(right.team, 1) },
                onSubtract = { viewModel.adjustScore(right.team, -1) },
                onEdit = { onEditScore(right.team) },
            )
        }
    }
}

@Composable
private fun PortraitScoreboard(
    top: TeamDisplay,
    bottom: TeamDisplay,
    state: ScoreboardState,
    onEditName: (Team) -> Unit,
    onEditScore: (Team) -> Unit,
    onEditSets: (Team) -> Unit,
    onEditTimeouts: (Team) -> Unit,
    viewModel: ScoreboardViewModel,
) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TimerPanel(state, Modifier.fillMaxWidth().height(62.dp), viewModel::toggleTimer, viewModel::resetTimer)
        TeamHeader(top, Modifier.fillMaxWidth().height(52.dp), onEditName, onEditTimeouts, viewModel::startTimeout)
        ScoreCard(
            top,
            Modifier.fillMaxWidth().weight(1f),
            { viewModel.adjustScore(top.team, 1) },
            { viewModel.adjustScore(top.team, -1) },
            { onEditScore(top.team) },
        )
        CenterControls(
            left = top,
            right = bottom,
            state = state,
            modifier = Modifier.fillMaxWidth().height(96.dp),
            onEditSets = onEditSets,
            viewModel = viewModel,
            horizontal = true,
        )
        TeamHeader(bottom, Modifier.fillMaxWidth().height(52.dp), onEditName, onEditTimeouts, viewModel::startTimeout)
        ScoreCard(
            bottom,
            Modifier.fillMaxWidth().weight(1f),
            { viewModel.adjustScore(bottom.team, 1) },
            { viewModel.adjustScore(bottom.team, -1) },
            { onEditScore(bottom.team) },
        )
    }
}

@Composable
private fun TeamHeader(
    team: TeamDisplay,
    modifier: Modifier,
    onEditName: (Team) -> Unit,
    onEditTimeouts: (Team) -> Unit,
    onStartTimeout: (Team) -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(
            text = team.name,
            color = team.color,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).pointerInput(team.name) {
                detectTapGestures(onLongPress = { onEditName(team.team) })
            },
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .pointerInput(team.timeouts) {
                    detectTapGestures(
                        onTap = { onStartTimeout(team.team) },
                        onLongPress = { onEditTimeouts(team.team) },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TIMEOUTS", color = MutedText, fontSize = 10.sp)
            Text("◷ ${team.timeouts}", color = team.color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimerPanel(
    state: ScoreboardState,
    modifier: Modifier,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    val seconds = state.match.timerSecondsRemaining
    val display = "%02d:%02d".format(seconds / 60, seconds % 60)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onToggle),
            shape = RoundedCornerShape(8.dp),
            color = PanelBackground,
        ) {
            Text(
                text = display,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        TextButton(onClick = onReset) { Text("↻", color = MutedText, fontSize = 24.sp) }
    }
}

@Composable
private fun ScoreCard(
    team: TeamDisplay,
    modifier: Modifier,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
    onEdit: () -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Card(
        modifier = modifier
            .pointerInput(team.score) {
                detectTapGestures(onTap = { onAdd() }, onLongPress = { onEdit() })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount
                    },
                    onDragEnd = {
                        if (dragDistance > 48f) onSubtract()
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f },
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = team.color),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val scoreText = team.score.toString().padStart(2, '0')
            val widthSize = maxWidth.value / (scoreText.length * 0.64f)
            val heightSize = maxHeight.value * 0.68f
            val scoreSize = min(widthSize, heightSize).coerceIn(54f, 220f).sp
            Text(
                text = scoreText,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = scoreSize,
                lineHeight = scoreSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            if (team.winner || team.matchWinner) {
                Text(
                    if (team.matchWinner) "MATCH WINNER" else "SET WINNER",
                    color = WinnerGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun CenterControls(
    left: TeamDisplay,
    right: TeamDisplay,
    state: ScoreboardState,
    modifier: Modifier,
    onEditSets: (Team) -> Unit,
    viewModel: ScoreboardViewModel,
    horizontal: Boolean = false,
) {
    var newMenu by remember { mutableStateOf(false) }
    var historyMenu by remember { mutableStateOf(false) }

    @Composable
    fun Sets() {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SetBox(left, onEditSets)
            SetBox(right, onEditSets)
        }
    }

    @Composable
    fun Actions() {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionMenuButton(
                symbol = "＋",
                description = "Match actions",
                expanded = newMenu,
                onClick = { newMenu = true },
                onDismiss = { newMenu = false },
            ) {
                DropdownMenuItem(text = { Text("New match") }, onClick = {
                    newMenu = false
                    viewModel.newMatch()
                })
                DropdownMenuItem(text = { Text("Clear score") }, onClick = {
                    newMenu = false
                    viewModel.clearScore()
                })
            }
            ActionMenuButton(
                symbol = "↶",
                description = "History",
                expanded = historyMenu,
                onClick = { historyMenu = true },
                onDismiss = { historyMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Undo") },
                    enabled = state.canUndo,
                    onClick = {
                        historyMenu = false
                        viewModel.undo()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Redo") },
                    enabled = state.canRedo,
                    onClick = {
                        historyMenu = false
                        viewModel.redo()
                    },
                )
            }
            ActionButton("⇄", "Switch sides", viewModel::switchSides)
            ActionButton("⟳", "Rotation setup", viewModel::showRotationPlaceholder)
            IconButton(
                onClick = { viewModel.navigate(AppScreen.SETTINGS) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = Color.White,
                )
            }
        }
    }

    if (horizontal) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Sets()
            Actions()
        }
    } else {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Sets()
            Spacer(Modifier.height(24.dp))
            Actions()
        }
    }
}

@Composable
private fun SetBox(team: TeamDisplay, onEditSets: (Team) -> Unit) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .pointerInput(team.sets) {
                detectTapGestures(onLongPress = { onEditSets(team.team) })
            },
        color = team.color,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                team.sets.toString(),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActionButton(symbol: String, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Text(symbol, color = Color.White, fontSize = 30.sp)
    }
}

@Composable
private fun ActionMenuButton(
    symbol: String,
    description: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit,
) {
    Box {
        ActionButton(symbol, description, onClick)
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = menuContent)
    }
}

@Composable
private fun SettingsScreen(
    settings: GameSettings,
    onSave: (GameSettings) -> Unit,
    onRemoteMapping: () -> Unit,
    onTutorial: () -> Unit,
    onBack: () -> Unit,
) {
    var winningScore by remember(settings) { mutableStateOf(settings.winningScore.toString()) }
    var winByTwo by remember(settings) { mutableStateOf(settings.winByTwo) }
    var hardCapEnabled by remember(settings) { mutableStateOf(settings.hardCapEnabled) }
    var hardCapScore by remember(settings) { mutableStateOf(settings.hardCapScore.toString()) }
    var setsToWin by remember(settings) { mutableStateOf(settings.setsToWin.toString()) }
    var timeouts by remember(settings) { mutableStateOf(settings.timeoutsPerSet.toString()) }
    var timeoutDuration by remember(settings) { mutableStateOf(settings.timeoutDurationSeconds.toString()) }

    SettingsScaffold("Settings", onBack) {
        NumberField("Winning score", winningScore) { winningScore = it }
        SettingCheck("Win by 2", winByTwo) {
            winByTwo = it
            if (!it) hardCapEnabled = false
        }
        if (winByTwo) {
            SettingCheck("Hard cap enabled", hardCapEnabled) { hardCapEnabled = it }
            NumberField("Hard cap score", hardCapScore, enabled = hardCapEnabled) { hardCapScore = it }
        }
        NumberField("Sets to win match", setsToWin) { setsToWin = it }
        NumberField("Timeouts per set", timeouts) { timeouts = it }
        NumberField("Timeout duration (seconds)", timeoutDuration) { timeoutDuration = it }
        OutlinedButton(onClick = onRemoteMapping, modifier = Modifier.fillMaxWidth()) {
            Text("Remote mapping")
        }
        OutlinedButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) {
            Text("Show tutorial again")
        }
        Button(
            onClick = {
                onSave(
                    settings.copy(
                        winningScore = winningScore.toIntOrNull()?.coerceAtLeast(1) ?: 25,
                        winByTwo = winByTwo,
                        hardCapEnabled = winByTwo && hardCapEnabled,
                        hardCapScore = hardCapScore.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                        setsToWin = setsToWin.toIntOrNull()?.coerceIn(1, 9) ?: 2,
                        timeoutsPerSet = timeouts.toIntOrNull()?.coerceIn(0, 9) ?: 2,
                        timeoutDurationSeconds = timeoutDuration.toIntOrNull()?.coerceIn(1, 600) ?: 30,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun NumberField(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, color = Color.White)
    }
}

@Composable
private fun TutorialScreen(onComplete: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("Score quickly", "Tap a score card to add a point.", "＋1"),
        Triple("Correct mistakes", "Swipe down to subtract. Long press names, scores, sets, or timeouts to edit.", "↕"),
        Triple("Run the match", "Use switch sides, undo/redo, timeout timer, and settings from the center controls.", "⇄"),
    )
    val current = pages[page]
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 560.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(current.third, color = HomeBlue, fontSize = 72.sp)
                Text(current.first, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(current.second, color = MutedText, fontSize = 18.sp, textAlign = TextAlign.Center)
                Text("${page + 1} / ${pages.size}", color = MutedText)
                Button(onClick = {
                    if (page < pages.lastIndex) page++ else onComplete()
                }) {
                    Text(if (page < pages.lastIndex) "Continue" else "Start scoring")
                }
            }
        }
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
    SettingsScaffold("Remote mapping", onBack) {
        Text(
            "Single press is active. Double and long press remain placeholders.",
            color = MutedText,
        )
        RemoteAction.entries.forEach { action ->
            Card(colors = CardDefaults.cardColors(containerColor = PanelBackground)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(action.label, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            state.remoteMappings[action]?.let { "${it.displayName} · ${it.inputType.label}" }
                                ?: "Not mapped",
                            color = MutedText,
                        )
                    }
                    TextButton(onClick = { onSetInput(action) }) { Text("Set input") }
                    if (state.remoteMappings[action] != null) {
                        TextButton(onClick = { onClear(action) }) { Text("Clear") }
                    }
                }
            }
        }
    }
    if (state.capturingAction != null) {
        AlertDialog(
            onDismissRequest = onCancelCapture,
            title = { Text("Press a remote button") },
            text = { Text("Waiting for ${state.capturingAction.label}") },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onCancelCapture) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EditTextDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(30) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }, enabled = value.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EditNumberDialog(
    title: String,
    currentValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> ScoreValidationError?,
) {
    var value by remember(currentValue) { mutableStateOf(currentValue.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = error != null,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val number = value.toIntOrNull()
                error = when {
                    number == null -> "Enter a non-negative integer"
                    else -> when (onSave(number)) {
                        ScoreValidationError.ABOVE_HARD_CAP -> "Score exceeds the hard cap"
                        ScoreValidationError.NEGATIVE -> "Value cannot be negative"
                        null -> null
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
