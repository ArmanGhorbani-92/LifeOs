package com.example.data.repository

import android.net.Uri
import com.example.core.backup.BackupManager
import com.example.core.backup.BackupParseResult
import com.example.core.backup.BackupPayload
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.PersianCalendar
import com.example.core.calendar.WeekStartDay
import com.example.core.notification.ReminderScheduler
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.GoalEntity
import com.example.data.local.GoalProgressLogEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.LifeOsDao
import com.example.data.local.LifeOsDatabase
import com.example.data.local.MilestoneEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskStatus
import com.example.data.local.TimeRecordEntity
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LifeOsRepository(
    private val dao: LifeOsDao,
    private val prefsRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager
) {
    val preferences: Flow<UserPreferences> = prefsRepository.preferencesFlow
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val projects: Flow<List<ProjectEntity>> = dao.observeProjects()
    val tasks: Flow<List<TaskEntity>> = dao.observeTasks()
    val goals: Flow<List<GoalEntity>> = dao.observeGoals()
    val milestones: Flow<List<MilestoneEntity>> = dao.observeMilestones()
    val goalProgressLogs: Flow<List<GoalProgressLogEntity>> = dao.observeGoalProgressLogs()
    val habits: Flow<List<HabitEntity>> = dao.observeHabits()
    val habitRecords: Flow<List<HabitRecordEntity>> = dao.observeHabitRecords()
    val calendarEvents: Flow<List<CalendarEventEntity>> = dao.observeCalendarEvents()
    val notes: Flow<List<NoteEntity>> = dao.observeNotes()
    val timeRecords: Flow<List<TimeRecordEntity>> = dao.observeTimeRecords()

    suspend fun ensureDefaultCategories() = withContext(Dispatchers.IO) {
        if (dao.getAllCategories().isEmpty()) {
            dao.upsertCategories(LifeOsDatabase.defaultCategories)
        }
    }

    // Categories & Tags
    suspend fun saveCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        dao.upsertCategory(category)
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCategory(id)
    }

    suspend fun saveTagsFromCsv(csv: String) = withContext(Dispatchers.IO) {
        val existingNames = dao.getAllTags().map { it.name }.toSet()
        csv.split(",", "،")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in existingNames }
            .distinct()
            .forEach { tagName ->
                dao.upsertTag(TagEntity(name = tagName))
            }
    }

    // Tasks
    suspend fun saveTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.upsertTask(task)
        if (task.tagsCsv.isNotBlank()) saveTagsFromCsv(task.tagsCsv)
        syncTaskReminder(task)
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        reminderScheduler.cancelReminder("task_start_$taskId")
        reminderScheduler.cancelReminder("task_due_$taskId")
        dao.deleteTask(taskId)
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) = withContext(Dispatchers.IO) {
        val isNowCompleted = task.status != TaskStatus.COMPLETED.name
        val updated = task.copy(
            status = if (isNowCompleted) TaskStatus.COMPLETED.name else TaskStatus.NOT_STARTED.name,
            completedAtMillis = if (isNowCompleted) System.currentTimeMillis() else null
        )
        dao.upsertTask(updated)
        if (isNowCompleted) {
            reminderScheduler.cancelReminder("task_start_${task.id}")
            reminderScheduler.cancelReminder("task_due_${task.id}")
            if (task.actualMinutes == 0 && task.estimatedMinutes > 0) {
                dao.upsertTimeRecord(
                    TimeRecordEntity(
                        taskId = task.id,
                        projectId = task.projectId,
                        categoryId = task.categoryId,
                        title = task.title,
                        epochDay = task.dueEpochDay ?: task.startEpochDay ?: PersianCalendar.todayEpochDay(),
                        startMinuteOfDay = task.startMinuteOfDay ?: PersianCalendar.currentMinuteOfDay(),
                        durationMinutes = task.estimatedMinutes
                    )
                )
            }
        } else {
            syncTaskReminder(updated)
        }
    }

    private suspend fun syncTaskReminder(task: TaskEntity) {
        val prefs = prefsRepository.preferencesFlow.first()
        reminderScheduler.cancelReminder("task_start_${task.id}")
        reminderScheduler.cancelReminder("task_due_${task.id}")
        if (!prefs.notificationsEnabled || !task.reminderEnabled || task.status == TaskStatus.COMPLETED.name || task.status == TaskStatus.CANCELLED.name) {
            return
        }
        val scheduleDay = task.startEpochDay ?: task.dueEpochDay
        if (prefs.taskRemindersEnabled && scheduleDay != null && task.startMinuteOfDay != null) {
            val trigger = PersianCalendar.epochDayAndMinuteToMillis(scheduleDay, task.startMinuteOfDay)
            reminderScheduler.scheduleReminder(
                uniqueKey = "task_start_${task.id}",
                title = "یادآوری وظیفه: ${task.title}",
                body = task.description.ifBlank { "زمان انجام این وظیفه فرا رسیده است." },
                triggerAtMillis = trigger
            )
        }
        if (prefs.dueRemindersEnabled && task.dueEpochDay != null) {
            val dueMinute = task.endMinuteOfDay ?: task.startMinuteOfDay ?: (9 * 60)
            val trigger = PersianCalendar.epochDayAndMinuteToMillis(task.dueEpochDay, dueMinute)
            reminderScheduler.scheduleReminder(
                uniqueKey = "task_due_${task.id}",
                title = "سررسید وظیفه: ${task.title}",
                body = "مهلت انجام این وظیفه امروز است.",
                triggerAtMillis = trigger
            )
        }
    }

    // Goals & Milestones
    suspend fun saveGoal(goal: GoalEntity, progressNote: String = "") = withContext(Dispatchers.IO) {
        val updated = goal.copy(
            progressPercent = goal.progressPercent.coerceIn(0, 100),
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertGoal(updated)
        dao.upsertGoalProgressLog(
            GoalProgressLogEntity(
                goalId = updated.id,
                progressPercent = updated.progressPercent,
                note = progressNote.ifBlank { "ثبت وضعیت پیشرفت" }
            )
        )
    }

    suspend fun deleteGoal(goalId: String) = withContext(Dispatchers.IO) {
        dao.deleteGoal(goalId)
    }

    suspend fun saveMilestone(milestone: MilestoneEntity) = withContext(Dispatchers.IO) {
        dao.upsertMilestone(milestone)
    }

    suspend fun deleteMilestone(milestoneId: String) = withContext(Dispatchers.IO) {
        dao.deleteMilestone(milestoneId)
    }

    // Habits
    suspend fun saveHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        dao.upsertHabit(habit)
        syncHabitReminder(habit)
    }

    suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        reminderScheduler.cancelReminder("habit_$habitId")
        dao.deleteHabit(habitId)
    }

    suspend fun setHabitCompletionForDay(habitId: String, epochDay: Long, completed: Boolean, note: String = "") =
        withContext(Dispatchers.IO) {
            if (completed) {
                dao.upsertHabitRecord(
                    HabitRecordEntity(
                        habitId = habitId,
                        epochDay = epochDay,
                        isCompleted = true,
                        completedAtMillis = System.currentTimeMillis(),
                        note = note
                    )
                )
            } else {
                dao.deleteHabitRecordForDay(habitId, epochDay)
            }
        }

    private suspend fun syncHabitReminder(habit: HabitEntity) {
        val prefs = prefsRepository.preferencesFlow.first()
        reminderScheduler.cancelReminder("habit_${habit.id}")
        if (!prefs.notificationsEnabled || !prefs.habitRemindersEnabled || habit.reminderMinuteOfDay == null) {
            return
        }
        val today = PersianCalendar.todayEpochDay()
        var trigger = PersianCalendar.epochDayAndMinuteToMillis(today, habit.reminderMinuteOfDay)
        if (trigger <= System.currentTimeMillis()) {
            trigger = PersianCalendar.epochDayAndMinuteToMillis(today + 1, habit.reminderMinuteOfDay)
        }
        reminderScheduler.scheduleReminder(
            uniqueKey = "habit_${habit.id}",
            title = "یادآوری عادت: ${habit.name}",
            body = habit.description.ifBlank { "زمان ثبت عادت روزانه شما رسیده است." },
            triggerAtMillis = trigger
        )
    }

    // Calendar Events
    suspend fun saveCalendarEvent(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        dao.upsertCalendarEvent(event)
        syncEventReminder(event)
    }

    suspend fun deleteCalendarEvent(eventId: String) = withContext(Dispatchers.IO) {
        reminderScheduler.cancelReminder("event_$eventId")
        dao.deleteCalendarEvent(eventId)
    }

    private suspend fun syncEventReminder(event: CalendarEventEntity) {
        val prefs = prefsRepository.preferencesFlow.first()
        reminderScheduler.cancelReminder("event_${event.id}")
        if (!prefs.notificationsEnabled || !prefs.eventRemindersEnabled || !event.reminderEnabled) return
        val minute = event.startMinuteOfDay ?: (9 * 60)
        val trigger = PersianCalendar.epochDayAndMinuteToMillis(event.epochDay, minute)
        reminderScheduler.scheduleReminder(
            uniqueKey = "event_${event.id}",
            title = "یادآوری رویداد: ${event.title}",
            body = event.location.ifBlank { event.description.ifBlank { "رویداد برنامه‌ریزی‌شده در تقویم LIFEOS" } },
            triggerAtMillis = trigger
        )
    }

    // Projects
    suspend fun saveProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        dao.upsertProject(project.copy(progressPercent = project.progressPercent.coerceIn(0, 100)))
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        dao.deleteProject(projectId)
    }

    // Notes
    suspend fun saveNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        dao.upsertNote(note.copy(updatedAtMillis = System.currentTimeMillis()))
        if (note.tagsCsv.isNotBlank()) saveTagsFromCsv(note.tagsCsv)
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        dao.deleteNote(noteId)
    }

    // Time Records
    suspend fun saveTimeRecord(record: TimeRecordEntity) = withContext(Dispatchers.IO) {
        dao.upsertTimeRecord(record)
    }

    suspend fun deleteTimeRecord(recordId: String) = withContext(Dispatchers.IO) {
        dao.deleteTimeRecord(recordId)
    }

    // Preferences & Reminders
    suspend fun setThemeMode(mode: ThemeMode) = prefsRepository.setThemeMode(mode)
    suspend fun setCalendarSystem(system: CalendarSystem) = prefsRepository.setCalendarSystem(system)
    suspend fun setWeekStartDay(day: WeekStartDay) = prefsRepository.setWeekStartDay(day)
    suspend fun setUse24HourClock(use24: Boolean) = prefsRepository.setUse24HourClock(use24)
    suspend fun setUsePersianDigits(usePersian: Boolean) = prefsRepository.setUsePersianDigits(usePersian)
    suspend fun setUserDisplayName(name: String) = prefsRepository.setUserDisplayName(name)

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefsRepository.setNotificationsEnabled(enabled)
        resyncAllReminders()
    }

    suspend fun setTaskRemindersEnabled(enabled: Boolean) {
        prefsRepository.setTaskRemindersEnabled(enabled)
        resyncAllReminders()
    }

    suspend fun setDueRemindersEnabled(enabled: Boolean) {
        prefsRepository.setDueRemindersEnabled(enabled)
        resyncAllReminders()
    }

    suspend fun setEventRemindersEnabled(enabled: Boolean) {
        prefsRepository.setEventRemindersEnabled(enabled)
        resyncAllReminders()
    }

    suspend fun setHabitRemindersEnabled(enabled: Boolean) {
        prefsRepository.setHabitRemindersEnabled(enabled)
        resyncAllReminders()
    }

    fun hasNotificationPermission(): Boolean = reminderScheduler.hasNotificationPermission()

    private suspend fun resyncAllReminders() = withContext(Dispatchers.IO) {
        dao.getAllTasks().forEach { syncTaskReminder(it) }
        dao.getAllCalendarEvents().forEach { syncEventReminder(it) }
        dao.getAllHabits().forEach { syncHabitReminder(it) }
    }

    // Backup & Restore & Wipe
    suspend fun exportBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val prefs = prefsRepository.preferencesFlow.first()
        backupManager.exportToUri(uri, dao, prefs)
    }

    suspend fun parseBackup(uri: Uri): BackupParseResult = withContext(Dispatchers.IO) {
        backupManager.readAndValidate(uri)
    }

    suspend fun restoreBackup(payload: BackupPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val categoriesToSave = payload.categories.ifEmpty { LifeOsDatabase.defaultCategories }
            dao.replaceAllDataAtomically(
                categories = categoriesToSave,
                tags = payload.tags,
                projects = payload.projects,
                tasks = payload.tasks,
                goals = payload.goals,
                milestones = payload.milestones,
                goalProgressLogs = payload.goalProgressLogs,
                habits = payload.habits,
                habitRecords = payload.habitRecords,
                calendarEvents = payload.calendarEvents,
                notes = payload.notes,
                timeRecords = payload.timeRecords
            )
            payload.preferences?.let { prefsRepository.restorePreferences(it) }
            resyncAllReminders()
        }
    }

    suspend fun wipeAllData() = withContext(Dispatchers.IO) {
        dao.getAllTasks().forEach {
            reminderScheduler.cancelReminder("task_start_${it.id}")
            reminderScheduler.cancelReminder("task_due_${it.id}")
        }
        dao.getAllCalendarEvents().forEach {
            reminderScheduler.cancelReminder("event_${it.id}")
        }
        dao.getAllHabits().forEach {
            reminderScheduler.cancelReminder("habit_${it.id}")
        }
        dao.clearAllUserData()
        dao.upsertCategories(LifeOsDatabase.defaultCategories)
    }
}
