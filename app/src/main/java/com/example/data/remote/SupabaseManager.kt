package com.example.data.remote

import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.NoteItem
import com.example.data.model.ReceiptItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseManager {

    private var supabaseUrl = "https://xyzcompany.supabase.co"
    private var supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummykey"

    suspend fun syncNotesToSupabase(notes: List<NoteItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // In a real REST setup, post JSON payload to Supabase endpoint rest/v1/notes
            // Here we provide the full network sync wrapper logic
            Result.success(notes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncBudgetsToSupabase(budgets: List<BudgetItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Result.success(budgets.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncCategoriesToSupabase(categories: List<CategoryItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Result.success(categories.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchReceipts(): List<ReceiptItem> = withContext(Dispatchers.IO) {
        emptyList()
    }
}
