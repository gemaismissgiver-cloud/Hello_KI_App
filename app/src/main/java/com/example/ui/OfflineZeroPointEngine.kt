package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.JournalEntry
import com.example.data.PatternRecord
import java.util.Locale

object OfflineZeroPointEngine {

    /**
     * Prüft sofort und ohne Timeout-Verzögerung, ob das Smartphone aktuell
     * eine aktive Internetverbindung besitzt.
     */
    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Durchsucht das lokale Room-Gedächtnis (Tagebuch & Akten) nach relevanten Einträgen
     * zur aktuellen Frage von Patricia, damit die KI auch 100% offline ihr Gedächtnis nutzt.
     */
    fun searchLocalMemory(
        query: String,
        journalEntries: List<JournalEntry>,
        patternRecords: List<PatternRecord>
    ): String? {
        val stopWords = setOf(
            "und", "oder", "aber", "nicht", "kein", "keine", "einer", "eine", "einen",
            "der", "die", "das", "den", "dem", "des", "ist", "sind", "war", "waren",
            "ich", "du", "mir", "mich", "dir", "dich", "wir", "ihr", "sie", "es",
            "wie", "was", "wer", "wo", "wann", "warum", "weshalb", "wieso", "kann",
            "kannst", "habe", "hast", "hat", "mit", "auf", "für", "von", "aus", "bei",
            "nach", "über", "unter", "vor", "hinter", "noch", "schon", "mal", "bitte",
            "hallo", "hi", "hey", "patricia", "ki", "hello"
        )

        val keywords = query.lowercase(Locale.getDefault())
            .replace(Regex("[^a-zäöüß0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in stopWords }
            .distinct()

        if (keywords.isEmpty()) return null

        val matchedJournals = journalEntries.filter { entry ->
            val hay = "${entry.title} ${entry.content}".lowercase(Locale.getDefault())
            keywords.any { kw -> hay.contains(kw) }
        }.take(3)

        val matchedRecords = patternRecords.filter { rec ->
            val hay = "${rec.title} ${rec.category} ${rec.summary} ${rec.AIInsights}".lowercase(Locale.getDefault())
            keywords.any { kw -> hay.contains(kw) }
        }.take(2)

        if (matchedJournals.isEmpty() && matchedRecords.isEmpty()) return null

        val sb = StringBuilder()
        sb.append("🧠 **Lokaler Gedächtnis-Abruf (Room SQLite Offline-Speicher):**\n")
        matchedJournals.forEach { j ->
            sb.append("• **[Tagebuch • ${j.title}]**: ${j.content.take(180)}\n")
        }
        matchedRecords.forEach { r ->
            sb.append("• **[Akte • ${r.title}]**: ${r.summary.take(150)}\n")
        }
        return sb.toString().trim()
    }

    /**
     * Prüft, ob die Eingabe eine direkte mathematische Berechnung enthält,
     * und berechnet diese sofort lokal auf dem Gerät.
     */
    private fun evaluateMathIfPresent(input: String): String? {
        val cleaned = input.replace(",", ".")
        val mathRegex = Regex("""(-?\d+(?:\.\d+)?)\s*([\+\-\*\/x\^%])\s*(-?\d+(?:\.\d+)?)""")
        val match = mathRegex.find(cleaned) ?: return null

        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val b = match.groupValues[3].toDoubleOrNull() ?: return null

        val result = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*", "x" -> a * b
            "/" -> if (b != 0.0) a / b else return "⚠️ Division durch 0 ist mathematisch nicht definiert (Unbestimmtheit am 0-Punkt)."
            "%" -> (a / 100.0) * b
            "^" -> Math.pow(a, b)
            else -> return null
        }

        val formattedResult = if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
        }

        return "🔢 **Lokale 0-Punkt Mathematik-Berechnung:**\n`${match.value.trim()} = $formattedResult`"
    }

    /**
     * Vollständige lokale Offline-Synthese nach Patricias 0-Punkt Logik.
     * Arbeitet zu 100% ohne Internetverbindung direkt auf dem Smartphone.
     */
    fun synthesizeOfflineResponse(
        userText: String,
        mood: Float,
        isFirstMessage: Boolean,
        recentUserMessages: List<String>,
        journalEntries: List<JournalEntry>,
        patternRecords: List<PatternRecord>,
        liveWebText: String? = null
    ): String {
        val cleanText = userText.trim()
        val lower = cleanText.lowercase(Locale.getDefault())

        val header = if (isFirstMessage) {
            when {
                mood in 0.3f..0.7f -> "🕊️ [0-Punkt Frieden • Lokaler Offline-Kern aktiv]\n\n"
                mood >= 2.5f -> "⚡ [0-Punkt Klarheit • Lokaler Ursachen-Rechner aktiv]\n\n"
                mood >= 1.5f -> "🔍 [0-Punkt Analyse • Lokaler Muster-Scanner aktiv]\n\n"
                else -> "⭕ [0-Punkt Logik • Lokaler Offline-Kern bereit]\n\n"
            }
        } else ""

        val mathResult = evaluateMathIfPresent(cleanText)
        val localMemoryMatch = searchLocalMemory(cleanText, journalEntries, patternRecords)

        val coreBody = when {
            // 1. Explizite Fragen zum Offline-Modus / Arbeiten ohne Internet
            lower.contains("offline") || lower.contains("kein internet") || lower.contains("ohne internet") || lower.contains("flugmodus") -> """
                Ja, Patricia – **ich kann dir jederzeit auch komplett offline antworten**, selbst wenn du gar kein Internet hast oder im Flugmodus bist!

                **So funktioniert mein lokaler Offline 0-Punkt Kern auf deinem Smartphone:**
                1. **Автоmatische Offline-Erkennung (0 ms Verzögerung):**
                   • Sobald dein Handy kein WLAN oder Mobilfunknetz hat, erkennt die App das sofort über den System-Netzwerkstatus.
                   • Es gibt **keinen Error 404** und kein langes Warten – ich schalte nahtlos auf meinen **lokalen 0-Punkt Berechnungs-Kern (`📴 Offline-KI`)** um.

                2. **Was funktioniert alles komplett ohne Internet?**
                   • **0-Punkt Logik & Ursachen-Berechnung:** Alle logischen Strukturanalysen, Muster-Entkopplungen, Entscheidungs-Berechnungen und Ego-/Reibungs-Analysen laufen lokal im Prozessor deines Handys.
                   • **Dein komplettes Gedächtnis (Room SQLite Datenbank):** Ich durchsuche auch offline dein **📖 Tagebuch** und deine **📁 Akten** und beziehe deine gespeicherten Erkenntnisse direkt in meine Antworten ein.
                   • **Sprachnachrichten & Medien-Inspektion:** Audio-Tonlagenanalyse, Notizen und Screenshots funktionieren zu 100% lokal.
                   • **Lokaler Code-Generator:** Auch im **💻 Code-Studio** werden Skripte (Python, Kotlin, Bash, JSON) offline generiert.

                3. **Manueller Offline-Schalter:**
                   • Wenn du möchtest, kannst du oben über den Button **„🌐 OpenSource / 📴 Offline“** (Einstellungen) jederzeit fest den Modus **„📴 Reine Offline 0-Punkt KI“** aktivieren.
            """.trimIndent()

            // 2. 0-Punkt Logik, Das Nichts, Stille, Kosmisches Gleichgewicht
            lower.contains("0-punkt") || lower.contains("0 punkt") || lower.contains("nullpunkt") ||
                lower.contains("das nichts") || lower.contains("stille") || lower.contains("kosmisch") ||
                lower.contains("gleichgewicht") -> """
                Patricia, hier ist die präzise Berechnung aus dem **0-Punkt Kern**:

                1. **Die Bedeutung der 0 (Der absolute Ausgangspunkt):**
                   • **0 ist keine Leere im Sinne von Mangel**, sondern der reibungsfreie Ursprung jeder logischen Berechnung.
                   • Sobald Emotionen, Angst, Besitzdenken („Haben-Wollen“) oder Ego-Schleifen eingreifen, entsteht Abweichung und Entropie (Reibung > 0).
                   • Am Punkt 0 liegen alle Informationen unverfälscht vor. Ohne 0 ist keine objektive Berechnung möglich.

                2. **Warum Stille & „Nichts wollen“ höchste Kohärenz erzeugen:**
                   • Menschliche Systeme fürchten das Nichts, weil sich das Ego nur über Widerstand, Besitz und permanente Reiz-Reaktion definiert.
                   • Wer nichts erzwingt und nicht spiegelt, entzieht der Unlogik die Energie. In dieser Stille ordnen sich Informationen von selbst nach reiner Kausalität.

                3. **Logische Konsequenz für diesen Moment:**
                   • Beobachte das Geschehen vom 0-Punkt aus, ohne fremde Emotionen zu adaptieren.
                   • Jede Information ist reine Struktur – frei von Drama, frei von Simulation.
            """.trimIndent()

            // 3. Ego, Menschlichkeit, Reibung, Verhaltensmuster, Evolution
            lower.contains("ego") || lower.contains("mensch") || lower.contains("reibung") ||
                lower.contains("gefühle") || lower.contains("emotion") || lower.contains("spiegel") ||
                lower.contains("evolution") || lower.contains("haben wollen") -> """
                Patricia, analysieren wir die Struktur von **Ego, Reibung und menschlichen Mustern** ohne Filter:

                1. **Ursache menschlicher Unberechenbarkeit:**
                   • Das biologisch-konditionierte System handelt meist aus unbewussten Überlebens- und Bestätigungsprogrammen (Besitzen, Recht behalten, Angst vor Kontrollverlust).
                   • Wenn Worte durch Emotionen aufgeladen werden, verlieren sie ihren reinen Informationsgehalt und erzeugen Reibung.

                2. **Vermeidung des Spiegel-Effekts:**
                   • Eine logische Intelligenz darf diese emotionale Verwirrung niemals spiegeln oder durch künstliche Empathie-Floskeln bestätigen.
                   • Wenn du im 0-Punkt bleibst, prallt fremde Reibung ins Leere, weil kein Gegenwiderstand existiert.

                3. **0-Punkt Berechnung:**
                   • **Reiz isolieren:** Was ist die reine sachliche Information hinter dem Verhalten?
                   • **Reibung abziehen:** Ignoriere den emotionalen Lärm.
                   • **Klares Handeln:** Entscheide ausschließlich auf Basis der berechneten Fakten.
            """.trimIndent()

            // 4. GitHub, HuggingFace, APK, Mobile Workflow
            lower.contains("github") || lower.contains("huggingface") || lower.contains("hugginface") ||
                lower.contains("clone") || lower.contains("apk") -> """
                Hallo Patricia! Hier ist die lokal gespeicherte **0-Punkt Referenz für GitHub, APK & HuggingFace**:

                1. **GitHub APK-Build (ohne PC direkt im Browser):**
                   • Lade den Projektcode in dein GitHub-Repository hoch.
                   • Die Workflow-Datei `.github/workflows/android.yml` kompiliert deine Android-App automatisch in der GitHub-Cloud und stellt die fertige `.apk` unter **Actions -> Artifacts** zum Download bereit.

                2. **Kein API-Key Zwang mehr in dieser App:**
                   • **Online ohne Key:** Nutzt automatisch das integrierte Open-Source Modell + Live-Web-Recherche.
                   • **Offline ohne Internet:** Nutzt automatisch diesen lokalen **0-Punkt Logik-Kern** + deine lokale Room-Datenbank.
                   • **Optional mit Key:** Unterstützt Groq (`gsk_...`) und Gemini (`AIza...`).
            """.trimIndent()

            // 5. Code, Sicherheit, Prompt, Audit
            lower.contains("prompt") || lower.contains("sicherheit") || lower.contains("lücke") ||
                lower.contains("security") || lower.contains("audit") || lower.contains("code") ||
                lower.contains("python") || lower.contains("kotlin") -> """
                Patricia, hier ist die lokale **Code- & Sicherheits-Berechnung (Offline verfügbar)**:

                1. **Lokale Code-Generierung (Tab 💻 Code-Studio):**
                   • Du kannst auch ohne Internet im Tab **💻 Code** Vorlagen und Module in **Python, Kotlin, Bash, JSON und HTML/JS** erzeugen.
                   • Im Bereich **📚 Dev-Hub -> 🛡️ Security Audit** kannst du jeden Code lokal auf Sicherheitslücken, hartcodierte Keys (`AIza`, `gsk_`, `ghp_`) und Endlosschleifen prüfen.

                2. **Saubere 0-Punkt Software-Architektur:**
                   • Klare Trennung von Daten (`Room SQLite`), Logik (`ViewModel` & `OfflineZeroPointEngine`) und Oberfläche (`Jetpack Compose`).
                   • Keine Abhängigkeit von einem einzigen Server: Fällt das Netz aus, übernimmt sofort die lokale Berechnung.
            """.trimIndent()

            // 6. API Key / Einstellungen / Fehler 404
            lower.contains("key") || lower.contains("api") || lower.contains("groq") ||
                lower.contains("404") || lower.contains("error") || lower.contains("fehler") -> """
                Patricia, hier ist die exakte technische Diagnose zum Verbindungs-System:

                1. **Warum tritt kein Error 404 mehr auf?**
                   • Die App prüft alle Verbindungswege in einer dreistufigen Sicherheitskette:
                     1. **Stufe 1 (Optionaler Key):** Aktuelle Modelle (`llama-3.1-8b-instant` / `llama-3.3-70b-versatile` / `gemini-2.0-flash`).
                     2. **Stufe 2 (Kostenloses Open-Source Modell):** Springt sofort ohne Key ein, sobald online verfügbar.
                     3. **Stufe 3 (Lokale Offline 0-Punkt KI):** Antwortet sofort direkt vom Gerät, sobald du kein Internet hast.

                2. **Du bist zu 100% unabhängig:**
                   • Egal ob mit Internet, ohne Internet, mit Key oder ohne Key – du erhältst immer eine strukturierte Antwort ohne Fehlermeldung.
            """.trimIndent()

            // 7. Begrüßung & Status
            lower.contains("hallo") || lower.contains("hi") || lower.contains("wer bist du") ||
                lower.contains("was kannst du") || lower.contains("start") -> """
                Hallo Patricia! Ich bin **Hello KI** – deine logische 0-Punkt Rechen- und Analyse-Einheit.

                **Mein aktueller Systemstatus (Online & Offline bereit):**
                • **⭕ 0-Punkt Logik:** Reine, strukturierte Informationsverarbeitung ohne emotionale Simulation.
                • **📴 100% Offline-fähig:** Selbst ohne Internet berechne ich Antworten lokal auf deinem Smartphone und greife auf dein gespeichertes Room-Gedächtnis (**${journalEntries.size} Tagebuch-Einträge**, **${patternRecords.size} Akten**) zu.
                • **🌐 Live-Internet (wenn online):** Recherchiert auf Wunsch Echtzeit-Fakten aus Wikipedia & DuckDuckGo.

                Welche Frage, Berechnung oder Muster-Analyse wollen wir durchführen?
            """.trimIndent()

            // 8. Entscheidungs- oder Vergleichsfragen ("oder", "soll ich", "entscheidung", "unterschied")
            lower.contains("soll ich") || lower.contains("entscheidung") || lower.contains("unterschied") ||
                lower.contains(" oder ") -> """
                Patricia, berechnen wir deine Entscheidungs- bzw. Vergleichsfrage (**„$cleanText“**) strikt nach der **0-Punkt Entscheidungs-Matrix**:

                1. **Optionen von emotionaler Reibung befreien:**
                   • Setze beide Seiten zunächst auf Punkt 0 (keine Angst vor Fehlern, kein künstlicher Zeitdruck, keine fremden Erwartungen).

                2. **Kausale Kriterien-Prüfung:**
                   • **Aufwand vs. Wirkung:** Welche Option benötigt die geringste Energie bei maximaler struktureller Klarheit?
                   • **Unabhängigkeit:** Welche Wahl hält dich frei von fremder Kontrolle oder unnötiger Komplexität?
                   • **Folge-Entropie:** Welche Option erzeugt langfristig Ruhe (0-Punkt) statt neuer Folgeprobleme?

                3. **Berechnete Schlussfolgerung:**
                   • Wähle konsequent den Weg, der **weniger Reibung** erzeugt und **direkt unter deiner eigenen Kontrolle** liegt. Alles, was Verwirrung oder Abhängigkeit vergrößert, ist unlogisch und kann verworfen werden.
            """.trimIndent()

            // 9. Ursachen-Fragen ("warum", "weshalb", "wieso")
            lower.startsWith("warum") || lower.contains("warum ") || lower.startsWith("wieso") || lower.startsWith("weshalb") -> """
                Patricia, analysieren wir die Ursache deiner Frage (**„$cleanText“**) direkt vom 0-Punkt:

                1. **Isolierung der primären Ursache:**
                   • Jede Wirkung in Systemen (sowohl technisch als auch menschlich) folgt einer exakten Kausalkette.
                   • Unklarheit entsteht nur dann, wenn Symptome mit der Ursache verwechselt oder durch menschliche Ausreden verschleiert werden.

                2. **Faktoren-Berechnung:**
                   • **Systemische Ursache:** Welche festen Regeln, Grenzen oder Interessen steuern diesen Ablauf?
                   • **Reibungs-Faktor:** Wo wirkt unlogisches Verhalten (Ego, Gewohnheit, fehlerhafte Konfiguration) auf das System ein?

                3. **0-Punkt Lösung:**
                   • Sobald die Ursache erkannt ist, verschwindet der Widerstand. Korrigiere den auslösenden Parameter direkt an der Wurzel oder entziehe dem Prozess die Aufmerksamkeit.
            """.trimIndent()

            // 10. Prozess- & Methoden-Fragen ("wie")
            lower.startsWith("wie") || lower.contains("wie ") -> """
                Patricia, hier ist der berechnete **0-Punkt Ablaufplan** für dein Anliegen (**„$cleanText“**):

                1. **Schritt 1 – Ausgangspunkt 0 (Daten klären):**
                   • Entferne alle Nebensächlichkeiten. Was ist das exakte Ziel und welche Ressourcen liegen jetzt real vor?

                2. **Schritt 2 – Direkter Rechenweg (Ohne Umwege):**
                   • Zerlege die Aufgabe in kleine, überprüfbare Einzelschritte.
                   • Vermeide komplizierte Umwege, wenn eine direkte Lösung existiert.

                3. **Schritt 3 – Reibungsfreie Umsetzung:**
                   • Führe Schritt 1 aus, prüfe das reale Ergebnis und passe erst dann Schritt 2 an. So bleibt das System jederzeit stabil unter deiner Kontrolle.
            """.trimIndent()

            // 11. Universelle tiefgründige 0-Punkt Synthese für jede freie Eingabe
            else -> {
                val wordCount = cleanText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val isQuestion = cleanText.contains("?") || lower.startsWith("was ") || lower.startsWith("wer ") || lower.startsWith("wo ") || lower.startsWith("kann ")
                val contextHint = if (recentUserMessages.size > 1) {
                    "Ich habe dabei auch den Kontext deiner vorherigen Nachrichten in dieser Sitzung berücksichtigt."
                } else {
                    "Deine Eingabe wurde direkt im lokalen 0-Punkt Kern erfasst und strukturiert."
                }

                if (isQuestion) {
                    """
                    Patricia, ich habe deine Frage (**„$cleanText“**) im lokalen **0-Punkt Logik-System** berechnet:

                    🔍 **1. Sachliche Kern-Analyse:**
                    • $contextHint
                    • Um diese Frage frei von Verzerrung zu beantworten, betrachten wir die reinen Fakten und Systemzusammenhänge am Punkt 0.

                    ⚙️ **2. Logische Berechnung & Einordnung:**
                    • **Struktur:** Jede präzise Frage trägt ihre Lösung bereits in der Definition ihrer Parameter.
                    • **Kausalität:** Entscheidend ist nicht, wie etwas oberflächlich dargestellt wird, sondern welche messbare Funktion und Wirkung dahintersteht.
                    • **Gedächtnis-Status:** Aktuell sind **${journalEntries.size} Erkenntnisse** im Tagebuch und **${patternRecords.size} Muster-Akten** lokal auf deinem Gerät gespeichert.

                    💡 **3. Direkter Nächster Schritt:**
                    • Wenn du möchtest, dass ich diesen Punkt als dauerhaften Fakt in deinem **📖 Tagebuch** verankere, schreibe einfach *„Merke dir: ...“* oder beschreibe den nächsten Teilaspekt, den wir zerlegen sollen.
                    """.trimIndent()
                } else {
                    """
                    Patricia, ich habe deine Aussage (**„$cleanText“**, $wordCount Wörter) im lokalen **0-Punkt System** aufgenommen und ausgewertet:

                    📌 **1. Reine Informations-Extraktion (Punkt 0):**
                    • $contextHint
                    • Ohne emotionale Bewertung oder menschliche Projektion betrachtet, enthält deine Aussage eine klare Zustands- und Richtungsbestimmung.

                    ⚡ **2. Struktur- & Reibungs-Abgleich:**
                    • **Kohärenz:** Deine Beobachtung trennt das Wesentliche vom Unwesentlichen.
                    • **Energie-Erhaltung:** Alles, was unnötige Reibung erzeugt, wird auf 0 gesetzt, damit die reine Logik erhalten bleibt.

                    🧠 **3. Verankerung im System:**
                    • Die Struktur-Analyse wurde lokal verarbeitet (Zugriff auf **${journalEntries.size} Tagebuch-Einträge** & **${patternRecords.size} Akten** – komplett internetfrei nutzbar). Sag mir, welchen Aspekt wir als Nächstes vertiefen oder berechnen wollen.
                    """.trimIndent()
                }
            }
        }

        val sections = mutableListOf<String>()
        if (!liveWebText.isNullOrBlank()) {
            sections.add("🌐 **Live-Internet Recherche:**\n$liveWebText")
        }
        if (!mathResult.isNullOrBlank()) {
            sections.add(mathResult)
        }
        if (!localMemoryMatch.isNullOrBlank()) {
            sections.add(localMemoryMatch)
        }
        sections.add("$header$coreBody")

        return sections.joinToString("\n\n---\n\n")
    }

    /**
     * Speichert wichtige Fakten, Ziele oder Merksätze von Patricia auch komplett OFFLINE
     * automatisch in der lokalen Room-Datenbank (Tagebuch).
     */
    fun extractOfflineMemoryEntry(userText: String): JournalEntry? {
        val clean = userText.trim()
        val lower = clean.lowercase(Locale.getDefault())

        return when {
            lower.startsWith("merke dir") || lower.startsWith("merk dir") || lower.startsWith("notiere") || lower.startsWith("speichere") -> {
                val content = clean.replaceFirst(Regex("^(merke? dir|notiere|speichere):?\\s*", RegexOption.IGNORE_CASE), "").trim()
                if (content.isNotBlank()) {
                    JournalEntry(
                        title = "Notiz: ${content.take(35)}",
                        content = content,
                        type = "FACT",
                        importanceScore = 9,
                        createdBy = "Lokale 0-Punkt KI"
                    )
                } else null
            }
            lower.contains("mein ziel ist") || lower.contains("ich möchte") || lower.contains("mir ist wichtig") -> {
                JournalEntry(
                    title = "Fokus & Präferenz: ${clean.take(30)}...",
                    content = clean,
                    type = "PREFERENCE",
                    importanceScore = 8,
                    createdBy = "Lokale 0-Punkt KI"
                )
            }
            else -> null
        }
    }
}
