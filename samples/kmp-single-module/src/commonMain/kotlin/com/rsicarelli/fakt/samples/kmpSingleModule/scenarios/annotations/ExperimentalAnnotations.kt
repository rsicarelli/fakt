// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

/**
 * Custom experimental API marker annotation.
 *
 * Used to test annotation propagation with @RequiresOptIn.
 * Generated fakes should propagate this annotation.
 */
@RequiresOptIn(
    message = "This API is experimental and may change without notice.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalFaktApi

/**
 * Another experimental marker for testing multiple OptIn requirements.
 */
@RequiresOptIn(
    message = "This API is in beta and subject to change.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class BetaFaktApi

/**
 * Custom annotation with string argument.
 *
 * Used to test string literal argument propagation.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class FeatureFlag(
    val name: String,
    val enabled: Boolean = true,
)

/**
 * Custom annotation with numeric arguments.
 *
 * Used to test numeric literal argument propagation.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Metrics(
    val priority: Int,
    val version: Double = 1.0,
    val maxRetries: Long = 3L,
)

/**
 * Custom annotation with array arguments.
 *
 * Used to test array argument propagation.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Tags(
    vararg val values: String,
)

/**
 * Custom annotation with enum argument.
 *
 * Used to test enum constant argument propagation.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class LogLevel(
    val level: Level = Level.INFO,
) {
    enum class Level {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
    }
}

/**
 * Annotation with class reference argument.
 *
 * Used to test KClass argument propagation.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Serializer(
    val with: kotlin.reflect.KClass<*>,
)

/**
 * Marker interface for serialization testing.
 */
interface JsonSerializer
