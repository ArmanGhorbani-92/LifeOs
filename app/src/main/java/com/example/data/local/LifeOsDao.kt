package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeOsDao {

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    // Goals
    @Query("SELECT * FROM goals ORDER BY updatedAtMillis DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY updatedAtMillis DESC")
    suspend fun getAllGoals(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    // Milestones
    @Query("SELECT * FROM milestones ORDER BY sortOrder ASC")
    fun observeMilestones(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones ORDER BY sortOrder ASC")
    suspend fun getAllMilestones(): List<MilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestone(milestone: MilestoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestones(milestones: List<MilestoneEntity>)

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteMilestone(id: String)

    // Goal Progress History
    @Query("SELECT * FROM goal_progress_logs ORDER BY recordedAtMillis DESC")
    fun observeGoalProgressLogs(): Flow<List<GoalProgressLogEntity>>

    @Query("SELECT * FROM goal_progress_logs ORDER BY recordedAtMillis DESC")
    suspend fun getAllGoalProgressLogs(): List<GoalProgressLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalProgressLog(log: GoalProgressLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalProgressLogs(logs: List<GoalProgressLogEntity>)

    // Habits
    @Query("SELECT * FROM habits ORDER BY createdAtMillis DESC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAtMillis DESC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: String)

    // Habit Records
    @Query("SELECT * FROM habit_records ORDER BY epochDay DESC")
    fun observeHabitRecords(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records ORDER BY epochDay DESC")
    suspend fun getAllHabitRecords(): List<HabitRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitRecord(record: HabitRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitRecords(records: List<HabitRecordEntity>)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteHabitRecordForDay(habitId: String, epochDay: Long)

    // Calendar Events
    @Query("SELECT * FROM calendar_events ORDER BY epochDay ASC, startMinuteOfDay ASC")
    fun observeCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY epochDay ASC, startMinuteOfDay ASC")
    suspend fun getAllCalendarEvents(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarEvent(event: CalendarEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarEvents(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEvent(id: String)

    // Projects
    @Query("SELECT * FROM projects ORDER BY createdAtMillis DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY createdAtMillis DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    // Notes
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtMillis DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    // Time Records
    @Query("SELECT * FROM time_records ORDER BY epochDay DESC, startMinuteOfDay DESC")
    fun observeTimeRecords(): Flow<List<TimeRecordEntity>>

    @Query("SELECT * FROM time_records ORDER BY epochDay DESC, startMinuteOfDay DESC")
    suspend fun getAllTimeRecords(): List<TimeRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeRecord(record: TimeRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeRecords(records: List<TimeRecordEntity>)

    @Query("DELETE FROM time_records WHERE id = :id")
    suspend fun deleteTimeRecord(id: String)

    // Atomic Clear & Restore Helpers
    @Query("DELETE FROM time_records")
    suspend fun clearTimeRecords()

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()

    @Query("DELETE FROM habit_records")
    suspend fun clearHabitRecords()

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM goal_progress_logs")
    suspend fun clearGoalProgressLogs()

    @Query("DELETE FROM milestones")
    suspend fun clearMilestones()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM projects")
    suspend fun clearProjects()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Transaction
    suspend fun clearAllUserData() {
        clearTimeRecords()
        clearNotes()
        clearCalendarEvents()
        clearHabitRecords()
        clearHabits()
        clearGoalProgressLogs()
        clearMilestones()
        clearGoals()
        clearTasks()
        clearProjects()
        clearTags()
        clearCategories()
    }

    @Transaction
    suspend fun replaceAllDataAtomically(
        categories: List<CategoryEntity>,
        tags: List<TagEntity>,
        projects: List<ProjectEntity>,
        tasks: List<TaskEntity>,
        goals: List<GoalEntity>,
        milestones: List<MilestoneEntity>,
        goalProgressLogs: List<GoalProgressLogEntity>,
        habits: List<HabitEntity>,
        habitRecords: List<HabitRecordEntity>,
        calendarEvents: List<CalendarEventEntity>,
        notes: List<NoteEntity>,
        timeRecords: List<TimeRecordEntity>
    ) {
        clearAllUserData()
        upsertCategories(categories)
        upsertTags(tags)
        upsertProjects(projects)
        upsertTasks(tasks)
        upsertGoals(goals)
        upsertMilestones(milestones)
        upsertGoalProgressLogs(goalProgressLogs)
        upsertHabits(habits)
        upsertHabitRecords(habitRecords)
        upsertCalendarEvents(calendarEvents)
        upsertNotes(notes)
        upsertTimeRecords(timeRecords)
    }
}
