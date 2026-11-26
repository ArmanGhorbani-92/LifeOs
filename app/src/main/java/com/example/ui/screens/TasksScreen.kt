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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.PersianCalendar
import com.example.data.local.CategoryEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.toTaskPriority
import com.example.data.local.toTaskStatus
import com.example.data.preferences.UserPreferences
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskEditorDialog
import com.example.ui.components.taskStatusColor
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsCalculators
import com.example.ui.viewmodel.LifeOsViewModel

private enum class TaskTimeScope(val label: String) {
    ALL("همه"),
    TODAY("امروز"),
    UPCOMING("آینده"),
    OVERDUE("عقب‌افتاده")
}

private enum class TaskViewMode(val label: String) {
    LIST("فهرست"),
    CARD("کارت"),
    GROUPED("گروه‌بندی وضعیت")
}

private enum class TaskSortOption(val label: String) {
    DUE_DATE("سررسید"),
    PRIORITY("اولویت"),
    NEWEST("جدیدترین")
}

@Composable
fun TasksScreen(viewModel: LifeOsViewModel) {
    val colors = LifeOsTheme.colors
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val today = remember { PersianCalendar.todayEpochDay() }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var timeScope by remember { mutableStateOf(TaskTimeScope.ALL) }
    var selectedPriority by remember { mutableStateOf<TaskPriority?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(TaskViewMode.CARD) }
    var sortOption by remember { mutableStateOf(TaskSortOption.DUE_DATE) }

    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val filteredTasks = remember(tasks, searchQuery, timeScope, selectedPriority, selectedCategoryId, sortOption, today) {
        tasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true) ||
                task.tagsCsv.contains(searchQuery, ignoreCase = true)
            val matchesScope = when (timeScope) {
                TaskTimeScope.ALL -> true
                TaskTimeScope.TODAY -> LifeOsCalculators.isTaskForDay(task, today)
                TaskTimeScope.UPCOMING -> (task.dueEpochDay ?: task.startEpochDay ?: Long.MIN_VALUE) > today && task.status != TaskStatus.COMPLETED.name
                TaskTimeScope.OVERDUE -> LifeOsCalculators.isTaskOverdue(task, today)
            }
            val matchesPriority = selectedPriority == null || task.priority == selectedPriority!!.name
            val matchesCategory = selectedCategoryId == null || task.categoryId == selectedCategoryId
            matchesQuery && matchesScope && matchesPriority && matchesCategory
        }.let { list ->
            when (sortOption) {
                TaskSortOption.DUE_DATE -> list.sortedWith(compareBy<TaskEntity> { it.status == TaskStatus.COMPLETED.name }.thenBy { it.dueEpochDay ?: Long.MAX_VALUE })
                TaskSortOption.PRIORITY -> list.sortedWith(compareByDescending<TaskEntity> { it.priority.toTaskPriority().sortRank }.thenBy { it.dueEpochDay ?: Long.MAX_VALUE })
                TaskSortOption.NEWEST -> list.sortedByDescending { it.createdAtMillis }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "مدیریت وظایف",
                subtitle = "${PersianCalendar.formatNumber(filteredTasks.size, preferences.usePersianDigits)} وظیفه نمایش داده‌شده",
                trailingAction = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                sortOption = when (sortOption) {
                                    TaskSortOption.DUE_DATE -> TaskSortOption.PRIORITY
                                    TaskSortOption.PRIORITY -> TaskSortOption.NEWEST
                                    TaskSortOption.NEWEST -> TaskSortOption.DUE_DATE
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "مرتب‌سازی", tint = colors.oliveAccent)
                        }
                        IconButton(
                            onClick = {
                                viewMode = when (viewMode) {
                                    TaskViewMode.LIST -> TaskViewMode.CARD
                                    TaskViewMode.CARD -> TaskViewMode.GROUPED
                                    TaskViewMode.GROUPED -> TaskViewMode.LIST
                                }
                            }
                        ) {
                            Icon(
                                imageVector = when (viewMode) {
                                    TaskViewMode.LIST -> Icons.Rounded.ViewList
                                    TaskViewMode.CARD -> Icons.Rounded.ViewAgenda
                                    TaskViewMode.GROUPED -> Icons.Rounded.GridView
                                },
                                contentDescription = "تغییر حالت نمایش",
                                tint = colors.oliveAccent
                            )
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search + Mode/Sort indicator
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جست‌وجو در عنوان، توضیحات یا برچسب...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_search_input")
                    )
                }

                // Scope filter chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TaskTimeScope.entries) { scope ->
                            GlassFilterChip(
                                label = scope.label,
                                selected = timeScope == scope,
                                onClick = { timeScope = scope }
                            )
                        }
                    }
                }

                // View mode + Sort + Priority/Category filters
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            GlassFilterChip(
                                label = "نمایش: ${viewMode.label}",
                                selected = true,
                                onClick = {
                                    viewMode = when (viewMode) {
                                        TaskViewMode.LIST -> TaskViewMode.CARD
                                        TaskViewMode.CARD -> TaskViewMode.GROUPED
                                        TaskViewMode.GROUPED -> TaskViewMode.LIST
                                    }
                                }
                            )
                        }
                        item {
                            GlassFilterChip(
                                label = "مرتب‌سازی: ${sortOption.label}",
                                selected = false,
                                onClick = {
                                    sortOption = when (sortOption) {
                                        TaskSortOption.DUE_DATE -> TaskSortOption.PRIORITY
                                        TaskSortOption.PRIORITY -> TaskSortOption.NEWEST
                                        TaskSortOption.NEWEST -> TaskSortOption.DUE_DATE
                                    }
                                }
                            )
                        }
                        items(TaskPriority.entries) { p ->
                            GlassFilterChip(
                                label = "اولویت ${p.persianLabel}",
                                selected = selectedPriority == p,
                                onClick = { selectedPriority = if (selectedPriority == p) null else p }
                            )
                        }
                        items(categories) { cat ->
                            GlassFilterChip(
                                label = cat.name,
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id }
                            )
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            icon = Icons.Rounded.TaskAlt,
                            title = "وظیفه‌ای یافت نشد",
                            description = "هنوز وظیفه‌ای با این فیلتر وجود ندارد. اولین وظیفه خود را ایجاد کنید.",
                            actionLabel = "افزودن وظیفه",
                            onAction = { showCreateDialog = true }
                        )
                    }
                } else {
                    when (viewMode) {
                        TaskViewMode.LIST -> {
                            items(filteredTasks, key = { it.id }) { task ->
                                TaskCompactRow(
                                    task = task,
                                    today = today,
                                    preferences = preferences,
                                    onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                                    onEdit = { editingTask = task },
                                    onDelete = { deletingTask = task }
                                )
                            }
                        }
                        TaskViewMode.CARD -> {
                            items(filteredTasks, key = { it.id }) { task ->
                                TaskDetailedCard(
                                    task = task,
                                    category = task.categoryId?.let { categoryMap[it] },
                                    today = today,
                                    preferences = preferences,
                                    onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                                    onEdit = { editingTask = task },
                                    onDelete = { deletingTask = task }
                                )
                            }
                        }
                        TaskViewMode.GROUPED -> {
                            TaskStatus.entries.forEach { st ->
                                val group = filteredTasks.filter { it.status == st.name }
                                if (group.isNotEmpty()) {
                                    item(key = "header_${st.name}") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            StatusBadge(label = st.persianLabel, tint = taskStatusColor(st))
                                            Text(
                                                text = "(${PersianCalendar.formatNumber(group.size, preferences.usePersianDigits)})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    items(group, key = { it.id }) { task ->
                                        TaskDetailedCard(
                                            task = task,
                                            category = task.categoryId?.let { categoryMap[it] },
                                            today = today,
                                            preferences = preferences,
                                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                                            onEdit = { editingTask = task },
                                            onDelete = { deletingTask = task }
                                        )
                                    }
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
                .testTag("add_task_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "افزودن وظیفه")
        }
    }

    if (showCreateDialog) {
        TaskEditorDialog(
            categories = categories,
            projects = projects,
            preferences = preferences,
            onSave = {
                viewModel.saveTask(it)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingTask?.let { taskToEdit ->
        TaskEditorDialog(
            initialTask = taskToEdit,
            categories = categories,
            projects = projects,
            preferences = preferences,
            onSave = {
                viewModel.saveTask(it, "تغییرات وظیفه ذخیره شد.")
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }

    deletingTask?.let { taskToDelete ->
        ConfirmDeleteDialog(
            title = "حذف وظیفه",
            message = "آیا از حذف وظیفه «${taskToDelete.title}» مطمئن هستید؟ این عملیات قابل بازگشت نیست.",
            onConfirm = {
                viewModel.deleteTask(taskToDelete.id)
                deletingTask = null
            },
            onDismiss = { deletingTask = null }
        )
    }
}

@Composable
private fun TaskCompactRow(
    task: TaskEntity,
    today: Long,
    preferences: UserPreferences,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val isDone = task.status == TaskStatus.COMPLETED.name
    val isOverdue = LifeOsCalculators.isTaskOverdue(task, today)

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "تغییر وضعیت",
                    tint = if (isDone) colors.success else colors.oliveAccent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) colors.textSecondary else colors.textPrimary,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                task.dueEpochDay?.let { due ->
                    Text(
                        text = PersianCalendar.formatShortDate(due, preferences.calendarSystem, preferences.usePersianDigits),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) colors.danger else colors.textSecondary
                    )
                }
            }
            PriorityBadge(priority = task.priority.toTaskPriority())
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger)
            }
        }
    }
}

@Composable
private fun TaskDetailedCard(
    task: TaskEntity,
    category: CategoryEntity?,
    today: Long,
    preferences: UserPreferences,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LifeOsTheme.colors
    val status = task.status.toTaskStatus()
    val priority = task.priority.toTaskPriority()
    val isDone = status == TaskStatus.COMPLETED
    val isOverdue = LifeOsCalculators.isTaskOverdue(task, today)

    GlassCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onToggleComplete) {
                    Icon(
                        imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "تکمیل وظیفه",
                        tint = if (isDone) colors.success else colors.oliveAccent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDone) colors.textSecondary else colors.textPrimary,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriorityBadge(priority = priority)
            StatusBadge(label = status.persianLabel, tint = taskStatusColor(status))
            if (category != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.oliveSoftContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(category.name, style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                }
            }
            if (isOverdue) {
                StatusBadge(label = "عقب‌افتاده", tint = colors.danger)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val dueText = task.dueEpochDay?.let {
                "سررسید: ${PersianCalendar.formatShortDate(it, preferences.calendarSystem, preferences.usePersianDigits)}"
            } ?: "بدون سررسید"
            Text(
                text = dueText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOverdue) colors.danger else colors.textSecondary
            )
            if (task.startMinuteOfDay != null) {
                Text(
                    text = "ساعت: ${PersianCalendar.formatTime(task.startMinuteOfDay, preferences.use24HourClock, preferences.usePersianDigits)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.oliveAccent
                )
            } else if (task.estimatedMinutes > 0) {
                Text(
                    text = "تخمین: ${PersianCalendar.formatDuration(task.estimatedMinutes, preferences.usePersianDigits)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
