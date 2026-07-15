package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.api.GeminiClient
import com.example.data.database.ChatDao
import com.example.data.database.ChatMessageEntity
import com.example.data.database.ChatSession
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForChat(chatId: Int): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForChat(chatId)

    suspend fun createNewChat(title: String, language: String = "English"): Int {
        val session = ChatSession(title = title, selectedLanguage = language)
        return chatDao.insertSession(session).toInt()
    }

    suspend fun deleteChat(chatId: Int) {
        chatDao.deleteSessionById(chatId)
    }

    suspend fun renameChat(chatId: Int, title: String) {
        chatDao.renameSession(chatId, title)
    }

    suspend fun togglePinChat(chatId: Int, isPinned: Boolean) {
        chatDao.pinSession(chatId, isPinned)
    }

    suspend fun clearHistory(chatId: Int) {
        chatDao.clearMessagesForChat(chatId)
    }

    /**
     * Sends a message to the AI, saves it locally, analyzes NLP aspects, and saves the AI response.
     */
    suspend fun sendMessage(
        chatId: Int,
        content: String,
        image: Bitmap? = null,
        historyList: List<ChatMessageEntity> = emptyList(),
        modelName: String = "gemini-3.5-flash",
        systemInstruction: String = "You are ChatATM, a highly advanced multilingual AI assistant with advanced NLP and document analysis features."
    ): ChatMessageEntity {
        // 1. Save user message to database
        val userEntity = ChatMessageEntity(
            chatId = chatId,
            role = "user",
            content = content,
            detectedLanguage = "Auto-detecting...",
            sentiment = "Neutral",
            sentimentEmoji = "😐"
        )
        chatDao.insertMessage(userEntity)

        // Prepare context history for Gemini
        // We only send the last 8-10 messages to keep request context lean and fast
        val conversationHistory = historyList.takeLast(10).map {
            Pair(it.role, it.content)
        }

        // 2. Query Gemini API
        val response = GeminiClient.chatWithNlp(
            prompt = content,
            history = conversationHistory,
            image = image,
            modelName = modelName,
            systemInstruction = systemInstruction
        )

        // 3. Serialize extracted entities to store as simple JSON array or text
        val entitiesJson = JSONArray(response.insights.entities).toString()

        // 4. Save model response with NLP insights
        val aiEntity = ChatMessageEntity(
            chatId = chatId,
            role = "model",
            content = response.text,
            detectedLanguage = response.insights.detectedLanguage,
            sentiment = response.insights.sentiment,
            sentimentEmoji = response.insights.sentimentEmoji,
            category = response.insights.category,
            isSpam = response.insights.isSpam,
            entitiesJson = entitiesJson
        )
        chatDao.insertMessage(aiEntity)

        return aiEntity
    }
}
