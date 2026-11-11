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
import io.github.amirezafahimi.cardscanner.utils.enhanceText
import org.junit.Test

class TextEnhancerTest {

    @Test
    fun `maps letters that ocr confuses with digits`() {
        assertThat(enhanceText("6O37 99l2 34S6 7B93")).isEqualTo("6037 9912 3456 7893")
    }

    @Test
    fun `keeps slashes so the expiry date survives`() {
        assertThat(enhanceText("VALID THRU 05/08")).endsWith("05/08")
    }

    @Test
    fun `drops characters that cannot be part of a number`() {
        assertThat(enhanceText("# 1234 * 5678 %")).isEqualTo("1234 5678")
    }

    @Test
    fun `collapses whitespace and trims`() {
        assertThat(enhanceText("  1234    5678  ")).isEqualTo("1234 5678")
    }

    @Test
    fun `returns empty for empty input`() {
        assertThat(enhanceText("")).isEmpty()
    }
}
