package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.KundaliProfile
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        val savedBirthCharts: List<KundaliProfile> = emptyList()
    ) : UserAuthUiState
    data class Error(val message: String) : UserAuthUiState
}

class UserAuthViewModel(
    private val databaseId: String,
    private val auth: FirebaseAuth = Firebase.auth
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

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
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
        } else {
            stopObservingUserBirthCharts()
            _savedBirthCharts.value = emptyList()
            if (_authState.value !is UserAuthUiState.Error) {
                _authState.value = UserAuthUiState.Unauthenticated
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun FirebaseUser.toAuthenticatedState(
        charts: List<KundaliProfile> = emptyList()
    ): UserAuthUiState.Authenticated {
        return UserAuthUiState.Authenticated(
            uid = uid,
            displayName = displayName ?: "JevanMantra Seeker",
            email = email ?: "",
            photoUrl = photoUrl?.toString(),
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
     * Silent auto sign-in using GetGoogleIdOption in a single-option GetCredentialRequest.
     */
    fun attemptSilentSignIn(context: Context, credentialManager: CredentialManager) {
        if (auth.currentUser != null) return

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }

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
                val result = credentialManager.getCredential(context, request)
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
     * Once Google Sign-In completes, the birth chart is automatically persisted to Cloud Firestore.
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
     * Interactive Google Sign-In using GetSignInWithGoogleOption in a single-option GetCredentialRequest.
     */
    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _authState.value = UserAuthUiState.Error(
                "Google Sign-In configuration missing: default_web_client_id not found"
            )
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        _authState.value = UserAuthUiState.Loading

        viewModelScope.launch {
            try {
                val activityContext = context as? Activity ?: context
                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    signInWithGoogleIdToken(googleIdToken)
                } else {
                    _authState.value = UserAuthUiState.Error("Unexpected credential type received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                _authState.value = UserAuthUiState.Unauthenticated
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _authState.value = UserAuthUiState.Error(
                    e.localizedMessage ?: "Google Sign-In failed. Please try again."
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
        val cleanTime = birthTime.ifBlank { "10:30" }
        val cleanPlace = birthPlace.ifBlank { "New Delhi, India" }
        val chart = VedicAstrologyEngine.calculateKundaliChart(cleanName, cleanDate, cleanTime, cleanPlace)
        val summary = aiSummary.ifBlank {
            "Lagna: ${chart.lagna}, Rashi: ${chart.rashi}, Nakshatra: ${chart.nakshatra}, Dasha: ${chart.vimshottariDasha}"
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
     * Clears Credential Manager state inside NonCancellable before signing out of Firebase Auth.
     */
    fun signOut(credentialManager: CredentialManager, onSignOutComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    stopObservingUserBirthCharts()
                    auth.signOut()
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
                UserAuthViewModel(databaseId = databaseId)
            }
        }
    }
}
