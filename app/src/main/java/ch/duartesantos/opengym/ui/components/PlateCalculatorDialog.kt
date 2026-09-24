package ch.duartesantos.opengym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.ui.window.Dialog
import ch.duartesantos.opengym.ui.theme.*

@Composable
fun PlateCalculatorDialog(
    targetWeight: Float,
    isKg: Boolean,
    onDismiss: () -> Unit
) {
    var weightInput by remember { mutableStateOf(targetWeight) }
    val barWeight = if (isKg) 20f else 45f
    val availablePlates = if (isKg) {
        listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)
    } else {
        listOf(45f, 35f, 25f, 10f, 5f, 2.5f)
    }

    val platesPerSide = remember(weightInput, isKg) {
        val remainingForPlates = (weightInput - barWeight).coerceAtLeast(0f)
        val perSideTarget = remainingForPlates / 2f
        var remainder = perSideTarget
        val result = mutableListOf<Float>()

        for (plate in availablePlates) {
            while (remainder >= plate - 0.01f) {
                result.add(plate)
                remainder -= plate
            }
        }
        result
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("plate_calculator_dialog"),
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Barbell Plate Calculator",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weight display & adjustment
                Text(
                    text = "Target: %.1f %s".format(weightInput, if (isKg) "kg" else "lbs"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Standard Bar: %.0f %s".format(barWeight, if (isKg) "kg" else "lbs"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick increment buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val inc = if (isKg) 2.5f else 5f
                    Button(
                        onClick = { weightInput = (weightInput - inc).coerceAtLeast(barWeight) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight)
                    ) {
                        Text("-$inc")
                    }
                    Button(
                        onClick = { weightInput += inc },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight)
                    ) {
                        Text("+$inc")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PLATES PER SIDE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (platesPerSide.isEmpty()) {
                    Text(
                        text = "Bar only — no extra plates needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(platesPerSide) { plate ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(getPlateColor(plate, isKg))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (plate % 1f == 0f) "%.0f".format(plate) else "%.1f".format(plate),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getPlateColor(weight: Float, isKg: Boolean): Color {
    return if (isKg) {
        when {
            weight >= 25f -> Color(0xFFD32F2F) // Red
            weight >= 20f -> Color(0xFF1976D2) // Blue
            weight >= 15f -> Color(0xFFFBC02D) // Yellow
            weight >= 10f -> Color(0xFF388E3C) // Green
            weight >= 5f -> Color(0xFFFFFFFF)  // White
            else -> Color(0xFF757575)          // Grey
        }
    } else {
        when {
            weight >= 45f -> Color(0xFF1976D2)
            weight >= 35f -> Color(0xFFF57C00)
            weight >= 25f -> Color(0xFF388E3C)
            weight >= 10f -> Color(0xFF7B1FA2)
            else -> Color(0xFF616161)
        }
    }
}
