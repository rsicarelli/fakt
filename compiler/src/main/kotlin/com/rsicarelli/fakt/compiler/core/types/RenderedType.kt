// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

/**
 * Rendered Kotlin type string paired with the FQNs it references.
 *
 * Lets [com.rsicarelli.fakt.compiler.core.context.ImportResolver] union pre-collected FQN sets and
 * filter by package, instead of walking [org.jetbrains.kotlin.ir.types.IrType] trees from
 * codegen-runtime (which has no IR access).
 *
 * @property shortName Rendered Kotlin type string ready for code generation (e.g. `"List<User>?"`).
 * @property fqns Fully-qualified names for every concrete type referenced in [shortName]. Kotlin
 *   builtins and primitives are excluded — they never need an explicit import.
 */
data class RenderedType(val shortName: String, val fqns: Set<String>) {
    companion object {
        /** Convenience for primitive / built-in types that require no imports. */
        fun primitive(shortName: String): RenderedType = RenderedType(shortName, emptySet())
    }
}
