// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

/**
 * Result of rendering an IR type to a Kotlin string, bundled with all fully-qualified names
 * referenced by that string.
 *
 * The side-channel [fqns] set is the foundation for the import-resolution rewrite in 3.1.d.5: once
 * every [IrPropertyMetadata] and [IrFunctionMetadata] carries a pre-rolled [RenderedType],
 * [ImportResolver] no longer needs to traverse [org.jetbrains.kotlin.ir.types.IrType] trees — it
 * can simply union the sets and filter by package.
 *
 * @property shortName Rendered Kotlin type string ready for code generation (e.g. `"List<User>?"`).
 * @property fqns Set of fully-qualified names for every concrete type referenced in [shortName]
 *   (e.g. `setOf("com.example.User")`). Kotlin builtins (`kotlin.*`, `kotlin.collections.*`) and
 *   primitives are excluded — they never need an explicit import.
 */
data class RenderedType(val shortName: String, val fqns: Set<String>) {
    companion object {
        /** Convenience for primitive / built-in types that require no imports. */
        fun primitive(shortName: String): RenderedType = RenderedType(shortName, emptySet())
    }
}
