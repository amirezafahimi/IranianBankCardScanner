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

package io.github.amirezafahimi.cardscanner.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.amirezafahimi.cardscanner.model.CardScannerConfig
import io.github.amirezafahimi.cardscanner.model.ScannedCard
import io.github.amirezafahimi.cardscanner.ui.CardScannerScreen

/** Minimal host for [CardScannerScreen]: a button that opens the scanner and shows the result. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleApp()
                }
            }
        }
    }
}

@Composable
private fun SampleApp() {
    var isScanning by remember { mutableStateOf(false) }
    var scannedCard by remember { mutableStateOf<ScannedCard?>(null) }

    if (isScanning) {
        CardScannerScreen(
            config = CardScannerConfig(scanExpiryDate = true),
            onCardDetected = { card ->
                scannedCard = card
                isScanning = false
            },
            onCloseClick = { isScanning = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val card = scannedCard

        Text(
            text = if (card == null) {
                "No card scanned yet."
            } else {
                buildString {
                    append(card.number.chunked(4).joinToString(" "))
                    if (card.expiryMonth != null && card.expiryYear != null) {
                        append("\n")
                        append("${card.expiryMonth}/${card.expiryYear}")
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium
        )

        Button(onClick = { isScanning = true }) {
            Text(text = "Scan a card")
        }
    }
}
