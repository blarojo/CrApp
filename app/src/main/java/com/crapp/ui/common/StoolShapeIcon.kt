package com.crapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * A small illustrative shape for one point on the 7-point consistency scale (see
 * ConsistencySelector), from separate hard lumps (1) through a smooth log (4) to a
 * flat puddle (7) -- the same picture-plus-text idea as the human Bristol Stool
 * Chart, hand-drawn here rather than reusing Bristol's own (human-specific)
 * artwork/wording since the app deliberately tracks the dog-appropriate Purina
 * Fecal Scoring Chart instead (docs/development-plan.md §2).
 */
@Composable
fun StoolShapeIcon(
    score: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        when (score) {
            1 -> {
                val r = h * 0.3f
                listOf(0.22f, 0.5f, 0.78f).forEach { fx ->
                    drawCircle(tint, radius = r, center = Offset(w * fx, midY))
                }
            }
            2 -> {
                val r = h * 0.34f
                listOf(0.26f, 0.5f, 0.74f).forEach { fx ->
                    drawCircle(tint, radius = r, center = Offset(w * fx, midY))
                }
            }
            3 -> {
                val r = h * 0.4f
                listOf(0.3f, 0.44f, 0.58f, 0.72f).forEach { fx ->
                    drawCircle(tint, radius = r, center = Offset(w * fx, midY))
                }
            }
            4 -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.1f, h * 0.28f),
                    size = Size(w * 0.8f, h * 0.44f),
                    cornerRadius = CornerRadius(h * 0.22f)
                )
            }
            5 -> {
                drawOval(tint, topLeft = Offset(w * 0.12f, h * 0.2f), size = Size(w * 0.76f, h * 0.6f))
            }
            6 -> {
                drawOval(tint, topLeft = Offset(w * 0.04f, h * 0.32f), size = Size(w * 0.6f, h * 0.44f))
                drawOval(tint, topLeft = Offset(w * 0.4f, h * 0.28f), size = Size(w * 0.56f, h * 0.5f))
            }
            else -> {
                drawOval(tint, topLeft = Offset(0f, h * 0.58f), size = Size(w, h * 0.26f))
            }
        }
    }
}
