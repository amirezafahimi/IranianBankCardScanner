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

package io.github.amirezafahimi.cardscanner.model

/**
 * A card read from the camera.
 *
 * @property number the 16 digit card number, without separators.
 * @property expiryMonth two digit month, or `null` when the expiry date was not read.
 * @property expiryYear two digit year, or `null` when the expiry date was not read.
 */
data class ScannedCard(
    val number: String,
    val expiryMonth: String?,
    val expiryYear: String?
)
