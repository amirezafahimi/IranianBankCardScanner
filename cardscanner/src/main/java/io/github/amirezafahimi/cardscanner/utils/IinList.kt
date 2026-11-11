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
 * Issuer Identification Numbers of Iranian banks, used as the default allow-list of
 * [io.github.amirezafahimi.cardscanner.model.CardScannerConfig].
 *
 * Entries are 6 digit prefixes, plus a few 8 digit ones where an issuer splits a 6 digit range.
 */
val defaultIinList = listOf(
    62198619,
    62198618,
    627648,
    207177,
    621986,
    636214,
    627381,
    502938,
    610433,
    991975,
    589463,
    589210,
    604932,
    603770,
    639217,
    627760,
    639370,
    639599,
    639591,
    622106,
    639194,
    627884,
    627412,
    502229,
    639347,
    502249,
    639607,
    639346,
    504706,
    502806,
    636949,
    505785,
    606256,
    605265,
    507677,
    636795,
    627961,
    628023,
    603799,
    170019,
    627353,
    585983,
    603769,
    903769,
    502908,
    627311,
    628157,
    627488,
    502910,
    606373,
    505416,
    504172,
    505801,
    585947,
    505809,
    581874
)
