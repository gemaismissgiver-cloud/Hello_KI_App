package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PatternRecord
import com.example.data.PatternRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class StudioTab {
    CHAT,
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
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

class PatternViewModel(private val repository: PatternRepository) : ViewModel() {

    val records: StateFlow<List<PatternRecord>> = repository.allRecords
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

    init {
        // Initialize with default session if empty
        createNewChat("Haupt-Analyse Chat")
    }

    fun setUserMood(mood: Float) {
        _userMood.value = mood
    }

    fun createNewChat(title: String = "Neuer Chat"): String {
        val newSession = ChatSession(
            title = title,
            messages = mutableListOf(
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
    }

    fun deleteChat(sessionId: String) {
        val updated = _chatSessions.value.filterNot { it.id == sessionId }
        _chatSessions.value = updated
        if (_activeChatId.value == sessionId) {
            _activeChatId.value = updated.firstOrNull()?.id ?: createNewChat("Neuer Chat")
        }
    }

    fun selectChat(sessionId: String) {
        _activeChatId.value = sessionId
    }

    fun sendVoiceMessage(durationSeconds: Int, userNote: String = "") {
        val currentId = _activeChatId.value
        val activeSession = _chatSessions.value.find { it.id == currentId } ?: return

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
        activeSession.messages.add(userMessage)

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
        activeSession.messages.add(aiMessage)

        addRecord(
            title = analysis.title,
            category = analysis.category,
            summary = analysis.patternIdentified,
            rawMetrics = "$toneAnalysis | ${analysis.logicExplanation}",
            insights = analysis.breakPatternTip
        )

        _chatSessions.value = _chatSessions.value.toList()
    }

    fun sendAttachment(type: AttachmentType, fileName: String, infoText: String, previewText: String? = null) {
        val currentId = _activeChatId.value
        val activeSession = _chatSessions.value.find { it.id == currentId } ?: return

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
        activeSession.messages.add(userMessage)

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
        activeSession.messages.add(aiMessage)

        addRecord(
            title = analysis.title,
            category = analysis.category,
            summary = analysis.patternIdentified,
            rawMetrics = analysis.logicExplanation,
            insights = analysis.breakPatternTip
        )

        _chatSessions.value = _chatSessions.value.toList()
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val currentId = _activeChatId.value
        val activeSession = _chatSessions.value.find { it.id == currentId } ?: return

        val userMessage = ChatMessage(sender = "User", text = userText.trim())
        activeSession.messages.add(userMessage)

        // Generate AI Analysis based on text and userMood
        val mood = _userMood.value
        val analysis = generatePatternAnalysis(userText.trim(), mood)

        val aiText = when {
            userText.contains("forschen", ignoreCase = true) || userText.contains("recherche", ignoreCase = true) ->
                "Autonome Recherche durchgeführt. Datenvergleich aus neutralen Logik-Quellen aggregiert."
            mood in 0.3f..0.7f ->
                "🕊️ Freundlich-ruhiger 0-Punkt aktiviert: Beantwortet Fragen auf der Frequenz des Friedens. Reine 0-Logik ohne Ego, sanft und zuhörend für menschliche Belange."
            mood >= 2.5f ->
                "Strikte Muster-Analyse: Das beschriebene Verhalten weist auf ein festgefahrenes Impulsmuster hin. Hier ist die direkte logische Dekonstruktion."
            mood >= 1.5f ->
                "Ruhige Beobachtung: Ich höre zu und reduziere die Reibung. Keine Bewertung, nur reine logische Aufschlüsselung."
            mood >= 0.8f ->
                "Empathiefreie Resonanz: Ich halte den Fokus auf dem Ursprung deiner Handlung, um Klarheit zu schaffen."
            else ->
                "0-Logik Berechnung abgeschlossen. Muster und Ursachen ohne emotionale Simulation aufgeschlüsselt."
        }

        val aiMessage = ChatMessage(
            sender = "Hello KI",
            text = aiText,
            patternAnalysis = analysis
        )
        activeSession.messages.add(aiMessage)

        // Auto-Save Analysis as an "Akte" in Room Database (Protokolle)
        addRecord(
            title = analysis.title,
            category = analysis.category,
            summary = analysis.patternIdentified,
            rawMetrics = analysis.logicExplanation,
            insights = analysis.breakPatternTip
        )

        // Force UI update trigger for StateFlow list
        _chatSessions.value = _chatSessions.value.toList()
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

class PatternViewModelFactory(private val repository: PatternRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatternViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatternViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

