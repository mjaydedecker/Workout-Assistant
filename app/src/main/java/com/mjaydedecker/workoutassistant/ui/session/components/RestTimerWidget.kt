package com.mjaydedecker.workoutassistant.ui.session.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mjaydedecker.workoutassistant.util.toMmSs

@Composable
fun RestTimerWidget(
    timeRemaining: Long,
    totalSeconds: Int,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    val idle = timeRemaining < 0
    val fraction = if (idle || totalSeconds == 0) 0f
    else (timeRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 300),
        label = "timer_arc"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val displayColor = if (idle) trackColor else color

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics {
            contentDescription = if (idle) "$label: idle"
            else "$label: ${timeRemaining.toMmSs()} remaining"
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)

                drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)

                if (!idle) {
                    drawArc(color = displayColor, startAngle = -90f,
                        sweepAngle = 360f * animatedFraction,
                        useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                }
            }
            Text(
                text = if (idle) "--:--" else timeRemaining.toMmSs(),
                fontSize = 14.sp,
                color = if (idle) MaterialTheme.colorScheme.onSurfaceVariant else displayColor
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
