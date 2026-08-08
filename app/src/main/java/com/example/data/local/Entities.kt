package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val rawText: String = "",
    val amount: Double = 0.0,
    val category: String = "Personal",
    val merchant: String = "",
    val type: String = "EXPENSE", // EXPENSE or INCOME
    val timestamp: Long = System.currentTimeMillis(),
    val tagsCsv: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val colorHex: String = "#FFFFFF",
    val imageUri: String? = null,
    val isChecklist: Boolean = false,
    val checklistJson: String = "",
    val isTrash: Boolean = false,
    val deletedTimestamp: Long = 0L
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val period: String,
    val startDate: Long,
    val endDate: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean,
    val budgetLimit: Double
)
