package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.calendar.PersianCalendar
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.TimeRecordEntity
import com.example.data.local.toTaskPriority
import com.example.data.local.toTaskStatus
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.LifeOsTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
    initialTask: TaskEntity? = null,
    defaultEpochDay: Long? = null,
    defaultStartMinute: Int? = null,
    defaultProjectId: String? = null,
    categories: List<CategoryEntity>,
    projects: List<ProjectEntity>,
    preferences: UserPreferences,
    onSave: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val today = remember { PersianCalendar.todayEpochDay() }

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var categoryId by remember { mutableStateOf(initialTask?.categoryId ?: categories.firstOrNull()?.id) }
    var projectId by remember { mutableStateOf(initialTask?.projectId ?: defaultProjectId) }
    var priority by remember { mutableStateOf(initialTask?.priority?.toTaskPriority() ?: TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(initialTask?.status?.toTaskStatus() ?: TaskStatus.NOT_STARTED) }
    var startEpochDay by remember { mutableStateOf(initialTask?.startEpochDay ?: defaultEpochDay ?: today) }
    var dueEpochDay by remember { mutableStateOf(initialTask?.dueEpochDay ?: defaultEpochDay ?: today) }
    var startMinute by remember { mutableStateOf(initialTask?.startMinuteOfDay ?: defaultStartMinute) }
    var endMinute by remember { mutableStateOf(initialTask?.endMinuteOfDay) }
    var estimatedMinutesText by remember { mutableStateOf((initialTask?.estimatedMinutes ?: 30).toString()) }
    var actualMinutesText by remember { mutableStateOf((initialTask?.actualMinutes ?: 0).toString()) }
    var tagsCsv by remember { mutableStateOf(initialTask?.tagsCsv ?: "") }
    var note by remember { mutableStateOf(initialTask?.note ?: "") }
    var reminderEnabled by remember { mutableStateOf(initialTask?.reminderEnabled ?: false) }
    var titleError by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            elevated = true,
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTask == null) "افزودن وظیفه جدید" else "ویرایش وظیفه",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = colors.textSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("عنوان وظیفه *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("لطفاً عنوان وظیفه را وارد کنید.") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اولویت", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { p ->
                        GlassFilterChip(
                            label = p.persianLabel,
                            selected = priority == p,
                            onClick = { priority = p }
                        )
                    }
                }

                Text("وضعیت", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskStatus.entries.forEach { st ->
                        GlassFilterChip(
                            label = st.persianLabel,
                            selected = status == st,
                            onClick = { status = st }
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            GlassFilterChip(
                                label = cat.name,
                                selected = categoryId == cat.id,
                                onClick = { categoryId = if (categoryId == cat.id) null else cat.id }
                            )
                        }
                    }
                }

                if (projects.isNotEmpty()) {
                    Text("پروژه مرتبط (اختیاری)", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassFilterChip(
                            label = "بدون پروژه",
                            selected = projectId == null,
                            onClick = { projectId = null }
                        )
                        projects.forEach { proj ->
                            GlassFilterChip(
                                label = proj.name,
                                selected = projectId == proj.id,
                                onClick = { projectId = proj.id }
                            )
                        }
                    }
                }

                // Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("شروع", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                text = PersianCalendar.formatShortDate(startEpochDay, preferences.calendarSystem, preferences.usePersianDigits),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showDueDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("سررسید", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                text = PersianCalendar.formatShortDate(dueEpochDay, preferences.calendarSystem, preferences.usePersianDigits),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // Times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("ساعت شروع", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                text = startMinute?.let {
                                    PersianCalendar.formatTime(it, preferences.use24HourClock, preferences.usePersianDigits)
                                } ?: "تعیین نشده",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("ساعت پایان", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                text = endMinute?.let {
                                    PersianCalendar.formatTime(it, preferences.use24HourClock, preferences.usePersianDigits)
                                } ?: "تعیین نشده",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = estimatedMinutesText,
                        onValueChange = { estimatedMinutesText = it.filter(Char::isDigit).take(4) },
                        label = { Text("زمان تخمینی (دقیقه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = actualMinutesText,
                        onValueChange = { actualMinutesText = it.filter(Char::isDigit).take(4) },
                        label = { Text("زمان واقعی (دقیقه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tagsCsv,
                    onValueChange = { tagsCsv = it },
                    label = { Text("برچسب‌ها (با ویرگول جدا کنید)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت تکمیلی") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("یادآوری محلی برای این وظیفه", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("انصراف", color = colors.textSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        val estMin = estimatedMinutesText.toIntOrNull() ?: 0
                        val actMin = actualMinutesText.toIntOrNull() ?: 0
                        val completedAt = if (status == TaskStatus.COMPLETED) {
                            initialTask?.completedAtMillis ?: System.currentTimeMillis()
                        } else null
                        val entity = (initialTask ?: TaskEntity(title = title.trim())).copy(
                            title = title.trim(),
                            description = description.trim(),
                            categoryId = categoryId,
                            projectId = projectId,
                            priority = priority.name,
                            status = status.name,
                            startEpochDay = startEpochDay,
                            dueEpochDay = dueEpochDay,
                            startMinuteOfDay = startMinute,
                            endMinuteOfDay = endMinute,
                            estimatedMinutes = estMin,
                            actualMinutes = actMin,
                            tagsCsv = tagsCsv.trim(),
                            note = note.trim(),
                            reminderEnabled = reminderEnabled,
                            completedAtMillis = completedAt
                        )
                        onSave(entity)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.oliveAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("save_task_button")
                ) {
                    Text("ذخیره وظیفه")
                }
            }
        }
    }

    if (showStartDatePicker) {
        PersianDatePickerDialog(
            initialEpochDay = startEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ شروع وظیفه",
            onDateSelected = {
                startEpochDay = it
                if (dueEpochDay < it) dueEpochDay = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showDueDatePicker) {
        PersianDatePickerDialog(
            initialEpochDay = dueEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ سررسید وظیفه",
            onDateSelected = {
                dueEpochDay = it
                showDueDatePicker = false
            },
            onDismiss = { showDueDatePicker = false }
        )
    }

    if (showStartTimePicker) {
        PersianTimePickerDialog(
            initialMinuteOfDay = startMinute ?: (9 * 60),
            usePersianDigits = preferences.usePersianDigits,
            title = "ساعت شروع",
            onTimeSelected = {
                startMinute = it
                if (endMinute == null || endMinute!! <= it) {
                    endMinute = (it + (estimatedMinutesText.toIntOrNull()?.coerceAtLeast(15) ?: 30)).coerceAtMost(23 * 60 + 55)
                }
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        PersianTimePickerDialog(
            initialMinuteOfDay = endMinute ?: ((startMinute ?: 540) + 60).coerceAtMost(1435),
            usePersianDigits = preferences.usePersianDigits,
            title = "ساعت پایان",
            onTimeSelected = {
                endMinute = it
                val st = startMinute
                if (st != null && it > st) {
                    estimatedMinutesText = (it - st).toString()
                }
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventEditorDialog(
    initialEvent: CalendarEventEntity? = null,
    defaultEpochDay: Long = PersianCalendar.todayEpochDay(),
    categories: List<CategoryEntity>,
    preferences: UserPreferences,
    onSave: (CalendarEventEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var epochDay by remember { mutableStateOf(initialEvent?.epochDay ?: defaultEpochDay) }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var startMinute by remember { mutableStateOf(initialEvent?.startMinuteOfDay ?: 600) }
    var endMinute by remember { mutableStateOf(initialEvent?.endMinuteOfDay ?: 660) }
    var categoryId by remember { mutableStateOf(initialEvent?.categoryId ?: categories.firstOrNull()?.id) }
    var reminderEnabled by remember { mutableStateOf(initialEvent?.reminderEnabled ?: false) }
    var titleError by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp),
            elevated = true,
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialEvent == null) "افزودن رویداد جدید" else "ویرایش رویداد",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = colors.textSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("عنوان رویداد *") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("مکان (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "تاریخ: ${PersianCalendar.formatFullDate(epochDay, preferences.calendarSystem, preferences.usePersianDigits)}",
                        color = colors.textPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("رویداد تمام‌روز", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }

                if (!isAllDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartTime = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("شروع: ${PersianCalendar.formatTime(startMinute, preferences.use24HourClock, preferences.usePersianDigits)}")
                        }
                        OutlinedButton(
                            onClick = { showEndTime = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("پایان: ${PersianCalendar.formatTime(endMinute, preferences.use24HourClock, preferences.usePersianDigits)}")
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            GlassFilterChip(
                                label = cat.name,
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("یادآوری اعلان", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("انصراف", color = colors.textSecondary) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        val entity = (initialEvent ?: CalendarEventEntity(title = title.trim(), epochDay = epochDay)).copy(
                            title = title.trim(),
                            description = description.trim(),
                            location = location.trim(),
                            epochDay = epochDay,
                            isAllDay = isAllDay,
                            startMinuteOfDay = if (isAllDay) null else startMinute,
                            endMinuteOfDay = if (isAllDay) null else endMinute,
                            categoryId = categoryId,
                            reminderEnabled = reminderEnabled
                        )
                        onSave(entity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("save_event_button")
                ) {
                    Text("ذخیره رویداد")
                }
            }
        }
    }

    if (showDatePicker) {
        PersianDatePickerDialog(
            initialEpochDay = epochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "انتخاب تاریخ رویداد",
            onDateSelected = {
                epochDay = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showStartTime) {
        PersianTimePickerDialog(
            initialMinuteOfDay = startMinute,
            usePersianDigits = preferences.usePersianDigits,
            title = "ساعت شروع رویداد",
            onTimeSelected = {
                startMinute = it
                if (endMinute <= it) endMinute = (it + 60).coerceAtMost(1435)
                showStartTime = false
            },
            onDismiss = { showStartTime = false }
        )
    }
    if (showEndTime) {
        PersianTimePickerDialog(
            initialMinuteOfDay = endMinute,
            usePersianDigits = preferences.usePersianDigits,
            title = "ساعت پایان رویداد",
            onTimeSelected = {
                endMinute = it
                showEndTime = false
            },
            onDismiss = { showEndTime = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeRecordEditorDialog(
    categories: List<CategoryEntity>,
    preferences: UserPreferences,
    onSave: (TimeRecordEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("45") }
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var epochDay by remember { mutableStateOf(PersianCalendar.todayEpochDay()) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(elevated = true, shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(18.dp)) {
            Text("ثبت زمان فعالیت", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text("عنوان فعالیت *") },
                isError = titleError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it.filter(Char::isDigit).take(4) },
                label = { Text("مدت زمان (دقیقه)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            if (categories.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        GlassFilterChip(
                            label = cat.name,
                            selected = categoryId == cat.id,
                            onClick = { categoryId = cat.id }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("انصراف", color = colors.textSecondary) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val mins = durationText.toIntOrNull() ?: 0
                        if (title.isBlank() || mins <= 0) {
                            titleError = title.isBlank()
                            return@Button
                        }
                        onSave(
                            TimeRecordEntity(
                                title = title.trim(),
                                categoryId = categoryId,
                                epochDay = epochDay,
                                startMinuteOfDay = PersianCalendar.currentMinuteOfDay(),
                                durationMinutes = mins
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White)
                ) {
                    Text("ثبت زمان")
                }
            }
        }
    }
}
