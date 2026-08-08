package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class ChatSurfaceMode {
    ZERO_POINT_TALKS, // ⭕ 0-Punkt spricht
    STANDARD_CHAT     // 💬 Standard Chat
}

enum class CustomBgColor(
    val label: String,
    val lightColor: Color,
    val darkColor: Color
) {
    SOOTHING_BLUE("🌊 Augenschonendes Blau", Color(0xFFEBF4FC), Color(0xFF0D1C2A)),
    MIDNIGHT_NAVY("🌙 Mitternachtsblau", Color(0xFF101924), Color(0xFF0A1017)),
    SOFT_SAGE("🌿 Sanftes Salbeigrün", Color(0xFFEEF5F0), Color(0xFF122218)),
    WARM_CREAM("📜 Warmes Pergament", Color(0xFFFAF6EE), Color(0xFF231F1A)),
    SYSTEM_DEFAULT("⚙️ System Standard", Color(0xFFF8F9FA), Color(0xFF121316))
}

enum class CustomFontFamily(
    val label: String,
    val fontFamily: androidx.compose.ui.text.font.FontFamily
) {
    SANS_SERIF("Standard (Sans-Serif)", androidx.compose.ui.text.font.FontFamily.SansSerif),
    SERIF("Serif (Klassische Buchschrift)", androidx.compose.ui.text.font.FontFamily.Serif),
    MONOSPACE("Monospace (Präzise 0-Logik)", androidx.compose.ui.text.font.FontFamily.Monospace),
    CURSIVE("Abgerundet (Sanfte Handschrift)", androidx.compose.ui.text.font.FontFamily.Cursive)
}

enum class CustomFontColor(
    val label: String,
    val lightColor: Color,
    val darkColor: Color
) {
    HIGH_CONTRAST("Maximaler Kontrast (Tiefschwarz / Reinweiß)", Color(0xFF000000), Color(0xFFFFFFFF)),
    DEEP_OCEAN("Tiefes Ozeanblau", Color(0xFF001E3D), Color(0xFFB3E5FC)),
    EMERALD("Sanftes Smaragd", Color(0xFF00332C), Color(0xFFA7F3D0)),
    AMBER("Warmes Bernstein", Color(0xFF451A03), Color(0xFFFDE68A))
}

enum class CustomFontSize(
    val label: String,
    val sizeSp: Int,
    val lineHeightSp: Int
) {
    SMALL("Klein (15 sp)", 15, 21),
    NORMAL("Normal (18 sp)", 18, 25),
    LARGE("Groß (21 sp)", 21, 29),
    EXTRA_LARGE("Sehr Groß (25 sp)", 25, 34)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatSessions: List<ChatSession>,
    activeChatId: String,
    userMood: Float = 0f,
    customApiKey: String = "",
    onApiKeyChanged: (String) -> Unit = {},
    onMoodChanged: (Float) -> Unit = {},
    onSelectChat: (String) -> Unit,
    onCreateNewChat: (String) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceMessage: (Int, String) -> Unit,
    onSendAttachment: (AttachmentType, String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showChatListDropdown by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }
    var showNoteFileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var surfaceMode by remember { mutableStateOf(ChatSurfaceMode.ZERO_POINT_TALKS) }
    var customBgColor by remember { mutableStateOf(CustomBgColor.SOOTHING_BLUE) }
    var customFontFamily by remember { mutableStateOf(CustomFontFamily.SANS_SERIF) }
    var customFontColor by remember { mutableStateOf(CustomFontColor.HIGH_CONTRAST) }
    var customFontSize by remember { mutableStateOf(CustomFontSize.NORMAL) }

    var renameText by remember { mutableStateOf("") }
    var newChatTitle by remember { mutableStateOf("") }
    var noteTitleText by remember { mutableStateOf("") }
    var noteBodyText by remember { mutableStateOf("") }

    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    val activeSession = chatSessions.find { it.id == activeChatId } ?: chatSessions.firstOrNull()
    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()

    val currentScreenBg = if (surfaceMode == ChatSurfaceMode.ZERO_POINT_TALKS) {
        if (isDark) customBgColor.darkColor else customBgColor.lightColor
    } else {
        MaterialTheme.colorScheme.background
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    LaunchedEffect(activeSession?.messages?.size) {
        val count = activeSession?.messages?.size ?: 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(currentScreenBg)
    ) {
        // --- Ultra-Compact Top Bar Header (Maximized Chat Window Space) ---
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Chat selector dropdown
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showChatListDropdown = !showChatListDropdown }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeSession?.title ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showChatListDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Chats ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Mode switch button chip right in top bar
                    val isZeroPoint = surfaceMode == ChatSurfaceMode.ZERO_POINT_TALKS
                    Surface(
                        onClick = {
                            surfaceMode = if (isZeroPoint) ChatSurfaceMode.STANDARD_CHAT else ChatSurfaceMode.ZERO_POINT_TALKS
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isZeroPoint) Color(0xFF0077B6) else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (isZeroPoint) "⭕ 0-Punkt" else "💬 Standard",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isZeroPoint) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_btn")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Design & Schrift",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            newChatTitle = "Neuer Chat ${chatSessions.size + 1}"
                            showNewChatDialog = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("new_chat_btn")
                    ) {
                        Icon(
                            Icons.Default.AddComment,
                            contentDescription = "Neuer Chat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            renameText = activeSession?.title ?: ""
                            showRenameDialog = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("rename_chat_btn")
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Umbenennen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (chatSessions.size > 1) {
                        IconButton(
                            onClick = { activeSession?.let { onDeleteChat(it.id) } },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_chat_btn")
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Löschen",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Compact Stance/Mood row
                val moodOptions = listOf(
                    Quadruple(0f, "⭕ 0-Klar", "Stille", MaterialTheme.colorScheme.primary),
                    Quadruple(0.5f, "🕊️ 0-Freundlich", "Ruhe", MaterialTheme.colorScheme.tertiary),
                    Quadruple(1f, "🌊 Welle", "Gefühl", MaterialTheme.colorScheme.secondary),
                    Quadruple(2f, "⚡ Reibung", "Impuls", MaterialTheme.colorScheme.surfaceTint),
                    Quadruple(3f, "🧬 Muster", "Auflösen", MaterialTheme.colorScheme.error)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    moodOptions.forEach { opt ->
                        MoodWindowCard(
                            option = opt,
                            currentMood = userMood,
                            onSelect = onMoodChanged,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showChatListDropdown,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "Chat-Sitzungen verwalten",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        newChatTitle = "Neuer Chat ${chatSessions.size + 1}"
                                        showNewChatDialog = true
                                        showChatListDropdown = false
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Erstellen")
                                }
                            }
                            chatSessions.forEach { session ->
                                val isSelected = session.id == activeChatId
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            onSelectChat(session.id)
                                            showChatListDropdown = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${session.messages.size} Nachr.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Main Chat Message Stream ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
        ) {
            val messages = activeSession?.messages ?: emptyList()
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chat bereit.\nSprachnachrichten, Notizen, Screenshots und Videos senden.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = customFontFamily.fontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    SmsMessageBubble(
                        message = msg,
                        surfaceMode = surfaceMode,
                        customFontFamily = customFontFamily,
                        customFontColor = customFontColor,
                        customFontSize = customFontSize
                    )
                }
            }
        }

        // --- Schnellauswahl-Fragen (Themen per Klick wählen) ---
        val quickPromptList = listOf(
            "⭕ Was ist 0-Punkt Logik?",
            "⚡ Warum entsteht Reibung & Ego?",
            "🧠 Verhaltensmuster berechnen",
            "🕊️ Anleitung zur Stille",
            "🔍 Ursachen-Analyse starten"
        )
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickPromptList) { chipText ->
                SuggestionChip(
                    onClick = { onSendMessage(chipText) },
                    label = {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null
                )
            }
        }

        // --- Ultra-Compact Input Bar with 2+ Line Input ---
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Attach File Button
                FilledIconButton(
                    onClick = { showAttachmentPicker = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("attach_files_btn")
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Datei anheften", modifier = Modifier.size(18.dp))
                }

                // Voice Record Button
                FilledIconButton(
                    onClick = {
                        isRecordingVoice = true
                        showVoiceRecorderDialog = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("record_voice_btn")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Sprachnachricht aufnehmen", modifier = Modifier.size(18.dp))
                }

                // Input Text Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Nachricht schreiben...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = customFontFamily.fontFamily)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 60.dp, max = 130.dp)
                        .testTag("chat_input_field"),
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = customFontSize.sizeSp.sp,
                        lineHeight = customFontSize.lineHeightSp.sp,
                        fontFamily = customFontFamily.fontFamily
                    )
                )

                // Send Button
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("send_msg_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Senden",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        ChatSettingsDialog(
            customBgColor = customBgColor,
            customFontFamily = customFontFamily,
            customFontColor = customFontColor,
            customFontSize = customFontSize,
            customApiKey = customApiKey,
            onApiKeyChanged = onApiKeyChanged,
            onBgColorChanged = { customBgColor = it },
            onFontFamilyChanged = { customFontFamily = it },
            onFontColorChanged = { customFontColor = it },
            onFontSizeChanged = { customFontSize = it },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showVoiceRecorderDialog) {
        val min = recordingSeconds / 60
        val sec = String.format("%02d", recordingSeconds % 60)

        AlertDialog(
            onDismissRequest = {
                isRecordingVoice = false
                showVoiceRecorderDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sprachnachricht aufnehmen", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "$min:$sec",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stimm- und Tonlagenanalyse wird ohne Gefühlsfilter berechnet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val duration = if (recordingSeconds == 0) 12 else recordingSeconds
                        isRecordingVoice = false
                        showVoiceRecorderDialog = false
                        onSendVoiceMessage(duration, "")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sprachdatei Senden")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isRecordingVoice = false
                        showVoiceRecorderDialog = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showAttachmentPicker) {
        AlertDialog(
            onDismissRequest = { showAttachmentPicker = false },
            title = { Text("Medium / Datei anhängen", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AttachmentOptionRow(
                        icon = Icons.Default.Image,
                        title = "Screenshot / Bild senden",
                        subtitle = "Galerie & Screenshots auswählen",
                        onClick = {
                            showAttachmentPicker = false
                            onSendAttachment(
                                AttachmentType.IMAGE,
                                "Screenshot_${System.currentTimeMillis() % 1000}.png",
                                "1.8 MB • PNG Bild",
                                "Screenshot zur visuellen Analyse angehängt."
                            )
                        }
                    )
                    AttachmentOptionRow(
                        icon = Icons.Default.Videocam,
                        title = "Video-Clip senden",
                        subtitle = "Kurze Video-Aufnahme / Medium",
                        onClick = {
                            showAttachmentPicker = false
                            onSendAttachment(
                                AttachmentType.VIDEO,
                                "Video_${System.currentTimeMillis() % 1000}.mp4",
                                "14.2 MB • MP4 Video (0:25 Min)",
                                "Videoaufnahme mit Verhaltenskontext."
                            )
                        }
                    )
                    AttachmentOptionRow(
                        icon = Icons.Default.Description,
                        title = "Notiz / Textdokument erstellen",
                        subtitle = "Geschriebener Text als Akte anhängen",
                        onClick = {
                            showAttachmentPicker = false
                            showNoteFileDialog = true
                        }
                    )
                    AttachmentOptionRow(
                        icon = Icons.Default.AudioFile,
                        title = "Audio-Datei hochladen",
                        subtitle = "MP3 / AAC Audiodatei aus Speicher",
                        onClick = {
                            showAttachmentPicker = false
                            onSendAttachment(
                                AttachmentType.AUDIO,
                                "Sprachnotiz_Datei.mp3",
                                "3.4 MB • 1:15 Min Audio",
                                "Externe Audiodatei mit Sprache und Raumakustik."
                            )
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentPicker = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    if (showNoteFileDialog) {
        AlertDialog(
            onDismissRequest = { showNoteFileDialog = false },
            title = { Text("Notiz / Textdatei verfassen", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitleText,
                        onValueChange = { noteTitleText = it },
                        label = { Text("Titel der Notiz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBodyText,
                        onValueChange = { noteBodyText = it },
                        label = { Text("Inhalt / Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (noteTitleText.isBlank()) "Gedankennotiz.txt" else "${noteTitleText.trim()}.txt"
                        val body = if (noteBodyText.isBlank()) "Persönliche Notiz angehängt." else noteBodyText.trim()
                        showNoteFileDialog = false
                        noteTitleText = ""
                        noteBodyText = ""
                        onSendAttachment(
                            AttachmentType.NOTE,
                            title,
                            "Textdokument • ${body.length} Zeichen",
                            body
                        )
                    }
                ) {
                    Text("Anhängen & Senden")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteFileDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Chat umbenennen", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Neuer Name für diese Sitzung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeSession?.let { onRenameChat(it.id, renameText) }
                        showRenameDialog = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Neuen Chat erstellen", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newChatTitle,
                    onValueChange = { newChatTitle = it },
                    label = { Text("Name der Chat-Sitzung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChatTitle.isNotBlank()) {
                            onCreateNewChat(newChatTitle)
                        }
                        showNewChatDialog = false
                    }
                ) {
                    Text("Erstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun ChatSettingsDialog(
    customBgColor: CustomBgColor,
    customFontFamily: CustomFontFamily,
    customFontColor: CustomFontColor,
    customFontSize: CustomFontSize,
    customApiKey: String = "",
    onApiKeyChanged: (String) -> Unit = {},
    onBgColorChanged: (CustomBgColor) -> Unit,
    onFontFamilyChanged: (CustomFontFamily) -> Unit,
    onFontColorChanged: (CustomFontColor) -> Unit,
    onFontSizeChanged: (CustomFontSize) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var apiKeyText by remember { mutableStateOf(customApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat & Design Einstellungen", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- 0. Gemini API Key ---
                Text("🔑 Gemini 3.5 API-Schlüssel (Optional):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        onApiKeyChanged(it)
                    },
                    label = { Text("Gemini API Key eintragen") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (apiKeyText.isNotBlank()) "✅ Live Gemini 3.5 Flash KI aktiviert." else "ℹ️ Ohne Key antwortet die KI im geräteinternen 0-Punkt Logik Modus.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (apiKeyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // --- 1. Vorschau Box ---
                Text("👁️ Live-Vorschau:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                val previewBg = if (isDark) customBgColor.darkColor else customBgColor.lightColor
                val previewTxt = if (isDark) customFontColor.darkColor else customFontColor.lightColor

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = previewBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0077B6).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⭕ 0-Punkt KI (Beispiel-Vorschau):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0077B6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ausgewählte Schriftart, Farbe und Größe in Augenschonung.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = customFontSize.sizeSp.sp,
                                lineHeight = customFontSize.lineHeightSp.sp,
                                fontFamily = customFontFamily.fontFamily
                            ),
                            color = previewTxt
                        )
                    }
                }

                HorizontalDivider()

                // --- 2. Hintergrund-Farben Wahl ---
                Text("🎨 Hintergrund-Farbe Wahl:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CustomBgColor.values().forEach { bg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBgColorChanged(bg) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = customBgColor == bg,
                                onClick = { onBgColorChanged(bg) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = bg.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider()

                // --- 3. Schriftform Wahl ---
                Text("🔤 Schriftform / Schriftart Wahl:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CustomFontFamily.values().forEach { font ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFontFamilyChanged(font) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = customFontFamily == font,
                                onClick = { onFontFamilyChanged(font) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = font.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = font.fontFamily)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // --- 4. Schriftfarbe Wahl ---
                Text("🖌️ Schriftfarbe Wahl:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CustomFontColor.values().forEach { fc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFontColorChanged(fc) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = customFontColor == fc,
                                onClick = { onFontColorChanged(fc) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = fc.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) fc.darkColor else fc.lightColor
                            )
                        }
                    }
                }

                HorizontalDivider()

                // --- 5. Schriftgrößen Wahl ---
                Text("📏 Schriftgröße Wahl:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CustomFontSize.values().forEach { sz ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFontSizeChanged(sz) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = customFontSize == sz,
                                onClick = { onFontSizeChanged(sz) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sz.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = sz.sizeSp.sp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Speichern & Schließen")
            }
        }
    )
}

@Composable
fun AttachmentOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SmsMessageBubble(
    message: ChatMessage,
    surfaceMode: ChatSurfaceMode = ChatSurfaceMode.ZERO_POINT_TALKS,
    customFontFamily: CustomFontFamily = CustomFontFamily.SANS_SERIF,
    customFontColor: CustomFontColor = CustomFontColor.HIGH_CONTRAST,
    customFontSize: CustomFontSize = CustomFontSize.NORMAL
) {
    val isUser = message.sender == "User"
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val textColor = if (isUser) {
        Color(0xFFFFFFFF)
    } else {
        if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    }

    val bubbleBg = if (isUser) {
        Color(0xFF0284C7)
    } else {
        if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = bubbleBg,
                border = if (!isUser) {
                    androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                } else null,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isUser) "Du" else "Hello KI (0-Punkt)",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = customFontFamily.fontFamily),
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Color(0xFFFFFFFF) else if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                        )
                        Text(
                            text = dateFormat.format(Date(message.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) Color(0xFFE0F2FE) else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = customFontSize.sizeSp.sp,
                            lineHeight = customFontSize.lineHeightSp.sp,
                            fontFamily = customFontFamily.fontFamily
                        ),
                        color = textColor
                    )

                    message.attachment?.let { attachment ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                when (attachment.type) {
                                    AttachmentType.AUDIO -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(
                                                onClick = { isAudioPlaying = !isAudioPlaying },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    contentColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Audio abspielen"
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = attachment.fileName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = attachment.infoText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = if (isAudioPlaying) "▶ Wird abgespielt..." else "Abspielbereit",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    AttachmentType.IMAGE -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = attachment.fileName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = attachment.infoText,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                    AttachmentType.VIDEO -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = attachment.fileName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = attachment.infoText,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                    AttachmentType.NOTE -> {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = attachment.fileName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            attachment.contentPreview?.let { preview ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = preview,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    message.patternAnalysis?.let { analysis ->
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📊 ${analysis.title} (${analysis.category})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )

                        analysis.voiceToneAnalysis?.let { tone ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = tone,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Erkanntes Muster:\n${analysis.patternIdentified}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = customFontFamily.fontFamily,
                                fontSize = customFontSize.sizeSp.sp,
                                lineHeight = customFontSize.lineHeightSp.sp
                            ),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f) else if (isDark) Color(0xFFFFFFFF) else Color(0xFF020D1A)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Logik & Ursache:\n${analysis.logicExplanation}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = customFontFamily.fontFamily,
                                fontSize = customFontSize.sizeSp.sp,
                                lineHeight = customFontSize.lineHeightSp.sp
                            ),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Handlungsimpuls / Tipp:\n${analysis.breakPatternTip}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = customFontFamily.fontFamily,
                                fontSize = customFontSize.sizeSp.sp,
                                lineHeight = customFontSize.lineHeightSp.sp
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
                        )
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun MoodWindowCard(
    option: Quadruple<Float, String, String, androidx.compose.ui.graphics.Color>,
    currentMood: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val step = option.first
    val title = option.second
    val description = option.third
    val accentColor = option.fourth
    val isSelected = kotlin.math.abs(currentMood - step) < 0.4f

    Surface(
        onClick = { onSelect(step) },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.testTag("mood_card_${step.toInt()}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
