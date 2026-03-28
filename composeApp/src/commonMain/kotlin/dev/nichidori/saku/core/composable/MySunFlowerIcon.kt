package dev.nichidori.saku.core.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MySunFlowerIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val rayCount = 11

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val center = Offset(size.width / 2, size.height / 2)
        val circleRadius = size.width * 0.12f
        val circlePadding = size.width * 0.08f

        for (i in 0 until rayCount) {
            val jitter = when {
                i % 5 == 0 -> 2f
                i % 3 == 0 -> -2f
                i % 2 == 0 -> 0f
                else -> 1f
            }

            val baseAngle = (i * 360f / rayCount)
            val finalAngle = toRadians((baseAngle + jitter).toDouble())

            val lengthFactor = when {
                i % 3 == 0 -> 0.16f
                i % 2 == 0 -> 0.12f
                else -> 0.04f
            }

            val widthFactor = when {
                i % 5 == 0 -> 0.056f
                i % 2 == 0 -> 0.048f
                else -> 0.052f
            }

            val rayLength = size.width * lengthFactor
            val rayWidth = size.width * widthFactor

            val startDistance = circleRadius + circlePadding
            val startX = center.x + cos(finalAngle).toFloat() * startDistance
            val startY = center.y + sin(finalAngle).toFloat() * startDistance

            val endDistance = startDistance + rayLength
            val endX = center.x + cos(finalAngle).toFloat() * endDistance
            val endY = center.y + sin(finalAngle).toFloat() * endDistance

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = rayWidth,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(
            color = color,
            radius = circleRadius,
            center = center,
        )
    }
}