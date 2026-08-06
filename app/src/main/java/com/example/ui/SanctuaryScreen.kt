package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SanctuaryCompanion(
    val name: String,
    val description: String,
    val eyeColor: Color,
    val hairStyle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuaryScreen(
    onSaveLog: (title: String, category: String, summary: String, rawMetrics: String, insights: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val companions = remember {
        listOf(
            SanctuaryCompanion("Schneeflocke", "Weißes Fell mit Scheitel & rote Augen", Color(0xFFD32F2F), "Scheitel"),
            SanctuaryCompanion("Frosty", "Reines weißes Fell & feine rote Augen", Color(0xFFC62828), "Glatt")
        )
    }

    var selectedTreat by remember { mutableStateOf("Karotte") }
    var hayStackCount by remember { mutableIntStateOf(5) }
    var petActionReaction by remember { mutableStateOf("Ruhig und entspannt im Gehege.") }
    var happinessScore by remember { mutableIntStateOf(95) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruhebereich & Gehege", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            hayStackCount = 5
                            happinessScore = 95
                            petActionReaction = "Gehege gesäubert und Heu aufgefüllt."
                        },
                        modifier = Modifier.testTag("reset_sanctuary_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ruhebereich zurücksetzen")
                    }
                }
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
                            Text("Umgebung im Gehege", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$happinessScore% Wohlbefinden", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .clickable {
                                    happinessScore = (happinessScore + 2).coerceAtMost(100)
                                    petActionReaction = "Sanftes Tippen – Gefährten blicken aufmerksam!"
                                }
                                .testTag("enclosure_canvas"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRoundRect(
                                    color = Color(0xFFA5D6A7),
                                    topLeft = Offset(10f, 10f),
                                    size = Size(size.width - 20f, size.height - 20f),
                                    cornerRadius = CornerRadius(16f, 16f)
                                )

                                for (i in 0 until hayStackCount) {
                                    val x = size.width * 0.15f + (i * 18f)
                                    val y = size.height * 0.65f
                                    drawOval(
                                        color = Color(0xFFFBC02D),
                                        topLeft = Offset(x, y),
                                        size = Size(45f, 25f)
                                    )
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = 28f,
                                    center = Offset(size.width * 0.4f, size.height * 0.45f + pulseOffset)
                                )
                                drawCircle(
                                    color = Color(0xFFD32F2F),
                                    radius = 5f,
                                    center = Offset(size.width * 0.42f, size.height * 0.42f + pulseOffset)
                                )

                                drawCircle(
                                    color = Color.White,
                                    radius = 28f,
                                    center = Offset(size.width * 0.65f, size.height * 0.5f - pulseOffset)
                                )
                                drawCircle(
                                    color = Color(0xFFC62828),
                                    radius = 5f,
                                    center = Offset(size.width * 0.67f, size.height * 0.47f - pulseOffset)
                                )
                            }

                            Text(
                                text = "Heuballen: $hayStackCount",
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = petActionReaction,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                        Text("Futter anbieten", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))

                        val treats = listOf("Karotte", "Trockenfutter", "Melone", "Tomate", "Frisches Grün")

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(treats) { treat ->
                                FilterChip(
                                    selected = selectedTreat == treat,
                                    onClick = { selectedTreat = treat },
                                    label = { Text(treat) },
                                    modifier = Modifier.testTag("treat_chip_$treat")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    happinessScore = (happinessScore + 5).coerceAtMost(100)
                                    petActionReaction = "$selectedTreat gefüttert! Alle knabbern zufrieden."
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("feed_treat_btn")
                            ) {
                                Text("$selectedTreat geben")
                            }

                            OutlinedButton(
                                onClick = {
                                    hayStackCount += 2
                                    petActionReaction = "Frisches Heu in das Gehege gelegt."
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_hay_btn")
                            ) {
                                Icon(Icons.Default.Grass, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Heu hinzufügen")
                            }
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
                        Text("Gefährten im Ruhebereich", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(8.dp))

                        companions.forEach { companion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        petActionReaction = "${companion.name} leise gestreichelt."
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(companion.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(companion.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onSaveLog(
                            "Status Ruhebereich",
                            "Ruhebereich",
                            "Wohlbefinden: $happinessScore% | Heuballen: $hayStackCount | Letztes Futter: $selectedTreat",
                            "Wohlbefinden=$happinessScore",
                            "Zustand im Ruhebereich dokumentiert."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_sanctuary_log_btn")
                ) {
                    Text("Status im Protokoll festhalten")
                }
            }
        }
    }
}
