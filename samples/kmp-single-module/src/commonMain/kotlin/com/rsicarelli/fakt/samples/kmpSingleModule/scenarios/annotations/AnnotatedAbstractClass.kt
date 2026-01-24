// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Test case: Annotated abstract class.
 *
 * The generated FakeAnnotatedRepositoryImpl should include:
 * ```kotlin
 * @OptIn(ExperimentalFaktApi::class)
 * @Deprecated("Use AnnotatedRepositoryV2 instead", level = DeprecationLevel.WARNING)
 * class FakeAnnotatedRepositoryImpl : AnnotatedRepository() { ... }
 * ```
 *
 * Tests:
 * - Annotation propagation for abstract classes
 * - Multiple annotations on abstract class
 * - Abstract methods inherit class-level annotations context
 */
@Fake
@OptIn(ExperimentalFaktApi::class)
@Deprecated("Use AnnotatedRepositoryV2 instead", level = DeprecationLevel.WARNING)
abstract class AnnotatedRepository {
    abstract fun findById(id: String): String?
    abstract suspend fun saveAll(items: List<String>): Int

    // Non-abstract method should NOT be overridden
    open fun getVersion(): String = "1.0"
}

/**
 * Test case: Annotated open class.
 *
 * The generated FakeAnnotatedConfigImpl should include:
 * ```kotlin
 * @FeatureFlag(name = "config-v2", enabled = true)
 * @Metrics(priority = 1, version = 1.5)
 * class FakeAnnotatedConfigImpl : AnnotatedConfig() { ... }
 * ```
 *
 * Tests:
 * - Annotation propagation for open classes
 * - Custom annotations on classes
 * - Numeric argument types
 */
@Fake
@FeatureFlag(name = "config-v2", enabled = true)
@Metrics(priority = 1, version = 1.5)
open class AnnotatedConfig {
    open val configName: String = "default"
    open fun getConfigValue(key: String): String? = null
}

/**
 * Test case: Abstract class with experimental annotation and suppress.
 *
 * Tests complex annotation combinations on abstract classes.
 */
@Fake
@OptIn(ExperimentalFaktApi::class, BetaFaktApi::class)
@Suppress("UNCHECKED_CAST")
@Tags("data-layer", "repository", "experimental")
abstract class AnnotatedDataSource {
    abstract fun <T> query(sql: String): List<T>
    abstract suspend fun <T> execute(command: String, params: Map<String, Any>): T
}
