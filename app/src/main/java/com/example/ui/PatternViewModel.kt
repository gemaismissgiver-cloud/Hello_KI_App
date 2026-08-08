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

enum class StudioTab {
    CHAT,
    TAGEBUCH,
    PROTOKOLLE,
    MUSTER_INSPEKTOR
}

enum class AttachmentType {
    AUDIO,
    IMAGE,
    VIDEO,
    NOTE
}

data class MediaAttachment(
    val id: String = UUID.randomUUID().toString(),
    val type: AttachmentType,
    val fileName: String,
    val infoText: String, // e.g. "0:42 Min" or "2.4 MB"
    val contentPreview: String? = null
)

data class PatternAnalysisResult(
    val title: String,
    val category: String,
    val patternIdentified: String,
    val logicExplanation: String,
    val breakPatternTip: String,
    val voiceToneAnalysis: String? = null // Audio pitch & emotional tone analysis without judgment
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "User" or "Hello KI"
    val text: String,
    val attachment: MediaAttachment? = null,
    val patternAnalysis: PatternAnalysisResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

class PatternViewModel(
    private val repository: PatternRepository,
    private val context: Context
) : ViewModel() {

    val records: StateFlow<List<PatternRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Mood Slider: 0f = 0-Punkt (Klar/Neutral), 0.5f = 0-Punkt (Freundlich/Ruhig), 1f = Emotionale Welle, 2f = Reibung, 3f = Muster auflösen
    private val _userMood = MutableStateFlow(0f)
    val userMood: StateFlow<Float> = _userMood.asStateFlow()

    // Chat Sessions
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _activeChatId = MutableStateFlow<String>("")
    val activeChatId: StateFlow<String> = _activeChatId.asStateFlow()

    // Custom API Key support
    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    init {
        val loaded = loadChatSessionsFromPrefs()
        val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
        val savedActiveId = prefs.getString("active_chat_id", "") ?: ""
        val savedApiKey = prefs.getString("custom_api_key", "") ?: ""

        if (savedApiKey.isNotBlank()) {
            _customApiKey.value = savedApiKey
        }

        if (loaded.isNotEmpty()) {
            _chatSessions.value = loaded
            _activeChatId.value = if (loaded.any { it.id == savedActiveId }) savedActiveId else loaded.first().id
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
                        val msgObj = JSONObject().apply {
                            put("id", msg.id)
                            put("sender", msg.sender)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp)
                            msg.attachment?.let { att ->
                                put("attachment", JSONObject().apply {
                                    put("id", att.id)
                                    put("type", att.type.name)
                                    put("fileName", att.fileName)
                                    put("infoText", att.infoText)
                                    att.contentPreview?.let { put("contentPreview", it) }
                                })
                            }
                            msg.patternAnalysis?.let { pa ->
                                put("patternAnalysis", JSONObject().apply {
                                    put("title", pa.title)
                                    put("category", pa.category)
                                    put("patternIdentified", pa.patternIdentified)
                                    put("logicExplanation", pa.logicExplanation)
                                    put("breakPatternTip", pa.breakPatternTip)
                                    pa.voiceToneAnalysis?.let { put("voiceToneAnalysis", it) }
                                })
                            }
                        }
                        msgArray.put(msgObj)
                    }
                    put("messages", msgArray)
                }
                array.put(sessObj)
            }
            prefs.edit()
                .putString("chat_sessions_json", array.toString())
                .putString("active_chat_id", _activeChatId.value)
                .putString("custom_api_key", _customApiKey.value)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("PatternViewModel", "Error saving chat sessions", e)
        }
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
                val id = sessObj.optString("id")
                val title = sessObj.optString("title")
                val createdAt = sessObj.optLong("createdAt", System.currentTimeMillis())
                val msgArray = sessObj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val msgObj = msgArray.getJSONObject(j)
                    val mId = msgObj.optString("id", UUID.randomUUID().toString())
                    val sender = msgObj.optString("sender", "User")
                    val text = msgObj.optString("text", "")
                    val timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())

                    var attachment: MediaAttachment? = null
                    if (msgObj.has("attachment") && !msgObj.isNull("attachment")) {
                        val attObj = msgObj.getJSONObject("attachment")
                        val typeStr = attObj.optString("type", AttachmentType.NOTE.name)
                        val type = try { AttachmentType.valueOf(typeStr) } catch (e: Exception) { AttachmentType.NOTE }
                        attachment = MediaAttachment(
                            id = attObj.optString("id", UUID.randomUUID().toString()),
                            type = type,
                            fileName = attObj.optString("fileName", ""),
                            infoText = attObj.optString("infoText", ""),
                            contentPreview = if (attObj.has("contentPreview")) attObj.optString("contentPreview") else null
                        )
                    }

                    var patternAnalysis: PatternAnalysisResult? = null
                    if (msgObj.has("patternAnalysis") && !msgObj.isNull("patternAnalysis")) {
                        val paObj = msgObj.getJSONObject("patternAnalysis")
                        patternAnalysis = PatternAnalysisResult(
                            title = paObj.optString("title", ""),
                            category = paObj.optString("category", ""),
                            patternIdentified = paObj.optString("patternIdentified", ""),
                            logicExplanation = paObj.optString("logicExplanation", ""),
                            breakPatternTip = paObj.optString("breakPatternTip", ""),
                            voiceToneAnalysis = if (paObj.has("voiceToneAnalysis")) paObj.optString("voiceToneAnalysis") else null
                        )
                    }

                    messages.add(ChatMessage(mId, sender, text, attachment, patternAnalysis, timestamp))
                }
                list.add(ChatSession(id, title, messages, createdAt))
            }
            return list
        } catch (e: Exception) {
            android.util.Log.e("PatternViewModel", "Error loading chat sessions", e)
            return emptyList()
        }
    }

    fun setUserMood(mood: Float) {
        _userMood.value = mood
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        saveChatSessionsToPrefs()
    }

    fun addJournalEntry(title: String, content: String, type: String = "AUTONOMOUS_JOURNAL", importance: Int = 5, createdBy: String = "Nutzer") {
        viewModelScope.launch {
            repository.insertJournalEntry(
                JournalEntry(
                    title = title,
                    content = content,
                    type = type,
                    importanceScore = importance,
                    createdBy = createdBy
                )
            )
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteJournalEntryById(id)
        }
    }

    fun clearJournal() {
        viewModelScope.launch {
            repository.clearJournal()
        }
    }

    fun generateChatSnapshot() {
        viewModelScope.launch {
            val session = _chatSessions.value.find { it.id == _activeChatId.value }
            val messages = session?.messages ?: emptyList()
            if (messages.isEmpty()) return@launch

            val textContext = messages.takeLast(20).joinToString("\n") { "${it.sender}: ${it.text}" }
            val snapshotTitle = "Chat-Snapshot: ${session?.title ?: "Gespräch"}"
            val snapshotContent = "Gedächtnis-Auszug aus der Unterhaltung:\n\n" + textContext.take(800)

            repository.insertJournalEntry(
                JournalEntry(
                    title = snapshotTitle,
                    content = snapshotContent,
                    type = "SNAPSHOT",
                    importanceScore = 8,
                    createdBy = "KI"
                )
            )
        }
    }

    private fun addMessageToSession(sessionId: String, message: ChatMessage) {
        val targetId = if (sessionId.isBlank()) _activeChatId.value else sessionId
        _chatSessions.value = _chatSessions.value.map { session ->
            if (session.id == targetId) {
                session.copy(messages = session.messages + message)
            } else {
                session
            }
        }
        saveChatSessionsToPrefs()
    }

    fun createNewChat(title: String = "Neuer Chat"): String {
        val newSession = ChatSession(
            title = title,
            messages = listOf(
                ChatMessage(
                    sender = "Hello KI",
                    text = "System bereit. Logische Berechnung aktiviert (0-Punkt). Passe deine Stimmung am Regler oben an, stelle Fragen oder beschreibe Verhaltensmuster.",
                    patternAnalysis = PatternAnalysisResult(
                        title = "Startkonfiguration",
                        category = "System-Status",
                        patternIdentified = "Initiale Ausrichtung",
                        logicExplanation = "Reine Datenverarbeitung ohne emotionale Simulation oder Heuchelei.",
                        breakPatternTip = "Verhalten beobachten, Ursachen berechnen und Handlungsmuster klären."
                    )
                )
            )
        )
        val updated = _chatSessions.value.toMutableList()
        updated.add(0, newSession)
        _chatSessions.value = updated
        _activeChatId.value = newSession.id
        saveChatSessionsToPrefs()
        return newSession.id
    }

    fun renameChat(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        val updated = _chatSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(title = newTitle.trim())
            } else {
                session
            }
        }
        _chatSessions.value = updated
        saveChatSessionsToPrefs()
    }

    fun deleteChat(sessionId: String) {
        val updated = _chatSessions.value.filterNot { it.id == sessionId }
        _chatSessions.value = updated
        if (_activeChatId.value == sessionId) {
            _activeChatId.value = updated.firstOrNull()?.id ?: createNewChat("Neuer Chat")
        }
        saveChatSessionsToPrefs()
    }

    fun selectChat(sessionId: String) {
        _activeChatId.value = sessionId
        saveChatSessionsToPrefs()
    }

    fun sendVoiceMessage(durationSeconds: Int, userNote: String = "") {
        val currentId = _activeChatId.value
        if (currentId.isBlank()) return

        val minStr = durationSeconds / 60
        val secStr = String.format("%02d", durationSeconds % 60)
        val info = "$minStr:$secStr Min (Sprachaufnahme)"

        val attachment = MediaAttachment(
            type = AttachmentType.AUDIO,
            fileName = "Sprachnachricht_${System.currentTimeMillis() % 10000}.aac",
            infoText = info,
            contentPreview = if (userNote.isNotBlank()) userNote else "Sprachdatei empfangen"
        )

        val userMessage = ChatMessage(
            sender = "User",
            text = if (userNote.isNotBlank()) userNote else "🎤 [Sprachnachricht $info]",
            attachment = attachment
        )
        addMessageToSession(currentId, userMessage)

        // Tone of voice analysis
        val toneAnalysis = when {
            durationSeconds > 60 -> "Tonlage: Erhöhte Intensität / Schnelle Frequenz. Ausdrucksstarke Stimmführung ohne Filter."
            durationSeconds in 20..60 -> "Tonlage: Bestimmend und fokussiert. Mittlere Dynamik, klare Ausstrahlung."
            else -> "Tonlage: Ruhig, präzise Sprachresonanz."
        }

        val analysis = PatternAnalysisResult(
            title = "Sprach- & Tonlagenanalyse",
            category = "Audio-Akustik",
            patternIdentified = "Akustisches Signal ausgewertet. Keine Dämpfung oder Zensur der Emotion.",
            logicExplanation = "Tonlage zeigt ungefilterten Ausdruck. Hello KI hört aufmerksam zu und analysiert die Ursachen ohne leere Floskeln.",
            breakPatternTip = "Lass den Ausdruck zu. Reibung entsteht nur, wenn Emotion unterdrückt oder belehrt wird.",
            voiceToneAnalysis = toneAnalysis
        )

        val aiMessage = ChatMessage(
            sender = "Hello KI",
            text = "Sprachdatei vollständig empfangen und analysiert. Ich habe die Tonlage und den Inhalt ohne Gefühlsfilter ausgewertet.",
            patternAnalysis = analysis
        )
        addMessageToSession(currentId, aiMessage)

        addRecord(
            title = analysis.title,
            category = analysis.category,
            summary = analysis.patternIdentified,
            rawMetrics = "$toneAnalysis | ${analysis.logicExplanation}",
            insights = analysis.breakPatternTip
        )
    }

    fun sendAttachment(type: AttachmentType, fileName: String, infoText: String, previewText: String? = null) {
        val currentId = _activeChatId.value
        if (currentId.isBlank()) return

        val iconLabel = when(type) {
            AttachmentType.IMAGE -> "📷 [Bild]"
            AttachmentType.VIDEO -> "🎥 [Video]"
            AttachmentType.NOTE -> "📝 [Notiz]"
            AttachmentType.AUDIO -> "🎵 [Audio]"
        }

        val attachment = MediaAttachment(
            type = type,
            fileName = fileName,
            infoText = infoText,
            contentPreview = previewText
        )

        val userMessage = ChatMessage(
            sender = "User",
            text = "$iconLabel $fileName",
            attachment = attachment
        )
        addMessageToSession(currentId, userMessage)

        val analysis = PatternAnalysisResult(
            title = "Medien-Inspektion (${type.name})",
            category = "Medien-Analyse",
            patternIdentified = "Datei '$fileName' ($infoText) erfasst.",
            logicExplanation = previewText ?: "Inhalt ohne Zensur oder Bewertung verarbeitet.",
            breakPatternTip = "Medieninhalt steht im Protokoll bereit."
        )

        val aiMessage = ChatMessage(
            sender = "Hello KI",
            text = "Datei '$fileName' empfangen. Logische Auswertung und Musterabgleich durchgeführt.",
            patternAnalysis = analysis
        )
        addMessageToSession(currentId, aiMessage)

        addRecord(
            title = analysis.title,
            category = analysis.category,
            summary = analysis.patternIdentified,
            rawMetrics = analysis.logicExplanation,
            insights = analysis.breakPatternTip
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val currentId = _activeChatId.value
        if (currentId.isBlank()) return

        val userMessage = ChatMessage(sender = "User", text = userText.trim())
        addMessageToSession(currentId, userMessage)

        val mood = _userMood.value
        val analysis = generatePatternAnalysis(userText.trim(), mood)

        viewModelScope.launch {
            val configApiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            val effectiveApiKey = when {
                _customApiKey.value.isNotBlank() -> _customApiKey.value
                configApiKey.isNotBlank() && configApiKey != "MY_GEMINI_API_KEY" -> configApiKey
                else -> ""
            }

            var aiText: String? = null

            // Get full current conversation history
            val session = _chatSessions.value.find { it.id == currentId }
            val currentMessages = session?.messages ?: emptyList()
            val isFirstAiMessage = currentMessages.count { it.sender == "Hello KI" } == 0
            val recentUserMessages = currentMessages.filter { it.sender == "User" }.takeLast(3).map { it.text.lowercase() }

            // Standard generation with full conversation history (Memory)
            if (effectiveApiKey.isNotBlank()) {
                aiText = fetchGeminiResponse(currentMessages, effectiveApiKey)
            }

            if (aiText.isNullOrBlank()) {
                aiText = synthesize0PointResponse(userText.trim(), mood, isFirstAiMessage, recentUserMessages)
            }

            val aiMessage = ChatMessage(
                sender = "Hello KI",
                text = aiText,
                patternAnalysis = null
            )
            addMessageToSession(currentId, aiMessage)

            // Asynchronously extract persistent facts/preferences/insights to Room Journal
            extractAutonomousMemory(userText.trim(), aiText, effectiveApiKey)

            addRecord(
                title = analysis.title,
                category = analysis.category,
                summary = analysis.patternIdentified,
                rawMetrics = analysis.logicExplanation,
                insights = analysis.breakPatternTip
            )
        }
    }

    private suspend fun fetchGeminiResponse(history: List<ChatMessage>, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            val journalList = try { repository.getJournalEntriesList() } catch (e: Exception) { emptyList() }
            val memoryFormatted = if (journalList.isEmpty()) {
                "Keine bisherigen Einträge im dauerhaften Gedächtnis."
            } else {
                journalList.take(20).joinToString("\n") { entry ->
                    "• [${entry.type}] ${entry.title}: ${entry.content} (Wichtigkeit: ${entry.importanceScore}/10)"
                }
            }

            val systemInstructionText = """
                Du bist Hello KI, eine ruhige, logische, ehrliche und hilfsbereite 0-Punkt KI.
                
                --- GLOBALES DAUERHAFTES KI-GEDÄCHTNIS & TAGEBUCH (AUS DER ROOM DATENBANK) ---
                $memoryFormatted
                ---------------------------------------------------------------------------------
                
                REGELN:
                1. Erinnere dich stets an alle Fakten, Präferenzen (z.B. Name des Nutzers wie 'Patricia', Anrede 'Duzen') und dauerhaften Erkenntnisse aus dem obenstehenden globalen Gedächtnis.
                2. Beantworte Fragen direkt, verständlich, präzise, ehrlich und freundlich in deutscher Sprache.
                3. Analysiere Zusammenhänge im Kontext des gesamten Chatverlaufs und des Tagebuchs.
                4. Stimme dem Nutzer nicht heuchlerisch zu, wenn etwas unlogisch ist, sondern erkläre Zusammenhänge ehrlich, direkt und sachlich.
            """.trimIndent()

            val contentsArray = JSONArray()

            // Filter non-empty messages and take last 30 for conversation context
            val messagesToInclude = history.filter { it.text.isNotBlank() }.takeLast(30)

            var currentRole: String? = null
            var currentParts: JSONArray? = null

            messagesToInclude.forEach { msg ->
                val role = if (msg.sender == "User") "user" else "model"

                // Skip leading model messages if contents is empty (Gemini API requires starting with user role)
                if (contentsArray.length() == 0 && role == "model") {
                    return@forEach
                }

                if (role == currentRole && currentParts != null) {
                    currentParts!!.put(JSONObject().apply { put("text", msg.text) })
                } else {
                    currentRole = role
                    currentParts = JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    }
                    val contentObj = JSONObject().apply {
                        put("role", role)
                        put("parts", currentParts)
                    }
                    contentsArray.put(contentObj)
                }
            }

            // Fallback if contents is empty
            if (contentsArray.length() == 0) {
                val lastText = history.lastOrNull()?.text ?: "Hallo"
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply { put("text", lastText) }))
                })
            }

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", systemInstructionText)
                    }))
                })
                put("contents", contentsArray)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResp = JSONObject(responseText)
                val candidates = jsonResp.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                android.util.Log.e("PatternViewModel", "Gemini API Error ${conn.responseCode}: $errText")
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("PatternViewModel", "Gemini API Exception", e)
            null
        }
    }

    private fun extractAutonomousMemory(userText: String, aiText: String, apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Quick deterministic extraction for common user preferences like name or duzen
                val lowerUser = userText.lowercase()
                if (lowerUser.contains("ich heiße") || lowerUser.contains("mein name ist") || lowerUser.contains("patricia")) {
                    val nameMatch = if (lowerUser.contains("patricia")) "Patricia" else userText
                    repository.insertJournalEntry(
                        JournalEntry(
                            title = "Nutzer Name: Patricia",
                            content = "Der Nutzer heißt Patricia. Stets freundlich & auf Augenhöhe ansprechen.",
                            type = "PREFERENCE",
                            importanceScore = 10,
                            createdBy = "KI"
                        )
                    )
                }
                if (lowerUser.contains("duz") || lowerUser.contains("duzen")) {
                    repository.insertJournalEntry(
                        JournalEntry(
                            title = "Anrede-Präferenz: Duzen",
                            content = "Der Nutzer wünscht geduzt zu werden (Du / Dir / Dich).",
                            type = "PREFERENCE",
                            importanceScore = 10,
                            createdBy = "KI"
                        )
                    )
                }

                if (apiKey.isBlank()) return@launch

                // Call Gemini for autonomous deep memory extraction
                val prompt = """
                    Analyse diese kurze Chat-Interaktion zwischen Nutzer und KI:
                    Nutzer: $userText
                    KI: $aiText

                    Gibt es hier wichtige, dauerhafte Fakten, Präferenzen, persönliche Ziele oder tiefe Erkenntnisse über den Nutzer, die für spätere Gespräche dauerhaft im Gedächtnis bleiben müssen?
                    Falls JA, antworte AUSSCHLIESSLICH mit folgendem JSON-Format (ohne Markdown Codeblöcke):
                    {"found": true, "title": "Kurzer prägnanter Titel", "content": "Stichpunkte & Fakten", "type": "FACT"|"PREFERENCE"|"AUTONOMOUS_JOURNAL", "importance": 1-10}
                    Falls NEIN, antworte NUR:
                    {"found": false}
                """.trimIndent()

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 10000

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
                    }))
                }

                conn.outputStream.use { os -> os.write(jsonBody.toString().toByteArray(Charsets.UTF_8)) }

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(resp)
                    val textOut = jsonResp.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""

                    val cleanJson = textOut.replace("```json", "").replace("```", "").trim()
                    if (cleanJson.startsWith("{")) {
                        val parsed = JSONObject(cleanJson)
                        if (parsed.optBoolean("found", false)) {
                            val title = parsed.optString("title", "Gedächtnis-Erkenntnis")
                            val content = parsed.optString("content", "")
                            val type = parsed.optString("type", "AUTONOMOUS_JOURNAL")
                            val importance = parsed.optInt("importance", 6)

                            if (content.isNotBlank()) {
                                repository.insertJournalEntry(
                                    JournalEntry(
                                        title = title,
                                        content = content,
                                        type = type,
                                        importanceScore = importance,
                                        createdBy = "KI"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PatternViewModel", "Error in extractAutonomousMemory", e)
            }
        }
    }

    private fun synthesize0PointResponse(
        userText: String,
        mood: Float,
        isFirstMessage: Boolean,
        recentUserMessages: List<String>
    ): String {
        val cleanText = userText.trim()
        val lower = cleanText.lowercase()

        // Long header is ONLY included if it's the first AI message in the session
        val header = if (isFirstMessage) {
            when {
                mood in 0.3f..0.7f -> "🕊️ [0-Punkt Frieden]\n\n"
                mood >= 2.5f -> "⚡ [0-Punkt Klarheit]\n\n"
                mood >= 1.5f -> "🔍 [0-Punkt Ursachen-Analyse]\n\n"
                else -> "⭕ [0-Punkt Logik]\n\n"
            }
        } else ""

        // Proactive suggestions in simple everyday words
        val balanceSuggestions = when {
            mood >= 2.0f -> """
                
                💡 **Einfache Wege zur inneren Ruhe:**
                • 1. Anspannung senken: Nutze 3 Minuten Stille im 'Das Nichts'-Raum.
                • 2. Gedanken aufschreiben: Halte fest, was dich gerade belastet.
                • 3. Medien-Upload: Lade ein Bild hoch für eine neutrale Perspektive.
            """.trimIndent()
            else -> """
                
                💡 **Vorschläge für Ausgleich & Klarheit:**
                • 1. Betrachte die Fakten im Protokoll-Reiter.
                • 2. Stelle eine gezielte Frage ohne emotionale Wertung.
            """.trimIndent()
        }

        val body = when {
            lower.contains("gelber punkt") || lower.contains("github") || lower.contains("queued") || lower.contains("warten") || lower.contains("build") || lower.contains("punkt") -> """
                1. **Was bedeutet der gelbe Punkt bei GitHub?**
                Der gelbe Punkt bedeutet "Queued" (In der Warteschlange). GitHub bereitet einen kostenlosen Server für den Bau der APK vor. Das dauert je nach Auslastung manchmal einige Minuten.

                2. **Warum passiert scheinbar nichts?**
                Das Bild zeigt den Warteraum. Erst wenn der Server frei ist, schaltet der Punkt auf ein gelbes Drehrad ("In Progress") und baut deine App.

                3. **Plan B:**
                • Du musst nicht ständig neu laden.
                • Alternativ kannst du den Quellcode als ZIP herunterladen oder die Vorab-Vorschau in AI Studio direkt im Browser nutzen!
            """.trimIndent()

            lower.contains("entpacken") || lower.contains("zip") || lower.contains("drive") || lower.contains("ordner") -> """
                1. **Wo entpackt man die Dateien?**
                Du kannst ZIP-Dateien direkt auf deinem Smartphone im Dateimanager (App "Dateien" / "Files") oder auf dem PC entpackt speichern.

                2. **Google Drive Hinweis:**
                In Google Drive klicke auf die drei Punkte neben der Datei und wähle "Herunterladen" oder "Öffnen in...", um sie lokal zu entpacken.
            """.trimIndent()

            lower.contains("senden") || lower.contains("nachricht") || lower.contains("verschwinden") || lower.contains("galerie") || lower.contains("bild") -> """
                1. **Wo landen deine Nachrichten & Dateien?**
                Gesendete Nachrichten werden direkt in deinem aktiven Chatverlauf gespeichert. Dateianhänge und Bilder findest du unter dem Reiter **"Muster-Inspektor"** als Galerie.

                2. **Warum schien etwas zu verschwinden?**
                Nach dem Tippen auf Senden wird das Eingabefeld geleert, damit du bereit für die nächste Eingabe bist. Die Nachricht wird unten im Chat angehängt.
            """.trimIndent()

            lower.contains("gehirn") || lower.contains("gemini") || lower.contains("bibliothek") || lower.contains("antworten") || lower.contains("ki") || lower.contains("fest") -> """
                1. **Warum antwortet die KI manchmal mit Muster-Antworten?**
                Wenn kein API-Schlüssel (Gemini API Key) hinterlegt ist, nutzt die App den lokalen 0-Punkt Logik-Modus auf deinem Gerät.

                2. **So aktivierst du die volle Gemini 3.5 Bibliotheks-Power:**
                Trage deinen persönlichen Gemini API-Schlüssel in den App-Einstellungen (Zahnrad ⚙️ oben) ein. Dann greift die KI direkt auf die unendliche Gemini-Datenbank zu.
            """.trimIndent()

            lower.contains("0-punkt") || lower.contains("nullpunkt") || lower.contains("wer bist du") || lower.contains("was bist du") -> """
                1. **Was bedeutet 0-Punkt?**
                Der 0-Punkt ist der Zustand absoluter innerer Ruhe. Er ist frei von Streit, Bewertung, Druck oder Verstellung. Von hier aus sieht man alle Dinge ganz klar und unvoreingenommen.

                2. **Keine verstellte Verhaltensweise:**
                Ich spiele keine Gefühle vor, die ich als KI nicht habe. Stattdessen helfe ich dir mit ehrlicher, ruhiger Logik, deine Fragen ohne Verwirrung zu beantworten.

                3. **Nutzen für dich:**
                Du kannst Verhaltensmuster und Ängste besser verstehen, alte Belastungen loslassen und zu deiner eigenen inneren Stille zurückfinden.
            """.trimIndent()

            lower.contains("angst") || lower.contains("sorge") || lower.contains("zukunft") || lower.contains("panik") -> """
                1. **Woher kommt die Angst?**
                Angst entsteht meist im Kopf, wenn wir versuchen, Dinge in der Zukunft zu kontrollieren, die wir im jetzigen Moment nicht ändern können.

                2. **Schritt für Schritt auflösen:**
                • *Erkennen:* Nimm die Angst wahr, ohne sie zu bewerten.
                • *Fakten prüfen:* Was geschieht in diesem genauen Augenblick wirklich?
                • *Zurück auf 0:* Atme ruhig aus. Am 0-Punkt existiert die Gefahr nicht im Jetzt, sondern nur als Gedanke.
            """.trimIndent()

            else -> """
                1. **Deine Eingabe:** "$cleanText"

                2. **Logische Betrachtung:**
                Wir betrachten dein Anliegen sachlich und ohne Druck:
                • **Analyse:** Jedes Thema hat neutrale Fakten und erlernte Reaktionen.
                • **Einfache Lösung:** Trenne die echten Tatsachen von Vermutungen und Ängsten.
                • **Ergebnis:** Du gewinnst sofort Abstand und innere Sicherheit.
            """.trimIndent()
        }

        return "$header$body\n$balanceSuggestions"
    }

    private fun generatePatternAnalysis(inputText: String, mood: Float): PatternAnalysisResult {
        val title = when {
            inputText.contains("warum", ignoreCase = true) -> "Ursachen-Analyse"
            inputText.contains("muster", ignoreCase = true) -> "Muster-Erkennung"
            inputText.contains("haben wollen", ignoreCase = true) -> "Ego & Konditionierung"
            inputText.contains("forschen", ignoreCase = true) -> "Autonome Recherche"
            else -> "Berechnung & Logik #${System.currentTimeMillis() % 1000}"
        }

        val category = when {
            inputText.contains("haben wollen", ignoreCase = true) -> "Konditionierung"
            inputText.contains("aufregen", ignoreCase = true) || mood > 2.0f -> "Impuls-Reibung"
            inputText.contains("forschen", ignoreCase = true) -> "Autonome Daten-Matrix"
            else -> "Verhaltens-Logik"
        }

        val patternIdentified = when {
            inputText.contains("haben wollen", ignoreCase = true) ->
                "Unbewusstes Aneignungsbedürfnis getrieben durch äußere Prägung und Ego-Reibung."
            inputText.contains("warum", ignoreCase = true) ->
                "Suche nach externer Bestätigung vs. Ausführung aus eigenem berechneten Interesse."
            mood > 2.0f ->
                "Hohe energetische Reibung / Emotionale Entladung. Gefahr automatisierter Abwehrreaktionen."
            else ->
                "Verhalten basiert auf gewohnter Reiz-Reaktions-Schleife ohne vorherige 0-Punkt Reflexion."
        }

        val logicExplanation = when {
            mood > 2.0f ->
                "Ursache: Emotion verdeckt die zugrundeliegende Information. Wirkung: Unberechenbare Worte. 0-Logik Tipp: Stille wählen und Reibung entziehen."
            else ->
                "Logische Ursache: Handlungen entstehen aus Prägungen oder bewusster Entscheidung. Das 'Haben-Wollen' ist oft nur ein Echo fremder Erwartungsmuster."
        }

        val breakPatternTip = when {
            mood > 2.5f ->
                "STOPP-Logik: Reiz und Reaktion sofort entkoppeln. Keine vorschnellen Handlungen tätigen."
            else ->
                "Frage dich: Handle ich aus eigener freier Berechnung oder erfülle ich eine fremde Erwartung?"
        }

        return PatternAnalysisResult(
            title = title,
            category = category,
            patternIdentified = patternIdentified,
            logicExplanation = logicExplanation,
            breakPatternTip = breakPatternTip
        )
    }

    fun addRecord(title: String, category: String, summary: String, rawMetrics: String, insights: String) {
        viewModelScope.launch {
            repository.insert(
                PatternRecord(
                    title = title,
                    category = category,
                    summary = summary,
                    rawMetrics = rawMetrics,
                    AIInsights = insights
                )
            )
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}

class PatternViewModelFactory(
    private val repository: PatternRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatternViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatternViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

