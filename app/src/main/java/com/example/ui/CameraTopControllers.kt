package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DualCameraLayout

import com.example.ui.theme.BorderGray
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.AccentBlue

@Composable
fun CameraTopControllers(
    selectedLayout: DualCameraLayout,
    onLayoutSelected: (DualCameraLayout) -> Unit,
    showLayoutPickerMenu: Boolean,
    onShowLayoutPickerMenuChange: (Boolean) -> Unit,
    selectedAspectRatio: com.example.RecordingAspectRatio,
    onAspectRatioSelected: (com.example.RecordingAspectRatio) -> Unit,
    showAspectRatioMenu: Boolean,
    onShowAspectRatioMenuChange: (Boolean) -> Unit,
    isGridEnabled: Boolean,
    isLevelEnabled: Boolean,
    isCrosshairEnabled: Boolean,
    onGuidesCycle: () -> Unit,
    onGridToggle: () -> Unit,
    onLevelToggle: () -> Unit,
    onCrosshairToggle: () -> Unit,
    showGuidesMenu: Boolean,
    onShowGuidesMenuChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isPro: Boolean = false // needed for the padlock icon if any, wait I handled the click outside
) {
    // Aspect Ratio and Layout Picker row
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Column 1: Layout Picker
        Box(
            modifier = Modifier.testTag("layout_picker_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Toggle Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE609090B))
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
                        .clickable {
                            onShowLayoutPickerMenuChange(!showLayoutPickerMenu)
                            if (!showLayoutPickerMenu) {
                                onShowAspectRatioMenuChange(false)
                                onShowGuidesMenuChange(false)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Format Layout",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (selectedLayout) {
                            DualCameraLayout.PIP -> "PiP"
                            DualCameraLayout.SPLIT_HORIZONTAL -> "SPLIT H"
                            DualCameraLayout.SPLIT_VERTICAL -> "SPLIT V"
                            DualCameraLayout.SPLIT_DIAGONAL -> "DIAGONAL"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showLayoutPickerMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Animated Layout Options Drawer Menu
                AnimatedVisibility(
                    visible = showLayoutPickerMenu,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -20 }) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -20 }) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xF2121214))
                            .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        DualCameraLayout.values().forEach { layoutOption ->
                            val isSelected = selectedLayout == layoutOption
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        onLayoutSelected(layoutOption)
                                    }
                                    .testTag("layout_option_${layoutOption.name.lowercase()}")
                                    .padding(4.dp)
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    when (layoutOption) {
                                        DualCameraLayout.PIP -> com.example.PipLayoutIcon(isSelected)
                                        DualCameraLayout.SPLIT_HORIZONTAL -> com.example.SplitHorizontalIcon(isSelected)
                                        DualCameraLayout.SPLIT_VERTICAL -> com.example.SplitVerticalIcon(isSelected)
                                        DualCameraLayout.SPLIT_DIAGONAL -> com.example.SplitDiagonalIcon(isSelected)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = when (layoutOption) {
                                        DualCameraLayout.PIP -> "PiP"
                                        DualCameraLayout.SPLIT_HORIZONTAL -> "Split H"
                                        DualCameraLayout.SPLIT_VERTICAL -> "Split V"
                                        DualCameraLayout.SPLIT_DIAGONAL -> "Diagonal"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSelected) NeonGreen else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Column 2: Aspect Ratio Picker
        Box(
            modifier = Modifier.testTag("aspect_ratio_picker_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Toggle Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE609090B))
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
                        .clickable {
                            onShowAspectRatioMenuChange(!showAspectRatioMenu)
                            if (!showAspectRatioMenu) {
                                onShowLayoutPickerMenuChange(false)
                                onShowGuidesMenuChange(false)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = "Aspect Ratio",
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedAspectRatio.valueString,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAspectRatioMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Animated Aspect Ratio Drawer Options Menu
                AnimatedVisibility(
                    visible = showAspectRatioMenu,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -20 }) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -20 }) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .width(230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xF2121214))
                            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        com.example.RecordingAspectRatio.values().forEach { ratioOption ->
                            val isSelected = selectedAspectRatio == ratioOption
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) com.example.ui.theme.GlowRecordRed.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        onAspectRatioSelected(ratioOption)
                                    }
                                    .testTag("ratio_option_${ratioOption.name.lowercase().replace("ratio_", "")}")
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini visual aspect icon box
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) com.example.ui.theme.GlowRecordRed else Color(0xFF27272A))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val miniRatio = ratioOption.ratio
                                    val (miniW, miniH) = if (miniRatio > 1.0f) {
                                        18.dp to (18.dp / miniRatio)
                                    } else {
                                        (18.dp * miniRatio) to 18.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(width = miniW, height = miniH)
                                            .border(1.dp, if (isSelected) com.example.ui.theme.ObsidianBlack else TextWhite, RoundedCornerShape(1.dp))
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ratioOption.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) com.example.ui.theme.GlowRecordRed else TextWhite
                                    )
                                    Text(
                                        text = ratioOption.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp
                                        ),
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Column 3: Guides Picker
        Box(
            modifier = Modifier.testTag("guides_picker_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Toggle Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE609090B))
                        .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
                        .clickable {
                            onShowGuidesMenuChange(!showGuidesMenu)
                            if (!showGuidesMenu) {
                                onShowLayoutPickerMenuChange(false)
                                onShowAspectRatioMenuChange(false)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Guides",
                        tint = if (isGridEnabled || isLevelEnabled || isCrosshairEnabled) Color(0xFFA855F7) else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Guides",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showGuidesMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Animated Guides Drawer Options Menu
                AnimatedVisibility(
                    visible = showGuidesMenu,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -20 }) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -20 }) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xF2121214))
                            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        // Quick Preset Cycle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E22))
                                .clickable {
                                    onGuidesCycle()
                                }
                                .padding(8.dp)
                                .testTag("cycle_guides_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Cycle Guides",
                                tint = com.example.ui.theme.GlowRecordRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cycle Quick Preset",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextWhite
                                )
                            )
                        }
                        
                        Divider(color = BorderGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        
                        com.example.GuideToggleRow(
                            title = "Rule of Thirds Grid",
                            description = "Align balance visually",
                            isEnabled = isGridEnabled,
                            onToggle = onGridToggle,
                            testTagSuffix = "grid"
                        )
                        
                        com.example.GuideToggleRow(
                            title = "Horizon Spirit Level",
                            description = "Keep shots straight",
                            isEnabled = isLevelEnabled,
                            onToggle = onLevelToggle,
                            testTagSuffix = "level"
                        )
                        
                        com.example.GuideToggleRow(
                            title = "Center Crosshair",
                            description = "Subtle viewport target",
                            isEnabled = isCrosshairEnabled,
                            onToggle = onCrosshairToggle,
                            testTagSuffix = "crosshair"
                        )
                    }
                }
            }
        }
    }
}
