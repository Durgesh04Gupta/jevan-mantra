package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.KundaliProfile
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AstrologyRepository
import com.example.domain.VedicAstrologyEngine
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

data class PendingRegistrationProfile(
    val fullName: String,
    val birthDate: String,
    val birthTime: String,
    val birthPlace: String
)

sealed interface UserAuthUiState {
    data object Unauthenticated : UserAuthUiState
    data object Loading : UserAuthUiState
    data class Authenticated(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String?,
        val isGoogleAccount: Boolean = true,
        val savedBirthCharts: List<KundaliProfile> = emptyList()
    ) : UserAuthUiState
    data class Error(val message: String) : UserAuthUiState
}

class UserAuthViewModel(
    private val databaseId: String,
    private val auth: FirebaseAuth = Firebase.auth,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    private val repository: AstrologyRepository by lazy {
        AstrologyRepository(db)
    }

    private val _authState = MutableStateFlow<UserAuthUiState>(
        auth.currentUser?.toAuthenticatedState() ?: UserAuthUiState.Unauthenticated
    )
    val authState: StateFlow<UserAuthUiState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _savedBirthCharts = MutableStateFlow<List<KundaliProfile>>(emptyList())
    val savedBirthCharts: StateFlow<List<KundaliProfile>> = _savedBirthCharts.asStateFlow()

    private var pendingRegistration: PendingRegistrationProfile? = null
    private var chartsObserverJob: Job? = null
    private var activeObservedUid: String? = null
    private var isManualSessionActive: Boolean = false

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            isManualSessionActive = false
            _authState.value = user.toAuthenticatedState(_savedBirthCharts.value)
            startObservingUserBirthCharts(user.uid)
            val reg = pendingRegistration
            if (reg != null) {
                pendingRegistration = null
                saveBirthChart(
                    name = reg.fullName.ifBlank { user.displayName ?: "JevanMantra Seeker" },
                    birthDate = reg.birthDate,
                    birthTime = reg.birthTime,
                    birthPlace = reg.birthPlace
                )
            }
        } else if (!isManualSessionActive) {
            stopObservingUserBirthCharts()
            _savedBirthCharts.value = emptyList()
            if (_authState.value !is UserAuthUiState.Error) {
                _authState.value = UserAuthUiState.Unauthenticated
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        restoreManualSessionIfPresent()
    }

    private fun restoreManualSessionIfPresent() {
        val prefsRepo = preferencesRepository ?: return
        if (auth.currentUser != null) return
        viewModelScope.launch {
            try {
                val prefs = prefsRepo.preferencesFlow.first()
                if (prefs.isManualLoggedIn && prefs.manualFullName.isNotBlank()) {
                    val uid = prefs.manualUserId.ifBlank { "manual_${prefs.manualFullName.lowercase(Locale.US).hashCode()}" }
                    val chart = VedicAstrologyEngine.calculateKundaliChart(
                        prefs.manualFullName,
                        prefs.manualBirthDate,
                        prefs.manualBirthTime,
                        prefs.manualBirthPlace
                    )
                    val initialProfile = KundaliProfile(
                        id = "local_primary",
                        userId = uid,
                        name = prefs.manualFullName,
                        birthDate = prefs.manualBirthDate,
                        birthTime = chart.formattedBirthTime,
                        birthPlace = prefs.manualBirthPlace,
                        rashi = chart.rashi,
                        nakshatra = chart.nakshatra,
                        lagna = chart.lagna,
                        aiSummary = "Lagna: ${chart.lagna}, Rashi: ${chart.rashi}, Nakshatra: ${chart.nakshatra}"
                    )
                    isManualSessionActive = true
                    _savedBirthCharts.value = listOf(initialProfile)
                    _authState.value = UserAuthUiState.Authenticated(
                        uid = uid,
                        displayName = prefs.manualFullName,
                        email = prefs.manualEmailOrMobile.ifBlank { "Vedic Profile Account" },
                        photoUrl = null,
                        isGoogleAccount = false,
                        savedBirthCharts = listOf(initialProfile)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore manual session", e)
            }
        }
    }

    private fun FirebaseUser.toAuthenticatedState(
        charts: List<KundaliProfile> = emptyList()
    ): UserAuthUiState.Authenticated {
        return UserAuthUiState.Authenticated(
            uid = uid,
            displayName = displayName ?: "JevanMantra Seeker",
            email = email ?: "",
            photoUrl = photoUrl?.toString(),
            isGoogleAccount = true,
            savedBirthCharts = charts
        )
    }

    private fun startObservingUserBirthCharts(userId: String) {
        if (activeObservedUid == userId && chartsObserverJob?.isActive == true) return
        stopObservingUserBirthCharts()
        activeObservedUid = userId

        chartsObserverJob = viewModelScope.launch {
            repository.observeKundalis(userId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing user birth charts", rootError)
                }
                .collect { charts ->
                    _savedBirthCharts.value = charts
                    val user = auth.currentUser
                    if (user != null && user.uid == userId) {
                        _authState.value = user.toAuthenticatedState(charts)
                    }
                }
        }
    }

    private fun stopObservingUserBirthCharts() {
        chartsObserverJob?.cancel()
        chartsObserverJob = null
        activeObservedUid = null
    }

    /**
     * Manual Sign-In using Email/Mobile/Name and Password.
     */
    fun signInManually(identifier: String, password: String) {
        val cleanId = identifier.trim()
        val cleanPass = password.trim()
        if (cleanId.isBlank()) {
            _authState.value = UserAuthUiState.Error("Please enter your Email, Mobile number, or Name.")
            return
        }
        if (cleanPass.length < 4) {
            _authState.value = UserAuthUiState.Error("Please enter your password (at least 4 characters).")
            return
        }

        _authState.value = UserAuthUiState.Loading
        viewModelScope.launch {
            try {
                val prefs = preferencesRepository?.preferencesFlow?.first()
                // If user has previously registered a manual account on this device, verify password if matching identifier
                if (prefs != null &&
                    prefs.manualPassword.isNotBlank() &&
                    (prefs.manualEmailOrMobile.equals(cleanId, ignoreCase = true) ||
                        prefs.manualFullName.equals(cleanId, ignoreCase = true)) &&
                    prefs.manualPassword != cleanPass
                ) {
                    _authState.value = UserAuthUiState.Error("Incorrect password for $cleanId. Please try again.")
                    return@launch
                }

                val displayName = when {
                    prefs != null && prefs.manualFullName.isNotBlank() &&
                        (prefs.manualEmailOrMobile.equals(cleanId, ignoreCase = true) ||
                            prefs.manualFullName.equals(cleanId, ignoreCase = true)) -> prefs.manualFullName
                    cleanId.contains("@") -> cleanId.substringBefore("@").replaceFirstChar { it.uppercase() }
                    else -> cleanId
                }
                val emailOrMobile = if (prefs != null && prefs.manualEmailOrMobile.isNotBlank()) {
                    prefs.manualEmailOrMobile
                } else {
                    cleanId
                }
                val birthDate = prefs?.manualBirthDate ?: "1998-05-14"
                val birthTime = prefs?.manualBirthTime ?: "10:30 AM"
                val birthPlace = prefs?.manualBirthPlace ?: "Varanasi, UP (25.32°N, 83.00°E)"
                val uid = prefs?.manualUserId?.ifBlank { null }
                    ?: "manual_${cleanId.lowercase(Locale.US).hashCode()}"

                val chart = VedicAstrologyEngine.calculateKundaliChart(displayName, birthDate, birthTime, birthPlace)
                val rashiId = VedicAstrologyEngine.zodiacSigns.firstOrNull {
                    chart.rashi.contains(it.sanskritName, ignoreCase = true)
                }?.id ?: "mesha"

                preferencesRepository?.saveManualAccount(
                    userId = uid,
                    fullName = displayName,
                    emailOrMobile = emailOrMobile,
                    password = cleanPass,
                    birthDate = birthDate,
                    birthTime = chart.formattedBirthTime,
                    birthPlace = birthPlace,
                    selectedRashiId = rashiId
                )

                val profile = KundaliProfile(
                    id = "local_primary",
                    userId = uid,
                    name = displayName,
                    birthDate = birthDate,
                    birthTime = chart.formattedBirthTime,
                    birthPlace = birthPlace,
                    rashi = chart.rashi,
                    nakshatra = chart.nakshatra,
                    lagna = chart.lagna,
                    aiSummary = "Lagna: ${chart.lagna}, Rashi: ${chart.rashi}, Nakshatra: ${chart.nakshatra}"
                )

                isManualSessionActive = true
                _savedBirthCharts.value = listOf(profile)
                _authState.value = UserAuthUiState.Authenticated(
                    uid = uid,
                    displayName = displayName,
                    email = emailOrMobile,
                    photoUrl = null,
                    isGoogleAccount = false,
                    savedBirthCharts = listOf(profile)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Manual sign-in failed", e)
                _authState.value = UserAuthUiState.Error(e.localizedMessage ?: "Sign in failed.")
            }
        }
    }

    /**
     * Manual Registration with Full Name, Email/Mobile, Password, and Birth Chart details.
     */
    fun registerManually(
        fullName: String,
        emailOrMobile: String,
        password: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String
    ) {
        val cleanName = fullName.trim()
        val cleanEmail = emailOrMobile.trim()
        val cleanPass = password.trim()
        val cleanDate = birthDate.trim()
        val cleanTime = birthTime.trim()
        val cleanPlace = birthPlace.trim()

        if (cleanName.isBlank()) {
            _authState.value = UserAuthUiState.Error("Please enter your full name.")
            return
        }
        if (cleanEmail.isBlank()) {
            _authState.value = UserAuthUiState.Error("Please enter your Email or Mobile number.")
            return
        }
        if (cleanPass.length < 4) {
            _authState.value = UserAuthUiState.Error("Please create a password of at least 4 characters.")
            return
        }
        if (cleanDate.isBlank() || cleanTime.isBlank() || cleanPlace.isBlank()) {
            _authState.value = UserAuthUiState.Error("Please enter your birth date, time (AM/PM), and birth city.")
            return
        }

        _authState.value = UserAuthUiState.Loading
        viewModelScope.launch {
            try {
                val uid = "manual_${UUID.randomUUID().toString().take(8)}"
                val chart = VedicAstrologyEngine.calculateKundaliChart(
                    cleanName,
                    cleanDate,
                    cleanTime,
                    cleanPlace
                )
                val rashiId = VedicAstrologyEngine.zodiacSigns.firstOrNull {
                    chart.rashi.contains(it.sanskritName, ignoreCase = true)
                }?.id ?: "mesha"

                preferencesRepository?.saveManualAccount(
                    userId = uid,
                    fullName = cleanName,
                    emailOrMobile = cleanEmail,
                    password = cleanPass,
                    birthDate = cleanDate,
                    birthTime = chart.formattedBirthTime,
                    birthPlace = cleanPlace,
                    selectedRashiId = rashiId
                )

                val profile = KundaliProfile(
                    id = "local_primary",
                    userId = uid,
                    name = cleanName,
                    birthDate = cleanDate,
                    birthTime = chart.formattedBirthTime,
                    birthPlace = cleanPlace,
                    rashi = chart.rashi,
                    nakshatra = chart.nakshatra,
                    lagna = chart.lagna,
                    aiSummary = "Lagna: ${chart.lagna}, Rashi: ${chart.rashi}, Nakshatra: ${chart.nakshatra}, Time: ${chart.formattedBirthTime}, Coords: ${chart.coordinatesLabel}"
                )

                isManualSessionActive = true
                _savedBirthCharts.value = listOf(profile)
                _authState.value = UserAuthUiState.Authenticated(
                    uid = uid,
                    displayName = cleanName,
                    email = cleanEmail,
                    photoUrl = null,
                    isGoogleAccount = false,
                    savedBirthCharts = listOf(profile)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Manual registration failed", e)
                _authState.value = UserAuthUiState.Error(e.localizedMessage ?: "Registration failed.")
            }
        }
    }

    /**
     * Silent auto sign-in using GetGoogleIdOption in a single-option GetCredentialRequest.
     */
    fun attemptSilentSignIn(context: Context, credentialManager: CredentialManager) {
        if (auth.currentUser != null || isManualSessionActive) return

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }
        if (clientId.isBlank() || clientId == "placeholder_web_client_id") return

        val activityContext = context.findActivity() ?: context
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    signInWithGoogleIdToken(googleIdToken)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto sign-in unavailable: ${e.message}")
            }
        }
    }

    /**
     * Registers a new user profile with their initial Janam Kundali birth details via Google Sign-In.
     */
    fun registerWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        fullName: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String
    ) {
        if (fullName.isBlank()) {
            _authState.value = UserAuthUiState.Error("Please enter your full name to register your Vedic profile.")
            return
        }
        if (birthDate.isBlank() || birthTime.isBlank() || birthPlace.isBlank()) {
            _authState.value = UserAuthUiState.Error("Please enter your birth date, time, and city for your Janam Kundali.")
            return
        }
        pendingRegistration = PendingRegistrationProfile(
            fullName = fullName.trim(),
            birthDate = birthDate.trim(),
            birthTime = birthTime.trim(),
            birthPlace = birthPlace.trim()
        )
        signInWithGoogle(context, credentialManager)
    }

    /**
     * Interactive Google Sign-In using GetSignInWithGoogleOption with automatic fallback to
     * GetGoogleIdOption(filterByAuthorizedAccounts = false) for broader device compatibility.
     */
    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _authState.value = UserAuthUiState.Error(
                "Google Sign-In configuration missing. Please use Manual Sign In / Register."
            )
            return
        }
        if (clientId.isBlank() || clientId == "placeholder_web_client_id") {
            _authState.value = UserAuthUiState.Error(
                "Google OAuth client not configured in this build. Please use Manual Sign In or Manual Register."
            )
            return
        }

        val activityContext = context.findActivity() ?: context
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val primaryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        _authState.value = UserAuthUiState.Loading

        viewModelScope.launch {
            try {
                val result = try {
                    credentialManager.getCredential(activityContext, primaryRequest)
                } catch (primaryErr: Exception) {
                    if (primaryErr is GetCredentialCancellationException || primaryErr is CancellationException) {
                        throw primaryErr
                    }
                    Log.w(TAG, "Primary GetSignInWithGoogleOption failed, trying GetGoogleIdOption fallback", primaryErr)
                    val fallbackOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    credentialManager.getCredential(activityContext, fallbackRequest)
                }

                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    signInWithGoogleIdToken(googleIdToken)
                } else {
                    _authState.value = UserAuthUiState.Error("Unexpected credential type received.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                _authState.value = UserAuthUiState.Unauthenticated
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google account on device/emulator: ${e.message}", e)
                _authState.value = UserAuthUiState.Error(
                    "No Google account is signed into this device/emulator. Please use Manual Sign In / Manual Register right below, or add a Google account in device Settings."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _authState.value = UserAuthUiState.Error(
                    "Google Sign-In unavailable on this device (${e.localizedMessage ?: "Play Services error"}). You can Sign In or Register Manually right below!"
                )
            }
        }
    }

    /**
     * Exchanges a Google ID Token for a Firebase Auth session.
     */
    fun signInWithGoogleIdToken(idToken: String) {
        if (auth.currentUser == null) {
            _authState.value = UserAuthUiState.Loading
        }
        viewModelScope.launch {
            try {
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(authCredential).await()
                val user = auth.currentUser
                if (user != null) {
                    isManualSessionActive = false
                    _authState.value = user.toAuthenticatedState(_savedBirthCharts.value)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase signInWithCredential failed", e)
                _authState.value = UserAuthUiState.Error(
                    e.localizedMessage ?: "Firebase authentication failed"
                )
            }
        }
    }

    /**
     * Saves a user's Vedic birth chart (Janam Kundali) to Cloud Firestore under `/users/{uid}/kundalis`.
     */
    fun saveBirthChart(
        name: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String,
        aiSummary: String = "",
        onSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanName = name.ifBlank { "JevanMantra Seeker" }
        val cleanDate = birthDate.ifBlank { "1998-05-14" }
        val cleanTime = VedicAstrologyEngine.parseTimeWithAmPm(birthTime.ifBlank { "10:30 AM" }).formatted12Hr
        val cleanPlace = birthPlace.ifBlank { "New Delhi, DL (28.61°N, 77.21°E)" }
        val chart = VedicAstrologyEngine.calculateKundaliChart(cleanName, cleanDate, cleanTime, cleanPlace)
        val summary = aiSummary.ifBlank {
            "Lagna: ${chart.lagna}, Rashi: ${chart.rashi}, Nakshatra: ${chart.nakshatra}, Time: ${chart.formattedBirthTime}, Coords: ${chart.coordinatesLabel} (${chart.lmtOffsetLabel})"
        }

        viewModelScope.launch {
            val result = repository.createKundali(
                name = cleanName,
                birthDate = cleanDate,
                birthTime = cleanTime,
                birthPlace = cleanPlace,
                rashi = chart.rashi,
                nakshatra = chart.nakshatra,
                lagna = chart.lagna,
                aiSummary = summary
            )
            result.onSuccess { docId ->
                onSaved(docId)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to save birth chart")
            }
        }
    }

    fun clearError() {
        if (_authState.value is UserAuthUiState.Error) {
            _authState.value = auth.currentUser?.toAuthenticatedState(_savedBirthCharts.value)
                ?: UserAuthUiState.Unauthenticated
        }
    }

    /**
     * Clears Credential Manager state inside NonCancellable before signing out of Firebase Auth
     * and clearing any active manual session.
     */
    fun signOut(credentialManager: CredentialManager, onSignOutComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    isManualSessionActive = false
                    preferencesRepository?.setManualLoggedIn(false)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    stopObservingUserBirthCharts()
                    auth.signOut()
                    _savedBirthCharts.value = emptyList()
                    _authState.value = UserAuthUiState.Unauthenticated
                    onSignOutComplete()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        stopObservingUserBirthCharts()
    }

    companion object {
        private const val TAG = "UserAuthViewModel"

        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val databaseId = appContext.getString(R.string.firestore_database_id)
                UserAuthViewModel(
                    databaseId = databaseId,
                    preferencesRepository = UserPreferencesRepository(appContext)
                )
            }
        }
    }
}
