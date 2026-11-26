package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.PersianCalendar
import com.example.data.local.TaskEntity
import com.example.data.local.TaskStatus
import com.example.data.local.toTaskPriority
import com.example.ui.components.AnimatedGlassProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.PersianDatePickerDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.TaskEditorDialog
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsCalculators
import com.example.ui.viewmodel.LifeOsViewModel

private enum class PlannerMode(val label: String) {
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه")
}

@Composable
fun PlannerScreen(viewModel: LifeOsViewModel) {
    val colors = LifeOsTheme.colors
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val today = remember { PersianCalendar.todayEpochDay() }
    var selectedDay by remember { mutableLongStateOf(today) }
    var mode by remember { mutableStateOf(PlannerMode.DAILY) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showCreateTask by remember { mutableStateOf(false) }
    var defaultStartMinute by remember { mutableStateOf<Int?>(540) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val tasksForSelectedDay = remember(tasks, selectedDay) {
        tasks.filter { LifeOsCalculators.isTaskForDay(it, selectedDay) && it.status != TaskStatus.CANCELLED.name }
    }
    val scheduledTimelineTasks = remember(tasksForSelectedDay) {
        tasksForSelectedDay.filter { it.startMinuteOfDay != null }.sortedBy { it.startMinuteOfDay }
    }
    val unscheduledDayTasks = remember(tasksForSelectedDay) {
        tasksForSelectedDay.filter { it.startMinuteOfDay == null }
    }
    val completedDayTasks = remember(tasksForSelectedDay) {
        tasksForSelectedDay.filter { it.status == TaskStatus.COMPLETED.name }
    }
    val conflicts = remember(scheduledTimelineTasks) {
        LifeOsCalculators.detectTimelineConflicts(scheduledTimelineTasks)
    }
    val conflictingIds = remember(conflicts) {
        conflicts.flatMap { listOf(it.firstTask.id, it.secondTask.id) }.toSet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "برنامه‌ریزی شخصی",
                subtitle = "${PersianCalendar.weekdayName(selectedDay)}، ${PersianCalendar.formatFullDate(selectedDay, preferences.calendarSystem, preferences.usePersianDigits)}",
                trailingAction = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedDay = today }) {
                            Text("امروز", color = colors.oliveAccent)
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "انتخاب تاریخ", tint = colors.oliveAccent)
                        }
                    }
                }
            )

            // Mode switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlannerMode.entries.forEach { m ->
                    GlassFilterChip(
                        label = m.label,
                        selected = mode == m,
                        onClick = { mode = m },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (mode) {
                PlannerMode.DAILY -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Day Navigator + Summary
                        item {
                            GlassCard(contentPadding = PaddingValues(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedDay -= 1L }) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "روز قبل", tint = colors.textPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = PersianCalendar.weekdayName(selectedDay),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = PersianCalendar.formatFullDate(selectedDay, preferences.calendarSystem, preferences.usePersianDigits),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    IconButton(onClick = { selectedDay += 1L }) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "روز بعد", tint = colors.textPrimary)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                val pct = if (tasksForSelectedDay.isEmpty()) 0 else (completedDayTasks.size * 100 / tasksForSelectedDay.size)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "انجام‌شده: ${PersianCalendar.formatNumber(completedDayTasks.size, preferences.usePersianDigits)} از ${PersianCalendar.formatNumber(tasksForSelectedDay.size, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = PersianCalendar.formatPercent(pct, preferences.usePersianDigits),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.oliveAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                AnimatedGlassProgressBar(progress = pct / 100f, height = 7.dp)
                            }
                        }

                        // Manual Conflict Alert (never auto-moves tasks!)
                        if (conflicts.isNotEmpty()) {
                            item {
                                GlassCard(
                                    accentTint = colors.warning.copy(alpha = 0.16f),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning)
                                        Text(
                                            text = "هشدار تداخل زمانی (${PersianCalendar.formatNumber(conflicts.size, preferences.usePersianDigits)} مورد)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.warning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    conflicts.forEach { c ->
                                        Text(
                                            text = "• «${c.firstTask.title}» و «${c.secondTask.title}» هم‌پوشانی زمانی دارند. در صورت تمایل ساعت یکی را به‌صورت دستی ویرایش کنید.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Professional Timeline Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("خط زمانی روزانه", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                                Text(
                                    text = "${PersianCalendar.formatNumber(scheduledTimelineTasks.size, preferences.usePersianDigits)} برنامه زمان‌بندی‌شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (scheduledTimelineTasks.isEmpty()) {
                            item {
                                EmptyStateGlassCard(
                                    icon = Icons.Rounded.Schedule,
                                    title = "برنامه ساعتی برای این روز ثبت نشده",
                                    description = "برای ساخت جدول زمانی دستی، یک وظیفه با ساعت شروع و پایان اضافه کنید.",
                                    actionLabel = "افزودن به برنامه روز",
                                    onAction = {
                                        defaultStartMinute = 540
                                        showCreateTask = true
                                    }
                                )
                            }
                        } else {
                            items(scheduledTimelineTasks, key = { "tl_${it.id}" }) { task ->
                                val hasConflict = task.id in conflictingIds
                                val isDone = task.status == TaskStatus.COMPLETED.name
                                val startM = task.startMinuteOfDay ?: 0
                                val endM = LifeOsCalculators.taskEffectiveEndMinute(task) ?: (startM + 30)

                                GlassCard(
                                    accentTint = if (hasConflict) colors.warning.copy(alpha = 0.12f) else null,
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp),
                                    onClick = { editingTask = task }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(68.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (hasConflict) colors.warning.copy(alpha = 0.22f) else colors.oliveSoftContainer)
                                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = PersianCalendar.formatTime(startM, preferences.use24HourClock, preferences.usePersianDigits),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "تا",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                            Text(
                                                text = PersianCalendar.formatTime(endM, preferences.use24HourClock, preferences.usePersianDigits),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textPrimary
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (isDone) colors.textSecondary else colors.textPrimary,
                                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                PriorityBadge(priority = task.priority.toTaskPriority())
                                            }
                                            if (hasConflict) {
                                                Text(
                                                    text = "تداخل زمانی دارد",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.warning,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "مدت: ${PersianCalendar.formatDuration((endM - startM).coerceAtLeast(1), preferences.usePersianDigits)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                        }

                                        Column {
                                            IconButton(onClick = { viewModel.toggleTaskCompleted(task) }) {
                                                Icon(
                                                    imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                    contentDescription = "تکمیل",
                                                    tint = if (isDone) colors.success else colors.oliveAccent
                                                )
                                            }
                                            IconButton(onClick = { deletingTask = task }) {
                                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(19.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Unscheduled tasks on this day
                        if (unscheduledDayTasks.isNotEmpty()) {
                            item {
                                Text("وظایف بدون ساعت مشخص این روز", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            }
                            items(unscheduledDayTasks, key = { "un_${it.id}" }) { task ->
                                val isDone = task.status == TaskStatus.COMPLETED.name
                                GlassCard(
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    onClick = { editingTask = task }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            IconButton(onClick = { viewModel.toggleTaskCompleted(task) }) {
                                                Icon(
                                                    imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                    contentDescription = "تغییر وضعیت",
                                                    tint = if (isDone) colors.success else colors.oliveAccent
                                                )
                                            }
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isDone) colors.textSecondary else colors.textPrimary,
                                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { editingTask = task }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "تعیین ساعت", tint = colors.oliveAccent, modifier = Modifier.size(19.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PlannerMode.WEEKLY -> {
                    val weekStart = remember(selectedDay, preferences.weekStartDay) {
                        PersianCalendar.startOfWeekEpochDay(selectedDay, preferences.weekStartDay)
                    }
                    val weekDays = remember(weekStart) { List(7) { weekStart + it } }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            GlassCard(contentPadding = PaddingValues(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedDay -= 7L }) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "هفته قبل", tint = colors.textPrimary)
                                    }
                                    Text(
                                        text = "${PersianCalendar.formatShortDate(weekDays.first(), preferences.calendarSystem, preferences.usePersianDigits)} تا ${PersianCalendar.formatShortDate(weekDays.last(), preferences.calendarSystem, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    IconButton(onClick = { selectedDay += 7L }) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "هفته بعد", tint = colors.textPrimary)
                                    }
                                }
                            }
                        }

                        items(weekDays, key = { it }) { day ->
                            val dayTasks = tasks.filter { LifeOsCalculators.isTaskForDay(it, day) && it.status != TaskStatus.CANCELLED.name }
                            val doneCount = dayTasks.count { it.status == TaskStatus.COMPLETED.name }
                            val scheduledCount = dayTasks.count { it.startMinuteOfDay != null }
                            val progress = if (dayTasks.isEmpty()) 0f else doneCount.toFloat() / dayTasks.size.toFloat()
                            val isToday = day == today

                            GlassCard(
                                elevated = isToday,
                                contentPadding = PaddingValues(14.dp),
                                onClick = {
                                    selectedDay = day
                                    mode = PlannerMode.DAILY
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = PersianCalendar.weekdayName(day),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (isToday) colors.oliveAccent else colors.textPrimary
                                            )
                                            if (isToday) {
                                                Text("(امروز)", style = MaterialTheme.typography.labelSmall, color = colors.oliveAccent)
                                            }
                                        }
                                        Text(
                                            text = PersianCalendar.formatFullDate(day, preferences.calendarSystem, preferences.usePersianDigits),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = PersianCalendar.formatPercent((progress * 100).toInt(), preferences.usePersianDigits),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.oliveAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                AnimatedGlassProgressBar(progress = progress, height = 7.dp)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "کل وظایف: ${PersianCalendar.formatNumber(dayTasks.size, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = "زمان‌بندی‌شده: ${PersianCalendar.formatNumber(scheduledCount, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = "انجام‌شده: ${PersianCalendar.formatNumber(doneCount, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.success
                                    )
                                }
                            }
                        }
                    }
                }

                PlannerMode.MONTHLY -> {
                    val (year, month) = remember(selectedDay, preferences.calendarSystem) {
                        when (preferences.calendarSystem) {
                            CalendarSystem.JALALI -> {
                                val j = PersianCalendar.epochDayToJalali(selectedDay)
                                j.year to j.month
                            }
                            CalendarSystem.GREGORIAN -> {
                                val g = PersianCalendar.epochDayToGregorian(selectedDay)
                                g.year to g.month
                            }
                        }
                    }
                    val monthLen = when (preferences.calendarSystem) {
                        CalendarSystem.JALALI -> PersianCalendar.jalaliMonthLength(year, month)
                        CalendarSystem.GREGORIAN -> PersianCalendar.gregorianMonthLength(year, month)
                    }
                    val firstDayEpoch = when (preferences.calendarSystem) {
                        CalendarSystem.JALALI -> PersianCalendar.jalaliToEpochDay(year, month, 1)
                        CalendarSystem.GREGORIAN -> PersianCalendar.gregorianToEpochDay(year, month, 1)
                    }
                    val weekdayOrder = PersianCalendar.weekdayOrder(preferences.weekStartDay)
                    val leadingBlanks = weekdayOrder.indexOf(PersianCalendar.dayOfWeekSaturdayFirst(firstDayEpoch)).coerceAtLeast(0)
                    val monthCells = buildList<Long?> {
                        repeat(leadingBlanks) { add(null) }
                        for (d in 0 until monthLen) add(firstDayEpoch + d)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            GlassCard(contentPadding = PaddingValues(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedDay = firstDayEpoch - 1L
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "ماه قبل", tint = colors.textPrimary)
                                    }
                                    Text(
                                        text = PersianCalendar.formatMonthYear(year, month, preferences.calendarSystem, preferences.usePersianDigits),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedDay = firstDayEpoch + monthLen
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "ماه بعد", tint = colors.textPrimary)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
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
                                Spacer(Modifier.height(8.dp))

                                val rows = monthCells.chunked(7)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rows.forEach { rowCells ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (col in 0 until 7) {
                                                val dayEpoch = rowCells.getOrNull(col)
                                                if (dayEpoch == null) {
                                                    Box(Modifier.weight(1f).aspectRatio(0.95f))
                                                } else {
                                                    val dayNum = when (preferences.calendarSystem) {
                                                        CalendarSystem.JALALI -> PersianCalendar.epochDayToJalali(dayEpoch).day
                                                        CalendarSystem.GREGORIAN -> PersianCalendar.epochDayToGregorian(dayEpoch).day
                                                    }
                                                    val count = tasks.count { LifeOsCalculators.isTaskForDay(it, dayEpoch) && it.status != TaskStatus.CANCELLED.name }
                                                    val isSel = dayEpoch == selectedDay
                                                    val isToday = dayEpoch == today
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(0.95f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                when {
                                                                    isSel -> colors.oliveAccent
                                                                    isToday -> colors.oliveSoftContainer
                                                                    else -> colors.glassSurfaceSubtle.copy(alpha = 0.2f)
                                                                }
                                                            )
                                                            .border(
                                                                width = if (isToday && !isSel) 1.dp else 0.dp,
                                                                color = if (isToday && !isSel) colors.oliveAccent else Color.Transparent,
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable {
                                                                selectedDay = dayEpoch
                                                                mode = PlannerMode.DAILY
                                                            },
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = PersianCalendar.formatNumber(dayNum, preferences.usePersianDigits),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = if (isSel) Color.White else colors.textPrimary,
                                                            fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                        if (count > 0) {
                                                            Text(
                                                                text = PersianCalendar.formatNumber(count, preferences.usePersianDigits),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isSel) Color.White.copy(alpha = 0.9f) else colors.oliveAccent,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                defaultStartMinute = 540
                showCreateTask = true
            },
            containerColor = colors.oliveAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("planner_add_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "افزودن به برنامه‌ریزی")
        }
    }

    if (showDatePicker) {
        PersianDatePickerDialog(
            initialEpochDay = selectedDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "انتخاب روز برنامه‌ریزی",
            onDateSelected = {
                selectedDay = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showCreateTask) {
        TaskEditorDialog(
            defaultEpochDay = selectedDay,
            defaultStartMinute = defaultStartMinute,
            categories = categories,
            projects = projects,
            preferences = preferences,
            onSave = {
                viewModel.saveTask(it, "در برنامه روز ثبت شد.")
                showCreateTask = false
            },
            onDismiss = { showCreateTask = false }
        )
    }

    editingTask?.let { taskToEdit ->
        TaskEditorDialog(
            initialTask = taskToEdit,
            categories = categories,
            projects = projects,
            preferences = preferences,
            onSave = {
                viewModel.saveTask(it, "برنامه به‌روزرسانی شد.")
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }

    deletingTask?.let { taskToDelete ->
        ConfirmDeleteDialog(
            title = "حذف از برنامه",
            message = "آیا از حذف «${taskToDelete.title}» مطمئن هستید؟",
            onConfirm = {
                viewModel.deleteTask(taskToDelete.id)
                deletingTask = null
            },
            onDismiss = { deletingTask = null }
        )
    }
}
