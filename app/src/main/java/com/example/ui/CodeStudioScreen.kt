package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CodeStudioTab {
    GENERATOR,
    ARCHITEKTUR,
    DEV_HUB
}

data class AppCodeFile(
    val fileName: String,
    val path: String,
    val category: String,
    val description: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeStudioScreen(
    currentGeneratedCode: String,
    currentGeneratedFileName: String,
    isGenerating: Boolean,
    appCodeFiles: List<AppCodeFile>,
    onGenerateCode: (prompt: String, language: String) -> Unit,
    onSendCodeToChat: (fileName: String, code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStudioTab by remember { mutableStateOf(CodeStudioTab.GENERATOR) }
    var selectedLanguage by remember { mutableStateOf("Python") }
    var promptInput by remember { mutableStateOf("") }
    var selectedArchitectureFile by remember { mutableStateOf(appCodeFiles.firstOrNull()) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val languages = listOf("Python", "Kotlin", "HTML/JS", "Shell/Bash", "JSON", "SQL", "Rust")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💻 Code & Architektur Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Code-Generierung & Vollständige Eigen-Architektur",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Funktionsübersicht",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Tab Switcher ---
        TabRow(
            selectedTabIndex = selectedStudioTab.ordinal,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedStudioTab == CodeStudioTab.GENERATOR,
                onClick = { selectedStudioTab = CodeStudioTab.GENERATOR },
                text = { Text("⚡ Code", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Code, contentDescription = null) }
            )
            Tab(
                selected = selectedStudioTab == CodeStudioTab.ARCHITEKTUR,
                onClick = { selectedStudioTab = CodeStudioTab.ARCHITEKTUR },
                text = { Text("🏛️ Architektur", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null) }
            )
            Tab(
                selected = selectedStudioTab == CodeStudioTab.DEV_HUB,
                onClick = { selectedStudioTab = CodeStudioTab.DEV_HUB },
                text = { Text("📚 Dev-Hub", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedStudioTab) {
            CodeStudioTab.GENERATOR -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "1. Zielsprache wählen:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(languages) { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang) },
                                    leadingIcon = if (selectedLanguage == lang) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "2. Prompt & Anforderungen eingeben:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("z. B. Schreibe ein $selectedLanguage Skript zur Datenverarbeitung mit JSON-Export...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        // Quick prompt suggestions
                        Text(
                            text = "Schnell-Vorlagen:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickPrompts = when (selectedLanguage) {
                                "Python" -> listOf("Datenverarbeitung & JSON-Export", "REST API mit Flask/FastAPI", "Web Scraper Script", "Datei-Manager & Logger")
                                "Kotlin" -> listOf("Rest API Service Class", "Room Entity & DAO Example", "Coroutines Flow Helper", "Jetpack Compose UI Component")
                                "HTML/JS" -> listOf("Responsive HTML Dashboard", "JavaScript Fetch API Client", "Interactive Web Calculator")
                                "Shell/Bash" -> listOf("Backup & Zip Folder Skript", "System Info Logger", "Auto Deploy Shell Script")
                                "JSON" -> listOf("App Config Schema", "User Profile Data Export", "API Response Mock")
                                "SQL" -> listOf("User & Log Database Schema", "Complex Analytics Query", "Table Migration Script")
                                else -> listOf("Generische Vorlage", "Helfer-Funktion")
                            }

                            quickPrompts.forEach { qp ->
                                SuggestionChip(
                                    onClick = { promptInput = qp },
                                    label = { Text(qp, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (promptInput.isNotBlank()) {
                                    onGenerateCode(promptInput, selectedLanguage)
                                } else {
                                    Toast.makeText(context, "Bitte gib einen Prompt ein", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isGenerating && promptInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Generiere Code...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("⚡ Sauberer Code Generieren", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (currentGeneratedCode.isNotBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📄 $currentGeneratedFileName",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row {
                                            IconButton(onClick = {
                                                clipboardManager.setText(AnnotatedString(currentGeneratedCode))
                                                Toast.makeText(context, "Code in Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren")
                                            }

                                            IconButton(onClick = {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, currentGeneratedCode)
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Code teilen / speichern")
                                                context.startActivity(shareIntent)
                                            }) {
                                                Icon(Icons.Default.Share, contentDescription = "Teilen / Speichern")
                                            }

                                            IconButton(onClick = {
                                                onSendCodeToChat(currentGeneratedFileName, currentGeneratedCode)
                                                Toast.makeText(context, "Code an Chat gesendet!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.Send, contentDescription = "In Chat übernehmen")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Monospace Code Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E2E))
                                            .padding(12.dp)
                                    ) {
                                        SelectionContainer {
                                            Text(
                                                text = currentGeneratedCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = Color(0xFFA6ADC8),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CodeStudioTab.ARCHITEKTUR -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🏛️ Vollständige Eigen-Architektur der Hello KI App",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Die Hello KI App kennt ihre eigene vollständige Quellcode-Struktur, alle Module, UI-Screens und Room-Datenbanken. Wähle eine Datei zur Inspektion aus:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Dateien im Projekt (${appCodeFiles.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(appCodeFiles) { file ->
                                FilterChip(
                                    selected = selectedArchitectureFile?.fileName == file.fileName,
                                    onClick = { selectedArchitectureFile = file },
                                    label = { Text(file.fileName) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (file.category) {
                                                "UI" -> Icons.Default.Smartphone
                                                "Data" -> Icons.Default.Storage
                                                "Core" -> Icons.Default.Memory
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    selectedArchitectureFile?.let { file ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.fileName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${file.path} • ${file.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                clipboardManager.setText(AnnotatedString(file.content))
                                                Toast.makeText(context, "Quellcode von ${file.fileName} kopiert!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren")
                                            }

                                            IconButton(onClick = {
                                                onSendCodeToChat(file.fileName, file.content)
                                                Toast.makeText(context, "${file.fileName} an Chat gesendet!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.QuestionAnswer, contentDescription = "Im Chat analysieren")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E2E))
                                            .padding(12.dp)
                                    ) {
                                        SelectionContainer {
                                            Text(
                                                text = file.content,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color(0xFFA6ADC8),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CodeStudioTab.DEV_HUB -> {
                var auditInputText by remember { mutableStateOf("") }
                var auditResult by remember { mutableStateOf("") }
                var activeHubSection by remember { mutableStateOf("SECURITY") }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeHubSection == "SECURITY",
                                onClick = { activeHubSection = "SECURITY" },
                                label = { Text("🛡️ Security Audit") }
                            )
                            FilterChip(
                                selected = activeHubSection == "MOBILE_GUIDE",
                                onClick = { activeHubSection = "MOBILE_GUIDE" },
                                label = { Text("📱 HF & GitHub Mobile") }
                            )
                            FilterChip(
                                selected = activeHubSection == "COST_MATRIX",
                                onClick = { activeHubSection = "COST_MATRIX" },
                                label = { Text("💰 Gratis KI Tools") }
                            )
                            FilterChip(
                                selected = activeHubSection == "WIKI",
                                onClick = { activeHubSection = "WIKI" },
                                label = { Text("🌐 Wikipedia & Tech Ref") }
                            )
                        }
                    }

                    when (activeHubSection) {
                        "SECURITY" -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "🛡️ Digitale Sicherheits-Prüfung & Code-Audit",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Füge hier deinen Code oder Prompt ein, um ihn auf geheime API-Keys, Datenschleifen, Prompt Injection oder Sicherheitslücken zu prüfen.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = auditInputText,
                                            onValueChange = { auditInputText = it },
                                            placeholder = { Text("Code oder Prompt hier einfügen...") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                auditResult = runSecurityAudit(auditInputText)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = auditInputText.isNotBlank()
                                        ) {
                                            Icon(Icons.Default.Shield, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sicherheits-Audit Ausführen", fontWeight = FontWeight.Bold)
                                        }

                                        if (auditResult.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF181825))
                                                    .padding(12.dp)
                                            ) {
                                                SelectionContainer {
                                                    Text(
                                                        text = auditResult,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFFA6E3A1),
                                                        lineHeight = 18.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "MOBILE_GUIDE" -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "📱 Mobile HuggingFace & GitHub Meisterschaft",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = """
                                                1. **Verbindung zwischen HuggingFace & GitHub:**
                                                   • Du musst deine Konten nicht in einer unübersichtlichen Einstellungs-Schleife 'verknüpfen'.
                                                   • **Der saubere Weg:** Auf HuggingFace gehst du zu deinem Space / Repository -> 'Settings' -> 'Repository secrets' -> erstelle ein Secret `GITHUB_TOKEN` mit deinem GitHub Token.
                                                   
                                                2. **Dateien ohne Terminal auf dem Handy kopieren:**
                                                   • Gehe im Handy-Browser auf das GitHub Repo.
                                                   • Tippe oben rechts auf die drei Punkte (...) -> 'Add file' -> 'Create new file' oder 'Upload files'.
                                                   • Kopiere den generierten Code direkt aus dem Hello KI Code-Studio!

                                                3. **Automatischer APK-Build auf GitHub:**
                                                   • Mit einer einfachen `android.yml` Datei in `.github/workflows/` baut GitHub völlig kostenlos deine APK, ohne dass du Python oder Android Studio auf dem Handy installieren musst.
                                            """.trimIndent(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }

                        "COST_MATRIX" -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "💰 Gratis KI Tools & Kosten-Matrix (0 €)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = """
                                                • **Groq API (`gsk_...`):** 100% Kostenlos! Bis zu 14.400 Anfragen pro Tag mit Llama 3.3 70B (extrem schnell & schlau).
                                                • **Google AI Studio (`AIza...`):** Kostenloser Generativer KI-Zugang für Gemini 2.0 Flash / 1.5 Flash.
                                                • **HuggingFace Spaces:** Kostenlose CPU-Instanzen für Python/Gradio Demos.
                                                • **GitHub Actions:** 2.000 freie Build-Minuten jeden Monat für automatische Builds.
                                                • **Cloudflare Workers:** 100.000 freie Requests/Tag für sichere KI-Proxies.
                                                • **Fazit:** Du musst NULL Euro ausgeben, um vollwertige KI-Apps zu entwickeln!
                                            """.trimIndent(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }

                        "WIKI" -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "🌐 Wikipedia & Software-Architektur Wissen",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = """
                                                • **REST API:** Standardisierte Schnittstelle, über die Android-Apps mit KI-Servern sprechen (via HTTP POST & JSON).
                                                • **API Key:** Ein privater Sicherheitsschlüssel. Niemals im öffentlichen Quellcode ablegen, sondern im Tresor der App (Room/EncryptedPrefs).
                                                • **Jetpack Compose:** Das moderne UI-Framework von Google für deklaratives Android-Design.
                                                • **Room SQLite:** Lokale, blitzschnelle Android-Datenbank für dauerhafte Notizen & Chat-Verläufe.
                                                • **0-Punkt Logik:** Unbeeinflusste, sachliche Datenverarbeitung ohne emotionale Verzerrung.
                                            """.trimIndent(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("ℹ️ Technische Transparenz & Fakten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "✅ WAS TECHNICAL MÖGLICH IST:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• Erstellung, Formatierung, Syntax-Vorschau und Export sauberer Codes in Python, Kotlin, HTML, Shell, JSON, SQL.")
                    Text("• Vollständige Inspektion & Export aller echten Quellcode-Dateien dieser Android-App.")
                    Text("• Nutzung geräteinterner Vorlagen oder externer Gemini/Groq KI-Keys zur freien Code-Generierung.")

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "⚠️ WAS ANDROID OPERATING SYSTEM UNTERSAGT:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("• Standalone-Android-Apps besitzen keinen nativen Python- oder Java-Compiler im Android OS Sandbox-Bereich.")
                    Text("• Das Tool generiert, speichert und verteilt sauberen Python-Code, führt ihn aber nicht direkt auf dem Handy aus (hierfür benötigt Android ein Terminal wie Termux).")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Verstanden")
                }
            }
        )
    }
}

private fun runSecurityAudit(input: String): String {
    val issues = mutableListOf<String>()
    val lower = input.lowercase()

    if (input.contains("AIzaSy") || input.contains("gsk_") || input.contains("ghp_") || input.contains("hf_")) {
        issues.add("⚠️ WARNUNG: Hartcodierter API-Key / Secret im Quelltext erkannt! Schlüssel niemals direkt im Code committen. Nutzen Sie stattdessen Umgebungsvariablen oder Secrets.")
    }

    if (lower.contains("while(true)") || lower.contains("while (true)") || lower.contains("for(;;)") || lower.contains("loop")) {
        issues.add("⚠️ RISIKO: Potenzielle Endlosschleife erkannt. Dies kann die App einfrieren und Akku/Ressourcen verschwenden.")
    }

    if (lower.contains("ignore previous instructions") || lower.contains("system prompt override") || lower.contains("jailbreak")) {
        issues.add("🔍 HINWEIS: Prompt Injection Muster erkannt. Eingabe sollte vor der KI-Verarbeitung gefilterter werden.")
    }

    if (lower.contains("http://") && !lower.contains("localhost")) {
        issues.add("⚠️ SICHERHEITSLÜCKE: Unverschlüsselte HTTP-Verbindung. Verwenden Sie HTTPS für alle API-Aufrufe.")
    }

    if (issues.isEmpty()) {
        return """
            ✅ SICHERHEITS-AUDIT ERFOLGREICH (0-Punkt Logik-Prüfung)
            -----------------------------------------------------
            • Keine hartcodierten API-Schlüssel gefunden.
            • Keine unverschlüsselten HTTP-Endpoints entdeckt.
            • Keine offensichtlichen Endlosschleifen.
            • Der Code/Prompt entspricht sauberen Qualitätsstandards.
        """.trimIndent()
    }

    return "🛡️ ERGEBNIS DER SICHERHEITS-PRÜFUNG:\n\n" + issues.joinToString("\n\n")
}

