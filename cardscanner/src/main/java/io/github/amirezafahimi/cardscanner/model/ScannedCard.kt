package io.github.amirezafahimi.cardscanner.model

/**
 * A card read from the camera.
 *
 * @property number the 16 digit card number, without separators.
 * @property expiryMonth two digit month, or `null` when the expiry date was not read.
 * @property expiryYear two digit year, or `null` when the expiry date was not read.
 */
data class ScannedCard(
    val number: String,
    val expiryMonth: String?,
    val expiryYear: String?
)
