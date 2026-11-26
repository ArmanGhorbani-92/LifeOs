package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.PersianCalendar
import com.example.core.calendar.WeekStartDay
import com.example.data.local.GoalStatus
import com.example.data.local.ProjectStatus
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.ui.theme.LifeOsTheme

@Composable
fun LifeOsBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LifeOsTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .drawBehind {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.backgroundOrbPrimary.copy(alpha = if (colors.isDark) 0.55f else 0.90f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.12f),
                        radius = w * 0.78f
                    ),
                    radius = w * 0.78f,
                    center = Offset(w * 0.85f, h * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.backgroundOrbSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.75f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.48f),
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = Offset(w * 0.12f, h * 0.48f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.oliveAccent.copy(alpha = if (colors.isDark) 0.12f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.75f, h * 0.86f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.75f, h * 0.86f)
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    accentTint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LifeOsTheme.colors
    val baseFill = accentTint ?: if (elevated) colors.glassSurfaceElevated else colors.glassSurface
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            baseFill.copy(alpha = (baseFill.alpha + 0.06f).coerceAtMost(0.96f)),
            baseFill
        )
    )
    val shadowColor = if (colors.isDark) Color.Black.copy(alpha = 0.28f) else colors.navyStrong.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 10.dp else 4.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor,
                clip = false
            )
            .clip(shape)
            .background(gradientBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(colors.glassHighlightTop, colors.glassBorder)
                ),
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val colors = LifeOsTheme.colors
    val bgColor = if (selected) colors.oliveAccent else colors.glassSurface
    val contentColor = if (selected) Color.White else colors.textPrimary
    val borderColor = if (selected) colors.oliveAccent else colors.glassBorder

    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LifeOsTopHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationAction: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            navigationAction?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailingAction != null) {
            Spacer(Modifier.width(8.dp))
            trailingAction()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.oliveAccent
                )
            }
        }
    }
}

@Composable
fun AnimatedGlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 9.dp,
    fillColor: Color = LifeOsTheme.colors.oliveAccent,
    trackColor: Color = LifeOsTheme.colors.glassBorder.copy(alpha = 0.22f)
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "glass_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(fillColor.copy(alpha = 0.85f), fillColor)
                    )
                )
        )
    }
}

@Composable
fun EmptyStateGlassCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(colors.oliveSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.oliveAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.oliveAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority, modifier: Modifier = Modifier) {
    val colors = LifeOsTheme.colors
    val (bg, fg) = when (priority) {
        TaskPriority.LOW -> colors.oliveSoftContainer to colors.oliveAccent
        TaskPriority.MEDIUM -> colors.info.copy(alpha = 0.14f) to colors.info
        TaskPriority.HIGH -> colors.warning.copy(alpha = 0.16f) to colors.warning
        TaskPriority.URGENT -> colors.danger.copy(alpha = 0.16f) to colors.danger
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = priority.persianLabel,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusBadge(label: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun taskStatusColor(status: TaskStatus): Color {
    val colors = LifeOsTheme.colors
    return when (status) {
        TaskStatus.NOT_STARTED -> colors.textSecondary
        TaskStatus.IN_PROGRESS -> colors.info
        TaskStatus.COMPLETED -> colors.success
        TaskStatus.CANCELLED -> colors.danger
    }
}

@Composable
fun goalStatusColor(status: GoalStatus): Color {
    val colors = LifeOsTheme.colors
    return when (status) {
        GoalStatus.ACTIVE -> colors.oliveAccent
        GoalStatus.COMPLETED -> colors.success
        GoalStatus.PAUSED -> colors.warning
        GoalStatus.CANCELLED -> colors.danger
    }
}

@Composable
fun projectStatusColor(status: ProjectStatus): Color {
    val colors = LifeOsTheme.colors
    return when (status) {
        ProjectStatus.ACTIVE -> colors.oliveAccent
        ProjectStatus.ON_HOLD -> colors.warning
        ProjectStatus.COMPLETED -> colors.success
        ProjectStatus.CANCELLED -> colors.danger
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.glassSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = colors.danger
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.danger,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("حذف")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("انصراف", color = colors.textPrimary)
            }
        }
    )
}

@Composable
fun PersianDatePickerDialog(
    initialEpochDay: Long,
    calendarSystem: CalendarSystem,
    weekStartDay: WeekStartDay,
    usePersianDigits: Boolean,
    title: String = "انتخاب تاریخ",
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val today = remember { PersianCalendar.todayEpochDay() }

    val initialYearMonth = remember(initialEpochDay, calendarSystem) {
        when (calendarSystem) {
            CalendarSystem.JALALI -> {
                val j = PersianCalendar.epochDayToJalali(initialEpochDay)
                j.year to j.month
            }
            CalendarSystem.GREGORIAN -> {
                val g = PersianCalendar.epochDayToGregorian(initialEpochDay)
                g.year to g.month
            }
        }
    }

    var displayYear by remember { mutableIntStateOf(initialYearMonth.first) }
    var displayMonth by remember { mutableIntStateOf(initialYearMonth.second) }

    val monthLength = remember(displayYear, displayMonth, calendarSystem) {
        when (calendarSystem) {
            CalendarSystem.JALALI -> PersianCalendar.jalaliMonthLength(displayYear, displayMonth)
            CalendarSystem.GREGORIAN -> PersianCalendar.gregorianMonthLength(displayYear, displayMonth)
        }
    }

    val firstDayEpoch = remember(displayYear, displayMonth, calendarSystem) {
        when (calendarSystem) {
            CalendarSystem.JALALI -> PersianCalendar.jalaliToEpochDay(displayYear, displayMonth, 1)
            CalendarSystem.GREGORIAN -> PersianCalendar.gregorianToEpochDay(displayYear, displayMonth, 1)
        }
    }

    val weekdayOrder = remember(weekStartDay) { PersianCalendar.weekdayOrder(weekStartDay) }
    val firstDow = PersianCalendar.dayOfWeekSaturdayFirst(firstDayEpoch)
    val leadingBlanks = weekdayOrder.indexOf(firstDow).coerceAtLeast(0)

    val cells = remember(leadingBlanks, monthLength, firstDayEpoch) {
        buildList<Long?> {
            repeat(leadingBlanks) { add(null) }
            for (d in 0 until monthLength) add(firstDayEpoch + d)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            elevated = true,
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.oliveAccent)
                    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (displayMonth == 1) {
                            displayMonth = 12
                            displayYear -= 1
                        } else {
                            displayMonth -= 1
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "ماه قبل", tint = colors.textPrimary)
                }
                Text(
                    text = PersianCalendar.formatMonthYear(displayYear, displayMonth, calendarSystem, usePersianDigits),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                IconButton(
                    onClick = {
                        if (displayMonth == 12) {
                            displayMonth = 1
                            displayYear += 1
                        } else {
                            displayMonth += 1
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "ماه بعد", tint = colors.textPrimary)
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayOrder.forEach { dow ->
                    Text(
                        text = PersianCalendar.weekdayShortNames[dow],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cells) { cellEpoch ->
                    if (cellEpoch == null) {
                        Box(Modifier.aspectRatio(1f))
                    } else {
                        val dayNum = when (calendarSystem) {
                            CalendarSystem.JALALI -> PersianCalendar.epochDayToJalali(cellEpoch).day
                            CalendarSystem.GREGORIAN -> PersianCalendar.epochDayToGregorian(cellEpoch).day
                        }
                        val isSelected = cellEpoch == initialEpochDay
                        val isToday = cellEpoch == today
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> colors.oliveAccent
                                        isToday -> colors.oliveSoftContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                    color = if (isToday && !isSelected) colors.oliveAccent else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onDateSelected(cellEpoch) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = PersianCalendar.formatNumber(dayNum, usePersianDigits),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { onDateSelected(today) }
                ) {
                    Text("برو به امروز", color = colors.oliveAccent)
                }
                TextButton(onClick = onDismiss) {
                    Text("بستن", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun PersianTimePickerDialog(
    initialMinuteOfDay: Int,
    usePersianDigits: Boolean,
    title: String = "انتخاب ساعت",
    onTimeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    var selectedHour by remember { mutableIntStateOf((initialMinuteOfDay / 60).coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf((initialMinuteOfDay % 60).coerceIn(0, 59)) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            elevated = true,
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.oliveAccent)
                    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.oliveSoftContainer.copy(alpha = 0.65f))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PersianCalendar.formatTime(selectedHour * 60 + selectedMinute, true, usePersianDigits),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minute controls
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("دقیقه", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedMinute = (selectedMinute - 5 + 60) % 60 },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("−۵")
                        }
                        OutlinedButton(
                            onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+۵")
                        }
                    }
                }

                // Hour controls
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("ساعت", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedHour = (selectedHour - 1 + 24) % 24 },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("−۱")
                        }
                        OutlinedButton(
                            onClick = { selectedHour = (selectedHour + 1) % 24 },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+۱")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("انصراف", color = colors.textSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onTimeSelected(selectedHour * 60 + selectedMinute) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.oliveAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("تأیید ساعت")
                }
            }
        }
    }
}
