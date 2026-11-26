package com.example

import com.example.core.calendar.JalaliDate
import com.example.core.calendar.PersianCalendar
import com.example.data.local.HabitEntity
import com.example.data.local.HabitFrequency
import com.example.data.local.HabitRecordEntity
import com.example.data.local.TaskEntity
import com.example.ui.viewmodel.LifeOsCalculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeOsCoreUnitTest {

    @Test
    fun jalaliGregorianExactRoundTripAndLeapYears() {
        // 1 Farvardin 1403 (leap year) == 2024-03-20
        val ep1403 = PersianCalendar.jalaliToEpochDay(1403, 1, 1)
        val g1403 = PersianCalendar.epochDayToGregorian(ep1403)
        assertEquals(2024, g1403.year)
        assertEquals(3, g1403.month)
        assertEquals(20, g1403.day)
        assertTrue(PersianCalendar.isJalaliLeapYear(1403))
        assertEquals(30, PersianCalendar.jalaliMonthLength(1403, 12))

        // 30 Esfand 1403 == 2025-03-20
        val epEnd1403 = PersianCalendar.jalaliToEpochDay(1403, 12, 30)
        val gEnd1403 = PersianCalendar.epochDayToGregorian(epEnd1403)
        assertEquals(2025, gEnd1403.year)
        assertEquals(3, gEnd1403.month)
        assertEquals(20, gEnd1403.day)
        assertEquals(JalaliDate(1403, 12, 30), PersianCalendar.epochDayToJalali(epEnd1403))

        // 1 Farvardin 1404 (common year) == 2025-03-21
        val ep1404 = PersianCalendar.jalaliToEpochDay(1404, 1, 1)
        val g1404 = PersianCalendar.epochDayToGregorian(ep1404)
        assertEquals(2025, g1404.year)
        assertEquals(3, g1404.month)
        assertEquals(21, g1404.day)
        assertFalse(PersianCalendar.isJalaliLeapYear(1404))
        assertEquals(29, PersianCalendar.jalaliMonthLength(1404, 12))

        // 1 Farvardin 1405 == 2026-03-21
        val ep1405 = PersianCalendar.jalaliToEpochDay(1405, 1, 1)
        val g1405 = PersianCalendar.epochDayToGregorian(ep1405)
        assertEquals(2026, g1405.year)
        assertEquals(3, g1405.month)
        assertEquals(21, g1405.day)

        // Round-trip over 1500 consecutive days
        for (day in ep1403..(ep1403 + 1500L)) {
            val j = PersianCalendar.epochDayToJalali(day)
            assertEquals(day, PersianCalendar.jalaliToEpochDay(j.year, j.month, j.day))
            val g = PersianCalendar.epochDayToGregorian(day)
            assertEquals(day, PersianCalendar.gregorianToEpochDay(g.year, g.month, g.day))
        }
    }

    @Test
    fun timelineConflictDetectionIsAccurate() {
        val t1 = TaskEntity(id = "1", title = "جلسه اول", startMinuteOfDay = 540, endMinuteOfDay = 600)
        val t2 = TaskEntity(id = "2", title = "جلسه دوم", startMinuteOfDay = 580, endMinuteOfDay = 640)
        val t3 = TaskEntity(id = "3", title = "مطالعه", startMinuteOfDay = 660, endMinuteOfDay = 720)

        val conflicts = LifeOsCalculators.detectTimelineConflicts(listOf(t1, t2, t3))
        assertEquals(1, conflicts.size)
        assertEquals("1", conflicts.first().firstTask.id)
        assertEquals("2", conflicts.first().secondTask.id)
    }

    @Test
    fun habitStreakCountsOnlyActualRecordedDays() {
        val today = 20500L
        val habit = HabitEntity(
            id = "h1",
            name = "مطالعه",
            startEpochDay = today - 10L,
            frequencyType = HabitFrequency.DAILY.name
        )
        val records = listOf(
            HabitRecordEntity(habitId = "h1", epochDay = today - 5L, isCompleted = true),
            HabitRecordEntity(habitId = "h1", epochDay = today - 4L, isCompleted = true),
            HabitRecordEntity(habitId = "h1", epochDay = today - 3L, isCompleted = true),
            // Gap on today - 2 breaks streak
            HabitRecordEntity(habitId = "h1", epochDay = today - 1L, isCompleted = true),
            HabitRecordEntity(habitId = "h1", epochDay = today, isCompleted = true)
        )
        val stats = LifeOsCalculators.computeHabitStats(habit, records, today)
        assertTrue(stats.completedToday)
        assertEquals(2, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(5, stats.totalCompletions)
    }
}
