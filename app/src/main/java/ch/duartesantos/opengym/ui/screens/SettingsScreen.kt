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
import ch.duartesantos.opengym.ui.components.AppHeader
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel

@Composable
fun SettingsScreen(
    viewModel: GymViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showTargetWeightDialog by remember { mutableStateOf(false) }

    val accentColors = listOf(
        "#32D74B" to ("Lime" to AccentLime),
        "#0A84FF" to ("Sky" to AccentSky),
        "#FF9F0A" to ("Orange" to AccentOrange),
        "#BF5AF2" to ("Violet" to AccentViolet),
        "#FF375F" to ("Pink" to AccentPink),
        "#FF453A" to ("Red" to AccentRed),
        "#40C8E0" to ("Teal" to AccentTeal),
        "#FFD60A" to ("Gold" to AccentGold)
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "Settings",
                subtitle = "Preferences & App Customization"
            )
        },
        containerColor = BlackBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // UNIT PREFERENCES
            item {
                Text(
                    text = "UNITS & MEASUREMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Weight Units", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Currently using ${if (settings.isKg) "Kilograms (kg)" else "Pounds (lbs)"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceHighlight)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (settings.isKg) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.updateUnit(true) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("KG", color = if (settings.isKg) Color.Black else TextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!settings.isKg) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.updateUnit(false) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("LBS", color = if (!settings.isKg) Color.Black else TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTargetWeightDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Goal Target Weight", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = if (settings.targetWeightKg != null) {
                                        GymCalculators.formatWeight(settings.targetWeightKg!!, settings.isKg)
                                    } else {
                                        "Not set"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }

            // ACCENT COLOR SELECTION
            item {
                Text(
                    text = "ACCENT COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Accent",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(accentColors) { (hex, pair) ->
                                val (name, color) = pair
                                val isSelected = settings.accentColorHex.equals(hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.updateAccentColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // REST TIMER PREFERENCES
            item {
                Text(
                    text = "REST TIMER",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Default Rest Interval", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        val restPresets = listOf(30, 60, 90, 120, 180)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            restPresets.forEach { sec ->
                                val isSelected = settings.defaultRestSeconds == sec
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else DarkSurfaceHighlight)
                                        .clickable { viewModel.updateDefaultRest(sec) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ABOUT OPENGYM
            item {
                Text(
                    text = "ABOUT OPENGYM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("openGym v1.4.0", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Text("Free & Open Source Gym Tracker", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Licensed under GNU AGPL-3.0. No account required, no ads, no cloud sync lock-in, and zero tracking telemetry. Your workouts, your weights, your data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }

    // Target Weight Dialog
    if (showTargetWeightDialog) {
        var targetInput by remember {
            val userUnitVal = settings.targetWeightKg?.let { GymCalculators.kgToUserUnit(it, settings.isKg) } ?: 75f
            mutableStateOf("%.1f".format(userUnitVal))
        }

        AlertDialog(
            onDismissRequest = { showTargetWeightDialog = false },
            title = { Text("Set Target Goal Weight") },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Weight (${if (settings.isKg) "kg" else "lbs"})") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = targetInput.toFloatOrNull()
                        viewModel.updateTargetWeight(v)
                        showTargetWeightDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
