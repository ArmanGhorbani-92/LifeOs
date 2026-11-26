package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.CalendarSystem
import com.example.core.calendar.PersianCalendar
import com.example.core.calendar.WeekStartDay
import com.example.data.preferences.ThemeMode
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val pendingRestorePreview by viewModel.pendingRestorePreview.collectAsStateWithLifecycle()

    var displayNameDraft by remember(preferences.userDisplayName) { mutableStateOf(preferences.userDisplayName) }
    var wipeDialogStep by remember { mutableIntStateOf(0) } // 0 = closed, 1 = first confirm, 2 = final confirm
    var wipeConfirmText by remember { mutableStateOf("") }

    // Android Storage Access Framework launchers for JSON Backup & Restore
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectBackupFile(uri)
        }
    }

    // Runtime POST_NOTIFICATIONS permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        } else {
            viewModel.setNotificationsEnabled(false)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LifeOsTopHeader(
            title = "تنظیمات",
            subtitle = "شخصی‌سازی ظاهر، تقویم، اعلان‌ها و پشتیبان‌گیری محلی",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Appearance & Theme
            item {
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = colors.oliveAccent)
                        Text("تنظیمات ظاهری", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            GlassFilterChip(
                                label = mode.persianLabel,
                                selected = preferences.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(12.dp))

                    Text("نام نمایشی برای خوشامدگویی (اختیاری)", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = displayNameDraft,
                            onValueChange = { displayNameDraft = it },
                            placeholder = { Text("نام شما...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.setUserDisplayName(displayNameDraft) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White)
                        ) {
                            Text("ثبت")
                        }
                    }
                }
            }

            // Language, Date, Week Start, Clock, Digits
            item {
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.oliveAccent)
                        Text("زبان، تاریخ و نمایش اعداد", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("زبان رابط کاربری", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        Text("فارسی (پیش‌فرض)", style = MaterialTheme.typography.labelLarge, color = colors.oliveAccent, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("سیستم تقویم پیش‌فرض", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassFilterChip(
                            label = "تقویم شمسی",
                            selected = preferences.calendarSystem == CalendarSystem.JALALI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.JALALI) }
                        )
                        GlassFilterChip(
                            label = "تقویم میلادی",
                            selected = preferences.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("شروع هفته", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeekStartDay.entries.forEach { wd ->
                            GlassFilterChip(
                                label = wd.persianLabel,
                                selected = preferences.weekStartDay == wd,
                                onClick = { viewModel.setWeekStartDay(wd) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(8.dp))

                    SettingsSwitchRow(
                        title = "نمایش ساعت ۲۴ ساعته",
                        subtitle = if (preferences.use24HourClock) "مثال: ۱۴:۳۰" else "مثال: ۰۲:۳۰ ب.ظ",
                        checked = preferences.use24HourClock,
                        onCheckedChange = { viewModel.setUse24HourClock(it) }
                    )

                    SettingsSwitchRow(
                        title = "نمایش اعداد فارسی",
                        subtitle = if (preferences.usePersianDigits) "مثال: ۱۴۰۵/۰۹/۰۵" else "مثال: 1405/09/05",
                        checked = preferences.usePersianDigits,
                        onCheckedChange = { viewModel.setUsePersianDigits(it) }
                    )
                }
            }

            // Notifications Management
            item {
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.oliveAccent)
                        Text("مدیریت اعلان‌های محلی", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(8.dp))

                    SettingsSwitchRow(
                        title = "فعال‌سازی اعلان‌های زمان‌بندی‌شده",
                        subtitle = "اعلان‌ها تنها با اجازه شما و کاملاً روی دستگاه اجرا می‌شوند",
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(true)
                                }
                            } else {
                                viewModel.setNotificationsEnabled(false)
                            }
                        }
                    )

                    if (preferences.notificationsEnabled) {
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 6.dp))
                        SettingsSwitchRow(
                            title = "یادآوری وظایف",
                            subtitle = "اعلان در ساعت شروع وظیفه",
                            checked = preferences.taskRemindersEnabled,
                            onCheckedChange = { viewModel.setTaskRemindersEnabled(it) }
                        )
                        SettingsSwitchRow(
                            title = "یادآوری سررسیدها",
                            subtitle = "اعلان در روز سررسید وظایف",
                            checked = preferences.dueRemindersEnabled,
                            onCheckedChange = { viewModel.setDueRemindersEnabled(it) }
                        )
                        SettingsSwitchRow(
                            title = "یادآوری رویدادها",
                            subtitle = "اعلان برای رویدادهای تقویم",
                            checked = preferences.eventRemindersEnabled,
                            onCheckedChange = { viewModel.setEventRemindersEnabled(it) }
                        )
                        SettingsSwitchRow(
                            title = "یادآوری عادت‌ها",
                            subtitle = "یادآوری روزانه برای عادت‌های زمان‌دار",
                            checked = preferences.habitRemindersEnabled,
                            onCheckedChange = { viewModel.setHabitRemindersEnabled(it) }
                        )
                    }
                }
            }

            // Data Management: Export / Backup / Import / Restore / Multi-step Wipe
            item {
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, tint = colors.oliveAccent)
                        Text("مدیریت اطلاعات و پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "تمام اطلاعات LIFEOS به‌صورت آفلاین در پایگاه داده محلی ذخیره می‌شوند و به هیچ سرویس خارجی ارسال نمی‌گردند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val today = PersianCalendar.todayEpochDay()
                            val j = PersianCalendar.epochDayToJalali(today)
                            val fileName = "LIFEOS_Backup_${j.year}_${j.month}_${j.day}.json"
                            exportLauncher.launch(fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("خروجی گرفتن و پشتیبان‌گیری (JSON)")
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_backup_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, tint = colors.oliveAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("وارد کردن و بازیابی اطلاعات از فایل", color = colors.textPrimary)
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            wipeConfirmText = ""
                            wipeDialogStep = 1
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wipe_data_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("پاک کردن تمام اطلاعات")
                    }
                }
            }

            // App Info
            item {
                GlassCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("LIFEOS — مدیریت زندگی شخصی", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                        Text("نسخه ۱.۰ (آفلاین)", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }

    // Validated Backup Restore Preview & Confirmation Dialog
    pendingRestorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePreview() },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("تأیید بازیابی پشتیبان", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "فایل پشتیبان معتبر است (نسخه ساختار: ${PersianCalendar.formatNumber(preview.schemaVersion, preferences.usePersianDigits)}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Text("خلاصه اطلاعات فایل:", style = MaterialTheme.typography.labelLarge, color = colors.oliveAccent)
                    Text("• وظایف: ${PersianCalendar.formatNumber(preview.taskCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Text("• اهداف: ${PersianCalendar.formatNumber(preview.goalCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Text("• عادت‌ها: ${PersianCalendar.formatNumber(preview.habitCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Text("• رویدادها: ${PersianCalendar.formatNumber(preview.eventCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Text("• پروژه‌ها: ${PersianCalendar.formatNumber(preview.projectCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Text("• یادداشت‌ها: ${PersianCalendar.formatNumber(preview.noteCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Text("• رکوردهای زمان: ${PersianCalendar.formatNumber(preview.timeRecordCount, preferences.usePersianDigits)}", color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "با تأیید بازیابی، اطلاعات فعلی به‌صورت یکجا با این نسخه جایگزین خواهند شد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.warning
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestoreBackup(preview.payload) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.oliveAccent, contentColor = Color.White)
                ) {
                    Text("تأیید و بازیابی")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissRestorePreview() }) {
                    Text("انصراف", color = colors.textPrimary)
                }
            }
        )
    }

    // Multi-step Wipe Confirmation Step 1
    if (wipeDialogStep == 1) {
        AlertDialog(
            onDismissRequest = { wipeDialogStep = 0 },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("هشدار پاک‌سازی اطلاعات (مرحله ۱ از ۲)", color = colors.danger) },
            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید تمام وظایف، اهداف، عادت‌ها، رویدادها، پروژه‌ها و یادداشت‌ها را حذف کنید؟ توصیه می‌شود ابتدا فایل پشتیبان JSON تهیه کنید.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { wipeDialogStep = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                ) {
                    Text("ادامه به تأیید نهایی")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { wipeDialogStep = 0 }) {
                    Text("انصراف", color = colors.textPrimary)
                }
            }
        )
    }

    // Multi-step Wipe Confirmation Step 2 (type "حذف" to confirm)
    if (wipeDialogStep == 2) {
        AlertDialog(
            onDismissRequest = { wipeDialogStep = 0 },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("تأیید نهایی پاک‌سازی (مرحله ۲ از ۲)", color = colors.danger) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "برای جلوگیری از حذف تصادفی، لطفاً کلمه «حذف» را در کادر زیر وارد کنید:",
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = wipeConfirmText,
                        onValueChange = { wipeConfirmText = it },
                        placeholder = { Text("حذف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = wipeConfirmText.trim() == "حذف",
                    onClick = {
                        viewModel.wipeAllData()
                        wipeDialogStep = 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                ) {
                    Text("پاک کردن قطعی همه داده‌ها")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { wipeDialogStep = 0 }) {
                    Text("انصراف", color = colors.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LifeOsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
