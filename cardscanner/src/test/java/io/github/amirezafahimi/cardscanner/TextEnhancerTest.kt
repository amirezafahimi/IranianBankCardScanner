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
