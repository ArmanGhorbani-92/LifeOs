package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.PersianCalendar
import com.example.data.local.GoalStatus
import com.example.data.local.ProjectStatus
import com.example.data.local.TaskStatus
import com.example.data.local.toTaskPriority
import com.example.ui.components.AnimatedGlassProgressBar
import com.example.ui.components.ChartBarPoint
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassLineAreaChartCard
import com.example.ui.components.GoalEditorDialog
import com.example.ui.components.HabitEditorDialog
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.TaskEditorDialog
import com.example.ui.navigation.LifeOsDestination
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsCalculators
import com.example.ui.viewmodel.LifeOsViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: LifeOsViewModel,
    onNavigate: (LifeOsDestination) -> Unit
) {
    val colors = LifeOsTheme.colors
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val habitRecords by viewModel.habitRecords.collectAsStateWithLifecycle()
    val events by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val today = remember { PersianCalendar.todayEpochDay() }
    var currentMinute by remember { mutableIntStateOf(PersianCalendar.currentMinuteOfDay()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentMinute = PersianCalendar.currentMinuteOfDay()
            delay(30_000L)
        }
    }

    // Quick Action modal states
    var showAddTask by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddHabit by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }

    // Real DB metrics for Today
    val todayTasks = remember(tasks, today) {
        tasks.filter { LifeOsCalculators.isTaskForDay(it, today) && it.status != TaskStatus.CANCELLED.name }
    }
    val completedTodayCount = remember(todayTasks) {
        todayTasks.count { it.status == TaskStatus.COMPLETED.name }
    }
    val remainingTodayCount = (todayTasks.size - completedTodayCount).coerceAtLeast(0)
    val overdueTasks = remember(tasks, today) {
        tasks.filter { LifeOsCalculators.isTaskOverdue(it, today) }
    }
    val activeGoals = remember(goals) {
        goals.filter { it.status == GoalStatus.ACTIVE.name }
    }
    val activeProjects = remember(projects) {
        projects.filter { it.status == ProjectStatus.ACTIVE.name }
    }
    val upcomingEvents = remember(events, today) {
        events.filter { it.epochDay >= today }.sortedWith(compareBy({ it.epochDay }, { it.startMinuteOfDay ?: 0 })).take(4)
    }
    val todayTimeline = remember(todayTasks) {
        todayTasks.filter { it.startMinuteOfDay != null }.sortedBy { it.startMinuteOfDay }
    }
    val dailyProgressPercent = if (todayTasks.isEmpty()) 0 else ((completedTodayCount * 100f) / todayTasks.size).toInt()

    // Weekly mini performance chart (last 7 days real completions)
    val weeklyPoints = remember(tasks, today, preferences.usePersianDigits) {
        (6 downTo 0).map { offset ->
            val d = today - offset
            val count = tasks.count { LifeOsCalculators.taskCompletionDay(it) == d }
            val dayShort = PersianCalendar.weekdayShortNames[PersianCalendar.dayOfWeekSaturdayFirst(d)]
            ChartBarPoint(
                label = dayShort,
                value = count.toFloat(),
                detailText = "${PersianCalendar.formatNumber(count, preferences.usePersianDigits)} وظیفه تکمیل‌شده"
            )
        }
    }

    val greeting = remember(preferences.userDisplayName) {
        val base = PersianCalendar.currentGreeting()
        if (preferences.userDisplayName.isNotBlank()) "$base، ${preferences.userDisplayName}" else "$base"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Hero Header
        item {
            GlassCard(
                elevated = true,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIFEOS",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.oliveAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${PersianCalendar.weekdayName(today)}، ${PersianCalendar.formatFullDate(today, CalendarSystem.JALALI, preferences.usePersianDigits)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.oliveSoftContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = PersianCalendar.formatTime(currentMinute, preferences.use24HourClock, preferences.usePersianDigits),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { onNavigate(LifeOsDestination.SETTINGS) },
                                modifier = Modifier.testTag("dashboard_settings_button")
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات", tint = colors.textSecondary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Card 1: پیشرفت امروز (Today's Progress)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "پیشرفت امروز",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = PersianCalendar.formatPercent(dailyProgressPercent, preferences.usePersianDigits),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.oliveAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedGlassProgressBar(progress = dailyProgressPercent / 100f, height = 10.dp)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardStatPill(
                        label = "کل امروز",
                        value = PersianCalendar.formatNumber(todayTasks.size, preferences.usePersianDigits),
                        tint = colors.navyStrong,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        label = "انجام‌شده",
                        value = PersianCalendar.formatNumber(completedTodayCount, preferences.usePersianDigits),
                        tint = colors.success,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        label = "باقیمانده",
                        value = PersianCalendar.formatNumber(remainingTodayCount, preferences.usePersianDigits),
                        tint = colors.info,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        label = "عقب‌افتاده",
                        value = PersianCalendar.formatNumber(overdueTasks.size, preferences.usePersianDigits),
                        tint = if (overdueTasks.isNotEmpty()) colors.danger else colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions (5 real working actions)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "دسترسی سریع")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        QuickActionButton(
                            icon = Icons.Rounded.AddTask,
                            label = "افزودن وظیفه",
                            testTag = "quick_action_add_task",
                            onClick = { showAddTask = true }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Rounded.Flag,
                            label = "ایجاد هدف",
                            testTag = "quick_action_add_goal",
                            onClick = { showAddGoal = true }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Rounded.Loop,
                            label = "ثبت عادت",
                            testTag = "quick_action_add_habit",
                            onClick = { showAddHabit = true }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Rounded.EditNote,
                            label = "افزودن یادداشت",
                            testTag = "quick_action_add_note",
                            onClick = { showAddNote = true }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Rounded.EventNote,
                            label = "برنامه‌ریزی جدید",
                            testTag = "quick_action_planner",
                            onClick = { onNavigate(LifeOsDestination.PLANNER) }
                        )
                    }
                }
            }
        }

        // Card 2: وظایف امروز (Today's Tasks)
        item {
            GlassCard(contentPadding = PaddingValues(16.dp)) {
                SectionHeader(
                    title = "وظایف امروز",
                    actionLabel = "مشاهده همه",
                    onActionClick = { onNavigate(LifeOsDestination.TASKS) }
                )
                Spacer(Modifier.height(10.dp))
                if (todayTasks.isEmpty()) {
                    Text(
                        text = "هنوز وظیفه‌ای برای امروز ثبت نشده است. با دکمه «افزودن وظیفه» برنامه‌ریزی خود را شروع کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        todayTasks.take(5).forEach { task ->
                            val isDone = task.status == TaskStatus.COMPLETED.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.glassSurfaceSubtle.copy(alpha = 0.25f))
                                    .clickable { viewModel.toggleTaskCompleted(task) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (isDone) "تکمیل‌شده" else "تکمیل وظیفه",
                                        tint = if (isDone) colors.success else colors.oliveAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDone) colors.textSecondary else colors.textPrimary,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (task.startMinuteOfDay != null) {
                                            Text(
                                                text = PersianCalendar.formatTime(task.startMinuteOfDay, preferences.use24HourClock, preferences.usePersianDigits),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                                PriorityBadge(priority = task.priority.toTaskPriority())
                            }
                        }
                    }
                }
            }
        }

        // Today's Schedule Preview (برنامه زمانی امروز)
        item {
            GlassCard(contentPadding = PaddingValues(16.dp)) {
                SectionHeader(
                    title = "برنامه زمانی امروز",
                    actionLabel = "برنامه‌ریز",
                    onActionClick = { onNavigate(LifeOsDestination.PLANNER) }
                )
                Spacer(Modifier.height(10.dp))
                val conflicts = remember(todayTimeline) { LifeOsCalculators.detectTimelineConflicts(todayTimeline) }
                if (conflicts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warning.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${PersianCalendar.formatNumber(conflicts.size, preferences.usePersianDigits)} تداخل زمانی در برنامه امروز وجود دارد.",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.warning
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (todayTimeline.isEmpty()) {
                    Text(
                        text = "برای امروز بازه ساعتی ثبت نشده است. در بخش برنامه‌ریزی می‌توانید ساعت انجام کارها را مشخص کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        todayTimeline.take(4).forEach { task ->
                            val startStr = PersianCalendar.formatTime(task.startMinuteOfDay!!, preferences.use24HourClock, preferences.usePersianDigits)
                            val endMin = LifeOsCalculators.taskEffectiveEndMinute(task)
                            val endStr = endMin?.let { PersianCalendar.formatTime(it, preferences.use24HourClock, preferences.usePersianDigits) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.oliveSoftContainer)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (endStr != null) "$startStr تا $endStr" else startStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly Mini Chart
        item {
            GlassLineAreaChartCard(
                title = "عملکرد هفتگی",
                subtitle = "تعداد وظایف تکمیل‌شده در ۷ روز اخیر",
                points = weeklyPoints,
                emptyMessage = "پس از تکمیل اولین وظایف، نمودار واقعی عملکرد هفتگی شما نمایش داده می‌شود.",
                usePersianDigits = preferences.usePersianDigits,
                chartHeight = 140.dp
            )
        }

        // Card 3: اهداف فعال (Active Goals)
        item {
            GlassCard(contentPadding = PaddingValues(16.dp)) {
                SectionHeader(
                    title = "اهداف فعال (${PersianCalendar.formatNumber(activeGoals.size, preferences.usePersianDigits)})",
                    actionLabel = "مدیریت اهداف",
                    onActionClick = { onNavigate(LifeOsDestination.GOALS) }
                )
                Spacer(Modifier.height(10.dp))
                if (activeGoals.isEmpty()) {
                    Text(
                        text = "هنوز هدف فعالی ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        activeGoals.take(3).forEach { goal ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = PersianCalendar.formatPercent(goal.progressPercent, preferences.usePersianDigits),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.oliveAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                AnimatedGlassProgressBar(progress = goal.progressPercent / 100f, height = 7.dp)
                            }
                        }
                    }
                }
            }
        }

        // Card 4: عادت‌های امروز (Today's Habits)
        item {
            GlassCard(contentPadding = PaddingValues(16.dp)) {
                SectionHeader(
                    title = "عادت‌های امروز",
                    actionLabel = "همه عادت‌ها",
                    onActionClick = { onNavigate(LifeOsDestination.HABITS) }
                )
                Spacer(Modifier.height(10.dp))
                if (habits.isEmpty()) {
                    Text(
                        text = "هنوز عادتی تعریف نکرده‌اید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        habits.take(4).forEach { habit ->
                            val stats = remember(habit, habitRecords, today) {
                                LifeOsCalculators.computeHabitStats(habit, habitRecords, today)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.glassSurfaceSubtle.copy(alpha = 0.25f))
                                    .clickable { viewModel.toggleHabitForDay(habit.id, today, stats.completedToday) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (stats.completedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = habit.name,
                                        tint = if (stats.completedToday) colors.success else colors.oliveAccent
                                    )
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "استمرار: ${PersianCalendar.formatNumber(stats.currentStreak, preferences.usePersianDigits)} روز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.oliveAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 5: رویدادهای نزدیک (Upcoming Events)
        item {
            GlassCard(contentPadding = PaddingValues(16.dp)) {
                SectionHeader(
                    title = "رویدادهای نزدیک",
                    actionLabel = "تقویم",
                    onActionClick = { onNavigate(LifeOsDestination.CALENDAR) }
                )
                Spacer(Modifier.height(10.dp))
                if (upcomingEvents.isEmpty()) {
                    Text(
                        text = "رویدادی در روزهای آینده ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcomingEvents.forEach { ev ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.oliveSoftContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.oliveAccent, modifier = Modifier.size(18.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ev.title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = PersianCalendar.formatFullDate(ev.epochDay, preferences.calendarSystem, preferences.usePersianDigits),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                if (ev.startMinuteOfDay != null && !ev.isAllDay) {
                                    Text(
                                        text = PersianCalendar.formatTime(ev.startMinuteOfDay, preferences.use24HourClock, preferences.usePersianDigits),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.oliveAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 6: پروژه‌های فعال (Active Projects)
        item {
            GlassCard(contentPadding = PaddingValues(16.dp)) {
                SectionHeader(
                    title = "پروژه‌های فعال",
                    actionLabel = "همه پروژه‌ها",
                    onActionClick = { onNavigate(LifeOsDestination.PROJECTS) }
                )
                Spacer(Modifier.height(10.dp))
                if (activeProjects.isEmpty()) {
                    Text(
                        text = "پروژه فعالی تعریف نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeProjects.take(3).forEach { proj ->
                            val linkedCount = tasks.count { it.projectId == proj.id }
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = colors.oliveAccent, modifier = Modifier.size(18.dp))
                                        Text(proj.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        text = "${PersianCalendar.formatNumber(linkedCount, preferences.usePersianDigits)} وظیفه • ${PersianCalendar.formatPercent(proj.progressPercent, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                AnimatedGlassProgressBar(progress = proj.progressPercent / 100f, height = 6.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTask) {
        TaskEditorDialog(
            categories = categories,
            projects = projects,
            preferences = preferences,
            onSave = {
                viewModel.saveTask(it)
                showAddTask = false
            },
            onDismiss = { showAddTask = false }
        )
    }
    if (showAddGoal) {
        GoalEditorDialog(
            categories = categories,
            preferences = preferences,
            onSave = { goal, note ->
                viewModel.saveGoal(goal, note)
                showAddGoal = false
            },
            onDismiss = { showAddGoal = false }
        )
    }
    if (showAddHabit) {
        HabitEditorDialog(
            categories = categories,
            preferences = preferences,
            onSave = {
                viewModel.saveHabit(it)
                showAddHabit = false
            },
            onDismiss = { showAddHabit = false }
        )
    }
    if (showAddNote) {
        NoteEditorDialog(
            categories = categories,
            onSave = {
                viewModel.saveNote(it)
                showAddNote = false
            },
            onDismiss = { showAddNote = false }
        )
    }
}

@Composable
private fun DashboardStatPill(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassSurfaceSubtle.copy(alpha = 0.35f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = tint,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = LifeOsTheme.colors
    GlassCard(
        modifier = Modifier
            .width(126.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 10.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.oliveSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = colors.oliveAccent, modifier = Modifier.size(21.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
