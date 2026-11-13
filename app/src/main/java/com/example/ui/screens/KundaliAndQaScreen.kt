package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.data.model.KundaliProfile
import com.example.domain.CalculatedKundaliChart
import com.example.ui.components.BirthPlaceMapField
import com.example.ui.components.BirthTimeAmPmField
import com.example.ui.components.NorthIndianKundaliChart
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.viewmodel.AiGenerationState
import com.example.ui.viewmodel.DataListState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KundaliAndQaScreen(
    activeChart: CalculatedKundaliChart,
    activeMeta: KundaliProfile,
    kundaliAiState: AiGenerationState,
    savedKundalisState: DataListState<KundaliProfile>,
    qaMessages: List<ChatMessage>,
    qaStreamingState: AiGenerationState,
    onGenerateKundali: (String, String, String, String) -> Unit,
    onLoadSavedKundali: (KundaliProfile) -> Unit,
    onDeleteSavedKundali: (String) -> Unit,
    onAskQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var nameInput by rememberSaveable { mutableStateOf(activeMeta.name) }
    var dateInput by rememberSaveable { mutableStateOf(activeMeta.birthDate) }
    var timeInput by rememberSaveable { mutableStateOf(activeMeta.birthTime) }
    var placeInput by rememberSaveable { mutableStateOf(activeMeta.birthPlace) }
    var questionInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("kundali_and_qa_screen")
    ) {
        PrimaryTabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Janam Kundali", maxLines = 1) },
                modifier = Modifier.testTag("subtab_janam_kundali")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Free AI Jyotish Q&A", maxLines = 1) },
                modifier = Modifier.testTag("subtab_kundali_qa")
            )
        }

        if (selectedSubTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Birth Details Form
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Create Sidereal Vedic Janam Kundali",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kundali_name_input")
                            )
                            OutlinedTextField(
                                value = dateInput,
                                onValueChange = { dateInput = it },
                                label = { Text("Birth Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kundali_date_input")
                            )
                            BirthTimeAmPmField(
                                timeValue = timeInput,
                                onTimeChange = { timeInput = it },
                                label = "Birth Time (HH:MM)",
                                inputTestTag = "kundali_time_input"
                            )
                            BirthPlaceMapField(
                                placeValue = placeInput,
                                onPlaceChange = { placeInput = it },
                                label = "Place of Birth (City & Map Pin)",
                                inputTestTag = "kundali_place_input"
                            )
                            Button(
                                onClick = {
                                    onGenerateKundali(nameInput, dateInput, timeInput, placeInput)
                                },
                                enabled = kundaliAiState !is AiGenerationState.Loading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_kundali_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Chart & Free AI Reading")
                            }
                        }
                    }
                }

                // North-Indian Diamond Chart + Planetary Summary
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Lagna Kundali • ${activeMeta.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${activeChart.lagna} Lagna • ${activeChart.rashi} Rashi • ${activeChart.nakshatra}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Birth: ${activeChart.formattedBirthTime} • Geo: ${activeChart.coordinatesLabel} (${activeChart.lmtOffsetLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            NorthIndianKundaliChart(
                                chart = activeChart,
                                modifier = Modifier.fillMaxWidth(0.92f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Active Dasha: ${activeChart.vimshottariDasha}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeChart.mangalDosha,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (activeChart.planetaryDegreesSummary.isNotBlank()) {
                                        Text(
                                            text = "Sidereal Graha Positions (Lahiri): ${activeChart.planetaryDegreesSummary}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            when (kundaliAiState) {
                                is AiGenerationState.Idle -> Unit
                                is AiGenerationState.Loading -> {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("AI Jyotish Acharya is analyzing planetary Yogas...")
                                    }
                                }
                                is AiGenerationState.Streaming -> {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    AiResponseCard(title = "Live AI Kundali Reading", content = kundaliAiState.partialText)
                                }
                                is AiGenerationState.Success -> {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    AiResponseCard(title = "Complete AI Kundali Analysis", content = kundaliAiState.text)
                                }
                                is AiGenerationState.Error -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = kundaliAiState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Saved Cloud Kundalis
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Saved Kundalis",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved Cloud Kundalis",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                when (savedKundalisState) {
                    is DataListState.Loading -> {
                        item {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                    is DataListState.Error -> {
                        item {
                            Text(
                                text = savedKundalisState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is DataListState.Success -> {
                        if (savedKundalisState.items.isEmpty()) {
                            item {
                                Text(
                                    text = "No saved Kundalis yet. Generate a Kundali above to save it automatically to your cloud profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(savedKundalisState.items, key = { it.id }) { profile ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            nameInput = profile.name
                                            dateInput = profile.birthDate
                                            timeInput = profile.birthTime
                                            placeInput = profile.birthPlace
                                            onLoadSavedKundali(profile)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${profile.birthDate} (${profile.birthTime}) • ${profile.birthPlace}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${profile.rashi} • ${profile.nakshatra}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onDeleteSavedKundali(profile.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Kundali",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sub-tab 1: Free AI Kundali Q&A Chat
            val quickQuestions = listOf(
                "When will my career & financial breakthrough happen?",
                "What does my 7th house say about marriage & life partner?",
                "Which gemstone and Vedic mantra suit my Lagna?",
                "Are there Raj Yogas or Dhan Yogas in my Kundali?"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Active Kundali: ${activeMeta.name} (${activeChart.lagna} Lagna • ${activeChart.rashi})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ask unlimited free questions about your planets, dasha, marriage, or career.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickQuestions.forEachIndexed { idx, q ->
                        AssistChip(
                            onClick = { onAskQuestion(q) },
                            label = {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.testTag("quick_question_chip_$idx")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(qaMessages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }

                    when (qaStreamingState) {
                        is AiGenerationState.Loading -> {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI Jyotish Acharya is reading your Bhava chart...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        is AiGenerationState.Streaming -> {
                            item {
                                ChatBubble(
                                    message = ChatMessage(
                                        id = "streaming",
                                        isUser = false,
                                        text = qaStreamingState.partialText,
                                        timestampLabel = "Typing..."
                                    )
                                )
                            }
                        }
                        is AiGenerationState.Error -> {
                            item {
                                Text(
                                    text = qaStreamingState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        else -> Unit
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text("Ask any question about your Kundali...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kundali_qa_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (questionInput.isNotBlank()) {
                                onAskQuestion(questionInput)
                                questionInput = ""
                            }
                        },
                        enabled = questionInput.isNotBlank() && qaStreamingState !is AiGenerationState.Loading,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("kundali_qa_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Question",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = bgColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (message.isUser) "You • ${message.timestampLabel}" else "✨ AI Jyotish Acharya • ${message.timestampLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
