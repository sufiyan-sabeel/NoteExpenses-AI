package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NoteItem
import com.example.data.model.NoteSortOption
import com.example.ui.components.M3NoteCard
import com.example.ui.components.NoteEditorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    trashNotes: List<NoteItem> = emptyList(),
    searchQuery: String,
    currencySymbol: String = "₹",
    categoriesList: List<String> = listOf("Personal", "Work", "Study", "Ideas", "Shopping", "Travel"),
    onSearchQueryChange: (String) -> Unit,
    onSaveNote: (NoteItem) -> Unit,
    onPinToggle: (NoteItem) -> Unit,
    onArchiveToggle: (NoteItem) -> Unit,
    onFavoriteToggle: (NoteItem) -> Unit,
    onLockToggle: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onRestoreFromTrash: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onDuplicateNote: (NoteItem) -> Unit,
    onShareNote: (NoteItem) -> Unit,
    onQuickAddClick: () -> Unit
) {
    var selectedFilterTab by remember { mutableStateOf("All") } // All, Personal, Work, Study, Ideas, Shopping, Travel, Pinned, Favorites, Trash
    var selectedSortOption by remember { mutableStateOf(NoteSortOption.NEWEST) }
    var isGridView by remember { mutableStateOf(false) }

    var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    var noteToDeleteConfirm by remember { mutableStateOf<NoteItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filterOptions = remember(categoriesList) {
        listOf("All", "Pinned", "Favorites") + categoriesList.distinct() + listOf("Trash")
    }

    val activeList = if (selectedFilterTab == "Trash") trashNotes else notes

    val filteredAndSortedNotes = remember(activeList, searchQuery, selectedFilterTab, selectedSortOption) {
        val list = activeList.filter { note ->
            val matchesFilter = when (selectedFilterTab) {
                "Pinned" -> note.isPinned && !note.isTrash
                "Favorites" -> note.isFavorite && !note.isTrash
                "Trash" -> note.isTrash
                "All" -> !note.isTrash
                else -> note.category.equals(selectedFilterTab, ignoreCase = true) && !note.isTrash
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.lowercase()
                note.title.lowercase().contains(q) ||
                        note.rawText.lowercase().contains(q) ||
                        note.category.lowercase().contains(q) ||
                        note.merchant.lowercase().contains(q) ||
                        note.checklistItems.any { it.text.lowercase().contains(q) } ||
                        note.tags.any { it.lowercase().contains(q) }
            }

            matchesFilter && matchesSearch
        }

        when (selectedSortOption) {
            NoteSortOption.NEWEST -> list.sortedWith(compareByDescending<NoteItem> { it.isPinned }.thenByDescending { it.timestamp })
            NoteSortOption.OLDEST -> list.sortedWith(compareByDescending<NoteItem> { it.isPinned }.thenBy { it.timestamp })
            NoteSortOption.ALPHABETICAL -> list.sortedWith(compareByDescending<NoteItem> { it.isPinned }.thenBy { it.title.ifBlank { it.rawText } })
            NoteSortOption.COLOR -> list.sortedWith(compareByDescending<NoteItem> { it.isPinned }.thenBy { it.colorHex })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("notes_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar & Actions
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search notes by title, content, tag...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }

                    // Sort dropdown
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort Notes")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = { selectedSortOption = NoteSortOption.NEWEST; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = { selectedSortOption = NoteSortOption.OLDEST; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Alphabetical (A-Z)") },
                                onClick = { selectedSortOption = NoteSortOption.ALPHABETICAL; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("By Color") },
                                onClick = { selectedSortOption = NoteSortOption.COLOR; showSortMenu = false }
                            )
                        }
                    }

                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_search_bar"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Categories LazyRow
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(filterOptions) { filterName ->
                val isSelected = selectedFilterTab == filterName
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterTab = filterName },
                    label = { Text(filterName) },
                    leadingIcon = {
                        if (filterName == "Trash") {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        } else if (filterName == "Pinned") {
                            Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        } else if (filterName == "Favorites") {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (selectedFilterTab == "Trash" && trashNotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recently Deleted Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onEmptyTrash) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Empty Trash")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notes Grid or List
        if (filteredAndSortedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedFilterTab == "Trash") Icons.Default.DeleteOutline else Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedFilterTab == "Trash") "Trash is empty" else "No notes found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (selectedFilterTab == "Trash") "Deleted notes will appear here for 30 days" else "Tap '+' below to create your first note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredAndSortedNotes, key = { it.id }) { note ->
                        if (selectedFilterTab == "Trash") {
                            TrashNoteCard(
                                note = note,
                                onRestore = { onRestoreFromTrash(note.id) },
                                onDeletePermanently = { onDeletePermanently(note.id) }
                            )
                        } else {
                            M3NoteCard(
                                note = note,
                                currencySymbol = currencySymbol,
                                onPinToggle = { onPinToggle(note) },
                                onArchiveToggle = { onArchiveToggle(note) },
                                onFavoriteToggle = { onFavoriteToggle(note) },
                                onLockToggle = { onLockToggle(note) },
                                onDelete = {
                                    noteToDeleteConfirm = note
                                    showDeleteConfirmDialog = true
                                },
                                onDuplicate = { onDuplicateNote(note) },
                                onShare = { onShareNote(note) },
                                onClick = {
                                    noteToEdit = note
                                    showEditorDialog = true
                                }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredAndSortedNotes, key = { it.id }) { note ->
                        if (selectedFilterTab == "Trash") {
                            TrashNoteCard(
                                note = note,
                                onRestore = { onRestoreFromTrash(note.id) },
                                onDeletePermanently = { onDeletePermanently(note.id) }
                            )
                        } else {
                            M3NoteCard(
                                note = note,
                                currencySymbol = currencySymbol,
                                onPinToggle = { onPinToggle(note) },
                                onArchiveToggle = { onArchiveToggle(note) },
                                onFavoriteToggle = { onFavoriteToggle(note) },
                                onLockToggle = { onLockToggle(note) },
                                onDelete = {
                                    noteToDeleteConfirm = note
                                    showDeleteConfirmDialog = true
                                },
                                onDuplicate = { onDuplicateNote(note) },
                                onShare = { onShareNote(note) },
                                onClick = {
                                    noteToEdit = note
                                    showEditorDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Note Editor Dialog for New or Existing Note
    if (showEditorDialog) {
        NoteEditorDialog(
            initialNote = noteToEdit,
            availableCategories = categoriesList,
            onDismissRequest = {
                showEditorDialog = false
                noteToEdit = null
            },
            onSaveNote = { saved ->
                onSaveNote(saved)
            },
            onDeleteNote = { noteId ->
                val target = notes.find { it.id == noteId }
                if (target != null) {
                    onDeleteNote(target)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && noteToDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                noteToDeleteConfirm = null
            },
            title = { Text("Move Note to Trash?") },
            text = { Text("Are you sure you want to move this note to recently deleted?") },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDeleteConfirm?.let { onDeleteNote(it) }
                        showDeleteConfirmDialog = false
                        noteToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteConfirmDialog = false
                    noteToDeleteConfirm = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrashNoteCard(
    note: NoteItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title.ifBlank { note.rawText.take(40) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.rawText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeletePermanently) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

