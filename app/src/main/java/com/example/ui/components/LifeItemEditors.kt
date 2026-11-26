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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.calendar.PersianCalendar
import com.example.data.local.CategoryEntity
import com.example.data.local.GoalEntity
import com.example.data.local.GoalStatus
import com.example.data.local.HabitEntity
import com.example.data.local.HabitFrequency
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.toGoalStatus
import com.example.data.local.toHabitFrequency
import com.example.data.local.toProjectStatus
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.LifeOsTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalEditorDialog(
    initialGoal: GoalEntity? = null,
    categories: List<CategoryEntity>,
    preferences: UserPreferences,
    onSave: (GoalEntity, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val today = remember { PersianCalendar.todayEpochDay() }
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var description by remember { mutableStateOf(initialGoal?.description ?: "") }
    var categoryId by remember { mutableStateOf(initialGoal?.categoryId ?: categories.firstOrNull()?.id) }
    var startEpochDay by remember { mutableStateOf(initialGoal?.startEpochDay ?: today) }
    var endEpochDay by remember { mutableStateOf(initialGoal?.endEpochDay ?: (today + 30L)) }
    var progress by remember { mutableFloatStateOf((initialGoal?.progressPercent ?: 0).toFloat()) }
    var status by remember { mutableStateOf(initialGoal?.status?.toGoalStatus() ?: GoalStatus.ACTIVE) }
    var notes by remember { mutableStateOf(initialGoal?.notes ?: "") }
    var progressNote by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp),
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
                    text = if (initialGoal == null) "ایجاد هدف جدید" else "ویرایش هدف",
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
                    label = { Text("عنوان هدف *") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات هدف") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("وضعیت هدف", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalStatus.entries.forEach { st ->
                        GlassFilterChip(
                            label = st.persianLabel,
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st == GoalStatus.COMPLETED) progress = 100f
                            }
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("شروع", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(PersianCalendar.formatShortDate(startEpochDay, preferences.calendarSystem, preferences.usePersianDigits))
                        }
                    }
                    OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("پایان", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(PersianCalendar.formatShortDate(endEpochDay, preferences.calendarSystem, preferences.usePersianDigits))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("درصد پیشرفت دستی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                        Text(
                            text = PersianCalendar.formatPercent(progress.toInt(), preferences.usePersianDigits),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.oliveAccent
                        )
                    }
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.oliveAccent,
                            activeTrackColor = colors.oliveAccent
                        )
                    )
                }

                if (initialGoal != null) {
                    OutlinedTextField(
                        value = progressNote,
                        onValueChange = { progressNote = it },
                        label = { Text("توضیح ثبت در تاریخچه پیشرفت (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت‌ها") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        val pct = progress.toInt().coerceIn(0, 100)
                        val entity = (initialGoal ?: GoalEntity(title = title.trim(), startEpochDay = startEpochDay)).copy(
                            title = title.trim(),
                            description = description.trim(),
                            categoryId = categoryId,
                            startEpochDay = startEpochDay,
                            endEpochDay = endEpochDay,
                            progressPercent = pct,
                            status = status.name,
                            notes = notes.trim()
                        )
                        onSave(entity, progressNote.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("save_goal_button")
                ) {
                    Text("ذخیره هدف")
                }
            }
        }
    }

    if (showStartPicker) {
        PersianDatePickerDialog(
            initialEpochDay = startEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ شروع هدف",
            onDateSelected = {
                startEpochDay = it
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        PersianDatePickerDialog(
            initialEpochDay = endEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ پایان هدف",
            onDateSelected = {
                endEpochDay = it
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditorDialog(
    initialHabit: HabitEntity? = null,
    categories: List<CategoryEntity>,
    preferences: UserPreferences,
    onSave: (HabitEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val today = remember { PersianCalendar.todayEpochDay() }
    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var categoryId by remember { mutableStateOf(initialHabit?.categoryId ?: categories.firstOrNull()?.id) }
    var startEpochDay by remember { mutableStateOf(initialHabit?.startEpochDay ?: today) }
    var frequency by remember { mutableStateOf(initialHabit?.frequencyType?.toHabitFrequency() ?: HabitFrequency.DAILY) }
    var weeklyTarget by remember { mutableIntStateOf(initialHabit?.weeklyTarget ?: 5) }
    var selectedDaysMask by remember { mutableIntStateOf(initialHabit?.selectedDaysMask ?: 0b1111111) }
    var note by remember { mutableStateOf(initialHabit?.note ?: "") }
    var reminderMinute by remember { mutableStateOf(initialHabit?.reminderMinuteOfDay) }
    var nameError by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp),
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
                    text = if (initialHabit == null) "ثبت عادت جدید" else "ویرایش عادت",
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
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("نام عادت *") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("تناوب انجام", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitFrequency.entries.forEach { freq ->
                        GlassFilterChip(
                            label = freq.persianLabel,
                            selected = frequency == freq,
                            onClick = { frequency = freq }
                        )
                    }
                }

                if (frequency == HabitFrequency.TIMES_PER_WEEK) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "هدف هفتگی: ${PersianCalendar.formatNumber(weeklyTarget, preferences.usePersianDigits)} روز",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { if (weeklyTarget > 1) weeklyTarget-- }) { Text("−") }
                            OutlinedButton(onClick = { if (weeklyTarget < 7) weeklyTarget++ }) { Text("+") }
                        }
                    }
                }

                if (frequency == HabitFrequency.SPECIFIC_DAYS) {
                    Text("روزهای انتخابی هفته", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PersianCalendar.weekdayNames.forEachIndexed { idx, dayName ->
                            val selected = (selectedDaysMask and (1 shl idx)) != 0
                            GlassFilterChip(
                                label = dayName,
                                selected = selected,
                                onClick = {
                                    val toggled = selectedDaysMask xor (1 shl idx)
                                    if (toggled != 0) selectedDaysMask = toggled
                                }
                            )
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(PersianCalendar.formatShortDate(startEpochDay, preferences.calendarSystem, preferences.usePersianDigits))
                    }
                    OutlinedButton(onClick = { showReminderPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            reminderMinute?.let {
                                PersianCalendar.formatTime(it, preferences.use24HourClock, preferences.usePersianDigits)
                            } ?: "بدون یادآوری"
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("انصراف", color = colors.textSecondary) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val target = when (frequency) {
                            HabitFrequency.DAILY -> 7
                            HabitFrequency.WEEKLY -> 1
                            HabitFrequency.TIMES_PER_WEEK -> weeklyTarget
                            HabitFrequency.SPECIFIC_DAYS -> Integer.bitCount(selectedDaysMask).coerceIn(1, 7)
                        }
                        val entity = (initialHabit ?: HabitEntity(name = name.trim(), startEpochDay = startEpochDay)).copy(
                            name = name.trim(),
                            description = description.trim(),
                            categoryId = categoryId,
                            startEpochDay = startEpochDay,
                            frequencyType = frequency.name,
                            weeklyTarget = target,
                            selectedDaysMask = selectedDaysMask,
                            note = note.trim(),
                            reminderMinuteOfDay = reminderMinute
                        )
                        onSave(entity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("save_habit_button")
                ) {
                    Text("ذخیره عادت")
                }
            }
        }
    }

    if (showStartPicker) {
        PersianDatePickerDialog(
            initialEpochDay = startEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ شروع عادت",
            onDateSelected = {
                startEpochDay = it
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showReminderPicker) {
        PersianTimePickerDialog(
            initialMinuteOfDay = reminderMinute ?: (20 * 60),
            usePersianDigits = preferences.usePersianDigits,
            title = "ساعت یادآوری عادت",
            onTimeSelected = {
                reminderMinute = it
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectEditorDialog(
    initialProject: ProjectEntity? = null,
    preferences: UserPreferences,
    onSave: (ProjectEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val today = remember { PersianCalendar.todayEpochDay() }
    var name by remember { mutableStateOf(initialProject?.name ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var startEpochDay by remember { mutableStateOf(initialProject?.startEpochDay ?: today) }
    var deadlineEpochDay by remember { mutableStateOf(initialProject?.deadlineEpochDay ?: (today + 30L)) }
    var status by remember { mutableStateOf(initialProject?.status?.toProjectStatus() ?: ProjectStatus.ACTIVE) }
    var progress by remember { mutableFloatStateOf((initialProject?.progressPercent ?: 0).toFloat()) }
    var notes by remember { mutableStateOf(initialProject?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp),
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
                    text = if (initialProject == null) "ایجاد پروژه جدید" else "ویرایش پروژه",
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
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("نام پروژه *") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات پروژه") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("وضعیت پروژه", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectStatus.entries.forEach { st ->
                        GlassFilterChip(
                            label = st.persianLabel,
                            selected = status == st,
                            onClick = { status = st }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                        Column {
                            Text("شروع", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(PersianCalendar.formatShortDate(startEpochDay, preferences.calendarSystem, preferences.usePersianDigits))
                        }
                    }
                    OutlinedButton(onClick = { showDeadlinePicker = true }, modifier = Modifier.weight(1f)) {
                        Column {
                            Text("مهلت پایان", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(PersianCalendar.formatShortDate(deadlineEpochDay, preferences.calendarSystem, preferences.usePersianDigits))
                        }
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("پیشرفت دستی پروژه", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                        Text(
                            PersianCalendar.formatPercent(progress.toInt(), preferences.usePersianDigits),
                            color = colors.oliveAccent
                        )
                    }
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.oliveAccent,
                            activeTrackColor = colors.oliveAccent
                        )
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت‌های پروژه") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("انصراف", color = colors.textSecondary) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val entity = (initialProject ?: ProjectEntity(name = name.trim(), startEpochDay = startEpochDay)).copy(
                            name = name.trim(),
                            description = description.trim(),
                            startEpochDay = startEpochDay,
                            deadlineEpochDay = deadlineEpochDay,
                            status = status.name,
                            progressPercent = progress.toInt().coerceIn(0, 100),
                            notes = notes.trim()
                        )
                        onSave(entity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("save_project_button")
                ) {
                    Text("ذخیره پروژه")
                }
            }
        }
    }

    if (showStartPicker) {
        PersianDatePickerDialog(
            initialEpochDay = startEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "تاریخ شروع پروژه",
            onDateSelected = {
                startEpochDay = it
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showDeadlinePicker) {
        PersianDatePickerDialog(
            initialEpochDay = deadlineEpochDay,
            calendarSystem = preferences.calendarSystem,
            weekStartDay = preferences.weekStartDay,
            usePersianDigits = preferences.usePersianDigits,
            title = "مهلت پایان پروژه",
            onDateSelected = {
                deadlineEpochDay = it
                showDeadlinePicker = false
            },
            onDismiss = { showDeadlinePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorDialog(
    initialNote: NoteEntity? = null,
    categories: List<CategoryEntity>,
    onSave: (NoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var categoryId by remember { mutableStateOf(initialNote?.categoryId ?: categories.firstOrNull()?.id) }
    var tagsCsv by remember { mutableStateOf(initialNote?.tagsCsv ?: "") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp),
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
                    text = if (initialNote == null) "یادداشت جدید" else "ویرایش یادداشت",
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
                    label = { Text("عنوان یادداشت *") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("متن یادداشت") },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )

                if (categories.isNotEmpty()) {
                    Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
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

                OutlinedTextField(
                    value = tagsCsv,
                    onValueChange = { tagsCsv = it },
                    label = { Text("برچسب‌ها (جداشده با ویرگول)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, tint = colors.oliveAccent)
                        Text("سنجاق کردن به بالای فهرست", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    }
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
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
                        val entity = (initialNote ?: NoteEntity(title = title.trim())).copy(
                            title = title.trim(),
                            content = content.trim(),
                            categoryId = categoryId,
                            tagsCsv = tagsCsv.trim(),
                            isPinned = isPinned,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                        onSave(entity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Text("ذخیره یادداشت")
                }
            }
        }
    }
}
