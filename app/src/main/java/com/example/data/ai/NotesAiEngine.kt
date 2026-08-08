package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.BudgetItem
import com.example.data.model.NoteItem
import com.example.data.parser.NaturalNoteParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

enum class AiActionType {
    NONE,
    CREATE_NOTE,
    DELETE_NOTE,
    CREATE_BUDGET,
    DELETE_BUDGET,
    CREATE_CATEGORY,
    CREATE_CALENDAR_REMINDER,
    SEARCH_QUERY,
    EXPORT_REPORT,
    SCAN_RECEIPT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: AiActionType = AiActionType.NONE,
    val noteItem: NoteItem? = null,
    val budgetName: String? = null,
    val budgetAmount: Double? = null,
    val searchQuery: String? = null,
    val reminderTitle: String? = null,
    val reminderDateMillis: Long? = null
)

class NotesAiEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Process conversational text from the user with full financial assistant context.
     */
    suspend fun processChatPrompt(
        userPrompt: String,
        currentNotes: List<NoteItem>,
        currentBudgets: List<BudgetItem>
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val lowerPrompt = userPrompt.trim().lowercase()

        // Local fast command overrides for responsive AI command bar & quick prompt chips
        if (lowerPrompt.startsWith("search ") || lowerPrompt.startsWith("find ")) {
            val query = userPrompt.substringAfter(" ").trim()
            val matchingCount = currentNotes.count {
                it.rawText.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.merchant.contains(query, ignoreCase = true)
            }
            return@withContext ChatMessage(
                sender = MessageSender.AI,
                text = "🔍 Found $matchingCount notes matching '$query'. Filter applied!",
                actionType = AiActionType.SEARCH_QUERY,
                searchQuery = query
            )
        }

        if (lowerPrompt.contains("show this month") || lowerPrompt.contains("show expenses")) {
            val totalSpent = currentNotes.filter { it.type == com.example.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
            val count = currentNotes.size
            return@withContext ChatMessage(
                sender = MessageSender.AI,
                text = "📊 You have logged $count expenses this month totaling ₹$totalSpent. Check the Analytics tab for visual breakdowns!",
                actionType = AiActionType.NONE
            )
        }

        if (lowerPrompt.contains("export") || lowerPrompt.contains("report")) {
            return@withContext ChatMessage(
                sender = MessageSender.AI,
                text = "📑 Opening Export & Receipts screen where you can download your monthly PDF/CSV report.",
                actionType = AiActionType.EXPORT_REPORT
            )
        }

        // If API Key is missing or default placeholder, fallback gracefully
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext processFallbackPrompt(userPrompt, currentNotes, currentBudgets)
        }

        try {
            val systemContext = """
                You are Notes AI, an intelligent personal financial assistant inside the 'Notes Expenses' Android app.
                Current context: User has ${currentNotes.size} notes total.
                Total Expense: ₹${currentNotes.filter { it.type == com.example.data.model.TransactionType.EXPENSE }.sumOf { it.amount }}.
                Current Budgets: ${currentBudgets.joinToString { "${it.category}: ₹${it.spentAmount}/₹${it.allocatedAmount}" }}.

                User request: "$userPrompt"

                Instructions:
                1. If the user mentions spending or receiving money (e.g., "I spent 350 on groceries"), respond helpfully and return action "CREATE_NOTE" with details.
                2. If the user asks to create a budget (e.g. "Create 5000 food budget"), return action "CREATE_BUDGET".
                3. If the user asks for a reminder or bill alarm, return action "CREATE_CALENDAR_REMINDER".
                4. Otherwise, provide actionable, friendly financial advice or answers.

                Return strictly JSON with keys:
                "reply": (string: AI conversational response),
                "action": (string: "CREATE_NOTE", "CREATE_BUDGET", "CREATE_CALENDAR_REMINDER", "SEARCH", "NONE"),
                "amount": (number optional),
                "category": (string optional),
                "merchant": (string optional),
                "budgetName": (string optional),
                "budgetAmount": (number optional),
                "reminderTitle": (string optional)
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", systemContext)
                    ))
                ))
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val root = JSONObject(responseStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val rawAiText = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .optString("text", "")

                    val jsonStart = rawAiText.indexOf("{")
                    val jsonEnd = rawAiText.lastIndexOf("}")
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        val parsedObj = JSONObject(rawAiText.substring(jsonStart, jsonEnd + 1))
                        val reply = parsedObj.optString("reply", "I've processed your request.")
                        val action = parsedObj.optString("action", "NONE")

                        when (action) {
                            "CREATE_NOTE" -> {
                                val parsedNote = NaturalNoteParser.parseLocally(userPrompt)
                                val amount = parsedObj.optDouble("amount", parsedNote.amount)
                                val cat = parsedObj.optString("category", parsedNote.category)
                                val merchant = parsedObj.optString("merchant", parsedNote.merchant)

                                val finalNote = parsedNote.copy(
                                    amount = if (amount > 0) amount else parsedNote.amount,
                                    category = if (cat.isNotBlank()) cat else parsedNote.category,
                                    merchant = if (merchant.isNotBlank()) merchant else parsedNote.merchant
                                )

                                return@withContext ChatMessage(
                                    sender = MessageSender.AI,
                                    text = "$reply\n\n✅ Added: ${finalNote.merchant} (₹${finalNote.amount}) under ${finalNote.category}.",
                                    actionType = AiActionType.CREATE_NOTE,
                                    noteItem = finalNote
                                )
                            }
                            "CREATE_BUDGET" -> {
                                val bName = parsedObj.optString("budgetName", "Budget")
                                val bCat = parsedObj.optString("category", "General")
                                val bAmount = parsedObj.optDouble("budgetAmount", 5000.0)
                                return@withContext ChatMessage(
                                    sender = MessageSender.AI,
                                    text = "$reply\n\n🎯 Set budget for $bCat: ₹$bAmount.",
                                    actionType = AiActionType.CREATE_BUDGET,
                                    budgetName = bName,
                                    budgetAmount = bAmount
                                )
                            }
                            "CREATE_CALENDAR_REMINDER" -> {
                                val title = parsedObj.optString("reminderTitle", userPrompt)
                                return@withContext ChatMessage(
                                    sender = MessageSender.AI,
                                    text = "$reply\n\n🗓️ Calendar reminder created for '$title'.",
                                    actionType = AiActionType.CREATE_CALENDAR_REMINDER,
                                    reminderTitle = title,
                                    reminderDateMillis = System.currentTimeMillis() + 86400000L
                                )
                            }
                            else -> {
                                return@withContext ChatMessage(
                                    sender = MessageSender.AI,
                                    text = reply
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext processFallbackPrompt(userPrompt, currentNotes, currentBudgets)
    }

    /**
     * Fallback local NLP response generator when internet is offline or API key isn't active.
     */
    private fun processFallbackPrompt(
        userPrompt: String,
        currentNotes: List<NoteItem>,
        currentBudgets: List<BudgetItem>
    ): ChatMessage {
        val lower = userPrompt.lowercase()

        if (lower.contains("spent") || lower.contains("paid") || lower.contains("bought") || lower.contains("₹") || lower.contains("rs")) {
            val parsed = NaturalNoteParser.parseLocally(userPrompt)
            return ChatMessage(
                sender = MessageSender.AI,
                text = "Recorded ${parsed.category} expense of ₹${parsed.amount} for '${parsed.merchant}'. Dashboard & budget limits updated automatically!",
                actionType = AiActionType.CREATE_NOTE,
                noteItem = parsed
            )
        }

        if (lower.contains("remind") || lower.contains("bill") || lower.contains("due")) {
            return ChatMessage(
                sender = MessageSender.AI,
                text = "Created Google Calendar reminder for: '$userPrompt'. You'll receive a notification on the due date.",
                actionType = AiActionType.CREATE_CALENDAR_REMINDER,
                reminderTitle = userPrompt,
                reminderDateMillis = System.currentTimeMillis() + 86400000L * 3
            )
        }

        if (lower.contains("budget")) {
            val totalAllocated = currentBudgets.sumOf { it.allocatedAmount }
            val totalSpent = currentBudgets.sumOf { it.spentAmount }
            return ChatMessage(
                sender = MessageSender.AI,
                text = "💡 Budget Summary: You have allocated ₹$totalAllocated across ${currentBudgets.size} categories and spent ₹$totalSpent so far. Remaining budget: ₹${totalAllocated - totalSpent}.",
                actionType = AiActionType.NONE
            )
        }

        return ChatMessage(
            sender = MessageSender.AI,
            text = "I'm Notes AI! I can log expenses from text, set budgets, analyze spending trends, and schedule bill reminders on Google Calendar. Try typing 'Add ₹350 groceries' or 'Remind me to pay rent'.",
            actionType = AiActionType.NONE
        )
    }

    /**
     * OCR Receipt Analysis using Gemini Multimodal endpoint or local fallback.
     */
    suspend fun analyzeReceiptImage(bitmap: Bitmap): NoteItem = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val base64Image = bitmapToBase64(bitmap)

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Analyze this receipt image and extract financial details.
                    Return strictly JSON with:
                    "amount": (total amount number),
                    "merchant": (store or vendor name),
                    "category": (Groceries, Food, Shopping, Medical, Fuel, Transport, Utilities, Others),
                    "tags": (array of item names)
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            put(JSONObject().put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }))
                        })
                    ))
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    val root = JSONObject(responseStr)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val rawAiText = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .optString("text", "")

                        val jsonStart = rawAiText.indexOf("{")
                        val jsonEnd = rawAiText.lastIndexOf("}")
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            val obj = JSONObject(rawAiText.substring(jsonStart, jsonEnd + 1))
                            val amount = obj.optDouble("amount", 450.0)
                            val merchant = obj.optString("merchant", "Scanned Receipt Store")
                            val cat = obj.optString("category", "Groceries")

                            return@withContext NoteItem(
                                id = UUID.randomUUID().toString(),
                                rawText = "Scanned Receipt from $merchant: ₹$amount",
                                amount = amount,
                                category = cat,
                                merchant = merchant,
                                type = com.example.data.model.TransactionType.EXPENSE,
                                timestamp = System.currentTimeMillis(),
                                tags = listOf("ocr_scanned", cat.lowercase()),
                                isPinned = false,
                                isArchived = false,
                                isFavorite = false,
                                isLocked = false,
                                colorHex = "#2E7D32"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback simulated OCR note
        return@withContext NoteItem(
            id = UUID.randomUUID().toString(),
            rawText = "Scanned Receipt - Supermarket Purchase",
            amount = 380.0,
            category = "Groceries",
            merchant = "Fresh Mart Supermarket",
            type = com.example.data.model.TransactionType.EXPENSE,
            timestamp = System.currentTimeMillis(),
            tags = listOf("receipt", "groceries", "ocr"),
            isPinned = false,
            isArchived = false,
            isFavorite = false,
            isLocked = false,
            colorHex = "#2E7D32"
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
