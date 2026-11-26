package com.example.ui.viewmodel

import com.example.core.calendar.PersianCalendar
import com.example.data.local.HabitEntity
import com.example.data.local.HabitFrequency
import com.example.data.local.HabitRecordEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskStatus
import com.example.data.local.toHabitFrequency
import kotlin.math.max

data class HabitStats(
    val completedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val completionPercent: Int,
    val totalCompletions: Int
)

data class TimelineConflict(
    val firstTask: TaskEntity,
    val secondTask: TaskEntity
)

object LifeOsCalculators {

    fun isTaskOverdue(task: TaskEntity, todayEpochDay: Long): Boolean {
        val status = task.status
        if (status == TaskStatus.COMPLETED.name || status == TaskStatus.CANCELLED.name) return false
        val due = task.dueEpochDay ?: return false
        return due < todayEpochDay
    }

    fun isTaskForDay(task: TaskEntity, epochDay: Long): Boolean {
        return task.startEpochDay == epochDay || task.dueEpochDay == epochDay
    }

    fun taskEffectiveStartMinute(task: TaskEntity): Int? = task.startMinuteOfDay

    fun taskEffectiveEndMinute(task: TaskEntity): Int? {
        val start = task.startMinuteOfDay ?: return null
        val explicitEnd = task.endMinuteOfDay
        if (explicitEnd != null && explicitEnd > start) return explicitEnd
        val duration = if (task.estimatedMinutes > 0) task.estimatedMinutes else 30
        return (start + duration).coerceAtMost(24 * 60)
    }

    fun detectTimelineConflicts(tasksOnDay: List<TaskEntity>): List<TimelineConflict> {
        val scheduled = tasksOnDay
            .filter { it.startMinuteOfDay != null && it.status != TaskStatus.CANCELLED.name }
            .sortedBy { it.startMinuteOfDay }

        val conflicts = mutableListOf<TimelineConflict>()
        for (i in scheduled.indices) {
            val a = scheduled[i]
            val aStart = taskEffectiveStartMinute(a) ?: continue
            val aEnd = taskEffectiveEndMinute(a) ?: continue
            for (j in (i + 1) until scheduled.size) {
                val b = scheduled[j]
                val bStart = taskEffectiveStartMinute(b) ?: continue
                val bEnd = taskEffectiveEndMinute(b) ?: continue
                if (aStart < bEnd && bStart < aEnd) {
                    conflicts.add(TimelineConflict(a, b))
                }
            }
        }
        return conflicts
    }

    fun isHabitScheduledOnDay(habit: HabitEntity, epochDay: Long): Boolean {
        if (epochDay < habit.startEpochDay) return false
        return when (habit.frequencyType.toHabitFrequency()) {
            HabitFrequency.DAILY -> true
            HabitFrequency.SPECIFIC_DAYS -> {
                val dow = PersianCalendar.dayOfWeekSaturdayFirst(epochDay)
                (habit.selectedDaysMask and (1 shl dow)) != 0
            }
            HabitFrequency.TIMES_PER_WEEK, HabitFrequency.WEEKLY -> true
        }
    }

    fun computeHabitStats(
        habit: HabitEntity,
        records: List<HabitRecordEntity>,
        todayEpochDay: Long
    ): HabitStats {
        val completedDays = records
            .asSequence()
            .filter { it.habitId == habit.id && it.isCompleted }
            .map { it.epochDay }
            .toSet()

        val completedToday = todayEpochDay in completedDays
        val totalCompletions = completedDays.size
        val freq = habit.frequencyType.toHabitFrequency()

        // Current Streak
        var currentStreak = 0
        var cursor = if (completedToday) todayEpochDay else todayEpochDay - 1L
        val lowerBound = minOf(habit.startEpochDay, completedDays.minOrNull() ?: habit.startEpochDay)

        while (cursor >= lowerBound) {
            val scheduled = if (freq == HabitFrequency.SPECIFIC_DAYS) {
                isHabitScheduledOnDay(habit, cursor)
            } else {
                true
            }
            if (!scheduled) {
                cursor -= 1L
                continue
            }
            if (cursor in completedDays) {
                currentStreak += 1
                cursor -= 1L
            } else {
                break
            }
        }

        // Best Streak
        var bestStreak = 0
        if (completedDays.isNotEmpty()) {
            val minDay = completedDays.min()
            val maxDay = completedDays.max()
            var run = 0
            var d = minDay
            while (d <= maxDay) {
                val scheduled = if (freq == HabitFrequency.SPECIFIC_DAYS) {
                    isHabitScheduledOnDay(habit, d)
                } else {
                    true
                }
                if (d in completedDays) {
                    run += 1
                    bestStreak = max(bestStreak, run)
                } else if (scheduled) {
                    run = 0
                }
                d += 1L
            }
        }

        // Completion percentage over active window (up to last 30 days since startEpochDay)
        val windowStart = max(habit.startEpochDay, todayEpochDay - 29L)
        val totalWindowDays = (todayEpochDay - windowStart + 1L).coerceAtLeast(1L)
        val expectedDays = when (freq) {
            HabitFrequency.DAILY -> totalWindowDays.toInt()
            HabitFrequency.SPECIFIC_DAYS -> {
                var cnt = 0
                var day = windowStart
                while (day <= todayEpochDay) {
                    if (isHabitScheduledOnDay(habit, day)) cnt++
                    day++
                }
                cnt.coerceAtLeast(1)
            }
            HabitFrequency.TIMES_PER_WEEK -> {
                val weeks = max(1f, totalWindowDays / 7f)
                (weeks * habit.weeklyTarget.coerceIn(1, 7)).toInt().coerceAtLeast(1)
            }
            HabitFrequency.WEEKLY -> {
                val weeks = max(1f, totalWindowDays / 7f)
                weeks.toInt().coerceAtLeast(1)
            }
        }
        val completedInWindow = completedDays.count { it in windowStart..todayEpochDay }
        val pct = ((completedInWindow.toFloat() / expectedDays.toFloat()) * 100f).toInt().coerceIn(0, 100)

        return HabitStats(
            completedToday = completedToday,
            currentStreak = currentStreak,
            bestStreak = max(bestStreak, currentStreak),
            completionPercent = pct,
            totalCompletions = totalCompletions
        )
    }

    fun taskCompletionDay(task: TaskEntity): Long? {
        if (task.status != TaskStatus.COMPLETED.name) return null
        val completedMillis = task.completedAtMillis
        if (completedMillis != null) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = completedMillis }
            return PersianCalendar.gregorianToEpochDay(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
        return task.dueEpochDay ?: task.startEpochDay
    }
}
