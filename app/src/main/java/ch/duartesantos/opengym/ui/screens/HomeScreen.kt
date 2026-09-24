package ch.duartesantos.opengym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.duartesantos.opengym.data.model.GymCalculators
import ch.duartesantos.opengym.data.model.Routine
import ch.duartesantos.opengym.ui.components.*
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: GymViewModel,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToLivePose: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val completedWorkouts by viewModel.completedWorkouts.collectAsStateWithLifecycle()
    val bodyWeights by viewModel.bodyWeights.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    var showLogWeightDialog by remember { mutableStateOf(false) }

    val todayDayOfWeek = viewModel.getTodayDayOfWeek()
    val todayRoutine = routines.find { it.dayOfWeek == todayDayOfWeek }

    val daysOfWeek = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
        5 to "Fri", 6 to "Sat", 7 to "Sun"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // App Header
        item {
            AppHeader(
                title = "openGym",
                subtitle = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            )
        }

        // Weekly Strip
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    daysOfWeek.forEach { (dayNum, dayLabel) ->
                        val isToday = dayNum == todayDayOfWeek
                        val hasRoutine = routines.any { it.dayOfWeek == dayNum }
                        val isFinishedThisWeek = completedWorkouts.any {
                            val cal = Calendar.getInstance().apply { timeInMillis = it.startTimeEpochMs }
                            val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> 1
                                Calendar.TUESDAY -> 2
                                Calendar.WEDNESDAY -> 3
                                Calendar.THURSDAY -> 4
                                Calendar.FRIDAY -> 5
                                Calendar.SATURDAY -> 6
                                Calendar.SUNDAY -> 7
                                else -> 1
                            }
                            dow == dayNum
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isFinishedThisWeek -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            hasRoutine -> DarkSurfaceHighlight
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 1.5.dp else 0.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFinishedThisWeek) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (hasRoutine) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isToday) MaterialTheme.colorScheme.primary else TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Hero Workout Card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        if (activeSession != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("today_workout_hero"),
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeSession != null) "WORKOUT IN PROGRESS" else "TODAY'S SESSION",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (activeSession != null) MaterialTheme.colorScheme.primary else TextSecondary
                        )

                        if (todayRoutine != null && activeSession == null) {
                            Text(
                                text = "${todayRoutine.targetDurationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeSession?.name ?: todayRoutine?.name ?: "Rest Day or Quick Workout",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )

                    if (activeSession == null && todayRoutine != null) {
                        Text(
                            text = todayRoutine.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Preview exercises
                        val previewExercises = todayRoutine.exerciseIds.mapNotNull { exId ->
                            exercises.find { it.id == exId }
                        }
                        if (previewExercises.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = previewExercises.joinToString(" • ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (activeSession != null) {
                            PrimaryActionButton(
                                text = "Resume Workout",
                                icon = Icons.Default.PlayArrow,
                                onClick = onNavigateToActiveWorkout,
                                modifier = Modifier.weight(1f),
                                testTag = "resume_workout_btn"
                            )
                        } else if (todayRoutine != null) {
                            PrimaryActionButton(
                                text = "Start Session",
                                icon = Icons.Default.PlayArrow,
                                onClick = {
                                    viewModel.startWorkoutFromRoutine(todayRoutine)
                                    onNavigateToActiveWorkout()
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "start_session_btn"
                            )
                        } else {
                            PrimaryActionButton(
                                text = "Quick Workout",
                                icon = Icons.Default.Add,
                                onClick = {
                                    viewModel.startQuickWorkout()
                                    onNavigateToActiveWorkout()
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "quick_workout_btn"
                            )
                        }

                        if (activeSession == null && todayRoutine != null) {
                            Button(
                                onClick = {
                                    viewModel.startQuickWorkout()
                                    onNavigateToActiveWorkout()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Custom", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Row
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val totalVolume = completedWorkouts.sumOf { it.totalVolumeKg.toDouble() }.toFloat()
                val prsCount = completedWorkouts.sumOf { it.prCount }

                MetricTile(
                    label = "Workouts",
                    value = "${completedWorkouts.size}",
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Personal Records",
                    value = "$prsCount",
                    icon = Icons.Default.EmojiEvents,
                    highlightColor = AccentGold,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Total Volume",
                    value = GymCalculators.formatWeight(totalVolume, settings.isKg).split(" ")[0],
                    unit = if (settings.isKg) "kg" else "lbs",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // AI Live Pose Form Coach Hero Banner
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, AccentViolet.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .clickable { onNavigateToLivePose() }
                    .testTag("ai_coach_home_banner"),
                color = DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AccentViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Live Pose Coach",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentViolet)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MediaPipe joint angle & squat depth tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open AI Coach",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Body Weight Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            val latestWeight = bodyWeights.maxByOrNull { it.timestampEpochMs }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("body_weight_card"),
                color = DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BODY WEIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (latestWeight != null) {
                                    GymCalculators.formatWeight(latestWeight.weightKg, settings.isKg)
                                } else {
                                    "--"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (latestWeight?.bodyFatPercent != null) {
                                Text(
                                    text = " • %.1f%% BF".format(latestWeight.bodyFatPercent),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showLogWeightDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Weight")
                        }

                        IconButton(
                            onClick = onNavigateToStats,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceHighlight)
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = "Stats", tint = TextPrimary)
                        }
                    }
                }
            }
        }

        // Recent Workouts Section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "Recent Workouts",
                actionText = if (completedWorkouts.isNotEmpty()) "See all" else null,
                onActionClick = onNavigateToStats
            )
        }

        if (completedWorkouts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No completed workouts yet. Start your first session above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(completedWorkouts.take(3)) { workout ->
                val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(workout.startTimeEpochMs))
                val durationMin = workout.durationSeconds / 60

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = workout.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$dateStr • $durationMin min • ${workout.totalSets} sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = GymCalculators.formatWeight(workout.totalVolumeKg, settings.isKg),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (workout.prCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = " ${workout.prCount} PRs",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = AccentGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Log Weight Dialog
    if (showLogWeightDialog) {
        var weightInputText by remember {
            val latest = bodyWeights.maxByOrNull { it.timestampEpochMs }
            val initVal = if (latest != null) {
                GymCalculators.kgToUserUnit(latest.weightKg, settings.isKg)
            } else {
                75f
            }
            mutableStateOf("%.1f".format(initVal))
        }
        var bodyFatText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogWeightDialog = false },
            title = { Text("Log Body Weight") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightInputText,
                        onValueChange = { weightInputText = it },
                        label = { Text("Weight (${if (settings.isKg) "kg" else "lbs"})") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = DarkSurfaceHighlight
                        )
                    )
                    OutlinedTextField(
                        value = bodyFatText,
                        onValueChange = { bodyFatText = it },
                        label = { Text("Body Fat % (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = DarkSurfaceHighlight
                        )
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = DarkSurfaceHighlight
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightInputText.toFloatOrNull()
                        val bf = bodyFatText.toFloatOrNull()
                        if (w != null && w > 0f) {
                            viewModel.logBodyWeight(w, bf, noteText)
                            showLogWeightDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
