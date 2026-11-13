package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GunaKootaScore
import com.example.data.model.MilanReport
import com.example.domain.CalculatedMilanResult
import com.example.ui.components.BirthPlaceMapField
import com.example.ui.components.BirthTimeAmPmField
import com.example.ui.viewmodel.AiGenerationState
import com.example.ui.viewmodel.DataListState

@Composable
fun KundliMilanScreen(
    milanCalculation: CalculatedMilanResult?,
    milanAiState: AiGenerationState,
    savedReportsState: DataListState<MilanReport>,
    onPerformMilan: (String, String, String, String, String, String, String, String) -> Unit,
    onDeleteReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var boyName by rememberSaveable { mutableStateOf("Rohan Verma") }
    var boyDate by rememberSaveable { mutableStateOf("1996-04-18") }
    var boyTime by rememberSaveable { mutableStateOf("09:15 AM") }
    var boyPlace by rememberSaveable { mutableStateOf("Jaipur, RJ (26.91°N, 75.79°E)") }

    var girlName by rememberSaveable { mutableStateOf("Priya Nair") }
    var girlDate by rememberSaveable { mutableStateOf("1998-09-24") }
    var girlTime by rememberSaveable { mutableStateOf("04:40 PM") }
    var girlPlace by rememberSaveable { mutableStateOf("Pune, MH (18.52°N, 73.86°E)") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("kundli_milan_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ashta-Koota 36-Guna Kundli Milan",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter birth details of Var (Partner 1) and Kanya (Partner 2) for 36-Guna matching, Mangal Dosha check, and AI marriage insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Partner 1 (Var)
                    Text(
                        text = "Partner 1 (Var / Groom)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = boyName,
                            onValueChange = { boyName = it },
                            label = { Text("Partner 1 Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("milan_boy_name_input")
                        )
                        OutlinedTextField(
                            value = boyDate,
                            onValueChange = { boyDate = it },
                            label = { Text("DOB (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("milan_boy_date_input")
                        )
                    }
                    BirthTimeAmPmField(
                        timeValue = boyTime,
                        onTimeChange = { boyTime = it },
                        label = "Partner 1 Time (HH:MM)",
                        inputTestTag = "milan_boy_time_input"
                    )
                    BirthPlaceMapField(
                        placeValue = boyPlace,
                        onPlaceChange = { boyPlace = it },
                        label = "Partner 1 Birth City & Map Pin",
                        inputTestTag = "milan_boy_place_input"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Partner 2 (Kanya)
                    Text(
                        text = "Partner 2 (Kanya / Bride)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = girlName,
                            onValueChange = { girlName = it },
                            label = { Text("Partner 2 Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("milan_girl_name_input")
                        )
                        OutlinedTextField(
                            value = girlDate,
                            onValueChange = { girlDate = it },
                            label = { Text("DOB (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("milan_girl_date_input")
                        )
                    }
                    BirthTimeAmPmField(
                        timeValue = girlTime,
                        onTimeChange = { girlTime = it },
                        label = "Partner 2 Time (HH:MM)",
                        inputTestTag = "milan_girl_time_input"
                    )
                    BirthPlaceMapField(
                        placeValue = girlPlace,
                        onPlaceChange = { girlPlace = it },
                        label = "Partner 2 Birth City & Map Pin",
                        inputTestTag = "milan_girl_place_input"
                    )

                    Button(
                        onClick = {
                            onPerformMilan(
                                boyName, boyDate, boyTime, boyPlace,
                                girlName, girlDate, girlTime, girlPlace
                            )
                        },
                        enabled = milanAiState !is AiGenerationState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_kundli_button")
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate 36-Guna Match & AI Compatibility")
                    }
                }
            }
        }

        // Results Section
        if (milanCalculation != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${milanCalculation.totalGuna} / 36 Gunas",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = milanCalculation.verdict,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${milanCalculation.boyRashi} (${milanCalculation.boyNakshatra})  ×  ${milanCalculation.girlRashi} (${milanCalculation.girlNakshatra})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "Mangal Dosha Assessment: ${milanCalculation.mangalCompatibility}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Text(
                            text = "Ashta-Koota (8-Fold) Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        milanCalculation.kootaScores.forEach { koota ->
                            KootaScoreBar(koota = koota)
                        }

                        when (milanAiState) {
                            is AiGenerationState.Idle -> Unit
                            is AiGenerationState.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("AI Marriage Astrologer is preparing detailed compatibility report...")
                                }
                            }
                            is AiGenerationState.Streaming -> {
                                AiResponseCard(title = "Live AI Compatibility Analysis", content = milanAiState.partialText)
                            }
                            is AiGenerationState.Success -> {
                                AiResponseCard(title = "AI Kundli Milan Verdict & Remedies", content = milanAiState.text)
                            }
                            is AiGenerationState.Error -> {
                                Text(
                                    text = milanAiState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Cloud Milan Reports
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Saved Reports",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved Kundli Milan History",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        when (savedReportsState) {
            is DataListState.Loading -> {
                item { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
            }
            is DataListState.Error -> {
                item {
                    Text(
                        text = savedReportsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is DataListState.Success -> {
                if (savedReportsState.items.isEmpty()) {
                    item {
                        Text(
                            text = "No Kundli Milan reports saved yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(savedReportsState.items, key = { it.id }) { report ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
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
                                            text = "${report.boyName} ❤️ ${report.girlName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = report.compatibilityVerdict,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteReport(report.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Report",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = report.aiAnalysis.take(220) + if (report.aiAnalysis.length > 220) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KootaScoreBar(koota: GunaKootaScore) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${koota.kootaName} • ${koota.domain}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${koota.obtainedPoints} / ${koota.maxPoints}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { koota.obtainedPoints.toFloat() / koota.maxPoints.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
