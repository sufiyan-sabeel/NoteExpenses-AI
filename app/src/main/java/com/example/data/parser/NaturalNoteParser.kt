package com.example.data.parser

import com.example.BuildConfig
import com.example.data.model.NoteItem
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.regex.Pattern

object NaturalNoteParser {

    private val categoryMap = mapOf(
        "Groceries" to listOf("milk", "vegetable", "vegetables", "tomato", "tomatoes", "fruit", "fruits", "apple", "bread", "egg", "eggs", "rice", "butter", "grocery", "groceries", "supermarket", "mart", "cheese", "chicken", "meat", "fish"),
        "Utilities" to listOf("electricity", "bill", "bills", "water", "wifi", "internet", "gas", "recharge", "mobile", "broadband", "power", "utility"),
        "Transport" to listOf("taxi", "cab", "uber", "ola", "bus", "metro", "train", "flight", "ticket", "autorickshaw", "auto", "commute"),
        "Fuel" to listOf("fuel", "petrol", "diesel", "gas station", "gasoline", "cng"),
        "Shopping" to listOf("clothes", "clothing", "shoes", "amazon", "flipkart", "dress", "shirt", "pants", "mall", "watch", "electronics", "laptop", "gadget"),
        "Medical" to listOf("doctor", "medicine", "medicines", "pharmacy", "hospital", "lab", "clinic", "pills", "health", "dental", "checkup"),
        "Rent" to listOf("rent", "house rent", "room rent", "flat rent", "office rent"),
        "Food" to listOf("pizza", "burger", "restaurant", "cafe", "coffee", "tea", "dinner", "lunch", "breakfast", "snack", "snacks", "zomato", "swiggy", "food", "eatout", "bakery"),
        "Entertainment" to listOf("movie", "cinema", "netflix", "spotify", "concert", "game", "gaming", "show", "event", "party", "pub"),
        "Education" to listOf("book", "books", "fee", "fees", "course", "college", "school", "tuition", "udemy", "class", "exam"),
        "Travel" to listOf("hotel", "trip", "resort", "vacation", "tour", "airbnb", "stay"),
        "Investment" to listOf("stock", "stocks", "shares", "crypto", "mutual fund", "sip", "gold", "fd", "bond", "asset"),
        "Income" to listOf("salary", "freelance", "client", "dividend", "bonus", "cashback", "refund", "interest", "income", "stipend", "paycheck"),
        "Savings" to listOf("savings", "deposit", "piggy bank", "reserve")
    )

    fun parseLocally(text: String): NoteItem {
        val lower = text.lowercase().trim()

        // 1. Extract Amount
        var amount = 0.0
        // Match ₹250, $250, 250 rs, 250.50, ₹ 250, etc.
        val amountRegex = Pattern.compile("(?:[₹$€£]|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rs\\.?|inr|rupees)?", Pattern.CASE_INSENSITIVE)
        val matcher = amountRegex.matcher(lower)

        // Find the most likely amount
        var foundAmount = false
        while (matcher.find()) {
            val matchedGroup = matcher.group(1)
            if (matchedGroup != null) {
                val parsed = matchedGroup.toDoubleOrNull() ?: 0.0
                if (parsed > 0) {
                    amount = parsed
                    foundAmount = true
                    break
                }
            }
        }

        // 2. Determine Category & Type
        var selectedCategory = "Others"
        var isIncome = lower.contains("salary") || lower.contains("income") || lower.contains("freelance") || lower.contains("refund") || lower.contains("received") || lower.contains("credited")

        for ((cat, keywords) in categoryMap) {
            if (keywords.any { lower.contains(it) }) {
                selectedCategory = cat
                if (cat == "Income") isIncome = true
                break
            }
        }

        // 3. Extract Merchant / Clean title
        // Remove amount tokens to clean title
        var cleanMerchant = text
            .replace(Regex("(?:[₹$€£]|rs\\.?|inr)?\\s*\\d+(?:\\.\\d+)?\\s*(?:rs\\.?|inr|rupees)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(bought|paid|spent|got|received|for|at|bill)\\b", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanMerchant.isEmpty()) {
            cleanMerchant = selectedCategory
        } else {
            // Capitalize first letter
            cleanMerchant = cleanMerchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // 4. Extract Tags
        val tags = mutableListOf<String>()
        // Match explicit #tags
        val hashTagMatcher = Pattern.compile("#(\\w+)").matcher(text)
        while (hashTagMatcher.find()) {
            hashTagMatcher.group(1)?.let { tags.add(it) }
        }

        if (tags.isEmpty()) {
            // Add automatic category/item tag
            tags.add(selectedCategory.lowercase())
            if (cleanMerchant.length in 3..20 && !cleanMerchant.equals(selectedCategory, ignoreCase = true)) {
                tags.add(cleanMerchant.lowercase().replace(" ", "_"))
            }
        }

        // Color mapping per category
        val colorHex = when (selectedCategory) {
            "Groceries" -> "#2E7D32"
            "Utilities" -> "#D84315"
            "Transport" -> "#0277BD"
            "Fuel" -> "#E65100"
            "Shopping" -> "#8E24AA"
            "Medical" -> "#C62828"
            "Rent" -> "#455A64"
            "Food" -> "#F57C00"
            "Entertainment" -> "#6A1B9A"
            "Education" -> "#1565C0"
            "Travel" -> "#00838F"
            "Investment" -> "#00695C"
            "Income" -> "#1B5E20"
            "Savings" -> "#004D40"
            else -> "#3F51B5"
        }

        return NoteItem(
            id = UUID.randomUUID().toString(),
            rawText = text,
            amount = amount,
            category = selectedCategory,
            merchant = cleanMerchant,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
            timestamp = System.currentTimeMillis(),
            tags = tags,
            isPinned = false,
            isArchived = false,
            isFavorite = false,
            isLocked = false,
            colorHex = colorHex
        )
    }

    suspend fun parseWithAiOrLocal(text: String): NoteItem = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext parseLocally(text)
        }

        try {
            val client = OkHttpClient()
            val prompt = """
                Extract financial details from this text note: "$text"
                Respond in strictly valid JSON format with keys:
                "amount": (number),
                "category": (string: Groceries, Utilities, Transport, Fuel, Shopping, Medical, Rent, Food, Entertainment, Education, Travel, Investment, Income, Savings, Others),
                "merchant": (string title),
                "type": (string: EXPENSE or INCOME),
                "tags": (array of strings)
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(
                        JSONObject().put("text", prompt)
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

                    // Sanitize JSON response
                    val jsonStart = rawAiText.indexOf("{")
                    val jsonEnd = rawAiText.lastIndexOf("}")
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        val parsedObj = JSONObject(rawAiText.substring(jsonStart, jsonEnd + 1))
                        val localResult = parseLocally(text)

                        val amount = parsedObj.optDouble("amount", localResult.amount)
                        val category = parsedObj.optString("category", localResult.category)
                        val merchant = parsedObj.optString("merchant", localResult.merchant)
                        val typeStr = parsedObj.optString("type", localResult.type.name)

                        val tagsArray = parsedObj.optJSONArray("tags")
                        val tagsList = mutableListOf<String>()
                        if (tagsArray != null) {
                            for (i in 0 until tagsArray.length()) {
                                tagsList.add(tagsArray.getString(i))
                            }
                        } else {
                            tagsList.addAll(localResult.tags)
                        }

                        return@withContext localResult.copy(
                            amount = if (amount > 0) amount else localResult.amount,
                            category = if (category.isNotBlank()) category else localResult.category,
                            merchant = if (merchant.isNotBlank()) merchant else localResult.merchant,
                            type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                            tags = if (tagsList.isNotEmpty()) tagsList else localResult.tags
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to local parser
        return@withContext parseLocally(text)
    }
}
