package com.example.ui

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// Re-using same colors from main activity, or redefining here
import com.example.ui.theme.GlowRecordRed
import com.example.ui.theme.GoldenHour
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun CameraStatusOverlays(
    isRecording: Boolean,
    isPaused: Boolean,
    isProcessingRecording: Boolean,
    showSavedOverlay: Boolean,
    activeRecordingUri: Uri?,
    showVideoPlayer: Boolean,
    onShowVideoPlayerChange: (Boolean) -> Unit,
    onShowSavedOverlayChange: (Boolean) -> Unit
) {
    // 1. Recording Pulsing Red or Paused Golden outer border & vignette overlay
    if (isRecording) {
        val infiniteTransition = rememberInfiniteTransition(label = "vignettePulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vignetteAlpha"
        )
        val vignetteColor = if (isPaused) GoldenHour else GlowRecordRed
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.5.dp, vignetteColor.copy(alpha = pulseAlpha), RoundedCornerShape(12.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.Transparent, vignetteColor.copy(alpha = 0.15f * pulseAlpha)),
                        radius = 2200f
                    )
                )
        )
    }

    // 2. Processing / Muxing overlay feedback
    if (isProcessingRecording) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE609090B))
                .pointerInput(Unit) {}, // Consume taps under interaction
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = GlowRecordRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "MUXING HIGH-FIDELITY FEED",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Writing cinematic frames...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }

    // 3. Floating "Saved!" slide-up card with elegant play-back trigger
    if (showSavedOverlay && activeRecordingUri != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 110.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF21C1C1F))
                    .border(1.dp, Color(0xFF2E2E33), RoundedCornerShape(16.dp))
                    .clickable { onShowVideoPlayerChange(true) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small thumbnail representing camera view with red play button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2E2E33)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play recording",
                        tint = GlowRecordRed,
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.5.dp, GlowRecordRed, CircleShape)
                            .padding(3.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SAVED TO GALLERY!",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap to preview instant playback",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextWhite.copy(alpha = 0.8f)
                        )
                    )
                }
                
                IconButton(
                    onClick = { onShowSavedOverlayChange(false) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // 4. Immersive Cinematic Video Player Overlay within the app!
    if (showVideoPlayer && activeRecordingUri != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .pointerInput(Unit) {}, // Consume clicks underneath
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header bar of player
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CINEMATIC PLAYBACK",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = GlowRecordRed
                        )
                        Text(
                            text = "Playing native source loop",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    
                    IconButton(
                        onClick = { onShowVideoPlayerChange(false) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }
                }

                // Main VideoView rendering container representing saved .mp4
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF09090B))
                        .border(1.dp, Color(0xFF1F1F23), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            android.widget.VideoView(context).apply {
                                setVideoURI(activeRecordingUri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom controller play hints
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC18181B))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Playing in Loop",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SWEEP COMPOSITION PLAYING INSTANTLY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextWhite
                        )
                    )
                }
            }
        }
    }
}
