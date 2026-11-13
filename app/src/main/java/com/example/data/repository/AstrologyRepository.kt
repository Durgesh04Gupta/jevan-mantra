package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.KundaliProfile
import com.example.data.model.MilanReport
import com.example.data.model.PalmReading
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AstrologyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- KUNDALI PROFILES ---

    fun observeKundalis(userId: String = requireUserId()): Flow<List<KundaliProfile>> = flow {
        val path = "users/$userId/kundalis"
        emitAll(
            db.collection("users").document(userId).collection("kundalis")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        KundaliProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createKundali(
        name: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String,
        rashi: String,
        nakshatra: String,
        lagna: String,
        aiSummary: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("kundalis").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(80),
            "birthDate" to birthDate.trim().take(30),
            "birthTime" to birthTime.trim().take(20),
            "birthPlace" to birthPlace.trim().take(120),
            "rashi" to rashi.trim().take(40),
            "nakshatra" to nakshatra.trim().take(50),
            "lagna" to lagna.trim().take(40),
            "aiSummary" to aiSummary.trim().take(8000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getKundaliById(targetUserId: String, kundaliId: String): Result<KundaliProfile?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("kundalis").document(kundaliId)
        try {
            docRef.get().await().toObject(
                KundaliProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun deleteKundali(kundaliId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("kundalis").document(kundaliId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- KUNDLI MILAN REPORTS ---

    fun observeMilanReports(userId: String = requireUserId()): Flow<List<MilanReport>> = flow {
        val path = "users/$userId/milan_reports"
        emitAll(
            db.collection("users").document(userId).collection("milan_reports")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MilanReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveMilanReport(
        boyName: String,
        boyDetails: String,
        girlName: String,
        girlDetails: String,
        totalGuna: Int,
        compatibilityVerdict: String,
        aiAnalysis: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("milan_reports").document()
        val payload = mapOf(
            "userId" to uid,
            "boyName" to boyName.trim().take(80),
            "boyDetails" to boyDetails.trim().take(160),
            "girlName" to girlName.trim().take(80),
            "girlDetails" to girlDetails.trim().take(160),
            "totalGuna" to totalGuna.coerceIn(0, 36).toLong(),
            "compatibilityVerdict" to compatibilityVerdict.trim().take(120),
            "aiAnalysis" to aiAnalysis.trim().ifBlank { "Harmonious match." }.take(8000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMilanReport(reportId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("milan_reports").document(reportId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- PALM READINGS ---

    fun observePalmReadings(userId: String = requireUserId()): Flow<List<PalmReading>> = flow {
        val path = "users/$userId/palm_readings"
        emitAll(
            db.collection("users").document(userId).collection("palm_readings")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PalmReading::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun savePalmReading(
        handSide: String,
        lifeLineSummary: String,
        heartLineSummary: String,
        headLineSummary: String,
        fateAndMounts: String,
        remedies: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("palm_readings").document()
        val payload = mapOf(
            "userId" to uid,
            "handSide" to handSide.trim().take(40),
            "lifeLineSummary" to lifeLineSummary.trim().ifBlank { "Clear Jeevan Rekha." }.take(2000),
            "heartLineSummary" to heartLineSummary.trim().ifBlank { "Balanced Hridaya Rekha." }.take(2000),
            "headLineSummary" to headLineSummary.trim().ifBlank { "Focused Mastishka Rekha." }.take(2000),
            "fateAndMounts" to fateAndMounts.trim().ifBlank { "Prominent Guru and Surya mounts." }.take(4000),
            "remedies" to remedies.trim().ifBlank { "Offer water to Surya Dev at sunrise." }.take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePalmReading(readingId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("palm_readings").document(readingId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
