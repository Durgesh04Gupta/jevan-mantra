package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.domain.AstroLocationPoint
import com.example.domain.VedicAstrologyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.hypot

/**
 * Birth Time input with explicit 12-hour HH:MM field, interactive AM / PM toggle chips,
 * and a Material 3 12-hour TimePicker dialog for accurate Vedic Lagna calculation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthTimeAmPmField(
    timeValue: String,
    onTimeChange: (String) -> Unit,
    label: String = "Birth Time (HH:MM)",
    inputTestTag: String = "birth_time_input",
    modifier: Modifier = Modifier
) {
    val parsed = remember(timeValue) { VedicAstrologyEngine.parseTimeWithAmPm(timeValue) }
    val hhMmText = remember(parsed.hour12, parsed.minute) {
        String.format(Locale.US, "%02d:%02d", parsed.hour12, parsed.minute)
    }
    var rawText by remember(hhMmText) { mutableStateOf(hhMmText) }
    var showClockDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = rawText,
                onValueChange = { updated ->
                    rawText = updated
                    val clean = updated.replace("AM", "", ignoreCase = true)
                        .replace("PM", "", ignoreCase = true)
                        .trim()
                    onTimeChange("$clean ${parsed.period}")
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { showClockDialog = true },
                        modifier = Modifier.testTag("${inputTestTag}_clock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Open AM/PM Clock Picker",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(inputTestTag)
            )

            // AM / PM Segmented Toggle Buttons (52dp height for clean alignment)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .height(54.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    val isAm = parsed.period == "AM"
                    AmPmPillButton(
                        text = "AM",
                        selected = isAm,
                        onClick = {
                            onTimeChange(String.format(Locale.US, "%02d:%02d AM", parsed.hour12, parsed.minute))
                        },
                        testTag = "${inputTestTag}_am_btn"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AmPmPillButton(
                        text = "PM",
                        selected = !isAm,
                        onClick = {
                            onTimeChange(String.format(Locale.US, "%02d:%02d PM", parsed.hour12, parsed.minute))
                        },
                        testTag = "${inputTestTag}_pm_btn"
                    )
                }
            }
        }
    }

    if (showClockDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = parsed.hour24,
            initialMinute = parsed.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showClockDialog = false },
            title = {
                Text(
                    text = "Select Exact Birth Time (AM / PM)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h24 = pickerState.hour
                        val min = pickerState.minute
                        val amPm = if (h24 >= 12) "PM" else "AM"
                        val h12 = when {
                            h24 == 0 -> 12
                            h24 > 12 -> h24 - 12
                            else -> h24
                        }
                        val formatted = String.format(Locale.US, "%02d:%02d %s", h12, min, amPm)
                        rawText = String.format(Locale.US, "%02d:%02d", h12, min)
                        onTimeChange(formatted)
                        showClockDialog = false
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AmPmPillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .height(44.dp)
            .width(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Birth Place input integrated with an Interactive Astrological Accuracy Map & GPS Coordinates Picker.
 * Keeps the text field clean while displaying a crisp single-line Geo Accuracy & LMT pill below.
 */
@Composable
fun BirthPlaceMapField(
    placeValue: String,
    onPlaceChange: (String) -> Unit,
    label: String = "Place of Birth (City, State)",
    inputTestTag: String = "birth_place_input",
    modifier: Modifier = Modifier
) {
    val resolvedGeo = remember(placeValue) {
        VedicAstrologyEngine.resolveCoordinatesForPlace(placeValue)
    }
    val displayTextFieldValue = remember(placeValue) {
        placeValue.substringBefore("(").trim().ifBlank { resolvedGeo.displayName }
    }
    var showMapDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = displayTextFieldValue,
                onValueChange = { typedCity ->
                    onPlaceChange(typedCity)
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { showMapDialog = true },
                        modifier = Modifier.testTag("${inputTestTag}_map_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Select Birth Place on Map",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(inputTestTag)
            )

            FilledTonalButton(
                onClick = { showMapDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("${inputTestTag}_open_map_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin on Map",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Map", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        // Crisp Single-Line Astrological Coordinates & Local Mean Time (LMT) Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMapDialog = true }
                .testTag("${inputTestTag}_coords_badge")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Geo: ${resolvedGeo.coordinatesBadge} • ${resolvedGeo.lmtOffsetLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pin Map ▸",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }

    if (showMapDialog) {
        InteractiveAstroMapDialog(
            initialLocation = resolvedGeo,
            onDismiss = { showMapDialog = false },
            onConfirmLocation = { point ->
                onPlaceChange(point.formattedWithCoords)
                showMapDialog = false
            }
        )
    }
}

@Composable
fun InteractiveAstroMapDialog(
    initialLocation: AstroLocationPoint,
    onDismiss: () -> Unit,
    onConfirmLocation: (AstroLocationPoint) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCityName by rememberSaveable { mutableStateOf(initialLocation.cityName) }
    var selectedStateName by rememberSaveable { mutableStateOf(initialLocation.stateOrRegion) }
    var pinLat by rememberSaveable { mutableDoubleStateOf(initialLocation.latitude) }
    var pinLon by rememberSaveable { mutableDoubleStateOf(initialLocation.longitude) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var gpsStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val currentPoint = remember(selectedCityName, selectedStateName, pinLat, pinLon) {
        AstroLocationPoint(
            cityName = selectedCityName,
            stateOrRegion = selectedStateName,
            latitude = pinLat,
            longitude = pinLon
        )
    }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            VedicAstrologyEngine.vedicCityAtlas
        } else {
            VedicAstrologyEngine.vedicCityAtlas.filter {
                it.cityName.contains(searchQuery, ignoreCase = true) ||
                    it.stateOrRegion.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val gpsPoint = fetchDeviceGpsLocation(context)
                if (gpsPoint != null) {
                    selectedCityName = gpsPoint.cityName
                    selectedStateName = gpsPoint.stateOrRegion
                    pinLat = gpsPoint.latitude
                    pinLon = gpsPoint.longitude
                    gpsStatusMessage = "GPS locked: ${gpsPoint.formattedWithCoords}"
                } else {
                    gpsStatusMessage = "GPS signal unavailable — tap anywhere on the map to pin exact coordinates."
                }
            }
        } else {
            gpsStatusMessage = "Location permission declined — tap/drag the map or search any city."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("interactive_astro_map_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vedic Birth Place Accuracy Map",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap or drag the map pin for exact Latitude, Longitude & LMT Lagna correction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Map")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search city, district or state...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        val q = searchQuery.trim()
                                        scope.launch {
                                            val geocoded = geocodeCityName(context, q)
                                            if (geocoded != null) {
                                                selectedCityName = geocoded.cityName
                                                selectedStateName = geocoded.stateOrRegion
                                                pinLat = geocoded.latitude
                                                pinLon = geocoded.longitude
                                                gpsStatusMessage = "Located ${geocoded.formattedWithCoords}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Locate", fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_city_search_input")
                    )

                    FilledTonalButton(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasFine || hasCoarse) {
                                scope.launch {
                                    val gpsPoint = fetchDeviceGpsLocation(context)
                                    if (gpsPoint != null) {
                                        selectedCityName = gpsPoint.cityName
                                        selectedStateName = gpsPoint.stateOrRegion
                                        pinLat = gpsPoint.latitude
                                        pinLon = gpsPoint.longitude
                                        gpsStatusMessage = "GPS locked: ${gpsPoint.formattedWithCoords}"
                                    } else {
                                        gpsStatusMessage = "GPS signal unavailable — tap anywhere on the map below."
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("map_use_gps_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use GPS", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GPS", maxLines = 1, softWrap = false)
                    }
                }

                if (gpsStatusMessage != null) {
                    Text(
                        text = gpsStatusMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredCities.forEach { city ->
                        val isSelected = city.cityName.equals(selectedCityName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCityName = city.cityName
                                selectedStateName = city.stateOrRegion
                                pinLat = city.latitude
                                pinLon = city.longitude
                            },
                            label = { Text("${city.cityName} (${String.format(Locale.US, "%.1f°N", city.latitude)})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                InteractiveIndiaAstroCanvas(
                    pinLatitude = pinLat,
                    pinLongitude = pinLon,
                    selectedCityLabel = selectedCityName,
                    onMapCoordinatePicked = { newLat, newLon ->
                        pinLat = newLat
                        pinLon = newLon
                        val nearest = VedicAstrologyEngine.vedicCityAtlas.minByOrNull {
                            hypot(it.latitude - newLat, it.longitude - newLon)
                        }
                        if (nearest != null && hypot(nearest.latitude - newLat, nearest.longitude - newLon) <= 1.15) {
                            selectedCityName = nearest.cityName
                            selectedStateName = nearest.stateOrRegion
                        } else if (!selectedCityName.contains("Pin")) {
                            selectedStateName = "Custom Pin"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .testTag("astro_map_canvas")
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Selected: ${currentPoint.formattedWithCoords}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lat: ${String.format(Locale.US, "%.2f° N", pinLat)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Lon: ${String.format(Locale.US, "%.2f° E", pinLon)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = currentPoint.lmtOffsetLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "Lagna & Sunrise are computed using Lahiri Ayanamsa & 82.50°E IST Meridian.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirmLocation(currentPoint) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("confirm_map_location_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Coordinates", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveIndiaAstroCanvas(
    pinLatitude: Double,
    pinLongitude: Double,
    selectedCityLabel: String,
    onMapCoordinatePicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val minLat = 6.0
    val maxLat = 36.0
    val minLon = 66.0
    val maxLon = 96.0

    val goldColor = Color(0xFFFFC857)
    val saffronColor = Color(0xFFFF6B35)
    val mapBgColor = Color(0xFF100820)
    val landFillColor = Color(0xFF211339)
    val gridColor = Color(0xFF3E2C5E).copy(alpha = 0.55f)

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFFEAE0F6).toArgb()
            textSize = 22f
        }
    }
    val pinLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFFFFC857).toArgb()
            textSize = 25f
            isFakeBoldText = true
        }
    }

    val indiaOutlineLatLon = remember {
        listOf(
            35.5 to 74.5, 34.2 to 77.8, 31.0 to 79.0, 29.5 to 80.2,
            27.5 to 84.0, 27.2 to 88.5, 28.0 to 95.5, 26.0 to 95.0,
            23.5 to 93.0, 22.0 to 89.0, 21.5 to 87.0, 19.8 to 85.8,
            17.7 to 83.3, 16.0 to 81.2, 13.1 to 80.3, 10.3 to 79.8,
            8.1 to 77.5, 9.9 to 76.2, 12.9 to 74.8, 15.4 to 73.8,
            19.0 to 72.8, 21.1 to 72.7, 20.7 to 70.4, 22.5 to 69.0,
            23.8 to 68.5, 25.0 to 70.5, 28.0 to 70.5, 30.2 to 73.8,
            32.8 to 74.5, 35.5 to 74.5
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(mapBgColor)
            .border(1.5.dp, goldColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val lon = (minLon + (offset.x / size.width.toDouble()) * (maxLon - minLon))
                        .coerceIn(minLon, maxLon)
                    val lat = (maxLat - (offset.y / size.height.toDouble()) * (maxLat - minLat))
                        .coerceIn(minLat, maxLat)
                    onMapCoordinatePicked(lat, lon)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pos = change.position
                    val lon = (minLon + (pos.x / size.width.toDouble()) * (maxLon - minLon))
                        .coerceIn(minLon, maxLon)
                    val lat = (maxLat - (pos.y / size.height.toDouble()) * (maxLat - minLat))
                        .coerceIn(minLat, maxLat)
                    onMapCoordinatePicked(lat, lon)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun lonToX(lon: Double): Float =
                (((lon - minLon) / (maxLon - minLon)) * w).toFloat()

            fun latToY(lat: Double): Float =
                (((maxLat - lat) / (maxLat - minLat)) * h).toFloat()

            for (lonStep in 70..95 step 5) {
                val x = lonToX(lonStep.toDouble())
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }
            for (latStep in 10..35 step 5) {
                val y = latToY(latStep.toDouble())
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            val outlinePath = Path().apply {
                indiaOutlineLatLon.forEachIndexed { idx, (lat, lon) ->
                    val px = lonToX(lon)
                    val py = latToY(lat)
                    if (idx == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path = outlinePath, color = landFillColor)
            drawPath(
                path = outlinePath,
                color = goldColor.copy(alpha = 0.45f),
                style = Stroke(width = 2.2f)
            )

            val istX = lonToX(82.5)
            drawLine(
                color = goldColor.copy(alpha = 0.65f),
                start = Offset(istX, 0f),
                end = Offset(istX, h),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "82.5°E IST",
                istX + 6f,
                26f,
                labelPaint
            )

            VedicAstrologyEngine.vedicCityAtlas.forEach { city ->
                val cx = lonToX(city.longitude)
                val cy = latToY(city.latitude)
                drawCircle(
                    color = goldColor.copy(alpha = 0.65f),
                    radius = 5f,
                    center = Offset(cx, cy)
                )
            }

            val pinX = lonToX(pinLongitude.coerceIn(minLon, maxLon))
            val pinY = latToY(pinLatitude.coerceIn(minLat, maxLat))

            drawLine(
                color = saffronColor.copy(alpha = 0.55f),
                start = Offset(pinX, 0f),
                end = Offset(pinX, h),
                strokeWidth = 1.5f
            )
            drawLine(
                color = saffronColor.copy(alpha = 0.55f),
                start = Offset(0f, pinY),
                end = Offset(w, pinY),
                strokeWidth = 1.5f
            )

            drawCircle(
                color = saffronColor.copy(alpha = 0.28f),
                radius = 24f,
                center = Offset(pinX, pinY)
            )
            drawCircle(
                color = saffronColor,
                radius = 9f,
                center = Offset(pinX, pinY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = Offset(pinX, pinY)
            )

            val labelText = String.format(
                Locale.US,
                "%s (%.2f°N, %.2f°E)",
                selectedCityLabel,
                pinLatitude,
                pinLongitude
            )
            val textX = (pinX + 14f).coerceAtMost(w - 260f).coerceAtLeast(12f)
            val textY = (pinY - 14f).coerceAtLeast(36f)
            drawContext.canvas.nativeCanvas.drawText(labelText, textX, textY, pinLabelPaint)
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchDeviceGpsLocation(context: Context): AstroLocationPoint? =
    withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            val bestLocation = providers.mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }.maxByOrNull { it.time } ?: return@withContext null

            val lat = bestLocation.latitude
            val lon = bestLocation.longitude
            val geocoder = Geocoder(context, Locale.US)
            @Suppress("DEPRECATION")
            val addresses = runCatching { geocoder.getFromLocation(lat, lon, 1) }.getOrNull()
            val addr = addresses?.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: "GPS Location"
            val state = addr?.adminArea ?: addr?.countryCode ?: "India"
            AstroLocationPoint(
                cityName = city,
                stateOrRegion = state,
                latitude = lat,
                longitude = lon
            )
        } catch (e: Exception) {
            null
        }
    }

private suspend fun geocodeCityName(context: Context, query: String): AstroLocationPoint? =
    withContext(Dispatchers.IO) {
        val localMatch = VedicAstrologyEngine.vedicCityAtlas.firstOrNull {
            it.cityName.contains(query, ignoreCase = true) ||
                query.contains(it.cityName, ignoreCase = true)
        }
        if (localMatch != null) return@withContext localMatch

        try {
            val geocoder = Geocoder(context, Locale.US)
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(query, 1)
            val first = results?.firstOrNull()
            if (first != null) {
                return@withContext AstroLocationPoint(
                    cityName = first.locality ?: first.featureName ?: query,
                    stateOrRegion = first.adminArea ?: first.countryName ?: "India",
                    latitude = first.latitude,
                    longitude = first.longitude
                )
            }
        } catch (_: Exception) {
        }

        VedicAstrologyEngine.resolveCoordinatesForPlace(query)
    }
