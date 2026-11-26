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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.PersianCalendar
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.TaskEntity
import com.example.data.local.TaskStatus
import com.example.data.local.toProjectStatus
import com.example.data.preferences.UserPreferences
import com.example.ui.components.AnimatedGlassProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.ProjectEditorDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskEditorDialog
import com.example.ui.components.projectStatusColor
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsViewModel

@Composable
fun ProjectsScreen(
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    var statusFilter by remember { mutableStateOf<ProjectStatus?>(null) }
    var showCreateProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var detailProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var deletingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var addTaskForProjectId by remember { mutableStateOf<String?>(null) }

    val filteredProjects = remember(projects, statusFilter) {
        if (statusFilter == null) projects else projects.filter { it.status == statusFilter!!.name }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "مدیریت پروژه‌ها",
                subtitle = "${PersianCalendar.formatNumber(projects.size, preferences.usePersianDigits)} پروژه ثبت‌شده",
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
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            GlassFilterChip(
                                label = "همه",
                                selected = statusFilter == null,
                                onClick = { statusFilter = null }
                            )
                        }
                        items(ProjectStatus.entries) { st ->
                            GlassFilterChip(
                                label = st.persianLabel,
                                selected = statusFilter == st,
                                onClick = { statusFilter = if (statusFilter == st) null else st }
                            )
                        }
                    }
                }

                if (filteredProjects.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            icon = Icons.Rounded.FolderOpen,
                            title = "پروژه‌ای وجود ندارد",
                            description = "پروژه‌های شخصی یا کاری خود را ایجاد کنید و وظایف مرتبط را زیرمجموعه آن قرار دهید.",
                            actionLabel = "ایجاد پروژه جدید",
                            onAction = { showCreateProject = true }
                        )
                    }
                } else {
                    items(filteredProjects, key = { it.id }) { proj ->
                        val linkedTasks = tasks.filter { it.projectId == proj.id }
                        val doneLinked = linkedTasks.count { it.status == TaskStatus.COMPLETED.name }
                        val effectiveProgress = if (linkedTasks.isNotEmpty() && proj.progressPercent == 0) {
                            (doneLinked * 100) / linkedTasks.size
                        } else {
                            proj.progressPercent
                        }
                        val status = proj.status.toProjectStatus()

                        GlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(16.dp),
                            onClick = { detailProject = proj }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(proj.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (proj.description.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(proj.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingProject = proj }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { deletingProject = proj }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(label = status.persianLabel, tint = projectStatusColor(status))
                                Text(
                                    text = "وظایف: ${PersianCalendar.formatNumber(doneLinked, preferences.usePersianDigits)}/${PersianCalendar.formatNumber(linkedTasks.size, preferences.usePersianDigits)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("پیشرفت پروژه", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                Text(
                                    text = PersianCalendar.formatPercent(effectiveProgress, preferences.usePersianDigits),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.oliveAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            AnimatedGlassProgressBar(progress = effectiveProgress / 100f, height = 8.dp)

                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "شروع: ${PersianCalendar.formatShortDate(proj.startEpochDay, preferences.calendarSystem, preferences.usePersianDigits)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                                proj.deadlineEpochDay?.let { dl ->
                                    Text(
                                        text = "مهلت: ${PersianCalendar.formatShortDate(dl, preferences.calendarSystem, preferences.usePersianDigits)}",
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
            onClick = { showCreateProject = true },
            containerColor = colors.oliveAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("add_project_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "ایجاد پروژه")
        }
    }

    if (showCreateProject) {
        ProjectEditorDialog(
            preferences = preferences,
            onSave = {
                viewModel.saveProject(it)
                showCreateProject = false
            },
            onDismiss = { showCreateProject = false }
        )
    }

    editingProject?.let { p ->
        ProjectEditorDialog(
            initialProject = p,
            preferences = preferences,
            onSave = {
                viewModel.saveProject(it, "پروژه به‌روزرسانی شد.")
                editingProject = null
            },
            onDismiss = { editingProject = null }
        )
    }

    detailProject?.let { snapshot ->
        val liveProj = projects.firstOrNull { it.id == snapshot.id } ?: snapshot
        val linkedTasks = tasks.filter { it.projectId == liveProj.id }
        ProjectDetailDialog(
            project = liveProj,
            linkedTasks = linkedTasks,
            preferences = preferences,
            onToggleTask = { viewModel.toggleTaskCompleted(it) },
            onAddTask = { addTaskForProjectId = liveProj.id },
            onChangeStatus = { newStatus ->
                viewModel.saveProject(liveProj.copy(status = newStatus.name), "وضعیت پروژه تغییر کرد.")
            },
            onDismiss = { detailProject = null }
        )
    }

    addTaskForProjectId?.let { projId ->
        TaskEditorDialog(
            defaultProjectId = projId,
            categories = categories,
            projects = projects,
            preferences = preferences,
            onSave = {
                viewModel.saveTask(it, "وظیفه به پروژه اضافه شد.")
                addTaskForProjectId = null
            },
            onDismiss = { addTaskForProjectId = null }
        )
    }

    deletingProject?.let { p ->
        ConfirmDeleteDialog(
            title = "حذف پروژه",
            message = "آیا از حذف پروژه «${p.name}» مطمئن هستید؟ وظایف مرتبط حفظ می‌شوند و از پروژه جدا خواهند شد.",
            onConfirm = {
                viewModel.deleteProject(p.id)
                deletingProject = null
            },
            onDismiss = { deletingProject = null }
        )
    }
}

@Composable
private fun ProjectDetailDialog(
    project: ProjectEntity,
    linkedTasks: List<TaskEntity>,
    preferences: UserPreferences,
    onToggleTask: (TaskEntity) -> Unit,
    onAddTask: () -> Unit,
    onChangeStatus: (ProjectStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LifeOsTheme.colors
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
                Text(project.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                if (project.description.isNotBlank()) {
                    Text(project.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }

                Text("تغییر وضعیت پروژه", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProjectStatus.entries) { st ->
                        GlassFilterChip(
                            label = st.persianLabel,
                            selected = project.status == st.name,
                            onClick = { onChangeStatus(st) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وظایف پروژه (${PersianCalendar.formatNumber(linkedTasks.size, preferences.usePersianDigits)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Button(
                        onClick = onAddTask,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("افزودن وظیفه")
                    }
                }

                if (linkedTasks.isEmpty()) {
                    Text("هنوز وظیفه‌ای به این پروژه متصل نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        linkedTasks.forEach { task ->
                            val isDone = task.status == TaskStatus.COMPLETED.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.glassSurfaceSubtle.copy(alpha = 0.25f))
                                    .clickable { onToggleTask(task) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isDone) colors.success else colors.oliveAccent
                                )
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDone) colors.textSecondary else colors.textPrimary,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }

                if (project.notes.isNotBlank()) {
                    Text("یادداشت‌های پروژه", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(project.notes, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}
