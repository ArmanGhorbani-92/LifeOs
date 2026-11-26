package com.example.core.backup

import android.content.Context
import android.net.Uri
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.WeekStartDay
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.GoalEntity
import com.example.data.local.GoalProgressLogEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.LifeOsDao
import com.example.data.local.MilestoneEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TimeRecordEntity
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val categories: List<CategoryEntity>,
    val tags: List<TagEntity>,
    val projects: List<ProjectEntity>,
    val tasks: List<TaskEntity>,
    val goals: List<GoalEntity>,
    val milestones: List<MilestoneEntity>,
    val goalProgressLogs: List<GoalProgressLogEntity>,
    val habits: List<HabitEntity>,
    val habitRecords: List<HabitRecordEntity>,
    val calendarEvents: List<CalendarEventEntity>,
    val notes: List<NoteEntity>,
    val timeRecords: List<TimeRecordEntity>,
    val preferences: UserPreferences?
)

data class BackupPreview(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val taskCount: Int,
    val goalCount: Int,
    val habitCount: Int,
    val eventCount: Int,
    val projectCount: Int,
    val noteCount: Int,
    val timeRecordCount: Int,
    val payload: BackupPayload
)

sealed interface BackupParseResult {
    data class Valid(val preview: BackupPreview) : BackupParseResult
    data class Invalid(val messageFa: String) : BackupParseResult
}

class BackupManager(private val context: Context) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        private const val APP_IDENTIFIER = "LIFEOS"
    }

    suspend fun exportToUri(
        uri: Uri,
        dao: LifeOsDao,
        preferences: UserPreferences
    ): Result<Unit> = runCatching {
        val root = JSONObject().apply {
            put("app", APP_IDENTIFIER)
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())

            put("categories", JSONArray().apply {
                dao.getAllCategories().forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("iconName", c.iconName)
                        put("colorHex", c.colorHex)
                        put("scope", c.scope)
                    })
                }
            })

            put("tags", JSONArray().apply {
                dao.getAllTags().forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                        put("createdAtMillis", t.createdAtMillis)
                    })
                }
            })

            put("projects", JSONArray().apply {
                dao.getAllProjects().forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description)
                        put("startEpochDay", p.startEpochDay)
                        putNullableLong("deadlineEpochDay", p.deadlineEpochDay)
                        put("status", p.status)
                        put("progressPercent", p.progressPercent)
                        put("notes", p.notes)
                        put("createdAtMillis", p.createdAtMillis)
                    })
                }
            })

            put("tasks", JSONArray().apply {
                dao.getAllTasks().forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("description", t.description)
                        putNullableString("categoryId", t.categoryId)
                        putNullableString("projectId", t.projectId)
                        put("priority", t.priority)
                        put("status", t.status)
                        put("createdAtMillis", t.createdAtMillis)
                        putNullableLong("startEpochDay", t.startEpochDay)
                        putNullableLong("dueEpochDay", t.dueEpochDay)
                        putNullableInt("startMinuteOfDay", t.startMinuteOfDay)
                        putNullableInt("endMinuteOfDay", t.endMinuteOfDay)
                        put("estimatedMinutes", t.estimatedMinutes)
                        put("actualMinutes", t.actualMinutes)
                        put("tagsCsv", t.tagsCsv)
                        put("note", t.note)
                        put("reminderEnabled", t.reminderEnabled)
                        putNullableLong("completedAtMillis", t.completedAtMillis)
                    })
                }
            })

            put("goals", JSONArray().apply {
                dao.getAllGoals().forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("title", g.title)
                        put("description", g.description)
                        putNullableString("categoryId", g.categoryId)
                        put("startEpochDay", g.startEpochDay)
                        putNullableLong("endEpochDay", g.endEpochDay)
                        put("progressPercent", g.progressPercent)
                        put("status", g.status)
                        put("notes", g.notes)
                        put("createdAtMillis", g.createdAtMillis)
                        put("updatedAtMillis", g.updatedAtMillis)
                    })
                }
            })

            put("milestones", JSONArray().apply {
                dao.getAllMilestones().forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("goalId", m.goalId)
                        put("title", m.title)
                        put("isCompleted", m.isCompleted)
                        put("sortOrder", m.sortOrder)
                        putNullableLong("completedAtMillis", m.completedAtMillis)
                    })
                }
            })

            put("goalProgressLogs", JSONArray().apply {
                dao.getAllGoalProgressLogs().forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id)
                        put("goalId", l.goalId)
                        put("progressPercent", l.progressPercent)
                        put("note", l.note)
                        put("recordedAtMillis", l.recordedAtMillis)
                    })
                }
            })

            put("habits", JSONArray().apply {
                dao.getAllHabits().forEach { h ->
                    put(JSONObject().apply {
                        put("id", h.id)
                        put("name", h.name)
                        put("description", h.description)
                        putNullableString("categoryId", h.categoryId)
                        put("startEpochDay", h.startEpochDay)
                        put("frequencyType", h.frequencyType)
                        put("weeklyTarget", h.weeklyTarget)
                        put("selectedDaysMask", h.selectedDaysMask)
                        put("note", h.note)
                        putNullableInt("reminderMinuteOfDay", h.reminderMinuteOfDay)
                        put("createdAtMillis", h.createdAtMillis)
                    })
                }
            })

            put("habitRecords", JSONArray().apply {
                dao.getAllHabitRecords().forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("habitId", r.habitId)
                        put("epochDay", r.epochDay)
                        put("isCompleted", r.isCompleted)
                        put("completedAtMillis", r.completedAtMillis)
                        put("note", r.note)
                    })
                }
            })

            put("calendarEvents", JSONArray().apply {
                dao.getAllCalendarEvents().forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("title", e.title)
                        put("description", e.description)
                        put("epochDay", e.epochDay)
                        putNullableInt("startMinuteOfDay", e.startMinuteOfDay)
                        putNullableInt("endMinuteOfDay", e.endMinuteOfDay)
                        put("location", e.location)
                        putNullableString("categoryId", e.categoryId)
                        put("isAllDay", e.isAllDay)
                        put("reminderEnabled", e.reminderEnabled)
                        put("createdAtMillis", e.createdAtMillis)
                    })
                }
            })

            put("notes", JSONArray().apply {
                dao.getAllNotes().forEach { n ->
                    put(JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("content", n.content)
                        putNullableString("categoryId", n.categoryId)
                        put("tagsCsv", n.tagsCsv)
                        put("isPinned", n.isPinned)
                        put("createdAtMillis", n.createdAtMillis)
                        put("updatedAtMillis", n.updatedAtMillis)
                    })
                }
            })

            put("timeRecords", JSONArray().apply {
                dao.getAllTimeRecords().forEach { tr ->
                    put(JSONObject().apply {
                        put("id", tr.id)
                        putNullableString("taskId", tr.taskId)
                        putNullableString("projectId", tr.projectId)
                        putNullableString("categoryId", tr.categoryId)
                        put("title", tr.title)
                        put("epochDay", tr.epochDay)
                        put("startMinuteOfDay", tr.startMinuteOfDay)
                        put("durationMinutes", tr.durationMinutes)
                        put("createdAtMillis", tr.createdAtMillis)
                    })
                }
            })

            put("preferences", JSONObject().apply {
                put("themeMode", preferences.themeMode.name)
                put("calendarSystem", preferences.calendarSystem.name)
                put("weekStartDay", preferences.weekStartDay.name)
                put("use24HourClock", preferences.use24HourClock)
                put("usePersianDigits", preferences.usePersianDigits)
                put("notificationsEnabled", preferences.notificationsEnabled)
                put("taskRemindersEnabled", preferences.taskRemindersEnabled)
                put("dueRemindersEnabled", preferences.dueRemindersEnabled)
                put("eventRemindersEnabled", preferences.eventRemindersEnabled)
                put("habitRemindersEnabled", preferences.habitRemindersEnabled)
                put("userDisplayName", preferences.userDisplayName)
            })
        }

        val jsonText = root.toString(2)
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: error("امکان نوشتن در فایل انتخاب‌شده وجود ندارد.")
        outputStream.use { it.write(jsonText.toByteArray(Charsets.UTF_8)) }
    }

    fun readAndValidate(uri: Uri): BackupParseResult {
        val rawText = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull() ?: return BackupParseResult.Invalid("امکان خواندن فایل پشتیبان وجود ندارد.")

        if (rawText.isBlank()) {
            return BackupParseResult.Invalid("فایل انتخاب‌شده خالی است.")
        }

        val root = runCatching { JSONObject(rawText) }.getOrNull()
            ?: return BackupParseResult.Invalid("ساختار JSON فایل معتبر نیست.")

        if (root.optString("app") != APP_IDENTIFIER) {
            return BackupParseResult.Invalid("این فایل متعلق به اپلیکیشن LIFEOS نیست.")
        }

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion !in 1..CURRENT_SCHEMA_VERSION) {
            return BackupParseResult.Invalid("نسخه فایل پشتیبان پشتیبانی نمی‌شود.")
        }

        val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

        return runCatching {
            val categories = root.getJSONArrayOrEmpty("categories").mapObjects { o ->
                CategoryEntity(
                    id = o.requireNonBlank("id"),
                    name = o.requireNonBlank("name"),
                    iconName = o.optString("iconName", "folder"),
                    colorHex = o.optString("colorHex", "#71805A"),
                    scope = o.optString("scope", "ALL")
                )
            }
            val categoryIds = categories.map { it.id }.toSet()

            val tags = root.getJSONArrayOrEmpty("tags").mapObjects { o ->
                TagEntity(
                    id = o.requireNonBlank("id"),
                    name = o.requireNonBlank("name"),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }

            val projects = root.getJSONArrayOrEmpty("projects").mapObjects { o ->
                ProjectEntity(
                    id = o.requireNonBlank("id"),
                    name = o.requireNonBlank("name"),
                    description = o.optString("description", ""),
                    startEpochDay = o.getLong("startEpochDay"),
                    deadlineEpochDay = o.optNullableLong("deadlineEpochDay"),
                    status = o.optString("status", "ACTIVE"),
                    progressPercent = o.optInt("progressPercent", 0).coerceIn(0, 100),
                    notes = o.optString("notes", ""),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }
            val projectIds = projects.map { it.id }.toSet()

            val tasks = root.getJSONArrayOrEmpty("tasks").mapObjects { o ->
                TaskEntity(
                    id = o.requireNonBlank("id"),
                    title = o.requireNonBlank("title"),
                    description = o.optString("description", ""),
                    categoryId = o.optNullableString("categoryId")?.takeIf { it in categoryIds },
                    projectId = o.optNullableString("projectId")?.takeIf { it in projectIds },
                    priority = o.optString("priority", "MEDIUM"),
                    status = o.optString("status", "NOT_STARTED"),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                    startEpochDay = o.optNullableLong("startEpochDay"),
                    dueEpochDay = o.optNullableLong("dueEpochDay"),
                    startMinuteOfDay = o.optNullableInt("startMinuteOfDay"),
                    endMinuteOfDay = o.optNullableInt("endMinuteOfDay"),
                    estimatedMinutes = o.optInt("estimatedMinutes", 0).coerceAtLeast(0),
                    actualMinutes = o.optInt("actualMinutes", 0).coerceAtLeast(0),
                    tagsCsv = o.optString("tagsCsv", ""),
                    note = o.optString("note", ""),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    completedAtMillis = o.optNullableLong("completedAtMillis")
                )
            }
            val taskIds = tasks.map { it.id }.toSet()

            val goals = root.getJSONArrayOrEmpty("goals").mapObjects { o ->
                GoalEntity(
                    id = o.requireNonBlank("id"),
                    title = o.requireNonBlank("title"),
                    description = o.optString("description", ""),
                    categoryId = o.optNullableString("categoryId")?.takeIf { it in categoryIds },
                    startEpochDay = o.getLong("startEpochDay"),
                    endEpochDay = o.optNullableLong("endEpochDay"),
                    progressPercent = o.optInt("progressPercent", 0).coerceIn(0, 100),
                    status = o.optString("status", "ACTIVE"),
                    notes = o.optString("notes", ""),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                )
            }
            val goalIds = goals.map { it.id }.toSet()

            val milestones = root.getJSONArrayOrEmpty("milestones").mapObjects { o ->
                MilestoneEntity(
                    id = o.requireNonBlank("id"),
                    goalId = o.requireNonBlank("goalId"),
                    title = o.requireNonBlank("title"),
                    isCompleted = o.optBoolean("isCompleted", false),
                    sortOrder = o.optInt("sortOrder", 0),
                    completedAtMillis = o.optNullableLong("completedAtMillis")
                )
            }.filter { it.goalId in goalIds }

            val goalProgressLogs = root.getJSONArrayOrEmpty("goalProgressLogs").mapObjects { o ->
                GoalProgressLogEntity(
                    id = o.requireNonBlank("id"),
                    goalId = o.requireNonBlank("goalId"),
                    progressPercent = o.optInt("progressPercent", 0).coerceIn(0, 100),
                    note = o.optString("note", ""),
                    recordedAtMillis = o.optLong("recordedAtMillis", System.currentTimeMillis())
                )
            }.filter { it.goalId in goalIds }

            val habits = root.getJSONArrayOrEmpty("habits").mapObjects { o ->
                HabitEntity(
                    id = o.requireNonBlank("id"),
                    name = o.requireNonBlank("name"),
                    description = o.optString("description", ""),
                    categoryId = o.optNullableString("categoryId")?.takeIf { it in categoryIds },
                    startEpochDay = o.getLong("startEpochDay"),
                    frequencyType = o.optString("frequencyType", "DAILY"),
                    weeklyTarget = o.optInt("weeklyTarget", 7).coerceIn(1, 7),
                    selectedDaysMask = o.optInt("selectedDaysMask", 0b1111111),
                    note = o.optString("note", ""),
                    reminderMinuteOfDay = o.optNullableInt("reminderMinuteOfDay"),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }
            val habitIds = habits.map { it.id }.toSet()

            val habitRecords = root.getJSONArrayOrEmpty("habitRecords").mapObjects { o ->
                HabitRecordEntity(
                    id = o.requireNonBlank("id"),
                    habitId = o.requireNonBlank("habitId"),
                    epochDay = o.getLong("epochDay"),
                    isCompleted = o.optBoolean("isCompleted", true),
                    completedAtMillis = o.optLong("completedAtMillis", System.currentTimeMillis()),
                    note = o.optString("note", "")
                )
            }.filter { it.habitId in habitIds }

            val calendarEvents = root.getJSONArrayOrEmpty("calendarEvents").mapObjects { o ->
                CalendarEventEntity(
                    id = o.requireNonBlank("id"),
                    title = o.requireNonBlank("title"),
                    description = o.optString("description", ""),
                    epochDay = o.getLong("epochDay"),
                    startMinuteOfDay = o.optNullableInt("startMinuteOfDay"),
                    endMinuteOfDay = o.optNullableInt("endMinuteOfDay"),
                    location = o.optString("location", ""),
                    categoryId = o.optNullableString("categoryId")?.takeIf { it in categoryIds },
                    isAllDay = o.optBoolean("isAllDay", false),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }

            val notes = root.getJSONArrayOrEmpty("notes").mapObjects { o ->
                NoteEntity(
                    id = o.requireNonBlank("id"),
                    title = o.requireNonBlank("title"),
                    content = o.optString("content", ""),
                    categoryId = o.optNullableString("categoryId")?.takeIf { it in categoryIds },
                    tagsCsv = o.optString("tagsCsv", ""),
                    isPinned = o.optBoolean("isPinned", false),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                )
            }

            val timeRecords = root.getJSONArrayOrEmpty("timeRecords").mapObjects { o ->
                TimeRecordEntity(
                    id = o.requireNonBlank("id"),
                    taskId = o.optNullableString("taskId")?.takeIf { it in taskIds },
                    projectId = o.optNullableString("projectId")?.takeIf { it in projectIds },
                    categoryId = o.optNullableString("categoryId")?.takeIf { it in categoryIds },
                    title = o.requireNonBlank("title"),
                    epochDay = o.getLong("epochDay"),
                    startMinuteOfDay = o.optInt("startMinuteOfDay", 540),
                    durationMinutes = o.optInt("durationMinutes", 0).coerceAtLeast(1),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }

            val prefsObj = root.optJSONObject("preferences")
            val prefs = prefsObj?.let { p ->
                UserPreferences(
                    themeMode = ThemeMode.entries.firstOrNull { it.name == p.optString("themeMode") }
                        ?: ThemeMode.SYSTEM,
                    calendarSystem = CalendarSystem.entries.firstOrNull { it.name == p.optString("calendarSystem") }
                        ?: CalendarSystem.JALALI,
                    weekStartDay = WeekStartDay.entries.firstOrNull { it.name == p.optString("weekStartDay") }
                        ?: WeekStartDay.SATURDAY,
                    use24HourClock = p.optBoolean("use24HourClock", true),
                    usePersianDigits = p.optBoolean("usePersianDigits", true),
                    notificationsEnabled = p.optBoolean("notificationsEnabled", false),
                    taskRemindersEnabled = p.optBoolean("taskRemindersEnabled", true),
                    dueRemindersEnabled = p.optBoolean("dueRemindersEnabled", true),
                    eventRemindersEnabled = p.optBoolean("eventRemindersEnabled", true),
                    habitRemindersEnabled = p.optBoolean("habitRemindersEnabled", true),
                    userDisplayName = p.optString("userDisplayName", "")
                )
            }

            val payload = BackupPayload(
                schemaVersion = schemaVersion,
                exportedAtMillis = exportedAtMillis,
                categories = categories,
                tags = tags,
                projects = projects,
                tasks = tasks,
                goals = goals,
                milestones = milestones,
                goalProgressLogs = goalProgressLogs,
                habits = habits,
                habitRecords = habitRecords,
                calendarEvents = calendarEvents,
                notes = notes,
                timeRecords = timeRecords,
                preferences = prefs
            )
            BackupParseResult.Valid(
                BackupPreview(
                    schemaVersion = schemaVersion,
                    exportedAtMillis = exportedAtMillis,
                    taskCount = tasks.size,
                    goalCount = goals.size,
                    habitCount = habits.size,
                    eventCount = calendarEvents.size,
                    projectCount = projects.size,
                    noteCount = notes.size,
                    timeRecordCount = timeRecords.size,
                    payload = payload
                )
            )
        }.getOrElse {
            BackupParseResult.Invalid("داده‌های داخل فایل پشتیبان ناقص یا خراب هستند.")
        }
    }

    private fun JSONObject.putNullableString(key: String, value: String?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putNullableLong(key: String, value: Long?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putNullableInt(key: String, value: Int?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun JSONObject.requireNonBlank(key: String): String {
        val value = getString(key).trim()
        require(value.isNotEmpty()) { "Missing required field $key" }
        return value
    }

    private fun JSONObject.getJSONArrayOrEmpty(key: String): JSONArray =
        optJSONArray(key) ?: JSONArray()

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val result = ArrayList<T>(length())
        for (i in 0 until length()) {
            result.add(transform(getJSONObject(i)))
        }
        return result
    }
}
