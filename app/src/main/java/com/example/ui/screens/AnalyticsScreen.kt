package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.PersianCalendar
import com.example.data.local.GoalStatus
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.ui.components.ChartBarPoint
import com.example.ui.components.ChartSlice
import com.example.ui.components.GlassBarChartCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDonutChartCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GlassHorizontalProgressChartCard
import com.example.ui.components.GlassLineAreaChartCard
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.PersianDatePickerDialog
import com.example.ui.components.TimeRecordEditorDialog
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsCalculators
import com.example.ui.viewmodel.LifeOsViewModel
import kotlin.math.max

private enum class AnalyticsPeriod(val label: String) {
    TODAY("امروز"),
    THIS_WEEK("این هفته"),
    THIS_MONTH("این ماه"),
    LAST_3_MONTHS("سه ماه اخیر"),
    CUSTOM("بازه دلخواه")
}

@Composable
fun AnalyticsScreen(
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val habitRecords by viewModel.habitRecords.collectAsStateWithLifecycle()
    val timeRecords by viewModel.timeRecords.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val today = remember { PersianCalendar.todayEpochDay() }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var period by remember { mutableStateOf(AnalyticsPeriod.THIS_WEEK) }
    var customStart by remember { mutableLongStateOf(today - 14L) }
    var customEnd by remember { mutableLongStateOf(today) }
    var showPickStart by remember { mutableStateOf(false) }
    var showPickEnd by remember { mutableStateOf(false) }
    var showLogTimeDialog by remember { mutableStateOf(false) }

    val (rangeStart, rangeEnd) = remember(period, customStart, customEnd, today) {
        when (period) {
            AnalyticsPeriod.TODAY -> today to today
            AnalyticsPeriod.THIS_WEEK -> (today - 6L) to today
            AnalyticsPeriod.THIS_MONTH -> (today - 29L) to today
            AnalyticsPeriod.LAST_3_MONTHS -> (today - 89L) to today
            AnalyticsPeriod.CUSTOM -> minOf(customStart, customEnd) to maxOf(customStart, customEnd)
        }
    }
    val spanDays = (rangeEnd - rangeStart + 1L).coerceAtLeast(1L)

    // Filter tasks inside period
    val periodTasks = remember(tasks, rangeStart, rangeEnd) {
        tasks.filter { t ->
            val refDay = t.dueEpochDay ?: t.startEpochDay ?: LifeOsCalculators.taskCompletionDay(t)
            refDay == null || refDay in rangeStart..rangeEnd
        }
    }
    val completedTasks = remember(periodTasks) { periodTasks.filter { it.status == TaskStatus.COMPLETED.name } }
    val overdueCount = remember(periodTasks, today) { periodTasks.count { LifeOsCalculators.isTaskOverdue(it, today) } }
    val taskCompletionPct = if (periodTasks.isEmpty()) 0 else ((completedTasks.size * 100f) / periodTasks.size).toInt()
    val avgDailyCompletion = completedTasks.size.toFloat() / spanDays.toFloat()

    val completedGoalsCount = remember(goals) { goals.count { it.status == GoalStatus.COMPLETED.name } }

    val habitStatsList = remember(habits, habitRecords, today) {
        habits.map { h -> h to LifeOsCalculators.computeHabitStats(h, habitRecords, today) }
    }
    val avgHabitSuccessPct = if (habitStatsList.isEmpty()) 0 else {
        habitStatsList.map { it.second.completionPercent }.average().toInt()
    }

    val periodTimeRecords = remember(timeRecords, rangeStart, rangeEnd) {
        timeRecords.filter { it.epochDay in rangeStart..rangeEnd }
    }
    val totalLoggedMinutes = remember(periodTimeRecords, periodTasks) {
        val fromRecords = periodTimeRecords.sumOf { it.durationMinutes }
        val fromTasks = periodTasks.sumOf { it.actualMinutes }
        max(fromRecords, fromTasks)
    }

    // Chart 1: Daily Task Completion (last 7 days of range)
    val dailyPoints = remember(tasks, rangeEnd, preferences.usePersianDigits) {
        (6 downTo 0).map { offset ->
            val d = rangeEnd - offset
            val cnt = tasks.count { LifeOsCalculators.taskCompletionDay(it) == d }
            ChartBarPoint(
                label = PersianCalendar.weekdayShortNames[PersianCalendar.dayOfWeekSaturdayFirst(d)],
                value = cnt.toFloat(),
                detailText = "${PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)} وظیفه تکمیل‌شده"
            )
        }
    }

    // Chart 2: Weekly Performance (4 recent weeks ending rangeEnd)
    val weeklyPoints = remember(tasks, rangeEnd, preferences.usePersianDigits) {
        (3 downTo 0).map { wk ->
            val end = rangeEnd - (wk * 7L)
            val start = end - 6L
            val cnt = tasks.count { t ->
                val day = LifeOsCalculators.taskCompletionDay(t)
                day != null && day in start..end
            }
            ChartBarPoint(
                label = "هفته ${PersianCalendar.formatNumber(4 - wk, preferences.usePersianDigits)}",
                value = cnt.toFloat(),
                detailText = "${PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)} وظیفه تکمیل‌شده"
            )
        }
    }

    // Chart 3: Monthly Performance (6 months ending current month)
    val monthlyPoints = remember(tasks, today, preferences.calendarSystem, preferences.usePersianDigits) {
        (5 downTo 0).map { mOffset ->
            val end = today - (mOffset * 30L)
            val start = end - 29L
            val cnt = tasks.count { t ->
                val day = LifeOsCalculators.taskCompletionDay(t)
                day != null && day in start..end
            }
            val j = PersianCalendar.epochDayToJalali(end)
            ChartBarPoint(
                label = PersianCalendar.jalaliMonthNames[(j.month - 1).coerceIn(0, 11)].take(4),
                value = cnt.toFloat(),
                detailText = "${PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)} وظیفه در این ماه"
            )
        }
    }

    // Chart 4: Goals Progress
    val goalProgressItems = remember(goals, preferences.usePersianDigits) {
        goals.take(6).map { g ->
            Triple(
                g.title,
                g.progressPercent / 100f,
                PersianCalendar.formatPercent(g.progressPercent, preferences.usePersianDigits)
            )
        }
    }

    // Chart 5: Habit Streaks
    val habitStreakPoints = remember(habitStatsList, preferences.usePersianDigits) {
        habitStatsList.take(6).map { (habit, stats) ->
            ChartBarPoint(
                label = habit.name.take(7),
                value = stats.currentStreak.toFloat(),
                detailText = "استمرار ${PersianCalendar.formatNumber(stats.currentStreak, preferences.usePersianDigits)} روز (بهترین: ${PersianCalendar.formatNumber(stats.bestStreak, preferences.usePersianDigits)})"
            )
        }
    }

    // Chart 6: Task Priority Distribution
    val prioritySlices = remember(periodTasks, colors, preferences.usePersianDigits) {
        listOf(
            TaskPriority.LOW to colors.oliveAccent,
            TaskPriority.MEDIUM to colors.info,
            TaskPriority.HIGH to colors.warning,
            TaskPriority.URGENT to colors.danger
        ).map { (p, col) ->
            val cnt = periodTasks.count { it.priority == p.name }
            ChartSlice(
                label = p.persianLabel,
                value = cnt.toFloat(),
                color = col,
                valueLabel = PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)
            )
        }
    }

    // Chart 7: Task Status Breakdown
    val statusSlices = remember(periodTasks, colors, preferences.usePersianDigits) {
        listOf(
            TaskStatus.NOT_STARTED to colors.textSecondary,
            TaskStatus.IN_PROGRESS to colors.info,
            TaskStatus.COMPLETED to colors.success,
            TaskStatus.CANCELLED to colors.danger
        ).map { (st, col) ->
            val cnt = periodTasks.count { it.status == st.name }
            ChartSlice(
                label = st.persianLabel,
                value = cnt.toFloat(),
                color = col,
                valueLabel = PersianCalendar.formatNumber(cnt, preferences.usePersianDigits)
            )
        }
    }

    // Chart 8: Activity Time Allocation by Category
    val timeSlices = remember(periodTimeRecords, periodTasks, categoryMap, colors, preferences.usePersianDigits) {
        val minutesByCat = mutableMapOf<String, Int>()
        if (periodTimeRecords.isNotEmpty()) {
            periodTimeRecords.forEach { tr ->
                val catName = tr.categoryId?.let { categoryMap[it]?.name } ?: "سایر"
                minutesByCat[catName] = (minutesByCat[catName] ?: 0) + tr.durationMinutes
            }
        } else {
            periodTasks.filter { it.actualMinutes > 0 || it.estimatedMinutes > 0 }.forEach { t ->
                val mins = if (t.actualMinutes > 0) t.actualMinutes else t.estimatedMinutes
                val catName = t.categoryId?.let { categoryMap[it]?.name } ?: "سایر"
                minutesByCat[catName] = (minutesByCat[catName] ?: 0) + mins
            }
        }
        minutesByCat.entries.sortedByDescending { it.value }.mapIndexed { idx, (name, mins) ->
            ChartSlice(
                label = name,
                value = mins.toFloat(),
                color = colors.chartPalette[idx % colors.chartPalette.size],
                valueLabel = PersianCalendar.formatDuration(mins, preferences.usePersianDigits)
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LifeOsTopHeader(
            title = "تحلیل و گزارش‌ها",
            subtitle = "آمار واقعی بر اساس فعالیت‌های ثبت‌شده شما",
            navigationAction = onBack?.let {
                {
                    IconButton(onClick = it) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "بازگشت", tint = colors.textPrimary)
                    }
                }
            },
            trailingAction = {
                IconButton(onClick = { showLogTimeDialog = true }) {
                    Icon(Icons.Rounded.Timer, contentDescription = "ثبت زمان فعالیت", tint = colors.oliveAccent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AnalyticsPeriod.entries) { p ->
                        GlassFilterChip(
                            label = p.label,
                            selected = period == p,
                            onClick = { period = p }
                        )
                    }
                }
            }

            if (period == AnalyticsPeriod.CUSTOM) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showPickStart = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.DateRange, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("از: ${PersianCalendar.formatShortDate(customStart, preferences.calendarSystem, preferences.usePersianDigits)}")
                        }
                        OutlinedButton(onClick = { showPickEnd = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.DateRange, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تا: ${PersianCalendar.formatShortDate(customEnd, preferences.calendarSystem, preferences.usePersianDigits)}")
                        }
                    }
                }
            }

            // Key Real Analytics Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricGlassBox(
                            title = "درصد تکمیل وظایف",
                            value = PersianCalendar.formatPercent(taskCompletionPct, preferences.usePersianDigits),
                            tint = colors.oliveAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassBox(
                            title = "وظایف انجام‌شده",
                            value = PersianCalendar.formatNumber(completedTasks.size, preferences.usePersianDigits),
                            tint = colors.success,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricGlassBox(
                            title = "وظایف عقب‌افتاده",
                            value = PersianCalendar.formatNumber(overdueCount, preferences.usePersianDigits),
                            tint = if (overdueCount > 0) colors.danger else colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassBox(
                            title = "میانگین تکمیل روزانه",
                            value = PersianCalendar.formatDigits("%.1f".format(avgDailyCompletion), preferences.usePersianDigits),
                            tint = colors.info,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricGlassBox(
                            title = "اهداف تکمیل‌شده",
                            value = PersianCalendar.formatNumber(completedGoalsCount, preferences.usePersianDigits),
                            tint = colors.oliveAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassBox(
                            title = "موفقیت عادت‌ها",
                            value = PersianCalendar.formatPercent(avgHabitSuccessPct, preferences.usePersianDigits),
                            tint = colors.success,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GlassCard(contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("مجموع زمان ثبت‌شده", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                Text(
                                    text = PersianCalendar.formatDuration(totalLoggedMinutes, preferences.usePersianDigits),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { showLogTimeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("ثبت زمان")
                            }
                        }
                    }
                }
            }

            // 1. Daily Task Completion Chart
            item {
                GlassBarChartCard(
                    title = "۱. نمودار تکمیل وظایف روزانه",
                    subtitle = "تعداد وظایف تکمیل‌شده به تفکیک روز",
                    points = dailyPoints,
                    emptyMessage = "در این بازه هنوز وظیفه‌ای تکمیل نشده است.",
                    usePersianDigits = preferences.usePersianDigits
                )
            }

            // 2. Weekly Performance Chart
            item {
                GlassLineAreaChartCard(
                    title = "۲. نمودار عملکرد هفتگی",
                    subtitle = "روند تکمیل وظایف در ۴ هفته اخیر",
                    points = weeklyPoints,
                    emptyMessage = "پس از تکمیل وظایف در هفته‌های مختلف، روند عملکرد نمایش داده می‌شود.",
                    usePersianDigits = preferences.usePersianDigits
                )
            }

            // 3. Monthly Performance Chart
            item {
                GlassBarChartCard(
                    title = "۳. نمودار عملکرد ماهانه",
                    subtitle = "مقایسه تکمیل وظایف در ۶ ماه اخیر",
                    points = monthlyPoints,
                    emptyMessage = "داده کافی برای نمایش نمودار ماهانه ثبت نشده است.",
                    usePersianDigits = preferences.usePersianDigits,
                    barColor = colors.info
                )
            }

            // 4. Goals Progress Chart
            item {
                GlassHorizontalProgressChartCard(
                    title = "۴. نمودار پیشرفت اهداف",
                    subtitle = "درصد پیشرفت دستی ثبت‌شده برای اهداف",
                    items = goalProgressItems,
                    emptyMessage = "هنوز هدفی ثبت نشده است."
                )
            }

            // 5. Habit Streaks Chart
            item {
                GlassBarChartCard(
                    title = "۵. نمودار استمرار عادت‌ها",
                    subtitle = "تعداد روزهای متوالی انجام هر عادت",
                    points = habitStreakPoints,
                    emptyMessage = "هنوز استمراری برای عادت‌ها ثبت نشده است.",
                    usePersianDigits = preferences.usePersianDigits,
                    barColor = colors.success
                )
            }

            // 6. Task Priority Distribution
            item {
                GlassDonutChartCard(
                    title = "۶. نمودار توزیع اولویت وظایف",
                    subtitle = "سهم وظایف بر اساس سطح اولویت",
                    slices = prioritySlices,
                    emptyMessage = "وظیفه‌ای برای تحلیل اولویت وجود ندارد.",
                    usePersianDigits = preferences.usePersianDigits
                )
            }

            // 7. Task Status Breakdown
            item {
                GlassDonutChartCard(
                    title = "۷. نمودار وضعیت وظایف",
                    subtitle = "تفکیک شروع‌نشده، در حال انجام، تکمیل‌شده و لغوشده",
                    slices = statusSlices,
                    emptyMessage = "وظیفه‌ای در این بازه ثبت نشده است.",
                    usePersianDigits = preferences.usePersianDigits
                )
            }

            // 8. Activity Time Allocation
            item {
                GlassDonutChartCard(
                    title = "۸. نمودار تقسیم زمان فعالیت‌ها",
                    subtitle = "سهم دسته‌بندی‌ها از زمان ثبت‌شده فعالیت‌ها",
                    slices = timeSlices,
                    emptyMessage = "برای مشاهده تقسیم زمان، دکمه «ثبت زمان» را بزنید یا زمان وظایف را ثبت کنید.",
                    usePersianDigits = preferences.usePersianDigits
                )
            }
        }
    }

    if (showPickStart) {
        PersianDatePickerDialog(
            initialEpochDay = customStart,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ شروع بازه",
            onDateSelected = {
                customStart = it
                showPickStart = false
            },
            onDismiss = { showPickStart = false }
        )
    }

    if (showPickEnd) {
        PersianDatePickerDialog(
            initialEpochDay = customEnd,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ پایان بازه",
            onDateSelected = {
                customEnd = it
                showPickEnd = false
            },
            onDismiss = { showPickEnd = false }
        )
    }

    if (showLogTimeDialog) {
        TimeRecordEditorDialog(
            categories = categories,
            preferences = preferences,
            onSave = {
                viewModel.saveTimeRecord(it)
                showLogTimeDialog = false
            },
            onDismiss = { showLogTimeDialog = false }
        )
    }
}

@Composable
private fun MetricGlassBox(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}
