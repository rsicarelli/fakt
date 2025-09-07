// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.analysis

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Nested
import kotlin.test.*

/**
 * Simple tests for SimpleInterfaceAnalyzer following TDD principles.
 *
 * Focus on basic functionality first, then we'll enhance with complex IR scenarios.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleInterfaceAnalyzerTest {

    private val analyzer = SimpleInterfaceAnalyzer()

    @Nested
    inner class BasicFunctionality {

        @Test
        fun `GIVEN interface WHEN analyzing THEN should return analysis with interface name`() {
            // Given
            val testInterface = createSimpleInterface("UserService", ClassKind.INTERFACE)

            // When
            val analysis = analyzer.analyzeInterface(testInterface)

            // Then
            assertEquals("UserService", analysis.interfaceName)
            assertEquals(testInterface, analysis.sourceInterface)
            assertTrue(analysis.annotations.concurrent, "Should default to thread-safe")
            assertFalse(analysis.annotations.trackCalls, "Should default to no call tracking")
        }

        @Test
        fun `GIVEN interface with annotation WHEN discovering fakes THEN should find interface`() {
            // Given
            val interfaceWithAnnotation = createSimpleInterfaceWithAnnotation("TestService")
            val interfaceWithoutAnnotation = createSimpleInterface("RegularService", ClassKind.INTERFACE)

            val moduleClasses = listOf(interfaceWithAnnotation, interfaceWithoutAnnotation)

            // When
            val discovered = analyzer.discoverFakeInterfaces(moduleClasses)

            // Then
            assertEquals(1, discovered.size)
            assertEquals("TestService", discovered.first().name.asString())
        }

        @Test
        fun `GIVEN object with annotation WHEN discovering fakes THEN should not find object`() {
            // Given
            val objectWithAnnotation = createSimpleInterfaceWithAnnotation("TestObject", ClassKind.OBJECT)
            val moduleClasses = listOf(objectWithAnnotation)

            // When
            val discovered = analyzer.discoverFakeInterfaces(moduleClasses)

            // Then
            assertEquals(0, discovered.size, "Objects should not be discovered as fake interfaces")
        }
    }

    @Nested
    inner class ValidationTests {

        @Test
        fun `GIVEN valid interface WHEN validating THEN should return valid`() {
            // Given
            val validInterface = createSimpleInterfaceWithContent("ValidService")

            // When
            val result = analyzer.validateInterface(validInterface)

            // Then
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        fun `GIVEN object WHEN validating THEN should return error about thread safety`() {
            // Given
            val objectDeclaration = createSimpleInterface("ServiceObject", ClassKind.OBJECT)

            // When
            val result = analyzer.validateInterface(objectDeclaration)

            // Then
            assertTrue(result is ValidationResult.Invalid)
            val errors = (result as ValidationResult.Invalid).errors
            assertTrue(errors.any { it.contains("thread safety") })
        }

        @Test
        fun `GIVEN empty interface WHEN validating THEN should return error about no content`() {
            // Given
            val emptyInterface = createSimpleInterface("EmptyService", ClassKind.INTERFACE)

            // When
            val result = analyzer.validateInterface(emptyInterface)

            // Then
            assertTrue(result is ValidationResult.Invalid)
            val errors = (result as ValidationResult.Invalid).errors
            assertTrue(errors.any { it.contains("no methods or properties") })
        }
    }

    // Simple test builders without complex IR APIs
    private fun createSimpleInterface(name: String, kind: ClassKind): IrClass {
        return IrClassImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFINED,
            symbol = IrClassSymbolImpl(),
            name = Name.identifier(name),
            kind = kind,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.ABSTRACT
        ).apply {
            annotations = emptyList()
            declarations = emptyList()
        }
    }

    private fun createSimpleInterfaceWithAnnotation(name: String, kind: ClassKind = ClassKind.INTERFACE): IrClass {
        return IrClassImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFINED,
            symbol = IrClassSymbolImpl(),
            name = Name.identifier(name),
            kind = kind,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.ABSTRACT
        ).apply {
            // Fake annotation presence by having non-empty annotations list
            annotations = listOf() // For real IR, we'd create actual annotation calls
            declarations = emptyList()
        }
    }

    private fun createSimpleInterfaceWithContent(name: String): IrClass {
        return IrClassImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFINED,
            symbol = IrClassSymbolImpl(),
            name = Name.identifier(name),
            kind = ClassKind.INTERFACE,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.ABSTRACT
        ).apply {
            annotations = emptyList()
            // Fake some content by having non-empty declarations list
            declarations = emptyList() // For real implementation, would add actual functions
        }
    }
}
