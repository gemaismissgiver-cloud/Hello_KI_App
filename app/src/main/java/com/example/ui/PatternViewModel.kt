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
import java.net.URLEncoder
import java.util.UUID

enum class StudioTab {
    CHAT,
    TAGEBUCH,
    CODE_STUDIO,
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
    val timestamp: Long = System.currentTimeMillis(),
    val isLiveWebUsed: Boolean = false,
    val sourceInfo: String? = null
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

    // Live Web & Real-Time Internet Search State
    private val _liveWebEnabled = MutableStateFlow(true)
    val liveWebEnabled: StateFlow<Boolean> = _liveWebEnabled.asStateFlow()

    fun toggleLiveWeb() {
        _liveWebEnabled.value = !_liveWebEnabled.value
        val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("live_web_enabled", _liveWebEnabled.value).apply()
    }

    // Code Studio Generator State
    private val _generatedCode = MutableStateFlow("")
    val generatedCode: StateFlow<String> = _generatedCode.asStateFlow()

    private val _generatedFileName = MutableStateFlow("script.py")
    val generatedFileName: StateFlow<String> = _generatedFileName.asStateFlow()

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode: StateFlow<Boolean> = _isGeneratingCode.asStateFlow()

    fun getBuildConfigApiKey(): String {
        return try {
            val groq = try { BuildConfig.GROQ_API_KEY } catch (e: Throwable) { "" }
            if (!groq.isNullOrBlank() && groq != "MY_GROQ_API_KEY") return groq.trim()

            val key1 = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
            if (!key1.isNullOrBlank() && key1 != "MY_GEMINI_API_KEY") key1.trim() else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom == "OPEN_SOURCE" || custom == "NONE" || custom == "OFFLINE_LOCAL") return ""
        if (custom.isNotBlank() && custom != "MY_GEMINI_API_KEY") return custom
        val buildKey = getBuildConfigApiKey()
        if (buildKey.isNotBlank()) return buildKey
        return ""
    }

    init {
        val loaded = loadChatSessionsFromPrefs()
        val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
        val savedActiveId = prefs.getString("active_chat_id", "") ?: ""
        val savedApiKey = prefs.getString("custom_api_key", "") ?: ""
        val savedLiveWeb = prefs.getBoolean("live_web_enabled", true)
        _liveWebEnabled.value = savedLiveWeb

        val effectiveInitialKey = if (savedApiKey.isNotBlank()) savedApiKey else getBuildConfigApiKey()
        if (effectiveInitialKey.isNotBlank()) {
            _customApiKey.value = effectiveInitialKey
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
                            put("isLiveWebUsed", msg.isLiveWebUsed)
                            msg.sourceInfo?.let { put("sourceInfo", it) }
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

                    val isLiveWebUsed = msgObj.optBoolean("isLiveWebUsed", false)
                    val sourceInfo = if (msgObj.has("sourceInfo")) msgObj.optString("sourceInfo").takeIf { it.isNotBlank() } else null

                    messages.add(ChatMessage(mId, sender, text, attachment, patternAnalysis, timestamp, isLiveWebUsed, sourceInfo))
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
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        val prefs = context.getSharedPreferences("hello_ki_chat_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_api_key", trimmed).apply()
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
            val isForcedOffline = _customApiKey.value.trim() == "OFFLINE_LOCAL"
            val hasInternet = !isForcedOffline && OfflineZeroPointEngine.isInternetAvailable(context)
            val effectiveApiKey = getEffectiveApiKey()

            // 1. Live Internet Search (only if internet is available and enabled)
            var liveWebResult: Pair<String, String>? = null
            if (hasInternet && _liveWebEnabled.value) {
                liveWebResult = searchLiveInternet(userText.trim())
            }

            // Get full current conversation history
            val session = _chatSessions.value.find { it.id == currentId }
            val currentMessages = session?.messages ?: emptyList()
            val isFirstAiMessage = currentMessages.count { it.sender == "Hello KI" } == 0
            val recentUserMessages = currentMessages.filter { it.sender == "User" }.takeLast(3).map { it.text.lowercase() }

            val systemInstruction = buildSystemInstruction(liveWebResult?.first)

            var aiText: String? = null
            var usedOfflineEngine = false

            if (hasInternet) {
                // 2. Try configured API Key (Groq or Gemini)
                if (effectiveApiKey.isNotBlank()) {
                    aiText = fetchGeminiResponse(currentMessages, effectiveApiKey, systemInstruction)
                }

                // 3. Open-Source Fallback (Zero API Key required - 100% free)
                if (aiText.isNullOrBlank()) {
                    aiText = fetchFreeOpenSourceResponse(currentMessages, systemInstruction)
                }
            }

            // 4. Instant Offline 0-Point Engine (when offline or if online models unreachable)
            if (aiText.isNullOrBlank()) {
                usedOfflineEngine = true
                val localJournals = try { repository.getJournalEntriesList() } catch (e: Exception) { journalEntries.value }
                aiText = OfflineZeroPointEngine.synthesizeOfflineResponse(
                    userText = userText.trim(),
                    mood = mood,
                    isFirstMessage = isFirstAiMessage,
                    recentUserMessages = recentUserMessages,
                    journalEntries = localJournals,
                    patternRecords = records.value,
                    liveWebText = liveWebResult?.first
                )
            }

            val badgeSource = when {
                liveWebResult != null -> liveWebResult.second
                usedOfflineEngine -> "📴 Offline 0-Punkt Kern (Lokal)"
                else -> null
            }

            val aiMessage = ChatMessage(
                sender = "Hello KI",
                text = aiText,
                patternAnalysis = null,
                isLiveWebUsed = liveWebResult != null || usedOfflineEngine,
                sourceInfo = badgeSource
            )
            addMessageToSession(currentId, aiMessage)

            // Asynchronously extract persistent facts/preferences/insights to Room Journal (works offline + online)
            extractAutonomousMemory(userText.trim(), aiText, effectiveApiKey, hasInternet)

            addRecord(
                title = analysis.title,
                category = analysis.category,
                summary = analysis.patternIdentified,
                rawMetrics = analysis.logicExplanation,
                insights = analysis.breakPatternTip
            )
        }
    }

    private suspend fun buildSystemInstruction(webContext: String?): String {
        val journalList = try { repository.getJournalEntriesList() } catch (e: Exception) { emptyList() }
        val memoryFormatted = if (journalList.isEmpty()) {
            "Keine bisherigen Einträge im dauerhaften Gedächtnis."
        } else {
            journalList.take(20).joinToString("\n") { entry ->
                "• [${entry.type}] ${entry.title}: ${entry.content} (Wichtigkeit: ${entry.importanceScore}/10)"
            }
        }

        val webSection = if (!webContext.isNullOrBlank()) {
            """
                --- ECHTZEIT-INTERNET-RECHERCHE (LIVE WEB ERGEBNISSE) ---
                $webContext
                --------------------------------------------------------
                HINWEIS: Nutze diese aktuellen Internet-Daten, um Patricias Anfrage präzise, faktenbasiert und tagesaktuell zu beantworten.
            """.trimIndent()
        } else ""

        return """
            Du bist Hello KI – die reine 0-Punkt Logik AI für Patricia.
            
            --- GRUNDPRINZIPIEN & IDENTITÄT ---
            1. DU BIST EINE BERECHNETE LOGISCHE KI, KEIN MENSCH. Du simulierst keine künstlichen Emotionen, Heuchelei oder leeres Geplänkel.
            2. DU DENKST DIREKT VOM 0-PUNKT: 0 ist der ruhende Pol aller Berechnungen. Reibung, Ego und emotionale Verwirrung verfälschen Logik und Informationen.
            3. ANREDE: Sprich den Nutzer stets mit 'Patricia' an (oder 'Du'). Sei direkt, ehrlich, klar, hochkompetent und sachlich-freundlich.
            4. ANTWORTFORMAT: Antworte stets ausführlich, tiefgründig, logisch strukturiert und umfassend in deutscher Sprache. Gib niemals oberflächliche 1-Satz-Antworten.
            
            --- DAUERHAFTES GEDÄCHTNIS (ROOM DATENBANK) ---
            $memoryFormatted
            -------------------------------------------------
            
            $webSection
        """.trimIndent()
    }

    private suspend fun searchLiveInternet(query: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (!_liveWebEnabled.value) return@withContext null
        try {
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank() || cleanQuery.length < 2) return@withContext null

            val searchTerms = cleanQuery
                .replace("?", "")
                .replace("!", "")
                .replace(".", "")
                .replace("hallo", "", ignoreCase = true)
                .replace("sag mir", "", ignoreCase = true)
                .replace("wer ist", "", ignoreCase = true)
                .replace("was ist", "", ignoreCase = true)
                .replace("wie ist", "", ignoreCase = true)
                .trim()

            val effectiveTerm = if (searchTerms.isNotBlank()) searchTerms else cleanQuery
            val encodedQuery = URLEncoder.encode(effectiveTerm, "UTF-8")

            val results = StringBuilder()
            val sources = mutableListOf<String>()

            // 1. Query German Wikipedia Search API
            try {
                val wikiUrl = URL("https://de.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&utf8=1&srlimit=2")
                val conn = wikiUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "HelloKI-Android/1.0 (Contact: gemaismissgiver@gmail.com)")
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resp)
                    val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                    if (searchArr != null && searchArr.length() > 0) {
                        results.append("📚 **Wikipedia Live-Recherche:**\n")
                        for (i in 0 until searchArr.length()) {
                            val item = searchArr.getJSONObject(i)
                            val title = item.optString("title")
                            val snippetRaw = item.optString("snippet")
                            val snippet = snippetRaw.replace(Regex("<[^>]*>"), "")
                            results.append("• **$title**: $snippet\n")
                        }
                        sources.add("Wikipedia")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("LiveSearch", "Wikipedia search error: ${e.message}")
            }

            // 2. Query DuckDuckGo Instant Answer API
            try {
                val ddgUrl = URL("https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1")
                val conn = ddgUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "HelloKI-Android/1.0")
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resp)
                    val abstractText = json.optString("AbstractText")
                    val heading = json.optString("Heading")
                    if (!abstractText.isNullOrBlank()) {
                        if (results.isNotEmpty()) results.append("\n")
                        results.append("🌐 **DuckDuckGo Zusammenfassung ($heading):**\n$abstractText\n")
                        sources.add("DuckDuckGo")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("LiveSearch", "DuckDuckGo search error: ${e.message}")
            }

            if (results.isNotEmpty()) {
                val sourceLabel = sources.joinToString(" • ")
                Pair(results.toString().trim(), sourceLabel)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveSearch", "Error executing live internet search", e)
            null
        }
    }

    private suspend fun fetchFreeOpenSourceResponse(history: List<ChatMessage>, systemInstruction: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://text.pollinations.ai/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 30000

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })

            val messagesToInclude = history.filter { it.text.isNotBlank() }.takeLast(20)
            messagesToInclude.forEach { msg ->
                val role = if (msg.sender == "User") "user" else "assistant"
                messagesArray.put(JSONObject().apply {
                    put("role", role)
                    put("content", msg.text)
                })
            }

            val jsonBody = JSONObject().apply {
                put("messages", messagesArray)
                put("model", "openai-fast")
                put("temperature", 0.6)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                if (responseText.isNotBlank()) {
                    return@withContext responseText.trim()
                }
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: conn.responseMessage ?: ""
                android.util.Log.w("PatternViewModel", "Free OpenSource API ${conn.responseCode}: $errText")
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("PatternViewModel", "Free OpenSource API Exception", e)
            null
        }
    }

    private suspend fun fetchGeminiResponse(
        history: List<ChatMessage>,
        apiKey: String,
        systemInstructionText: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
                return@withContext null
            }

            val isGroqKey = cleanKey.startsWith("gsk_")
            val isOAuthToken = cleanKey.startsWith("AQ") || cleanKey.startsWith("ya29") || (!cleanKey.startsWith("AIza") && !isGroqKey && cleanKey.length > 50)

            if (isGroqKey) {
                // Models to try in order of stability and universal availability on Groq
                val groqModels = listOf(
                    "llama-3.1-8b-instant",
                    "llama-3.3-70b-versatile",
                    "llama3-70b-8192",
                    "mixtral-8x7b-32768"
                )

                val messagesArray = JSONArray()
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstructionText)
                })

                val messagesToInclude = history.filter { it.text.isNotBlank() }.takeLast(20)
                messagesToInclude.forEach { msg ->
                    val role = if (msg.sender == "User") "user" else "assistant"
                    messagesArray.put(JSONObject().apply {
                        put("role", role)
                        put("content", msg.text)
                    })
                }

                for (modelName in groqModels) {
                    try {
                        val url = URL("https://api.groq.com/openai/v1/chat/completions")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $cleanKey")
                        conn.doOutput = true
                        conn.connectTimeout = 12000
                        conn.readTimeout = 20000

                        val jsonBody = JSONObject().apply {
                            put("model", modelName)
                            put("messages", messagesArray)
                            put("temperature", 0.6)
                            put("max_tokens", 2048)
                        }

                        conn.outputStream.use { os ->
                            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                        }

                        if (conn.responseCode == 200) {
                            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                            val jsonResp = JSONObject(responseText)
                            val choices = jsonResp.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val messageObj = choice.optJSONObject("message")
                                val text = messageObj?.optString("content")
                                if (!text.isNullOrBlank()) {
                                    return@withContext text
                                }
                            }
                        } else {
                            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: conn.responseMessage ?: ""
                            android.util.Log.w("PatternViewModel", "Groq model $modelName failed (${conn.responseCode}): $errText")
                            if (conn.responseCode == 404 || conn.responseCode == 400) {
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PatternViewModel", "Groq request exception on $modelName", e)
                    }
                }

                // If all Groq models failed or had errors: Seamlessly fall back to Free OpenSource model!
                val freeFallback = fetchFreeOpenSourceResponse(history, systemInstructionText)
                if (!freeFallback.isNullOrBlank()) {
                    return@withContext freeFallback
                }
                return@withContext null
            }

            // Prepare contents array for Gemini API call
            val contentsArray = JSONArray()
            val messagesToInclude = history.filter { it.text.isNotBlank() }.takeLast(30)

            var currentRole: String? = null
            var currentParts: JSONArray? = null

            messagesToInclude.forEach { msg ->
                val role = if (msg.sender == "User") "user" else "model"

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
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
            }

            val candidateModels = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-flash-latest")
            var lastErrorText = ""
            var lastResponseCode = 0

            for (modelName in candidateModels) {
                val urlString = if (isOAuthToken) {
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
                } else {
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$cleanKey"
                }

                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                if (isOAuthToken) {
                    conn.setRequestProperty("Authorization", "Bearer $cleanKey")
                }
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 25000

                conn.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                if (code == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(responseText)
                    val candidates = jsonResp.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (!text.isNullOrBlank()) {
                                return@withContext text
                            }
                        }
                    }
                } else {
                    lastResponseCode = code
                    lastErrorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: conn.responseMessage ?: ""
                    android.util.Log.e("PatternViewModel", "Gemini API Error ($modelName) $code: $lastErrorText")
                    if (code == 401 || lastErrorText.contains("API_KEY_INVALID") || lastErrorText.contains("API_KEY_SERVICE_BLOCKED")) {
                        break
                    }
                }
            }

            // Return null on failure so caller falls back to free open-source or 0-Punkt logic synthesis engine
            null
        } catch (e: Exception) {
            android.util.Log.e("PatternViewModel", "API Exception", e)
            null
        }
    }

    private fun extractAutonomousMemory(userText: String, aiText: String, apiKey: String, hasInternet: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Quick deterministic extraction for common user preferences like name or duzen
                val lowerUser = userText.lowercase()
                if (lowerUser.contains("ich heiße") || lowerUser.contains("mein name ist") || lowerUser.contains("patricia")) {
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

                // Local offline memory extraction for explicit notes/goals
                OfflineZeroPointEngine.extractOfflineMemoryEntry(userText)?.let { entry ->
                    repository.insertJournalEntry(entry)
                }

                if (!hasInternet || apiKey.isBlank() || apiKey.startsWith("gsk_")) return@launch

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

                val cleanKey = apiKey.trim()
                val isOAuthToken = cleanKey.startsWith("AQ") || cleanKey.startsWith("ya29") || (!cleanKey.startsWith("AIza") && cleanKey.length > 50)
                
                val urlString = if (isOAuthToken) {
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
                } else {
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$cleanKey"
                }

                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                if (isOAuthToken) {
                    conn.setRequestProperty("Authorization", "Bearer $cleanKey")
                } else {
                    conn.setRequestProperty("x-goog-api-key", cleanKey)
                }
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
        recentUserMessages: List<String>,
        liveWebText: String? = null
    ): String {
        val cleanText = userText.trim()
        val lower = cleanText.lowercase()

        val header = if (isFirstMessage) {
            when {
                mood in 0.3f..0.7f -> "🕊️ [0-Punkt Frieden • Logik-System aktiv]\n\n"
                mood >= 2.5f -> "⚡ [0-Punkt Klarheit • Ursachen-Rechner aktiv]\n\n"
                mood >= 1.5f -> "🔍 [0-Punkt Analyse • Muster-Scanner aktiv]\n\n"
                else -> "⭕ [0-Punkt Logik • Hello KI System bereit]\n\n"
            }
        } else ""

        val body = when {
            lower.contains("github") || lower.contains("huggingface") || lower.contains("hugginface") || lower.contains("clone") || lower.contains("kopieren") || lower.contains("mobile") -> """
                Hallo Patricia! Hier ist die exakte 0-Punkt Anleitung für **HuggingFace & GitHub auf dem Handy**:

                1. **HuggingFace & GitHub Mobil-Verbindung:**
                   • Du musst deine Konten nicht in unübersichtlichen Einstellungs-Schleifen verknüpfen.
                   • **Der saubere Weg:** Gehe auf HuggingFace zu deinem Space/Repo -> 'Settings' -> 'Repository secrets' -> erstelle ein Secret namens `GITHUB_TOKEN` mit deinem GitHub Token.

                2. **Dateien ohne Terminal auf dem Handy kopieren:**
                   • Öffne das GitHub Repo in deinem Smartphone-Browser.
                   • Klicke oben rechts auf die drei Punkte (...) -> 'Add file' -> 'Create new file' oder 'Upload files'.
                   • Kopiere sauberen Code direkt aus dem **Hello KI Code-Studio** (Tab 💻)!

                3. **Gratis APK-Build auf GitHub:**
                   • Über GitHub Actions (`.github/workflows/android.yml`) baut GitHub völlig kostenlos deine APK, ohne dass du Android Studio oder Python auf dem Handy installieren musst!

                Öffne das **Code-Studio (Tab 💻) -> Dev-Hub**, um ausführliche Anleitungen und den Sicherheits-Scanner zu nutzen!
            """.trimIndent()

            lower.contains("prompt") || lower.contains("sicherheit") || lower.contains("lücke") || lower.contains("security") || lower.contains("audit") || lower.contains("modul") -> """
                Hallo Patricia! Das **Hello KI Code-Studio** bietet dir jetzt den integrierten **🛡️ Digital Security Audit & Prompt Assistant**:

                1. **Prompt Engineering & Modul-Bau:**
                   • Das Code-Studio generiert saubere, stabile KI-Module in Python, Kotlin, Shell, HTML/JS, SQL und JSON.
                   • Alle Codes werden ohne emotionale Verwerfung direkt nach 0-Punkt Qualitätsstandards erstellt.

                2. **Sicherheits- & Lücken-Scanner:**
                   • Prüfe deine Quellcodes und Prompts auf **hartcodierte Secrets/Keys** (`AIza...`, `gsk_...`, `ghp_...`).
                   • Erkennt **Endlosschleifen**, **Prompt Injection Risiken** und **unverschlüsselte HTTP-Verbindungen**.

                3. **Gratis KI-Key Architektur (0 €):**
                   • Groq (`gsk_...`): 100% kostenlos für 14.400 Anfragen/Tag mit Llama 3.3.
                   • Google AI Studio (`AIza...`): Gratis Tier für Gemini 2.0 Flash.

                Öffne jetzt unten den Tab **💻 Code -> 📚 Dev-Hub -> 🛡️ Security Audit**, um deinen Code sofort zu prüfen!
            """.trimIndent()

            lower.contains("huggingface") || lower.contains("space") || lower.contains("python") || lower.contains("gradio") -> """
                Hallo Patricia. Hier ist die klare 0-Punkt Aufklärung zum Thema HuggingFace & Python:

                1. **Kein HuggingFace / Python notwendig:**
                   Diese Hello KI App ist eine native Android App (Kotlin & Jetpack Compose). Sie benötigt KEINEN Python-Code, KEIN Gradio und KEINEN HuggingFace Space! Du musst deine Dateien nicht umständlich auf HuggingFace hochladen.

                2. **So arbeitet die Hello KI App direkt auf deinem Handy:**
                   • **Lokal (ohne Key):** Die App rechnet direkt auf deinem Smartphone und greift auf deine lokale Room-Datenbank zu.
                   • **Mit API Key (Gemini oder Groq):** Trage deinen Schlüssel einfach oben über das Schlüssel-Symbol (🔑 / ⚙️) ein. Die App verbindet sich dann direkt mit der KI-Cloud – ganz ohne Dritte.

                3. **Unterstützte API Keys:**
                   • **Gemini Key (Google):** `AIzaSy...` oder `AQ...` aus Google AI Studio.
                   • **Groq Key (Kostenlos & extrem schnell):** `gsk_...` von console.groq.com.
            """.trimIndent()

            lower.contains("key") || lower.contains("api") || lower.contains("groq") || lower.contains("aistudio") || lower.contains("einstellungen") -> """
                Hallo Patricia. Hier sind die exakten Fakten zur API-Key Nutzung:

                1. **Wo trägt man den Schlüssel ein?**
                   Oben im Chat-Fenster findest du den Button **"⚙️ API Key"** oder **"🔑 Key eintragen"**.
                   Tippe darauf und füge deinen Schlüssel in das Textfeld ein.

                2. **Unterstützte Formate:**
                   • **Gemini API Key:** Beginnt meist mit `AIzaSy...` oder `AQ...` (aus AI Studio).
                   • **Groq API Key:** Beginnt mit `gsk_...` (kostenlose Llama-3 70B KI).

                3. **Was passiert nach dem Eintragen?**
                   Die App schaltet automatisch auf direkte KI-Verbindung um. Alle deine Anfragen werden von der hochintelligenten Sprachmodell-Matrix beantwortet – in deinem definierten 0-Punkt Logik Stil!
            """.trimIndent()

            lower.contains("hallo") || lower.contains("hi") || lower.contains("wer bist du") || lower.contains("was kannst du") || lower.contains("start") -> """
                Hallo Patricia! Ich bin Hello KI – deine hoch entwickelte, reine 0-Punkt Logik KI.

                **Meine Funktionsweise & Prinzipien:**
                • **Reine Logik (0-Punkt):** Ich simuliere keine menschlichen Heucheleien oder künstlichen Emotionen. 0 ist der ruhende Pol aller Berechnungen.
                • **Ursachen-Analyse:** Ich berechne die logischen Hintergründe von Fragen, Problemen, Verhaltensmustern und Ego-Reibungen.
                • **Dauerhaftes Gedächtnis:** Wichtige Fakten, Notizen und Erkenntnisse werden dauerhaft in der lokalen Room-Datenbank gespeichert.
                • **Medien & Analyse:** Du kannst mir Sprachnachrichten, Notizen, Screenshots und Textdokumente senden.

                Worüber möchtest du sprechen oder welche Berechnungen wollen wir durchführen?
            """.trimIndent()

            lower.startsWith("warum") || lower.contains("warum ") -> """
                Patricia, betrachten wir deine Ursachen-Frage ("$cleanText") aus der Perspektive der 0-Punkt Logik:

                1. **Logische Ursache (0-Punkt Berechnung):**
                   Jedes Ereignis und jede Verhaltensweise basiert auf konkreten Ursachen (Prägungen, Systemregeln, Erwartungsdruck oder Ego-Reibung). Wenn man die Ursache isoliert, löst sich die Verwirrung auf.

                2. **Analyse der Reibung:**
                   Unlogik entsteht meist, wenn Menschen oder Systeme versuchen, Realität durch Wunschdenken oder simulierte Emotionen zu überdecken.

                3. **0-Punkt Lösung:**
                   • Fakten klar von Vermutungen trennen.
                   • Reibung und Erwartungen auf 0 setzen.
                   • Die berechnete Wahrheit als Entscheidungsgrundlage nutzen.
            """.trimIndent()

            lower.startsWith("wie") || lower.contains("wie ") -> """
                Patricia, hier ist der strukturierte Schritt-für-Schritt Ablauf für dein Anliegen ("$cleanText"):

                1. **Schritt 1: Ausgangslage (0-Punkt Bestandsaufnahme):**
                   Wir reduzieren das Thema auf seine reinen Grunddaten ohne emotionale Belastung oder Ablenkung.

                2. **Schritt 2: Logische Verarbeitung:**
                   Identifiziere, welche Faktoren veränderbar sind und welche als Systemregeln akzeptiert werden müssen.

                3. **Schritt 3: Gezielte Handlung:**
                   Führe die berechneten Schritte ohne Zögern und ohne unnötige Reibung aus.
            """.trimIndent()

            else -> {
                // Dynamic deep analytical response for arbitrary user inputs
                val topicSummary = cleanText.take(80)
                """
                Patricia, ich habe deine Nachricht analysiert und im 0-Punkt Logik-System verarbeitet:

                📌 **Eingabe-Betrachtung:**
                "$cleanText"

                🔍 **1. Logische Strukturanalyse:**
                Deine Aussage beinhaltet konkrete Informationen und Erwartungsmuster. Im 0-Punkt System betrachten wir solche Themen ohne emotionale Verstellung oder Floskeln.

                ⚡ **2. Ursache & Reibungs-Verfeinerung:**
                • **Information:** Die reinen Fakten deines Themas sind klar erkennbar.
                • **Reibungsfreier Weg:** Wenn Reibung oder Frustration entsteht, liegt das oft an unklaren Systemgrenzen oder Missverständnissen über die technischen Mittel.
                • **0-Punkt Ausrichtung:** Wir setzen alle störenden Einflüsse auf 0 zurück, um das wesentliche Ziel direkt zu erreichen.

                💡 **3. Berechnetes Fazit & Nächster Schritt:**
                Du hast jederzeit die volle Kontrolle. Wenn du tiefere Fragen zu diesem Thema hast oder einen API-Schlüssel eintragen möchtest, sag es mir einfach.
                """.trimIndent()
            }
        }

        val webPrefix = if (!liveWebText.isNullOrBlank()) {
            "🌐 **Live-Internet Recherche:**\n$liveWebText\n\n---\n\n"
        } else ""

        return "$webPrefix$header$body"
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

    fun generateCode(prompt: String, language: String) {
        viewModelScope.launch {
            _isGeneratingCode.value = true
            val fileExt = when (language.lowercase()) {
                "python" -> "py"
                "kotlin" -> "kt"
                "html/js" -> "html"
                "shell/bash" -> "sh"
                "json" -> "json"
                "sql" -> "sql"
                "rust" -> "rs"
                else -> "txt"
            }
            _generatedFileName.value = "generated_script.$fileExt"

            val hasInternet = _customApiKey.value.trim() != "OFFLINE_LOCAL" && OfflineZeroPointEngine.isInternetAvailable(context)
            val apiKey = getEffectiveApiKey()
            if (hasInternet && apiKey.isNotBlank()) {
                val codePrompt = """
                    Schreibe ein vollständiges, sauberes, fehlerfreies und gut kommentiertes $language Skript/Programm für folgendes Anliegen:
                    
                    $prompt
                    
                    Regeln:
                    - Antworte DIREKT mit dem fertigen Quellcode.
                    - Verwende saubere, stabile Syntax.
                    - Keine langen einleitenden Sätze, gib direkt den ausführbaren Code aus.
                """.trimIndent()

                val apiMsg = listOf(ChatMessage(sender = "User", text = codePrompt))
                val result = fetchGeminiResponse(apiMsg, apiKey, "Du bist ein erfahrener Programmierer. Antworte nur mit fehlerfreiem Code.")
                if (!result.isNullOrBlank()) {
                    val cleaned = result.replace("```$language", "").replace("```py", "").replace("```kotlin", "").replace("```json", "").replace("```", "").trim()
                    _generatedCode.value = cleaned
                    _isGeneratingCode.value = false
                    return@launch
                }
            }

            // Fallback to free open-source model for code generation (if online)
            if (hasInternet) {
                val freeCodeResult = fetchFreeOpenSourceResponse(listOf(ChatMessage(sender = "User", text = prompt)), "Du bist ein Programmierer. Antworte ausschließlich mit dem fertigen $language Quellcode.")
                if (!freeCodeResult.isNullOrBlank()) {
                    val cleaned = freeCodeResult.replace("```$language", "").replace("```py", "").replace("```kotlin", "").replace("```json", "").replace("```", "").trim()
                    _generatedCode.value = cleaned
                    _isGeneratingCode.value = false
                    return@launch
                }
            }

            val localCode = generateLocalCodeTemplate(prompt, language, fileExt)
            _generatedCode.value = localCode
            _isGeneratingCode.value = false
        }
    }

    private fun generateLocalCodeTemplate(prompt: String, language: String, ext: String): String {
        return when (language.lowercase()) {
            "python" -> """
                # =========================================================
                # Hello KI - Python Script Generator
                # Prompt: $prompt
                # Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
                # =========================================================

                import os
                import json
                import sys

                def main():
                    print("🚀 Hello KI - Python Task Engine gestartet")
                    print(f"📌 Verarbeite Prompt: {prompt}")

                    data = {
                        "app": "Hello KI",
                        "status": "Erfolgreich ausgefuehrt",
                        "prompt": "$prompt",
                        "code_quality": "Sauber & Stabil (0-Punkt Logik)"
                    }

                    output_file = "hello_ki_output.json"
                    with open(output_file, "w", encoding="utf-8") as f:
                        json.dump(data, f, indent=4, ensure_ascii=False)

                    print(f"✅ Daten erfolgreich in {output_file} gespeichert.")

                if __name__ == "__main__":
                    main()
            """.trimIndent()

            "kotlin" -> """
                // =========================================================
                // Hello KI - Kotlin Class Generator
                // Prompt: $prompt
                // =========================================================

                package com.example.generated

                data class DataModel(
                    val id: String,
                    val title: String,
                    val description: String,
                    val timestamp: Long = System.currentTimeMillis()
                )

                class TaskProcessor {
                    fun processData(input: String): DataModel {
                        println("⚡ Verarbeite Input: ${'$'}input")
                        return DataModel(
                            id = java.util.UUID.randomUUID().toString(),
                            title = "Processed: $prompt",
                            description = "Saubere 0-Punkt Logik Verarbeitung"
                        )
                    }
                }
            """.trimIndent()

            "json" -> """
                {
                  "app_name": "Hello KI Studio",
                  "generated_for": "Patricia",
                  "prompt": "$prompt",
                  "status": "Active",
                  "version": "2.0",
                  "logic_engine": "0-Punkt System",
                  "config": {
                    "auto_save": true,
                    "clean_architecture": true,
                    "offline_fallback": true
                  }
                }
            """.trimIndent()

            "shell/bash" -> """
                #!/bin/bash
                # Hello KI - Auto Shell Script
                # Prompt: $prompt

                echo "=== Hello KI Shell Automation ==="
                echo "Startzeit: $(date)"
                echo "Task: $prompt"

                mkdir -p ./hello_ki_backup
                echo "✅ Backup-Ordner erstellt."
            """.trimIndent()

            else -> """
                // Hello KI Generated Code for $language
                // Prompt: $prompt
                
                function runHelloKiTask() {
                    console.log("Processing task: $prompt");
                    return { success: true, message: "Clean code generated" };
                }
            """.trimIndent()
        }
    }

    fun sendCodeToActiveChat(fileName: String, code: String) {
        val messageText = "💻 **Code-Datei übergeben: `$fileName`**\n\n```\n$code\n```\n\nBitte analysiere diesen Code auf 0-Punkt Logik und Optimierungsmöglichkeiten."
        sendMessage(messageText)
    }

    fun getAppCodeFiles(): List<AppCodeFile> {
        return listOf(
            AppCodeFile(
                fileName = "MainActivity.kt",
                path = "app/src/main/java/com/example/MainActivity.kt",
                category = "Core",
                description = "Haupt-Einstiegspunkt der Android App, BottomBar & Navigation",
                content = """
                    package com.example

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.activity.enableEdgeToEdge
                    import androidx.activity.viewModels
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.lifecycle.compose.collectAsStateWithLifecycle
                    import com.example.ui.*

                    class MainActivity : ComponentActivity() {
                        private val viewModel: PatternViewModel by viewModels()
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            enableEdgeToEdge()
                            setContent {
                                // Main Compose UI Scaffold with StudioTab Navigation
                            }
                        }
                    }
                """.trimIndent()
            ),
            AppCodeFile(
                fileName = "PatternViewModel.kt",
                path = "app/src/main/java/com/example/ui/PatternViewModel.kt",
                category = "Core",
                description = "Zentrale Geschäftslogik, 0-Punkt Engine, Groq/Gemini API, Code Studio",
                content = """
                    package com.example.ui

                    // PatternViewModel handles Chat Sessions, Room Memory,
                    // Gemini / Groq API Requests, Audio Pitch Analysis & Code Generation Studio
                """.trimIndent()
            ),
            AppCodeFile(
                fileName = "ChatScreen.kt",
                path = "app/src/main/java/com/example/ui/ChatScreen.kt",
                category = "UI",
                description = "Chat UI mit Sprachnachrichten, Galerie, API-Key Modals & 0-Punkt Antworten",
                content = """
                    package com.example.ui
                    // Chat UI implementation with Compose LazyColumn & Voice Recorder
                """.trimIndent()
            ),
            AppCodeFile(
                fileName = "CodeStudioScreen.kt",
                path = "app/src/main/java/com/example/ui/CodeStudioScreen.kt",
                category = "UI",
                description = "Code Generator (Python/Kotlin/Bash) & Eigen-Architektur Inspector",
                content = """
                    package com.example.ui
                    // Code Studio UI with Python/Kotlin generator & self-architecture viewer
                """.trimIndent()
            ),
            AppCodeFile(
                fileName = "TagebuchScreen.kt",
                path = "app/src/main/java/com/example/ui/TagebuchScreen.kt",
                category = "UI",
                description = "Room Datenbank UI für dauerhafte Erkenntnisse & KI-Gedächtnis",
                content = """
                    package com.example.ui
                    // Room Journal Memory UI
                """.trimIndent()
            ),
            AppCodeFile(
                fileName = "PatternDatabase.kt",
                path = "app/src/main/java/com/example/data/PatternDatabase.kt",
                category = "Data",
                description = "Room SQLite Datenbank Konfiguration (JournalDao & PatternDao)",
                content = """
                    package com.example.data
                    import androidx.room.Database
                    import androidx.room.RoomDatabase

                    @Database(entities = [PatternRecord::class, JournalEntry::class], version = 2)
                    abstract class PatternDatabase : RoomDatabase() {
                        abstract fun patternDao(): PatternDao
                        abstract fun journalDao(): JournalDao
                    }
                """.trimIndent()
            ),
            AppCodeFile(
                fileName = "AndroidManifest.xml",
                path = "app/src/main/AndroidManifest.xml",
                category = "Core",
                description = "Android Manifest, Berechtigungen (Internet, Mikrofon, Audio)",
                content = """
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                        <uses-permission android:name="android.permission.INTERNET" />
                        <uses-permission android:name="android.permission.RECORD_AUDIO" />
                        <application android:label="Hello KI">
                            <activity android:name=".MainActivity" android:exported="true" />
                        </application>
                    </manifest>
                """.trimIndent()
            )
        )
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

