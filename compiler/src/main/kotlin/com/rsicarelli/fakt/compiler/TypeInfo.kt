// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

/**
 * Represents metadata about a type discovered during compilation.
 *
 * This data class contains all the necessary information to track and generate
 * fake implementations for interfaces annotated with configured annotations.
 *
 * @property name The simple name of the type (e.g., "UserService")
 * @property fullyQualifiedName The fully qualified name (e.g., "com.example.UserService")
 * @property packageName The package name (e.g., "com.example")
 * @property fileName The source file name (e.g., "UserService.kt")
 * @property annotations List of annotation fully qualified names applied to this type
 * @property signature A unique signature for change detection and incremental compilation
 *
 * @since 1.0.0
 */
data class TypeInfo(
    val name: String,
    val fullyQualifiedName: String,
    val packageName: String,
    val fileName: String,
    val annotations: List<String>,
    val signature: String
)
