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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.PersianCalendar
import com.example.data.local.GoalEntity
import com.example.data.local.GoalProgressLogEntity
import com.example.data.local.GoalStatus
import com.example.data.local.MilestoneEntity
import com.example.data.local.toGoalStatus
import com.example.data.preferences.UserPreferences
import com.example.ui.components.AnimatedGlassProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GoalEditorDialog
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.goalStatusColor
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsViewModel
import java.util.Calendar

private enum class GoalTimeFilter(val label: String) {
    ALL("همه زمان‌ها"),
    SHORT_TERM("کوتاه‌مدت (تا ۳ ماه)"),
    LONG_TERM("بلندمدت (بیش از ۳ ماه)")
}

@Composable
fun GoalsScreen(viewModel: LifeOsViewModel) {
    val colors = LifeOsTheme.colors
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val milestones by viewModel.milestones.collectAsStateWithLifecycle()
    val progressLogs by viewModel.goalProgressLogs.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var statusFilter by remember { mutableStateOf<GoalStatus?>(null) }
    var timeFilter by remember { mutableStateOf(GoalTimeFilter.ALL) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var selectedDetailGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var deletingGoal by remember { mutableStateOf<GoalEntity?>(null) }

    val filteredGoals = remember(goals, statusFilter, timeFilter) {
        goals.filter { goal ->
            val matchesStatus = statusFilter == null || goal.status == statusFilter!!.name
            val spanDays = (goal.endEpochDay ?: (goal.startEpochDay + 30L)) - goal.startEpochDay
            val matchesTime = when (timeFilter) {
                GoalTimeFilter.ALL -> true
                GoalTimeFilter.SHORT_TERM -> spanDays <= 90L
                GoalTimeFilter.LONG_TERM -> spanDays > 90L
            }
            matchesStatus && matchesTime
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "مدیریت اهداف",
                subtitle = "${PersianCalendar.formatNumber(goals.count { it.status == GoalStatus.ACTIVE.name }, preferences.usePersianDigits)} هدف فعال"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            GlassFilterChip(
                                label = "همه وضعیت‌ها",
                                selected = statusFilter == null,
                                onClick = { statusFilter = null }
                            )
                        }
                        items(GoalStatus.entries) { st ->
                            GlassFilterChip(
                                label = st.persianLabel,
                                selected = statusFilter == st,
                                onClick = { statusFilter = if (statusFilter == st) null else st }
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GoalTimeFilter.entries) { tf ->
                            GlassFilterChip(
                                label = tf.label,
                                selected = timeFilter == tf,
                                onClick = { timeFilter = tf }
                            )
                        }
                    }
                }

                if (filteredGoals.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            icon = Icons.Rounded.Flag,
                            title = "هدفی ثبت نشده است",
                            description = "اهداف کوتاه‌مدت و بلندمدت خود را تعریف کنید، مراحل آن را بچینید و پیشرفت را دستی دنبال کنید.",
                            actionLabel = "ایجاد هدف جدید",
                            onAction = { showCreateDialog = true }
                        )
                    }
                } else {
                    items(filteredGoals, key = { it.id }) { goal ->
                        val goalMilestones = milestones.filter { it.goalId == goal.id }
                        val doneMilestones = goalMilestones.count { it.isCompleted }
                        val status = goal.status.toGoalStatus()

                        GlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(16.dp),
                            onClick = { selectedDetailGoal = goal }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (goal.description.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = goal.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingGoal = goal }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { deletingGoal = goal }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(label = status.persianLabel, tint = goalStatusColor(status))
                                goal.categoryId?.let { categoryMap[it] }?.let { cat ->
                                    StatusBadge(label = cat.name, tint = colors.oliveAccent)
                                }
                                if (goalMilestones.isNotEmpty()) {
                                    Text(
                                        text = "مراحل: ${PersianCalendar.formatNumber(doneMilestones, preferences.usePersianDigits)}/${PersianCalendar.formatNumber(goalMilestones.size, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("پیشرفت", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                Text(
                                    text = PersianCalendar.formatPercent(goal.progressPercent, preferences.usePersianDigits),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.oliveAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            AnimatedGlassProgressBar(progress = goal.progressPercent / 100f, height = 9.dp)

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "شروع: ${PersianCalendar.formatShortDate(goal.startEpochDay, preferences.calendarSystem, preferences.usePersianDigits)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                                goal.endEpochDay?.let { end ->
                                    Text(
                                        text = "پایان: ${PersianCalendar.formatShortDate(end, preferences.calendarSystem, preferences.usePersianDigits)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = colors.oliveAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("add_goal_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "ایجاد هدف")
        }
    }

    if (showCreateDialog) {
        GoalEditorDialog(
            categories = categories,
            preferences = preferences,
            onSave = { goal, note ->
                viewModel.saveGoal(goal, note)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingGoal?.let { g ->
        GoalEditorDialog(
            initialGoal = g,
            categories = categories,
            preferences = preferences,
            onSave = { updated, note ->
                viewModel.saveGoal(updated, note, "هدف به‌روزرسانی شد.")
                editingGoal = null
            },
            onDismiss = { editingGoal = null }
        )
    }

    selectedDetailGoal?.let { goalSnapshot ->
        val liveGoal = goals.firstOrNull { it.id == goalSnapshot.id } ?: goalSnapshot
        val goalMilestones = milestones.filter { it.goalId == liveGoal.id }
        val logs = progressLogs.filter { it.goalId == liveGoal.id }
        GoalDetailDialog(
            goal = liveGoal,
            milestones = goalMilestones,
            logs = logs,
            preferences = preferences,
            onUpdateProgress = { newPercent, logNote ->
                val newStatus = if (newPercent == 100) GoalStatus.COMPLETED.name else liveGoal.status
                viewModel.saveGoal(
                    liveGoal.copy(progressPercent = newPercent, status = newStatus),
                    progressNote = logNote.ifBlank { "به‌روزرسانی دستی پیشرفت" },
                    successMessage = "پیشرفت هدف ثبت شد."
                )
            },
            onAddMilestone = { title ->
                viewModel.saveMilestone(
                    MilestoneEntity(
                        goalId = liveGoal.id,
                        title = title,
                        sortOrder = goalMilestones.size
                    )
                )
            },
            onToggleMilestone = { ms ->
                viewModel.saveMilestone(
                    ms.copy(
                        isCompleted = !ms.isCompleted,
                        completedAtMillis = if (!ms.isCompleted) System.currentTimeMillis() else null
                    )
                )
            },
            onDeleteMilestone = { viewModel.deleteMilestone(it.id) },
            onDismiss = { selectedDetailGoal = null }
        )
    }

    deletingGoal?.let { g ->
        ConfirmDeleteDialog(
            title = "حذف هدف",
            message = "آیا از حذف هدف «${g.title}» و مراحل آن مطمئن هستید؟",
            onConfirm = {
                viewModel.deleteGoal(g.id)
                deletingGoal = null
            },
            onDismiss = { deletingGoal = null }
        )
    }
}

@Composable
private fun GoalDetailDialog(
    goal: GoalEntity,
    milestones: List<MilestoneEntity>,
    logs: List<GoalProgressLogEntity>,
    preferences: UserPreferences,
    onUpdateProgress: (Int, String) -> Unit,
    onAddMilestone: (String) -> Unit,
    onToggleMilestone: (MilestoneEntity) -> Unit,
    onDeleteMilestone: (MilestoneEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
    var sliderProgress by remember(goal.progressPercent) { mutableFloatStateOf(goal.progressPercent.toFloat()) }
    var progressNote by remember { mutableStateOf("") }
    var newMilestoneTitle by remember { mutableStateOf("") }

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
                    text = goal.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = colors.textSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Manual progress updater
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.oliveSoftContainer.copy(alpha = 0.45f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تغییر دستی پیشرفت", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            text = PersianCalendar.formatPercent(sliderProgress.toInt(), preferences.usePersianDigits),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.oliveAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = sliderProgress,
                        onValueChange = { sliderProgress = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = colors.oliveAccent, activeTrackColor = colors.oliveAccent)
                    )
                    OutlinedTextField(
                        value = progressNote,
                        onValueChange = { progressNote = it },
                        label = { Text("یادداشت این پیشرفت (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onUpdateProgress(sliderProgress.toInt(), progressNote)
                            progressNote = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("ثبت پیشرفت")
                    }
                }

                // Milestones
                Text("مراحل هدف (${PersianCalendar.formatNumber(milestones.size, preferences.usePersianDigits)})", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMilestoneTitle,
                        onValueChange = { newMilestoneTitle = it },
                        placeholder = { Text("افزودن مرحله جدید...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newMilestoneTitle.isNotBlank()) {
                                onAddMilestone(newMilestoneTitle.trim())
                                newMilestoneTitle = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "افزودن مرحله")
                    }
                }

                if (milestones.isEmpty()) {
                    Text("هنوز مرحله‌ای برای این هدف تعریف نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        milestones.forEach { ms ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.glassSurfaceSubtle.copy(alpha = 0.25f))
                                    .clickable { onToggleMilestone(ms) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (ms.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (ms.isCompleted) colors.success else colors.oliveAccent
                                    )
                                    Text(
                                        text = ms.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (ms.isCompleted) colors.textSecondary else colors.textPrimary,
                                        textDecoration = if (ms.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                                IconButton(onClick = { onDeleteMilestone(ms) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف مرحله", tint = colors.danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Progress History Log
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = colors.oliveAccent, modifier = Modifier.size(20.dp))
                    Text("تاریخچه پیشرفت", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }

                if (logs.isEmpty()) {
                    Text("تاریخچه‌ای ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logs.take(10).forEach { log ->
                            val cal = Calendar.getInstance().apply { timeInMillis = log.recordedAtMillis }
                            val dayEpoch = PersianCalendar.gregorianToEpochDay(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            val dateStr = PersianCalendar.formatShortDate(dayEpoch, preferences.calendarSystem, preferences.usePersianDigits)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.glassSurfaceSubtle.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.note.ifBlank { "ثبت پیشرفت" }, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                                Text(
                                    text = PersianCalendar.formatPercent(log.progressPercent, preferences.usePersianDigits),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.oliveAccent,
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
