// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.File

/** Route token for the task's `generatedKotlinDir`. */
internal const val GENERATED_ROUTE_TOKEN: String = "fakt://generated"

/** Route token for the task's `commonGeneratedKotlinDir`. */
internal const val COMMON_ROUTE_TOKEN: String = "fakt://common"

/**
 * Swaps the route tokens stored in the task's `@Input` JSON for real directories.
 *
 * The stored map holds tokens only (never absolute paths) so the input stays the same on every
 * machine. At execution time `fakt://generated` becomes [generated] and `fakt://common` becomes
 * [common].
 *
 * @throws IllegalStateException for an unknown token, or for `fakt://common` when [common] is null.
 */
internal fun resolveOutputRoutes(
    tokens: Map<String, String>,
    generated: File,
    common: File?,
): Map<String, String> =
    tokens.mapValues { (sourceSet, token) ->
        when (token) {
            GENERATED_ROUTE_TOKEN -> generated.absolutePath
            COMMON_ROUTE_TOKEN ->
                checkNotNull(common) {
                        "Source set '$sourceSet' routes to '$COMMON_ROUTE_TOKEN', but the task " +
                            "has no commonGeneratedKotlinDir: a task that emits common fakes " +
                            "needs a declared output for them."
                    }
                    .absolutePath
            else ->
                error(
                    "Source set '$sourceSet' has the unknown output route '$token'; expected " +
                        "'$GENERATED_ROUTE_TOKEN' or '$COMMON_ROUTE_TOKEN'."
                )
        }
    }
