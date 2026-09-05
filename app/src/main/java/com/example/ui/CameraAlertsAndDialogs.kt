package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.DualCameraLayout
import com.example.VideoResolution
import com.example.ui.theme.*

import androidx.compose.foundation.layout.BoxScope

@Composable
fun BoxScope.CameraAlertsAndDialogs(
    recognizedCommandText: String?,
    isLayoutPreviewActive: Boolean,
    isPro: Boolean,
    layoutPreviewRemainingSeconds: Int,
    selectedLayout: DualCameraLayout,
    selectedResolution: VideoResolution,
    show4KWarningBanner: Boolean,
    isStabilizationActive: Boolean,
    showLowStorageWarningBanner: Boolean,
    freeSpaceMB: Long,
    effectiveBatteryLevel: Int,
    showDiscardConfirmDialog: Boolean,
    showPracticeCompletedDialog: Boolean,
    onGoProClick: (String) -> Unit,
    onDiscardCancel: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onPracticeDialogDismiss: () -> Unit
) {
    // 1. Voice Command Recognition Overlay
    AnimatedVisibility(
        visible = recognizedCommandText != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.Center).zIndex(100f)
    ) {
        recognizedCommandText?.let { cmd ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11).copy(alpha = 0.95f)),
                border = BorderStroke(1.5.dp, NeonGreen),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("voice_command_feedback_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic Icon",
                        tint = NeonGreen,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = 1.1f
                                scaleY = 1.1f
                            }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "COMMAND RECOGNIZED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cmd,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }

    // Layout preview countdown banner
    AnimatedVisibility(
        visible = isLayoutPreviewActive && !isPro,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp).zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE62A1B0C))
                .border(2.dp, GoldenHour, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("layout_preview_countdown_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "👑", fontSize = 20.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PREVIEW MODE: ${layoutPreviewRemainingSeconds}s REMAINING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GoldenHour,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Upgrade to Pro to unlock unlimited use of professional layouts!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
                Button(
                    onClick = { onGoProClick("Multi-Cam ${selectedLayout.name.replace("_", " ")} layout") },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenHour),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Upgrade", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                }
            }
        }
    }

    // Resolution preview watermark notice banner
    AnimatedVisibility(
        visible = !isPro && (selectedResolution != VideoResolution.HD),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = if (isLayoutPreviewActive) 160.dp else 90.dp).zIndex(99f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE609090B))
                .border(1.dp, NeonGreen, RoundedCornerShape(12.dp))
                .clickable { onGoProClick("${selectedResolution.name} High Quality Recording") }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("resolution_watermark_free_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Watermark notice",
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Recording in high-res preview mode. Upgrade to Pro to remove watermark!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
            }
        }
    }

    // Sleek top-center sliding warning banner when 4K (UHD) is enabled
    AnimatedVisibility(
        visible = show4KWarningBanner,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp).zIndex(99f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE62A1B0C))
                .border(1.dp, GoldenHour, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("4k_warning_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "⚠️", fontSize = 16.sp)
                Column {
                    Text(
                        text = "4K HEAVY DURATION WARNING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GoldenHour,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "4K uses more battery and may cause the phone to warm up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    // Stabilization crop warning banner
    AnimatedVisibility(
        visible = isStabilizationActive,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
            .padding(top = if (show4KWarningBanner || showLowStorageWarningBanner) 160.dp else 90.dp)
            .zIndex(98f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F0F11).copy(alpha = 0.92f))
                .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("stabilization_crop_warning_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Stabilization crop warning",
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Stabilization enabled — slight crop applied for smoother video",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextWhite
                )
            }
        }
    }

    // Sleek top-center sliding warning banner when device storage is under 500MB
    AnimatedVisibility(
        visible = showLowStorageWarningBanner,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp).zIndex(99f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE62B1717))
                .border(1.dp, GlowRecordRed, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("low_storage_warning_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "⚠️", fontSize = 16.sp)
                Column {
                    Text(
                        text = "LOW DEVICE STORAGE WARNING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GlowRecordRed,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Only ${freeSpaceMB} MB available. Clean up recordings or re-encode to HEVC to save space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    // Sleek top-center sliding warning banner when battery level is <= 15%
    AnimatedVisibility(
        visible = effectiveBatteryLevel <= 15,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = if (showLowStorageWarningBanner) 205.dp else 90.dp).zIndex(99f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE62A1F12))
                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("low_battery_warning_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "⚠️", fontSize = 16.sp)
                Column {
                    Text(
                        text = "LOW BATTERY ALERT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF59E0B),
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Low battery — consider stopping recording. Auto-save safe-stop triggers at 5%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    // Sleek cinematic discard confirmation dialog
    if (showDiscardConfirmDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDiscardCancel
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE61C1C1F))
                    .border(1.dp, Color(0xFF2E2E33), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x26EA4335)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Discard Warning Icon",
                            tint = GlowRecordRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Discard this recording?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This will permanently delete the current recording. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDiscardCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_discard_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E2E33),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Keep",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        
                        Button(
                            onClick = onDiscardConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("confirm_discard_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowRecordRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Discard",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Sleek majestic rehearsal completion dialog
    if (showPracticeCompletedDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onPracticeDialogDismiss
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE61C1C1F))
                    .border(1.dp, Color(0xFF2E2E33), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x2610B981)), // Emerald Green
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Practice Success Icon",
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Practice Complete",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Excellent rehearsal! You have successfully completed your practice session without consuming file storage space. Ready to start recording the real video?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onPracticeDialogDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("practice_complete_dismiss_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Got it, thanks!",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
