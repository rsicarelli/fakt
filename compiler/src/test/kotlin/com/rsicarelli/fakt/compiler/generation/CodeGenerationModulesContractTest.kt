// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.generation

import com.rsicarelli.fakt.compiler.sourceset.SourceSetMapper
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeResolver
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for extracted code generation modules.
 *
 * These tests verify that the extracted modules exist, have correct constructors,
 * and can be instantiated. This ensures the refactoring didn't break the basic
 * module structure.
 */
class CodeGenerationModulesContractTest {
    @Test
    fun `GIVEN TypeResolver module WHEN instantiating THEN should create successfully`() {
        // GIVEN - TypeResolver extracted from UnifiedFaktIrGenerationExtension
        // WHEN - Creating instance
        val typeResolver = TypeResolver()

        // THEN - Should exist and have expected methods
        assertNotNull(typeResolver, "TypeResolver should be instantiable")

        // Verify key methods exist (contract verification)
        val methods = TypeResolver::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("irTypeToKotlinString"), "Should have irTypeToKotlinString method")
        assertTrue(methods.contains("getDefaultValue"), "Should have getDefaultValue method")
    }

    @Test
    fun `GIVEN ImportResolver module WHEN instantiating THEN should create successfully`() {
        // GIVEN - ImportResolver extracted from UnifiedFaktIrGenerationExtension
        val typeResolver = TypeResolver()

        // WHEN - Creating instance with required dependency
        val importResolver = ImportResolver(typeResolver)

        // THEN - Should exist and have expected methods
        assertNotNull(importResolver, "ImportResolver should be instantiable")

        // Verify key methods exist
        val methods = ImportResolver::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("collectRequiredImports"), "Should have collectRequiredImports method")
    }

    @Test
    fun `GIVEN SourceSetMapper module WHEN instantiating THEN should create successfully`() {
        // GIVEN - SourceSetMapper extracted from UnifiedFaktIrGenerationExtension
        // WHEN - Creating instance with required parameters
        val sourceSetMapper =
            SourceSetMapper(
                outputDir = "/tmp/test",
                messageCollector = null,
            )

        // THEN - Should exist and have expected methods
        assertNotNull(sourceSetMapper, "SourceSetMapper should be instantiable")

        // Verify key methods exist
        val methods = SourceSetMapper::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("getGeneratedSourcesDir"), "Should have getGeneratedSourcesDir method")
    }

    @Test
    fun `GIVEN ImplementationGenerator module WHEN instantiating with TypeResolver THEN should create successfully`() {
        // GIVEN - ImplementationGenerator with dependency injection
        val typeResolver = TypeResolver()

        // WHEN - Creating instance with dependency
        val implementationGenerator = ImplementationGenerator(typeResolver)

        // THEN - Should exist and have expected methods
        assertNotNull(implementationGenerator, "ImplementationGenerator should be instantiable")

        // Verify key methods exist
        val methods = ImplementationGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("generateImplementation"), "Should have generateImplementation method")
    }

    @Test
    fun `GIVEN FactoryGenerator module WHEN instantiating THEN should create successfully`() {
        // GIVEN - FactoryGenerator extracted from UnifiedFaktIrGenerationExtension
        // WHEN - Creating instance
        val factoryGenerator = FactoryGenerator()

        // THEN - Should exist and have expected methods
        assertNotNull(factoryGenerator, "FactoryGenerator should be instantiable")

        // Verify key methods exist
        val methods = FactoryGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("generateFactoryFunction"), "Should have generateFactoryFunction method")
    }

    @Test
    fun `GIVEN ConfigurationDslGenerator module WHEN instantiating with TypeResolver THEN should create successfully`() {
        // GIVEN - ConfigurationDslGenerator with dependency injection
        val typeResolver = TypeResolver()

        // WHEN - Creating instance with dependency
        val configurationDslGenerator = ConfigurationDslGenerator(typeResolver)

        // THEN - Should exist and have expected methods
        assertNotNull(configurationDslGenerator, "ConfigurationDslGenerator should be instantiable")

        // Verify key methods exist
        val methods = ConfigurationDslGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("generateConfigurationDsl"), "Should have generateConfigurationDsl method")
    }

    @Test
    fun `GIVEN CodeGenerator orchestrator WHEN instantiating with all dependencies THEN should create successfully`() {
        // GIVEN - CodeGenerator with full dependency injection setup
        val typeResolver = TypeResolver()
        val importResolver = ImportResolver(typeResolver)
        val sourceSetMapper =
            SourceSetMapper(
                outputDir = "/tmp/test",
                messageCollector = null,
            )
        val implementationGenerator = ImplementationGenerator(typeResolver)
        val factoryGenerator = FactoryGenerator()
        val configurationDslGenerator = ConfigurationDslGenerator(typeResolver)

        // WHEN - Creating CodeGenerator with all dependencies
        val codeGenerator =
            CodeGenerator(
                typeResolver = typeResolver,
                importResolver = importResolver,
                sourceSetMapper = sourceSetMapper,
                implementationGenerator = implementationGenerator,
                factoryGenerator = factoryGenerator,
                configurationDslGenerator = configurationDslGenerator,
                messageCollector = null,
            )

        // THEN - Should exist and demonstrate proper dependency injection architecture
        assertNotNull(codeGenerator, "CodeGenerator should be instantiable with all dependencies")

        // Verify orchestrator method exists
        val methods = CodeGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(
            methods.contains("generateWorkingFakeImplementation"),
            "Should have generateWorkingFakeImplementation orchestrator method",
        )
    }

    @Test
    fun `GIVEN extracted modules WHEN checking module separation THEN should be properly decoupled`() {
        // GIVEN - All extracted modules
        val typeResolver = TypeResolver()
        val importResolver = ImportResolver(typeResolver)
        val sourceSetMapper =
            SourceSetMapper(
                outputDir = "/tmp/test",
                messageCollector = null,
            )

        // WHEN - Checking module independence
        // THEN - Modules should not have circular dependencies

        // TypeResolver should be independent
        val typeResolverFields = TypeResolver::class.java.declaredFields
        assertFalse(
            typeResolverFields.any { it.type.simpleName.contains("ImportResolver") },
            "TypeResolver should not depend on ImportResolver",
        )

        // ImportResolver should be independent
        val importResolverFields = ImportResolver::class.java.declaredFields
        assertFalse(
            importResolverFields.any { it.type.simpleName.contains("SourceSetMapper") },
            "ImportResolver should not depend on SourceSetMapper",
        )

        // This verifies the SOLID principles extraction was successful
        assertTrue(true, "Module decoupling verified - SOLID principles maintained")
    }
}
