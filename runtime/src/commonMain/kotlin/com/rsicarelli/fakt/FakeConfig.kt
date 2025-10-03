// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Additional configuration annotation for advanced fake behavior.
 *
 * This annotation provides fine-grained control over fake generation and behavior.
 * It can be applied to classes, functions, or properties to customize specific aspects
 * of fake generation beyond the basic @Fake annotation capabilities.
 *
 * ## Usage Examples
 * ```kotlin
 * @Fake
 * @FakeConfig(factoryName = "createTestUserService")
 * interface UserService {
 *     fun getUser(id: String): User
 * }
 *
 * // Generated factory function will be named `createTestUserService()`
 * val userService = createTestUserService { ... }
 * ```
 *
 * ```kotlin
 * @Fake
 * @FakeConfig(
 *     defaults = ["getUser=User('default', 'Default User')", "isActive=true"],
 *     excludeMethods = ["complexMethod"]
 * )
 * interface UserService {
 *     fun getUser(id: String): User
 *     fun isActive(): Boolean
 *     fun complexMethod(): ComplexResult // Will be excluded from fake
 * }
 * ```
 *
 * @param factoryName Custom fake factory name instead of generated default.
 * By default, generates `fakeServiceName()` for interface `ServiceName`.
 *
 * @param defaults Custom default values for method parameters.
 * Provides default return values when no configuration is specified.
 * Format: ["methodName=defaultValue", "otherMethod=otherDefault"]
 *
 * @param excludeMethods Exclude specific methods from fake generation.
 * Useful for excluding methods with complex signatures or behaviors.
 *
 * @param strictMode Enable strict validation of fake configuration.
 * When true, compilation fails if configuration methods don't match interface methods.
 *
 * @param generateDsl Generate DSL-style configuration instead of lambda-based.
 * When true, creates builder-style configuration methods.
 *
 * @param customScope Custom scope identifier for advanced scope management.
 * Overrides the scope parameter from @Fake annotation.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class FakeConfig(
    val factoryName: String = "",
    val defaults: Array<String> = [],
    val excludeMethods: Array<String> = [],
    val strictMode: Boolean = false,
    val generateDsl: Boolean = false,
    val customScope: String = "",
)
