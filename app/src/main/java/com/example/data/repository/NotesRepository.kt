package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.model.ChecklistItem
import com.example.data.model.NoteItem
import com.example.data.model.TransactionType
import com.example.data.parser.NaturalNoteParser
import com.example.data.remote.SupabaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepository(
    private val noteDao: NoteDao,
    private val supabaseManager: SupabaseManager
) {

    val allNotesFlow: Flow<List<NoteItem>> = noteDao.getAllNotes().map { list ->
        list.map { entityToModel(it) }
    }

    val activeNotesFlow: Flow<List<NoteItem>> = noteDao.getActiveNotes().map { list ->
        list.map { entityToModel(it) }
    }

    val archivedNotesFlow: Flow<List<NoteItem>> = noteDao.getArchivedNotes().map { list ->
        list.map { entityToModel(it) }
    }

    val trashNotesFlow: Flow<List<NoteItem>> = noteDao.getTrashNotes().map { list ->
        list.map { entityToModel(it) }
    }

    suspend fun parseAndAddNote(rawText: String): NoteItem {
        val parsedNote = NaturalNoteParser.parseWithAiOrLocal(rawText)
        saveNote(parsedNote)
        return parsedNote
    }

    suspend fun saveNote(note: NoteItem) {
        val entity = modelToEntity(note)
        noteDao.insertNote(entity)
    }

    suspend fun moveToTrash(id: String) {
        noteDao.moveToTrash(id)
    }

    suspend fun restoreFromTrash(id: String) {
        noteDao.restoreFromTrash(id)
    }

    suspend fun deleteNotePermanently(id: String) {
        noteDao.deleteNoteById(id)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun syncWithSupabase(notes: List<NoteItem>): Result<Int> {
        return supabaseManager.syncNotesToSupabase(notes)
    }

    private fun serializeChecklist(items: List<ChecklistItem>): String {
        return items.joinToString("\n") { "${if (it.isDone) "1" else "0"}|${it.id}|${it.text.replace("|", " ")}" }
    }

    private fun deserializeChecklist(raw: String): List<ChecklistItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                ChecklistItem(
                    id = parts[1],
                    isDone = parts[0] == "1",
                    text = parts.subList(2, parts.size).joinToString("|")
                )
            } else if (parts.size == 2) {
                ChecklistItem(
                    isDone = parts[0] == "1",
                    text = parts[1]
                )
            } else null
        }
    }

    private fun entityToModel(e: NoteEntity): NoteItem {
        return NoteItem(
            id = e.id,
            title = e.title,
            rawText = e.rawText,
            amount = e.amount,
            category = e.category,
            merchant = e.merchant,
            type = if (e.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
            timestamp = e.timestamp,
            tags = if (e.tagsCsv.isBlank()) emptyList() else e.tagsCsv.split(","),
            isPinned = e.isPinned,
            isArchived = e.isArchived,
            isFavorite = e.isFavorite,
            isLocked = e.isLocked,
            colorHex = e.colorHex,
            imageUri = e.imageUri,
            isChecklist = e.isChecklist,
            checklistItems = deserializeChecklist(e.checklistJson),
            isTrash = e.isTrash,
            deletedTimestamp = e.deletedTimestamp
        )
    }

    private fun modelToEntity(m: NoteItem): NoteEntity {
        return NoteEntity(
            id = m.id,
            title = m.title,
            rawText = m.rawText,
            amount = m.amount,
            category = m.category,
            merchant = m.merchant,
            type = m.type.name,
            timestamp = m.timestamp,
            tagsCsv = m.tags.joinToString(","),
            isPinned = m.isPinned,
            isArchived = m.isArchived,
            isFavorite = m.isFavorite,
            isLocked = m.isLocked,
            colorHex = m.colorHex,
            imageUri = m.imageUri,
            isChecklist = m.isChecklist,
            checklistJson = serializeChecklist(m.checklistItems),
            isTrash = m.isTrash,
            deletedTimestamp = m.deletedTimestamp
        )
    }

    suspend fun seedSampleNotesIfEmpty() {
        val samples = listOf(
            NaturalNoteParser.parseLocally("Bought vegetables ₹250"),
            NaturalNoteParser.parseLocally("Paid electricity bill ₹850"),
            NaturalNoteParser.parseLocally("Pizza ₹399"),
            NaturalNoteParser.parseLocally("Salary credited ₹45000"),
            NaturalNoteParser.parseLocally("Uber ride to office ₹180"),
            NaturalNoteParser.parseLocally("Fuel for car ₹1200")
        )
        samples.forEach { saveNote(it) }
    }
}
