// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.builder

import com.rsicarelli.fakt.codegen.model.CodeAnnotation
import com.rsicarelli.fakt.codegen.model.CodeDataClass
import com.rsicarelli.fakt.codegen.model.CodeModifier
import com.rsicarelli.fakt.codegen.model.DataClassProperty

/**
 * Builder for [CodeDataClass] using type-safe DSL.
 *
 * Example:
 * ```kotlin
 * dataClass("UserServiceGetUserCall") {
 *     public()
 *     property("id", "String")
 *     property("includeDeleted", "Boolean")
 * }
 * ```
 *
 * @property name The data class name
 */
@CodeDsl
public class DataClassBuilder @PublishedApi internal constructor(private val name: String) {
    private val properties = mutableListOf<DataClassProperty>()
    private val modifiers = mutableSetOf<CodeModifier>()
    private val annotations = mutableListOf<CodeAnnotation>()

    /**
     * Adds a constructor property to the data class.
     *
     * Example:
     * ```kotlin
     * property("id", "String")
     * property("count", "Int")
     * ```
     *
     * @param name Property name
     * @param type Property type as string
     */
    public fun property(name: String, type: String) {
        properties.add(DataClassProperty(name, type))
    }

    /** Makes data class public (explicit visibility for explicitApi() mode). */
    public fun public() {
        modifiers.add(CodeModifier.PUBLIC)
    }

    /** Makes data class internal. */
    public fun internal() {
        modifiers.add(CodeModifier.INTERNAL)
    }

    /**
     * Adds an annotation to the data class.
     *
     * Example:
     * ```kotlin
     * annotation("Serializable")
     * ```
     *
     * @param name Annotation simple name
     * @param arguments Pre-rendered argument strings
     */
    public fun annotation(name: String, vararg arguments: String) {
        annotation(name, arguments.toList())
    }

    /**
     * Adds an annotation with a list of arguments (avoids spread operator overhead).
     *
     * @param name Annotation simple name
     * @param arguments Pre-rendered argument strings
     */
    public fun annotation(name: String, arguments: List<String>) {
        annotations.add(CodeAnnotation(name, arguments))
    }

    /**
     * Builds the final [CodeDataClass].
     *
     * @return Immutable [CodeDataClass] instance
     */
    @PublishedApi
    internal fun build(): CodeDataClass =
        CodeDataClass(
            name = name,
            properties = properties.toList(),
            modifiers = modifiers.toSet(),
            annotations = annotations.toList(),
        )
}
