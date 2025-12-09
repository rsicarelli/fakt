// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.builder

import com.rsicarelli.fakt.codegen.model.CodeBlock
import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeFunction
import com.rsicarelli.fakt.codegen.model.CodeModifier
import com.rsicarelli.fakt.codegen.model.CodeParameter
import com.rsicarelli.fakt.codegen.model.CodeType
import com.rsicarelli.fakt.codegen.model.CodeTypeParameter

/**
 * Builder for [CodeFunction] using type-safe DSL.
 *
 * Example:
 * ```kotlin
 * function("getUser") {
 *     override()
 *     suspend()
 *     parameter("id", "String")
 *     returns("User?")
 *     body = "return null"
 * }
 * ```
 *
 * @property name The function name
 */
@CodeDsl
public class FunctionBuilder
    @PublishedApi
    internal constructor(
        private val name: String,
    ) {
        private val parameters = mutableListOf<CodeParameter>()
        private val typeParameters = mutableListOf<CodeTypeParameter>()
        private val modifiers = mutableSetOf<CodeModifier>()
        private var returnType: CodeType = CodeType.Simple("Unit")
        private var bodyBlock: CodeBlock = CodeBlock.Empty
        private var receiverType: CodeType? = null

        /**
         * Whether this is a suspend function.
         */
        public var isSuspend: Boolean = false

        /**
         * Whether this is an inline function.
         */
        public var isInline: Boolean = false

        /**
         * Sets function body as raw code string.
         *
         * Example:
         * ```kotlin
         * body = "return 42"
         * ```
         */
        public var body: String
            get() = error("Write-only property")
            set(value) {
                bodyBlock = CodeBlock.Statements(listOf(value))
            }

        /**
         * Sets function body using a lambda.
         *
         * Example:
         * ```kotlin
         * body {
         *     buildString {
         *         appendLine("val x = 1")
         *         appendLine("return x")
         *     }
         * }
         * ```
         *
         * @param block Lambda that returns the body code
         */
        public fun body(block: () -> String) {
            bodyBlock = CodeBlock.Statements(listOf(block()))
        }

        /**
         * Adds parameter.
         *
         * Example:
         * ```kotlin
         * parameter("id", "String")
         * parameter("count", "Int", defaultValue = "0")
         * parameter("items", "String", vararg = true)
         * ```
         *
         * @param name Parameter name
         * @param type Parameter type as string
         * @param defaultValue Optional default value expression
         * @param vararg Whether this is a vararg parameter
         */
        public fun parameter(
            name: String,
            type: String,
            defaultValue: String? = null,
            vararg: Boolean = false,
        ) {
            parameters.add(
                CodeParameter(
                    name = name,
                    type = parseType(type),
                    defaultValue = defaultValue?.let { CodeExpression.Raw(it) },
                    isVararg = vararg,
                ),
            )
        }

        /**
         * Adds a type parameter to the function.
         *
         * Example:
         * ```kotlin
         * typeParam("T")  // fun <T> method()
         * typeParam("R", "Comparable<R>")  // fun <R : Comparable<R>> method()
         * ```
         *
         * @param name Type parameter name
         * @param constraints Optional type constraints
         */
        public fun typeParam(
            name: String,
            vararg constraints: String,
        ) {
            typeParameters.add(
                CodeTypeParameter(
                    name = name,
                    constraints = constraints.toList(),
                    isReified = false,
                ),
            )
        }

        /**
         * Sets return type.
         *
         * Example:
         * ```kotlin
         * returns("User?")
         * returns("List<String>")
         * ```
         *
         * @param type Return type as string
         */
        public fun returns(type: String) {
            returnType = parseType(type)
        }

        /**
         * Makes function override.
         */
        public fun override() {
            modifiers.add(CodeModifier.OVERRIDE)
        }

        /**
         * Makes function internal.
         */
        public fun internal() {
            modifiers.add(CodeModifier.INTERNAL)
        }

        /**
         * Makes function suspend.
         */
        public fun suspend() {
            isSuspend = true
        }

        /**
         * Makes function operator.
         */
        public fun operator() {
            modifiers.add(CodeModifier.OPERATOR)
        }

        /**
         * Sets extension receiver type for extension functions.
         *
         * Example:
         * ```kotlin
         * receiver("Vector")  // fun Vector.plus()
         * receiver("List<T>")  // fun List<T>.sum()
         * ```
         *
         * @param type Receiver type as string
         */
        public fun receiver(type: String) {
            receiverType = parseType(type)
        }

        /**
         * Builds the final [CodeFunction].
         *
         * @return Immutable [CodeFunction] instance
         */
        @PublishedApi
        internal fun build(): CodeFunction =
            CodeFunction(
                name = name,
                parameters = parameters,
                typeParameters = typeParameters,
                returnType = returnType,
                body = bodyBlock,
                modifiers = modifiers,
                isSuspend = isSuspend,
                isInline = isInline,
                receiverType = receiverType,
            )
    }
