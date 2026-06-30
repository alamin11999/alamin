package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisCoreWidget(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_core_hologram")

    // Slow ambient ring rotation
    val rotationAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotate_outer"
    )

    // Medium counter-ring rotation
    val rotationAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotate_middle"
    )

    // Quick inner ring rotation
    val rotationAngle3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotate_inner"
    )

    // Ambient center core size pulse
    val corePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Glowing particle offsets
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particles"
    )

    // Map RMS dB (normally -2f to 10f in standard recognize) to a clean 0f..1f factor
    val normalizedRms = remember(rmsDb) {
        val clamped = rmsDb.coerceIn(0f, 12f)
        clamped / 12f
    }

    // Dynamic multiplier for active modes
    val activityFactor = when {
        isListening -> 1.4f
        isSpeaking -> 1.8f
        else -> 1.0f
    }

    // Interactive sky/cyan immersive colors
    val neonBlue = Color(0xFF38BDF8) // Sky 400
    val darkNeonBlue = Color(0xFF0EA5E9) // Sky 500
    val neonTeal = Color(0xFF0284C7) // Sky 600
    val hologramYellow = Color(0xFF7DD3FC) // Sky 300

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.width.coerceAtMost(size.height) / 2.6f)

            // 1. Draw central glowing background glow (Hologram radial gradient)
            val glowRadius = baseRadius * 1.5f * (corePulseScale + (normalizedRms * 0.2f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        neonBlue.copy(alpha = 0.25f * activityFactor),
                        darkNeonBlue.copy(alpha = 0.08f * activityFactor),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Draw outer rotating HUD Ring with segmented dashes
            withTransform({
                rotate(rotationAngle1, pivot = center)
            }) {
                val outerRadius = baseRadius * 1.25f
                drawCircle(
                    color = neonBlue.copy(alpha = 0.4f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(
                        width = 4f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(40f, 20f, 10f, 20f),
                            phase = 0f
                        )
                    )
                )

                // Outer telemetry circles
                drawArc(
                    color = hologramYellow.copy(alpha = 0.6f),
                    startAngle = 45f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = center - Offset(outerRadius, outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = hologramYellow.copy(alpha = 0.6f),
                    startAngle = 225f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = center - Offset(outerRadius, outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            // 3. Draw middle reverse rotating HUD Ring with tiny dashes
            withTransform({
                rotate(rotationAngle2, pivot = center)
            }) {
                val middleRadius = baseRadius * 1.05f
                drawCircle(
                    color = neonTeal.copy(alpha = 0.5f),
                    radius = middleRadius,
                    center = center,
                    style = Stroke(
                        width = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(15f, 15f),
                            phase = 0f
                        )
                    )
                )
            }

            // 4. Draw inner rotating HUD telemetry with solid quadrants
            withTransform({
                rotate(rotationAngle3, pivot = center)
            }) {
                val innerRadius = baseRadius * 0.85f
                drawArc(
                    color = neonBlue.copy(alpha = 0.7f),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = center - Offset(innerRadius, innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 8f)
                )
                drawArc(
                    color = neonBlue.copy(alpha = 0.7f),
                    startAngle = 180f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = center - Offset(innerRadius, innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 8f)
                )
            }

            // 5. Draw reactive Audio Waves (orbit waves flaring outwards)
            val waveCount = 40
            val innerRingRadius = baseRadius * 0.7f
            for (i in 0 until waveCount) {
                val angleRad = Math.toRadians((360f / waveCount * i).toDouble())
                // Reactive ripple length based on decibel level and index (for jagged authentic look)
                val reactiveHeight = if (isListening || isSpeaking) {
                    (baseRadius * 0.35f) * normalizedRms * (1f + (i % 3) * 0.4f)
                } else {
                    0f
                }
                
                val startX = center.x + innerRingRadius * cos(angleRad).toFloat()
                val startY = center.y + innerRingRadius * sin(angleRad).toFloat()
                val endX = center.x + (innerRingRadius + 5f + reactiveHeight) * cos(angleRad).toFloat()
                val endY = center.y + (innerRingRadius + 5f + reactiveHeight) * sin(angleRad).toFloat()

                drawLine(
                    color = if (i % 2 == 0) neonBlue else neonTeal,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            // 6. Draw glowing holographic core capsule (Immersive UI: deep-space container, sharp outline, geometric brackets, glowing eye)
            val coreRadius = baseRadius * 0.5f * (corePulseScale + (normalizedRms * 0.15f))
            
            // Solid deep space sphere background
            drawCircle(
                color = Color(0xFF020617),
                radius = coreRadius,
                center = center
            )
            
            // Sharp tech border outline
            drawCircle(
                color = neonBlue.copy(alpha = 0.8f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 4f)
            )

            // Inner Hardware quadrant corner brackets
            val bracketOffset = coreRadius * 0.45f
            val bracketSize = coreRadius * 0.18f
            val strokeW = 3f

            // Top-Left bracket
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x - bracketOffset, center.y - bracketOffset), Offset(center.x - bracketOffset + bracketSize, center.y - bracketOffset), strokeW)
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x - bracketOffset, center.y - bracketOffset), Offset(center.x - bracketOffset, center.y - bracketOffset + bracketSize), strokeW)

            // Top-Right bracket
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x + bracketOffset, center.y - bracketOffset), Offset(center.x + bracketOffset - bracketSize, center.y - bracketOffset), strokeW)
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x + bracketOffset, center.y - bracketOffset), Offset(center.x + bracketOffset, center.y - bracketOffset + bracketSize), strokeW)

            // Bottom-Left bracket
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x - bracketOffset, center.y + bracketOffset), Offset(center.x - bracketOffset + bracketSize, center.y + bracketOffset), strokeW)
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x - bracketOffset, center.y + bracketOffset), Offset(center.x - bracketOffset, center.y + bracketOffset - bracketSize), strokeW)

            // Bottom-Right bracket
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x + bracketOffset, center.y + bracketOffset), Offset(center.x + bracketOffset - bracketSize, center.y + bracketOffset), strokeW)
            drawLine(neonBlue.copy(alpha = 0.6f), Offset(center.x + bracketOffset, center.y + bracketOffset), Offset(center.x + bracketOffset, center.y + bracketOffset - bracketSize), strokeW)

            // The "Eye" (central white blur and sky-300 core)
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = coreRadius * 0.35f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        hologramYellow,
                        neonBlue.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 0.25f
                ),
                radius = coreRadius * 0.25f,
                center = center
            )

            // 7. Draw floating holographic technical particles
            val particleCount = 12
            for (i in 0 until particleCount) {
                val angle = (360f / particleCount) * i
                val rad = Math.toRadians(angle.toDouble())
                // Push particles outwards based on infinite loop spec
                val distance = baseRadius * 1.4f + particleOffset + (normalizedRms * 30f)
                val pX = center.x + distance * cos(rad).toFloat()
                val pY = center.y + distance * sin(rad).toFloat()

                drawCircle(
                    color = neonBlue.copy(alpha = 0.6f),
                    radius = 3f + (normalizedRms * 4f),
                    center = Offset(pX, pY)
                )
            }
        }

        // Micro HUD telemetry labels placed symmetrically around the central core
        Text(
            text = "NEURAL_LINK: 0.04ms",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = hologramYellow.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )

        Text(
            text = when {
                isListening -> "PROCESS: LISTENING"
                isSpeaking -> "PROCESS: TRANSMITTING"
                else -> "PROCESS: STANDBY"
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = hologramYellow.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )

        Text(
            text = "MEM: 94.2GB",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = hologramYellow.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .rotate(-90f)
                .offset(y = (-12).dp)
        )

        Text(
            text = "SYNC: STABLE",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = hologramYellow.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .rotate(90f)
                .offset(y = (-12).dp)
        )
    }
}
