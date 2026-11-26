package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.WeekStartDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val persianLabel: String) {
    SYSTEM("هماهنگ با سیستم"),
    LIGHT("تم روشن"),
    DARK("تم تاریک")
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val calendarSystem: CalendarSystem = CalendarSystem.JALALI,
    val weekStartDay: WeekStartDay = WeekStartDay.SATURDAY,
    val use24HourClock: Boolean = true,
    val usePersianDigits: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val taskRemindersEnabled: Boolean = true,
    val dueRemindersEnabled: Boolean = true,
    val eventRemindersEnabled: Boolean = true,
    val habitRemindersEnabled: Boolean = true,
    val userDisplayName: String = ""
)

private val Context.lifeOsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lifeos_user_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CALENDAR_SYSTEM = stringPreferencesKey("calendar_system")
        val WEEK_START_DAY = stringPreferencesKey("week_start_day")
        val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
        val USE_PERSIAN_DIGITS = booleanPreferencesKey("use_persian_digits")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TASK_REMINDERS_ENABLED = booleanPreferencesKey("task_reminders_enabled")
        val DUE_REMINDERS_ENABLED = booleanPreferencesKey("due_reminders_enabled")
        val EVENT_REMINDERS_ENABLED = booleanPreferencesKey("event_reminders_enabled")
        val HABIT_REMINDERS_ENABLED = booleanPreferencesKey("habit_reminders_enabled")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
    }

    val preferencesFlow: Flow<UserPreferences> = context.lifeOsDataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.SYSTEM,
            calendarSystem = prefs[Keys.CALENDAR_SYSTEM]?.let { name ->
                CalendarSystem.entries.firstOrNull { it.name == name }
            } ?: CalendarSystem.JALALI,
            weekStartDay = prefs[Keys.WEEK_START_DAY]?.let { name ->
                WeekStartDay.entries.firstOrNull { it.name == name }
            } ?: WeekStartDay.SATURDAY,
            use24HourClock = prefs[Keys.USE_24_HOUR_CLOCK] ?: true,
            usePersianDigits = prefs[Keys.USE_PERSIAN_DIGITS] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            taskRemindersEnabled = prefs[Keys.TASK_REMINDERS_ENABLED] ?: true,
            dueRemindersEnabled = prefs[Keys.DUE_REMINDERS_ENABLED] ?: true,
            eventRemindersEnabled = prefs[Keys.EVENT_REMINDERS_ENABLED] ?: true,
            habitRemindersEnabled = prefs[Keys.HABIT_REMINDERS_ENABLED] ?: true,
            userDisplayName = prefs[Keys.USER_DISPLAY_NAME] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.lifeOsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCalendarSystem(system: CalendarSystem) {
        context.lifeOsDataStore.edit { it[Keys.CALENDAR_SYSTEM] = system.name }
    }

    suspend fun setWeekStartDay(day: WeekStartDay) {
        context.lifeOsDataStore.edit { it[Keys.WEEK_START_DAY] = day.name }
    }

    suspend fun setUse24HourClock(use24Hour: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.USE_24_HOUR_CLOCK] = use24Hour }
    }

    suspend fun setUsePersianDigits(usePersian: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.USE_PERSIAN_DIGITS] = usePersian }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setTaskRemindersEnabled(enabled: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.TASK_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setDueRemindersEnabled(enabled: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.DUE_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setEventRemindersEnabled(enabled: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.EVENT_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setHabitRemindersEnabled(enabled: Boolean) {
        context.lifeOsDataStore.edit { it[Keys.HABIT_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setUserDisplayName(name: String) {
        context.lifeOsDataStore.edit { it[Keys.USER_DISPLAY_NAME] = name.trim() }
    }

    suspend fun restorePreferences(prefs: UserPreferences) {
        context.lifeOsDataStore.edit {
            it[Keys.THEME_MODE] = prefs.themeMode.name
            it[Keys.CALENDAR_SYSTEM] = prefs.calendarSystem.name
            it[Keys.WEEK_START_DAY] = prefs.weekStartDay.name
            it[Keys.USE_24_HOUR_CLOCK] = prefs.use24HourClock
            it[Keys.USE_PERSIAN_DIGITS] = prefs.usePersianDigits
            it[Keys.NOTIFICATIONS_ENABLED] = prefs.notificationsEnabled
            it[Keys.TASK_REMINDERS_ENABLED] = prefs.taskRemindersEnabled
            it[Keys.DUE_REMINDERS_ENABLED] = prefs.dueRemindersEnabled
            it[Keys.EVENT_REMINDERS_ENABLED] = prefs.eventRemindersEnabled
            it[Keys.HABIT_REMINDERS_ENABLED] = prefs.habitRemindersEnabled
            it[Keys.USER_DISPLAY_NAME] = prefs.userDisplayName
        }
    }

    suspend fun resetAll() {
        context.lifeOsDataStore.edit { it.clear() }
    }
}
