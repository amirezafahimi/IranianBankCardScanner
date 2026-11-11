/*
 * Copyright 2026 Amirreza Fahimi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.amirezafahimi.cardscanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the card cut-out: a dimmed overlay with a rounded rectangle hole in the middle and a corner
 * bracket at each of its corners.
 *
 * The hole is 80% of the available width, at the 0.88 aspect ratio of a bank card.
 *
 * @param cornerColor color of the four corner brackets.
 * @param cornerStroke thickness of the corner brackets.
 * @param cornerLength how far each bracket runs along the edges of the hole.
 * @param overlayColor color painted over everything outside the hole.
 */
@Composable
fun ScannerFrame(
    modifier: Modifier = Modifier,
    cornerColor: Color = Color.White,
    cornerStroke: Dp = 4.dp,
    cornerLength: Dp = 24.dp,
    overlayColor: Color = Color.Black.copy(alpha = 0.3f) // Darkened background
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .focusable(false)
    ) {
        val stroke = cornerStroke.toPx()
        val length = cornerLength.toPx()
        val cornerRadius = 8.dp.toPx()

        // This is the rectangle for the card (the hole)
        // Calculated based on the aspect ratio provided in the modifier
        val rectWidth = size.width * 0.80f
        val rectHeight = rectWidth / 0.88f

        val left = (size.width - rectWidth) / 2
        val top = (size.height - rectHeight) / 2
        val right = left + rectWidth
        val bottom = top + rectHeight

        val holeRect = RoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // 1. Draw the "Hole" Effect
        // We clip the path so that we draw the overlay EVERYWHERE EXCEPT the hole
        val path = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRoundRect(holeRect)
            fillType = PathFillType.EvenOdd
        }
        drawPath(path, color = overlayColor)

        // 2. Draw the White Corners
        // Top Left
        drawPath(
            path = Path().apply {
                moveTo(left, top + length)
                lineTo(left, top + cornerRadius)
                arcTo(
                    rect = Rect(left, top, left + cornerRadius * 2, top + cornerRadius * 2),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + length, top)
            },
            color = cornerColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Top Right
        drawPath(
            path = Path().apply {
                moveTo(right - length, top)
                lineTo(right - cornerRadius, top)
                arcTo(
                    rect = Rect(right - cornerRadius * 2, top, right, top + cornerRadius * 2),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(right, top + length)
            },
            color = cornerColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Bottom Left
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - length)
                lineTo(left, bottom - cornerRadius)
                arcTo(
                    rect = Rect(left, bottom - cornerRadius * 2, left + cornerRadius * 2, bottom),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(left + length, bottom)
            },
            color = cornerColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Bottom Right
        drawPath(
            path = Path().apply {
                moveTo(right - length, bottom)
                lineTo(right - cornerRadius, bottom)
                arcTo(
                    rect = Rect(right - cornerRadius * 2, bottom - cornerRadius * 2, right, bottom),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(right, bottom - length)
            },
            color = cornerColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
