// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen

import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.generator.ConfigurationDslGenerator
import com.rsicarelli.fakt.codegen.generator.GeneratedFakeCode
import com.rsicarelli.fakt.codegen.generator.ImplementationGenerator
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderDeclarationsOnly
import com.rsicarelli.fakt.codegen.renderer.renderHeaderOnly
import com.rsicarelli.fakt.codegen.renderer.renderTo

/**
 * Single entry point for rendering a [FakeDeclaration] to source code, with no compiler dependency.
 *
 * Hosts and the upcoming `FaktGenerateTask` (Gradle `@CacheableTask`) call into this object — input
 * is the pure declaration contract plus a pre-resolved import list, output is the file's name and
 * contents. File I/O, path selection, telemetry, and import resolution remain caller-owned.
 */
public object FaktCodegen {
    private val implementationGenerator = ImplementationGenerator()
    private val configDslGenerator = ConfigurationDslGenerator()

    /**
     * Renders [decl] into a single Kotlin source file: factory function first (user-facing API),
     * then config DSL, then the implementation class with any call-history components.
     *
     * @param decl Pure declaration contract (types pre-rendered to strings).
     * @param imports Fully-qualified names to import. Caller is responsible for resolution —
     *   typically the output of the compiler-side `ImportResolver`.
     */
    public fun render(
        decl: FakeDeclaration,
        imports: List<String> = emptyList(),
    ): RenderedFakeFile =
        when (decl) {
            is FakeDeclaration.Interface -> renderInterface(decl, imports)
            is FakeDeclaration.Class -> renderClass(decl, imports)
        }

    private fun renderInterface(
        decl: FakeDeclaration.Interface,
        imports: List<String>,
    ): RenderedFakeFile {
        val fakeClassName = "Fake${decl.simpleName}Impl"
        val generated =
            implementationGenerator.generateImplementation(decl, decl.packageName, imports)
        val configDsl = configDslGenerator.generateConfigurationDslCodeFile(decl, fakeClassName)
        return assemble(decl.packageName, fakeClassName, generated, configDsl)
    }

    private fun renderClass(decl: FakeDeclaration.Class, imports: List<String>): RenderedFakeFile {
        val fakeClassName = "Fake${decl.simpleName}Impl"
        val generated = implementationGenerator.generateClassFake(decl, decl.packageName, imports)
        val configDsl = configDslGenerator.generateConfigurationDslCodeFile(decl, fakeClassName)
        return assemble(decl.packageName, fakeClassName, generated, configDsl)
    }

    private fun assemble(
        packageName: String,
        fakeClassName: String,
        generated: GeneratedFakeCode,
        configDsl: CodeFile,
    ): RenderedFakeFile {
        val builder = CodeBuilder()
        generated.implementationFile.renderHeaderOnly(builder)
        generated.factoryFunction.declarations.forEach { it.renderTo(builder) }
        builder.appendLine()
        configDsl.declarations.forEach { it.renderTo(builder) }
        builder.appendLine()
        generated.implementationFile.renderDeclarationsOnly(builder)
        return RenderedFakeFile(
            packageName = packageName,
            fileName = "$fakeClassName.kt",
            content = builder.build(),
        )
    }
}

/**
 * Output of [FaktCodegen.render]: the file's content plus the package and file name needed to place
 * it on disk.
 *
 * @property packageName Target package (e.g. `"com.example.auth"`); maps to the directory layout.
 * @property fileName Generated file name (e.g. `"FakeUserServiceImpl.kt"`).
 * @property content Full Kotlin source ready to be written verbatim.
 */
public data class RenderedFakeFile(
    val packageName: String,
    val fileName: String,
    val content: String,
)
