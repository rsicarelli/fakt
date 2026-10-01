// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

private const val GENERATE_ANNOTATION =
    "com.rsicarelli.fakt.samples.compilerInterop.processor.Generate"

/** Writes `Generated<Name>` (same package) for each class annotated with [Generate]. */
class GenerateProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(GENERATE_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach(::generate)
        return emptyList()
    }

    private fun generate(source: KSClassDeclaration) {
        val packageName = source.packageName.asString()
        val name = source.simpleName.asString()
        val file =
            codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false, checkNotNull(source.containingFile)),
                packageName = packageName,
                fileName = "Generated$name",
            )
        file.bufferedWriter().use { writer ->
            writer.write(
                """
                |package $packageName
                |
                |/** Written by the KSP processor for [$name]. */
                |data class Generated$name(val origin: String = "$name")
                |"""
                    .trimMargin()
            )
        }
    }
}

/** Service entry point registered in META-INF/services. */
class GenerateProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        GenerateProcessor(environment.codeGenerator)
}
