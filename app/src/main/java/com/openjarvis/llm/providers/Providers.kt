
package com.openjarvis.llm.providers

import com.openjarvis.llm.HttpClient
import com.openjarvis.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenRouterProvider(
    private val apiKey: String,
    private val model: String = "meta-llama/llama-3-8b-instruct:free"
) : LLMProvider {

    override val name = "OpenRouter"
    override val baseUrl = "https://openrouter.ai/api/v1"

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userMessage))
                })
                put("max_tokens", 1024)
                put("temperature", 0.1)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/tokenarc/open-jarvis")
                .addHeader("X-Title", "Open Jarvis")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            HttpClient.client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(text)
                if (!response.isSuccessful) {
                    throw Exception(json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}")
                }
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val request = Request.Builder()
                .url("$baseUrl/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            HttpClient.client.newCall(request).execute().use {
                if (!it.isSuccessful) throw Exception("HTTP ${it.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}

class AnthropicProvider(
    private val apiKey: String,
    private val model: String = "claude-haiku-4-20250514"
) : LLMProvider {

    override val name = "Anthropic Claude"
    override val baseUrl = "https://api.anthropic.com/v1"

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1024)
                put("system", systemPrompt)
                put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", userMessage)
                ))
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            HttpClient.client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(text)
                if (!response.isSuccessful) {
                    throw Exception(json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}")
                }
                json.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
            }
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1)
                put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", "ping")
                ))
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            HttpClient.client.newCall(request).execute().use {
                if (!it.isSuccessful) throw Exception("HTTP ${it.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}

class OpenAIProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : LLMProvider {

    override val name = "OpenAI"
    override val baseUrl = "https://api.openai.com/v1"

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userMessage))
                })
                put("max_tokens", 1024)
                put("temperature", 0.1)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            HttpClient.client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(text)
                if (!response.isSuccessful) {
                    throw Exception(json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}")
                }
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val request = Request.Builder()
                .url("$baseUrl/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            HttpClient.client.newCall(request).execute().use {
                if (!it.isSuccessful) throw Exception("HTTP ${it.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}

class OllamaProvider(
    override val baseUrl: String,
    private val model: String = "llama3"
) : LLMProvider {

    override val name = "Ollama"

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userMessage))
                })
                put("stream", false)
            }.toString()

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/chat")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            HttpClient.client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(text)
                if (!response.isSuccessful) {
                    throw Exception(json.optString("error", "HTTP ${response.code}"))
                }
                json.getJSONObject("message").getString("content")
            }
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/tags")
                .get()
                .build()

            HttpClient.client.newCall(request).execute().use {
                if (!it.isSuccessful) throw Exception("HTTP ${it.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}

class CustomProvider(
    override val baseUrl: String,
    private val apiKey: String = "",
    private val model: String = "gpt-4o-mini"
) : LLMProvider {

    override val name = "Custom"

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userMessage))
                })
                put("max_tokens", 1024)
                put("temperature", 0.1)
            }.toString()

            val builder = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))

            if (apiKey.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer $apiKey")
            }

            HttpClient.client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(text)
                if (!response.isSuccessful) {
                    throw Exception(json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}")
                }
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val builder = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/models")
                .get()

            if (apiKey.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer $apiKey")
            }

            HttpClient.client.newCall(builder.build()).execute().use {
                if (!it.isSuccessful) throw Exception("HTTP ${it.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}
