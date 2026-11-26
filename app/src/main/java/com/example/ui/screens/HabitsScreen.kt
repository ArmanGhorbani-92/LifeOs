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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.PersianCalendar
import com.example.data.local.HabitEntity
import com.example.data.local.toHabitFrequency
import com.example.ui.components.AnimatedGlassProgressBar
import com.example.ui.components.ChartBarPoint
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.GlassBarChartCard
import com.example.ui.components.GlassCard
import com.example.ui.components.HabitEditorDialog
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsCalculators
import com.example.ui.viewmodel.LifeOsViewModel

@Composable
fun HabitsScreen(
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val records by viewModel.habitRecords.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val today = remember { PersianCalendar.todayEpochDay() }
    var showCreate by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var deletingHabit by remember { mutableStateOf<HabitEntity?>(null) }

    val completedTodayTotal = remember(habits, records, today) {
        habits.count { h -> records.any { it.habitId == h.id && it.epochDay == today && it.isCompleted } }
    }

    // Weekly habit check-in chart (last 7 days real records)
    val weeklyHabitPoints = remember(records, today, preferences.usePersianDigits) {
        (6 downTo 0).map { offset ->
            val d = today - offset
            val cnt = records.count { it.epochDay == d && it.isCompleted }
            ChartBarPoint(
                label = PersianCalendar.weekdayShortNames[PersianCalendar.dayOfWeekSaturdayFirst(d)],
                value = cnt.toFloat(),
                detailText = "${PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)} عادت انجام‌شده"
            )
        }
    }

    // Monthly habit completion grouped into 4 weeks (last 28 days)
    val monthlyHabitPoints = remember(records, today, preferences.usePersianDigits) {
        (3 downTo 0).map { weekIdx ->
            val endDay = today - (weekIdx * 7L)
            val startDay = endDay - 6L
            val cnt = records.count { it.epochDay in startDay..endDay && it.isCompleted }
            ChartBarPoint(
                label = "هفته ${PersianCalendar.formatNumber(4 - weekIdx, preferences.usePersianDigits)}",
                value = cnt.toFloat(),
                detailText = "${PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)} ثبت عادت"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "مدیریت عادت‌ها",
                subtitle = "امروز: ${PersianCalendar.formatNumber(completedTodayTotal, preferences.usePersianDigits)} از ${PersianCalendar.formatNumber(habits.size, preferences.usePersianDigits)} عادت انجام‌شده",
                navigationAction = onBack?.let {
                    {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "بازگشت", tint = colors.textPrimary)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (habits.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            icon = Icons.Rounded.Loop,
                            title = "هنوز عادتی ثبت نشده است",
                            description = "عادت‌های روزانه یا هفتگی خود را ایجاد کنید و با ثبت واقعی هر روز استمرار خود را بسازید.",
                            actionLabel = "ثبت عادت جدید",
                            onAction = { showCreate = true }
                        )
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        val stats = remember(habit, records, today) {
                            LifeOsCalculators.computeHabitStats(habit, records, today)
                        }
                        val completedSet = remember(habit, records) {
                            records.asSequence()
                                .filter { it.habitId == habit.id && it.isCompleted }
                                .map { it.epochDay }
                                .toSet()
                        }

                        GlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleHabitForDay(habit.id, today, stats.completedToday) },
                                        modifier = Modifier.testTag("habit_toggle_${habit.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (stats.completedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = if (stats.completedToday) "لغو ثبت امروز" else "ثبت انجام امروز",
                                            tint = if (stats.completedToday) colors.success else colors.oliveAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            StatusBadge(label = habit.frequencyType.toHabitFrequency().persianLabel, tint = colors.oliveAccent)
                                            StatusBadge(
                                                label = if (stats.completedToday) "امروز انجام شد" else "در انتظار ثبت امروز",
                                                tint = if (stats.completedToday) colors.success else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingHabit = habit }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { deletingHabit = habit }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Streak metrics & progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.oliveAccent, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "استمرار فعلی: ${PersianCalendar.formatNumber(stats.currentStreak, preferences.usePersianDigits)} روز",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "بهترین: ${PersianCalendar.formatNumber(stats.bestStreak, preferences.usePersianDigits)} روز • ${PersianCalendar.formatPercent(stats.completionPercent, preferences.usePersianDigits)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            AnimatedGlassProgressBar(progress = stats.completionPercent / 100f, height = 7.dp)

                            Spacer(Modifier.height(12.dp))

                            // Interactive 14-day Habit Calendar Heat Strip (real check-in / undo for any recent day)
                            Text("تقویم ۱۴ روز اخیر (برای ثبت یا لغو لمس کنید)", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Spacer(Modifier.height(6.dp))
                            val last14Days = remember(today) { (13 downTo 0).map { today - it } }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                last14Days.take(7).forEach { day ->
                                    HabitDayDot(
                                        day = day,
                                        completed = day in completedSet,
                                        isToday = day == today,
                                        usePersianDigits = preferences.usePersianDigits,
                                        calendarSystem = preferences.calendarSystem,
                                        onToggle = { viewModel.toggleHabitForDay(habit.id, day, day in completedSet) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                last14Days.drop(7).forEach { day ->
                                    HabitDayDot(
                                        day = day,
                                        completed = day in completedSet,
                                        isToday = day == today,
                                        usePersianDigits = preferences.usePersianDigits,
                                        calendarSystem = preferences.calendarSystem,
                                        onToggle = { viewModel.toggleHabitForDay(habit.id, day, day in completedSet) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Weekly & Monthly Habit Charts
                    item {
                        GlassBarChartCard(
                            title = "نمودار هفتگی عادت‌ها",
                            subtitle = "تعداد ثبت واقعی عادت‌ها در ۷ روز اخیر",
                            points = weeklyHabitPoints,
                            emptyMessage = "پس از ثبت روزانه عادت‌ها، نمودار هفتگی فعال می‌شود.",
                            usePersianDigits = preferences.usePersianDigits,
                            chartHeight = 145.dp
                        )
                    }

                    item {
                        GlassBarChartCard(
                            title = "نمودار ماهانه عادت‌ها",
                            subtitle = "مجموع ثبت عادت‌ها در ۴ هفته اخیر",
                            points = monthlyHabitPoints,
                            emptyMessage = "هنوز داده‌ای در ماه اخیر ثبت نشده است.",
                            usePersianDigits = preferences.usePersianDigits,
                            barColor = colors.info,
                            chartHeight = 145.dp
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreate = true },
            containerColor = colors.oliveAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("add_habit_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "افزودن عادت")
        }
    }

    if (showCreate) {
        HabitEditorDialog(
            categories = categories,
            preferences = preferences,
            onSave = {
                viewModel.saveHabit(it)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }

    editingHabit?.let { h ->
        HabitEditorDialog(
            initialHabit = h,
            categories = categories,
            preferences = preferences,
            onSave = {
                viewModel.saveHabit(it, "عادت به‌روزرسانی شد.")
                editingHabit = null
            },
            onDismiss = { editingHabit = null }
        )
    }

    deletingHabit?.let { h ->
        ConfirmDeleteDialog(
            title = "حذف عادت",
            message = "آیا از حذف عادت «${h.name}» و تاریخچه آن مطمئن هستید؟",
            onConfirm = {
                viewModel.deleteHabit(h.id)
                deletingHabit = null
            },
            onDismiss = { deletingHabit = null }
        )
    }
}

@Composable
private fun HabitDayDot(
    day: Long,
    completed: Boolean,
    isToday: Boolean,
    usePersianDigits: Boolean,
    calendarSystem: com.example.core.calendar.CalendarSystem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    val dayNum = when (calendarSystem) {
        com.example.core.calendar.CalendarSystem.JALALI -> PersianCalendar.epochDayToJalali(day).day
        com.example.core.calendar.CalendarSystem.GREGORIAN -> PersianCalendar.epochDayToGregorian(day).day
    }
    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    completed -> colors.oliveAccent
                    isToday -> colors.oliveSoftContainer
                    else -> colors.glassSurfaceSubtle.copy(alpha = 0.25f)
                }
            )
            .border(
                width = if (isToday) 1.5.dp else 0.5.dp,
                color = if (isToday) colors.oliveAccent else colors.glassBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = PersianCalendar.formatNumber(dayNum, usePersianDigits),
            style = MaterialTheme.typography.labelSmall,
            color = if (completed) Color.White else colors.textPrimary,
            fontWeight = if (completed || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
