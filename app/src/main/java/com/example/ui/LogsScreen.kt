package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PatternRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    records: List<PatternRecord>,
    onAddRecord: (title: String, category: String, summary: String, rawMetrics: String, insights: String) -> Unit,
    onDeleteRecord: (id: Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Geschichte & Erfahrung") }
    var inputSummary by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Akte & Datenprotokolle", fontWeight = FontWeight.Bold) },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.testTag("clear_all_logs_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Alle Protokolle löschen")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_log")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Neuer Eintrag")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Noch keine Protokolle vorhanden.\nTippe auf '+', um frei und unverfälscht zu dokumentieren.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    LogItemCard(
                        record = record,
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Freien Eintrag erstellen") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Titel oder Thema") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_title_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputCategory,
                            onValueChange = { inputCategory = it },
                            label = { Text("Kategorie (z. B. Erfahrung, Erinnerung)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_category_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputSummary,
                            onValueChange = { inputSummary = it },
                            label = { Text("Gedanken, Geschichte oder Beobachtung") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_summary_input"),
                            minLines = 3,
                            maxLines = 6
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputTitle.isNotBlank()) {
                                onAddRecord(
                                    inputTitle.trim(),
                                    inputCategory.ifBlank { "Allgemein" },
                                    inputSummary.ifBlank { "Unverfälschter Eintrag." },
                                    "Manuell",
                                    "Dokumentiert ohne Bewertung."
                                )
                                inputTitle = ""
                                inputCategory = "Geschichte & Erfahrung"
                                inputSummary = ""
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.testTag("dialog_save_btn")
                    ) {
                        Text("Eintrag speichern")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}

@Composable
fun LogItemCard(
    record: PatternRecord,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(record.timestamp) { formatter.format(Date(record.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_log_${record.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eintrag löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = "Kategorie: ${record.category}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = record.summary,
                style = MaterialTheme.typography.bodyMedium
            )

            if (record.AIInsights.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "Vermerk: ${record.AIInsights}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(6.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
