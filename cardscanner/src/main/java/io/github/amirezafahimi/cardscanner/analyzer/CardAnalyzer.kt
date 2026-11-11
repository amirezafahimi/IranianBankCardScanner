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

package io.github.amirezafahimi.cardscanner.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import io.github.amirezafahimi.cardscanner.model.CardScannerConfig
import io.github.amirezafahimi.cardscanner.model.ScannedCard
import io.github.amirezafahimi.cardscanner.utils.enhanceText
import io.github.amirezafahimi.cardscanner.utils.extractCardNumber
import io.github.amirezafahimi.cardscanner.utils.extractCardNumberGreedy
import io.github.amirezafahimi.cardscanner.utils.extractExpiryDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

/**
 * CameraX [ImageAnalysis.Analyzer] that turns camera frames into a [ScannedCard].
 *
 * OCR is noisy, so a single frame is never trusted: every frame that yields a Luhn and IIN valid
 * number casts a vote, and a number is reported once it collects [CardScannerConfig.requiredScore]
 * votes — or once [CardScannerConfig.scanTimeoutMs] has passed since the first detection, whichever
 * comes first. Votes are discarded when no card is seen for
 * [CardScannerConfig.lostDetectionTimeoutMs], so pointing the camera at a different card does not
 * mix results.
 *
 * Use it directly against your own CameraX setup when you don't want the bundled
 * [io.github.amirezafahimi.cardscanner.ui.CardScannerScreen]:
 *
 * ```kotlin
 * imageAnalysis.setAnalyzer(
 *     executor,
 *     CardAnalyzer(
 *         recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
 *         scope = coroutineScope,
 *         onResult = { card -> /* ... */ }
 *     )
 * )
 * ```
 *
 * All vote state is read and written on [scope]'s dispatcher only, so the analyzer is safe to hand
 * to CameraX's background executor.
 *
 * @param recognizer the ML Kit recognizer used for OCR. The caller owns it and must close it.
 * @param scope the scope OCR runs in. Cancelling it stops analysis.
 * @param config scan tuning, see [CardScannerConfig].
 * @param onResult called on [scope]'s dispatcher with the first card that clears the vote
 *   threshold. It may fire again if frames keep arriving after a result.
 */
class CardAnalyzer(
    private val recognizer: TextRecognizer,
    private val scope: CoroutineScope,
    private val config: CardScannerConfig = CardScannerConfig(),
    private val onResult: (ScannedCard) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzed = 0L

    /** Vote counts per candidate card number. Only touched from [scope]. */
    private val cardScores = mutableMapOf<String, Int>()

    /** Vote counts per candidate (month, year) pair. Only touched from [scope]. */
    private val expiryScores = mutableMapOf<Pair<String, String>, Int>()

    /** When the first valid card was seen, or 0. Only touched from [scope]. */
    private var firstDetectionAt = 0L

    /** When the last valid card was seen, or 0. Only touched from [scope]. */
    private var lastDetectionAt = 0L

    @Volatile
    private var isProcessing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val now = System.currentTimeMillis()

        // Throttle: OCR on every frame would saturate the CPU without improving accuracy.
        if (now - lastAnalyzed < config.analyzeIntervalMs) {
            imageProxy.close()
            return
        }

        lastAnalyzed = now

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        isProcessing = true

        scope.launch {
            try {
                resetIfDetectionLost(now)

                val visionText = recognizer.process(inputImage).await()

                val result = withContext(Dispatchers.Default) { process(visionText) }

                val currentTime = System.currentTimeMillis()

                result.cardNumber?.let { card ->
                    cardScores[card] = (cardScores[card] ?: 0) + 1

                    if (firstDetectionAt == 0L) {
                        firstDetectionAt = currentTime
                    }

                    lastDetectionAt = currentTime
                }

                result.expiry?.let { expiry ->
                    expiryScores[expiry] = (expiryScores[expiry] ?: 0) + 1
                }

                val bestCard = cardScores.maxByValueOrNull()
                val bestExpiry = expiryScores.maxByValueOrNull()

                val enoughVotes = (bestCard?.value ?: 0) >= config.requiredScore
                val timeoutReached = firstDetectionAt != 0L &&
                    currentTime - firstDetectionAt >= config.scanTimeoutMs

                if (bestCard != null && (enoughVotes || timeoutReached)) {
                    val scannedCard = ScannedCard(
                        number = bestCard.key,
                        expiryMonth = bestExpiry?.key?.first,
                        expiryYear = bestExpiry?.key?.second
                    )

                    clearState()

                    onResult(scannedCard)
                }
            } catch (_: Exception) {
                // A failed frame is not worth reporting: the next one is one interval away.
            } finally {
                isProcessing = false
                imageProxy.close()
            }
        }
    }

    /**
     * Groups recognised lines into visual rows, flattens them in reading order, and extracts the
     * card number and expiry date.
     *
     * ML Kit returns blocks in an order that does not follow the card layout, so lines whose
     * bounding boxes share a vertical center are merged into one row and sorted left to right. That
     * keeps the four digit groups of a card number adjacent in the flattened text.
     */
    private fun process(visionText: Text): ProcessedResult {
        val rows = mutableListOf<MutableList<Text.Line>>()

        visionText.textBlocks
            .flatMap { it.lines }
            .sortedBy { it.boundingBox?.top }
            .forEach { line ->
                val lineBox = line.boundingBox

                val existingRow = rows.find { row ->
                    if (lineBox == null) return@find false

                    val threshold = max(MIN_ROW_THRESHOLD_PX, lineBox.height() * ROW_HEIGHT_RATIO)
                    val rowCenterY = row.first().boundingBox?.centerY() ?: 0

                    abs(rowCenterY - lineBox.centerY()) < threshold
                }

                if (existingRow != null) {
                    existingRow.add(line)
                } else {
                    rows.add(mutableListOf(line))
                }
            }

        val sortedText = rows
            .sortedBy { it.first().boundingBox?.top }
            .joinToString(" ") { row ->
                row.sortedBy { it.boundingBox?.left }.joinToString(" ") { it.text }
            }

        val rawText = enhanceText(sortedText)

        val cardNumber = extractCardNumber(rawText, config.iinList)
            ?: extractCardNumberGreedy(rawText, config.iinList)

        val expiry = if (config.scanExpiryDate) extractExpiryDate(rawText) else null

        return ProcessedResult(cardNumber = cardNumber, expiry = expiry)
    }

    /** Returns the entry with the highest value, preferring the latest one on a tie. */
    private fun <K, V : Comparable<V>> Map<K, V>.maxByValueOrNull(): Map.Entry<K, V>? =
        entries.fold<Map.Entry<K, V>, Map.Entry<K, V>?>(null) { acc, entry ->
            if (acc == null || entry.value >= acc.value) entry else acc
        }

    private fun resetIfDetectionLost(now: Long) {
        if (lastDetectionAt != 0L && now - lastDetectionAt > config.lostDetectionTimeoutMs) {
            clearState()
        }
    }

    private fun clearState() {
        cardScores.clear()
        expiryScores.clear()

        firstDetectionAt = 0L
        lastDetectionAt = 0L
    }

    private data class ProcessedResult(
        val cardNumber: String?,
        val expiry: Pair<String, String>?
    )

    private companion object {
        const val MIN_ROW_THRESHOLD_PX = 10f
        const val ROW_HEIGHT_RATIO = 0.5f
    }
}
