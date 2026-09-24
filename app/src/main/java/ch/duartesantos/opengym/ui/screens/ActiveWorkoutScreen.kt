package ch.duartesantos.opengym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import ch.duartesantos.opengym.data.model.*
import ch.duartesantos.opengym.ui.components.*
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel

@Composable
fun ActiveWorkoutScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val workoutExercises by viewModel.activeWorkoutExercises.collectAsStateWithLifecycle()
    val allExercises by viewModel.exercises.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.activeWorkoutDurationSeconds.collectAsStateWithLifecycle()
    val restRemaining by viewModel.restTimerRemaining.collectAsStateWithLifecycle()
    val restTotal by viewModel.restTimerTotal.collectAsStateWithLifecycle()
    val finishedSummary by viewModel.finishedWorkoutSummary.collectAsStateWithLifecycle()

    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var workoutNotes by remember { mutableStateOf(activeSession?.notes ?: "") }

    // Dialog state helpers
    var plateCalcWeight by remember { mutableStateOf<Float?>(null) }
    var oneRmCalcData by remember { mutableStateOf<Triple<String, Float, Int>?>(null) }

    if (activeSession == null && finishedSummary == null) {
        // No active workout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Workout",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start a session from the Home or Plan screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryActionButton(
                    text = "Go to Today",
                    onClick = onNavigateBack,
                    modifier = Modifier.widthIn(min = 200.dp)
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Surface(
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = activeSession?.name ?: "Workout",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1
                            )
                            val minutes = durationSeconds / 60
                            val seconds = durationSeconds % 60
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showDiscardConfirmDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceHighlight)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Discard", tint = AccentRed)
                        }

                        Button(
                            onClick = { showFinishConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("finish_workout_btn")
                        ) {
                            Text("Finish", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Live Rest Timer pinned to bottom
            RestTimerBar(
                remainingSeconds = restRemaining,
                totalSeconds = restTotal,
                onAddSeconds = { viewModel.addRestSeconds(it) },
                onSkip = { viewModel.skipRestTimer() }
            )
        },
        containerColor = BlackBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("active_workout_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (workoutExercises.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No exercises added yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddExerciseSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Exercise", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                items(workoutExercises, key = { it.exercise.id }) { group ->
                    ExerciseCard(
                        group = group,
                        isKg = settings.isKg,
                        onAddSet = { prev -> viewModel.addSet(group.exercise.id, prev) },
                        onUpdateSet = { set -> viewModel.updateSet(set) },
                        onToggleSet = { set -> viewModel.toggleSetCompleted(set) },
                        onDeleteSet = { set -> viewModel.deleteSet(set) },
                        onRemoveExercise = { viewModel.removeExerciseFromActiveWorkout(group.exercise.id) },
                        onOpenPlateCalc = { w -> plateCalcWeight = w },
                        onOpenOneRmCalc = { w, r -> oneRmCalcData = Triple(group.exercise.name, w, r) }
                    )
                }
            }

            // Bottom Add Exercise button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAddExerciseSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("add_exercise_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Exercise",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }
    }

    // Add Exercise Library Sheet
    if (showAddExerciseSheet) {
        AddExerciseBottomSheet(
            exercises = allExercises,
            onSelect = { ex ->
                viewModel.addExerciseToActiveWorkout(ex.id)
                showAddExerciseSheet = false
            },
            onDismiss = { showAddExerciseSheet = false }
        )
    }

    // Finish Workout Confirmation Dialog
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = { Text("Finish Workout?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Great workout! Would you like to add any notes?")
                    OutlinedTextField(
                        value = workoutNotes,
                        onValueChange = { workoutNotes = it },
                        label = { Text("Workout Notes") },
                        placeholder = { Text("Felt strong on bench, nice pump...") },
                        modifier = Modifier.fillMaxWidth(),
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
                        viewModel.finishWorkout(workoutNotes)
                        showFinishConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Finish & Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text("Keep Working")
                }
            }
        )
    }

    // Discard Workout Confirmation
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to discard this workout? Recorded sets will not be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.discardWorkout()
                        showDiscardConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Post Workout Celebration Dialog
    finishedSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearFinishedWorkoutSummary()
                onNavigateBack()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentGold)
                    Text("Workout Complete!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = summary.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${summary.durationSeconds / 60} mins", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Column {
                            Text("Total Sets", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${summary.totalSets}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Column {
                            Text("Volume", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(GymCalculators.formatWeight(summary.totalVolumeKg, settings.isKg), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                    }
                    if (summary.prCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGold.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🏆 You set ${summary.prCount} new Personal Records!",
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearFinishedWorkoutSummary()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Plate Calculator Modal
    plateCalcWeight?.let { w ->
        PlateCalculatorDialog(
            targetWeight = w,
            isKg = settings.isKg,
            onDismiss = { plateCalcWeight = null }
        )
    }

    // 1RM Table Modal
    oneRmCalcData?.let { (name, w, r) ->
        OneRmTableDialog(
            exerciseName = name,
            weight = w,
            reps = r,
            isKg = settings.isKg,
            onDismiss = { oneRmCalcData = null }
        )
    }
}

@Composable
private fun ExerciseCard(
    group: ch.duartesantos.opengym.ui.viewmodel.WorkoutExerciseGroup,
    isKg: Boolean,
    onAddSet: (prev: ExerciseSet?) -> Unit,
    onUpdateSet: (ExerciseSet) -> Unit,
    onToggleSet: (ExerciseSet) -> Unit,
    onDeleteSet: (ExerciseSet) -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenPlateCalc: (Float) -> Unit,
    onOpenOneRmCalc: (Float, Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("exercise_card_${group.exercise.id}"),
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.exercise.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${group.exercise.primaryMuscle.displayName} • ${group.exercise.equipment.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Plate Calculator Shortcut
                    IconButton(
                        onClick = {
                            val lastWeight = group.sets.lastOrNull()?.weightKg ?: 60f
                            val userW = GymCalculators.kgToUserUnit(lastWeight, isKg)
                            onOpenPlateCalc(userW)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Plates",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Remove Exercise
                    IconButton(onClick = onRemoveExercise) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Set Table Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SET", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(text = if (isKg) "KG" else "LBS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(text = "REPS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.width(44.dp))
            }

            // Sets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.sets.forEachIndexed { idx, set ->
                    SetRow(
                        set = set,
                        isKg = isKg,
                        onUpdate = onUpdateSet,
                        onToggle = { onToggleSet(set) },
                        onDelete = { onDeleteSet(set) },
                        onCalc1Rm = {
                            val userW = GymCalculators.kgToUserUnit(set.weightKg, isKg)
                            onOpenOneRmCalc(userW, set.reps)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Set Button
            Button(
                onClick = { onAddSet(group.sets.lastOrNull()) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Add Set", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SetRow(
    set: ExerciseSet,
    isKg: Boolean,
    onUpdate: (ExerciseSet) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCalc1Rm: () -> Unit
) {
    val displayWeight = GymCalculators.kgToUserUnit(set.weightKg, isKg)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (set.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else DarkSurfaceHighlight)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number & type badge
        Box(
            modifier = Modifier
                .width(36.dp)
                .clickable {
                    // Cycle set types
                    val nextType = when (set.type) {
                        SetType.NORMAL -> SetType.WARMUP
                        SetType.WARMUP -> SetType.DROP
                        SetType.DROP -> SetType.FAILURE
                        SetType.FAILURE -> SetType.NORMAL
                    }
                    onUpdate(set.copy(type = nextType))
                },
            contentAlignment = Alignment.Center
        ) {
            if (set.type == SetType.NORMAL) {
                Text(
                    text = "${set.setNumber}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            when (set.type) {
                                SetType.WARMUP -> WarmupColor
                                SetType.DROP -> DropSetColor
                                SetType.FAILURE -> FailureColor
                                else -> TextSecondary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = set.type.badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }

        // Weight Input + Increment buttons
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    val step = if (isKg) 2.5f else 5f
                    val newKg = (set.weightKg - GymCalculators.weightToKg(step, isKg)).coerceAtLeast(0f)
                    onUpdate(set.copy(weightKg = newKg))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }

            Text(
                text = if (displayWeight % 1f == 0f) "%.0f".format(displayWeight) else "%.1f".format(displayWeight),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(
                onClick = {
                    val step = if (isKg) 2.5f else 5f
                    val newKg = set.weightKg + GymCalculators.weightToKg(step, isKg)
                    onUpdate(set.copy(weightKg = newKg))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }
        }

        // Reps Input + Increment buttons
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    val newReps = (set.reps - 1).coerceAtLeast(0)
                    onUpdate(set.copy(reps = newReps))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }

            Text(
                text = "${set.reps}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(
                onClick = {
                    val newReps = set.reps + 1
                    onUpdate(set.copy(reps = newReps))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }
        }

        // Checkmark Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (set.isCompleted) MaterialTheme.colorScheme.primary
                    else DarkSurfaceHighlight
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete Set",
                tint = if (set.isCompleted) Color.Black else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseBottomSheet(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }

    val filtered = remember(exercises, searchQuery, selectedMuscle) {
        exercises.filter { ex ->
            val matchesQuery = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
            val matchesMuscle = selectedMuscle == null || ex.primaryMuscle == selectedMuscle
            matchesQuery && matchesMuscle
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Select Exercise",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercise name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkSurfaceHighlight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Muscle filter row
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChipM3(
                        selected = selectedMuscle == null,
                        label = "All",
                        onClick = { selectedMuscle = null }
                    )
                }
                items(MuscleGroup.entries) { muscle ->
                    FilterChipM3(
                        selected = selectedMuscle == muscle,
                        label = muscle.displayName,
                        onClick = { selectedMuscle = muscle }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceHighlight)
                            .clickable { onSelect(ex) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ex.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${ex.primaryMuscle.displayName} • ${ex.equipment.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
