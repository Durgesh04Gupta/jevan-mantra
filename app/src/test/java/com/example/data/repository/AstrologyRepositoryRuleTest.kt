package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AstrologyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveKundali_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AstrologyRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createKundali(
                name = "Aarav Sharma",
                birthDate = "1996-08-15",
                birthTime = "10:45",
                birthPlace = "Jaipur, India",
                rashi = "Simha (Leo)",
                nakshatra = "Magha - Pada 2",
                lagna = "Tula (Libra)",
                aiSummary = "Sun in 10th house indicates strong leadership."
            )
        }
        assertTrue(createResult.isSuccess)
        val kundaliId = createResult.getOrThrow()

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeKundalis(aliceUid).first { list -> list.any { it.id == kundaliId } }
        }
        assertTrue(emittedList.any { it.id == kundaliId && it.name == "Aarav Sharma" })
    }

    @Test
    fun saveMilanAndPalmReading_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AstrologyRepository(firestore)

        val milanResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveMilanReport(
                boyName = "Rohan",
                boyDetails = "1995-03-12 • Mesh Rashi",
                girlName = "Priya",
                girlDetails = "1997-07-21 • Simha Rashi",
                totalGuna = 28,
                compatibilityVerdict = "Uttam Milan (28/36 Gunas)",
                aiAnalysis = "Harmonious Bhakoot and Nadi alignment."
            )
        }
        assertTrue(milanResult.isSuccess)
        val milanId = milanResult.getOrThrow()

        val milanList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMilanReports(aliceUid).first { list -> list.any { it.id == milanId } }
        }
        assertTrue(milanList.any { it.id == milanId && it.totalGuna == 28L })

        val palmResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePalmReading(
                handSide = "Right Hand",
                lifeLineSummary = "Deep and curved Jeevan Rekha.",
                heartLineSummary = "Balanced Hridaya Rekha.",
                headLineSummary = "Clear Mastishka Rekha.",
                fateAndMounts = "Prominent Guru Mount.",
                remedies = "Chant Gayatri Mantra at sunrise."
            )
        }
        assertTrue(palmResult.isSuccess)
    }

    @Test
    fun getKundaliById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AstrologyRepository(firestore)
        val kundaliId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createKundali(
                name = "Alice Private Chart",
                birthDate = "1996-08-15",
                birthTime = "10:45",
                birthPlace = "Delhi, India",
                rashi = "Simha (Leo)",
                nakshatra = "Magha",
                lagna = "Tula",
                aiSummary = "Private summary"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = AstrologyRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getKundaliById(aliceUid, kundaliId)
        }
        assertTrue(crossResult.isFailure)
        val firestoreException = checkNotNull(crossResult.exceptionOrNull()?.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeKundalis_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = AstrologyRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeKundalis(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun userAuthViewModel_saveBirthChart_persistsAndEmitsInAuthState() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val authVm = com.example.ui.auth.UserAuthViewModel(databaseId = databaseId, auth = auth)

        val savedIdDeferred = kotlinx.coroutines.CompletableDeferred<String>()
        authVm.saveBirthChart(
            name = "Meera Joshi",
            birthDate = "1999-11-08",
            birthTime = "06:45",
            birthPlace = "Udaipur, India",
            onSaved = { id -> savedIdDeferred.complete(id) },
            onError = { err -> savedIdDeferred.completeExceptionally(IllegalStateException(err)) }
        )

        val savedId = withTimeout(DEFAULT_TIMEOUT_MS) { savedIdDeferred.await() }
        assertTrue(savedId.isNotBlank())

        val charts = withTimeout(FLOW_TIMEOUT_MS) {
            authVm.savedBirthCharts.first { list -> list.any { it.id == savedId } }
        }
        assertTrue(charts.any { it.id == savedId && it.name == "Meera Joshi" && it.userId == aliceUid })
    }

    private companion object {
        const val ALICE_EMAIL = "alice@jevanmantra.test"
        const val BOB_EMAIL = "bob@jevanmantra.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
