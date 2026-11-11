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
import io.github.amirezafahimi.cardscanner.utils.extractExpiryDate
import org.junit.Test

class ExpiryExtractorTest {

    @Test
    fun `reads a two digit solar year followed by a month`() {
        assertThat(extractExpiryDate("08/05")).isEqualTo("05" to "08")
    }

    @Test
    fun `reads a four digit solar year`() {
        assertThat(extractExpiryDate("1408/05")).isEqualTo("05" to "08")
    }

    @Test
    fun `reads a month followed by a year`() {
        assertThat(extractExpiryDate("05/28")).isEqualTo("05" to "28")
    }

    @Test
    fun `returns null for persian digits`() {
        assertThat(extractExpiryDate("۱۴۰۸/۰۵")).isNull()
    }

    @Test
    fun `returns null when there is no date`() {
        assertThat(extractExpiryDate("6037 9912 3456 7893")).isNull()
    }
}
