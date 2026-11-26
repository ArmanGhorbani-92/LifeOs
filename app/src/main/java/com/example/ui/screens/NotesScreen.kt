package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.calendar.PersianCalendar
import com.example.data.local.NoteEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.LifeOsTopHeader
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsViewModel
import java.util.Calendar

private enum class NoteSortMode(val label: String) {
    UPDATED_DESC("جدیدترین ویرایش"),
    CREATED_DESC("تاریخ ایجاد"),
    TITLE_ASC("عنوان")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val colors = LifeOsTheme.colors
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(NoteSortMode.UPDATED_DESC) }

    var showCreate by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deletingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes = remember(notes, searchQuery, selectedCategoryId, sortMode) {
        notes.filter { n ->
            val matchesQuery = searchQuery.isBlank() ||
                n.title.contains(searchQuery, ignoreCase = true) ||
                n.content.contains(searchQuery, ignoreCase = true) ||
                n.tagsCsv.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategoryId == null || n.categoryId == selectedCategoryId
            matchesQuery && matchesCat
        }.let { list ->
            val comparator = when (sortMode) {
                NoteSortMode.UPDATED_DESC -> compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAtMillis }
                NoteSortMode.CREATED_DESC -> compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.createdAtMillis }
                NoteSortMode.TITLE_ASC -> compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title }
            }
            list.sortedWith(comparator)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LifeOsTopHeader(
                title = "یادداشت‌ها",
                subtitle = "${PersianCalendar.formatNumber(filteredNotes.size, preferences.usePersianDigits)} یادداشت",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جست‌وجو در یادداشت‌ها و برچسب‌ها...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_search_input")
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            GlassFilterChip(
                                label = "مرتب‌سازی: ${sortMode.label}",
                                selected = true,
                                onClick = {
                                    sortMode = when (sortMode) {
                                        NoteSortMode.UPDATED_DESC -> NoteSortMode.CREATED_DESC
                                        NoteSortMode.CREATED_DESC -> NoteSortMode.TITLE_ASC
                                        NoteSortMode.TITLE_ASC -> NoteSortMode.UPDATED_DESC
                                    }
                                }
                            )
                        }
                        item {
                            GlassFilterChip(
                                label = "همه دسته‌ها",
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null }
                            )
                        }
                        items(categories) { cat ->
                            GlassFilterChip(
                                label = cat.name,
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id }
                            )
                        }
                    }
                }

                if (filteredNotes.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            icon = Icons.Rounded.StickyNote2,
                            title = "یادداشتی وجود ندارد",
                            description = "ایده‌ها، جمع‌بندی‌های روزانه و نکات مهم خود را به فارسی ثبت و دسته‌بندی کنید.",
                            actionLabel = "ایجاد یادداشت",
                            onAction = { showCreate = true }
                        )
                    }
                } else {
                    items(filteredNotes, key = { it.id }) { note ->
                        val cal = Calendar.getInstance().apply { timeInMillis = note.updatedAtMillis }
                        val epochDay = PersianCalendar.gregorianToEpochDay(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        val dateStr = PersianCalendar.formatShortDate(epochDay, preferences.calendarSystem, preferences.usePersianDigits)
                        val tags = note.tagsCsv.split(",", "،").map { it.trim() }.filter { it.isNotEmpty() }

                        GlassCard(
                            elevated = note.isPinned,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(15.dp),
                            onClick = { editingNote = note }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (note.isPinned) {
                                            Icon(Icons.Rounded.PushPin, contentDescription = "سنجاق‌شده", tint = colors.oliveAccent, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (note.content.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.saveNote(
                                                note.copy(isPinned = !note.isPinned),
                                                if (!note.isPinned) "یادداشت سنجاق شد." else "سنجاق یادداشت برداشته شد."
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.PushPin,
                                            contentDescription = "سنجاق",
                                            tint = if (note.isPinned) colors.oliveAccent else colors.textSecondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(onClick = { editingNote = note }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = colors.textSecondary, modifier = Modifier.size(19.dp))
                                    }
                                    IconButton(onClick = { deletingNote = note }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = colors.danger, modifier = Modifier.size(19.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                note.categoryId?.let { categoryMap[it] }?.let { cat ->
                                    StatusBadge(label = cat.name, tint = colors.oliveAccent)
                                }
                                tags.forEach { tg ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.glassSurfaceSubtle.copy(alpha = 0.45f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("#$tg", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "آخرین ویرایش: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreate = true },
            containerColor = colors.oliveAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("add_note_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "ایجاد یادداشت")
        }
    }

    if (showCreate) {
        NoteEditorDialog(
            categories = categories,
            onSave = {
                viewModel.saveNote(it)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }

    editingNote?.let { n ->
        NoteEditorDialog(
            initialNote = n,
            categories = categories,
            onSave = {
                viewModel.saveNote(it, "تغییرات یادداشت ذخیره شد.")
                editingNote = null
            },
            onDismiss = { editingNote = null }
        )
    }

    deletingNote?.let { n ->
        ConfirmDeleteDialog(
            title = "حذف یادداشت",
            message = "آیا از حذف یادداشت «${n.title}» مطمئن هستید؟",
            onConfirm = {
                viewModel.deleteNote(n.id)
                deletingNote = null
            },
            onDismiss = { deletingNote = null }
        )
    }
}
