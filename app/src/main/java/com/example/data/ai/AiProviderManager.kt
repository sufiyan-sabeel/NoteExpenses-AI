package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

enum class AiProvider(
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String
) {
    GEMINI("Google Gemini API", "gemini-3.5-flash", "https://generativelanguage.googleapis.com/v1beta"),
    OPENAI("OpenAI API", "gpt-4o-mini", "https://api.openai.com/v1"),
    OPENROUTER("OpenRouter API", "google/gemini-2.5-flash", "https://openrouter.ai/api/v1"),
    CLAUDE("Anthropic Claude API", "claude-3-5-haiku-20241022", "https://api.anthropic.com/v1"),
    LOCAL_AI("Local AI (Ollama/LocalHost)", "llama3.2", "http://10.0.2.2:11434/v1")
}

data class AiProviderConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val modelName: String = provider.defaultModel,
    val baseUrl: String = provider.defaultBaseUrl
)

class AiProviderManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("encrypted_ai_prefs", Context.MODE_PRIVATE)

    var activeProvider: AiProvider
        get() {
            val saved = prefs.getString("selected_ai_provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
            return try {
                AiProvider.valueOf(saved)
            } catch (e: Exception) {
                AiProvider.GEMINI
            }
        }
        set(value) {
            prefs.edit().putString("selected_ai_provider", value.name).apply()
        }

    fun getApiKeyForProvider(provider: AiProvider): String {
        val savedKey = prefs.getString("api_key_${provider.name}", "") ?: ""
        if (savedKey.isNotBlank()) return savedKey
        // Fallback to BuildConfig GEMINI_API_KEY if Gemini provider is selected
        return if (provider == AiProvider.GEMINI && BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
            BuildConfig.GEMINI_API_KEY
        } else {
            ""
        }
    }

    fun setApiKeyForProvider(provider: AiProvider, apiKey: String) {
        prefs.edit().putString("api_key_${provider.name}", apiKey.trim()).apply()
    }

    fun getModelForProvider(provider: AiProvider): String {
        return prefs.getString("model_${provider.name}", provider.defaultModel) ?: provider.defaultModel
    }

    fun setModelForProvider(provider: AiProvider, model: String) {
        prefs.edit().putString("model_${provider.name}", model.trim()).apply()
    }

    fun getBaseUrlForProvider(provider: AiProvider): String {
        return prefs.getString("base_url_${provider.name}", provider.defaultBaseUrl) ?: provider.defaultBaseUrl
    }

    fun setBaseUrlForProvider(provider: AiProvider, url: String) {
        prefs.edit().putString("base_url_${provider.name}", url.trim()).apply()
    }

    fun getCurrentConfig(): AiProviderConfig {
        val prov = activeProvider
        return AiProviderConfig(
            provider = prov,
            apiKey = getApiKeyForProvider(prov),
            modelName = getModelForProvider(prov),
            baseUrl = getBaseUrlForProvider(prov)
        )
    }
}
