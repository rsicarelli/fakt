// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNoJvm

/**
 * Deliberately ordinary `expect` declaration: from the commonMain producer's point of view it is
 * unpaired (the producer compiles commonMain sources only, never the platform `actual`s below).
 * Under the old K2JVM-driven producer this shape failed with `NO_ACTUAL_FOR_EXPECT`; the
 * `KotlinMetadataCompiler` producer has no IR actualizer, so it must compile fine (issue #79).
 */
expect fun currentPlatform(): String
