package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.UserAuthUiState
import com.example.ui.auth.UserAuthViewModel
import com.example.ui.screens.HomeRashifalScreen
import com.example.ui.screens.KundaliAndQaScreen
import com.example.ui.screens.KundliMilanScreen
import com.example.ui.screens.PalmReadingScreen
import com.example.ui.screens.SettingsAndProfileScreen
import com.example.ui.theme.JevanMantraTheme
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.util.configureAppCheck
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            JevanMantraRootApp()
        }
    }
}

@Composable
fun JevanMantraRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val userPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.UserPreferences()
    )

    val authViewModel: UserAuthViewModel = viewModel(
        factory = UserAuthViewModel.provideFactory(context)
    )
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    JevanMantraTheme(darkTheme = userPrefs.isDarkMode) {
        val authenticatedSession = authState as? UserAuthUiState.Authenticated
        if (authenticatedSession == null) {
            SignInScreen(
                authViewModel = authViewModel,
                isDarkMode = userPrefs.isDarkMode,
                onToggleDarkMode = { enabled ->
                    scope.launch { prefsRepo.setDarkMode(enabled) }
                }
            )
        } else {
            key(authenticatedSession.uid) {
                val viewModel: AstrologyViewModel = viewModel(
                    key = authenticatedSession.uid,
                    factory = AstrologyViewModel.provideFactory(context, authenticatedSession.uid)
                )
                JevanMantraMainScaffold(
                    authenticatedSession = authenticatedSession,
                    currentUser = currentUser,
                    authViewModel = authViewModel,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JevanMantraMainScaffold(
    authenticatedSession: UserAuthUiState.Authenticated,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    authViewModel: UserAuthViewModel,
    viewModel: AstrologyViewModel
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val dailyAiState by viewModel.dailyHoroscopeAiState.collectAsStateWithLifecycle()
    val activeChart by viewModel.activeKundaliChart.collectAsStateWithLifecycle()
    val activeMeta by viewModel.activeKundaliMeta.collectAsStateWithLifecycle()
    val kundaliAiState by viewModel.kundaliAiState.collectAsStateWithLifecycle()
    val savedKundalis by viewModel.savedKundalis.collectAsStateWithLifecycle()
    val qaMessages by viewModel.qaMessages.collectAsStateWithLifecycle()
    val qaStreamingState by viewModel.qaStreamingState.collectAsStateWithLifecycle()
    val milanCalculation by viewModel.milanCalculation.collectAsStateWithLifecycle()
    val milanAiState by viewModel.milanAiState.collectAsStateWithLifecycle()
    val savedMilanReports by viewModel.savedMilanReports.collectAsStateWithLifecycle()
    val palmAnalysisState by viewModel.palmAnalysisState.collectAsStateWithLifecycle()
    val savedPalmReadings by viewModel.savedPalmReadings.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // BackHandler on secondary tabs returns to Home Rashifal tab
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_icon_1794486134011),
                            contentDescription = "JevanMantra Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "JevanMantra",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode(!preferences.isDarkMode) },
                        modifier = Modifier.testTag("top_bar_dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (preferences.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark or Light Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.WbSunny, contentDescription = "Rashifal") },
                    label = { Text("Rashifal") },
                    modifier = Modifier.testTag("nav_tab_rashifal")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Kundali & AI") },
                    label = { Text("Kundali AI") },
                    modifier = Modifier.testTag("nav_tab_kundali")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Kundli Milan") },
                    label = { Text("Milan") },
                    modifier = Modifier.testTag("nav_tab_milan")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.BackHand, contentDescription = "Hand Analysis") },
                    label = { Text("Hastrekha") },
                    modifier = Modifier.testTag("nav_tab_palm")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    val primarySavedKundali = (savedKundalis as? com.example.ui.viewmodel.DataListState.Success)?.items?.firstOrNull()
                    HomeRashifalScreen(
                        selectedRashiId = preferences.selectedRashiId,
                        savedKundaliProfile = primarySavedKundali,
                        dailyAiState = dailyAiState,
                        onSelectRashi = viewModel::selectRashi,
                        onEnsureDailyHoroscopeLoaded = viewModel::ensureDailyHoroscopeLoaded,
                        onRefreshAiRashifal = { rashiId ->
                            viewModel.generatePersonalizedDailyHoroscope(rashiId, forceRefresh = true)
                        },
                        onNavigateToKundali = { selectedTab = 1 },
                        onNavigateToMilan = { selectedTab = 2 },
                        onNavigateToPalm = { selectedTab = 3 }
                    )
                }
                1 -> KundaliAndQaScreen(
                    activeChart = activeChart,
                    activeMeta = activeMeta,
                    kundaliAiState = kundaliAiState,
                    savedKundalisState = savedKundalis,
                    qaMessages = qaMessages,
                    qaStreamingState = qaStreamingState,
                    onGenerateKundali = { name, dob, tob, pob ->
                        viewModel.generateAndAnalyzeKundali(name, dob, tob, pob)
                    },
                    onLoadSavedKundali = viewModel::loadSavedKundali,
                    onDeleteSavedKundali = viewModel::deleteSavedKundali,
                    onAskQuestion = viewModel::askKundaliQuestion
                )
                2 -> KundliMilanScreen(
                    milanCalculation = milanCalculation,
                    milanAiState = milanAiState,
                    savedReportsState = savedMilanReports,
                    onPerformMilan = viewModel::performKundliMilan,
                    onDeleteReport = viewModel::deleteMilanReport
                )
                3 -> PalmReadingScreen(
                    palmAnalysisState = palmAnalysisState,
                    savedReadingsState = savedPalmReadings,
                    onAnalyzePalmBitmap = viewModel::analyzePalmImage,
                    onDeleteReading = viewModel::deletePalmReading
                )
                4 -> SettingsAndProfileScreen(
                    currentUser = currentUser,
                    userDisplayName = authenticatedSession.displayName,
                    userEmailOrPhone = authenticatedSession.email,
                    isGoogleAccount = authenticatedSession.isGoogleAccount,
                    preferences = preferences,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onUpdateNotifications = { enabled, h, m ->
                        viewModel.updateNotificationSettings(
                            context = context,
                            enabled = enabled,
                            hour = h,
                            minute = m,
                            rashiId = preferences.selectedRashiId
                        )
                    },
                    onSignOut = {
                        authViewModel.signOut(credentialManager = credentialManager)
                    }
                )
            }
        }
    }
}
