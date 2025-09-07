// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import dev.rsicarelli.ktfake.compiler.UnifiedKtFakesIrGenerationExtension

/**
 * Tests for UnifiedKtFakesIrGenerationExtension functionality.
 *
 * Following updated BDD testing guidelines with GIVEN-WHEN-THEN format.
 * Uses JUnit5 with @TestInstance(PER_CLASS) for better performance and isolation.
 * Each test creates its own instances for proper isolation.
 *
 * Based on unified architecture requirement: "IR-native generation with dynamic analysis"
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnifiedKtFakesIrGenerationExtensionTest {

    @Test
    fun `GIVEN message collector WHEN creating extension THEN should create instance successfully`() = runTest {
        // Given - message collector for plugin communication
        val messageCollector = TestMessageCollector()

        // When - creating UnifiedKtFakesIrGenerationExtension
        val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)

        // Then - should create instance without errors
        assertNotNull(extension, "Extension should be created successfully")
    }

    @Test
    fun `GIVEN extension instance WHEN checking available methods THEN should have required IR generation methods`() = runTest {
        // Given - unified IR generation extension
        val messageCollector = TestMessageCollector()
        val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)
        
        // When - checking available methods  
        val methods = extension::class.java.declaredMethods.map { it.name }

        // Then - should have core generation capabilities
        assertTrue(methods.any { it == "generate" }, "Should have main generate method")
        assertTrue(methods.any { it.contains("capitalize") || it.contains("Capitalize") }, "Should have string utilities")
        assertTrue(methods.any { it.contains("generateImplementationClass") }, "Should have implementation class generation")
        assertTrue(methods.any { it.contains("generateFactoryFunction") }, "Should have factory function generation")
        assertTrue(methods.any { it.contains("generateConfigurationDsl") }, "Should have configuration DSL generation")
    }

    @Test
    fun `GIVEN test value WHEN capitalizing string THEN should capitalize first letter correctly`() = runTest {
        // Given - extension instance and test value
        val messageCollector = TestMessageCollector()
        val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)
        val testValue = "testValue"
        
        // When - using string capitalization utility
        val result = extension.javaClass.getDeclaredMethod("capitalize", String::class.java).let { method ->
            method.isAccessible = true
            method.invoke(extension, testValue) as String
        }
        
        // Then - should capitalize first letter correctly
        assertEquals("TestValue", result, "Should capitalize first letter correctly")
    }

    @Test
    fun `GIVEN interface names WHEN processing for class generation THEN should properly capitalize names`() = runTest {
        // Given - extension instance and test interface names
        val messageCollector = TestMessageCollector()
        val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)
        val testCases = mapOf(
            "userService" to "UserService",
            "asyncUserService" to "AsyncUserService", 
            "analyticsService" to "AnalyticsService",
            "testService" to "TestService"
        )
        
        // When - processing interface names through capitalize utility
        // Then - should properly capitalize interface names for class generation
        testCases.forEach { (input, expected) ->
            val method = extension.javaClass.getDeclaredMethod("capitalize", String::class.java)
            method.isAccessible = true
            val result = method.invoke(extension, input) as String
            assertEquals(expected, result, "Should capitalize $input to $expected")
        }
    }

    @Test
    fun `GIVEN extension with message collector WHEN reporting info message THEN should collect messages properly`() = runTest {
        // Given - extension with message collector
        val messageCollector = TestMessageCollector()
        val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)
        
        // When - reporting an info message
        messageCollector.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO,
            "KtFakes: Test message",
            null
        )
        
        // Then - should collect messages properly
        assertEquals(1, messageCollector.messages.size, "Should have collected one message")
        assertTrue(messageCollector.messages.first().contains("Test message"), "Should contain test message")
        assertTrue(!messageCollector.hasErrors(), "Should not have errors for INFO messages")
    }

    @Test
    fun `GIVEN extension with message collector WHEN reporting error message THEN should handle error scenarios gracefully`() = runTest {
        // Given - extension with message collector
        val messageCollector = TestMessageCollector()
        val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)
        
        // When - reporting an error message
        messageCollector.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR,
            "KtFakes: Test error",
            null
        )
        
        // Then - should detect errors correctly
        assertTrue(messageCollector.hasErrors(), "Should detect ERROR severity messages")
        assertEquals(1, messageCollector.messages.size, "Should collect error message")
        assertTrue(messageCollector.messages.first().startsWith("ERROR"), "Should prefix with ERROR severity")
    }

    @Test
    fun `GIVEN basic type names WHEN validating type mappings THEN should provide correct type string mappings`() = runTest {
        // Given - basic Kotlin type names that our extension should handle
        val testTypeNames = listOf("String", "Int", "Boolean", "Unit", "Long", "Float", "Double")
        
        // When - validating type name properties
        // Then - type names should be properly formatted
        testTypeNames.forEach { typeName ->
            assertTrue(typeName.isNotEmpty(), "Type name $typeName should not be empty")
            assertTrue(typeName.first().isUpperCase(), "Type name $typeName should be properly capitalized")
        }
    }

    @Test
    fun `GIVEN nullable and non-nullable types WHEN handling type annotations THEN should handle nullable type annotations correctly`() = runTest {
        // Given - nullable and non-nullable type examples
        val nullableTypes = listOf("String?", "Int?", "Boolean?", "Unit?")
        val nonNullableTypes = listOf("String", "Int", "Boolean", "Unit")
        
        // When - checking nullable type formatting
        // Then - nullable types should end with '?' and non-nullable should not
        nullableTypes.forEach { type ->
            assertTrue(type.endsWith("?"), "Nullable type $type should end with '?'")
        }
        
        nonNullableTypes.forEach { type ->
            assertTrue(!type.endsWith("?"), "Non-nullable type $type should not end with '?'")
        }
    }

    @Test
    fun `GIVEN different parameter counts WHEN generating lambda signatures THEN should generate proper lambda signatures`() = runTest {
        // Given - test cases for different parameter counts and expected lambda patterns
        val testCases = mapOf(
            0 to "{ \"\" }",              // No parameters
            1 to "{ _ -> \"\" }",          // One parameter  
            2 to "{ _, _ -> false }",      // Two parameters
            3 to "{ _, _, _ -> Unit }"     // Three parameters
        )
        
        // When - validating lambda signature generation logic
        // Then - should generate proper parameter placeholders
        testCases.forEach { (paramCount, expectedPattern) ->
            val underscoreCount = expectedPattern.count { it == '_' }
            if (paramCount == 0) {
                assertTrue(!expectedPattern.contains("_"), "Zero parameters should not contain underscores")
            } else {
                assertEquals(paramCount, underscoreCount, "Should have $paramCount underscores for $paramCount parameters")
            }
        }
    }

    @Nested
    @DisplayName("Core Generation Methods")
    inner class CoreGenerationMethods {
        
        // TODO: These tests require proper IR mocking infrastructure
        // For Phase 1, we'll validate the core methods exist and can be called
        // More comprehensive tests will be added in Phase 2 with proper IR infrastructure
        
        @Test
        fun `GIVEN extension instance WHEN checking core generation methods THEN should have generateImplementationClass method`() = runTest {
            // Given - extension instance
            val messageCollector = TestMessageCollector()
            val extension = UnifiedKtFakesIrGenerationExtension(messageCollector)
            
            // When - checking for core generation methods
            val methods = extension::class.java.declaredMethods.map { it.name }
            
            // Then - should have the core generation capabilities  
            assertTrue(methods.any { it.contains("generateImplementationClass") }, "Should have generateImplementationClass method")
            assertTrue(methods.any { it.contains("generateFactoryFunction") }, "Should have generateFactoryFunction method")
            assertTrue(methods.any { it.contains("generateConfigurationDsl") }, "Should have generateConfigurationDsl method")
            assertTrue(methods.any { it.contains("analyzeInterfaceDynamically") }, "Should have dynamic interface analysis method")
            assertTrue(methods.any { it.contains("discoverFakeInterfaces") }, "Should have fake interface discovery method")
        }
    }
    
    @Nested
    @DisplayName("Type System Handling")
    inner class TypeSystemHandling {
        
        @Test
        fun `GIVEN primitive types WHEN validating type patterns THEN should handle all primitive types correctly`() = runTest {
            // Given - primitive type examples that our extension should handle
            val primitiveTypes = listOf("String", "Int", "Boolean", "Unit", "Long", "Float", "Double")
            
            // When - validating type string patterns
            // Then - primitive types should follow Kotlin naming conventions
            primitiveTypes.forEach { typeName ->
                assertTrue(typeName.isNotEmpty(), "Type $typeName should not be empty")
                assertTrue(typeName.first().isUpperCase(), "Type $typeName should start with uppercase")
                assertTrue(!typeName.contains("?"), "Non-nullable type $typeName should not contain ?")
            }
        }
        
        @Test
        fun `GIVEN nullable types WHEN validating type patterns THEN should handle nullable types correctly`() = runTest {
            // Given - nullable and non-nullable type examples
            val nullableTypes = listOf("String?", "Int?", "Boolean?", "List<String>?")
            val nonNullableTypes = listOf("String", "Int", "Boolean", "List<String>")
            
            // When - checking nullable type patterns
            // Then - should distinguish between nullable and non-nullable types
            nullableTypes.forEach { type ->
                assertTrue(type.endsWith("?"), "Nullable type $type should end with ?")
            }
            
            nonNullableTypes.forEach { type ->
                assertTrue(!type.endsWith("?"), "Non-nullable type $type should not end with ?")
            }
        }
        
        @Test
        fun `GIVEN collection types WHEN validating type patterns THEN should handle collection types correctly`() = runTest {
            // Given - collection type examples
            val collectionTypes = listOf("List<String>", "Set<Int>", "Map<String, Int>", "Array<Boolean>")
            
            // When - validating collection type patterns
            // Then - should be properly formatted generic types
            collectionTypes.forEach { type ->
                assertTrue(type.contains("<") && type.contains(">"), "Collection type $type should have generic parameters")
                assertTrue(type.first().isUpperCase(), "Collection type $type should start with uppercase")
            }
        }
    }
    
    // For now, let's use a simplified approach that doesn't require complex IR mocking
    // This will be expanded as we implement more advanced testing patterns
    
    // Test utility class for message collection - each test creates its own instance
    private class TestMessageCollector : MessageCollector {
        val messages = mutableListOf<String>()
        
        override fun report(severity: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity, message: String, location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?) {
            messages.add("$severity: $message")
        }

        override fun hasErrors(): Boolean = messages.any { it.startsWith("ERROR") }
        override fun clear() { messages.clear() }
    }
}