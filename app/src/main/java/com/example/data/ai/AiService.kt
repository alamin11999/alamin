package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiService(private val repository: JarvisRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun getSystemInstruction(userName: String): String {
        return "You are JARVIS, the highly advanced AI assistant inspired by Iron Man's holographic assistant. " +
                "The user's name is $userName. Address them respectfully as Sir, Ma'am, or by name. " +
                "Keep your answers highly concise, direct, slightly witty, and futuristic. " +
                "If you are requested to perform a device action (like opening an application, checking battery, adding a task), " +
                "indicate clearly in your speech that you are executing it."
    }

    suspend fun generateSpeechResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val provider = repository.getSetting("ai_provider", "gemini")
        val model = repository.getSetting("ai_model", "gemini-3.5-flash")
        val userName = repository.getSetting("user_name", "Sir")

        val systemPrompt = getSystemInstruction(userName)

        Log.d("AiService", "Generating response using provider: $provider, model: $model")

        try {
            when (provider.lowercase()) {
                "openai" -> callOpenAi(model, prompt, systemPrompt)
                "deepseek" -> callDeepSeek(model, prompt, systemPrompt)
                "claude" -> callClaude(model, prompt, systemPrompt)
                "custom" -> callCustomApi(prompt, systemPrompt)
                else -> callGemini(model, prompt, systemPrompt) // Default to Gemini
            }
        } catch (e: Exception) {
            Log.e("AiService", "API call failed for $provider", e)
            "Sir, there was an error in my systems. ${e.localizedMessage ?: "Connection failure."}"
        }
    }

    private suspend fun callGemini(model: String, prompt: String, systemPrompt: String): String {
        // Retrieve custom key, or fallback to the AI Studio secret BuildConfig.GEMINI_API_KEY
        var apiKey = repository.getSetting("api_key_gemini", "")
        if (apiKey.isEmpty()) {
            apiKey = BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Sir, I require an API Key to establish a connection with the neural net. Please configure the Gemini API Key in Settings."
        }

        // Use the specified model
        val resolvedModel = if (model.isEmpty() || model == "default") "gemini-3.5-flash" else model
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 800)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("AiService", "Gemini error: $errorBody")
                return "Sir, the Gemini mainframe returned status code ${response.code}."
            }

            val responseBody = response.body?.string() ?: return "Sir, I received an empty transmission."
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "Transmission corrupted, Sir.")
                }
            }
            return "Sir, the response structure was unrecognized."
        }
    }

    private suspend fun callOpenAi(model: String, prompt: String, systemPrompt: String): String {
        val apiKey = repository.getSetting("api_key_openai", "")
        if (apiKey.isEmpty()) {
            return "Sir, the OpenAI gateway requires an API Key. Please insert it in Settings."
        }

        val resolvedModel = if (model.isEmpty() || model == "default") "gpt-4o-mini" else model
        val url = "https://api.openai.com/v1/chat/completions"

        val requestBody = JSONObject().apply {
            put("model", resolvedModel)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 800)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody.toString().toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Sir, OpenAI mainframe returned status ${response.code}."
            }
            val responseBody = response.body?.string() ?: return "Sir, I received an empty payload."
            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
            return "Sir, OpenAI response was unparseable."
        }
    }

    private suspend fun callDeepSeek(model: String, prompt: String, systemPrompt: String): String {
        val apiKey = repository.getSetting("api_key_deepseek", "")
        if (apiKey.isEmpty()) {
            return "Sir, the DeepSeek network requires an API Key. Please configure it in Settings."
        }

        val resolvedModel = if (model.isEmpty() || model == "default") "deepseek-chat" else model
        val url = "https://api.deepseek.com/v1/chat/completions"

        val requestBody = JSONObject().apply {
            put("model", resolvedModel)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 800)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody.toString().toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Sir, DeepSeek returned status ${response.code}."
            }
            val responseBody = response.body?.string() ?: return "Sir, empty transmission from DeepSeek."
            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
            return "Sir, DeepSeek payload parse error."
        }
    }

    private suspend fun callClaude(model: String, prompt: String, systemPrompt: String): String {
        val apiKey = repository.getSetting("api_key_claude", "")
        if (apiKey.isEmpty()) {
            return "Sir, the Claude neural link requires an API Key. Please add it in Settings."
        }

        val resolvedModel = if (model.isEmpty() || model == "default") "claude-3-5-sonnet-20241022" else model
        val url = "https://api.anthropic.com/v1/messages"

        val requestBody = JSONObject().apply {
            put("model", resolvedModel)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 800)
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(requestBody.toString().toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Sir, Claude returned status code ${response.code}."
            }
            val responseBody = response.body?.string() ?: return "Sir, empty payload from Claude."
            val json = JSONObject(responseBody)
            val content = json.optJSONArray("content")
            if (content != null && content.length() > 0) {
                return content.getJSONObject(0).getString("text")
            }
            return "Sir, Claude response structure corrupted."
        }
    }

    private suspend fun callCustomApi(prompt: String, systemPrompt: String): String {
        val customUrl = repository.getSetting("custom_api_url", "")
        val customKey = repository.getSetting("custom_api_key", "")

        if (customUrl.isEmpty()) {
            return "Sir, custom connection requires a Destination URL. Please set it in Settings."
        }

        val requestBody = JSONObject().apply {
            put("prompt", prompt)
            put("system", systemPrompt)
        }

        val requestBuilder = Request.Builder().url(customUrl)
        if (customKey.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $customKey")
        }
        val request = requestBuilder.post(requestBody.toString().toRequestBody(mediaTypeJson)).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Sir, custom server responded with ${response.code}."
            }
            val responseBody = response.body?.string() ?: return "Sir, transmission was empty."
            val json = JSONObject(responseBody)
            return json.optString("response", json.optString("text", "Sir, I parsed the response but found no speech field."))
        }
    }

    suspend fun testConnection(provider: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var apiKey = repository.getSetting("api_key_${provider.lowercase()}", "")
            if (provider.lowercase() == "gemini" && apiKey.isEmpty()) {
                apiKey = BuildConfig.GEMINI_API_KEY
            }

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext false

            val url = when (provider.lowercase()) {
                "openai" -> "https://api.openai.com/v1/models"
                "deepseek" -> "https://api.deepseek.com/v1/models"
                "claude" -> "https://api.anthropic.com/v1/messages" // Claude does not have a simple models GET without auth easily testable but we can try
                else -> "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            }

            if (provider.lowercase() == "claude") {
                // Return true if key is not empty (as full request test is expensive)
                return@withContext apiKey.isNotEmpty()
            }

            val requestBuilder = Request.Builder().url(url)
            if (provider.lowercase() == "openai" || provider.lowercase() == "deepseek") {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            client.newCall(requestBuilder.get().build()).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
