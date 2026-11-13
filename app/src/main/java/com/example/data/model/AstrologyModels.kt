package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class KundaliProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val birthDate: String = "",
    val birthTime: String = "",
    val birthPlace: String = "",
    val rashi: String = "",
    val nakshatra: String = "",
    val lagna: String = "",
    val aiSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MilanReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val boyName: String = "",
    val boyDetails: String = "",
    val girlName: String = "",
    val girlDetails: String = "",
    val totalGuna: Long = 0L,
    val compatibilityVerdict: String = "",
    val aiAnalysis: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PalmReading(
    @DocumentId val id: String = "",
    val userId: String = "",
    val handSide: String = "",
    val lifeLineSummary: String = "",
    val heartLineSummary: String = "",
    val headLineSummary: String = "",
    val fateAndMounts: String = "",
    val remedies: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ZodiacSignInfo(
    val id: String,
    val sanskritName: String,
    val englishName: String,
    val hindiTitle: String,
    val element: String,
    val rulingPlanet: String,
    val symbolGlyph: String,
    val luckyColor: String,
    val luckyNumber: Int
)

data class GunaKootaScore(
    val kootaName: String,
    val domain: String,
    val maxPoints: Int,
    val obtainedPoints: Int,
    val description: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String
)
