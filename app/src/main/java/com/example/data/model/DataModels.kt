package com.example.data.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

data class NoteItem(
    val id: String,
    val rawText: String,
    val amount: Double,
    val category: String,
    val merchant: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val colorHex: String = "#303F9F"
)

data class BudgetItem(
    val id: String,
    val name: String,
    val category: String, // "All" or specific category name
    val allocatedAmount: Double,
    val spentAmount: Double = 0.0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
)

data class CategoryItem(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val budgetLimit: Double = 0.0
)

data class UserSession(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isAuthenticated: Boolean = false,
    val isEmailVerified: Boolean = false,
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val dynamicColor: Boolean = true,
    val biometricEnabled: Boolean = false,
    val pinLockEnabled: Boolean = false,
    val userPin: String = "",
    val currencySymbol: String = "₹",
    val supabaseSyncEnabled: Boolean = true
)

data class ReceiptItem(
    val id: String,
    val noteId: String,
    val merchant: String,
    val amount: Double,
    val category: String,
    val dateTimestamp: Long,
    val itemsCount: Int = 1,
    val taxAmount: Double = 0.0
)

data class AiInsight(
    val summary: String,
    val topSpendingCategory: String,
    val predictionText: String,
    val savingsAdvice: String,
    val monthlyHealthScore: Int // 0-100
)
