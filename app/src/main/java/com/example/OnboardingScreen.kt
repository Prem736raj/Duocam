package com.example

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// High-fidelity dark styling color tokens
val ObsidianBg = Color(0xFF09090B)
val GlassMuted = Color(0x331E1E24)
val GlowPremiumGold = Color(0xFFF59E0B)
val LightAccentGreen = Color(0xFF10B981)
val WarmAccentRed = Color(0xFFEF4444)
val PureWhite = Color(0xFFFFFFFF)
val MutedSlate = Color(0xFF9CA3AF)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    // Total 5 onboard screens requested
    val totalPages = 5
    val pagerState = rememberPagerState(pageCount = { totalPages })
    
    // Outer Obsidian container to provide rich backdrops
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0A0F),
                        Color(0xFF121118),
                        Color(0xFF07060A)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("onboarding_container")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top navigation: Skip and Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic decorative branding logo
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(WarmAccentRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DuoCam",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite,
                        letterSpacing = 1.sp
                    )
                }

                // If not last page, show the small Skip button
                if (pagerState.currentPage < totalPages - 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1F9CA3AF))
                            .clickable {
                                // Save selection and terminate onboarding flow immediately
                                sharedPreferences.edit().putBoolean("has_completed_onboarding", true).apply()
                                onComplete()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "SKIP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = PureWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Central pager workspace containing visual sections
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("onboarding_pager")
            ) { page ->
                when (page) {
                    0 -> OnboardingPageLayout(
                        title = "Record Both Sides",
                        description = "Capture your reaction AND the action — simultaneously",
                        mediaContent = { AnimatedDualCameraPreview() }
                    )
                    1 -> OnboardingPageLayout(
                        title = "Multiple Layouts",
                        description = "PiP, split-screen, diagonal — choose your style",
                        mediaContent = { AnimatedLayoutsPreview() }
                    )
                    2 -> OnboardingPageLayout(
                        title = "Built-in Editor",
                        description = "Trim, add music, filters — edit right in the app",
                        mediaContent = { AnimatedTimelineEditorPreview() }
                    )
                    3 -> OnboardingPageLayout(
                        title = "Creator Tools",
                        description = "Teleprompter, watermark, voice control — made for creators",
                        mediaContent = { AnimatedCreativeSuitePreview() }
                    )
                    4 -> OnboardingPageLayout(
                        title = "Let's Go!",
                        description = "Elevate your creativity with elite dual-camera capture layouts",
                        mediaContent = { CelebrateLetGoPreview() }
                    )
                }
            }

            // Bottom control navigation panel with dots and Primary CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Dot page indicator list
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("onboarding_dots")
                ) {
                    repeat(totalPages) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_width"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) GlowPremiumGold else MutedSlate.copy(alpha = 0.4f),
                            label = "dot_color"
                        )
                        
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                // Right action main button flow control
                if (pagerState.currentPage < totalPages - 1) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = GlowPremiumGold,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("onboarding_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next Onboarding Screen"
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            sharedPreferences.edit().putBoolean("has_completed_onboarding", true).apply()
                            onComplete()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightAccentGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("onboarding_complete_button")
                    ) {
                        Text(
                            text = "Start Recording",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageLayout(
    title: String,
    description: String,
    mediaContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual element panel top-level container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            mediaContent()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // High fidelity descriptive body text labels
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = PureWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp
            ),
            color = MutedSlate,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

/**
 * Screen 1: Record Both Sides - Animated Dual Camera in Action
 */
@Composable
fun AnimatedDualCameraPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "cam_signals")
    
    // Bounce / alternate lens scaling to represent active capturing
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0E14))
            .border(1.5.dp, GlassMuted, RoundedCornerShape(20.dp))
    ) {
        // Split-screen wireframe diagram representing simultaneous Streams
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Front Camera Side
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF13131F))
                    .drawBehind {
                        // Drawing simple decorative grid lines reflecting dual processing channels
                        drawLine(
                            color = Color(0x1F9CA3AF),
                            start = Offset(0f, size.height * 0.33f),
                            end = Offset(size.width, size.height * 0.33f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x1F9CA3AF),
                            start = Offset(0f, size.height * 0.66f),
                            end = Offset(size.width, size.height * 0.66f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x1F9CA3AF),
                            start = Offset(size.width * 0.5f, 0f),
                            end = Offset(size.width * 0.5f, size.height),
                            strokeWidth = 1f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer(scaleX = scaleFactor, scaleY = scaleFactor)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(GlowPremiumGold.copy(alpha = 0.15f))
                            .border(2.dp, GlowPremiumGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = GlowPremiumGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "SELF/VLOG",
                        color = GlowPremiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color(0x3FF59E0B))
            )

            // Rear Camera Side
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF1C1313))
                    .drawBehind {
                        drawLine(
                            color = Color(0x1F9CA3AF),
                            start = Offset(0f, size.height * 0.33f),
                            end = Offset(size.width, size.height * 0.33f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x1F9CA3AF),
                            start = Offset(0f, size.height * 0.66f),
                            end = Offset(size.width, size.height * 0.66f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x1F9CA3AF),
                            start = Offset(size.width * 0.5f, 0f),
                            end = Offset(size.width * 0.5f, size.height),
                            strokeWidth = 1f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer(scaleX = 1f / scaleFactor, scaleY = 1f / scaleFactor)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(WarmAccentRed.copy(alpha = 0.15f))
                            .border(2.dp, WarmAccentRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = WarmAccentRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "SCENE/POV",
                        color = WarmAccentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Concentric shutter camera visual bounds in center
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE609090B))
                .border(0.8.dp, Color(0xFF2E2E34), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Pulsing Red Recording Indicator
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer(alpha = pulseAlpha)
                        .clip(CircleShape)
                        .background(WarmAccentRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DUAL STREAM LIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = PureWhite
                )
            }
        }
    }
}

/**
 * Screen 2: Multiple Layouts - PiP, split, diagonal switching
 */
@Composable
fun AnimatedLayoutsPreview() {
    var activeState by remember { mutableStateOf(0) }
    
    // Cycle every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            activeState = (activeState + 1) % 3
        }
    }
    
    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0E14))
            .border(1.5.dp, GlassMuted, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main decorative viewport
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF09090B))
                    .border(1.2.dp, Color(0xFF2E2E34), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (activeState) {
                    0 -> {
                        // Picture in picture (PiP) layout mockup
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Main large background slice representing scenic frame
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E1313))
                            )
                            
                            // Small floating foreground camera PIP panel with rounded edges
                            Box(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(width = 65.dp, height = 50.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13131F))
                                    .border(1.dp, GlowPremiumGold, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = GlowPremiumGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    1 -> {
                        // Split screen mockup
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF13131F))
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .fillMaxHeight()
                                    .background(PureWhite.copy(alpha = 0.5f))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1E1313))
                            )
                        }
                    }
                    2 -> {
                        // Diagonal split layout mockup
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    // Left top polygon
                                    drawRect(
                                        color = Color(0xFF13131F),
                                        size = size
                                    )
                                    // Right bottom diagonal path
                                    val points = arrayOf(
                                        Offset(size.width, 0f),
                                        Offset(size.width, size.height),
                                        Offset(0f, size.height)
                                    )
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(points[0].x, points[0].y)
                                        lineTo(points[1].x, points[1].y)
                                        lineTo(points[2].x, points[2].y)
                                        close()
                                    }
                                    drawPath(path, Color(0xFF1E1313))
                                    
                                    // Slice line
                                    drawLine(
                                        color = LightAccentGreen,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Layout status tag labels
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tags = listOf("PiP", "SPLIT", "DIAGONAL")
                tags.forEachIndexed { idx, tag ->
                    val isTagActive = activeState == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTagActive) Color(0xFF332000) else Color(0x1F2E2E34))
                            .border(1.dp, if (isTagActive) GlowPremiumGold else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTagActive) GlowPremiumGold else MutedSlate,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 3: Built-In Video Timeline Editor Preview
 */
@Composable
fun AnimatedTimelineEditorPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "editor")
    
    // Animate a sweeping playhead back and forth
    val playheadX by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )
    
    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0E14))
            .border(1.5.dp, GlassMuted, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Mock panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = null, tint = LightAccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TIMELINE EDITOR", color = LightAccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("00:14.30 / 01:00", color = MutedSlate, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            // Visual audio waveform and trimming cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF08080B))
                    .border(1.dp, Color(0xFF1F1F24), RoundedCornerShape(10.dp))
                    .drawBehind {
                        // Drawing timeline background segment paths
                        drawRoundRect(
                            color = Color(0xFF221535),
                            topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                            size = Size(size.width * 0.7f, size.height * 0.7f),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        
                        // Render simulated waveform lines
                        val waveLines = 24
                        val waveStart = size.width * 0.2f
                        val waveEnd = size.width * 0.8f
                        val waveWidthFraction = (waveEnd - waveStart) / waveLines
                        
                        for (i in 0 until waveLines) {
                            val h = if (i % 3 == 0) size.height * 0.5f else if (i % 2 == 0) size.height * 0.35f else size.height * 0.2f
                            val px = waveStart + (i * waveWidthFraction)
                            drawLine(
                                color = if (px < size.width * playheadX) LightAccentGreen else Color(0xFF6B7280),
                                start = Offset(px, (size.height - h) / 2f),
                                end = Offset(px, (size.height + h) / 2f),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                        
                        // Left trimmer handle frame boundary
                        drawLine(
                            color = GlowPremiumGold,
                            start = Offset(size.width * 0.15f, 0f),
                            end = Offset(size.width * 0.15f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                        // Right trimmer handle frame boundary
                        drawLine(
                            color = GlowPremiumGold,
                            start = Offset(size.width * 0.85f, 0f),
                            end = Offset(size.width * 0.85f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                        
                        // Drawing Sweep Playhead
                        val phX = size.width * playheadX
                        drawLine(
                            color = PureWhite,
                            start = Offset(phX, 0f),
                            end = Offset(phX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = PureWhite,
                            center = Offset(phX, 0f),
                            radius = 4.dp.toPx()
                        )
                    }
            )

            // Editor auxiliary tool rows showing functional triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio trigger
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161B16))
                        .border(1.dp, Color(0xFF263326), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = LightAccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Music", color = PureWhite, fontSize = 11.sp)
                    }
                }
                
                // Trim trigger
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1716))
                        .border(1.dp, Color(0xFF332524), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Crop, contentDescription = null, tint = WarmAccentRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trimmer", color = PureWhite, fontSize = 11.sp)
                    }
                }
                
                // Filters trigger
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1C15))
                        .border(1.dp, Color(0xFF332D24), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FilterDrama, contentDescription = null, tint = GlowPremiumGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Filters", color = PureWhite, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Screen 4: Creator Tools Suite - Teleprompter, Voice and Watermark
 */
@Composable
fun AnimatedCreativeSuitePreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "suite")
    
    // Animate teleprompter scrolling lines offset
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scroll"
    )
    
    // Animate vocal ripples expansion
    val microRippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vocal"
    )

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0E14))
            .border(1.5.dp, GlassMuted, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Teleprompter floating frame mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x3D9CA3AF), RoundedCornerShape(10.dp))
                    .background(Color(0xE609090B))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PROMPTER LAYER ACTIVE", color = GlowPremiumGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(12.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Display scrolling prompter lines matching text scroll transitions
                    Column(
                        modifier = Modifier
                            .graphicsLayer(translationY = yOffset)
                            .fillMaxSize()
                    ) {
                        Text("Welcome to the ultimate dual camera studio setup...", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ensure both lenses are highlighted properly and record reactions live...", fontSize = 11.sp, color = MutedSlate)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Keep your eyes on the camera lens grid array for maximum eye contact...", fontSize = 11.sp, color = MutedSlate)
                    }
                }
            }

            // Lower Row featuring Audio Wave Metering and Brand Watermark overlays
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive vocal control module
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(85.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF13131F))
                        .border(1.dp, Color(0xFF232338), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing expanding vocal pulsing circles
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(scaleX = microRippleScale, scaleY = microRippleScale, alpha = 2.1f - microRippleScale)
                                .clip(CircleShape)
                                .background(Color(0x7FEE8F00))
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEE8F00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Text(
                        text = "VOICE OFF/ON",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }

                // Brand Custom Watermark overlay simulator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(85.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161517))
                        .border(1.dp, Color(0xFF272529), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = LightAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "© DuoCam Watermark",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 5: Congratulations & Entry - Lets Go!
 */
@Composable
fun CelebrateLetGoPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "celebrate")
    
    // Golden Ring dynamic rotating loop configuration
    val rotateCrownAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_spin"
    )

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0E14))
            .border(2.dp, GlowPremiumGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glowing background vector rings simulating gold studio concentricity
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotateCrownAngle)
                        .drawBehind {
                            drawCircle(
                                color = GlowPremiumGold,
                                radius = size.width / 2.2f,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(25f, 25f)
                                    )
                                )
                            )
                        }
                )

                // Concentric inner ring representing premium glass reflection
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF281E10), Color(0xFF0E0D06))
                            )
                        )
                        .border(1.5.dp, GlowPremiumGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 36.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "STUDIO HARDWARE ACTIVE",
                color = GlowPremiumGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your smartphone is fully calibrated to record dual cinematic vlogs successfully.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = MutedSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}
