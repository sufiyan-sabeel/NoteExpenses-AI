package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
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

    suspend fun parseAndAddNote(rawText: String): NoteItem {
        val parsedNote = NaturalNoteParser.parseWithAiOrLocal(rawText)
        saveNote(parsedNote)
        return parsedNote
    }

    suspend fun saveNote(note: NoteItem) {
        val entity = modelToEntity(note)
        noteDao.insertNote(entity)
    }

    suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    suspend fun syncWithSupabase(notes: List<NoteItem>): Result<Int> {
        return supabaseManager.syncNotesToSupabase(notes)
    }

    private fun entityToModel(e: NoteEntity): NoteItem {
        return NoteItem(
            id = e.id,
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
            colorHex = e.colorHex
        )
    }

    private fun modelToEntity(m: NoteItem): NoteEntity {
        return NoteEntity(
            id = m.id,
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
            colorHex = m.colorHex
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
