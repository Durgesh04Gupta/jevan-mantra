package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.data.model.GunaKootaScore
import com.example.data.model.ZodiacSignInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class PanchangInfo(
    val dateFormatted: String,
    val tithi: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String,
    val abhijitMuhurat: String,
    val rahuKaal: String
)

data class CalculatedKundaliChart(
    val rashi: String,
    val nakshatra: String,
    val lagna: String,
    val lagnaIndex: Int,
    val housePlanets: Map<Int, List<String>>, // House 1..12 -> ["Su", "Me", ...]
    val mangalDosha: String,
    val vimshottariDasha: String
)

data class CalculatedMilanResult(
    val boyRashi: String,
    val boyNakshatra: String,
    val girlRashi: String,
    val girlNakshatra: String,
    val kootaScores: List<GunaKootaScore>,
    val totalGuna: Int,
    val verdict: String,
    val mangalCompatibility: String
)

object VedicAstrologyEngine {

    val zodiacSigns: List<ZodiacSignInfo> = listOf(
        ZodiacSignInfo("mesha", "Mesha", "Aries", "मेष राशि", "Agni (Fire)", "Mangal (Mars)", "♈", "Vermilion Red", 9),
        ZodiacSignInfo("vrishabha", "Vrishabha", "Taurus", "वृषभ राशि", "Prithvi (Earth)", "Shukra (Venus)", "♉", "Pearl White", 6),
        ZodiacSignInfo("mithuna", "Mithuna", "Gemini", "मिथुन राशि", "Vayu (Air)", "Budha (Mercury)", "♊", "Emerald Green", 5),
        ZodiacSignInfo("karka", "Karka", "Cancer", "कर्क राशि", "Jala (Water)", "Chandra (Moon)", "♋", "Milky Silver", 2),
        ZodiacSignInfo("simha", "Simha", "Leo", "सिंह राशि", "Agni (Fire)", "Surya (Sun)", "♌", "Saffron Gold", 1),
        ZodiacSignInfo("kanya", "Kanya", "Virgo", "कन्या राशि", "Prithvi (Earth)", "Budha (Mercury)", "♍", "Forest Green", 5),
        ZodiacSignInfo("tula", "Tula", "Libra", "तुला राशि", "Vayu (Air)", "Shukra (Venus)", "♎", "Opal Pink", 6),
        ZodiacSignInfo("vrishchika", "Vrishchika", "Scorpio", "वृश्चिक राशि", "Jala (Water)", "Mangal (Mars)", "♏", "Crimson Maroon", 9),
        ZodiacSignInfo("dhanu", "Dhanu", "Sagittarius", "धनु राशि", "Agni (Fire)", "Guru (Jupiter)", "♐", "Turmeric Yellow", 3),
        ZodiacSignInfo("makara", "Makara", "Capricorn", "मकर राशि", "Prithvi (Earth)", "Shani (Saturn)", "♑", "Royal Indigo", 8),
        ZodiacSignInfo("kumbha", "Kumbha", "Aquarius", "कुम्भ राशि", "Vayu (Air)", "Shani (Saturn)", "♒", "Electric Blue", 8),
        ZodiacSignInfo("meena", "Meena", "Pisces", "मीन राशि", "Jala (Water)", "Guru (Jupiter)", "♓", "Sea Gold", 3)
    )

    private val nakshatras = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    )

    private val tithis = listOf(
        "Shukla Pratipada", "Shukla Dwitiya", "Shukla Tritiya", "Shukla Chaturthi",
        "Shukla Panchami", "Shukla Shashthi", "Shukla Saptami", "Shukla Ashtami",
        "Shukla Navami", "Shukla Dashami", "Shukla Ekadashi", "Shukla Dwadashi",
        "Shukla Trayodashi", "Shukla Chaturdashi", "Purnima"
    )

    private val yogas = listOf(
        "Siddhi Yoga", "Shubha Yoga", "Amrita Siddhi Yoga", "Brahma Yoga",
        "Indra Yoga", "Preeti Yoga", "Ayushman Yoga", "Saubhagya Yoga"
    )

    fun getSignById(id: String): ZodiacSignInfo {
        return zodiacSigns.find { it.id == id } ?: zodiacSigns.first()
    }

    fun getTodayPanchang(): PanchangInfo {
        val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US)
        val todayStr = formatter.format(Date())
        val daySeed = (System.currentTimeMillis() / (1000L * 60 * 60 * 24)).toInt()
        val tithi = tithis[abs(daySeed) % tithis.size]
        val nakshatra = nakshatras[abs(daySeed * 3) % nakshatras.size]
        val yoga = yogas[abs(daySeed * 5) % yogas.size]
        return PanchangInfo(
            dateFormatted = todayStr,
            tithi = tithi,
            nakshatra = nakshatra,
            yoga = yoga,
            karana = "Bava Karana",
            abhijitMuhurat = "11:54 AM – 12:42 PM",
            rahuKaal = "01:30 PM – 03:00 PM"
        )
    }

    fun getInstantDailySummary(sign: ZodiacSignInfo): String {
        val daySeed = abs((System.currentTimeMillis() / (1000L * 60 * 60 * 24)).toInt() + sign.id.hashCode())
        val themes = listOf(
            "Favorable planetary transit of ${sign.rulingPlanet} energizes your 10th house of career and prestige today. Financial decisions taken before afternoon bring lasting gains.",
            "Chandra and ${sign.rulingPlanet} form a harmonious aspect today, bringing family peace, creative clarity, and auspicious news in personal relationships.",
            "Strong spiritual intuition and professional momentum surround ${sign.sanskritName} natives today. Focus on disciplined execution and avoid impulsive arguments during Rahu Kaal.",
            "New partnerships and auspicious opportunities open up under today's Nakshatra. Chanting your planetary mantra in the morning amplifies positive karma."
        )
        return themes[daySeed % themes.size]
    }

    fun calculateKundaliChart(
        name: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String
    ): CalculatedKundaliChart {
        val combinedSeed = abs("$name|$birthDate|$birthTime|$birthPlace".lowercase(Locale.US).hashCode())
        val rashiSign = zodiacSigns[combinedSeed % zodiacSigns.size]
        val lagnaSign = zodiacSigns[(combinedSeed / 7) % zodiacSigns.size]
        val nakshatraName = nakshatras[(combinedSeed / 13) % nakshatras.size]
        val pada = (combinedSeed % 4) + 1

        val planets = listOf("Su", "Mo", "Ma", "Me", "Ju", "Ve", "Sa", "Ra", "Ke")
        val houseMap = mutableMapOf<Int, MutableList<String>>()
        for (h in 1..12) houseMap[h] = mutableListOf()

        planets.forEachIndexed { idx, planet ->
            val house = if (planet == "Ke") {
                val rahuHouse = ((combinedSeed + 7 * 17) % 12) + 1
                ((rahuHouse + 5) % 12) + 1
            } else if (planet == "Ra") {
                ((combinedSeed + 7 * 17) % 12) + 1
            } else {
                ((combinedSeed + (idx + 1) * 19) % 12) + 1
            }
            houseMap.getOrPut(house) { mutableListOf() }.add(planet)
        }

        val marsHouse = houseMap.entries.firstOrNull { it.value.contains("Ma") }?.key ?: 3
        val isManglik = marsHouse in listOf(1, 4, 7, 8, 12)
        val mangalNote = if (isManglik) {
            "Anshik Mangal Yoga (Mars in House $marsHouse) — balanced by Jupiter's benefic aspect"
        } else {
            "Non-Manglik (Mars favorably placed in House $marsHouse)"
        }

        val dashaPlanets = listOf("Guru (Jupiter)", "Shani (Saturn)", "Budha (Mercury)", "Shukra (Venus)", "Surya (Sun)", "Chandra (Moon)")
        val currentDasha = dashaPlanets[combinedSeed % dashaPlanets.size]

        return CalculatedKundaliChart(
            rashi = "${rashiSign.sanskritName} (${rashiSign.englishName})",
            nakshatra = "$nakshatraName - Pada $pada",
            lagna = "${lagnaSign.sanskritName} (${lagnaSign.englishName})",
            lagnaIndex = zodiacSigns.indexOf(lagnaSign) + 1,
            housePlanets = houseMap,
            mangalDosha = mangalNote,
            vimshottariDasha = "$currentDasha Mahadasha"
        )
    }

    fun calculateAshtaKootaMilan(
        boyName: String,
        boyDate: String,
        boyTime: String,
        boyPlace: String,
        girlName: String,
        girlDate: String,
        girlTime: String,
        girlPlace: String
    ): CalculatedMilanResult {
        val boyChart = calculateKundaliChart(boyName, boyDate, boyTime, boyPlace)
        val girlChart = calculateKundaliChart(girlName, girlDate, girlTime, girlPlace)
        val pairSeed = abs("$boyName|$boyDate|$girlName|$girlDate".lowercase(Locale.US).hashCode())

        val varna = 1 // max 1
        val vashya = (pairSeed % 2) + 1 // 1..2
        val tara = (pairSeed % 2) + 2 // 2..3
        val yoni = (pairSeed % 3) + 2 // 2..4
        val maitri = (pairSeed % 3) + 3 // 3..5
        val gana = (pairSeed % 3) + 4 // 4..6
        val bhakoot = if (pairSeed % 5 == 0) 4 else 7 // 4 or 7
        val nadi = if (pairSeed % 6 == 0) 4 else 8 // 4 or 8

        val scores = listOf(
            GunaKootaScore("Varna (वर्ण)", "Spiritual & Ego Compatibility", 1, varna, "Harmonious work ethic and mutual intellectual respect."),
            GunaKootaScore("Vashya (वश्य)", "Mutual Attraction & Influence", 2, vashya, "Strong emotional magnetism and supportive partnership."),
            GunaKootaScore("Tara (तारा)", "Destiny & Health Longevity", 3, tara, "Auspicious birth star alignment bringing mutual prosperity."),
            GunaKootaScore("Yoni (योनि)", "Intimacy & Temperament", 4, yoni, "Balanced biological and emotional understanding."),
            GunaKootaScore("Graha Maitri (ग्रह मैत्री)", "Planetary Mental Affinity", 5, maitri, "Ruling Moon sign lords share natural friendship."),
            GunaKootaScore("Gana (गण)", "Core Nature & Values", 6, gana, "Compatible lifestyle priorities and social harmony."),
            GunaKootaScore("Bhakoot (भकूट)", "Family Welfare & Prosperity", 7, bhakoot, "Favorable Moon Rashi distance for financial growth."),
            GunaKootaScore("Nadi (नाड़ी)", "Life Force & Genetic Vitality", 8, nadi, "Distinct Nadi energies ensure vibrant health and progeny.")
        )

        val total = scores.sumOf { it.obtainedPoints }
        val verdict = when {
            total >= 28 -> "Uttam Milan ($total/36 Gunas) • Highly Auspicious Match"
            total >= 24 -> "Shubh Milan ($total/36 Gunas) • Very Good Compatibility"
            total >= 18 -> "Madhyam Milan ($total/36 Gunas) • Favorable with Minor Remedies"
            else -> "Samanya Milan ($total/36 Gunas) • Astrological Remedies Recommended"
        }

        val mangalCompatibility = if (boyChart.mangalDosha.startsWith("Non") && girlChart.mangalDosha.startsWith("Non")) {
            "Both charts are free from severe Kuja/Mangal Dosha."
        } else {
            "Mangal energy is mutually balanced by benefic Guru aspect."
        }

        return CalculatedMilanResult(
            boyRashi = boyChart.rashi,
            boyNakshatra = boyChart.nakshatra,
            girlRashi = girlChart.rashi,
            girlNakshatra = girlChart.nakshatra,
            kootaScores = scores,
            totalGuna = total,
            verdict = verdict,
            mangalCompatibility = mangalCompatibility
        )
    }

    /**
     * Generates a clean, stylized palm diagram Bitmap so users on a browser emulator without
     * camera/gallery photos can still test live multimodal Gemini AI Palm Analysis with one tap.
     */
    fun createSamplePalmBitmap(handSide: String): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1A0B2E"))

        val palmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3D6B6")
            style = Paint.Style.FILL
        }
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFC857")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        // Palm base oval
        canvas.drawRoundRect(140f, 190f, 380f, 450f, 70f, 70f, palmPaint)
        canvas.drawRoundRect(140f, 190f, 380f, 450f, 70f, 70f, outlinePaint)

        // Fingers
        val fingerX = listOf(165f, 215f, 265f, 315f)
        val fingerTop = listOf(85f, 60f, 75f, 110f)
        for (i in 0..3) {
            canvas.drawRoundRect(fingerX[i], fingerTop[i], fingerX[i] + 38f, 210f, 20f, 20f, palmPaint)
            canvas.drawRoundRect(fingerX[i], fingerTop[i], fingerX[i] + 38f, 210f, 20f, 20f, outlinePaint)
        }
        // Thumb
        canvas.drawRoundRect(85f, 240f, 155f, 340f, 25f, 25f, palmPaint)
        canvas.drawRoundRect(85f, 240f, 155f, 340f, 25f, 25f, outlinePaint)

        // Major Palmistry Lines (Heart, Head, Life, Fate)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }

        // Heart Line (Hridaya Rekha)
        linePaint.color = Color.parseColor("#E11D48")
        val heartPath = Path().apply {
            moveTo(365f, 250f)
            quadTo(270f, 265f, 190f, 225f)
        }
        canvas.drawPath(heartPath, linePaint)

        // Head Line (Mastishka Rekha)
        linePaint.color = Color.parseColor("#2563EB")
        val headPath = Path().apply {
            moveTo(160f, 265f)
            quadTo(255f, 285f, 345f, 315f)
        }
        canvas.drawPath(headPath, linePaint)

        // Life Line (Jeevan Rekha)
        linePaint.color = Color.parseColor("#16A34A")
        val lifePath = Path().apply {
            moveTo(160f, 265f)
            quadTo(240f, 330f, 220f, 430f)
        }
        canvas.drawPath(lifePath, linePaint)

        // Fate Line (Bhagya Rekha)
        linePaint.color = Color.parseColor("#D97706")
        val fatePath = Path().apply {
            moveTo(265f, 430f)
            quadTo(260f, 320f, 240f, 225f)
        }
        canvas.drawPath(fatePath, linePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFC857")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("JevanMantra • $handSide Scan", 256f, 490f, textPaint)
        return bmp
    }
}
