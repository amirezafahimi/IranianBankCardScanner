package io.github.amirezafahimi.cardscanner.utils

/**
 * Normalises OCR output before digits are extracted.
 *
 * Embossed card digits are frequently misread as letters, so letters that look like digits are
 * mapped back (`O`/`D`/`Q`/`U` to `0`, `S` to `5`, `B` to `8`, and so on), everything that is not a
 * digit, slash or space is dropped, and runs of whitespace are collapsed.
 *
 * This is lossy by design: it is only safe on text that is expected to be a card number or an
 * expiry date, never on a cardholder name.
 */
fun enhanceText(text: String): String {
    if (text.isEmpty()) return ""
    return text.uppercase()
        // Expanded replacements for worn-out embossed cards
        .replace("IR", "").replace("O", "0")
        .replace("D", "0").replace("Q", "0")
        .replace("U", "0").replace("I", "1")
        .replace("L", "1").replace("|", "1")
        .replace("!", "1").replace("Z", "2")
        .replace("S", "5").replace("$", "5")
        .replace("G", "6").replace("B", "8")
        .replace("A", "4").replace("H", "4")
        .replace("T", "7").replace("Y", "7")
        .replace("E", "3")
        .replace(Regex("[^0-9/\\s-]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
