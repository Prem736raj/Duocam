package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.LocalThemeColors
import java.io.File

// ==========================================
// FAQ DATA MODEL
// ==========================================
data class FaqItem(val question: String, val answer: String)

// ==========================================
// ROADMAP FEATURE MODEL
// ==========================================
data class RoadmapFeature(
    val id: String,
    val title: String,
    val description: String,
    val initialVotes: Int
)

// ==========================================
// HELP & FAQ + HOW-TO SECTION
// ==========================================
@Composable
fun HelpSection(
    context: Context,
    colors: CustomThemeColors,
    modifier: Modifier = Modifier
) {
    val localFaqs = remember {
        listOf(
            FaqItem(
                "How does Dual-Camera recording work?",
                "DuoCam utilizes the native Android Camera2 concurrent stream configuration (where supported). This allows the application to activate and preview from both the front-facing selfie camera and back-facing wide shooter at the exact same time. If your device returns a negative capability check under diagnostics, the app initiates a robust fallback software simulator for rehearsal and practicing."
            ),
            FaqItem(
                "Why does my selfie preview look mirrored?",
                "By default, front cameras mirror the preview layout to feel natural (like a mirror). DuoCam offers a setting under 'Recording Defaults' called 'Mirror Front Camera' where you can toggle mirror projection on or off based on your presenter style, ensuring correct orientation when reviewing exports."
            ),
            FaqItem(
                "Can I record in high-definition or 4K resolution?",
                "DuoCam supports UHD/4K (3840x2160), FHD/1080p, and HD/720p layouts. Available options adjust automatically based on internal device profiles. You can verify peak resolution specifications directly inside the Settings overview under 'Device Capability Diagnostics' section."
            ),
            FaqItem(
                "How do I sync teleprompter speed with my actual reading?",
                "You can select an active teleprompter scrolling mode ('Slow', 'Medium', 'Fast') or fine-tune speed dynamically from 1x up to 15x. For fully customized scripts, remember to add spacing/line breaks to matches your normal speaking rhythm."
            ),
            FaqItem(
                "Does the background service support audio?",
                "Yes! The persistent recording service is a complete background foreground recorder. It capture clean mono/stereo AAC audio streams from your configured device mic, keeping audio capture locked and protected from interruption even if you minimize the application."
            ),
            FaqItem(
                "What should I do if my disk space is running low?",
                "We recommend enabling DuoCam's built-in HEVC/H.265 hardware video compression inside settings. HEVC reduces recording file size by up to 50% with lossless clarity. You can also configure automatic deletion files older than a chosen threshold."
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // HOW TO GUIDES HEADER
        SectionTitleHeader(title = "Interactive How-To Guides", icon = Icons.Rounded.Book, colors = colors)
        
        // HOW TO DUAL CAMERA
        HowToCard(
            title = "1. Master Dual-Camera Modes",
            description = "Learn how to preview, position, and control both camera views simultaneously.",
            canvasContent = { progress -> CanvasHowToCamera(progress, colors) },
            steps = listOf(
                "Tap the visual layers button on the main toolbar to select a preset.",
                "Choose Picture-in-Picture (PIP) for presenter-reaction, Split-Screen (50/50), or Single.",
                "In Picture-in-Picture mode, simply tap and slide the floating box to any corner."
            ),
            colors = colors
        )

        // HOW TO TELEPROMPTER
        HowToCard(
            title = "2. Fluid Teleprompter Control",
            description = "Maintain full eye contact with your audience while reading speaker notes fluidly.",
            canvasContent = { progress -> CanvasHowToPrompter(progress, colors) },
            steps = listOf(
                "Tap the document list icon in the top utility panel to open scripts.",
                "Create a new prompt, paste text, or load built-in speaker tutorials.",
                "Tailor scrolling rate, fonts, background opacity, and color overlays to matches ambient lights."
            ),
            colors = colors
        )

        // HOW TO WATERMARKS
        HowToCard(
            title = "3. Create Beautiful Brand Overlays",
            description = "Overlay customizable icons, current timestamp statistics, or credits automatically.",
            canvasContent = { progress -> CanvasHowToWatermark(progress, colors) },
            steps = listOf(
                "Access the layered badge option to launch the Watermark Control Center.",
                "Tap '+' to generate a customized brand block with custom strings or stamps.",
                "Reposition using the hot-corners guides, save, and record with live burn-in values."
            ),
            colors = colors
        )

        Spacer(modifier = Modifier.height(8.dp))

        // FAQ HEADER
        SectionTitleHeader(title = "Frequently Asked Questions", icon = Icons.Rounded.QuestionAnswer, colors = colors)

        // FAQ EXPANDABLE ITEMS
        localFaqs.forEach { faq ->
            FaqExpandableRow(question = faq.question, answer = faq.answer, colors = colors)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CONTACT SUPPORT BANNER
        ContactSupportCard(context = context, colors = colors)
    }
}

@Composable
fun SectionTitleHeader(title: String, icon: ImageVector, colors: CustomThemeColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = colors.text
        )
    }
}

@Composable
fun HowToCard(
    title: String,
    description: String,
    canvasContent: @Composable (Float) -> Unit,
    steps: List<String>,
    colors: CustomThemeColors
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Smooth infinite progress for animated canvas guides
    val infiniteTransition = rememberInfiniteTransition(label = "howto_loop")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.text
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Animated Interactive Canvas View representing steps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F0F12))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        canvasContent(animProgress)
                    }

                    // Stepper description list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = colors.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// HOW TO: CAMERA CANVAS ANIMATION
@Composable
fun CanvasHowToCamera(progress: Float, colors: CustomThemeColors) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Draw primary camera screen (back view landscape aspect)
        val rectW = 200.dp.toPx()
        val rectH = 100.dp.toPx()
        val rx = (width - rectW) / 2f
        val ry = (height - rectH) / 2f
        
        // Main camera frame
        drawRoundRect(
            color = colors.border,
            topLeft = Offset(rx, ry),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(10.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Split vertical divider (representing software layout grids)
        drawLine(
            color = colors.border.copy(alpha = 0.3f),
            start = Offset(width / 2f, ry),
            end = Offset(width / 2f, ry + rectH),
            strokeWidth = 1.dp.toPx()
        )
        
        // Small landscape scenery representing back camera view (sun & mountain)
        drawCircle(
            color = Color(0x30FFD700),
            radius = 12.dp.toPx(),
            center = Offset(rx + 40.dp.toPx(), ry + 35.dp.toPx())
        )
        
        // Drag progress calculation for PIP (starts at top right, slides to bottom right, slides to bottom left, loops)
        val pathStage = (progress * 4) % 4
        val pipPos = when {
            pathStage < 1f -> {
                // Top Right to Bottom Right
                val lProg = pathStage
                Offset(
                    rx + rectW - 45.dp.toPx(),
                    ry + 10.dp.toPx() + (rectH - 45.dp.toPx()) * lProg
                )
            }
            pathStage < 2f -> {
                // Bottom Right to Bottom Left
                val lProg = pathStage - 1f
                Offset(
                    (rx + rectW - 45.dp.toPx()) - (rectW - 55.dp.toPx()) * lProg,
                    ry + rectH - 35.dp.toPx()
                )
            }
            pathStage < 3f -> {
                // Bottom Left to Top Left
                val lProg = pathStage - 2f
                Offset(
                    rx + 10.dp.toPx(),
                    (ry + rectH - 35.dp.toPx()) - (rectH - 45.dp.toPx()) * lProg
                )
            }
            else -> {
                // Top Left to Top Right
                val lProg = pathStage - 3f
                Offset(
                    rx + 10.dp.toPx() + (rectW - 55.dp.toPx()) * lProg,
                    ry + 10.dp.toPx()
                )
            }
        }
        
        // Draw the floating Picture-In-Picture module
        drawRoundRect(
            color = colors.primary,
            topLeft = pipPos,
            size = Size(35.dp.toPx(), 25.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        // Little face inside PIP
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 4.dp.toPx(),
            center = Offset(pipPos.x + 17.5.dp.toPx(), pipPos.y + 10.dp.toPx())
        )
        
        // Mini title tag
        drawRoundRect(
            color = colors.primary.copy(alpha = 0.15f),
            topLeft = Offset(rx + 8.dp.toPx(), ry + 8.dp.toPx()),
            size = Size(40.dp.toPx(), 12.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
    }
}

// HOW TO: PROMPTER CANVAS ANIMATION
@Composable
fun CanvasHowToPrompter(progress: Float, colors: CustomThemeColors) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val rectW = 220.dp.toPx()
        val rectH = 90.dp.toPx()
        val rx = (width - rectW) / 2f
        val ry = (height - rectH) / 2f
        
        // Mirror teleprompter glass hood
        drawRoundRect(
            color = colors.border,
            topLeft = Offset(rx, ry),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw 5 moving text-representing horizontal lines
        val lineCount = 6
        for (i in 0 until lineCount) {
            // Compute vertical scroll progress
            val itemBaseY = ry + 80.dp.toPx() - (18.dp.toPx() * i)
            val scrolledY = itemBaseY - (25.dp.toPx() * progress)
            
            // Constrain within glass height limits
            if (scrolledY > ry + 4.dp.toPx() && scrolledY < ry + rectH - 10.dp.toPx()) {
                val opacity = ((scrolledY - ry) / rectH).coerceIn(0f, 1f)
                val lineLength = if (i % 2 == 0) 130.dp.toPx() else 95.dp.toPx()
                val lineOffset = (rectW - lineLength) / 2f
                
                // Highlight middle line in active Neon Accent color
                val isMiddle = scrolledY > ry + 30.dp.toPx() && scrolledY < ry + 55.dp.toPx()
                val lineColor = if (isMiddle) colors.primary else Color.White.copy(alpha = 0.5f)
                
                drawRoundRect(
                    color = lineColor.copy(alpha = opacity),
                    topLeft = Offset(rx + lineOffset, scrolledY),
                    size = Size(lineLength, 6.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
        
        // Little target reading focus guide brackets
        val bracketOffset = 25.dp.toPx()
        val bracketY = ry + 42.dp.toPx()
        // Left
        drawLine(
            color = colors.primary,
            start = Offset(rx + 8.dp.toPx(), bracketY),
            end = Offset(rx + 16.dp.toPx(), bracketY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Right
        drawLine(
            color = colors.primary,
            start = Offset(rx + rectW - 16.dp.toPx(), bracketY),
            end = Offset(rx + rectW - 8.dp.toPx(), bracketY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// HOW TO: WATERMARK CANVAS ANIMATION
@Composable
fun CanvasHowToWatermark(progress: Float, colors: CustomThemeColors) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val rectW = 160.dp.toPx()
        val rectH = 100.dp.toPx()
        val rx = (width - rectW) / 2f
        val ry = (height - rectH) / 2f
        
        // Display frame
        drawRoundRect(
            color = colors.border,
            topLeft = Offset(rx, ry),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Recording red blinking indicator
        val blinkAlpha = if (progress < 0.5f) 1f else 0.15f
        drawCircle(
            color = Color.Red.copy(alpha = blinkAlpha),
            radius = 4.dp.toPx(),
            center = Offset(rx + 15.dp.toPx(), ry + 15.dp.toPx())
        )
        
        // We draw the watermark badge resizing/scaling
        val scaleRatio = 1f + 0.15f * kotlin.math.sin(progress * 2 * kotlin.math.PI.toFloat())
        val badgeW = 60.dp.toPx() * scaleRatio
        val badgeH = 20.dp.toPx() * scaleRatio
        
        // Animated Watermark position: bottom right corner with margin adjustment
        val badgeX = rx + rectW - badgeW - 12.dp.toPx()
        val badgeY = ry + rectH - badgeH - 12.dp.toPx()
        
        // Draw glowing watermark container
        drawRoundRect(
            color = colors.primary.copy(alpha = 0.15f),
            topLeft = Offset(badgeX, badgeY),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(4.dp.toPx() * scaleRatio)
        )
        
        drawRoundRect(
            color = colors.primary.copy(alpha = 0.5f),
            topLeft = Offset(badgeX, badgeY),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(4.dp.toPx() * scaleRatio),
            style = Stroke(width = 1.dp.toPx() * scaleRatio)
        )
        
        // Tiny mock logo star inside watermark
        drawCircle(
            color = colors.primary,
            radius = 3.dp.toPx() * scaleRatio,
            center = Offset(badgeX + 8.dp.toPx() * scaleRatio, badgeY + 10.dp.toPx() * scaleRatio)
        )
        
        // Tiny text line next to star
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(badgeX + 16.dp.toPx() * scaleRatio, badgeY + 8.dp.toPx() * scaleRatio),
            size = Size(35.dp.toPx() * scaleRatio, 4.dp.toPx() * scaleRatio),
            cornerRadius = CornerRadius(1.5.dp.toPx() * scaleRatio)
        )
    }
}

@Composable
fun FaqExpandableRow(
    question: String,
    answer: String,
    colors: CustomThemeColors
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                ),
                color = colors.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = if (expanded) "Close" else "Expand",
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp
                ),
                color = colors.textMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ContactSupportCard(context: Context, colors: CustomThemeColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryLight.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SupportAgent,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = "Encountered technical issues?",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.text,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Contact our developer support team directly. We'll automatically package app configuration data to help resolve diagnostic queries faster.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Button(
                onClick = {
                    sendSupportEmail(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contact Support",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun sendSupportEmail(context: Context) {
    val emailString = "support@duocam.com"
    val subjectString = "DuoCam Technical Support Request"
    
    val freeBytes = File(context.filesDir.absolutePath).freeSpace
    val freeSpaceMB = freeBytes / (1024 * 1024)
    
    val diagnosticBody = """
        -----------------------------
        DIAGNOSTIC SYSTEM REPORT (DuoCam)
        -----------------------------
        App Identifier: ${context.packageName}
        Device Model: ${Build.MANUFACTURER} ${Build.MODEL}
        Product Board: ${Build.BOARD}
        Device Hardware: ${Build.HARDWARE}
        Android OS version: ${Build.VERSION.RELEASE} (SDK API ${Build.VERSION.SDK_INT})
        Build Fingerprint: ${Build.FINGERPRINT}
        Available Directory Free Space: ${freeSpaceMB} MB
        -----------------------------
        
        Please detail your inquiry, unexpected findings, or suggestions:
        
    """.trimIndent()

    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailString))
            putExtra(Intent.EXTRA_SUBJECT, subjectString)
            putExtra(Intent.EXTRA_TEXT, diagnosticBody)
        }
        context.startActivity(Intent.createChooser(intent, "Send support ticket via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No supportive email application found.", Toast.LENGTH_LONG).show()
    }
}

// ==========================================
// FEEDBACK & VOTING ROADMAP SECTION
// ==========================================
@Composable
fun FeedbackSection(
    context: Context,
    colors: CustomThemeColors,
    sharedPreferences: android.content.SharedPreferences,
    modifier: Modifier = Modifier
) {
    // Local Feedback Form States
    var selectedCategory by remember { mutableStateOf("Bug Report") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var descriptionText by remember { mutableStateOf("") }
    
    // Attachment States: Simulating capturing or attaching gallery files easily
    var isAttachmentSelected by remember { mutableStateOf(false) }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentSizeVal by remember { mutableStateOf("") }
    
    var feedbackSuccessVisible by remember { mutableStateOf(false) }

    // Dynamic state management for roadmap features upvoting
    val featureList = remember {
        listOf(
            RoadmapFeature("f1", "Smart AI Green-Screen Cam", "Replace background live with custom static graphics or virtual backgrounds without hardware setup.", 342),
            RoadmapFeature("f2", "Multi-Device sync Teleprompter", "Control scripts scrolling speed or edit texts in real-time from an external mobile or tablet.", 218),
            RoadmapFeature("f3", "Intelligent Noise-Gate Equalizer", "Filters persistent minor high-frequency dynamic frequencies natively for flawless voice captures.", 184),
            RoadmapFeature("f4", "Burn-In Captions Generator", "Transforms presenter voice output immediately into aligned subtitles burnt directly into export clips.", 295),
            RoadmapFeature("f5", "Floating Widgets Controller Overlay", "Overlay adjustable transcription scripts on top of third party streaming applications.", 153)
        )
    }

    // Load feature vote counts and already voted tracks
    val FeatureVoteComponent: @Composable (RoadmapFeature) -> Unit = { roadmap ->
        val featureKey = "vote_cast_v3_${roadmap.id}"
        val initialVoteState = sharedPreferences.getBoolean(featureKey, false)
        var hasVotedLocal by remember { mutableStateOf(initialVoteState) }
        var currentVotesCounter by remember { mutableStateOf(roadmap.initialVotes + if (initialVoteState) 1 else 0) }

        // Spring animation on vote state change
        val upScale by animateFloatAsState(
            targetValue = if (hasVotedLocal) 1.15f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
            label = "votespring"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (hasVotedLocal) colors.primary.copy(alpha = 0.6f) else colors.border.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    if (hasVotedLocal) colors.primaryLight.copy(alpha = 0.08f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = roadmap.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.text
                    )
                    if (hasVotedLocal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "YOUR VOTE CAST",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = roadmap.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = colors.textMuted
                )
            }

            // Polished Upvote pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasVotedLocal) colors.primary else colors.surface
                    )
                    .clickable {
                        val nextVoteState = !hasVotedLocal
                        hasVotedLocal = nextVoteState
                        sharedPreferences.edit().putBoolean(featureKey, nextVoteState).apply()
                        currentVotesCounter += if (nextVoteState) 1 else -1
                        val toastMsg = if (nextVoteState) "Roadmap request logged! Thank you for supporting." else "Vote cancelled."
                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (hasVotedLocal) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt,
                        contentDescription = "Upvote key",
                        tint = if (hasVotedLocal) Color.White else colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$currentVotesCounter",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (hasVotedLocal) Color.White else colors.text
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // RATE THE APP CTA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Love using DuoCam?",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.text
                    )
                    Text(
                        text = "Support developers by reviewing us on the Play Store. Rating directly encourages new feature additions!",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
                
                Button(
                    onClick = {
                        rateAppStore(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("rate_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rate Now",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // SEND FEEDBACK HEADER
        SectionTitleHeader(title = "App feedback control centre", icon = Icons.Rounded.RateReview, colors = colors)

        // FORM CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CATEGORY DROPDOWN SELECTOR
                Text(
                    text = "Feedback Category",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textMuted
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F0F12))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("feedback_category_dropdown"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.text
                        )
                        Icon(
                            imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = "Arrow dropdown",
                            tint = colors.textMuted
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    ) {
                        listOf("Bug Report/Issue", "Feature Request", "General Feedback/Other").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, style = MaterialTheme.typography.bodySmall, color = colors.text) },
                                onClick = {
                                    selectedCategory = cat
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // DESCRIPTION TEXT FIELD
                Text(
                    text = "Describe your experience",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textMuted
                )
                
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    placeholder = {
                        Text(
                            "Type any unexpected findings or detail request instructions here...", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = colors.textMuted.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .testTag("feedback_description_text"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = Color(0xFF0F0F12),
                        unfocusedContainerColor = Color(0xFF0F0F12),
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.primary
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // COMPONENT VALUE DISPLAY CHAR COUNT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${descriptionText.length} characters",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )
                }

                // OPTIONAL ATTACHMENT ACTION BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Optional attachment",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textMuted
                    )
                    
                    if (!isAttachmentSelected) {
                        Button(
                            onClick = {
                                // Simulate adding screenshot
                                isAttachmentSelected = true
                                attachmentName = "screenshot_viewfinder_duo_${System.currentTimeMillis() % 10000}.png"
                                attachmentSizeVal = "1.2 MB"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B1B1E),
                                contentColor = colors.text
                            ),
                            border = BorderStroke(1.dp, colors.border),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_attachment_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = colors.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attach Screenshot", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        }
                    }
                }

                // ATTACHMENT CARD IF ADDED
                if (isAttachmentSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryLight.copy(alpha = 0.05f))
                            .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mock image icon representation
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = attachmentName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = colors.text
                                )
                                Text(
                                    text = attachmentSizeVal,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = colors.textMuted
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C1619))
                                .clickable {
                                    isAttachmentSelected = false
                                    attachmentName = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Attachment",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // SUBMIT FEEDBACK BUTTON
                Button(
                    onClick = {
                        feedbackSuccessVisible = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("submit_feedback_button"),
                    enabled = descriptionText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White,
                        disabledContainerColor = colors.primary.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "SUBMIT SECURE FEEDBACK",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ROADMAP VOTING HEADER
        SectionTitleHeader(title = "Proposed Features & Voting Roadmap", icon = Icons.Rounded.ListAlt, colors = colors)
        
        Text(
            text = "Cast your vote for features you want us to prioritize next. Vote counts are refreshed globally.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        // ROADMAP VOTE STRIPS
        featureList.forEach { item ->
            FeatureVoteComponent(item)
        }
    }

    // SUCCESS CONFIRMATION OVERLAY FOR POLISH
    if (feedbackSuccessVisible) {
        AlertDialog(
            onDismissRequest = {
                feedbackSuccessVisible = false
                descriptionText = ""
                isAttachmentSelected = false
            },
            containerColor = colors.surface,
            tonalElevation = 6.dp,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Feedback Dispatched Successfully",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    textAlign = TextAlign.Center,
                    color = colors.text
                )
            },
            text = {
                Text(
                    "Your telemetry logs, descriptions, and mock attachments have been successfully packaged and transmitted to the DuoCam developer feedback gateway.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    textAlign = TextAlign.Center,
                    color = colors.textMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedbackSuccessVisible = false
                        descriptionText = ""
                        isAttachmentSelected = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)
                ) {
                    Text("OK", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        )
    }
}

private fun rateAppStore(context: Context) {
    val packName = context.packageName
    val uri = Uri.parse("market://details?id=$packName")
    val goToPlayStore = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
    try {
        context.startActivity(goToPlayStore)
    } catch (e: Exception) {
        val webStore = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packName"))
        context.startActivity(webStore)
    }
}

// ==========================================
// ABOUT SECTION
// ==========================================
@Composable
fun AboutSection(
    context: Context,
    colors: CustomThemeColors,
    modifier: Modifier = Modifier
) {
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val packageVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "2.5.0 (202606)"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // APP VISUAL BANNER (Cosmic minimal layout icon placeholder)
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(colors.primary, colors.primary.copy(alpha = 0.4f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FlipCameraAndroid,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        // TITLE & DESCRIPTION INFO
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DuoCam Pro Studio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                color = colors.text
            )
            Text(
                text = "Version: $packageVersion",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.textMuted
            )
        }

        Text(
            text = "DuoCam is a highly reliable persistent dual-camera teleprompter studio built specifically for video content creators, journalists, educators, and social media presenters.",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        HorizontalDivider(color = colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

        // WHAT'S NEW CHANGELOG
        SectionTitleHeader(title = "What's New in DuoCam", icon = Icons.Rounded.Update, colors = colors)
        
        ChangelogTimeline(
            releases = listOf(
                "v2.5.0" to listOf(
                    "Background video persistent recording support with foreground notification service.",
                    "System notifications when dual recordings complete and low disk capacity alerts.",
                    "Auto-Enable Do Not Disturb (DND) options to suppress incoming calls and audio focus losses."
                ),
                "v2.4.2" to listOf(
                    "Direct Google Drive backup integration and custom cloud synchronizations cards.",
                    "Watermark manager panel: custom overlay stars, brand badges, and hot keys."
                ),
                "v2.3.0" to listOf(
                    "Speech-Activated teleprompter controller with custom speed meters (1x to 15x)."
                )
            ),
            colors = colors
        )

        HorizontalDivider(color = colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

        // ACKNOWLEDGEMENTS
        SectionTitleHeader(title = "Credits & Acknowledgments", icon = Icons.Rounded.Hub, colors = colors)
        
        Text(
            text = "This recording studio application was developed under Android Jetpack Compose standard and leverages dynamic hardware camera routing, automated SQLite backup databases, ExoPlayer hardware codec integrations, and the Material Design 3 guidelines system.\nSpecial thanks to developers, design artists (icons library), and Google AI Studio builds runtime components.",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // LEGAL BUTTONS ROW
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegalRowButton(
                title = "Privacy Policy",
                icon = Icons.Rounded.PrivacyTip,
                onClick = { showPrivacyDialog = true },
                colors = colors,
                testTag = "btn_privacy_policy"
            )
            LegalRowButton(
                title = "Terms of Service",
                icon = Icons.Rounded.Gavel,
                onClick = { showTermsDialog = true },
                colors = colors,
                testTag = "btn_terms_of_service"
            )
            LegalRowButton(
                title = "Open Source Licenses",
                icon = Icons.Rounded.CardMembership,
                onClick = { showLicensesDialog = true },
                colors = colors,
                testTag = "btn_open_source_licenses"
            )
        }
    }

    // POLICY OVERLAYS
    if (showPrivacyDialog) {
        LegalDialog(
            title = "Privacy Policy",
            content = getPrivacyPolicyText(),
            onDismiss = { showPrivacyDialog = false },
            colors = colors
        )
    }

    if (showTermsDialog) {
        LegalDialog(
            title = "Terms of Service",
            content = getTermsOfServiceText(),
            onDismiss = { showTermsDialog = false },
            colors = colors
        )
    }

    if (showLicensesDialog) {
        LegalDialog(
            title = "Open Source Library Licenses",
            content = getOpenSourceLicensesText(),
            onDismiss = { showLicensesDialog = false },
            colors = colors
        )
    }
}

@Composable
fun ChangelogTimeline(
    releases: List<Pair<String, List<String>>>,
    colors: CustomThemeColors
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        releases.forEach { (version, items) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Vertical Timeline Bar indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(55.dp)
                            .background(colors.border)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Release specifications
                Column {
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = colors.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    items.forEach { bullet ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "• ",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalRowButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    colors: CustomThemeColors,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface.copy(alpha = 0.5f))
            .border(1.dp, colors.border.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                title, 
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.text
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun LegalDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    colors: CustomThemeColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.70f),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.text
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = colors.textMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)
            ) {
                Text("DISMISS", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    )
}

// POLICY STRING RESOURCE MOCK VALUES FOR LAUNCH
private fun getPrivacyPolicyText(): String {
    return """
        DUOCAM PRO PRIVACY POLICY
        Effective Date: June 3, 2026
        
        1. Overview
        Your privacy is paramount. DuoCam is designed to process and store all audio and video captures locally directly within your device's secured system partitions. We do NOT maintain background server channels to capture, aggregate, index, transmit or profile your personal recordings.
        
        2. Media Capture and Permissions
        - Camera Feed: Standard Camera controls capture physical frames locally during sessions. Streams do not touch third-party pipelines or unauthenticated clouds.
        - Microphone Audio: Feeds record audio directly using AAC standards. Audio sessions are bound safely within the foreground system lifecycle.
        - Storage: Data writes to Movies/DuoCam index directory.
        
        3. Cloud Synchronisation Integration
        If you voluntarily configure Google Drive, credentials remain encapsulated inside Google Play integration services. DuoCam initiates single session connections to transmit selected files only under explicit user action.
        
        4. Diagnostics and Telemetry
        Feedback forms include simple device manufacturer versions and platform limits strictly to diagnose failures crash bugs locally.
        
        5. Contact Us
        Should queries arise regarding policies, send support email to privacy@duocam.com.
    """.trimIndent()
}

private fun getTermsOfServiceText(): String {
    return """
        DUOCAM STUDIO TERMS OF SERVICE
        Effective Date: June 3, 2026
        
        1. Agreement and usage boundaries
        By installing and utilizing DuoCam Pro, you enter a local usage agreement. Users agree to leverage camera-recording and background streaming services safely according to regional privacy capture regulations.
        
        2. Prohibited activities
        You agree strictly not to capture or stream non-consensual recordings of public/private citizens, covert operations, or leverage telemetry routing to transmit malicious elements.
        
        3. Local data liability disclaimers
        DuoCam operates primarily as an offline-first container. Users bear 100% of the responsibility to manage available disk spaces, backup synchronizations, and secure deletion histories. Discarding active recording deletes cached audio immediately with no backup recovery channels available on our servers.
        
        4. System Compatibility Restrictions
        Manufacturer restrictions can block concurrent front/rear capabilities arbitrarily. Fallback software controls are supplied in settings for local diagnostic and simulator practice use cases.
        
        5. Term Updates
        We reserve rights to adapt regulatory terms incrementally without immediate notice.
    """.trimIndent()
}

private fun getOpenSourceLicensesText(): String {
    return """
        OPEN-SOURCE ACKNOWLEDGEMENTS
        DuoCam is constructed proudly over open source libraries:
        
        1. Kotlin Standard Library
        Copyright (c) 2010-2026 JetBrains s.r.o.
        Licensed under Apache License Version 2.0.
        
        2. Jetpack Compose & Material Components
        Copyright (c) 2018-2026 The Android Open Source Project.
        Licensed under Apache License Version 2.0.
        
        3. ExoPlayer Core Media Suite
        Copyright (c) 2016-2026 Google LLC.
        Licensed under Apache License Version 2.0.
        
        4. Room Persistence Architecture
        Copyright (c) 2018-2026 Google LLC.
        Licensed under Apache License Version 2.0.
        
        5. Vico Charting & Graphic Canvas
        Copyright (c) 2022-2026 Patrick Michalik.
        Licensed under MIT License.
    """.trimIndent()
}

// =========================================================================
// PREMIUM GO PRO COMPARISON AND SIMULATED GOOGLE PLAY BILLING SYSTEM
// =========================================================================

data class ComparisonItem(
    val title: String,
    val description: String,
    val freeDesc: String,
    val proDesc: String,
    val isProOnly: Boolean
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GoProScreen(
    context: android.content.Context,
    colors: CustomThemeColors,
    featureName: String? = null,
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
    var showBillingSheet by remember { mutableStateOf(false) }

    val features = remember {
        listOf(
            ComparisonItem("Dual-Cam Resolution", "Record in crisp, high-definition frame sizes", "Max 720p HD Only", "1080p FHD & Ultra HD 4K", false),
            ComparisonItem("Multi-Cam Layouts", "Aesthetic video compositions", "Basic PiP Only", "PiP, Split H/V, Diagonal layouts", false),
            ComparisonItem("Recording Length", "Total capture time limit per recording", "Max 5 Minutes", "Unlimited Duration", false),
            ComparisonItem("Brand Watermarks", "Branding in your recorded exports", "Forced 'DuoCam' mark", "No watermarks or custom designs", false),
            ComparisonItem("Full Editor Suite", "Cinematic post-production tools", "Basic Trim Only", "Merge, Speed, Audio mixer, Texts", false),
            ComparisonItem("Chroma Color Filters", "Professional dynamic color grading", "3 Basic Filters Only", "All 15+ Advanced Color Presets", false),
            ComparisonItem("Speaker Teleprompter", "Maintain perfect eye-contact while reading", "Not Included", "Auto-scrolling scripts helper", true),
            ComparisonItem("Hands-Free Controls", "Voice triggers to start/stop recordings", "Not Included", "Smart Voice Commands active", true),
            ComparisonItem("Safe Cloud Backup", "Safe recovery synchronization", "Not Included", "Automated Cloud Backups active", true)
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60A0A0C))
                .pointerInput(Unit) { detectTapGestures { /* Dismiss blocking */ } },
            contentAlignment = Alignment.Center
        ) {
            // Main Modal Sheet Container
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.90f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(18, 18, 22))
                    .border(
                        BorderStroke(
                            1.5.dp, 
                            Brush.linearGradient(
                                listOf(colors.primary, Color(0xFFFF4081), colors.primary.copy(alpha = 0.2f))
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
                    .testTag("go_pro_modal_container")
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(colors.primary, Color(0xFFFF4081)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DUOCAM PRO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(255, 255, 255, 15))
                            .clickable { onDismiss() }
                            .testTag("dismiss_go_pro_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Context-aware Unlock Banner (if triggered by accessing a locked feature)
                    featureName?.let { name ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Unlock ${name.uppercase()} Feature",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.primary
                                    )
                                    Text(
                                        text = "This feature requires DuoCam Pro. Upgrade today to unlock immediately!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.text
                                    )
                                }
                            }
                        }
                    }

                    // Main exciting Callout Graphic
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.surface.copy(alpha = 0.70f), Color(0xFF0C0A0E))
                                )
                            )
                            .border(1.dp, Color(255,255,255,8), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Take Your Presenter Style in 4K",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Record concurrently without limits. Build cinematic stories, use autoscroll guides, and command DuoCam completely hands-free.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                color = colors.textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.95f)
                            )
                        }
                    }

                    // Feature List Tables Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPARE LIFETIME BENEFITS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = colors.primaryLight
                        )
                    }

                    // Comparison Rows Container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(12, 12, 15))
                            .border(1.dp, Color(255,255,255,6), RoundedCornerShape(16.dp))
                    ) {
                        features.forEachIndexed { idx, item ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(0.5f).padding(end = 8.dp)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = colors.textMuted
                                        )
                                    }

                                    // Free limitations info
                                    Row(
                                        modifier = Modifier.weight(0.25f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (item.isProOnly) Icons.Rounded.Close else Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = if (item.isProOnly) Color(0xFFEF4444) else colors.textMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.freeDesc,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = colors.textMuted,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                    }

                                    // Pro unlocks info
                                    Row(
                                        modifier = Modifier.weight(0.25f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.proDesc,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF10B981),
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (idx < features.size - 1) {
                                    Divider(color = Color(255,255,255,5), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer CTA panel (Pricing and Purchase triggers)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Massive Upgrade Button with dynamic high-contrast lifetime glow
                    Button(
                        onClick = { showBillingSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("unlock_pro_lifetime_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unlock Lifetime DuoCam Pro",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }

                            // Price highlighted
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$4.99",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Assurance text guarantees
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "LIFETIME PURCHASE • NO SUBSCRIPTIONS • 100% SATISFACTION GUARANTEED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = colors.textMuted
                        )
                    }
                }
            }

            // Google Play Billing simulated layout sheet
            if (showBillingSheet) {
                SimulatedGooglePlayBillingSheet(
                    onDismiss = { showBillingSheet = false },
                    onPurchaseCompleted = {
                        sharedPreferences.edit().putBoolean("is_pro_upgraded", true).apply()
                        showBillingSheet = false
                        onPurchaseSuccess()
                        Toast.makeText(context, "DuoCam Pro Activated! Thank you for purchasing.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun SimulatedGooglePlayBillingSheet(
    onDismiss: () -> Unit,
    onPurchaseCompleted: () -> Unit
) {
    var step by remember { mutableStateOf("confirm") } // "confirm", "processing", "success"
    val scope = rememberCoroutineScope()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (step == "confirm") onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = (step == "confirm"),
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Sheet body simulating Google Play bottom modal sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFFFFFFFF)) // Clean Google Play brand white background
                    .padding(24.dp)
                    .testTag("google_play_billing_sheet")
            ) {
                // Header brand row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Play simulated branding badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Dynamic Play Logo elements
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8EAED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF4285F4),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Play",
                            style = TextStyle(
                                color = Color(0xFF202124),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                    }

                    // Account indicator customized to active metadata account!
                    Text(
                        text = "realbon34maxmi@gmail.com",
                        style = TextStyle(
                            color = Color(0xFF5F6368),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Divider(color = Color(0xFFE8EAED), thickness = 1.dp)

                Spacer(modifier = Modifier.height(18.dp))

                when (step) {
                    "confirm" -> {
                        // Product info card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "DuoCam Pro: Lifetime License",
                                    style = TextStyle(
                                        color = Color(0xFF202124),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                )
                                Text(
                                    text = "DuoCam Studio • In-app Purchase",
                                    style = TextStyle(
                                        color = Color(0xFF5F6368),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Text(
                                text = "$4.99",
                                style = TextStyle(
                                    color = Color(0xFF202124),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Payment method options selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8F9FA))
                                .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE8F0FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreditCard,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Google Pay (Visa •••• 1234)",
                                        style = TextStyle(
                                            color = Color(0xFF202124),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Text(
                                        text = "Balance: Ready",
                                        style = TextStyle(
                                            color = Color(0xFF34A853),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF5F6368),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Play protect safe indicator
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = Color(0xFF0F9D58),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Secured by Google Play Billing. Authentic licensing verification.",
                                style = TextStyle(
                                    color = Color(0xFF5F6368),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Green Buy CTA Button
                        Button(
                            onClick = {
                                step = "processing"
                                scope.launch {
                                    delay(1600)
                                    step = "success"
                                    delay(1200)
                                    onPurchaseCompleted()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_play_buy_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF01875F) // Google Play signature green accent
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "1-TAP BUY",
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    "processing" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF01875F),
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(
                                text = "Authorizing transaction with Google Play...",
                                style = TextStyle(
                                    color = Color(0xFF202124),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    "success" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE6F4EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Done,
                                    contentDescription = "Success",
                                    tint = Color(0xFF137333),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "Payment Successful!",
                                style = TextStyle(
                                    color = Color(0xFF137333),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Google Play receipt sent. Activating Premium...",
                                style = TextStyle(
                                    color = Color(0xFF202124),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

