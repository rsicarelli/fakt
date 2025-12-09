// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.builder

import com.rsicarelli.fakt.codegen.model.CodeBlock
import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeModifier
import com.rsicarelli.fakt.codegen.model.CodeProperty

/**
 * Builder for [CodeProperty] using type-safe DSL.
 *
 * Example:
 * ```kotlin
 * property("count", "Int") {
 *     private()
 *     mutable()
 *     initializer = "0"
 * }
 * ```
 *
 * @property name Property name
 * @property typeString Property type as string
 */
@CodeDsl
public class PropertyBuilder
    @PublishedApi
    internal constructor(
        private val name: String,
        private val typeString: String,
    ) {
        private val modifiers = mutableSetOf<CodeModifier>()

        /**
         * Property initializer expression.
         *
         * Example:
         * ```kotlin
         * initializer = "0"
         * initializer = "emptyList()"
         * initializer = "\"\""
         * ```
         */
        public var initializer: String? = null

        /**
         * Custom getter expression.
         *
         * Example:
         * ```kotlin
         * getter = "_count.value"
         * ```
         */
        public var getter: String? = null

        /**
         * Custom setter expression.
         *
         * Example:
         * ```kotlin
         * setter = "_count.value = value"
         * ```
         */
        public var setter: String? = null

        /**
         * Whether this is a mutable property (var vs val).
         */
        public var isMutable: Boolean = false

        /**
         * Makes property private.
         */
        public fun private() {
            modifiers.add(CodeModifier.PRIVATE)
        }

        /**
         * Makes property mutable (var).
         */
        public fun mutable() {
            isMutable = true
        }

        /**
         * Makes property override.
         */
        public fun override() {
            modifiers.add(CodeModifier.OVERRIDE)
        }

        /**
         * Makes property internal.
         */
        public fun internal() {
            modifiers.add(CodeModifier.INTERNAL)
        }

        /**
         * Builds the final [CodeProperty].
         *
         * @return Immutable [CodeProperty] instance
         */
        @PublishedApi
        internal fun build(): CodeProperty =
            CodeProperty(
                name = name,
                type = parseType(typeString),
                modifiers = modifiers,
                initializer = initializer?.let { CodeExpression.Raw(it) },
                // Getter needs block syntax for multi-line statements (contains newline or return)
                getter =
                    getter?.let {
                        if (it.contains("\n") || it.contains("return")) {
                            // Multi-line statements, needs block syntax: get() { ... }
                            CodeBlock.Statements(listOf(it))
                        } else {
                            // Simple expression, can use expression syntax: get() = ...
                            CodeBlock.Expression(CodeExpression.Raw(it))
                        }
                    },
                // Setter needs block syntax for multi-line statements or assignments
                setter =
                    setter?.let {
                        if (it.contains("\n") || it.contains("return") || it.contains("=")) {
                            // Multi-line or statement with assignment, needs block syntax: set(value) { ... }
                            CodeBlock.Statements(listOf(it))
                        } else {
                            // Pure expression, can use expression syntax: set(value) = ...
                            CodeBlock.Expression(CodeExpression.Raw(it))
                        }
                    },
                isMutable = isMutable,
            )
    }
