package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.KundaliProfile
import com.example.data.model.MilanReport
import com.example.data.model.PalmReading
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AstrologyRepository
import com.example.domain.CalculatedKundaliChart
import com.example.domain.CalculatedMilanResult
import com.example.domain.VedicAstrologyEngine
import com.example.notifications.HoroscopeNotificationScheduler
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data object Loading : AiGenerationState
    data class Streaming(val partialText: String) : AiGenerationState
    data class Success(val text: String) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

sealed interface DataListState<out T> {
    data object Loading : DataListState<Nothing>
    data class Success<T>(val items: List<T>) : DataListState<T>
    data class Error(val message: String) : DataListState<Nothing>
}

data class PalmAnalysisResult(
    val handSide: String,
    val lifeLineSummary: String,
    val heartLineSummary: String,
    val headLineSummary: String,
    val fateAndMounts: String,
    val remedies: String
)

sealed interface PalmAnalysisUiState {
    data object Idle : PalmAnalysisUiState
    data object Analyzing : PalmAnalysisUiState
    data class Success(val result: PalmAnalysisResult) : PalmAnalysisUiState
    data class Error(val message: String) : PalmAnalysisUiState
}

class AstrologyViewModel(
    private val repository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val currentUserId: String
) : ViewModel() {

    private val textModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    // 2. Structured JSON Gemini 3.8 Flash model for Multimodal Hand/Palmistry Analysis
    private val palmJsonModel by lazy {
        val palmSchema = Schema.obj(
            properties = mapOf(
                "lifeLineSummary" to Schema.string(description = "Analysis of Jeevan Rekha (Life Line): vitality, energy, and major life transitions"),
                "heartLineSummary" to Schema.string(description = "Analysis of Hridaya Rekha (Heart Line): emotional nature, relationships, and devotion"),
                "headLineSummary" to Schema.string(description = "Analysis of Mastishka Rekha (Head Line): intellect, career aptitude, and decision-making style"),
                "fateAndMounts" to Schema.string(description = "Analysis of Bhagya Rekha (Fate Line) and planetary mounts (Guru, Surya, Shukra, Shani)"),
                "remedies" to Schema.string(description = "Auspicious Vedic mantra, gemstone/color suggestion, and daily spiritual remedy")
            )
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = palmSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    // --- USER PREFERENCES ---
    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UserPreferences()
        )

    // --- FIRESTORE REALTIME COLLECTIONS (Two-Tier Flow Error Handling) ---
    val savedKundalis: StateFlow<DataListState<KundaliProfile>> = repository.observeKundalis(currentUserId)
        .map<List<KundaliProfile>, DataListState<KundaliProfile>> { DataListState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing kundalis", rootError)
            emit(DataListState.Error(rootError.message ?: "Failed to load saved Kundalis"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DataListState.Loading
        )

    val savedMilanReports: StateFlow<DataListState<MilanReport>> = repository.observeMilanReports(currentUserId)
        .map<List<MilanReport>, DataListState<MilanReport>> { DataListState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing milan reports", rootError)
            emit(DataListState.Error(rootError.message ?: "Failed to load saved Milan reports"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DataListState.Loading
        )

    val savedPalmReadings: StateFlow<DataListState<PalmReading>> = repository.observePalmReadings(currentUserId)
        .map<List<PalmReading>, DataListState<PalmReading>> { DataListState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing palm readings", rootError)
            emit(DataListState.Error(rootError.message ?: "Failed to load saved palm readings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DataListState.Loading
        )

    // --- DAILY HOROSCOPE AI STATE ---
    private val _dailyHoroscopeAiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val dailyHoroscopeAiState: StateFlow<AiGenerationState> = _dailyHoroscopeAiState.asStateFlow()

    // --- KUNDALI CHART & AI ANALYSIS STATE ---
    private val _activeKundaliChart = MutableStateFlow(
        VedicAstrologyEngine.calculateKundaliChart("Aarav Sharma", "1998-05-14", "10:30", "Varanasi, UP")
    )
    val activeKundaliChart: StateFlow<CalculatedKundaliChart> = _activeKundaliChart.asStateFlow()

    private val _activeKundaliMeta = MutableStateFlow(
        KundaliProfile(
            name = "Aarav Sharma",
            birthDate = "1998-05-14",
            birthTime = "10:30",
            birthPlace = "Varanasi, UP"
        )
    )
    val activeKundaliMeta: StateFlow<KundaliProfile> = _activeKundaliMeta.asStateFlow()

    private val _kundaliAiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val kundaliAiState: StateFlow<AiGenerationState> = _kundaliAiState.asStateFlow()

    // --- FREE AI KUNDALI Q&A CHAT STATE ---
    private val _qaMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome_1",
                isUser = false,
                text = "Namaste! 🙏 I am your JevanMantra AI Jyotish Acharya. Ask me any question about your Janam Kundali — career yoga, marriage timing, Raj Yoga, financial prosperity, or planetary remedies — 100% free!",
                timestampLabel = "Just now"
            )
        )
    )
    val qaMessages: StateFlow<List<ChatMessage>> = _qaMessages.asStateFlow()

    private val _qaStreamingState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val qaStreamingState: StateFlow<AiGenerationState> = _qaStreamingState.asStateFlow()

    // --- KUNDLI MILAN STATE ---
    private val _milanCalculation = MutableStateFlow<CalculatedMilanResult?>(null)
    val milanCalculation: StateFlow<CalculatedMilanResult?> = _milanCalculation.asStateFlow()

    private val _milanAiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val milanAiState: StateFlow<AiGenerationState> = _milanAiState.asStateFlow()

    // --- PALM READING MULTIMODAL AI STATE ---
    private val _palmAnalysisState = MutableStateFlow<PalmAnalysisUiState>(PalmAnalysisUiState.Idle)
    val palmAnalysisState: StateFlow<PalmAnalysisUiState> = _palmAnalysisState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    // --- PREFERENCE ACTIONS ---
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun selectRashi(rashiId: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedRashi(rashiId)
            _dailyHoroscopeAiState.value = AiGenerationState.Idle
        }
    }

    fun updateNotificationSettings(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        rashiId: String
    ) {
        viewModelScope.launch {
            preferencesRepository.setNotificationSchedule(enabled, hour, minute)
            HoroscopeNotificationScheduler.scheduleDailyReminder(
                context = context,
                enabled = enabled,
                hourOfDay = hour,
                minute = minute,
                rashiId = rashiId
            )
            _statusBanner.value = if (enabled) {
                String.format(Locale.US, "Daily Rashifal alerts scheduled for %02d:%02d", hour, minute)
            } else {
                "Daily Rashifal notifications paused"
            }
        }
    }

    // --- 1. AI PERSONALIZED DAILY HOROSCOPE ---
    fun generatePersonalizedDailyHoroscope(rashiId: String) {
        val sign = VedicAstrologyEngine.getSignById(rashiId)
        val panchang = VedicAstrologyEngine.getTodayPanchang()
        val prompt = """
            You are an expert Vedic Jyotish Acharya on the JevanMantra app.
            Provide a deeply personalized, uplifting, and actionable Daily Rashifal (Horoscope) for:
            - Rashi: ${sign.sanskritName} (${sign.englishName} - ${sign.hindiTitle})
            - Ruling Planet: ${sign.rulingPlanet}, Element: ${sign.element}
            - Today's Panchang: ${panchang.dateFormatted}, Tithi: ${panchang.tithi}, Nakshatra: ${panchang.nakshatra}, Yoga: ${panchang.yoga}
            
            Format with clear sections using concise bullet points:
            1. Career & Finance (Karmakshetra & Arthik)
            2. Love & Family Harmony (Prem & Parivar)
            3. Health & Vitality (Swasthya)
            4. Today's Vedic Upay & Mantra (Remedy of the Day)
        """.trimIndent()

        viewModelScope.launch {
            _dailyHoroscopeAiState.value = AiGenerationState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    textModel.generateContentStream(prompt).collect { chunk ->
                        accumulated.append(chunk.text.orEmpty())
                        _dailyHoroscopeAiState.value = AiGenerationState.Streaming(accumulated.toString())
                    }
                }
                if (accumulated.isNotBlank()) {
                    _dailyHoroscopeAiState.value = AiGenerationState.Success(accumulated.toString())
                } else {
                    _dailyHoroscopeAiState.value = AiGenerationState.Error("Model returned an empty horoscope.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Daily horoscope generation timed out", e)
                _dailyHoroscopeAiState.value = AiGenerationState.Error("Prediction timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Daily horoscope generation failed", e)
                _dailyHoroscopeAiState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to generate prediction.")
            }
        }
    }

    // --- 2. FREE AI JANAM KUNDALI GENERATION & SAVING ---
    fun generateAndAnalyzeKundali(
        name: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String,
        saveToCloud: Boolean = true
    ) {
        val cleanName = name.ifBlank { "JevanMantra Seeker" }
        val cleanDate = birthDate.ifBlank { "1998-05-14" }
        val cleanTime = birthTime.ifBlank { "10:30" }
        val cleanPlace = birthPlace.ifBlank { "New Delhi, India" }

        val chart = VedicAstrologyEngine.calculateKundaliChart(cleanName, cleanDate, cleanTime, cleanPlace)
        _activeKundaliChart.value = chart
        _activeKundaliMeta.value = KundaliProfile(
            name = cleanName,
            birthDate = cleanDate,
            birthTime = cleanTime,
            birthPlace = cleanPlace,
            rashi = chart.rashi,
            nakshatra = chart.nakshatra,
            lagna = chart.lagna
        )

        val houseSummary = (1..12).joinToString(", ") { h ->
            val p = chart.housePlanets[h].orEmpty()
            "H$h: ${if (p.isEmpty()) "Empty" else p.joinToString("/")}"
        }

        val prompt = """
            You are a revered Vedic Jyotish Acharya on JevanMantra.
            Analyze this Janam Kundali birth chart in clear, warm English with authentic Sanskrit astrological terms (Hinglish/Vedic friendly):
            - Native Name: $cleanName
            - Date & Time of Birth: $cleanDate at $cleanTime
            - Place of Birth: $cleanPlace
            - Lagna (Ascendant): ${chart.lagna}
            - Janam Rashi (Moon Sign): ${chart.rashi}
            - Janam Nakshatra: ${chart.nakshatra}
            - Current Dasha: ${chart.vimshottariDasha}
            - Mangal Dosha Status: ${chart.mangalDosha}
            - Bhava Planetary Placements: $houseSummary

            Provide a structured, insightful reading covering:
            1. Core Personality & Lagna Strength
            2. Raj Yogas, Career & Wealth Potential (10th & 11th House insights)
            3. Marriage, Relationships & 7th House Harmony
            4. Personalized Vedic Remedies (Upay, Mantra & Gemstone advice)
        """.trimIndent()

        viewModelScope.launch {
            _kundaliAiState.value = AiGenerationState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    textModel.generateContentStream(prompt).collect { chunk ->
                        accumulated.append(chunk.text.orEmpty())
                        _kundaliAiState.value = AiGenerationState.Streaming(accumulated.toString())
                    }
                }
                val finalSummary = accumulated.toString()
                if (finalSummary.isNotBlank()) {
                    _kundaliAiState.value = AiGenerationState.Success(finalSummary)
                    if (saveToCloud) {
                        val saveRes = repository.createKundali(
                            name = cleanName,
                            birthDate = cleanDate,
                            birthTime = cleanTime,
                            birthPlace = cleanPlace,
                            rashi = chart.rashi,
                            nakshatra = chart.nakshatra,
                            lagna = chart.lagna,
                            aiSummary = finalSummary
                        )
                        if (saveRes.isSuccess) {
                            _statusBanner.value = "Kundali for $cleanName saved to your cloud account"
                        }
                    }
                } else {
                    _kundaliAiState.value = AiGenerationState.Error("Model returned an empty Kundali reading.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Kundali AI analysis timed out", e)
                _kundaliAiState.value = AiGenerationState.Error("Kundali analysis timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Kundali AI analysis failed", e)
                _kundaliAiState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to analyze Kundali.")
            }
        }
    }

    fun loadSavedKundali(profile: KundaliProfile) {
        val chart = VedicAstrologyEngine.calculateKundaliChart(
            profile.name,
            profile.birthDate,
            profile.birthTime,
            profile.birthPlace
        )
        _activeKundaliChart.value = chart
        _activeKundaliMeta.value = profile
        if (profile.aiSummary.isNotBlank()) {
            _kundaliAiState.value = AiGenerationState.Success(profile.aiSummary)
        }
    }

    fun deleteSavedKundali(id: String) {
        viewModelScope.launch {
            repository.deleteKundali(id)
        }
    }

    // --- 3. FREE AI KUNDALI QUESTION & ANSWER (JYOTISH CHAT) ---
    fun askKundaliQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return

        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed,
            timestampLabel = timeFmt
        )
        _qaMessages.value = _qaMessages.value + userMsg

        val meta = _activeKundaliMeta.value
        val chart = _activeKundaliChart.value
        val houseSummary = (1..12).joinToString(", ") { h ->
            val p = chart.housePlanets[h].orEmpty()
            "H$h:${if (p.isEmpty()) "-" else p.joinToString("/")}"
        }

        val prompt = """
            You are the JevanMantra Free AI Jyotish Acharya answering a user's specific question about their Janam Kundali.
            Active Kundali Context:
            - Name: ${meta.name} (DOB: ${meta.birthDate} at ${meta.birthTime}, ${meta.birthPlace})
            - Lagna: ${chart.lagna}, Moon Rashi: ${chart.rashi}, Nakshatra: ${chart.nakshatra}
            - Dasha: ${chart.vimshottariDasha}, Mangal Status: ${chart.mangalDosha}
            - Planetary Houses: $houseSummary

            User's Question: "$trimmed"

            Give a clear, compassionate, specific Vedic astrology answer referencing their Lagna, Rashi, planetary houses, and practical remedies (Upay/Mantra). Keep it well-structured and easy to read.
        """.trimIndent()

        viewModelScope.launch {
            _qaStreamingState.value = AiGenerationState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    textModel.generateContentStream(prompt).collect { chunk ->
                        accumulated.append(chunk.text.orEmpty())
                        _qaStreamingState.value = AiGenerationState.Streaming(accumulated.toString())
                    }
                }
                val answer = accumulated.toString()
                if (answer.isNotBlank()) {
                    _qaMessages.value = _qaMessages.value + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = answer,
                        timestampLabel = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                    )
                    _qaStreamingState.value = AiGenerationState.Idle
                } else {
                    _qaStreamingState.value = AiGenerationState.Error("Empty response from AI Jyotish.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Kundali Q&A timed out", e)
                _qaStreamingState.value = AiGenerationState.Error("Response timed out. Please ask again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Kundali Q&A failed", e)
                _qaStreamingState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to answer question.")
            }
        }
    }

    // --- 4. KUNDLI MILAN (36 GUNA + AI COMPATIBILITY) ---
    fun performKundliMilan(
        boyName: String,
        boyDate: String,
        boyTime: String,
        boyPlace: String,
        girlName: String,
        girlDate: String,
        girlTime: String,
        girlPlace: String
    ) {
        val cleanBoy = boyName.ifBlank { "Rohan" }
        val cleanGirl = girlName.ifBlank { "Priya" }
        val result = VedicAstrologyEngine.calculateAshtaKootaMilan(
            boyName = cleanBoy,
            boyDate = boyDate.ifBlank { "1996-04-18" },
            boyTime = boyTime.ifBlank { "09:15" },
            boyPlace = boyPlace.ifBlank { "Delhi" },
            girlName = cleanGirl,
            girlDate = girlDate.ifBlank { "1998-09-24" },
            girlTime = girlTime.ifBlank { "16:40" },
            girlPlace = girlPlace.ifBlank { "Mumbai" }
        )
        _milanCalculation.value = result

        val kootaBreakdown = result.kootaScores.joinToString("\n") {
            "- ${it.kootaName}: ${it.obtainedPoints}/${it.maxPoints}"
        }

        val prompt = """
            You are an expert Vedic Marriage Astrologer on JevanMantra performing Ashta-Koota Kundli Milan.
            Partner 1 (Var): $cleanBoy (${result.boyRashi}, ${result.boyNakshatra})
            Partner 2 (Kanya): $cleanGirl (${result.girlRashi}, ${result.girlNakshatra})
            Total Ashta-Koota Score: ${result.totalGuna} / 36 Gunas (${result.verdict})
            Mangal Dosha Check: ${result.mangalCompatibility}
            Koota Breakdown:
            $kootaBreakdown

            Provide a warm, comprehensive Kundli Milan compatibility report covering:
            1. Emotional & Mental Bonding (Graha Maitri & Gana insights)
            2. Financial Prosperity & Family Luck (Bhakoot & Tara insights)
            3. Health, Longevity & Nadi Assessment
            4. Auspicious Vedic Blessings & Remedies for a Happy Married Life
        """.trimIndent()

        viewModelScope.launch {
            _milanAiState.value = AiGenerationState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    textModel.generateContentStream(prompt).collect { chunk ->
                        accumulated.append(chunk.text.orEmpty())
                        _milanAiState.value = AiGenerationState.Streaming(accumulated.toString())
                    }
                }
                val finalAnalysis = accumulated.toString()
                if (finalAnalysis.isNotBlank()) {
                    _milanAiState.value = AiGenerationState.Success(finalAnalysis)
                    repository.saveMilanReport(
                        boyName = cleanBoy,
                        boyDetails = "$boyDate • ${result.boyRashi}",
                        girlName = cleanGirl,
                        girlDetails = "$girlDate • ${result.girlRashi}",
                        totalGuna = result.totalGuna,
                        compatibilityVerdict = result.verdict,
                        aiAnalysis = finalAnalysis
                    )
                    _statusBanner.value = "Kundli Milan report ($cleanBoy & $cleanGirl) saved"
                } else {
                    _milanAiState.value = AiGenerationState.Error("Empty compatibility analysis returned.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Kundli Milan AI timed out", e)
                _milanAiState.value = AiGenerationState.Error("Compatibility analysis timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Kundli Milan AI failed", e)
                _milanAiState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to generate compatibility report.")
            }
        }
    }

    fun deleteMilanReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteMilanReport(reportId)
        }
    }

    // --- 5. MULTIMODAL HAND / PALM READING (HASTREKHA ANALYSIS) ---
    fun analyzePalmImage(bitmap: Bitmap, handSide: String) {
        viewModelScope.launch {
            _palmAnalysisState.value = PalmAnalysisUiState.Analyzing
            try {
                // Scale bitmap down if very large to keep payload well under 20MB and speed up inference
                val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                    Bitmap.createScaledBitmap(bitmap, 768, 768, true)
                } else {
                    bitmap
                }

                val promptText = """
                    You are a master of Vedic Samudrika Shastra (Hastrekha / Indian Palmistry) on the JevanMantra app.
                    Carefully examine this $handSide image (or palm diagram) and provide a detailed, uplifting, and authentic Vedic palmistry reading across all 5 required JSON fields:
                    - lifeLineSummary (Jeevan Rekha: vitality, health, and major life milestones)
                    - heartLineSummary (Hridaya Rekha: love, emotional depth, and interpersonal harmony)
                    - headLineSummary (Mastishka Rekha: wisdom, focus, career strengths, and creativity)
                    - fateAndMounts (Bhagya Rekha & Mounts of Guru/Jupiter, Surya/Sun, Shukra/Venus, Shani/Saturn: wealth and destiny)
                    - remedies (Specific Vedic mantra, charitable Upay, and spiritual practice)
                """.trimIndent()

                val response = withTimeout(AI_TIMEOUT_MS) {
                    palmJsonModel.generateContent(
                        content {
                            image(scaledBitmap)
                            text(promptText)
                        }
                    )
                }

                val jsonString = response.text ?: throw IllegalStateException("Model returned an empty palm reading.")
                val json = JSONObject(jsonString)

                val result = PalmAnalysisResult(
                    handSide = handSide,
                    lifeLineSummary = json.optString("lifeLineSummary", "Strong and curving Jeevan Rekha indicating enduring vitality."),
                    heartLineSummary = json.optString("heartLineSummary", "Balanced Hridaya Rekha showing deep devotion and emotional clarity."),
                    headLineSummary = json.optString("headLineSummary", "Clear Mastishka Rekha reflecting sharp intellect and leadership."),
                    fateAndMounts = json.optString("fateAndMounts", "Elevated Guru and Surya mounts indicate career growth and respect."),
                    remedies = json.optString("remedies", "Offer Arghya (water) to Surya Dev at sunrise and chant Gayatri Mantra 11 times daily.")
                )

                _palmAnalysisState.value = PalmAnalysisUiState.Success(result)

                repository.savePalmReading(
                    handSide = result.handSide,
                    lifeLineSummary = result.lifeLineSummary,
                    heartLineSummary = result.heartLineSummary,
                    headLineSummary = result.headLineSummary,
                    fateAndMounts = result.fateAndMounts,
                    remedies = result.remedies
                )
                _statusBanner.value = "Hastrekha ($handSide) analysis saved to your profile"
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Palm reading timed out", e)
                _palmAnalysisState.value = PalmAnalysisUiState.Error("Palm analysis timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Palm reading failed", e)
                _palmAnalysisState.value = PalmAnalysisUiState.Error(e.localizedMessage ?: "Failed to analyze palm image.")
            }
        }
    }

    fun deletePalmReading(readingId: String) {
        viewModelScope.launch {
            repository.deletePalmReading(readingId)
        }
    }

    companion object {
        private const val TAG = "AstrologyViewModel"
        private const val AI_TIMEOUT_MS = 60_000L
        private const val STOP_TIMEOUT_MS = 5000L

        fun provideFactory(context: Context, currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val databaseId = appContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AstrologyViewModel(
                    repository = AstrologyRepository(db),
                    preferencesRepository = UserPreferencesRepository(appContext),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
