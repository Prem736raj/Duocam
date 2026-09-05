package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.GoldenHour
import com.example.ui.theme.TextWhite
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.BorderGray

enum class ContextualTip(
    val key: String,
    val message: String,
    val testTag: String,
    val title: String
) {
    CAMERA_LAYOUT(
        "tip_seen_camera_layout_v1",
        "Tap the layout icon to change your camera arrangement",
        "tip_seen_camera_layout",
        "Camera Layout"
    ),
    PIP_GESTURE(
        "tip_seen_pip_gesture_v1",
        "Drag the small window to reposition • Pinch to resize • Tap to swap",
        "tip_seen_pip_gesture",
        "PiP Controls"
    ),
    EDITOR_TRIM(
        "tip_seen_editor_trim_v1",
        "Drag the handles to trim your video",
        "tip_seen_editor_trim",
        "Video Trimming"
    ),
    GALLERY_MULTISELECT(
        "tip_seen_gallery_multiselect_v1",
        "Long press to select multiple videos",
        "tip_seen_gallery_multiselect",
        "Batch Operations"
    ),
    TELEPROMPTER_USE(
        "tip_seen_teleprompter_use_v1",
        "Paste your script and set scroll speed",
        "tip_seen_teleprompter_use",
        "Smart Teleprompter"
    )
}

enum class ArrowDirection {
    UP, DOWN, NONE
}

@Composable
fun ContextualTipOverlay(
    tip: ContextualTip,
    sharedPreferences: SharedPreferences,
    alignment: Alignment = Alignment.Center,
    yOffset: Dp = 0.dp,
    arrowDirection: ArrowDirection = ArrowDirection.NONE,
    arrowOffsetX: Dp = 0.dp,
    onDismissed: () -> Unit = {}
) {
    val context = LocalContext.current
    var isVisible by remember {
        mutableStateOf(!sharedPreferences.getBoolean(tip.key, false))
    }

    if (!isVisible) return

    val dismissAction = {
        sharedPreferences.edit().putBoolean(tip.key, true).apply()
        isVisible = false
        onDismissed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9999f)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissAction
            )
    ) {
        Column(
            modifier = Modifier
                .align(alignment)
                .offset(y = yOffset)
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Avoid clicks inside card triggering dismissal
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (arrowDirection == ArrowDirection.UP) {
                ArrowIndicator(direction = ArrowDirection.UP, offsetX = arrowOffsetX)
            }

            // Beautiful Card with Gradient Borders or premium Dark themes
            Box(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianBlack)
                    .border(
                        width = 1.5.dp,
                        color = NeonGreen.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
                    .testTag(tip.testTag)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = "Tip",
                            tint = GoldenHour,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tip.title.uppercase(),
                            color = GoldenHour,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Text(
                        text = tip.message,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = dismissAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("${tip.testTag}_dismiss_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GOT IT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            if (arrowDirection == ArrowDirection.DOWN) {
                ArrowIndicator(direction = ArrowDirection.DOWN, offsetX = arrowOffsetX)
            }
        }
    }
}

@Composable
fun ArrowIndicator(direction: ArrowDirection, offsetX: Dp) {
    Canvas(
        modifier = Modifier
            .size(width = 18.dp, height = 10.dp)
            .offset(x = offsetX)
    ) {
        val path = Path()
        if (direction == ArrowDirection.UP) {
            path.moveTo(size.width / 2f, 0f)
            path.lineTo(0f, size.height)
            path.lineTo(size.width, size.height)
            path.close()
        } else if (direction == ArrowDirection.DOWN) {
            path.moveTo(size.width / 2f, size.height)
            path.lineTo(0f, 0f)
            path.lineTo(size.width, 0f)
            path.close()
        }
        drawPath(path = path, color = NeonGreen.copy(alpha = 0.8f))
    }
}

object ContextualTipsManager {
    fun resetAllTips(context: Context) {
        val sharedPreferences = context.getSharedPreferences("duocam_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            ContextualTip.values().forEach { tip ->
                putBoolean(tip.key, false)
            }
            apply()
        }
    }
}
