package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.calendar.PersianCalendar
import com.example.ui.theme.LifeOsTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

data class ChartBarPoint(
    val label: String,
    val value: Float,
    val detailText: String
)

data class ChartSlice(
    val label: String,
    val value: Float,
    val color: Color,
    val valueLabel: String
)

@Composable
fun GlassBarChartCard(
    title: String,
    subtitle: String,
    points: List<ChartBarPoint>,
    emptyMessage: String,
    usePersianDigits: Boolean,
    barColor: Color = LifeOsTheme.colors.oliveAccent,
    chartHeight: Dp = 170.dp,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    var triggered by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { triggered = true }

    val animProgress by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "bar_chart_anim"
    )

    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))

        val hasData = points.any { it.value > 0f }
        if (!hasData) {
            ChartEmptyPlaceholder(emptyMessage)
            return@GlassCard
        }

        if (selectedIndex in points.indices) {
            val selected = points[selectedIndex]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.oliveSoftContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${selected.label}: ${selected.detailText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val maxVal = max(points.maxOf { it.value }, 1f)
        val gridColor = colors.divider

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty()) {
                            val slotWidth = size.width / points.size.toFloat()
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = if (selectedIndex == idx) -1 else idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            // Horizontal guide lines
            for (step in 0..3) {
                val y = h * (step / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val count = points.size
            val slotW = w / count
            val barW = (slotW * 0.46f).coerceIn(10.dp.toPx(), 28.dp.toPx())

            points.forEachIndexed { index, pt ->
                val ratio = (pt.value / maxVal).coerceIn(0f, 1f) * animProgress
                val barH = (h - 12.dp.toPx()) * ratio
                val left = index * slotW + (slotW - barW) / 2f
                val top = h - barH
                val isSelected = index == selectedIndex

                // Track
                drawRoundRect(
                    color = gridColor.copy(alpha = 0.5f),
                    topLeft = Offset(left, 6.dp.toPx()),
                    size = Size(barW, h - 6.dp.toPx()),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )

                if (barH > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(colors.navyStrong, barColor)
                            } else {
                                listOf(barColor, barColor.copy(alpha = 0.68f))
                            },
                            startY = top,
                            endY = h
                        ),
                        topLeft = Offset(left, top),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.oliveAccent else colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "حداکثر محور: ${PersianCalendar.formatNumber(maxVal.toInt(), usePersianDigits)} | برای مشاهده جزئیات روی ستون‌ها لمس کنید",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
fun GlassLineAreaChartCard(
    title: String,
    subtitle: String,
    points: List<ChartBarPoint>,
    emptyMessage: String,
    usePersianDigits: Boolean,
    lineColor: Color = LifeOsTheme.colors.oliveAccent,
    chartHeight: Dp = 155.dp,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    var triggered by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { triggered = true }

    val animProgress by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "area_chart_anim"
    )

    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(10.dp))

        if (points.isEmpty() || points.all { it.value <= 0f }) {
            ChartEmptyPlaceholder(emptyMessage)
            return@GlassCard
        }

        if (selectedIndex in points.indices) {
            val pt = points[selectedIndex]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.oliveSoftContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pt.label}: ${pt.detailText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val maxVal = max(points.maxOf { it.value }, 1f)
        val gridColor = colors.divider

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size > 1) {
                            val step = size.width / (points.size - 1).toFloat()
                            val idx = ((offset.x + step / 2f) / step).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = if (selectedIndex == idx) -1 else idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val topPad = 14.dp.toPx()
            val bottomPad = 10.dp.toPx()
            val usableH = h - topPad - bottomPad

            for (step in 0..3) {
                val y = topPad + usableH * (step / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            val coords = points.mapIndexed { idx, pt ->
                val x = if (points.size == 1) w / 2f else (w * idx / (points.size - 1).toFloat())
                val ratio = (pt.value / maxVal).coerceIn(0f, 1f) * animProgress
                val y = topPad + usableH * (1f - ratio)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 0 until coords.lastIndex) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, h - bottomPad)
                    lineTo(coords.first().x, h - bottomPad)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                        startY = topPad,
                        endY = h - bottomPad
                    )
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                drawCircle(
                    color = if (isSelected) colors.navyStrong else lineColor,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 2.5.dp.toPx() else 1.8.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == selectedIndex) colors.oliveAccent else colors.textSecondary,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "سقف نمودار: ${PersianCalendar.formatNumber(maxVal.toInt(), usePersianDigits)}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
fun GlassDonutChartCard(
    title: String,
    subtitle: String,
    slices: List<ChartSlice>,
    emptyMessage: String,
    usePersianDigits: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    val nonZero = remember(slices) { slices.filter { it.value > 0f } }
    var selectedIdx by remember(nonZero) { mutableIntStateOf(-1) }
    var triggered by remember(nonZero) { mutableStateOf(false) }
    LaunchedEffect(nonZero) { triggered = true }

    val animProgress by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "donut_anim"
    )

    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(14.dp))

        if (nonZero.isEmpty()) {
            ChartEmptyPlaceholder(emptyMessage)
            return@GlassCard
        }

        val total = nonZero.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(136.dp)
                        .pointerInput(nonZero) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val dist = hypot(dx, dy)
                                if (dist > size.width * 0.22f) {
                                    var angle = ((atan2(dy, dx) * 180.0 / PI) + 90.0 + 360.0) % 360.0
                                    var acc = 0.0
                                    for (i in nonZero.indices) {
                                        val sweep = (nonZero[i].value / total) * 360.0
                                        if (angle >= acc && angle <= acc + sweep) {
                                            selectedIdx = if (selectedIdx == i) -1 else i
                                            break
                                        }
                                        acc += sweep
                                    }
                                }
                            }
                        }
                ) {
                    val strokeW = 22.dp.toPx()
                    val diameter = size.minDimension - strokeW
                    val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                    var startAngle = -90f
                    nonZero.forEachIndexed { idx, slice ->
                        val sweep = (slice.value / total) * 360f * animProgress
                        val isSel = idx == selectedIdx
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = if (isSel) strokeW + 4.dp.toPx() else strokeW,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweep
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedIdx in nonZero.indices) {
                        val sel = nonZero[selectedIdx]
                        val pct = ((sel.value / total) * 100f).toInt()
                        Text(
                            text = PersianCalendar.formatPercent(pct, usePersianDigits),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = sel.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = PersianCalendar.formatNumber(total.toInt(), usePersianDigits),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مجموع",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nonZero.forEachIndexed { idx, slice ->
                    val pct = ((slice.value / total) * 100f).toInt()
                    val isSel = idx == selectedIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colors.oliveSoftContainer else Color.Transparent)
                            .clickable { selectedIdx = if (selectedIdx == idx) -1 else idx }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${slice.valueLabel} (${PersianCalendar.formatPercent(pct, usePersianDigits)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassHorizontalProgressChartCard(
    title: String,
    subtitle: String,
    items: List<Triple<String, Float, String>>,
    emptyMessage: String,
    barColor: Color = LifeOsTheme.colors.oliveAccent,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            ChartEmptyPlaceholder(emptyMessage)
            return@GlassCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { (label, progress, valueText) ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = valueText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.oliveAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    AnimatedGlassProgressBar(progress = progress, fillColor = barColor, height = 8.dp)
                }
            }
        }
    }
}

@Composable
private fun ChartEmptyPlaceholder(message: String) {
    val colors = LifeOsTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassSurfaceSubtle.copy(alpha = 0.35f))
            .padding(vertical = 22.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
    }
}
