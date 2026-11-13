package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ZodiacSignInfo
import com.example.domain.VedicAstrologyEngine
import com.example.ui.viewmodel.AiGenerationState

@Composable
fun HomeRashifalScreen(
    selectedRashiId: String,
    dailyAiState: AiGenerationState,
    onSelectRashi: (String) -> Unit,
    onGenerateAiRashifal: (String) -> Unit,
    onNavigateToKundali: () -> Unit,
    onNavigateToMilan: () -> Unit,
    onNavigateToPalm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSign = remember(selectedRashiId) { VedicAstrologyEngine.getSignById(selectedRashiId) }
    val panchang = remember { VedicAstrologyEngine.getTodayPanchang() }
    val instantPreview = remember(selectedSign) { VedicAstrologyEngine.getInstantDailySummary(selectedSign) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_rashifal_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Vedic Panchang Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cosmic_banner_1794486167917),
                        contentDescription = "Cosmic Vedic Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Panchang",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Vedic Panchang • ${panchang.dateFormatted}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${panchang.tithi} • ${panchang.nakshatra} Nakshatra",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PanchangBadge(label = "Yoga", value = panchang.yoga, modifier = Modifier.weight(1f))
                            PanchangBadge(label = "Abhijit Muhurat", value = panchang.abhijitMuhurat, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2. Quick Feature Shortcuts (Kundali Q&A, Kundli Milan, Hand Analysis)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickFeatureCard(
                    title = "Free Kundali & AI Q&A",
                    subtitle = "Birth Chart & Chat",
                    icon = Icons.Default.QuestionAnswer,
                    onClick = onNavigateToKundali,
                    testTag = "shortcut_kundali_qa",
                    modifier = Modifier.weight(1f)
                )
                QuickFeatureCard(
                    title = "Kundli Milan",
                    subtitle = "36 Guna Match",
                    icon = Icons.Default.Favorite,
                    onClick = onNavigateToMilan,
                    testTag = "shortcut_kundli_milan",
                    modifier = Modifier.weight(1f)
                )
                QuickFeatureCard(
                    title = "Hand Analysis",
                    subtitle = "AI Hastrekha",
                    icon = Icons.Default.BackHand,
                    onClick = onNavigateToPalm,
                    testTag = "shortcut_palm_reading",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. 12 Rashi Selector
        item {
            Column {
                Text(
                    text = "Select Your Janam Rashi (Zodiac Sign)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(VedicAstrologyEngine.zodiacSigns, key = { it.id }) { sign ->
                        val isSelected = sign.id == selectedSign.id
                        RashiChipCard(
                            sign = sign,
                            isSelected = isSelected,
                            onClick = { onSelectRashi(sign.id) }
                        )
                    }
                }
            }
        }

        // 4. Selected Rashi Daily Horoscope Card + AI Deep Prediction
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedSign.symbolGlyph,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${selectedSign.sanskritName} (${selectedSign.englishName})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${selectedSign.hindiTitle} • Lord: ${selectedSign.rulingPlanet}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PanchangBadge(
                            label = "Element (Tattva)",
                            value = selectedSign.element,
                            modifier = Modifier.weight(1f)
                        )
                        PanchangBadge(
                            label = "Lucky Color",
                            value = selectedSign.luckyColor,
                            modifier = Modifier.weight(1f)
                        )
                        PanchangBadge(
                            label = "Lucky Number",
                            value = selectedSign.luckyNumber.toString(),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Today's Transit Overview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = instantPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onGenerateAiRashifal(selectedSign.id) },
                        enabled = dailyAiState !is AiGenerationState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_ai_horoscope_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Rashifal",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Detailed AI Personalized Prediction & Upay")
                    }

                    when (dailyAiState) {
                        is AiGenerationState.Idle -> Unit
                        is AiGenerationState.Loading -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Consulting AI Jyotish Acharya for ${selectedSign.sanskritName}...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is AiGenerationState.Streaming -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            AiResponseCard(title = "Live AI Rashifal", content = dailyAiState.partialText)
                        }
                        is AiGenerationState.Success -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            AiResponseCard(title = "Complete AI Rashifal & Vedic Upay", content = dailyAiState.text)
                        }
                        is AiGenerationState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = dailyAiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanchangBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RashiChipCard(
    sign: ZodiacSignInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, borderCol, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("rashi_chip_${sign.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = sign.symbolGlyph,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sign.sanskritName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sign.englishName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AiResponseCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
