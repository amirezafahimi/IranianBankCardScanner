# CardScanner

[![Maven Central](https://img.shields.io/maven-central/v/io.github.amirezafahimi/IranianBankCardScanner.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.amirezafahimi/IranianBankCardScanner)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://android-arsenal.com/api?level=23)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

A Jetpack Compose bank card scanner for Android. Point the camera at a card and get the card number
— and optionally the expiry date — back as a `ScannedCard`.

Built on **CameraX** and **ML Kit** on-device text recognition. Nothing leaves the device: no
network calls, no analytics, no card data stored anywhere.

Tuned for Iranian bank cards (Shetab IINs and Solar Hijri expiry dates out of the box), but the
issuer list is a constructor parameter, so it works for any 16-digit card.

---

## Why not just read the OCR output?

OCR on an embossed card is noisy. A frame can read `6O37` instead of `6037`, split the number into
groups ML Kit returns out of order, or pick up the 24-digit IBAN printed next to it. CardScanner
deals with that:

- **Character repair** — letters that OCR confuses with digits (`O`→`0`, `S`→`5`, `B`→`8`, …) are
  mapped back before anything is parsed.
- **Row reconstruction** — recognised lines are grouped by their vertical center and sorted left to
  right, so the four digit groups of the number end up adjacent in the text.
- **Luhn + IIN validation** — a candidate is only considered if its checksum is valid *and* it
  starts with a known issuer prefix. 24-digit IBANs and phone numbers are rejected.
- **Frame voting** — a number must be read identically in several frames before it is reported, with
  a timeout fallback so a worn card still scans.
- **Reversed-row retry** — cards read bottom-up are retried with their digit groups reversed.

---

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.github.amirezafahimi:cardscanner:1.0.0")
}
```

<details>
<summary>Groovy (<code>build.gradle</code>)</summary>

```groovy
dependencies {
    implementation 'io.github.amirezafahimi:cardscanner:1.0.0'
}
```

</details>

It is on Maven Central, so no extra repository is needed as long as `mavenCentral()` is in your
`settings.gradle.kts` — which it is in every project the Android Studio template generates.

### Version catalog

```toml
[versions]
cardscanner = "1.0.0"

[libraries]
cardscanner = { module = "io.github.amirezafahimi:cardscanner", version.ref = "cardscanner" }
```

### Requirements

| | |
|---|---|
| `minSdk` | 23 |
| `compileSdk` | 35 |
| Kotlin | 2.1.0+ |
| Compose BOM | 2024.12.01+ (Material 3) |
| Java | 17 |

The library brings CameraX, ML Kit text recognition and the Compose UI/Foundation/Material 3
artifacts it needs along as transitive dependencies — you do not have to declare them.

### Permission

`android.permission.CAMERA` is declared by the library's manifest and merged into your app, so you
do not need to add it. The runtime request is handled by the scanner itself.

---

## Usage

### The whole screen

```kotlin
@Composable
fun AddCardRoute(onCardScanned: (ScannedCard) -> Unit, onBack: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }

    if (isScanning) {
        CardScannerScreen(
            onCardDetected = { card ->
                isScanning = false
                onCardScanned(card)
            },
            onCloseClick = { isScanning = false }
        )
    } else {
        Button(onClick = { isScanning = true }) { Text("Scan card") }
    }
}
```

`CardScannerScreen` is self-contained. It asks for the camera permission, walks the user to app
settings if they previously chose "Don't ask again", binds the CameraX preview, runs the OCR, and
releases the camera, the torch and the ML Kit recognizer when it leaves composition.

`onCardDetected` is called on the main thread. The scanner keeps running until it is removed from
composition, so dismiss it from that callback.

### The result

```kotlin
data class ScannedCard(
    val number: String,      // 16 digits, no separators
    val expiryMonth: String?, // "05", or null when expiry scanning is off
    val expiryYear: String?   // "08", or null
)
```

### Configuration

```kotlin
CardScannerScreen(
    config = CardScannerConfig(
        iinList = listOf(603799, 621986),  // only accept these issuers
        scanExpiryDate = false,            // number only — faster
        requiredScore = 3,                 // more agreeing frames = more accurate, slower
        scanTimeoutMs = 3_000,             // report the best guess after this long
        analyzeIntervalMs = 500,           // min delay between OCR passes
        lostDetectionTimeoutMs = 1_000     // forget votes after the card leaves the frame
    ),
    onCardDetected = { /* ... */ },
    onCloseClick = { /* ... */ }
)
```

| Property | Default | Meaning |
|---|---|---|
| `iinList` | `defaultIinList` (Iranian banks) | Accepted card prefixes. Pass `emptyList()` to accept any Luhn-valid number. |
| `scanExpiryDate` | `true` | Read the expiry date along with the number. |
| `analyzeIntervalMs` | `500` | Minimum delay between two OCR passes. |
| `requiredScore` | `2` | Frames that must agree before a number is reported. |
| `scanTimeoutMs` | `2000` | After this long from the first detection, the best candidate wins anyway. |
| `lostDetectionTimeoutMs` | `1000` | Votes are discarded after the card disappears for this long. |

### Analytics

`onScannerOpened` fires once per open, before the camera starts:

```kotlin
CardScannerScreen(
    onScannerOpened = { analytics.log("card_scan_opened", mapOf("flow" to "card_to_card")) },
    onCardDetected = { /* ... */ },
    onCloseClick = { /* ... */ }
)
```

### A custom overlay

The default overlay is a dimmed cut-out with a close button, instructions and a torch toggle.
Replace it entirely with the `overlay` slot — you get the torch state and the two actions:

```kotlin
CardScannerScreen(
    onCardDetected = { /* ... */ },
    onCloseClick = { /* ... */ },
    overlay = { isTorchOn, onTorchToggle, onClose ->
        Box(Modifier.fillMaxSize()) {
            ScannerFrame(Modifier.fillMaxSize())          // keep the cut-out, drop the rest
            MyBrandedControls(isTorchOn, onTorchToggle, onClose)
        }
    }
)
```

`CardOverlay` and `ScannerFrame` are public, so you can reuse either piece. `CardOverlay` also takes
`title` and `subtitle` if you only want to change the copy.

### Your own camera setup

If you already have a CameraX pipeline, use the analyzer on its own and skip the bundled UI:

```kotlin
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

imageAnalysis.setAnalyzer(
    executor,
    CardAnalyzer(
        recognizer = recognizer,
        scope = lifecycleScope,
        config = CardScannerConfig(),
        onResult = { card -> /* ... */ }
    )
)

// recognizer is yours to close
```

`CardAnalyzer` keeps all of its vote state on `scope`'s dispatcher, so it is safe to hand to
CameraX's background executor. The low-level helpers — `extractCardNumber`, `extractExpiryDate`,
`enhanceText`, `isValidLuhn` — are public too, if you want to run them over text from somewhere
else.

---

## Localization

The library ships English (default) and Persian (`values-fa`) strings and is RTL-ready. Override any
string by redeclaring its name in your app's `strings.xml`:

```xml
<string name="card_scanner_overlay_title">Position your card in the frame</string>
```

| Resource | Default |
|---|---|
| `card_scanner_overlay_title` | Hold your card in front of the camera. |
| `card_scanner_overlay_subtitle` | Make sure the card number is clearly visible. |
| `card_scanner_camera_permission_title` | Camera permission |
| `card_scanner_camera_permission_message` | Please allow camera access to scan your card. |
| `card_scanner_camera_permission_settings_message` | Camera access was denied… |
| `card_scanner_go_to_settings` | Go to settings |
| `card_scanner_confirm` / `card_scanner_cancel` | Confirm / Cancel |

Theming follows the ambient `MaterialTheme`: the permission dialog uses your color scheme and
typography, no extra setup needed.

### Expiry date format

`extractExpiryDate` accepts both `YY/MM` (how Iranian cards print their Solar Hijri expiry) and
`MM/YY`, resolving the ambiguity by range. It returns the month and the year as two separate two-digit
strings, and rejects dates written in Persian or Arabic-Indic digits, since those come from the
printed Persian side of the card rather than the embossed date.

---

## R8 / ProGuard

Nothing to do: the library ships `consumer-rules.pro` with the keep rules ML Kit needs.

---

## Third-party licenses

CardScanner itself is Apache 2.0, and so are CameraX, Compose, AndroidX and the Kotlin
coroutines artifacts it pulls in.

One dependency is not: **`com.google.mlkit:text-recognition` is governed by the
[ML Kit Terms of Service](https://developers.google.com/ml-kit/terms)**, not by the Apache License.
Recognition runs entirely on the device — this library sends no image or card data anywhere and
stores none of it — but if your legal review tracks dependency terms, that is the one to flag.

The full list is in [NOTICE](NOTICE).

---

## Sample app

The `:sample` module is a one-screen app that opens the scanner and prints the result:

```bash
./gradlew :sample:installDebug
```

---

## Building from source

```bash
./gradlew :cardscanner:assembleRelease   # build the AAR
./gradlew :cardscanner:test              # unit tests for the extraction logic
./gradlew :cardscanner:publishToMavenLocal
```

## Releasing

Maven Central setup and the release checklist live in [PUBLISHING.md](PUBLISHING.md).

## Contributing

Issues and pull requests are welcome. Adding an issuer prefix to `defaultIinList` or a locale to
`values-*` is a particularly easy first contribution.

## License

```
Copyright 2026 Amirreza Fahimi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

First developed as an internal component of the Hafhashtad (780) Android application and published
with that project's permission. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
