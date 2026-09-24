package ch.duartesantos.opengym.ui.screens

import androidx.compose.foundation.background
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
import ch.duartesantos.opengym.data.model.Exercise
import ch.duartesantos.opengym.data.model.Routine
import ch.duartesantos.opengym.ui.components.AppHeader
import ch.duartesantos.opengym.ui.components.PrimaryActionButton
import ch.duartesantos.opengym.ui.components.SectionHeader
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel

@Composable
fun PlanScreen(
    viewModel: GymViewModel,
    onStartRoutine: (Routine) -> Unit
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    var showRoutineDialog by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }

    val daysOfWeekMap = mapOf(
        1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
        4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday"
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "Workout Plan",
                subtitle = "Schedule routines for every day of the week",
                actions = {
                    IconButton(
                        onClick = {
                            editingRoutine = null
                            showRoutineDialog = true
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurfaceHighlight)
                            .testTag("add_routine_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Routine", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        containerColor = BlackBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("plan_screen"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Day-by-day Overview
            item {
                SectionHeader(title = "Weekly Schedule")
            }

            items(daysOfWeekMap.entries.toList()) { (dayNum, dayName) ->
                val routineForDay = routines.find { it.dayOfWeek == dayNum }
                val isToday = dayNum == viewModel.getTodayDayOfWeek()

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isToday) MaterialTheme.colorScheme.primary else TextPrimary
                                )
                                if (isToday) {
                                    Text(
                                        text = " (Today)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = routineForDay?.name ?: "Rest Day",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (routineForDay != null) TextSecondary else TextMuted
                            )
                        }

                        if (routineForDay != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        editingRoutine = routineForDay
                                        showRoutineDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                                Button(
                                    onClick = { onStartRoutine(routineForDay) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Start", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    editingRoutine = Routine(
                                        id = "routine_" + System.currentTimeMillis(),
                                        name = "$dayName Workout",
                                        dayOfWeek = dayNum,
                                        description = ""
                                    )
                                    showRoutineDialog = true
                                }
                            ) {
                                Text("+ Assign", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // All Saved Routines Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(title = "All Routines (${routines.size})")
            }

            items(routines) { routine ->
                val routineExercises = routine.exerciseIds.mapNotNull { exId ->
                    exercises.find { it.id == exId }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                if (routine.dayOfWeek in 1..7) {
                                    Text(
                                        text = daysOfWeekMap[routine.dayOfWeek] ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingRoutine = routine
                                        showRoutineDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (routine.description.isNotBlank()) {
                            Text(
                                text = routine.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${routineExercises.size} exercises: " + routineExercises.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        PrimaryActionButton(
                            text = "Start This Routine",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onStartRoutine(routine) },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        )
                    }
                }
            }
        }
    }

    // Routine Edit / Create Dialog
    if (showRoutineDialog) {
        RoutineEditDialog(
            routine = editingRoutine,
            allExercises = exercises,
            onSave = { id, name, dayOfWeek, desc, exerciseIds ->
                viewModel.saveRoutine(id, name, dayOfWeek, desc, exerciseIds)
                showRoutineDialog = false
            },
            onDismiss = { showRoutineDialog = false }
        )
    }
}

@Composable
private fun RoutineEditDialog(
    routine: Routine?,
    allExercises: List<Exercise>,
    onSave: (id: String?, name: String, dayOfWeek: Int, description: String, exerciseIds: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(routine?.name ?: "") }
    var description by remember { mutableStateOf(routine?.description ?: "") }
    var selectedDay by remember { mutableStateOf(routine?.dayOfWeek ?: 0) }
    var selectedExerciseIds by remember { mutableStateOf(routine?.exerciseIds ?: emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (routine != null) "Edit Routine" else "New Routine") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name") },
                    placeholder = { Text("e.g., Push Day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Focus on chest & triceps") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assigned Day", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val days = listOf(
                        0 to "None", 1 to "Mon", 2 to "Tue", 3 to "Wed",
                        4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
                    )
                    items(days) { (dNum, dLabel) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedDay == dNum) MaterialTheme.colorScheme.primary else DarkSurfaceHighlight)
                                .clickable { selectedDay = dNum }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = dLabel,
                                color = if (selectedDay == dNum) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text("Select Exercises (${selectedExerciseIds.size})", style = MaterialTheme.typography.labelMedium)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allExercises) { ex ->
                        val isChecked = selectedExerciseIds.contains(ex.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else DarkSurfaceHighlight)
                                .clickable {
                                    selectedExerciseIds = if (isChecked) {
                                        selectedExerciseIds - ex.id
                                    } else {
                                        selectedExerciseIds + ex.id
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ex.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedExerciseIds = if (checked) selectedExerciseIds + ex.id else selectedExerciseIds - ex.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(routine?.id, name, selectedDay, description, selectedExerciseIds)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Routine", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
