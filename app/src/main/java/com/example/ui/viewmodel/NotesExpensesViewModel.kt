package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.*
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.FirebaseAuthManager
import com.example.data.remote.GoogleCalendarManager
import com.example.data.remote.SupabaseManager
import com.example.data.repository.BudgetRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.NotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesExpensesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val supabaseManager = SupabaseManager()
    val authManager = FirebaseAuthManager()
    val calendarManager = GoogleCalendarManager(application)

    val aiProviderManager = AiProviderManager(application)
    val aiRepository = AIRepository(aiProviderManager)
    val automationEngine = AutomationEngine()
    val voiceAssistantHelper = VoiceAssistantHelper(application)

    val notesRepository = NotesRepository(db.noteDao(), supabaseManager)
    val budgetRepository = BudgetRepository(db.budgetDao())
    val categoryRepository = CategoryRepository(db.categoryDao())

    // State flows
    val userSession: StateFlow<UserSession> = authManager.currentUserState

    val allNotes: StateFlow<List<NoteItem>> = notesRepository.allNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteItem>> = notesRepository.activeNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteItem>> = notesRepository.archivedNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<NoteItem>> = notesRepository.trashNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetItem>> = budgetRepository.budgetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryItem>> = categoryRepository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Theme & App Settings
    private val _themeMode = MutableStateFlow("SYSTEM") // LIGHT, DARK, SYSTEM
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _fontSizeOption = MutableStateFlow("MEDIUM") // SMALL, MEDIUM, LARGE
    val fontSizeOption: StateFlow<String> = _fontSizeOption.asStateFlow()

    private val _sortOption = MutableStateFlow(NoteSortOption.NEWEST)
    val sortOption: StateFlow<NoteSortOption> = _sortOption.asStateFlow()

    private var lastDeletedNote: NoteItem? = null

    // AI Chat & Voice State
    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Hello! I'm Notes AI assistant powered by ${aiProviderManager.activeProvider.displayName}. Type or speak an expense like 'I spent ₹350 on groceries', analyze budgets, or scan receipts!"
            )
        )
    )
    val aiChatMessages: StateFlow<List<ChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _totalTokenCount = MutableStateFlow(120)
    val totalTokenCount: StateFlow<Int> = _totalTokenCount.asStateFlow()

    private var currentStreamJob: Job? = null

    private val _isTtsMuted = MutableStateFlow(false)
    val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

    // Automation Engine State
    private val _automationRules = MutableStateFlow(automationEngine.rules)
    val automationRules: StateFlow<List<AutomationRule>> = _automationRules.asStateFlow()

    private val _automationLogs = MutableStateFlow(automationEngine.logs)
    val automationLogs: StateFlow<List<AutomationLog>> = _automationLogs.asStateFlow()

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
            notesRepository.seedSampleNotesIfEmpty()
            budgetRepository.seedDefaultBudgetsIfEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    // AI Chat Interaction with Streaming & Actions
    fun executeAiCommand(command: String) {
        sendAiChatMessage(command)
    }

    fun sendAiChatMessage(
        userPrompt: String,
        enableSearchGrounding: Boolean = false,
        enableMapsGrounding: Boolean = false,
        enableHighThinking: Boolean = false
    ) {
        if (userPrompt.isBlank()) return

        currentStreamJob?.cancel()
        currentStreamJob = viewModelScope.launch {
            _isGenerating.value = true

            val userMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                sender = MessageSender.USER,
                text = userPrompt
            )
            _aiChatMessages.value = _aiChatMessages.value + userMsg

            val aiMsgId = java.util.UUID.randomUUID().toString()
            val initialAiMsg = ChatMessage(
                id = aiMsgId,
                sender = MessageSender.AI,
                text = "Thinking..."
            )
            _aiChatMessages.value = _aiChatMessages.value + initialAiMsg

            try {
                aiRepository.streamChatResponse(
                    userPrompt = userPrompt,
                    notes = allNotes.value,
                    budgets = budgets.value,
                    enableSearchGrounding = enableSearchGrounding,
                    enableMapsGrounding = enableMapsGrounding,
                    enableHighThinking = enableHighThinking
                ).collect { chunk ->
                    val updatedList = _aiChatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) {
                            msg.copy(
                                text = chunk.textChunk,
                                actionType = chunk.actionType,
                                noteItem = chunk.noteItem,
                                budgetName = chunk.budgetName,
                                budgetAmount = chunk.budgetAmount,
                                searchQuery = chunk.searchQuery,
                                reminderTitle = chunk.reminderTitle
                            )
                        } else {
                            msg
                        }
                    }
                    _aiChatMessages.value = updatedList
                    _totalTokenCount.value += chunk.estimatedTokens

                    if (chunk.isComplete) {
                        // Perform actions
                        when (chunk.actionType) {
                            AiActionType.CREATE_NOTE -> {
                                chunk.noteItem?.let { notesRepository.saveNote(it) }
                            }
                            AiActionType.CREATE_BUDGET -> {
                                if (chunk.budgetName != null && chunk.budgetAmount != null) {
                                    addBudget(chunk.budgetName, chunk.budgetName, chunk.budgetAmount)
                                }
                            }
                            AiActionType.CREATE_CALENDAR_REMINDER -> {
                                val title = chunk.reminderTitle ?: userPrompt
                                addGoogleCalendarEvent(title, "Reminder created by Notes AI", System.currentTimeMillis() + 86400000L)
                            }
                            AiActionType.SEARCH_QUERY -> {
                                chunk.searchQuery?.let { setSearchQuery(it) }
                            }
                            else -> {}
                        }

                        voiceAssistantHelper.speak(chunk.textChunk)
                        evaluateAutomationRules()
                    }
                }
            } catch (e: Exception) {
                val updatedList = _aiChatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(text = "Error interacting with AI provider: ${e.localizedMessage}")
                    } else {
                        msg
                    }
                }
                _aiChatMessages.value = updatedList
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        currentStreamJob?.cancel()
        _isGenerating.value = false
    }

    fun regenerateResponse() {
        val lastUserMsg = _aiChatMessages.value.lastOrNull { it.sender == MessageSender.USER }
        lastUserMsg?.let { sendAiChatMessage(it.text) }
    }

    fun copyToClipboard(text: String) {
        viewModelScope.launch {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Notes AI Text", text)
            clipboard.setPrimaryClip(clip)
            _snackbarMessage.emit("Copied message to clipboard")
        }
    }

    fun processReceiptScan(bitmap: Bitmap) {
        viewModelScope.launch {
            _snackbarMessage.emit("Analyzing receipt image with Notes AI OCR...")
            val note = aiRepository.analyzeReceiptImage(bitmap)
            notesRepository.saveNote(note)
            val aiMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                sender = MessageSender.AI,
                text = "📸 Receipt Scanned! Recorded ${note.merchant}: ₹${note.amount} under ${note.category}.",
                actionType = AiActionType.CREATE_NOTE,
                noteItem = note
            )
            _aiChatMessages.value = _aiChatMessages.value + aiMsg
            _snackbarMessage.emit("Receipt added: ${note.merchant} ₹${note.amount}")
        }
    }

    fun toggleTtsMute() {
        _isTtsMuted.value = !_isTtsMuted.value
        voiceAssistantHelper.isMuted = _isTtsMuted.value
        if (_isTtsMuted.value) voiceAssistantHelper.stop()
    }

    fun clearAiChat() {
        _aiChatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Chat history cleared. Active provider: ${aiProviderManager.activeProvider.displayName}. How can Notes AI assist you today?"
            )
        )
    }

    // Google Calendar Integration
    fun addGoogleCalendarEvent(title: String, description: String, dateMillis: Long) {
        viewModelScope.launch {
            val result = calendarManager.addCalendarEvent(
                title = title,
                description = description,
                startTimeMillis = dateMillis
            )
            if (result.isSuccess) {
                _snackbarMessage.emit("🗓️ Google Calendar Event Created: '$title'")
            } else {
                _snackbarMessage.emit("Google Calendar sync opened")
            }
        }
    }

    // Automation Rule Engine
    fun toggleAutomationRule(ruleId: String) {
        automationEngine.toggleRule(ruleId)
        _automationRules.value = automationEngine.rules
    }

    fun addCustomAutomationRule(name: String, condition: RuleCondition, action: RuleAction, value: Double) {
        val newRule = AutomationRule(
            name = name,
            condition = condition,
            action = action,
            thresholdValue = value
        )
        automationEngine.addRule(newRule)
        _automationRules.value = automationEngine.rules
        _automationLogs.value = automationEngine.logs
        viewModelScope.launch {
            _snackbarMessage.emit("Automation rule '$name' saved")
        }
    }

    fun deleteAutomationRule(ruleId: String) {
        automationEngine.deleteRule(ruleId)
        _automationRules.value = automationEngine.rules
    }

    private fun evaluateAutomationRules() {
        val alerts = automationEngine.evaluateNotesAndBudgets(allNotes.value, budgets.value)
        _automationLogs.value = automationEngine.logs
        if (alerts.isNotEmpty()) {
            viewModelScope.launch {
                _snackbarMessage.emit(alerts.first())
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun setFontSizeOption(size: String) {
        _fontSizeOption.value = size
    }

    fun setSortOption(sort: NoteSortOption) {
        _sortOption.value = sort
    }

    fun saveNoteItem(note: NoteItem) {
        viewModelScope.launch {
            notesRepository.saveNote(note)
            _snackbarMessage.emit("Note saved")
            evaluateAutomationRules()
        }
    }

    // Add note via natural language or parser
    fun addNoteFromRawText(rawText: String) {
        viewModelScope.launch {
            val note = notesRepository.parseAndAddNote(rawText)
            _snackbarMessage.emit("Added note under ${note.category}")
            evaluateAutomationRules()
        }
    }

    fun togglePin(note: NoteItem) {
        viewModelScope.launch {
            notesRepository.saveNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleArchive(note: NoteItem) {
        viewModelScope.launch {
            notesRepository.saveNote(note.copy(isArchived = !note.isArchived))
        }
    }

    fun toggleFavorite(note: NoteItem) {
        viewModelScope.launch {
            notesRepository.saveNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun toggleLock(note: NoteItem) {
        viewModelScope.launch {
            notesRepository.saveNote(note.copy(isLocked = !note.isLocked))
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch {
            lastDeletedNote = note
            notesRepository.moveToTrash(note.id)
            _snackbarMessage.emit("Moved note to Trash")
        }
    }

    fun undoDeleteNote() {
        val noteToRestore = lastDeletedNote ?: return
        viewModelScope.launch {
            notesRepository.restoreFromTrash(noteToRestore.id)
            lastDeletedNote = null
            _snackbarMessage.emit("Restored note: ${noteToRestore.title.ifBlank { noteToRestore.rawText.take(15) }}")
        }
    }

    fun restoreFromTrash(noteId: String) {
        viewModelScope.launch {
            notesRepository.restoreFromTrash(noteId)
            _snackbarMessage.emit("Restored note from Trash")
        }
    }

    fun deleteNotePermanently(noteId: String) {
        viewModelScope.launch {
            notesRepository.deleteNotePermanently(noteId)
            _snackbarMessage.emit("Note permanently deleted")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            notesRepository.emptyTrash()
            _snackbarMessage.emit("Trash emptied")
        }
    }

    fun shareNoteText(context: Context, note: NoteItem) {
        try {
            val titleStr = if (note.title.isNotBlank()) "${note.title}\n\n" else ""
            val contentStr = if (note.isChecklist) {
                note.checklistItems.joinToString("\n") { "${if (it.isDone) "✓ " else "☐ "}${it.text}" }
            } else note.rawText

            val shareBody = "$titleStr$contentStr\n\nCategory: ${note.category}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareBody)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
        } catch (e: Exception) {
            viewModelScope.launch { _snackbarMessage.emit("Error sharing note") }
        }
    }

    fun duplicateNote(note: NoteItem) {
        viewModelScope.launch {
            val copy = note.copy(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis()
            )
            notesRepository.saveNote(copy)
            _snackbarMessage.emit("Note duplicated")
        }
    }

    fun addBudget(name: String, category: String, amount: Double) {
        viewModelScope.launch {
            val b = BudgetItem(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                category = category,
                allocatedAmount = amount
            )
            budgetRepository.saveBudget(b)
            _snackbarMessage.emit("Budget added for $category")
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
            _snackbarMessage.emit("Budget removed")
        }
    }

    fun addCustomCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val c = CategoryItem(
                id = "cat_" + System.currentTimeMillis(),
                name = name,
                iconName = "label",
                colorHex = colorHex,
                isCustom = true
            )
            categoryRepository.addCategory(c)
            _snackbarMessage.emit("Category '$name' created")
        }
    }

    fun triggerSupabaseSync() {
        viewModelScope.launch {
            val result = notesRepository.syncWithSupabase(allNotes.value)
            if (result.isSuccess) {
                _snackbarMessage.emit("Synced successfully with Supabase!")
            } else {
                _snackbarMessage.emit("Supabase sync failed, check connectivity.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantHelper.shutdown()
    }
}
