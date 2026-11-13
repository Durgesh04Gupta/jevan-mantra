package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PalmReading
import com.example.domain.VedicAstrologyEngine
import com.example.ui.viewmodel.DataListState
import com.example.ui.viewmodel.PalmAnalysisUiState

@Composable
fun PalmReadingScreen(
    palmAnalysisState: PalmAnalysisUiState,
    savedReadingsState: DataListState<PalmReading>,
    onAnalyzePalmBitmap: (Bitmap, String) -> Unit,
    onDeleteReading: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedHandSide by rememberSaveable { mutableStateOf("Right Hand") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bmp
                onAnalyzePalmBitmap(bmp, selectedHandSide)
            } catch (_: Exception) {
            }
        }
    }

    // Camera capture preview
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            selectedBitmap = bmp
            onAnalyzePalmBitmap(bmp, selectedHandSide)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("palm_reading_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AI Samudrika Shastra • Hand & Palm Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Analyze your Jeevan Rekha (Life Line), Hridaya Rekha (Heart Line), Mastishka Rekha (Head Line) & Planetary Mounts using Gemini Multimodal Vision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hand side selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Right Hand", "Left Hand").forEach { side ->
                            FilterChip(
                                selected = selectedHandSide == side,
                                onClick = { selectedHandSide = side },
                                label = { Text(side) },
                                modifier = Modifier.testTag("filter_chip_${side.replace(" ", "_").lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Palm Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_palm_guide_1794486135041),
                            contentDescription = "Vedic Palmistry Lines Guide",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = palmAnalysisState !is PalmAnalysisUiState.Analyzing,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_palm_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Photo")
                        }

                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            enabled = palmAnalysisState !is PalmAnalysisUiState.Analyzing,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_palm_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Scan")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instant Sample Scan Button for one-tap verification on browser emulator
                    OutlinedButton(
                        onClick = {
                            val bmp = VedicAstrologyEngine.createSamplePalmBitmap(selectedHandSide)
                            selectedBitmap = bmp
                            onAnalyzePalmBitmap(bmp, selectedHandSide)
                        },
                        enabled = palmAnalysisState !is PalmAnalysisUiState.Analyzing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sample_palm_scan_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run AI Analysis on Sample $selectedHandSide Diagram")
                    }
                }
            }
        }

        // AI Analysis Output
        when (palmAnalysisState) {
            is PalmAnalysisUiState.Idle -> Unit
            is PalmAnalysisUiState.Analyzing -> {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Scanning Hastrekha lines & planetary mounts with AI...")
                        }
                    }
                }
            }
            is PalmAnalysisUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = palmAnalysisState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is PalmAnalysisUiState.Success -> {
                val res = palmAnalysisState.result
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "✨ Samudrika Shastra Report (${res.handSide})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PalmLineCard(title = "Jeevan Rekha (Life Line & Vitality)", body = res.lifeLineSummary)
                        PalmLineCard(title = "Hridaya Rekha (Heart Line & Emotions)", body = res.heartLineSummary)
                        PalmLineCard(title = "Mastishka Rekha (Head Line & Intellect)", body = res.headLineSummary)
                        PalmLineCard(title = "Bhagya Rekha & Planetary Mounts", body = res.fateAndMounts)
                        PalmLineCard(title = "Sacred Vedic Remedies & Mantra", body = res.remedies)
                    }
                }
            }
        }

        // Saved Cloud Palm Readings
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Saved Palm Readings",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved Hastrekha History",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        when (savedReadingsState) {
            is DataListState.Loading -> {
                item { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
            }
            is DataListState.Error -> {
                item {
                    Text(
                        text = savedReadingsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is DataListState.Success -> {
                if (savedReadingsState.items.isEmpty()) {
                    item {
                        Text(
                            text = "No palm readings saved yet. Scan a palm photo above to receive and save your AI Hastrekha report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(savedReadingsState.items, key = { it.id }) { reading ->
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
                                    Text(
                                        text = "🖐️ ${reading.handSide} Hastrekha Reading",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(onClick = { onDeleteReading(reading.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Palm Reading",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = "Life Line: ${reading.lifeLineSummary}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remedy: ${reading.remedies}",
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
private fun PalmLineCard(
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
