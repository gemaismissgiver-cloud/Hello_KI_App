package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PatternDatabase
import com.example.data.PatternRepository
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: PatternViewModel by viewModels {
    val db = PatternDatabase.getDatabase(applicationContext)
    val repo = PatternRepository(db.patternDao())
    PatternViewModelFactory(repo)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var currentTab by remember { mutableStateOf(StudioTab.CHAT) }
        val records by viewModel.records.collectAsStateWithLifecycle()
        val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
        val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
        val userMood by viewModel.userMood.collectAsStateWithLifecycle()

        Scaffold(
          bottomBar = {
              val navItems = listOf(
                Triple(StudioTab.CHAT, "💬 KI-Chat", Icons.Filled.ChatBubble),
                Triple(StudioTab.PROTOKOLLE, "📁 Akten", Icons.Filled.Folder),
                Triple(StudioTab.MUSTER_INSPEKTOR, "🧬 Muster & Logik", Icons.Filled.Analytics)
              )
              NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
              ) {
                navItems.forEach { (tab, label, icon) ->
                  NavigationBarItem(
                    selected = currentTab == tab,
                    onClick = { currentTab = tab },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                  )
                }
              }
          },
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          val modifier = Modifier.padding(innerPadding)
          when (currentTab) {
            StudioTab.CHAT -> ChatScreen(
              chatSessions = chatSessions,
              activeChatId = activeChatId,
              userMood = userMood,
              onMoodChanged = { viewModel.setUserMood(it) },
              onSelectChat = { viewModel.selectChat(it) },
              onCreateNewChat = { viewModel.createNewChat(it) },
              onRenameChat = { id, title -> viewModel.renameChat(id, title) },
              onDeleteChat = { viewModel.deleteChat(it) },
              onSendMessage = { viewModel.sendMessage(it) },
              onSendVoiceMessage = { duration, note -> viewModel.sendVoiceMessage(duration, note) },
              onSendAttachment = { type, fileName, infoText, preview -> viewModel.sendAttachment(type, fileName, infoText, preview) },
              modifier = modifier
            )
            StudioTab.PROTOKOLLE -> ProtokolleScreen(
              records = records,
              onDeleteRecord = { id -> viewModel.deleteRecord(id) },
              onClearAll = { viewModel.clearAllRecords() },
              modifier = modifier
            )
            StudioTab.MUSTER_INSPEKTOR -> MusterInspektorScreen(
              recordCount = records.size,
              userMood = userMood,
              modifier = modifier
            )
          }
        }
      }
    }
  }
}


