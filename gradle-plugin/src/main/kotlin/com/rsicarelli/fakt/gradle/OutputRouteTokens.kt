// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.worker.COMMON_ROUTE_TOKEN
import com.rsicarelli.fakt.gradle.worker.GENERATED_ROUTE_TOKEN

/**
 * The route tokens a task of [shape] stores in its placeholder context. The worker swaps them for
 * real directories at execution time, so the `@Input` JSON never carries a machine path.
 * - [TaskShape.PRODUCER]: empty, which means "emit everything analysed" (AGP built-in producers
 *   rely on this).
 * - [TaskShape.CONSUMER]: only the default source set, into `generatedKotlinDir`.
 * - [TaskShape.SINGLE_TARGET]: the default source set into `generatedKotlinDir`, every ancestor
 *   into `commonGeneratedKotlinDir`.
 */
internal fun outputRouteTokens(context: SourceSetContext, shape: TaskShape): Map<String, String> {
    val default = context.defaultSourceSet.name
    return when (shape) {
        TaskShape.PRODUCER -> emptyMap()
        TaskShape.CONSUMER -> mapOf(default to GENERATED_ROUTE_TOKEN)
        TaskShape.SINGLE_TARGET ->
            buildMap {
                put(default, GENERATED_ROUTE_TOKEN)
                context.allSourceSets
                    .map { it.name }
                    .filter { it != default }
                    .forEach { put(it, COMMON_ROUTE_TOKEN) }
            }
    }
}
