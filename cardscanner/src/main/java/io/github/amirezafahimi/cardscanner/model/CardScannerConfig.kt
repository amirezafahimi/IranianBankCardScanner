package io.github.amirezafahimi.cardscanner.model

import io.github.amirezafahimi.cardscanner.utils.defaultIinList

/**
 * Tuning knobs for a card scan session.
 *
 * All values have sensible defaults; override only what you need:
 *
 * ```kotlin
 * CardScannerConfig(
 *     iinList = listOf(603799, 621986),
 *     scanExpiryDate = false
 * )
 * ```
 *
 * @property iinList Issuer Identification Numbers (card prefixes) accepted by the analyzer.
 *   A candidate number is only reported if it starts with one of these. Pass an empty list to
 *   accept any Luhn-valid 16 digit number.
 * @property scanExpiryDate whether the expiry date is read along with the card number.
 * @property analyzeIntervalMs minimum delay between two OCR passes. Lower values burn more CPU.
 * @property requiredScore how many frames must agree on the same card number before it is
 *   reported. Higher values trade speed for accuracy.
 * @property scanTimeoutMs after this long from the first detection, the best candidate so far is
 *   reported even if [requiredScore] was never reached.
 * @property lostDetectionTimeoutMs if no card is detected for this long, collected votes are
 *   discarded and scanning starts over.
 */
data class CardScannerConfig(
    val iinList: List<Int> = defaultIinList,
    val scanExpiryDate: Boolean = true,
    val analyzeIntervalMs: Long = 500L,
    val requiredScore: Int = 2,
    val scanTimeoutMs: Long = 2_000L,
    val lostDetectionTimeoutMs: Long = 1_000L
)
