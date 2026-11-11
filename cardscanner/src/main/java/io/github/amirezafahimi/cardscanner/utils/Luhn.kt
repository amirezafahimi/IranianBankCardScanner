package io.github.amirezafahimi.cardscanner.utils

/**
 * Checks a card number against the Luhn checksum.
 *
 * @param cardNumber digits only; any other character makes this throw.
 */
fun isValidLuhn(cardNumber: String): Boolean {
    var sum = 0
    var alternate = false

    for (i in cardNumber.length - 1 downTo 0) {
        var n = cardNumber[i].digitToInt()

        if (alternate) {
            n *= 2
            if (n > 9) n -= 9
        }

        sum += n
        alternate = !alternate
    }

    return sum % 10 == 0
}
