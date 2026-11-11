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

package io.github.amirezafahimi.cardscanner

import com.google.common.truth.Truth.assertThat
import io.github.amirezafahimi.cardscanner.utils.defaultIinList
import io.github.amirezafahimi.cardscanner.utils.enhanceText
import io.github.amirezafahimi.cardscanner.utils.extractCardNumber
import io.github.amirezafahimi.cardscanner.utils.extractCardNumberGreedy
import io.github.amirezafahimi.cardscanner.utils.isValidLuhn
import org.junit.Test

class CardNumberExtractorTest {

    @Test
    fun `extracts grouped card number`() {
        val result = extractCardNumber("6037 9912 3456 7893", defaultIinList)

        assertThat(result).isEqualTo("6037991234567893")
    }

    @Test
    fun `extracts card number surrounded by other card text`() {
        val raw = enhanceText("VALID THRU 05/08  6219 8612 3456 7898  CARDHOLDER")

        assertThat(extractCardNumber(raw, defaultIinList)).isEqualTo("6219861234567898")
    }

    @Test
    fun `ignores numbers with an unknown iin`() {
        val result = extractCardNumber("4000 0012 3456 7899", defaultIinList)

        assertThat(result).isNull()
    }

    @Test
    fun `accepts any luhn valid number when no iin is configured`() {
        val result = extractCardNumber("4000 0012 3456 7899", emptyList())

        assertThat(result).isEqualTo("4000001234567899")
    }

    @Test
    fun `ignores a 24 digit iban-like run`() {
        val result = extractCardNumber("603799123456789360379912", defaultIinList)

        assertThat(result).isNull()
    }

    @Test
    fun `greedy extraction picks the valid combination out of extra groups`() {
        val result = extractCardNumberGreedy(
            "1234 6037 9912 3456 7893 9876",
            defaultIinList
        )

        assertThat(result).isEqualTo("6037991234567893")
    }

    @Test
    fun `luhn rejects a mistyped digit`() {
        assertThat(isValidLuhn("6037991234567893")).isTrue()
        assertThat(isValidLuhn("6037991234567894")).isFalse()
    }
}
