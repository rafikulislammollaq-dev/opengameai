package ch.duartesantos.opengym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ch.duartesantos.opengym.ui.components.*
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    viewModel: GymViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bodyWeights by viewModel.bodyWeights.collectAsStateWithLifecycle()
    val completedWorkouts by viewModel.completedWorkouts.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0=Weight, 1=Volume, 2=1RM Lifts
    var showLogWeightDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Analytics & Stats",
                subtitle = "Track your body, strength & volume over time"
            )
        },
        containerColor = BlackBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("stats_screen"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Tab Controls
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceHighlight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        listOf("Body Weight", "Muscle Volume", "1RM Maxes").forEachIndexed { index, label ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // BODY WEIGHT TAB
                    item {
                        BodyWeightChartView(
                            entries = bodyWeights,
                            targetWeightKg = settings.targetWeightKg,
                            isKg = settings.isKg,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weight History (${bodyWeights.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Button(
                                onClick = { showLogWeightDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val sortedWeights = bodyWeights.sortedByDescending { it.timestampEpochMs }
                    items(sortedWeights, key = { it.id }) { entry ->
                        val dateFormatted = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                            .format(Date(entry.timestampEpochMs))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = DarkSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = GymCalculators.formatWeight(entry.weightKg, settings.isKg),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (entry.note.isNotBlank()) {
                                        Text(
                                            text = entry.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (entry.bodyFatPercent != null) {
                                        Text(
                                            text = "%.1f%% BF".format(entry.bodyFatPercent),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteWeightEntry(entry.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // MUSCLE VOLUME HEATMAP TAB
                    item {
                        val muscleCounts = viewModel.getPastWeekMuscleSetCount()
                        MuscleHeatmapView(
                            muscleSetCounts = muscleCounts,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = DarkSurfaceElevated
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "💡 Hypertrophy Volume Guidelines",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Maintenance: 6-8 direct sets/week per muscle\n• Growth (Optimal): 10-20 direct sets/week\n• Maximum Recoverable Volume: 22+ sets/week",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // 1RM COMPOUND LIFTS TAB
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = DarkSurfaceElevated
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Compound Strength 1RM Estimates",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val compoundLifts = listOf(
                                    "Barbell Back Squat" to (if (settings.isKg) 130f else 285f),
                                    "Barbell Bench Press" to (if (settings.isKg) 102f else 225f),
                                    "Barbell Deadlift" to (if (settings.isKg) 160f else 350f),
                                    "Overhead Press (OHP)" to (if (settings.isKg) 65f else 145f),
                                    "Barbell Bent Over Row" to (if (settings.isKg) 85f else 185f)
                                )

                                compoundLifts.forEach { (lift, maxEst) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkSurfaceHighlight)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = lift,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Calculated via Epley standard",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }

                                        Text(
                                            text = GymCalculators.formatWeight(
                                                GymCalculators.weightToKg(maxEst, settings.isKg),
                                                settings.isKg
                                            ),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Log Body Weight Dialog
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
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bodyFatText,
                        onValueChange = { bodyFatText = it },
                        label = { Text("Body Fat % (Optional)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true
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
