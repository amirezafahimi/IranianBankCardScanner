package io.github.amirezafahimi.cardscanner.utils

/**
 * Splits OCR text into the 4 digit groups a card number is printed in, discarding anything else.
 *
 * Tokens longer than 4 characters are truncated to 4, shorter ones and non-numeric ones are
 * dropped.
 */
internal fun filterAndNormalizeForGreedy(input: String): MutableList<String> {
    return input
        .split(" ")
        .mapNotNull { token ->
            when {
                token.length < 4 -> null
                token.length > 4 -> token.take(4)
                else -> token
            }
        }
        .mapNotNull { token ->
            if (!token.all { it.isDigit() }) {
                null
            } else {
                token
            }
        }
        .toMutableList()
}

/**
 * Returns every 16 digit candidate that can be formed by picking 4 of the groups returned by
 * [filterAndNormalizeForGreedy], in order.
 */
internal fun groupTo16Digits(input: String): MutableList<String> {
    val subs = filterAndNormalizeForGreedy(input)

    val result = mutableListOf<String>()

    fun backtrack(start: Int, current: MutableList<String>) {
        if (current.size == 4) {
            result.add(current.joinToString(""))
            return
        }

        for (i in start until subs.size) {
            current.add(subs[i])
            backtrack(i + 1, current)
            current.removeAt(current.size - 1)
        }
    }

    backtrack(0, mutableListOf())

    return result
}

private fun chunkReversed(num: String): String {
    return num.chunked(4)
        .reversed()
        .joinToString("")
}

/** True when [cardNumber] starts with one of [iinList], or when no IINs were configured. */
private fun isValidCardNumber(
    cardNumber: String,
    iinList: List<Int>
): Boolean {
    if (iinList.isEmpty()) return true

    return iinList.any {
        cardNumber.startsWith(it.toString())
    }
}

/**
 * Last-resort extraction: combines the 4 digit groups found anywhere in [raw] and returns the first
 * combination that is Luhn valid and starts with one of [iinList].
 *
 * Use it when [extractCardNumber] returns `null`, e.g. when OCR broke the number across the card.
 * Combinations are also retried with their groups reversed, because the camera sometimes reads the
 * rows of a card bottom-up.
 *
 * @param raw OCR text, ideally already passed through [enhanceText].
 * @param iinList accepted card prefixes; an empty list accepts any Luhn-valid number.
 */
fun extractCardNumberGreedy(
    raw: String,
    iinList: List<Int>
): String? {
    groupTo16Digits(raw).forEach { potentialNumber ->

        val digitNumber = potentialNumber.filter { it.isDigit() }

        if (digitNumber.length == 16) {
            if (isValidLuhn(digitNumber)) {
                if (isValidCardNumber(digitNumber, iinList)) {
                    return digitNumber
                }
            } else {
                val chunkReversed = chunkReversed(digitNumber)

                if (isValidLuhn(chunkReversed)) {
                    if (isValidCardNumber(chunkReversed, iinList)) {
                        return chunkReversed
                    }
                }
            }
        }
    }

    return null
}

/**
 * Extracts a card number from OCR text.
 *
 * Looks for 16 consecutive digits or four space separated groups of 4, ignoring longer digit runs
 * such as a 24 digit IBAN, and accepts the first candidate that is Luhn valid and starts with one
 * of [iinList]. Candidates are also retried with their 4 digit groups reversed, because the camera
 * sometimes reads the rows of a card bottom-up.
 *
 * @param raw OCR text, ideally already passed through [enhanceText].
 * @param iinList accepted card prefixes; an empty list accepts any Luhn-valid number.
 * @return the 16 digit number without separators, or `null` when nothing matched.
 */
fun extractCardNumber(
    raw: String,
    iinList: List<Int>
): String? {
    // 1. Clean the text but keep spaces/newlines to maintain separation between fields
    val cleanRaw = raw
        .replace("-", " ")
        .replace("\n", " ")

    // 2. Look for 16-digit sequences or 4 groups of 4 digits (common on cards)
    // (?<!\d) ensures no digit precedes the 16 digits
    // (?!\d) ensures no digit follows the 16 digits (filtering out 24-digit Sheba)
    val cardRegex = Regex("""(?<!\d)\d{16}(?!\d)|(\d{4}[ \s]\d{4}[ \s]\d{4}[ \s]\d{4})""")

    val matches = cardRegex.findAll(cleanRaw)

    for (match in matches) {
        val potentialNumber =
            match.value.replace(Regex("\\s"), "")

        if (potentialNumber.length == 16) {
            if (isValidCardNumber(potentialNumber, iinList)) {
                if (isValidLuhn(potentialNumber)) {
                    return potentialNumber
                }
            } else {
                val chunkReversed = chunkReversed(potentialNumber)

                if (isValidCardNumber(chunkReversed, iinList)) {
                    if (isValidLuhn(chunkReversed)) {
                        return chunkReversed
                    }
                }
            }
        }
    }

    val words = cleanRaw.split(Regex("\\s+"))

    for (word in words) {
        val digitsOnly = word.filter { it.isDigit() }

        if (digitsOnly.length == 16) {
            if (isValidCardNumber(digitsOnly, iinList)) {
                if (isValidLuhn(digitsOnly)) {
                    return digitsOnly
                }
            } else {
                val chunkReversed = chunkReversed(digitsOnly)

                if (isValidCardNumber(chunkReversed, iinList)) {
                    if (isValidLuhn(chunkReversed)) {
                        return chunkReversed
                    }
                }
            }
        }
    }

    return null
}
