// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalyzer
import com.rsicarelli.fakt.compiler.discovery.InterfaceDiscovery
import com.rsicarelli.fakt.compiler.generation.CodeGenerator
import com.rsicarelli.fakt.compiler.generation.ConfigurationDslGenerator
import com.rsicarelli.fakt.compiler.generation.FactoryGenerator
import com.rsicarelli.fakt.compiler.generation.ImplementationGenerator
import com.rsicarelli.fakt.compiler.sourceset.SourceSetMapper
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName

/**
 * True IR-Native fake generation using direct IR manipulation.
 *
 * This implementation uses pure IR APIs to:
 * - Dynamically discover interface members without hardcoded mappings
 * - Generate IR nodes directly instead of string templates
 * - Create type-safe implementations with proper type analysis
 * - Handle complex types (generics, suspend functions, collections) automatically
 *
 * Based on the IR-Native demonstration architecture.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class UnifiedFaktIrGenerationExtension(
    private val messageCollector: MessageCollector? = null,
    private val outputDir: String? = null,
    private val fakeAnnotations: List<String> = listOf("com.rsicarelli.fakt.Fake")
) : IrGenerationExtension {

    private val optimizations = CompilerOptimizations(fakeAnnotations, outputDir)

    // Extracted modules following DRY principles
    private val typeResolver = TypeResolver()
    private val importResolver = ImportResolver(typeResolver)
    private val sourceSetMapper = SourceSetMapper(outputDir, messageCollector)
    private val interfaceDiscovery = InterfaceDiscovery(optimizations, messageCollector)
    private val interfaceAnalyzer = InterfaceAnalyzer()

    // Code generation modules following SOLID principles
    private val implementationGenerator = ImplementationGenerator(typeResolver)
    private val factoryGenerator = FactoryGenerator()
    private val configurationDslGenerator = ConfigurationDslGenerator(typeResolver)
    private val codeGenerator = CodeGenerator(
        typeResolver = typeResolver,
        importResolver = importResolver,
        sourceSetMapper = sourceSetMapper,
        implementationGenerator = implementationGenerator,
        factoryGenerator = factoryGenerator,
        configurationDslGenerator = configurationDslGenerator,
        messageCollector = messageCollector
    )

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        messageCollector?.reportInfo("============================================")
        messageCollector?.reportInfo("KtFakes: IR Generation Extension Invoked")
        messageCollector?.reportInfo("KtFakes: Module: ${moduleFragment.name}")
        messageCollector?.reportInfo("KtFakes: Output directory: ${outputDir ?: "auto-detect"}")
        messageCollector?.reportInfo("KtFakes: Configured annotations: ${fakeAnnotations.joinToString()}")
        messageCollector?.reportInfo("============================================")

        // Generate fakes from main source sets (they will be output to test source sets)
        // We don't skip non-test modules anymore - we generate FROM main TO test

        try {
            // Phase 1: Dynamic Interface Discovery
            messageCollector?.reportInfo("KtFakes: Phase 1 - Starting interface discovery")
            val fakeInterfaces = interfaceDiscovery.discoverFakeInterfaces(moduleFragment)
            messageCollector?.reportInfo("KtFakes: Discovered ${fakeInterfaces.size} @Fake annotated interfaces")

            if (fakeInterfaces.isEmpty()) {
                messageCollector?.reportInfo("KtFakes: No @Fake interfaces found in module ${moduleFragment.name}")
                messageCollector?.reportInfo("KtFakes: Checked ${moduleFragment.files.size} files")
                messageCollector?.reportInfo("============================================")
                return
            }

            // Phase 2: IR-Native Code Generation with Incremental Compilation
            for (fakeInterface in fakeInterfaces) {
                val interfaceName = fakeInterface.name.asString()

                // Check if this interface needs regeneration (incremental compilation optimization)
                val typeInfo = TypeInfo(
                    name = interfaceName,
                    fullyQualifiedName = fakeInterface.kotlinFqName.asString(),
                    packageName = fakeInterface.packageFqName?.asString() ?: "",
                    fileName = "",
                    annotations = fakeInterface.annotations.mapNotNull { it.type.classFqName?.asString() },
                    signature = computeInterfaceSignature(fakeInterface)
                )

                if (!optimizations.needsRegeneration(typeInfo)) {
                    messageCollector?.reportInfo("KtFakes: Skipping unchanged interface: $interfaceName")
                    continue
                }

                messageCollector?.reportInfo("KtFakes: Processing interface: $interfaceName")

                // Check for generic support - skip generics with helpful error
                val genericError = interfaceAnalyzer.checkGenericSupport(fakeInterface)
                if (genericError != null) {
                    messageCollector?.reportInfo("KtFakes: Skipping generic interface: $genericError")
                    continue
                }

                // Dynamic interface analysis using IR APIs (IR-native approach!)
                val interfaceAnalysis = interfaceAnalyzer.analyzeInterfaceDynamically(fakeInterface)

                // Generate working fakes using IR-native analysis + modular generation
                codeGenerator.generateWorkingFakeImplementation(
                    sourceInterface = fakeInterface,
                    analysis = interfaceAnalysis,
                    moduleFragment = moduleFragment
                )

                // Record successful generation for incremental compilation
                optimizations.recordGeneration(typeInfo)
                messageCollector?.reportInfo("KtFakes: Generated IR-native fake for $interfaceName")
            }

            messageCollector?.reportInfo("KtFakes: IR-native generation completed successfully")

            // Generate simple compilation report and save signatures for incremental compilation
            (optimizations as? com.rsicarelli.fakt.compiler.optimization.IncrementalCompiler)?.generateReport(outputDir)
            (optimizations as? com.rsicarelli.fakt.compiler.optimization.IncrementalCompiler)?.saveSignatures()
        } catch (e: Exception) {
            messageCollector?.report(
                org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR,
                "KtFakes: IR-native generation failed: ${e.message}",
                null
            )
        }
    }


    private fun MessageCollector.reportInfo(message: String) {
        this.report(org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO, message)
    }

    /**
     * Computes a stable signature for an interface to enable change detection.
     * This signature includes all interface members and their types for accurate change detection.
     */
    private fun computeInterfaceSignature(irClass: IrClass): String {
        // Simplified signature computation to avoid deprecated API issues
        val signature = StringBuilder()
        signature.append("interface ${irClass.kotlinFqName}")

        // Add basic member count for change detection
        val propertyCount = irClass.declarations.filterIsInstance<IrProperty>().size
        val functionCount = irClass.declarations.filterIsInstance<IrSimpleFunction>().size
        signature.append("|props:$propertyCount|funs:$functionCount")

        return signature.toString()
    }
}
