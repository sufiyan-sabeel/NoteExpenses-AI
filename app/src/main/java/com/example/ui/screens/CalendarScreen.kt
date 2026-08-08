package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NoteItem
import com.example.ui.components.M3NoteCard
import com.example.ui.viewmodel.NotesExpensesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    notes: List<NoteItem>,
    viewModel: NotesExpensesViewModel,
    currencySymbol: String = "₹",
    onNoteClick: (NoteItem) -> Unit,
    onPinToggle: (NoteItem) -> Unit,
    onArchiveToggle: (NoteItem) -> Unit,
    onFavoriteToggle: (NoteItem) -> Unit,
    onLockToggle: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onDuplicateNote: (NoteItem) -> Unit,
    onShareNote: (NoteItem) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    val formattedSelectedDate = remember(selectedDateMillis) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("calendar_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendar & Reminders",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Two-Way Google Calendar Sync & Bill Alarms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showAddEventDialog = true },
                    modifier = Modifier.testTag("add_calendar_event_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Event")
                }
            }
        }

        // Google Calendar Integration Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Google Calendar Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.addGoogleCalendarEvent("Electricity Bill Payment", "Monthly bill reminder created via Notes Expenses", System.currentTimeMillis() + 86400000L * 5) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync Now")
                        }
                    }

                    Text(
                        "Automatic bill, EMI, salary, and subscription reminders are synced directly with your primary Google Calendar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Timeline Entries (${notes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (notes.isEmpty()) {
            item {
                Text(text = "No notes recorded on this timeline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(notes, key = { it.id }) { note ->
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
                    onClick = { onNoteClick(note) }
                )
            }
        }
    }

    if (showAddEventDialog) {
        AddCalendarEventDialog(
            onDismiss = { showAddEventDialog = false },
            onAdd = { title, desc ->
                viewModel.addGoogleCalendarEvent(title, desc, System.currentTimeMillis() + 86400000L)
                showAddEventDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("Rent Bill Reminder") }
    var description by remember { mutableStateOf("Monthly house rent payment due") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Google Calendar Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(title, description) }) {
                Text("Sync to Google Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
