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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.PersianCalendar
import com.example.data.local.CalendarEventEntity
import com.example.data.local.TaskStatus
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.EventEditorDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsCalculators
import com.example.ui.viewmodel.LifeOsViewModel

private enum class CalendarViewScope(val label: String) {
    MONTHLY("ماهانه"),
    WEEKLY("هفتگی"),
    DAILY("روزانه")
}

@Composable
fun CalendarScreen(
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    val events by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val today = remember { PersianCalendar.todayEpochDay() }
    var selectedDay by remember { mutableLongStateOf(today) }
    var scope by remember { mutableStateOf(CalendarViewScope.MONTHLY) }
    var activeSystem by remember(preferences.calendarSystem) { mutableStateOf(preferences.calendarSystem) }

    var showCreateEvent by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var deletingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    val dayEvents = remember(events, selectedDay) {
        events.filter { it.epochDay == selectedDay }.sortedBy { it.startMinuteOfDay ?: -1 }
    }
    val dayTasks = remember(tasks, selectedDay) {
        tasks.filter { LifeOsCalculators.isTaskForDay(it, selectedDay) }
    }
    val dayDueTasks = remember(tasks, selectedDay) {
        tasks.filter { it.dueEpochDay == selectedDay && it.status != TaskStatus.COMPLETED.name && it.status != TaskStatus.CANCELLED.name }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "تقویم",
                subtitle = "${PersianCalendar.weekdayName(selectedDay)}، ${PersianCalendar.formatFullDate(selectedDay, activeSystem, preferences.usePersianDigits)}",
                navigationAction = onBack?.let {
                    {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "بازگشت", tint = colors.textPrimary)
                        }
                    }
                },
                trailingAction = {
                    TextButton(onClick = { selectedDay = today }) {
                        Text("امروز", color = colors.oliveAccent)
                    }
                }
            )

            // Calendar system switcher (Jalali / Gregorian) + View scope
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassFilterChip(
                    label = "شمسی",
                    selected = activeSystem == CalendarSystem.JALALI,
                    onClick = { activeSystem = CalendarSystem.JALALI }
                )
                GlassFilterChip(
                    label = "میلادی",
                    selected = activeSystem == CalendarSystem.GREGORIAN,
                    onClick = { activeSystem = CalendarSystem.GREGORIAN }
                )
                Spacer(Modifier.weight(1f))
                CalendarViewScope.entries.forEach { s ->
                    GlassFilterChip(
                        label = s.label,
                        selected = scope == s,
                        onClick = { scope = s }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (scope == CalendarViewScope.MONTHLY) {
                    item {
                        val (year, month) = when (activeSystem) {
                            CalendarSystem.JALALI -> PersianCalendar.epochDayToJalali(selectedDay).let { it.year to it.month }
                            CalendarSystem.GREGORIAN -> PersianCalendar.epochDayToGregorian(selectedDay).let { it.year to it.month }
                        }
                        val monthLen = when (activeSystem) {
                            CalendarSystem.JALALI -> PersianCalendar.jalaliMonthLength(year, month)
                            CalendarSystem.GREGORIAN -> PersianCalendar.gregorianMonthLength(year, month)
                        }
                        val firstDay = when (activeSystem) {
                            CalendarSystem.JALALI -> PersianCalendar.jalaliToEpochDay(year, month, 1)
                            CalendarSystem.GREGORIAN -> PersianCalendar.gregorianToEpochDay(year, month, 1)
                        }
                        val weekdayOrder = PersianCalendar.weekdayOrder(preferences.weekStartDay)
                        val blanks = weekdayOrder.indexOf(PersianCalendar.dayOfWeekSaturdayFirst(firstDay)).coerceAtLeast(0)
                        val cells = buildList<Long?> {
                            repeat(blanks) { add(null) }
                            for (i in 0 until monthLen) add(firstDay + i)
                        }

                        GlassCard(contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedDay = firstDay - 1L }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "ماه قبل", tint = colors.textPrimary)
                                }
                                Text(
                                    text = PersianCalendar.formatMonthYear(year, month, activeSystem, preferences.usePersianDigits),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                IconButton(onClick = { selectedDay = firstDay + monthLen }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "ماه بعد", tint = colors.textPrimary)
                                }
                            }

                            Spacer(Modifier.height(8.dp))
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

                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                cells.chunked(7).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (col in 0 until 7) {
                                            val d = row.getOrNull(col)
                                            if (d == null) {
                                                Box(Modifier.weight(1f).aspectRatio(1f))
                                            } else {
                                                val dayNum = when (activeSystem) {
                                                    CalendarSystem.JALALI -> PersianCalendar.epochDayToJalali(d).day
                                                    CalendarSystem.GREGORIAN -> PersianCalendar.epochDayToGregorian(d).day
                                                }
                                                val hasEv = events.any { it.epochDay == d }
                                                val hasTk = tasks.any { LifeOsCalculators.isTaskForDay(it, d) }
                                                val isSel = d == selectedDay
                                                val isToday = d == today
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            when {
                                                                isSel -> colors.oliveAccent
                                                                isToday -> colors.oliveSoftContainer
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .border(
                                                            width = if (isToday && !isSel) 1.5.dp else 0.dp,
                                                            color = if (isToday && !isSel) colors.oliveAccent else Color.Transparent,
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { selectedDay = d },
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = PersianCalendar.formatNumber(dayNum, preferences.usePersianDigits),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSel) Color.White else colors.textPrimary,
                                                        fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        if (hasEv) {
                                                            Box(
                                                                Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSel) Color.White else colors.info)
                                                            )
                                                        }
                                                        if (hasTk) {
                                                            Box(
                                                                Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSel) Color.White.copy(alpha = 0.7f) else colors.oliveAccent)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors.info))
                                    Text("رویداد", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors.oliveAccent))
                                    Text("وظیفه / سررسید", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                } else if (scope == CalendarViewScope.WEEKLY) {
                    item {
                        val weekStart = PersianCalendar.startOfWeekEpochDay(selectedDay, preferences.weekStartDay)
                        val days = List(7) { weekStart + it }
                        GlassCard(contentPadding = PaddingValues(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                days.forEach { d ->
                                    val dayNum = when (activeSystem) {
                                        CalendarSystem.JALALI -> PersianCalendar.epochDayToJalali(d).day
                                        CalendarSystem.GREGORIAN -> PersianCalendar.epochDayToGregorian(d).day
                                    }
                                    val isSel = d == selectedDay
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSel) colors.oliveAccent else colors.glassSurfaceSubtle.copy(alpha = 0.25f))
                                            .clickable { selectedDay = d }
                                            .padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = PersianCalendar.weekdayShortNames[PersianCalendar.dayOfWeekSaturdayFirst(d)],
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSel) Color.White else colors.textSecondary
                                        )
                                        Text(
                                            text = PersianCalendar.formatNumber(dayNum, preferences.usePersianDigits),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSel) Color.White else colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dual calendar equivalent date info bar
                item {
                    val altSystem = if (activeSystem == CalendarSystem.JALALI) CalendarSystem.GREGORIAN else CalendarSystem.JALALI
                    val altLabel = if (altSystem == CalendarSystem.GREGORIAN) "معادل میلادی" else "معادل شمسی"
                    GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$altLabel: ${PersianCalendar.formatFullDate(selectedDay, altSystem, preferences.usePersianDigits)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                            if (dayDueTasks.isNotEmpty()) {
                                StatusBadge(
                                    label = "${PersianCalendar.formatNumber(dayDueTasks.size, preferences.usePersianDigits)} سررسید",
                                    tint = colors.warning
                                )
                            }
                        }
                    }
                }

                // Events of Selected Day
                item {
                    Text(
                        text = "رویدادهای این روز (${PersianCalendar.formatNumber(dayEvents.size, preferences.usePersianDigits)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }

                if (dayEvents.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            icon = Icons.Rounded.Event,
                            title = "رویدادی برای این روز ثبت نشده",
                            description = "رویدادها، جلسات و قرارهای مهم خود را در تقویم ثبت کنید.",
                            actionLabel = "ایجاد رویداد",
                            onAction = { showCreateEvent = true }
                        )
                    }
                } else {
                    items(dayEvents, key = { it.id }) { ev ->
                        GlassCard(
                            accentTint = colors.info.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(14.dp),
                            onClick = { editingEvent = ev }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusBadge(label = "رویداد", tint = colors.info)
                                        Text(
                                            text = if (ev.isAllDay) "تمام‌روز" else ev.startMinuteOfDay?.let {
                                                PersianCalendar.formatTime(it, preferences.use24HourClock, preferences.usePersianDigits)
                                            } ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.info
                                        )
                                    }
                                    Text(ev.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    if (ev.description.isNotBlank()) {
                                        Text(ev.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (ev.location.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                            Text(ev.location, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingEvent = ev }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = colors.textSecondary, modifier = Modifier.size(19.dp))
                                    }
                                    IconButton(onClick = { deletingEvent = ev }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(19.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Tasks & Deadlines on Selected Day
                if (dayTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "وظایف و سررسیدهای این روز (${PersianCalendar.formatNumber(dayTasks.size, preferences.usePersianDigits)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    items(dayTasks, key = { "cal_t_${it.id}" }) { task ->
                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(12.dp)
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
                                    Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = colors.oliveAccent, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(task.title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (task.dueEpochDay == selectedDay) {
                                            Text("سررسید وظیفه", style = MaterialTheme.typography.labelSmall, color = colors.warning)
                                        }
                                    }
                                }
                                StatusBadge(label = "وظیفه", tint = colors.oliveAccent)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateEvent = true },
            containerColor = colors.oliveAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("add_event_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "ایجاد رویداد")
        }
    }

    if (showCreateEvent) {
        EventEditorDialog(
            defaultEpochDay = selectedDay,
            categories = categories,
            preferences = preferences,
            onSave = {
                viewModel.saveCalendarEvent(it)
                showCreateEvent = false
            },
            onDismiss = { showCreateEvent = false }
        )
    }

    editingEvent?.let { ev ->
        EventEditorDialog(
            initialEvent = ev,
            categories = categories,
            preferences = preferences,
            onSave = {
                viewModel.saveCalendarEvent(it, "رویداد به‌روزرسانی شد.")
                editingEvent = null
            },
            onDismiss = { editingEvent = null }
        )
    }

    deletingEvent?.let { ev ->
        ConfirmDeleteDialog(
            title = "حذف رویداد",
            message = "آیا از حذف رویداد «${ev.title}» مطمئن هستید؟",
            onConfirm = {
                viewModel.deleteCalendarEvent(ev.id)
                deletingEvent = null
            },
            onDismiss = { deletingEvent = null }
        )
    }
}
