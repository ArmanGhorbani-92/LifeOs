package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.backup.BackupManager
import com.example.core.backup.BackupParseResult
import com.example.core.backup.BackupPayload
import com.example.core.backup.BackupPreview
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.WeekStartDay
import com.example.core.notification.ReminderScheduler
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.GoalEntity
import com.example.data.local.GoalProgressLogEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.LifeOsDatabase
import com.example.data.local.MilestoneEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TimeRecordEntity
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.LifeOsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LifeOsFeedbackMessage(
    val text: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class LifeOsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LifeOsDatabase.getInstance(application)
    private val prefsRepo = UserPreferencesRepository(application)
    private val reminderScheduler = ReminderScheduler(application)
    private val backupManager = BackupManager(application)

    private val repository = LifeOsRepository(
        dao = database.lifeOsDao(),
        prefsRepository = prefsRepo,
        reminderScheduler = reminderScheduler,
        backupManager = backupManager
    )

    val preferences: StateFlow<UserPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = repository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val milestones: StateFlow<List<MilestoneEntity>> = repository.milestones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalProgressLogs: StateFlow<List<GoalProgressLogEntity>> = repository.goalProgressLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.habits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitRecords: StateFlow<List<HabitRecordEntity>> = repository.habitRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEventEntity>> = repository.calendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeRecords: StateFlow<List<TimeRecordEntity>> = repository.timeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _feedback = MutableStateFlow<LifeOsFeedbackMessage?>(null)
    val feedback: StateFlow<LifeOsFeedbackMessage?> = _feedback.asStateFlow()

    private val _pendingRestorePreview = MutableStateFlow<BackupPreview?>(null)
    val pendingRestorePreview: StateFlow<BackupPreview?> = _pendingRestorePreview.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun postFeedback(message: String, isError: Boolean = false) {
        _feedback.value = LifeOsFeedbackMessage(text = message, isError = isError)
    }

    // Tasks
    fun saveTask(task: TaskEntity, successMessage: String = "وظیفه با موفقیت ذخیره شد.") {
        viewModelScope.launch {
            runCatching { repository.saveTask(task) }
                .onSuccess { postFeedback(successMessage) }
                .onFailure { postFeedback("خطا در ذخیره وظیفه رخ داد.", isError = true) }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTask(taskId) }
                .onSuccess { postFeedback("وظیفه حذف شد.") }
                .onFailure { postFeedback("خطا در حذف وظیفه رخ داد.", isError = true) }
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            runCatching { repository.toggleTaskCompleted(task) }
                .onFailure { postFeedback("خطا در به‌روزرسانی وضعیت وظیفه.", isError = true) }
        }
    }

    // Goals & Milestones
    fun saveGoal(goal: GoalEntity, progressNote: String = "", successMessage: String = "هدف با موفقیت ذخیره شد.") {
        viewModelScope.launch {
            runCatching { repository.saveGoal(goal, progressNote) }
                .onSuccess { postFeedback(successMessage) }
                .onFailure { postFeedback("خطا در ذخیره هدف رخ داد.", isError = true) }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteGoal(goalId) }
                .onSuccess { postFeedback("هدف حذف شد.") }
                .onFailure { postFeedback("خطا در حذف هدف رخ داد.", isError = true) }
        }
    }

    fun saveMilestone(milestone: MilestoneEntity) {
        viewModelScope.launch {
            runCatching { repository.saveMilestone(milestone) }
                .onFailure { postFeedback("خطا در ذخیره مرحله هدف.", isError = true) }
        }
    }

    fun deleteMilestone(milestoneId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMilestone(milestoneId) }
                .onFailure { postFeedback("خطا در حذف مرحله هدف.", isError = true) }
        }
    }

    // Habits
    fun saveHabit(habit: HabitEntity, successMessage: String = "عادت با موفقیت ذخیره شد.") {
        viewModelScope.launch {
            runCatching { repository.saveHabit(habit) }
                .onSuccess { postFeedback(successMessage) }
                .onFailure { postFeedback("خطا در ذخیره عادت رخ داد.", isError = true) }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteHabit(habitId) }
                .onSuccess { postFeedback("عادت حذف شد.") }
                .onFailure { postFeedback("خطا در حذف عادت رخ داد.", isError = true) }
        }
    }

    fun toggleHabitForDay(habitId: String, epochDay: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.setHabitCompletionForDay(
                    habitId = habitId,
                    epochDay = epochDay,
                    completed = !currentlyCompleted
                )
            }.onFailure {
                postFeedback("خطا در ثبت وضعیت عادت.", isError = true)
            }
        }
    }

    // Calendar Events
    fun saveCalendarEvent(event: CalendarEventEntity, successMessage: String = "رویداد با موفقیت ذخیره شد.") {
        viewModelScope.launch {
            runCatching { repository.saveCalendarEvent(event) }
                .onSuccess { postFeedback(successMessage) }
                .onFailure { postFeedback("خطا در ذخیره رویداد رخ داد.", isError = true) }
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteCalendarEvent(eventId) }
                .onSuccess { postFeedback("رویداد حذف شد.") }
                .onFailure { postFeedback("خطا در حذف رویداد رخ داد.", isError = true) }
        }
    }

    // Projects
    fun saveProject(project: ProjectEntity, successMessage: String = "پروژه با موفقیت ذخیره شد.") {
        viewModelScope.launch {
            runCatching { repository.saveProject(project) }
                .onSuccess { postFeedback(successMessage) }
                .onFailure { postFeedback("خطا در ذخیره پروژه رخ داد.", isError = true) }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteProject(projectId) }
                .onSuccess { postFeedback("پروژه حذف شد.") }
                .onFailure { postFeedback("خطا در حذف پروژه رخ داد.", isError = true) }
        }
    }

    // Notes
    fun saveNote(note: NoteEntity, successMessage: String = "یادداشت ذخیره شد.") {
        viewModelScope.launch {
            runCatching { repository.saveNote(note) }
                .onSuccess { postFeedback(successMessage) }
                .onFailure { postFeedback("خطا در ذخیره یادداشت رخ داد.", isError = true) }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteNote(noteId) }
                .onSuccess { postFeedback("یادداشت حذف شد.") }
                .onFailure { postFeedback("خطا در حذف یادداشت رخ داد.", isError = true) }
        }
    }

    // Categories & Time Records
    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            runCatching { repository.saveCategory(category) }
                .onSuccess { postFeedback("دسته‌بندی ذخیره شد.") }
        }
    }

    fun saveTimeRecord(record: TimeRecordEntity) {
        viewModelScope.launch {
            runCatching { repository.saveTimeRecord(record) }
                .onSuccess { postFeedback("زمان فعالیت ثبت شد.") }
                .onFailure { postFeedback("خطا در ثبت زمان.", isError = true) }
        }
    }

    fun deleteTimeRecord(recordId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTimeRecord(recordId) }
        }
    }

    // Preferences
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setCalendarSystem(system: CalendarSystem) = viewModelScope.launch { repository.setCalendarSystem(system) }
    fun setWeekStartDay(day: WeekStartDay) = viewModelScope.launch { repository.setWeekStartDay(day) }
    fun setUse24HourClock(use24: Boolean) = viewModelScope.launch { repository.setUse24HourClock(use24) }
    fun setUsePersianDigits(usePersian: Boolean) = viewModelScope.launch { repository.setUsePersianDigits(usePersian) }
    fun setUserDisplayName(name: String) = viewModelScope.launch { repository.setUserDisplayName(name) }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
            postFeedback(if (enabled) "اعلان‌های محلی فعال شدند." else "اعلان‌های محلی غیرفعال شدند.")
        }
    }

    fun setTaskRemindersEnabled(enabled: Boolean) = viewModelScope.launch { repository.setTaskRemindersEnabled(enabled) }
    fun setDueRemindersEnabled(enabled: Boolean) = viewModelScope.launch { repository.setDueRemindersEnabled(enabled) }
    fun setEventRemindersEnabled(enabled: Boolean) = viewModelScope.launch { repository.setEventRemindersEnabled(enabled) }
    fun setHabitRemindersEnabled(enabled: Boolean) = viewModelScope.launch { repository.setHabitRemindersEnabled(enabled) }
    fun hasNotificationPermission(): Boolean = repository.hasNotificationPermission()

    // Backup & Restore & Wipe
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            repository.exportBackup(uri)
                .onSuccess { postFeedback("فایل پشتیبان JSON با موفقیت ذخیره شد.") }
                .onFailure { postFeedback("خطا در تهیه فایل پشتیبان رخ داد.", isError = true) }
        }
    }

    fun inspectBackupFile(uri: Uri) {
        viewModelScope.launch {
            when (val result = repository.parseBackup(uri)) {
                is BackupParseResult.Valid -> _pendingRestorePreview.value = result.preview
                is BackupParseResult.Invalid -> postFeedback(result.messageFa, isError = true)
            }
        }
    }

    fun dismissRestorePreview() {
        _pendingRestorePreview.value = null
    }

    fun confirmRestoreBackup(payload: BackupPayload) {
        viewModelScope.launch {
            _pendingRestorePreview.value = null
            repository.restoreBackup(payload)
                .onSuccess { postFeedback("بازیابی اطلاعات با موفقیت انجام شد.") }
                .onFailure { postFeedback("خطا در بازیابی اطلاعات. داده‌های قبلی حفظ شدند.", isError = true) }
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            runCatching { repository.wipeAllData() }
                .onSuccess { postFeedback("تمام اطلاعات کاربری با موفقیت پاک شدند.") }
                .onFailure { postFeedback("خطا در پاک‌سازی اطلاعات رخ داد.", isError = true) }
        }
    }
}
