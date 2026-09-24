package ch.duartesantos.opengym.ui.screens

import androidx.compose.foundation.background
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
import ch.duartesantos.opengym.data.model.Equipment
import ch.duartesantos.opengym.data.model.Exercise
import ch.duartesantos.opengym.data.model.MuscleGroup
import ch.duartesantos.opengym.ui.components.AppHeader
import ch.duartesantos.opengym.ui.components.FilterChipM3
import ch.duartesantos.opengym.ui.components.OneRmTableDialog
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: GymViewModel
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }
    var showCustomExerciseDialog by remember { mutableStateOf(false) }

    var selectedDetailExercise by remember { mutableStateOf<Exercise?>(null) }
    var showOneRmCalcForExercise by remember { mutableStateOf<Exercise?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedMuscle, selectedEquipment) {
        exercises.filter { ex ->
            val matchesQuery = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
            val matchesMuscle = selectedMuscle == null || ex.primaryMuscle == selectedMuscle
            val matchesEquip = selectedEquipment == null || ex.equipment == selectedEquipment
            matchesQuery && matchesMuscle && matchesEquip
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Exercise Library",
                subtitle = "${exercises.size} movements catalogued",
                actions = {
                    IconButton(
                        onClick = { showCustomExerciseDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurfaceHighlight)
                            .testTag("add_custom_exercise_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise", tint = MaterialTheme.colorScheme.primary)
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
                .testTag("exercises_screen"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkSurfaceHighlight
                    )
                )
            }

            // Muscle Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipM3(
                            selected = selectedMuscle == null,
                            label = "All Muscles",
                            onClick = { selectedMuscle = null }
                        )
                    }
                    items(MuscleGroup.entries) { m ->
                        FilterChipM3(
                            selected = selectedMuscle == m,
                            label = m.displayName,
                            onClick = { selectedMuscle = m }
                        )
                    }
                }
            }

            // Equipment Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipM3(
                            selected = selectedEquipment == null,
                            label = "All Equipment",
                            onClick = { selectedEquipment = null }
                        )
                    }
                    items(Equipment.entries) { eq ->
                        FilterChipM3(
                            selected = selectedEquipment == eq,
                            label = eq.displayName,
                            onClick = { selectedEquipment = eq }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${filteredExercises.size} EXERCISES FOUND",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Exercise List Items
            items(filteredExercises, key = { it.id }) { exercise ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedDetailExercise = exercise },
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
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                if (exercise.isCustom) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AccentViolet.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("CUSTOM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AccentViolet)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${exercise.primaryMuscle.displayName} • ${exercise.equipment.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleFavoriteExercise(exercise.id) }
                        ) {
                            Icon(
                                imageVector = if (exercise.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (exercise.isFavorite) AccentGold else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Exercise Detail Bottom Sheet
    selectedDetailExercise?.let { ex ->
        ModalBottomSheet(
            onDismissRequest = { selectedDetailExercise = null },
            containerColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = ex.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${ex.primaryMuscle.displayName} • ${ex.equipment.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            selectedDetailExercise = null
                            showOneRmCalcForExercise = ex
                        }
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "1RM", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (ex.secondaryMuscles.isNotEmpty()) {
                    Text(
                        text = "Secondary Muscles: " + ex.secondaryMuscles.joinToString(", ") { it.displayName },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (ex.instructions.isNotBlank()) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = ex.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { selectedDetailExercise = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 1RM Table Dialog
    showOneRmCalcForExercise?.let { ex ->
        OneRmTableDialog(
            exerciseName = ex.name,
            weight = if (settings.isKg) 80f else 185f,
            reps = 6,
            isKg = settings.isKg,
            onDismiss = { showOneRmCalcForExercise = null }
        )
    }

    // Custom Exercise Builder Dialog
    if (showCustomExerciseDialog) {
        var name by remember { mutableStateOf("") }
        var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
        var equip by remember { mutableStateOf(Equipment.DUMBBELL) }
        var instructions by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomExerciseDialog = false },
            title = { Text("Create Custom Exercise") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Exercise Name") },
                        placeholder = { Text("e.g., Incline Cable Fly") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Primary Muscle: ${muscle.displayName}", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MuscleGroup.entries) { m ->
                            FilterChipM3(
                                selected = muscle == m,
                                label = m.displayName,
                                onClick = { muscle = m }
                            )
                        }
                    }

                    Text("Equipment: ${equip.displayName}", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(Equipment.entries) { eq ->
                            FilterChipM3(
                                selected = equip == eq,
                                label = eq.displayName,
                                onClick = { equip = eq }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Form Instructions (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createCustomExercise(name, muscle, equip, instructions)
                            showCustomExerciseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Exercise", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomExerciseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
