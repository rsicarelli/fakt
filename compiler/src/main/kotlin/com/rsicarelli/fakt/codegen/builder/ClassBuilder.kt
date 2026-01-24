// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.builder

import com.rsicarelli.fakt.codegen.model.CodeAnnotation
import com.rsicarelli.fakt.codegen.model.CodeClass
import com.rsicarelli.fakt.codegen.model.CodeComment
import com.rsicarelli.fakt.codegen.model.CodeMember
import com.rsicarelli.fakt.codegen.model.CodeModifier
import com.rsicarelli.fakt.codegen.model.CodeRegionEnd
import com.rsicarelli.fakt.codegen.model.CodeRegionStart
import com.rsicarelli.fakt.codegen.model.CodeType
import com.rsicarelli.fakt.codegen.model.CodeTypeParameter

/**
 * Builder for [CodeClass] using type-safe DSL.
 *
 * Example:
 * ```kotlin
 * klass("FakeUserService") {
 *     implements("UserService")
 *     typeParam("T", "Comparable<T>")
 *     property("value", "String") { ... }
 *     function("test") { ... }
 * }
 * ```
 *
 * @property name The class name
 */
@CodeDsl
public class ClassBuilder
    @PublishedApi
    internal constructor(
        private val name: String,
    ) {
        @PublishedApi
        internal val typeParameters = mutableListOf<CodeTypeParameter>()

        @PublishedApi
        internal val superTypes = mutableListOf<CodeType>()

        @PublishedApi
        internal val modifiers = mutableSetOf<CodeModifier>()

        @PublishedApi
        internal val members = mutableListOf<CodeMember>()

        @PublishedApi
        internal var whereClause: String? = null

        @PublishedApi
        internal val annotations = mutableListOf<CodeAnnotation>()

        /**
         * Add annotation to the class.
         *
         * Example:
         * ```kotlin
         * annotation("OptIn", "ExperimentalApi::class")
         * annotation("Deprecated", "\"use NewService\"", "level = DeprecationLevel.WARNING")
         * ```
         *
         * @param name Annotation simple name (e.g., "OptIn", "Deprecated")
         * @param arguments Pre-rendered argument strings
         */
        public fun annotation(
            name: String,
            vararg arguments: String,
        ) {
            annotations.add(CodeAnnotation(name, arguments.toList()))
        }

        /**
         * Add type parameter: typeParam("T") or typeParam("T", "Comparable<T>")
         *
         * TODO Phase 10: Will be used for generic interfaces like Repository<T>.
         *
         * @param name Type parameter name
         * @param constraints Type constraints (e.g., "Comparable<T>")
         * @param reified Whether this is a reified type parameter
         */
        public fun typeParam(
            name: String,
            vararg constraints: String,
            reified: Boolean = false,
        ) {
            typeParameters.add(
                CodeTypeParameter(
                    name = name,
                    constraints = constraints.toList(),
                    isReified = reified,
                ),
            )
        }

        /**
         * Implements interface or extends class.
         *
         * Example:
         * ```kotlin
         * implements("UserService")
         * implements("Repository", "User")  // Repository<User>
         * ```
         *
         * @param typeName Interface or class name
         * @param typeArgs Optional generic type arguments
         */
        public fun implements(
            typeName: String,
            vararg typeArgs: String,
        ) {
            superTypes.add(
                if (typeArgs.isEmpty()) {
                    CodeType.Simple(typeName)
                } else {
                    CodeType.Generic(
                        typeName,
                        typeArgs.map { CodeType.Simple(it) },
                    )
                },
            )
        }

        /**
         * Adds where clause for multiple constraints.
         *
         * TODO Phase 10: Will be used for complex generic constraints like where T : Comparable<T>.
         *
         * @param clause The where clause (e.g., "T : Comparable<T>, T : Serializable")
         */
        public fun where(clause: String) {
            whereClause = clause
        }

        /**
         * Makes class public (explicit visibility for explicitApi() mode).
         */
        public fun public() {
            modifiers.add(CodeModifier.PUBLIC)
        }

        /**
         * Makes class internal.
         */
        public fun internal() {
            modifiers.add(CodeModifier.INTERNAL)
        }

        /**
         * Adds a comment to the class body.
         *
         * Example:
         * ```kotlin
         * comment("Section: Properties")
         * ```
         *
         * @param text The comment text (without // prefix)
         */
        public fun comment(text: String) {
            members.add(CodeComment(text))
        }

        /**
         * Starts a collapsible region in the class body.
         *
         * Regions are IDE-collapsible sections (IntelliJ, Android Studio).
         * Must be paired with [endRegion].
         *
         * Example:
         * ```kotlin
         * region("Private State")
         * // ... properties ...
         * endRegion()
         * ```
         *
         * @param name The region name displayed when collapsed
         */
        public fun region(name: String) {
            members.add(CodeRegionStart(name))
        }

        /**
         * Ends a collapsible region in the class body.
         *
         * Must be paired with a preceding [region] call.
         */
        public fun endRegion() {
            members.add(CodeRegionEnd)
        }

        /**
         * Adds a property member.
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
         * @param name Property name
         * @param type Property type as string
         * @param block DSL block for configuring the property
         */
        public inline fun property(
            name: String,
            type: String,
            block: PropertyBuilder.() -> Unit = {},
        ) {
            members.add(PropertyBuilder(name, type).apply(block).build())
        }

        /**
         * Adds a function member.
         *
         * Example:
         * ```kotlin
         * function("getUser") {
         *     override()
         *     parameter("id", "String")
         *     returns("User?")
         *     body = "return null"
         * }
         * ```
         *
         * @param name Function name
         * @param block DSL block for configuring the function
         */
        public inline fun function(
            name: String,
            block: FunctionBuilder.() -> Unit,
        ) {
            members.add(FunctionBuilder(name).apply(block).build())
        }

        /**
         * Builds the final [CodeClass].
         *
         * @return Immutable [CodeClass] instance
         */
        @PublishedApi
        internal fun build(): CodeClass =
            CodeClass(
                name = name,
                typeParameters = typeParameters,
                superTypes = superTypes,
                modifiers = modifiers,
                members = members,
                whereClause = whereClause,
                annotations = annotations,
            )
    }
