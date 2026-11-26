package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskPriority(val persianLabel: String, val sortRank: Int) {
    LOW("کم", 1),
    MEDIUM("متوسط", 2),
    HIGH("زیاد", 3),
    URGENT("فوری", 4)
}

enum class TaskStatus(val persianLabel: String) {
    NOT_STARTED("شروع‌نشده"),
    IN_PROGRESS("در حال انجام"),
    COMPLETED("تکمیل‌شده"),
    CANCELLED("لغوشده")
}

enum class GoalStatus(val persianLabel: String) {
    ACTIVE("فعال"),
    COMPLETED("تکمیل‌شده"),
    PAUSED("متوقف‌شده"),
    CANCELLED("لغوشده")
}

enum class HabitFrequency(val persianLabel: String) {
    DAILY("روزانه"),
    TIMES_PER_WEEK("چند روز در هفته"),
    WEEKLY("هفتگی"),
    SPECIFIC_DAYS("روزهای انتخابی")
}

enum class ProjectStatus(val persianLabel: String) {
    ACTIVE("فعال"),
    ON_HOLD("متوقف‌شده"),
    COMPLETED("تکمیل‌شده"),
    CANCELLED("لغوشده")
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "folder",
    val colorHex: String = "#71805A",
    val scope: String = "ALL"
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val startEpochDay: Long,
    val deadlineEpochDay: Long? = null,
    val status: String = ProjectStatus.ACTIVE.name,
    val progressPercent: Int = 0,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("projectId"),
        Index("dueEpochDay"),
        Index("startEpochDay")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val categoryId: String? = null,
    val projectId: String? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.NOT_STARTED.name,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val startEpochDay: Long? = null,
    val dueEpochDay: Long? = null,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val tagsCsv: String = "",
    val note: String = "",
    val reminderEnabled: Boolean = false,
    val completedAtMillis: Long? = null
)

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val categoryId: String? = null,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val progressPercent: Int = 0,
    val status: String = GoalStatus.ACTIVE.name,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class MilestoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val completedAtMillis: Long? = null
)

@Entity(
    tableName = "goal_progress_logs",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class GoalProgressLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val progressPercent: Int,
    val note: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val categoryId: String? = null,
    val startEpochDay: Long,
    val frequencyType: String = HabitFrequency.DAILY.name,
    val weeklyTarget: Int = 7,
    val selectedDaysMask: Int = 0b1111111,
    val note: String = "",
    val reminderMinuteOfDay: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habitId"),
        Index(value = ["habitId", "epochDay"], unique = true)
    ]
)
data class HabitRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val epochDay: Long,
    val isCompleted: Boolean = true,
    val completedAtMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "calendar_events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("epochDay")]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val epochDay: Long,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val location: String = "",
    val categoryId: String? = null,
    val isAllDay: Boolean = false,
    val reminderEnabled: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String = "",
    val categoryId: String? = null,
    val tagsCsv: String = "",
    val isPinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "time_records",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("taskId"),
        Index("projectId"),
        Index("categoryId"),
        Index("epochDay")
    ]
)
data class TimeRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val projectId: String? = null,
    val categoryId: String? = null,
    val title: String,
    val epochDay: Long,
    val startMinuteOfDay: Int = 540,
    val durationMinutes: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// Safe enum parsing extensions
fun String.toTaskPriority(): TaskPriority =
    TaskPriority.entries.firstOrNull { it.name == this } ?: TaskPriority.MEDIUM

fun String.toTaskStatus(): TaskStatus =
    TaskStatus.entries.firstOrNull { it.name == this } ?: TaskStatus.NOT_STARTED

fun String.toGoalStatus(): GoalStatus =
    GoalStatus.entries.firstOrNull { it.name == this } ?: GoalStatus.ACTIVE

fun String.toHabitFrequency(): HabitFrequency =
    HabitFrequency.entries.firstOrNull { it.name == this } ?: HabitFrequency.DAILY

fun String.toProjectStatus(): ProjectStatus =
    ProjectStatus.entries.firstOrNull { it.name == this } ?: ProjectStatus.ACTIVE
