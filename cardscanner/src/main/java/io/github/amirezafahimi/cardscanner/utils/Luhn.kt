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
