package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val rawText: String,
    val amount: Double,
    val category: String,
    val merchant: String,
    val type: String, // EXPENSE or INCOME
    val timestamp: Long,
    val tagsCsv: String,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val colorHex: String
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
