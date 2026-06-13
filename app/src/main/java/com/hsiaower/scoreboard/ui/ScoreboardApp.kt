package com.hsiaower.scoreboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.hsiaower.scoreboard.model.MatchTimeline
import com.hsiaower.scoreboard.model.RemoteAction
import com.hsiaower.scoreboard.model.ScoreTimeline
import com.hsiaower.scoreboard.model.ScoreSnapshot
import com.hsiaower.scoreboard.model.ScoreValidationError
import com.hsiaower.scoreboard.model.ScoreboardState
import com.hsiaower.scoreboard.model.SetTimeline
import com.hsiaower.scoreboard.model.Team
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
                AppScreen.HISTORY -> MatchHistoryScreen(
                    title = "History",
                    timeline = state.currentTimeline,
                    currentScore = state.match.team1Score to state.match.team2Score,
                    onBack = { viewModel.navigate(AppScreen.SCOREBOARD) },
                )
                AppScreen.PREVIOUS_MATCHES -> PreviousMatchesScreen(
                    matches = state.previousMatches,
                    onBack = { viewModel.navigate(AppScreen.SCOREBOARD) },
                    onOpen = viewModel::openMatchHistory,
                )
                AppScreen.MATCH_HISTORY -> {
                    val match = state.previousMatches.firstOrNull { it.id == state.selectedMatchId }
                    if (match == null) {
                        PreviousMatchesScreen(
                            matches = state.previousMatches,
                            onBack = { viewModel.navigate(AppScreen.SCOREBOARD) },
                            onOpen = viewModel::openMatchHistory,
                        )
                    } else {
                        MatchHistoryScreen(
                            title = "Match history",
                            timeline = match,
                            currentScore = match.currentSetEvents
                                .lastOrNull()
                                ?.takeIf { it.team1Score != 0 || it.team2Score != 0 }
                                ?.let { it.team1Score to it.team2Score },
                            onBack = { viewModel.navigate(AppScreen.PREVIOUS_MATCHES) },
                        )
                    }
                }
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
        state.matchWinner == Team.TEAM_1,
    )
    val team2 = TeamDisplay(
        Team.TEAM_2,
        state.settings.team2Name,
        state.match.team2Score,
        state.match.team2Sets,
        state.match.team2Timeouts,
        AwayRed,
        state.winner == Team.TEAM_2,
        state.matchWinner == Team.TEAM_2,
    )
    val left = if (state.match.team1OnLeft) team1 else team2
    val right = if (state.match.team1OnLeft) team2 else team1

    Box(Modifier.fillMaxSize().background(AppBackground)) {
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
    state.matchWinner?.let { team ->
        val winnerName = if (team == Team.TEAM_1) state.settings.team1Name else state.settings.team2Name
        AlertDialog(
            onDismissRequest = {},
            title = { Text("$winnerName wins the match!") },
            text = {
                Text(
                    "${state.match.team1Sets} - ${state.match.team2Sets} sets",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = viewModel::newMatch) {
                    Text("New Match")
                }
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
            TeamHeader(left, Modifier.weight(1f), onEditName)
            TimerCluster(
                left = left,
                right = right,
                state = state,
                modifier = Modifier.width(330.dp),
                onToggleTimer = viewModel::toggleTimer,
                onStartTimeout = viewModel::startTimeout,
                onEditTimeouts = onEditTimeouts,
            )
            TeamHeader(right, Modifier.weight(1f), onEditName)
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            ScoreCard(
                team = left,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = state.match.timerSecondsRemaining <= 0,
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
                enabled = state.match.timerSecondsRemaining <= 0,
                onAdd = { viewModel.adjustScore(right.team, 1) },
                onSubtract = { viewModel.adjustScore(right.team, -1) },
                onEdit = { onEditScore(right.team) },
            )
        }
    }
}

@Composable
private fun TeamHeader(
    team: TeamDisplay,
    modifier: Modifier,
    onEditName: (Team) -> Unit,
) {
    Text(
        text = team.name,
        color = team.color,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier.pointerInput(team.name) {
            detectTapGestures(onLongPress = { onEditName(team.team) })
        },
    )
}

@Composable
private fun TimerCluster(
    left: TeamDisplay,
    right: TeamDisplay,
    state: ScoreboardState,
    modifier: Modifier,
    onToggleTimer: () -> Unit,
    onStartTimeout: (Team) -> Unit,
    onEditTimeouts: (Team) -> Unit,
) {
    val seconds = state.match.timerSecondsRemaining
    val display = "%02d:%02d".format(seconds / 60, seconds % 60)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TimeoutControl(left, onStartTimeout, onEditTimeouts)
        Surface(
            modifier = Modifier.width(170.dp).clickable(onClick = onToggleTimer),
            shape = RoundedCornerShape(8.dp),
            color = PanelBackground,
        ) {
            Text(
                text = display,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        TimeoutControl(right, onStartTimeout, onEditTimeouts)
    }
}

@Composable
private fun TimeoutControl(
    team: TeamDisplay,
    onStartTimeout: (Team) -> Unit,
    onEditTimeouts: (Team) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .pointerInput(team.timeouts) {
                detectTapGestures(
                    onTap = { onStartTimeout(team.team) },
                    onLongPress = { onEditTimeouts(team.team) },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("TIMEOUT", color = MutedText, fontSize = 9.sp)
        Text(
            text = "◷${team.timeouts}",
            color = team.color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ScoreCard(
    team: TeamDisplay,
    modifier: Modifier,
    enabled: Boolean,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
    onEdit: () -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Card(
        modifier = modifier
            .pointerInput(team.score) {
                detectTapGestures(
                    onTap = { if (enabled) onAdd() },
                    onLongPress = { if (enabled) onEdit() },
                )
            }
            .pointerInput(enabled) {
                detectVerticalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount
                    },
                    onDragEnd = {
                        if (enabled && dragDistance > 48f) onSubtract()
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
) {
    var newMenu by remember { mutableStateOf(false) }
    var historyMenu by remember { mutableStateOf(false) }
    var timelineMenu by remember { mutableStateOf(false) }

    @Composable
    fun Sets() {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SetBox(
                team = left,
                canAward = state.winner == left.team && state.matchWinner == null,
                onAwardSet = viewModel::awardSet,
                onEditSets = onEditSets,
            )
            SetBox(
                team = right,
                canAward = state.winner == right.team && state.matchWinner == null,
                onAwardSet = viewModel::awardSet,
                onEditSets = onEditSets,
            )
        }
    }

    @Composable
    fun Actions() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionMenuButton(
                    symbol = "+",
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
                    symbol = "\u21B6",
                    description = "Undo and redo",
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
                ActionButton("\u21C4", "Switch sides", viewModel::switchSides)
                ActionButton("\u27F3", "Rotation setup", viewModel::showRotationPlaceholder)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconActionMenuButton(
                    iconRes = R.drawable.ic_timeline_chart,
                    description = "Timeline",
                    expanded = timelineMenu,
                    onClick = { timelineMenu = true },
                    onDismiss = { timelineMenu = false },
                ) {
                    DropdownMenuItem(text = { Text("History") }, onClick = {
                        timelineMenu = false
                        viewModel.navigate(AppScreen.HISTORY)
                    })
                    DropdownMenuItem(text = { Text("Previous matches") }, onClick = {
                        timelineMenu = false
                        viewModel.navigate(AppScreen.PREVIOUS_MATCHES)
                    })
                }
                ActionButton("\u25A3", "Display placeholder", viewModel::showRotationPlaceholder)
                ActionButton("\u2605", "Favorite placeholder", viewModel::showRotationPlaceholder)
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
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Sets()
        Spacer(Modifier.height(16.dp))
        Actions()
    }
}

@Composable
private fun SetBox(
    team: TeamDisplay,
    canAward: Boolean,
    onAwardSet: (Team) -> Unit,
    onEditSets: (Team) -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .then(
                if (canAward) {
                    Modifier.border(3.dp, WinnerGold, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .pointerInput(team.sets, canAward) {
                detectTapGestures(
                    onTap = { if (canAward) onAwardSet(team.team) },
                    onLongPress = { onEditSets(team.team) },
                )
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
private fun IconActionMenuButton(
    iconRes: Int,
    description: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit,
) {
    Box {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = menuContent)
    }
}

@Composable
private fun MatchHistoryScreen(
    title: String,
    timeline: MatchTimeline,
    currentScore: Pair<Int, Int>?,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        ScreenHeader(title, onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(timeline.team1Name, color = HomeBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            MatchSetBox(timeline.team1Sets, HomeBlue)
            Text("-", color = MutedText, fontSize = 28.sp, modifier = Modifier.padding(horizontal = 8.dp))
            MatchSetBox(timeline.team2Sets, AwayRed)
            Text(timeline.team2Name, color = AwayRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            formatMatchTime(timeline.startedAt),
            color = MutedText,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            shape = RoundedCornerShape(16.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(timeline.completedSets, key = { it.number }) { set ->
                    SetHistoryRow(
                        number = set.number,
                        team1Score = set.team1Score,
                        team2Score = set.team2Score,
                        events = set.events,
                        winner = set.winner,
                    )
                }
                if (currentScore != null) {
                    item {
                        SetHistoryRow(
                            number = timeline.completedSets.size + 1,
                            team1Score = currentScore.first,
                            team2Score = currentScore.second,
                            events = timeline.currentSetEvents,
                            winner = null,
                        )
                    }
                }
                if (timeline.completedSets.isEmpty() && currentScore == null) {
                    item {
                        Text(
                            "No recorded sets",
                            color = MutedText,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviousMatchesScreen(
    matches: List<MatchTimeline>,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        ScreenHeader("Previous matches", onBack)
        Card(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (matches.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No previous matches yet", color = MutedText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(matches, key = { it.id }) { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(match.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.width(150.dp)) {
                                Text(formatMatchDate(match.startedAt), color = Color.White)
                                Text(formatMatchTimeOnly(match.startedAt), color = MutedText, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(match.team1Name, color = HomeBlue, fontWeight = FontWeight.Bold)
                                MatchSetBox(match.team1Sets, HomeBlue)
                                Text("-", color = MutedText, modifier = Modifier.padding(horizontal = 6.dp))
                                MatchSetBox(match.team2Sets, AwayRed)
                                Text(match.team2Name, color = AwayRed, fontWeight = FontWeight.Bold)
                            }
                            Text("\u203A", color = Color.White, fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Text("\u2039", color = HomeBlue, fontSize = 38.sp)
        }
        Text(title.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MatchSetBox(value: Int, color: Color) {
    Surface(
        modifier = Modifier.padding(horizontal = 10.dp).size(width = 54.dp, height = 48.dp),
        color = color,
        shape = RoundedCornerShape(7.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(value.toString(), color = Color.White, fontSize = 26.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SetHistoryRow(
    number: Int,
    team1Score: Int,
    team2Score: Int,
    events: List<ScoreSnapshot>,
    winner: Team?,
) {
    val displayedEvents = remember(events, winner, team1Score, team2Score) {
        if (winner == null) {
            events
        } else {
            ScoreTimeline.throughWinningPoint(
                snapshots = events,
                winner = winner,
                recordedWinningScore = if (winner == Team.TEAM_1) team1Score else team2Score,
            )
        }
    }
    val displayedScore = displayedEvents.lastOrNull() ?: ScoreSnapshot(team1Score, team2Score)
    val pointEvents = remember(displayedEvents) { ScoreTimeline.pointEvents(displayedEvents) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Set $number", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MatchSetBox(displayedScore.team1Score, HomeBlue)
                MatchSetBox(displayedScore.team2Score, AwayRed)
            }
            LazyRow(
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(pointEvents) { event ->
                    PointTimelineColumn(event.team, event.score)
                }
            }
        }
    }
}

@Composable
private fun PointTimelineColumn(team: Team, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (team == Team.TEAM_1) {
            PointTimelineBox(score, HomeBlue)
        } else {
            Spacer(Modifier.size(width = 38.dp, height = 34.dp))
        }
        if (team == Team.TEAM_2) {
            PointTimelineBox(score, AwayRed)
        } else {
            Spacer(Modifier.size(width = 38.dp, height = 34.dp))
        }
    }
}

@Composable
private fun PointTimelineBox(score: Int, color: Color) {
    Surface(
        modifier = Modifier.size(width = 38.dp, height = 34.dp),
        color = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                score.toString(),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val matchDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy")
private val matchTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatMatchDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(matchDateFormatter)

private fun formatMatchTimeOnly(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(matchTimeFormatter)

private fun formatMatchTime(timestamp: Long): String =
    "${formatMatchDate(timestamp)}  ${formatMatchTimeOnly(timestamp)}"

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
    val pages = listOf(
        Triple("Score quickly", "Tap a score card to add a point.", "+1"),
        Triple(
            "Correct mistakes",
            "Swipe down to subtract. Long press names, scores, sets, or timeouts to edit.",
            "\u2195",
        ),
        Triple(
            "Run the match",
            "Use switch sides, undo/redo, timeout timer, and settings from the center controls.",
            "\u21C4",
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(0.72f).height(440.dp).widthIn(max = 760.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                ) {
                    Text(
                        text = "\u00D7",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { page ->
                        val content = pages[page]
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(content.third, color = HomeBlue, fontSize = 64.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                content.first,
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                content.second,
                                color = MutedText,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        pages.indices.forEach { page ->
                            Box(
                                Modifier
                                    .size(if (pagerState.currentPage == page) 12.dp else 10.dp)
                                    .background(
                                        if (pagerState.currentPage == page) {
                                            Color.White
                                        } else {
                                            MutedText.copy(alpha = 0.45f)
                                        },
                                        CircleShape,
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(page) }
                                    },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
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
                        ScoreValidationError.TIMEOUT_ACTIVE -> "Score changes are disabled during a timeout"
                        null -> null
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
