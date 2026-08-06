package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onSaveAnalysis: (title: String, category: String, summary: String, metrics: String, insights: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sampleSize by remember { mutableFloatStateOf(50f) }
    var metricSensitivity by remember { mutableFloatStateOf(0.7f) }
    var calculatedMean by remember { mutableDoubleStateOf(42.8) }
    var calculatedDeviation by remember { mutableDoubleStateOf(3.14) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Muster-Analysen & Übersicht", fontWeight = FontWeight.Bold) }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Muster-Parameter anpassen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Stichprobengröße: ${sampleSize.toInt()} Einheiten", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = sampleSize,
                            onValueChange = { sampleSize = it },
                            valueRange = 10f..200f,
                            modifier = Modifier.testTag("sample_size_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Empfindlichkeit: ${(metricSensitivity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = metricSensitivity,
                            onValueChange = { metricSensitivity = it },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.testTag("sensitivity_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                calculatedMean = (30..60).random() + (0..99).random() / 100.0
                                calculatedDeviation = (1..5).random() + (0..99).random() / 100.0
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_calculation_btn")
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Muster neu berechnen")
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
                        Text("Verlaufsdarstellung der Daten", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            val w = size.width
                            val h = size.height

                            val path = Path().apply {
                                moveTo(0f, h * 0.5f)
                                val points = sampleSize.toInt().coerceAtMost(20)
                                for (i in 1..points) {
                                    val x = (w / points) * i
                                    val y = (h * 0.5f) + (Math.sin(i * metricSensitivity.toDouble() * 2.0) * h * 0.35f).toFloat()
                                    lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF1B4D3E),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Mittelwert", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%.2f", calculatedMean), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Abweichung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%.2f", calculatedDeviation), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Kohärenz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(100 - calculatedDeviation * 10).toInt().coerceIn(10, 99)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyse speichern", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Speichert das aktuelle Datenmuster in den Protokollen zur späteren Einsicht.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onSaveAnalysis(
                                    "Muster-Analyse #${(1000..9999).random()}",
                                    "Analyse",
                                    "Mittelwert: ${String.format("%.2f", calculatedMean)} | Abw: ${String.format("%.2f", calculatedDeviation)}",
                                    "Stichprobe=${sampleSize.toInt()}, Empfindlichkeit=$metricSensitivity",
                                    "Berechnete Stabilität über ${sampleSize.toInt()} Werte."
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_analysis_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("In Protokollen speichern")
                        }
                    }
                }
            }
        }
    }
}
