package ch.duartesantos.opengym.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.duartesantos.opengym.data.model.BodyWeightEntry
import ch.duartesantos.opengym.data.model.GymCalculators
import ch.duartesantos.opengym.ui.theme.*

@Composable
fun BodyWeightChartView(
    entries: List<BodyWeightEntry>,
    targetWeightKg: Float?,
    isKg: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("bodyweight_chart_card"),
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val latest = entries.maxByOrNull { it.timestampEpochMs }
            val unitStr = if (isKg) "kg" else "lbs"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weight Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (latest != null) {
                        Text(
                            text = "Current: " + GymCalculators.formatWeight(latest.weightKg, isKg),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (targetWeightKg != null) {
                    Text(
                        text = "Goal: " + GymCalculators.formatWeight(targetWeightKg, isKg),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log at least 2 weight entries to see trend chart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                val sorted = entries.sortedBy { it.timestampEpochMs }
                val displayWeights = sorted.map { GymCalculators.kgToUserUnit(it.weightKg, isKg) }
                val minWeight = (displayWeights.minOrNull() ?: 60f) - 1.5f
                val maxWeight = (displayWeights.maxOrNull() ?: 90f) + 1.5f
                val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)

                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / (sorted.size - 1).coerceAtLeast(1)

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = h * (i.toFloat() / gridLines)
                        drawLine(
                            color = DividerColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Target line if set
                    if (targetWeightKg != null) {
                        val targetUserVal = GymCalculators.kgToUserUnit(targetWeightKg, isKg)
                        if (targetUserVal in minWeight..maxWeight) {
                            val targetY = h - ((targetUserVal - minWeight) / weightRange * h)
                            drawLine(
                                color = primaryColor.copy(alpha = 0.5f),
                                start = Offset(0f, targetY),
                                end = Offset(w, targetY),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                    }

                    // Build graph path
                    val path = Path()
                    val points = mutableListOf<Offset>()

                    sorted.forEachIndexed { index, entry ->
                        val userVal = GymCalculators.kgToUserUnit(entry.weightKg, isKg)
                        val x = index * stepX
                        val y = h - ((userVal - minWeight) / weightRange * h)
                        points.add(Offset(x, y))

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            // Smooth bezier curve
                            val prev = points[index - 1]
                            val midX = (prev.x + x) / 2
                            path.cubicTo(midX, prev.y, midX, y, x, y)
                        }
                    }

                    // Draw line
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots
                    points.forEach { point ->
                        drawCircle(
                            color = DarkSurface,
                            radius = 5.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }
    }
}
