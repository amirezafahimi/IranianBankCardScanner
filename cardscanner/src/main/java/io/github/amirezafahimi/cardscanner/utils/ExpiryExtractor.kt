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

package io.github.amirezafahimi.cardscanner.utils

/**
 * Finds an expiry date in OCR text.
 *
 * Accepts both `YY/MM` (common on Iranian cards, where the year is Solar Hijri) and `MM/YY`,
 * resolving the ambiguity by range: a part in `1..12` next to a part that cannot be a month is read
 * as the month. Text containing Persian or Arabic-Indic digits is rejected, because those come from
 * the printed Persian side of the card rather than from the embossed date.
 *
 * @return a (month, year) pair, both two digits, or `null` when no plausible date was found.
 */
fun extractExpiryDate(raw: String): Pair<String, String>? {
    // This regex looks for:
    // 1. A 2 or 4 digit year (e.g., 02 or 1402)
    // 2. A '/' separator
    // 3. A 2 digit month (01-12)
    // OR vice versa as some scanners might flip order

    if (containsPersianOrArabicDigits(raw)) {
        return null
    }

    val pattern = Regex("""(\d{2,4})/(\d{2})""")
    val matches = pattern.findAll(raw)

    for (match in matches) {
        val firstPart = match.groupValues[1]
        val secondPart = match.groupValues[2]

        val firstInt = firstPart.toIntOrNull() ?: continue
        val secondInt = secondPart.toIntOrNull() ?: continue

        // Iranian Month is always 01-12.
        // Iranian Year (Solar Hijri) is currently in the 1400s or 00s (e.g., 02, 03, 04, 05)

        // Case A: Year/Month (Most common on newer Iranian cards)
        // Check if secondPart is a valid month (1-12) and firstPart is a plausible year
        if (secondInt in 1..12 && (firstInt in 0..99 || firstInt in 1300..1500)) {
            val year = if (firstPart.length == 4) firstPart.takeLast(2) else firstPart
            return Pair(secondPart, year) // Return (Month, Year)
        }

        // Case B: Month/Year
        if (firstInt in 1..12 && (secondInt in 0..99)) {
            return Pair(firstPart, secondPart) // Return (Month, Year)
        }
    }

    return null
}

private fun containsPersianOrArabicDigits(text: String): Boolean {
    return Regex("[\\u06F0-\\u06F9\\u0660-\\u0669]")
        .containsMatchIn(text)
}
