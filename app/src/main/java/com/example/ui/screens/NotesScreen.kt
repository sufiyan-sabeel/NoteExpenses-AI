package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NoteItem
import com.example.ui.components.M3NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    searchQuery: String,
    currencySymbol: String = "₹",
    onSearchQueryChange: (String) -> Unit,
    onPinToggle: (NoteItem) -> Unit,
    onArchiveToggle: (NoteItem) -> Unit,
    onFavoriteToggle: (NoteItem) -> Unit,
    onLockToggle: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onDuplicateNote: (NoteItem) -> Unit,
    onShareNote: (NoteItem) -> Unit,
    onQuickAddClick: () -> Unit
) {
    var selectedFilterTab by remember { mutableStateOf("All") } // All, Pinned, Favorites, Archived, Locked
    var isGridView by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchQuery, selectedFilterTab) {
        notes.filter { note ->
            val matchesFilter = when (selectedFilterTab) {
                "Pinned" -> note.isPinned
                "Favorites" -> note.isFavorite
                "Archived" -> note.isArchived
                "Locked" -> note.isLocked
                else -> !note.isArchived
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.lowercase()
                note.rawText.lowercase().contains(q) ||
                        note.category.lowercase().contains(q) ||
                        note.merchant.lowercase().contains(q) ||
                        note.amount.toString().contains(q) ||
                        note.tags.any { it.lowercase().contains(q) }
            }

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("notes_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Material SearchBar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search notes, amount, category, tag...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View Layout"
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

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Pinned", "Favorites", "Archived", "Locked")
            filters.forEach { filterName ->
                FilterChip(
                    selected = selectedFilterTab == filterName,
                    onClick = { selectedFilterTab = filterName },
                    label = { Text(filterName) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notes List or Grid
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No notes found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try adjusting your search query or filter",
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
                    items(filteredNotes, key = { it.id }) { note ->
                        M3NoteCard(
                            note = note,
                            currencySymbol = currencySymbol,
                            onPinToggle = { onPinToggle(note) },
                            onArchiveToggle = { onArchiveToggle(note) },
                            onFavoriteToggle = { onFavoriteToggle(note) },
                            onLockToggle = { onLockToggle(note) },
                            onDelete = { onDeleteNote(note) },
                            onDuplicate = { onDuplicateNote(note) },
                            onShare = { onShareNote(note) },
                            onClick = { }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        M3NoteCard(
                            note = note,
                            currencySymbol = currencySymbol,
                            onPinToggle = { onPinToggle(note) },
                            onArchiveToggle = { onArchiveToggle(note) },
                            onFavoriteToggle = { onFavoriteToggle(note) },
                            onLockToggle = { onLockToggle(note) },
                            onDelete = { onDeleteNote(note) },
                            onDuplicate = { onDuplicateNote(note) },
                            onShare = { onShareNote(note) },
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}
