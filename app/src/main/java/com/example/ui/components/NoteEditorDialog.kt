package com.example.ui.components

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.ChecklistItem
import com.example.data.model.NoteItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val NOTE_BACKGROUND_COLORS = listOf(
    "#FFFFFF" to "White",
    "#FFF8E1" to "Warm Amber",
    "#E8F5E9" to "Soft Mint",
    "#E3F2FD" to "Sky Blue",
    "#F3E5F5" to "Lavender",
    "#FFEBEE" to "Blush Red",
    "#E0F2F1" to "Teal",
    "#FFF3E0" to "Peach",
    "#EFEBE9" to "Sand",
    "#F5F5F5" to "Slate Grey"
)

val DEFAULT_NOTE_CATEGORIES = listOf(
    "Personal", "Work", "Study", "Ideas", "Shopping", "Travel", "Groceries", "Finance"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorDialog(
    initialNote: NoteItem? = null,
    availableCategories: List<String> = DEFAULT_NOTE_CATEGORIES,
    onDismissRequest: () -> Unit,
    onSaveNote: (NoteItem) -> Unit,
    onDeleteNote: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.rawText ?: "") }
    var selectedCategory by remember { mutableStateOf(initialNote?.category ?: "Personal") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(initialNote?.isFavorite ?: false) }
    var isLocked by remember { mutableStateOf(initialNote?.isLocked ?: false) }
    var selectedColorHex by remember { mutableStateOf(initialNote?.colorHex ?: "#FFFFFF") }
    var imageUri by remember { mutableStateOf(initialNote?.imageUri) }
    var isChecklist by remember { mutableStateOf(initialNote?.isChecklist ?: false) }
    var checklistItems by remember {
        mutableStateOf(
            initialNote?.checklistItems?.ifEmpty {
                listOf(ChecklistItem(text = ""))
            } ?: listOf(ChecklistItem(text = ""))
        )
    }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Speech to text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                content = if (content.isBlank()) spokenText else "$content\n$spokenText"
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    // Auto-save calculated note state
    val currentNoteState = remember(title, content, selectedCategory, isPinned, isFavorite, isLocked, selectedColorHex, imageUri, isChecklist, checklistItems) {
        NoteItem(
            id = initialNote?.id ?: UUID.randomUUID().toString(),
            title = title,
            rawText = content,
            amount = initialNote?.amount ?: 0.0,
            category = selectedCategory,
            merchant = initialNote?.merchant ?: "",
            type = initialNote?.type ?: com.example.data.model.TransactionType.EXPENSE,
            timestamp = initialNote?.timestamp ?: System.currentTimeMillis(),
            tags = initialNote?.tags ?: emptyList(),
            isPinned = isPinned,
            isArchived = initialNote?.isArchived ?: false,
            isFavorite = isFavorite,
            isLocked = isLocked,
            colorHex = selectedColorHex,
            imageUri = imageUri,
            isChecklist = isChecklist,
            checklistItems = checklistItems.filter { it.text.isNotBlank() },
            isTrash = initialNote?.isTrash ?: false,
            deletedTimestamp = initialNote?.deletedTimestamp ?: 0L
        )
    }

    // Text stats calculation
    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(content) { content.length }
    val readingTimeMin = remember(wordCount) {
        val min = (wordCount / 200.0).toInt()
        if (min < 1) 1 else min
    }

    val dialogBgColor = try {
        Color(android.graphics.Color.parseColor(selectedColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surface
    }

    Dialog(
        onDismissRequest = {
            onSaveNote(currentNoteState)
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("note_editor_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = dialogBgColor,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onSaveNote(currentNoteState)
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Save and Back")
                    }

                    // Category Selector Chip
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { showCategoryMenu = true },
                            label = { Text(selectedCategory, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.testTag("editor_category_chip")
                        )

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            val categoriesToDisplay = (DEFAULT_NOTE_CATEGORIES + availableCategories).distinct()
                            categoriesToDisplay.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin Toggle
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Favorite Toggle
                        IconButton(onClick = { isFavorite = !isFavorite }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite Note",
                                tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Voice Speech Button
                        IconButton(onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to dictate note...")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                // Speech recognition not available
                            }
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Dictation", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Save Done Button
                        Button(
                            onClick = {
                                onSaveNote(currentNoteState)
                                onDismissRequest()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("editor_save_btn")
                        ) {
                            Text("Save")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Background Colors Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text("Color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    items(NOTE_BACKGROUND_COLORS) { (hex, name) ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedColorHex == hex) 2.5.dp else 1.dp,
                                    color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Note Title", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().testTag("editor_title_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Rich Formatting / Mode Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Toggle: Text vs Checklist
                    FilterChip(
                        selected = !isChecklist,
                        onClick = { isChecklist = false },
                        label = { Text("Note") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = isChecklist,
                        onClick = { isChecklist = true },
                        label = { Text("Checklist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Formatting actions for text mode
                    if (!isChecklist) {
                        IconButton(onClick = { content += " **Bold** " }) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold Text")
                        }
                        IconButton(onClick = { content += " *Italic* " }) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic Text")
                        }
                        IconButton(onClick = { content = if (content.isBlank()) "• " else "$content\n• " }) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Bullet Point")
                        }
                    }

                    // Attach image button
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach Image", tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Image Attachment Display (if present)
                if (!imageUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Note Image Attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main Body: Standard Note Text or Checklist Editor
                Box(modifier = Modifier.weight(1f)) {
                    if (!isChecklist) {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("Start typing your note here...") },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("editor_content_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        // Checklist View
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(checklistItems) { index, item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = item.isDone,
                                            onCheckedChange = { isChecked ->
                                                val updated = checklistItems.toMutableList()
                                                updated[index] = item.copy(isDone = isChecked)
                                                checklistItems = updated
                                            }
                                        )
                                        OutlinedTextField(
                                            value = item.text,
                                            onValueChange = { txt ->
                                                val updated = checklistItems.toMutableList()
                                                updated[index] = item.copy(text = txt)
                                                checklistItems = updated
                                            },
                                            placeholder = { Text("Checklist item...") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = if (item.isDone) {
                                                MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                                            } else {
                                                MaterialTheme.typography.bodyLarge
                                            }
                                        )
                                        IconButton(onClick = {
                                            if (checklistItems.size > 1) {
                                                val updated = checklistItems.toMutableList()
                                                updated.removeAt(index)
                                                checklistItems = updated
                                            }
                                        }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    checklistItems = checklistItems + ChecklistItem(text = "")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Item")
                            }
                        }
                    }
                }

                // Footer Info Bar: Word Count | Char Count | Reading Time | Last Edited
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount words  •  $charCount chars  •  ~$readingTimeMin min read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (onDeleteNote != null && initialNote != null) {
                        TextButton(
                            onClick = {
                                onDeleteNote(initialNote.id)
                                onDismissRequest()
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
