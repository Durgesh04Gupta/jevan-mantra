package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.domain.CalculatedKundaliChart

/**
 * Renders an authentic North-Indian Diamond Janam Kundali Chart (Lagna / Bhava Chalit Chart)
 * showing all 12 houses, Rashi numbers, and planetary positions.
 */
@Composable
fun NorthIndianKundaliChart(
    chart: CalculatedKundaliChart,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val planetTextColor = MaterialTheme.colorScheme.onSurface
    val houseNumColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

    val rashiPaint = remember(houseNumColor) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (houseNumColor.alpha * 255).toInt(),
                (houseNumColor.red * 255).toInt(),
                (houseNumColor.green * 255).toInt(),
                (houseNumColor.blue * 255).toInt()
            )
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val planetPaint = remember(planetTextColor) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (planetTextColor.alpha * 255).toInt(),
                (planetTextColor.red * 255).toInt(),
                (planetTextColor.green * 255).toInt(),
                (planetTextColor.blue * 255).toInt()
            )
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, lineColor, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 3.5f)

            drawRect(color = lineColor, style = stroke)
            drawLine(lineColor, Offset(0f, 0f), Offset(w, h), strokeWidth = 3f)
            drawLine(lineColor, Offset(w, 0f), Offset(0f, h), strokeWidth = 3f)

            val topMid = Offset(w / 2f, 0f)
            val rightMid = Offset(w, h / 2f)
            val bottomMid = Offset(w / 2f, h)
            val leftMid = Offset(0f, h / 2f)

            drawLine(lineColor, topMid, rightMid, strokeWidth = 3.5f)
            drawLine(lineColor, rightMid, bottomMid, strokeWidth = 3.5f)
            drawLine(lineColor, bottomMid, leftMid, strokeWidth = 3.5f)
            drawLine(lineColor, leftMid, topMid, strokeWidth = 3.5f)

            val houseCenters = mapOf(
                1 to Offset(w * 0.50f, h * 0.26f),
                2 to Offset(w * 0.25f, h * 0.12f),
                3 to Offset(w * 0.11f, h * 0.25f),
                4 to Offset(w * 0.26f, h * 0.50f),
                5 to Offset(w * 0.11f, h * 0.75f),
                6 to Offset(w * 0.25f, h * 0.88f),
                7 to Offset(w * 0.50f, h * 0.74f),
                8 to Offset(w * 0.75f, h * 0.88f),
                9 to Offset(w * 0.89f, h * 0.75f),
                10 to Offset(w * 0.74f, h * 0.50f),
                11 to Offset(w * 0.89f, h * 0.25f),
                12 to Offset(w * 0.75f, h * 0.12f)
            )

            rashiPaint.textSize = w * 0.036f
            planetPaint.textSize = w * 0.042f

            for (house in 1..12) {
                val center = houseCenters[house] ?: continue
                val rashiNum = ((chart.lagnaIndex + house - 2) % 12) + 1
                val planets = chart.housePlanets[house].orEmpty()
                val planetStr = if (planets.isEmpty()) "" else planets.joinToString(" ")

                drawContext.canvas.nativeCanvas.drawText(
                    "R$rashiNum",
                    center.x,
                    center.y - (w * 0.025f),
                    rashiPaint
                )
                if (planetStr.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        planetStr,
                        center.x,
                        center.y + (w * 0.032f),
                        planetPaint
                    )
                }
            }
        }
    }
}
