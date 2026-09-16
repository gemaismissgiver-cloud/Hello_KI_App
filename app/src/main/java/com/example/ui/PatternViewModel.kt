package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.JournalEntry
import com.example.data.PatternRecord
import com.example.data.PatternRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class StudioTab { CHAT, TAGEBUCH, PROTOKOLLE, MUSTER_INSPEKTOR }
enum class AttachmentType { AUDIO, IMAGE, VIDEO, NOTE }

data class MediaAttachment(val id: String = UUID.randomUUID().toString(), val type: AttachmentType, val fileName: String, val infoText: String, val contentPreview: String? = null)
data class PatternAnalysisResult(val title: String, val category: String, val patternIdentified: String, val logicExplanation: String, val breakPatternTip: String, val voiceToneAnalysis: String? = null)
data class ChatMessage(val id: String = UUID.randomUUID().toString(), val sender: String, val text: String, val attachment: MediaAttachment? = null, val patternAnalysis: PatternAnalysisResult? = null, val timestamp: Long = System.currentTimeMillis())
data class ChatSession(val id: String = UUID.randomUUID().toString(), var title: String, val messages: List<ChatMessage> = emptyList(), val createdAt: Long = System.currentTimeMillis())

class PatternViewModel(private val repository: PatternRepository, private val context: Context) : ViewModel() {
    val records: StateFlow<List<PatternRecord>> = repository.allRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _userMood = MutableStateFlow(0f)
    val userMood: StateFlow<Float> = _userMood.asStateFlow()
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()
    private val _activeChatId = MutableStateFlow<String>("")
    val activeChatId: StateFlow<String> = _activeChatId.asStateFlow()
    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    init {
        val loaded = loadChatSessionsFromPrefs()
        val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
        val savedApiKey = prefs.getString("custom_api_key", "") ?: ""
        if (savedApiKey.isNotBlank()) { _customApiKey.value = savedApiKey }
        if (loaded.isNotEmpty()) {
            _chatSessions.value = loaded
            _activeChatId.value = loaded.firstOrNull()?.id ?: ""
        } else {
            createNewChat("Haupt-Analyse Chat")
        }
    }

    private fun saveChatSessionsToPrefs() {
        try {
            val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
            val array = JSONArray()
            _chatSessions.value.forEach { session ->
                val sessObj = JSONObject().apply {
                    put("id", session.id)
                    put("title", session.title)
                    put("createdAt", session.createdAt)
                    val msgArray = JSONArray()
                    session.messages.forEach { msg ->
                        msgArray.put(JSONObject().apply {
                            put("id", msg.id)
                            put("sender", msg.sender)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp)
                        })
                    }
                    put("messages", msgArray)
                }
                array.put(sessObj)
            }
            prefs.edit().putString("chat_sessions_json", array.toString()).putString("custom_api_key", _customApiKey.value).apply()
        } catch (e: Exception) { android.util.Log.e("VM", "Save error", e) }
    }

    private fun loadChatSessionsFromPrefs(): List<ChatSession> {
        try {
            val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("chat_sessions_json", null) ?: return emptyList()
            if (jsonStr.isBlank()) return emptyList()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until array.length()) {
                val sessObj = array.getJSONObject(i)
                val msgArray = sessObj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val msgObj = msgArray.getJSONObject(j)
                    messages.add(ChatMessage(msgObj.optString("id", UUID.randomUUID().toString()), msgObj.optString("sender", "User"), msgObj.optString("text", "")))
                }
                list.add(ChatSession(sessObj.optString("id"), sessObj.optString("title"), messages))
            }
            return list
        } catch (e: Exception) { return emptyList() }
    }

    fun setUserMood(mood: Float) { _userMood.value = mood }
    fun setCustomApiKey(key: String) { _customApiKey.value = key.trim(); saveChatSessionsToPrefs() }
    fun addJournalEntry(title: String, content: String, type: String = "JOURNAL", importance: Int = 5, createdBy: String = "User") {
        viewModelScope.launch { repository.insertJournalEntry(JournalEntry(title, content, type, importance, createdBy)) }
    }
    fun deleteJournalEntry(id: Long) { viewModelScope.launch { repository.deleteJournalEntryById(id) } }
    fun clearJournal() { viewModelScope.launch { repository.clearJournal() } }
    fun generateChatSnapshot() { viewModelScope.launch { } }
    
    private fun addMessageToSession(sessionId: String, message: ChatMessage) {
        val targetId = if (sessionId.isBlank()) _activeChatId.value else sessionId
        _chatSessions.value = _chatSessions.value.map { if (it.id == targetId) { it.copy(messages = it.messages + message) } else { it } }
        saveChatSessionsToPrefs()
    }

    fun createNewChat(title: String = "Neuer Chat"): String {
        val newSession = ChatSession(title = title, messages = listOf(ChatMessage(sender = "Hello KI", text = "0-Punkt Logik aktiviert.")))
        val updated = _chatSessions.value.toMutableList()
        updated.add(0, newSession)
        _chatSessions.value = updated
        _activeChatId.value = newSession.id
        saveChatSessionsToPrefs()
        return newSession.id
    }

    fun renameChat(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        _chatSessions.value = _chatSessions.value.map { if (it.id == sessionId) { it.copy(title = newTitle.trim()) } else { it } }
        saveChatSessionsToPrefs()
    }

    fun deleteChat(sessionId: String) {
        _chatSessions.value = _chatSessions.value.filterNot { it.id == sessionId }
        if (_activeChatId.value == sessionId) { _activeChatId.value = _chatSessions.value.firstOrNull()?.id ?: createNewChat() }
        saveChatSessionsToPrefs()
    }

    fun selectChat(sessionId: String) { _activeChatId.value = sessionId }
    fun sendVoiceMessage(durationSeconds: Int, userNote: String = "") {
        val msg = ChatMessage(sender = "User", text = "🎤 Voice message")
        addMessageToSession(_activeChatId.value, msg)
    }
    fun sendAttachment(type: AttachmentType, fileName: String, infoText: String, previewText: String? = null) {
        val msg = ChatMessage(sender = "User", text = "📎 $fileName")
        addMessageToSession(_activeChatId.value, msg)
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val currentId = _activeChatId.value
        if (currentId.isBlank()) return

        val userMsg = ChatMessage(sender = "User", text = userText.trim())
        addMessageToSession(currentId, userMsg)

        viewModelScope.launch {
            val apiKey = try { BuildConfig.GROQ_API_KEY } catch (e: Exception) { "" }
            val key = if (_customApiKey.value.isNotBlank()) _customApiKey.value else apiKey
            var response = if (key.isNotBlank() && key != "your_groq_api_key_here") fetchGroqResponse(userText, key) else null
            if (response.isNullOrBlank()) { response = "0-Punkt: Ich verstehe dein Anliegen. Groq API Key erforderlich für vollständige Antworten." }
            val aiMsg = ChatMessage(sender = "Hello KI", text = response)
            addMessageToSession(currentId, aiMsg)
        }
    }

    private suspend fun fetchGroqResponse(userText: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            conn.doOutput = true

            val messages = JSONArray().put(JSONObject().put("role", "user").put("content", userText))
            val body = JSONObject().put("model", "mixtral-8x7b-32768").put("messages", messages).put("max_tokens", 512)
            conn.outputStream.write(body.toString().toByteArray())

            if (conn.responseCode == 200) {
                val resp = JSONObject(conn.inputStream.bufferedReader().readText())
                return@withContext resp.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            } else {
                return@withContext "API Error ${conn.responseCode}: Check your Groq key at https://console.groq.com/"
            }
        } catch (e: Exception) {
            return@withContext "Connection error: ${e.message}"
        }
    }

    fun addRecord(title: String, category: String, summary: String, rawMetrics: String, insights: String) {
        viewModelScope.launch { repository.insert(PatternRecord(title, category, summary, rawMetrics, insights)) }
    }
    fun deleteRecord(id: Long) { viewModelScope.launch { repository.deleteById(id) } }
    fun clearAllRecords() { viewModelScope.launch { repository.clearAll() } }
}

class PatternViewModelFactory(private val repository: PatternRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatternViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatternViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
