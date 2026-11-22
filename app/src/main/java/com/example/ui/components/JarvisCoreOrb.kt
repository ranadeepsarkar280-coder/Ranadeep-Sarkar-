package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.assistant.VoiceState
import com.example.ui.theme.ArcBlueDeep
import com.example.ui.theme.ArcCyanSoft
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.VoidBlack
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun JarvisCoreOrb(
    voiceState: VoiceState,
    micRmsLevel: Float,
    isProcessing: Boolean,
    onToggleListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = voiceState != VoiceState.IDLE || isProcessing
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 4500 else 14000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outer_rotation",
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 3200 else 10000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "inner_rotation",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isActive) 1.07f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 800 else 2200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_phase",
    )

    val animatedRms by animateFloatAsState(
        targetValue = micRmsLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 380f),
        label = "animated_rms",
    )

    val ringColor = when (voiceState) {
        VoiceState.ACTIVE_COMMAND_LISTENING -> ElectricBlue
        VoiceState.WAKE_WORD_LISTENING -> StatusOnline
        VoiceState.SPEAKING -> ArcCyanSoft
        VoiceState.IDLE -> ElectricBlue.copy(alpha = 0.55f)
    }

    val stateLabelBn = when {
        isProcessing -> "প্রক্রিয়াকরণ চলছে…"
        voiceState == VoiceState.ACTIVE_COMMAND_LISTENING -> "Boss, শুনছি — আপনার নির্দেশ বলুন"
        voiceState == VoiceState.WAKE_WORD_LISTENING -> "ওয়েক-ওয়ার্ড সক্রিয় (\"হে জার্ভিস\")"
        voiceState == VoiceState.SPEAKING -> "JARVIS উত্তর দিচ্ছে…"
        else -> "ট্যাপ করুন বা \"Hey JARVIS\" বলুন"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(216.dp)
                .testTag("jarvis_core_orb")
                .clip(CircleShape)
                .clickable(onClick = onToggleListen),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f

                // Ambient radial glow behind the RA core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = if (isActive) 0.34f else 0.16f),
                            ArcBlueDeep.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = baseRadius,
                    ),
                    radius = baseRadius,
                    center = center,
                )

                // Outer segmented HUD arc
                val outerInset = 10.dp.toPx()
                val outerArcSize = Size(
                    size.width - outerInset * 2,
                    size.height - outerInset * 2,
                )
                drawArc(
                    color = ringColor.copy(alpha = 0.85f),
                    startAngle = outerRotation,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = outerArcSize,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = ringColor.copy(alpha = 0.55f),
                    startAngle = outerRotation + 140f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = outerArcSize,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = MetallicSilver.copy(alpha = 0.45f),
                    startAngle = outerRotation + 245f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = outerArcSize,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )

                // Inner counter-rotating reactor ring
                val innerInset = 22.dp.toPx()
                val innerArcSize = Size(
                    size.width - innerInset * 2,
                    size.height - innerInset * 2,
                )
                drawArc(
                    color = ArcCyanSoft.copy(alpha = if (isActive) 0.9f else 0.35f),
                    startAngle = innerRotation,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = innerArcSize,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = ArcBlueDeep.copy(alpha = 0.65f),
                    startAngle = innerRotation + 180f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = innerArcSize,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Large circular RA logo centerpiece
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(VoidBlack)
                    .border(
                        width = 1.5.dp,
                        color = ringColor.copy(alpha = 0.8f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ra_logo_core_1795334799778),
                    contentDescription = stringResource(id = R.string.cd_ra_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Voice Waveform Animation
        VoiceWaveformStrip(
            voiceState = voiceState,
            rmsLevel = animatedRms,
            wavePhase = wavePhase,
            isProcessing = isProcessing,
            accentColor = ringColor,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stateLabelBn,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isActive) ArcCyanSoft else MetallicSilver.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun VoiceWaveformStrip(
    voiceState: VoiceState,
    rmsLevel: Float,
    wavePhase: Float,
    isProcessing: Boolean,
    accentColor: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.74f)
            .height(32.dp)
            .padding(horizontal = 8.dp)
            .testTag("voice_waveform"),
    ) {
        val barCount = 24
        val totalWidth = size.width
        val gap = 4.dp.toPx()
        val barWidth = ((totalWidth - gap * (barCount - 1)) / barCount).coerceAtLeast(2.dp.toPx())
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalizedPos = i.toFloat() / (barCount - 1).toFloat()
            val envelope = sin(normalizedPos * PI).toFloat()
            val harmonic = (sin(wavePhase + i * 0.55f) + 1f) * 0.5f

            val dynamicAmplitude = when {
                voiceState == VoiceState.ACTIVE_COMMAND_LISTENING ||
                    voiceState == VoiceState.WAKE_WORD_LISTENING -> {
                    val energy = (0.28f + rmsLevel * 0.72f)
                    (0.18f + harmonic * energy) * envelope
                }
                voiceState == VoiceState.SPEAKING || isProcessing -> {
                    (0.24f + harmonic * 0.68f) * envelope
                }
                else -> {
                    (0.10f + harmonic * 0.16f) * envelope
                }
            }

            val barHeight = (size.height * dynamicAmplitude).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = accentColor.copy(alpha = if (voiceState == VoiceState.IDLE) 0.4f else 0.92f),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
