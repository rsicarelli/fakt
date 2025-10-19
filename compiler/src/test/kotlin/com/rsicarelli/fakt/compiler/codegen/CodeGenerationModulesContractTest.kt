// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.output.SourceSetMapper
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeResolver
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for code generation modules.
 *
 * Verifies that all code generation modules have correct constructors,
 * can be instantiated, and maintain expected method contracts.
 */
class CodeGenerationModulesContractTest {
    @Test
    fun `GIVEN TypeResolver module WHEN instantiating THEN should create successfully`() {
        // GIVEN & WHEN
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
        // GIVEN & WHEN
        val typeResolver = TypeResolver()
        val importResolver = ImportResolver(typeResolver)

        // THEN - Should exist and have expected methods
        assertNotNull(importResolver, "ImportResolver should be instantiable")

        // Verify key methods exist
        val methods = ImportResolver::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("collectRequiredImports"), "Should have collectRequiredImports method")
    }

    @Test
    fun `GIVEN SourceSetMapper module WHEN instantiating THEN should create successfully`() {
        // GIVEN & WHEN
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
        // GIVEN & WHEN
        val typeResolver = TypeResolver()
        val implementationGenerator = ImplementationGenerator(typeResolver)

        // THEN - Should exist and have expected methods
        assertNotNull(implementationGenerator, "ImplementationGenerator should be instantiable")

        // Verify key methods exist
        val methods = ImplementationGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("generateImplementation"), "Should have generateImplementation method")
    }

    @Test
    fun `GIVEN FactoryGenerator module WHEN instantiating THEN should create successfully`() {
        // GIVEN & WHEN
        val factoryGenerator = FactoryGenerator()

        // THEN - Should exist and have expected methods
        assertNotNull(factoryGenerator, "FactoryGenerator should be instantiable")

        // Verify key methods exist
        val methods = FactoryGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("generateFactoryFunction"), "Should have generateFactoryFunction method")
    }

    @Test
    fun `GIVEN ConfigurationDslGenerator module WHEN instantiating with TypeResolver THEN should create successfully`() {
        // GIVEN & WHEN
        val typeResolver = TypeResolver()
        val configurationDslGenerator = ConfigurationDslGenerator(typeResolver)

        // THEN - Should exist and have expected methods
        assertNotNull(configurationDslGenerator, "ConfigurationDslGenerator should be instantiable")

        // Verify key methods exist
        val methods = ConfigurationDslGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("generateConfigurationDsl"), "Should have generateConfigurationDsl method")
    }

    @Test
    fun `GIVEN CodeGenerator orchestrator WHEN instantiating with all dependencies THEN should create successfully`() {
        // GIVEN & WHEN
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

        val generators =
            CodeGenerators(
                implementation = implementationGenerator,
                factory = factoryGenerator,
                configDsl = configurationDslGenerator,
            )

        val codeGenerator =
            CodeGenerator(
                importResolver = importResolver,
                sourceSetMapper = sourceSetMapper,
                generators = generators,
                messageCollector = null,
            )

        // THEN
        assertNotNull(codeGenerator, "CodeGenerator should be instantiable with all dependencies")

        // Verify orchestrator method exists
        val methods = CodeGenerator::class.java.declaredMethods.map { it.name }
        assertTrue(
            methods.contains("generateWorkingFakeImplementation"),
            "Should have generateWorkingFakeImplementation orchestrator method",
        )
    }

    @Test
    fun `GIVEN code generation modules WHEN checking module separation THEN should be properly decoupled`() {
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
    }
}
