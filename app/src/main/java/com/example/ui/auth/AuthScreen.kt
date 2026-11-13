package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.BirthPlaceMapField
import com.example.ui.components.BirthTimeAmPmField
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val activityContext = context.findActivity() ?: context
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val activityContext = context.findActivity() ?: context
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun SignInScreen(
    authViewModel: UserAuthViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onAuthSuccess: () -> Unit = {}
) {
    AuthScreen(
        authViewModel = authViewModel,
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        onAuthSuccess = onAuthSuccess
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: UserAuthViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onAuthSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // 0 = Sign In, 1 = Register
    var selectedAuthTab by rememberSaveable { mutableIntStateOf(0) }

    // Manual Sign-In state
    var loginIdentifier by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var showLoginPassword by rememberSaveable { mutableStateOf(false) }

    // Registration state (Manual + Google)
    var regFullName by rememberSaveable { mutableStateOf("") }
    var regEmailOrMobile by rememberSaveable { mutableStateOf("") }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var showRegPassword by rememberSaveable { mutableStateOf(false) }
    var regBirthDate by rememberSaveable { mutableStateOf("1998-05-14") }
    var regBirthTime by rememberSaveable { mutableStateOf("10:30 AM") }
    var regBirthPlace by rememberSaveable { mutableStateOf("Varanasi, UP (25.32°N, 83.00°E)") }

    val isLoading = authState is UserAuthUiState.Loading
    val errorMessage = (authState as? UserAuthUiState.Error)?.message

    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        kotlinx.coroutines.delay(350L)
        authViewModel.attemptSilentSignIn(
            context = context,
            credentialManager = credentialManager
        )
    }

    LaunchedEffect(authState) {
        if (authState is UserAuthUiState.Authenticated) {
            onAuthSuccess()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sign_in_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with Theme Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JevanMantra Vedic AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { onToggleDarkMode(!isDarkMode) },
                    modifier = Modifier.testTag("auth_dark_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark or Light Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sleek Compact Cosmic Header with Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cosmic_banner_1794486167917),
                    contentDescription = "Vedic Astrology Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_icon_1794486134011),
                        contentDescription = "JevanMantra Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "JevanMantra",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Free AI Kundali • 36-Guna Milan • Hastrekha Scan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Clean & Attractive Auth Card (Sign In | Register)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedAuthTab,
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        Tab(
                            selected = selectedAuthTab == 0,
                            onClick = {
                                authViewModel.clearError()
                                selectedAuthTab = 0
                            },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Sign In", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("auth_tab_sign_in")
                        )
                        Tab(
                            selected = selectedAuthTab == 1,
                            onClick = {
                                authViewModel.clearError()
                                selectedAuthTab = 1
                            },
                            icon = {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Register", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("auth_tab_register")
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (errorMessage != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (selectedAuthTab == 0) {
                            // ================= SIGN IN TAB =================
                            Text(
                                text = "Welcome Back, Seeker 🙏",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sign in manually with your account details or continue with Google.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = {
                                    authViewModel.clearError()
                                    loginIdentifier = it
                                },
                                label = { Text("Email, Mobile or Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_login_identifier_input")
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    authViewModel.clearError()
                                    loginPassword = it
                                },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                        Icon(
                                            imageVector = if (showLoginPassword) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = "Toggle Password Visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (showLoginPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_login_password_input")
                            )

                            // Option 1: Manual Sign In Button
                            Button(
                                onClick = {
                                    authViewModel.clearError()
                                    authViewModel.signInManually(
                                        identifier = loginIdentifier,
                                        password = loginPassword
                                    )
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("manual_sign_in_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sign In Manually",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OrDivider(text = "OR")

                            // Option 2: Google Sign In Button
                            OutlinedButton(
                                onClick = {
                                    authViewModel.clearError()
                                    authViewModel.signInWithGoogle(
                                        context = context,
                                        credentialManager = credentialManager
                                    )
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = {
                                    authViewModel.clearError()
                                    selectedAuthTab = 1
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("switch_to_register_button")
                            ) {
                                Text("New to JevanMantra? Create Free Account →")
                            }
                        } else {
                            // ================= REGISTER TAB (MANUAL & GOOGLE) =================
                            Text(
                                text = "Create Vedic Profile & Janam Kundali",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Fill your birth details once — register manually or link your Google account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = {
                                    authViewModel.clearError()
                                    regFullName = it
                                },
                                label = { Text("Full Name (e.g., Aarav Sharma)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = regEmailOrMobile,
                                    onValueChange = {
                                        authViewModel.clearError()
                                        regEmailOrMobile = it
                                    },
                                    label = { Text("Email or Mobile") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("register_email_input")
                                )

                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = {
                                        authViewModel.clearError()
                                        regPassword = it
                                    },
                                    label = { Text("Password") },
                                    trailingIcon = {
                                        IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                            Icon(
                                                imageVector = if (showRegPassword) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = "Toggle Password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showRegPassword) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .testTag("register_password_input")
                                )
                            }

                            OutlinedTextField(
                                value = regBirthDate,
                                onValueChange = { regBirthDate = it },
                                label = { Text("Birth Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_dob_input")
                            )

                            BirthTimeAmPmField(
                                timeValue = regBirthTime,
                                onTimeChange = { regBirthTime = it },
                                label = "Birth Time (HH:MM)",
                                inputTestTag = "register_tob_input"
                            )

                            BirthPlaceMapField(
                                placeValue = regBirthPlace,
                                onPlaceChange = { regBirthPlace = it },
                                label = "Birth City & Map Coordinates",
                                inputTestTag = "register_place_input"
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Option 1: Manual Registration Button
                            Button(
                                onClick = {
                                    authViewModel.clearError()
                                    authViewModel.registerManually(
                                        fullName = regFullName,
                                        emailOrMobile = regEmailOrMobile,
                                        password = regPassword,
                                        birthDate = regBirthDate,
                                        birthTime = regBirthTime,
                                        birthPlace = regBirthPlace
                                    )
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("manual_register_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Register Manually & Create Kundali",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OrDivider(text = "OR")

                            // Option 2: Google Registration Button
                            OutlinedButton(
                                onClick = {
                                    authViewModel.clearError()
                                    authViewModel.registerWithGoogle(
                                        context = context,
                                        credentialManager = credentialManager,
                                        fullName = regFullName.ifBlank { "JevanMantra Seeker" },
                                        birthDate = regBirthDate,
                                        birthTime = regBirthTime,
                                        birthPlace = regBirthPlace
                                    )
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("register_with_google_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Register & Sync with Google",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact 3-pill Feature Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactFeatureBadge(
                    icon = Icons.Default.AutoAwesome,
                    label = "Free AI Kundali",
                    modifier = Modifier.weight(1f)
                )
                CompactFeatureBadge(
                    icon = Icons.Default.Favorite,
                    label = "36-Guna Milan",
                    modifier = Modifier.weight(1f)
                )
                CompactFeatureBadge(
                    icon = Icons.Default.BackHand,
                    label = "Palm Scan",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OrDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "  $text  ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CompactFeatureBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
