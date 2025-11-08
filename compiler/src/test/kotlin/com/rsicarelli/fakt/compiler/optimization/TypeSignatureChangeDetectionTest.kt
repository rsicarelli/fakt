// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.types.TypeInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for comprehensive signature-based change detection in CompilerOptimizations.
 *
 * **Goal**: Verify that the MD5-based signature system detects ALL structural changes:
 * - Parameter name changes
 * - Parameter type changes
 * - Return type changes
 * - Nullability changes
 * - Modifier changes (suspend, inline)
 * - Type parameter changes
 *
 * **Note on Signature Format**:
 * These tests use detailed structural signatures for readability and explicitness:
 * - Test format: `"interface pkg.Name|functions:1|method(param:Type):ReturnType"`
 * - Production format: MD5 file hash (32-character hex: `"a3f8b7c9d1e2f5a6..."`)
 *
 * Production code uses MD5 file hashing via `SignatureBuilder.buildSignature()`.
 * Since MD5 hashes the entire source file, ANY change (including parameter names,
 * types, comments, formatting) triggers regeneration. This test verifies the cache
 * system's ability to distinguish different signatures (format-agnostic).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeSignatureChangeDetectionTest {
    @Test
    fun `GIVEN interface with parameter name change WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: Interface generated with parameter "id"
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.UserService|
                functions:1|getUser(id:kotlin.String):com.example.User
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            // Record generation with original signature
            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Interface modified - parameter name changed from "id" to "userId"
            val modifiedSignature =
                """
                interface com.example.UserService|
                functions:1|getUser(userId:kotlin.String):com.example.User
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration (parameter name changed)
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Parameter name change should invalidate cache (id -> userId)",
            )
            assertNotEquals(originalSignature, modifiedSignature, "Signatures should be different")
        }

    @Test
    fun `GIVEN interface with parameter type change WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: Interface with String parameter
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.UserService|
                functions:1|getUser(id:kotlin.String):com.example.User
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Parameter type changed from String to Int
            val modifiedSignature =
                """
                interface com.example.UserService|
                functions:1|getUser(id:kotlin.Int):com.example.User
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Parameter type change should invalidate cache (String -> Int)",
            )
        }

    @Test
    fun `GIVEN interface with return type change WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: Interface returning User
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.UserService|
                functions:1|getUser(id:kotlin.String):com.example.User
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Return type changed from User to UserDto
            val modifiedSignature =
                """
                interface com.example.UserService|
                functions:1|getUser(id:kotlin.String):com.example.UserDto
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Return type change should invalidate cache (User -> UserDto)",
            )
        }

    @Test
    fun `GIVEN interface with nullability change WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: Interface with non-nullable String
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.UserService|
                properties:1|name:kotlin.String:val:nonNull
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Property changed to nullable
            val modifiedSignature =
                """
                interface com.example.UserService|
                properties:1|name:kotlin.String?:val:nullable
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Nullability change should invalidate cache (String -> String?)",
            )
        }

    @Test
    fun `GIVEN interface with modifier change WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: Regular function
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.UserService|
                functions:1|loadUser():com.example.User
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Function becomes suspend
            val modifiedSignature =
                """
                interface com.example.UserService|
                functions:1|loadUser():com.example.User:suspend
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Modifier change should invalidate cache (regular -> suspend)",
            )
        }

    @Test
    fun `GIVEN interface with property mutability change WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: val property
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.UserService|
                properties:1|name:kotlin.String:val:nonNull
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Property changed to var
            val modifiedSignature =
                """
                interface com.example.UserService|
                properties:1|name:kotlin.String:var:nonNull
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Mutability change should invalidate cache (val -> var)",
            )
        }

    @Test
    fun `GIVEN interface with added type parameter WHEN checking regeneration THEN should detect change`() =
        runTest {
            // GIVEN: Interface without generics
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val originalSignature =
                """
                interface com.example.Repository|
                functions:1|save(item:com.example.User):kotlin.Unit
                """.trimIndent()
            val originalTypeInfo =
                TypeInfo(
                    name = "Repository",
                    fullyQualifiedName = "com.example.Repository",
                    packageName = "com.example",
                    fileName = "Repository.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = originalSignature,
                )

            optimizations.recordGeneration(originalTypeInfo)

            // WHEN: Interface becomes generic Repository<T>
            val modifiedSignature =
                """
                interface com.example.Repository|
                typeParams:<T:kotlin.Any?>|
                functions:1|save(item:T):kotlin.Unit
                """.trimIndent()
            val modifiedTypeInfo = originalTypeInfo.copy(signature = modifiedSignature)

            // THEN: Should require regeneration
            assertTrue(
                optimizations.needsRegeneration(modifiedTypeInfo),
                "Type parameter addition should invalidate cache (Repository -> Repository<T>)",
            )
        }

    @Test
    fun `GIVEN identical interface WHEN checking regeneration THEN should NOT detect change`() =
        runTest {
            // GIVEN: Interface generated with specific signature
            val optimizations = CompilerOptimizations(logger = FaktLogger.quiet())
            val signature =
                """
                interface com.example.UserService|
                properties:1|name:kotlin.String:val:nonNull|
                functions:2|
                  getUser(id:kotlin.String):com.example.User|
                  save(user:com.example.User):kotlin.Unit
                """.trimIndent()
            val typeInfo =
                TypeInfo(
                    name = "UserService",
                    fullyQualifiedName = "com.example.UserService",
                    packageName = "com.example",
                    fileName = "UserService.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = signature,
                )

            // Record generation
            optimizations.recordGeneration(typeInfo)

            // WHEN: Checking same interface again (e.g., different KMP target)
            val sameTypeInfo = typeInfo.copy() // Exact same signature

            // THEN: Should NOT require regeneration (cached)
            assertFalse(
                optimizations.needsRegeneration(sameTypeInfo),
                "Identical interface should use cache",
            )
        }
}
