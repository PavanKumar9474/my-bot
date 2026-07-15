package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.database.ChatDatabase
import com.example.data.database.ChatMessageEntity
import com.example.data.database.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(db.chatDao())

    private val TAG = "ChatViewModel"

    // --- UI State Variables ---
    var activeTab by mutableStateOf("chat") // "chat", "ocr", "writing", "translation", "settings"
    var textInput by mutableStateOf("")
    var isSending by mutableStateOf(false)
    var selectedImage by mutableStateOf<Bitmap?>(null)
    
    // Voice / Audio Settings
    var isSpeechListening by mutableStateOf(false)
    var speechResultText by mutableStateOf("")
    var ttsSpeed by mutableStateOf(1.0f)
    var ttsPitch by mutableStateOf(1.0f)
    var ttsVoiceGender by mutableStateOf("Female") // "Male" or "Female"

    // Model and Instruction Configs
    var selectedModel by mutableStateOf("gemini-3.5-flash") // gemini-3.5-flash or gemini-3.1-pro-preview
    var customSystemInstruction by mutableStateOf(
        "You are ChatATM, a highly advanced multilingual AI assistant with advanced NLP, voice response, and document analysis features."
    )

    // Translation Tab specific state
    var translationInputText by mutableStateOf("")
    var translationOutputText by mutableStateOf("")
    var sourceLanguage by mutableStateOf("Auto-Detect")
    var targetLanguage by mutableStateOf("Spanish")
    var isTranslating by mutableStateOf(false)

    // OCR / Document AI specific state
    var ocrStatusText by mutableStateOf("Select a document or mock template to extract text and analyze content.")
    var ocrAnalysisResult by mutableStateOf("")
    var isOcrProcessing by mutableStateOf(false)

    // --- Database Flows ---
    val chatSessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected Chat Session ID
    private val _selectedChatId = MutableStateFlow<Int?>(null)
    val selectedChatId = _selectedChatId.asStateFlow()

    // Loaded Messages Flow for the currently selected session
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _selectedChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForChat(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Text To Speech
    private var textToSpeech: TextToSpeech? = null
    var isTtsSpeaking by mutableStateOf(false)

    // Speech Recognizer
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        // Initialize TTS
        textToSpeech = TextToSpeech(application, this)

        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
        }

        // Auto-create a first session if none exists
        viewModelScope.launch {
            chatSessions.collect { sessions ->
                if (sessions.isEmpty() && _selectedChatId.value == null) {
                    createNewChat("First ChatATM Session")
                } else if (_selectedChatId.value == null && sessions.isNotEmpty()) {
                    _selectedChatId.value = sessions.first().id
                }
            }
        }
    }

    // --- Chat Actions ---

    fun createNewChat(title: String, language: String = "English") {
        viewModelScope.launch {
            val id = repository.createNewChat(title, language)
            _selectedChatId.value = id
        }
    }

    fun selectChat(id: Int) {
        _selectedChatId.value = id
    }

    fun deleteChat(id: Int) {
        viewModelScope.launch {
            repository.deleteChat(id)
            if (_selectedChatId.value == id) {
                _selectedChatId.value = null
            }
        }
    }

    fun renameChat(id: Int, newTitle: String) {
        viewModelScope.launch {
            repository.renameChat(id, newTitle)
        }
    }

    fun togglePinChat(id: Int, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinChat(id, isPinned)
        }
    }

    fun clearChatHistory(id: Int) {
        viewModelScope.launch {
            repository.clearHistory(id)
        }
    }

    /**
     * Send message from input box.
     */
    fun sendMessage() {
        val chatId = _selectedChatId.value ?: return
        val text = textInput.trim()
        if (text.isEmpty() && selectedImage == null) return

        textInput = ""
        val imageToProcess = selectedImage
        selectedImage = null
        isSending = true

        viewModelScope.launch {
            try {
                // Fetch the current messages to serve as historical context for conversational continuity
                val currentMessages = chatMessages.value
                repository.sendMessage(
                    chatId = chatId,
                    content = text,
                    image = imageToProcess,
                    historyList = currentMessages,
                    modelName = selectedModel,
                    systemInstruction = customSystemInstruction
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            } finally {
                isSending = false
            }
        }
    }

    /**
     * Send custom preloaded text (used by writing templates)
     */
    fun sendPreloadedPrompt(promptText: String) {
        textInput = promptText
        sendMessage()
    }

    // --- TTS (Text To Speech) Methods ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language is not supported or missing data")
                }
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun speakText(text: String) {
        textToSpeech?.let { tts ->
            if (isTtsSpeaking) {
                tts.stop()
                isTtsSpeaking = false
                return
            }

            tts.setSpeechRate(ttsSpeed)
            tts.setPitch(ttsPitch)

            // Select a voice accent or style depending on gender setting if available on device
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val voices = tts.voices
                    val selectedVoice = voices.firstOrNull { voice ->
                        val nameLower = voice.name.lowercase()
                        if (ttsVoiceGender == "Male") {
                            nameLower.contains("male") && !nameLower.contains("female")
                        } else {
                            nameLower.contains("female")
                        }
                    } ?: voices.firstOrNull { it.locale.language == Locale.getDefault().language }
                    
                    if (selectedVoice != null) {
                        tts.voice = selectedVoice
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply custom TTS Voice profiles", e)
                }
            }

            isTtsSpeaking = true
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ChatATMTTS")
            
            // Check status of speaking to toggle state
            viewModelScope.launch {
                while (tts.isSpeaking) {
                    kotlinx.coroutines.delay(100)
                }
                isTtsSpeaking = false
            }
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        isTtsSpeaking = false
    }

    // --- STT (Speech-to-Text) Methods ---

    fun startListening(context: Context) {
        if (speechRecognizer == null) {
            // Re-initialize if possible
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                speechResultText = "Speech recognition is not supported on this device/emulator. Please type instead!"
                return
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isSpeechListening = true
                speechResultText = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                speechResultText = "Processing speech..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isSpeechListening = false
            }

            override fun onError(error: Int) {
                isSpeechListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient record permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
                    else -> "Unknown speech recognizer error ($error)"
                }
                speechResultText = "Error: $errorMsg. Try typing or verify microphone permissions."
                Log.e(TAG, "SpeechRecognizer Error: $errorMsg")
            }

            override fun onResults(results: Bundle?) {
                isSpeechListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val speechText = matches[0]
                    textInput = speechText
                    speechResultText = speechText
                } else {
                    speechResultText = "Could not understand audio. Try again!"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    speechResultText = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isSpeechListening = false
    }

    // --- Translation Mode ---

    fun performTranslation() {
        val inputText = translationInputText.trim()
        if (inputText.isEmpty()) return

        isTranslating = true
        translationOutputText = "Translating..."

        viewModelScope.launch {
            try {
                val prompt = """
                    Translate the following text to $targetLanguage. 
                    Source language is $sourceLanguage. If 'Auto-Detect', detect it.
                    
                    Only return the direct translated text. Do not provide any conversational text, explanations, or metadata outside the translation.
                    
                    Text: "$inputText"
                """.trimIndent()

                val response = GeminiClient.chatWithNlp(
                    prompt = prompt,
                    modelName = selectedModel,
                    systemInstruction = "You are a professional language translator. Translate accurately and naturally while maintaining tone."
                )

                translationOutputText = response.text
            } catch (e: Exception) {
                translationOutputText = "Translation failed: ${e.localizedMessage}"
            } finally {
                isTranslating = false
            }
        }
    }

    // --- OCR & Document AI Analysis ---

    /**
     * Executes an OCR and structured Document analysis on the selected bitmap image or simulation asset.
     */
    fun performDocumentOcr(imageBitmap: Bitmap, documentType: String) {
        isOcrProcessing = true
        ocrStatusText = "Analyzing Document using Gemini Multimodal OCR AI..."
        ocrAnalysisResult = ""

        viewModelScope.launch {
            try {
                val prompt = """
                    You are performing high-precision Document AI OCR and intelligence analysis for a $documentType.
                    
                    Perform the following tasks:
                    1. OCR text extraction: Extract all text.
                    2. Document Categorization & Key Details extraction (e.g., Dates, Names, Totals, Tables).
                    3. Summarize the content and provide an analysis recommendation.
                    
                    Present your results in beautiful Markdown format with clear sections, bullet points, and high-contrast styling.
                """.trimIndent()

                val response = GeminiClient.chatWithNlp(
                    prompt = prompt,
                    image = imageBitmap,
                    modelName = selectedModel,
                    systemInstruction = "You are an expert Document AI analyzer, specialized in scanning receipts, invoices, passports, business cards, resumes, and text documents with pristine accuracy."
                )

                ocrAnalysisResult = response.text
                ocrStatusText = "Document Scan and OCR completed successfully!"
            } catch (e: Exception) {
                ocrStatusText = "OCR Processing failed: ${e.localizedMessage}"
            } finally {
                isOcrProcessing = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}
