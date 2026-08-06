package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaToolsScreen(
    onSaveLog: (title: String, category: String, summary: String, rawMetrics: String, insights: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var audioPitchHz by remember { mutableIntStateOf(220) }
    var audioAmplitude by remember { mutableFloatStateOf(0.4f) }
    var selectedFilter by remember { mutableStateOf("Standard") }
    var imageContrast by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(150)
                audioPitchHz = Random.nextInt(180, 441)
                audioAmplitude = Random.nextFloat() * 0.7f + 0.2f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medien & Signale", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Audio- & Tonfrequenz Signal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { isRecording = !isRecording },
                                modifier = Modifier.testTag("record_audio_btn")
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                                    contentDescription = "Aufnahme umschalten",
                                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.Black.copy(alpha = 0.8f))
                        ) {
                            val barWidth = size.width / 20
                            for (i in 0 until 20) {
                                val heightFactor = if (isRecording) {
                                    (0.1f + ((i % 5) * 0.18f * audioAmplitude)).coerceIn(0.1f, 0.95f)
                                } else 0.15f
                                val barHeight = size.height * heightFactor
                                drawRect(
                                    color = if (isRecording) Color(0xFF1B4D3E) else Color(0xFF757575),
                                    topLeft = Offset(i * barWidth + 2f, size.height - barHeight),
                                    size = androidx.compose.ui.geometry.Size(barWidth - 4f, barHeight)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Geschätzte Frequenz: ${if (isRecording) "$audioPitchHz Hz" else "Inaktiv"}", style = MaterialTheme.typography.bodySmall)
                            Text("Spitzenpegel: ${if (isRecording) "${(audioAmplitude * 100).toInt()}%" else "0%"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bildspektrum & Farbfilter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (selectedFilter) {
                                        "Hoher Kontrast" -> Color(0xFF212121)
                                        "Weiches Lila" -> Color(0xFF7E57C2)
                                        "Dunkelgrün" -> Color(0xFF1B4D3E)
                                        else -> Color(0xFF37474F)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Filter, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Vorschau Filter: $selectedFilter", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Standard", "Hoher Kontrast", "Weiches Lila", "Dunkelgrün").forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("filter_chip_$filter")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Kontrastwert: ${String.format("%.1f", imageContrast)}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = imageContrast,
                            onValueChange = { imageContrast = it },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.testTag("contrast_slider")
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onSaveLog(
                            "Mediensignal-Prüfung",
                            "Medien",
                            "Frequenz: ${audioPitchHz}Hz | Filter: $selectedFilter | Kontrast: ${String.format("%.1f", imageContrast)}",
                            "PitchHz=$audioPitchHz, Filter=$selectedFilter",
                            "Signaldaten ohne Bewertung festgehalten."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_media_log_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mediensignal protokollieren")
                }
            }
        }
    }
}
