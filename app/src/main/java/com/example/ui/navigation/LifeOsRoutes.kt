package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.ui.graphics.vector.ImageVector

enum class LifeOsDestination(
    val route: String,
    val titleFa: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isPrimaryBottomNav: Boolean
) {
    DASHBOARD("dashboard", "داشبورد", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, true),
    PLANNER("planner", "برنامه‌ریزی", Icons.Rounded.EventNote, Icons.Outlined.EventNote, true),
    TASKS("tasks", "وظایف", Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle, true),
    GOALS("goals", "اهداف", Icons.Rounded.Flag, Icons.Outlined.Flag, true),
    HABITS("habits", "عادت‌ها", Icons.Rounded.Loop, Icons.Outlined.Loop, false),
    CALENDAR("calendar", "تقویم", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, false),
    PROJECTS("projects", "پروژه‌ها", Icons.Rounded.FolderOpen, Icons.Outlined.FolderOpen, false),
    NOTES("notes", "یادداشت‌ها", Icons.Rounded.StickyNote2, Icons.Outlined.StickyNote2, false),
    ANALYTICS("analytics", "گزارش‌ها", Icons.Rounded.Analytics, Icons.Outlined.Analytics, false),
    SETTINGS("settings", "تنظیمات", Icons.Rounded.Settings, Icons.Outlined.Settings, false);

    companion object {
        val primaryTabs: List<LifeOsDestination> = entries.filter { it.isPrimaryBottomNav }
        val secondaryScreens: List<LifeOsDestination> = entries.filter { !it.isPrimaryBottomNav }

        fun fromRoute(route: String?): LifeOsDestination =
            entries.firstOrNull { it.route == route } ?: DASHBOARD
    }
}
