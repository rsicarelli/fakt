// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Meta-annotation that marks annotations as triggers for fake generation.
 *
 * This meta-annotation enables companies to define their own annotations for fake generation
 * without being locked into the built-in `@Fake` annotation. The compiler will detect any
 * annotation marked with `@GeneratesFake` and generate fake implementations for interfaces
 * or classes annotated with it.
 *
 * ## Pattern Inspiration
 * This pattern is inspired by Kotlin's `@HidesFromObjC` meta-annotation, which marks
 * annotations that should trigger specific compiler behavior.
 *
 * ## Usage Example
 *
 * ### Defining a Custom Annotation
 * ```kotlin
 * @GeneratesFake
 * @Target(AnnotationTarget.CLASS)
 * @Retention(AnnotationRetention.BINARY)
 * annotation class TestDouble
 * ```
 *
 * ### Using the Custom Annotation
 * ```kotlin
 * @TestDouble
 * interface UserService {
 *     suspend fun getUser(id: String): User
 * }
 *
 * // The compiler will generate:
 * // - FakeUserServiceImpl class
 * // - fakeUserService() factory function
 * // - FakeUserServiceConfig DSL
 * ```
 *
 * ## Benefits for Companies
 *
 * 1. **Ownership**: Define your own annotation instead of depending on Fakt's `@Fake`
 * 2. **Migration Safety**: Breaking changes in Fakt won't affect your annotation
 * 3. **Naming Control**: Use company-specific naming conventions (e.g., `@TestDouble`, `@MockService`)
 * 4. **Minimal Runtime Dependency**: Only the annotation definition requires Fakt runtime; generated code has no runtime dependency
 *
 * ## Backward Compatibility
 *
 * The built-in `@Fake` annotation is annotated with `@GeneratesFake`, so existing code
 * continues to work without any changes. Companies can migrate to custom annotations
 * incrementally without breaking existing tests.
 *
 * @since 1.0.0
 * @see Fake
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class GeneratesFake
