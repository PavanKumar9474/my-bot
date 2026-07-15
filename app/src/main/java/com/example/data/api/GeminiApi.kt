package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class representing the rich NLP insights extracted from the assistant's reply.
 */
data class NlpInsights(
    val detectedLanguage: String = "Unknown",
    val sentiment: String = "Neutral",
    val sentimentEmoji: String = "😐",
    val category: String = "General",
    val isSpam: Boolean = false,
    val entities: List<String> = emptyList()
)

/**
 * Data class representing the complete model response.
 */
data class GeminiResponse(
    val text: String,
    val insights: NlpInsights
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Converts a Bitmap to a base64 string for Gemini API.
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Calls the Gemini API with structured instructions to return a JSON containing both the main reply
     * and structured NLP analytics.
     */
    suspend fun chatWithNlp(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(), // list of role to content
        image: Bitmap? = null,
        modelName: String = "gemini-3.5-flash",
        systemInstruction: String = "You are ChatATM, a highly advanced multilingual AI assistant with advanced NLP and document analysis features."
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "API Key not configured. Please enter your GEMINI_API_KEY in the AI Studio Secrets panel to chat with ChatATM.",
                insights = NlpInsights(sentiment = "Error", sentimentEmoji = "⚠️")
            )
        }

        val url = "$BASE_URL/models/$modelName:generateContent?key=$apiKey"

        try {
            // Build the contents list
            val contentsArray = JSONArray()

            // Add previous history
            for ((role, content) in history) {
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "user") "user" else "model")
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", content)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Add the current prompt (and image if any)
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val partsArray = JSONArray()

            // Text prompt part
            val textPartObj = JSONObject()
            textPartObj.put("text", prompt)
            partsArray.put(textPartObj)

            // Image part if provided
            if (image != null) {
                val imagePartObj = JSONObject()
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", "image/jpeg")
                inlineDataObj.put("data", image.toBase64())
                imagePartObj.put("inlineData", inlineDataObj)
                partsArray.put(imagePartObj)
            }

            currentTurn.put("parts", partsArray)
            contentsArray.put(currentTurn)

            // Construct System Instruction
            val sysInstructionObj = JSONObject()
            val sysPartsArray = JSONArray()
            val sysPartObj = JSONObject()
            // Provide exact system instruction prompting the model to answer, but format as JSON
            val fullSystemInstruction = """
                $systemInstruction
                You MUST return your output strictly in JSON format matching this exact schema:
                {
                  "reply": "your conversation text or answer in rich Markdown. Keep formatting beautifully structured with list items or bold text if appropriate.",
                  "language": "Detected language name (e.g. English, French, Telugu, Hindi, Spanish, etc.)",
                  "sentiment": "Positive, Negative, or Neutral",
                  "category": "Main topic classification of this turn (e.g. Technology, Coding, Writing, Mathematics, General, Travel, Finance, Business)",
                  "spamScore": 0.0 to 1.0 checking if the prompt was spam, advertisement, or malicious (usually 0.0),
                  "entities": ["list of named entities, places, names, dates, or key terms extracted from the prompt and your reply"]
                }
            """.trimIndent()
            sysPartObj.put("text", fullSystemInstruction)
            sysPartsArray.put(sysPartObj)
            sysInstructionObj.put("parts", sysPartsArray)

            // Construct Generation Config for JSON response format
            val generationConfig = JSONObject()
            val responseFormat = JSONObject()
            responseFormat.put("type", "OBJECT") // Standard JSON response schema is triggered
            
            // We can also request the standard JSON format schema directly via response_mime_type
            generationConfig.put("responseMimeType", "application/json")
            generationConfig.put("temperature", 0.7)

            // Main Request Payload
            val requestPayload = JSONObject()
            requestPayload.put("contents", contentsArray)
            requestPayload.put("systemInstruction", sysInstructionObj)
            requestPayload.put("generationConfig", generationConfig)

            val requestBody = requestPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed with code ${response.code}: $responseStr")
                    return@withContext GeminiResponse(
                        text = "API Error: ${response.code}. Please verify your API key and network connection.",
                        insights = NlpInsights(sentiment = "Error", sentimentEmoji = "⚠️")
                    )
                }

                // Parse response
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext GeminiResponse(
                        text = "No response from AI model.",
                        insights = NlpInsights(sentiment = "No Data", sentimentEmoji = "❓")
                    )
                }

                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    return@withContext GeminiResponse(
                        text = "No text content returned from the AI model.",
                        insights = NlpInsights(sentiment = "No Data", sentimentEmoji = "❓")
                    )
                }

                val rawText = parts.getJSONObject(0).optString("text", "")
                
                // Parse the returned JSON payload
                try {
                    val innerJson = JSONObject(rawText.trim())
                    val replyText = innerJson.optString("reply", "No message content")
                    val detectedLang = innerJson.optString("language", "English")
                    val sentiment = innerJson.optString("sentiment", "Neutral")
                    val category = innerJson.optString("category", "General")
                    val isSpam = innerJson.optDouble("spamScore", 0.0) > 0.5
                    
                    val entitiesArray = innerJson.optJSONArray("entities")
                    val entitiesList = mutableListOf<String>()
                    if (entitiesArray != null) {
                        for (i in 0 until entitiesArray.length()) {
                            entitiesList.add(entitiesArray.getString(i))
                        }
                    }

                    val emoji = when (sentiment.lowercase()) {
                        "positive" -> "😊"
                        "negative" -> "😔"
                        else -> "😐"
                    }

                    val insights = NlpInsights(
                        detectedLanguage = detectedLang,
                        sentiment = sentiment,
                        sentimentEmoji = emoji,
                        category = category,
                        isSpam = isSpam,
                        entities = entitiesList
                    )

                    GeminiResponse(replyText, insights)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse inner JSON response, raw text was: $rawText", e)
                    // Fallback to text directly if the model returned plain text instead of JSON
                    GeminiResponse(
                        text = rawText,
                        insights = NlpInsights(
                            detectedLanguage = "Auto-detected",
                            sentiment = "Analyzed",
                            sentimentEmoji = "💡",
                            category = "General",
                            entities = listOf("AI Response")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            GeminiResponse(
                text = "Connection error: ${e.localizedMessage}. Please verify your internet connection.",
                insights = NlpInsights(sentiment = "Exception", sentimentEmoji = "❌")
            )
        }
    }
}
