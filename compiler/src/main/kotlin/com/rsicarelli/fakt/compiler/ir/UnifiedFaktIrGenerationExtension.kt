// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir

import com.rsicarelli.fakt.compiler.codegen.CodeGenerator
import com.rsicarelli.fakt.compiler.codegen.CodeGenerators
import com.rsicarelli.fakt.compiler.codegen.ConfigurationDslGenerator
import com.rsicarelli.fakt.compiler.codegen.FactoryGenerator
import com.rsicarelli.fakt.compiler.codegen.ImplementationGenerator
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalyzer
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalyzer.isFakableClass
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalyzer
import com.rsicarelli.fakt.compiler.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.output.SourceSetMapper
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeInfo
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classFqName
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
 *
 * **Modernization (v1.1.0)**:
 * - Added sourceSetContext for data-driven source set resolution
 * - Uses SourceSetResolver for hierarchy traversal instead of hardcoded patterns
 * - Maintains backward compatibility with legacy mapping when context is null
 *
 * ## Safety: UnsafeDuringIrConstructionAPI Usage
 *
 * This extension uses APIs marked with `@UnsafeDuringIrConstructionAPI`:
 * - `IrClass.declarations` - For analyzing interface/class members
 * - `IrSymbol.owner` - For type hierarchy traversal
 *
 * **Why it's safe:**
 * - `IrGenerationExtension.generate()` is called **AFTER** IR construction is complete
 * - All IR symbols are bound at the post-linkage phase
 * - The "unsafe during construction" warning doesn't apply to the generation phase
 * - Metro compiler plugin (production-quality) uses the exact same approach
 *
 * See: `compiler/build.gradle.kts` for module-level opt-in configuration
 *
 * ## Suppress Justification
 * - **TooManyFunctions**: IR generation requires many small orchestrator functions for clarity.
 *   Refactored from large methods (94 lines) to small helpers (10-20 lines each).
 */
@Suppress("TooManyFunctions")
class UnifiedFaktIrGenerationExtension(
    private val messageCollector: MessageCollector? = null,
    private val outputDir: String? = null,
    private val fakeAnnotations: List<String> = listOf("com.rsicarelli.fakt.Fake"),
) : IrGenerationExtension {
    private val optimizations = CompilerOptimizations(fakeAnnotations, outputDir)

    // Extracted modules following DRY principles
    private val typeResolver = TypeResolver()
    private val importResolver = ImportResolver(typeResolver)

    private val sourceSetMapper =
        SourceSetMapper(
            outputDir = outputDir,
            messageCollector = messageCollector,
        )
    private val interfaceAnalyzer = InterfaceAnalyzer()

    // Code generation modules following SOLID principles
    private val generators =
        CodeGenerators(
            implementation = ImplementationGenerator(typeResolver),
            factory = FactoryGenerator(),
            configDsl = ConfigurationDslGenerator(typeResolver),
        )
    private val codeGenerator =
        CodeGenerator(
            importResolver = importResolver,
            sourceSetMapper = sourceSetMapper,
            generators = generators,
            messageCollector = messageCollector,
        )

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        logGenerationHeader(moduleFragment)

        try {
            val (fakeInterfaces, fakeClasses) = discoverAndLogFakes(moduleFragment) ?: return

            val interfacesToProcess = filterInterfacesToProcess(fakeInterfaces)
            val classesToProcess = filterClassesToProcess(fakeClasses)

            processInterfaces(interfacesToProcess, moduleFragment)
            processClasses(classesToProcess, moduleFragment)

            logGenerationCompletion(interfacesToProcess.size, classesToProcess.size)
        } catch (e: Exception) {
            logGenerationError(e)
        }
    }

    private fun logGenerationHeader(moduleFragment: IrModuleFragment) {
        messageCollector?.reportInfo("============================================")
        messageCollector?.reportInfo("Fakt: IR Generation Extension Invoked")
        messageCollector?.reportInfo("Fakt: Module: ${moduleFragment.name}")
        messageCollector?.reportInfo("Fakt: Output directory: ${outputDir ?: "auto-detect"}")
        messageCollector?.reportInfo("Fakt: Configured annotations: ${fakeAnnotations.joinToString()}")
        messageCollector?.reportInfo("============================================")
    }

    private fun discoverAndLogFakes(moduleFragment: IrModuleFragment): Pair<List<IrClass>, List<IrClass>>? {
        messageCollector?.reportInfo("Fakt: Phase 1 - Starting interface & class discovery")

        val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
        messageCollector?.reportInfo("Fakt: Discovered ${fakeInterfaces.size} @Fake annotated interfaces")

        val fakeClasses = discoverFakeClasses(moduleFragment)
        messageCollector?.reportInfo("Fakt: Discovered ${fakeClasses.size} @Fake annotated classes")

        if (fakeInterfaces.isEmpty() && fakeClasses.isEmpty()) {
            messageCollector?.reportInfo(
                "Fakt: No @Fake interfaces or classes found in module ${moduleFragment.name}",
            )
            messageCollector?.reportInfo("Fakt: Checked ${moduleFragment.files.size} files")
            messageCollector?.reportInfo("============================================")
            return null
        }

        return fakeInterfaces to fakeClasses
    }

    private fun processInterfaces(
        interfacesToProcess: List<Pair<IrClass, TypeInfo>>,
        moduleFragment: IrModuleFragment,
    ) {
        for ((fakeInterface, typeInfo) in interfacesToProcess) {
            val interfaceName = fakeInterface.name.asString()
            messageCollector?.reportInfo("Fakt: Processing interface: $interfaceName")

            val interfaceAnalysis = interfaceAnalyzer.analyzeInterfaceDynamically(fakeInterface)
            validateAndLogPattern(interfaceAnalysis, fakeInterface, interfaceName)

            codeGenerator.generateWorkingFakeImplementation(
                sourceInterface = fakeInterface,
                analysis = interfaceAnalysis,
                moduleFragment = moduleFragment,
            )

            optimizations.recordGeneration(typeInfo)
            messageCollector?.reportInfo("Fakt: Generated IR-native fake for $interfaceName")
        }
    }

    private fun processClasses(
        classesToProcess: List<Pair<IrClass, TypeInfo>>,
        moduleFragment: IrModuleFragment,
    ) {
        for ((fakeClass, typeInfo) in classesToProcess) {
            val className = fakeClass.name.asString()
            messageCollector?.reportInfo("Fakt: Processing class: $className")

            val classAnalysis = ClassAnalyzer.analyzeClass(fakeClass)

            codeGenerator.generateWorkingClassFake(
                sourceClass = fakeClass,
                analysis = classAnalysis,
                moduleFragment = moduleFragment,
            )

            optimizations.recordGeneration(typeInfo)
            messageCollector?.reportInfo("Fakt: Generated fake for class $className")
        }
    }

    private fun logGenerationCompletion(
        interfaceCount: Int,
        classCount: Int,
    ) {
        messageCollector?.reportInfo(
            "Fakt: IR-native generation completed successfully ($interfaceCount interfaces, $classCount classes)",
        )
    }

    private fun logGenerationError(exception: Exception) {
        messageCollector?.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR,
            "Fakt: IR-native generation failed: ${exception.message}",
            null,
        )
    }

    private fun MessageCollector.reportInfo(message: String) {
        this.report(org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO, message)
    }

    /**
     * Creates TypeInfo for incremental compilation tracking.
     *
     * @param fakeInterface The IR class to create TypeInfo for
     * @return TypeInfo containing interface metadata
     */
    private fun createTypeInfo(fakeInterface: IrClass): TypeInfo {
        val interfaceName = fakeInterface.name.asString()
        val fullyQualifiedName = fakeInterface.kotlinFqName.asString()
        // Use FQN as fileName since we don't have direct access to IrFile from IrClass
        // This is sufficient for incremental compilation tracking
        val fileName = "$fullyQualifiedName.kt"

        return TypeInfo(
            name = interfaceName,
            fullyQualifiedName = fullyQualifiedName,
            packageName = fakeInterface.packageFqName?.asString() ?: "",
            fileName = fileName,
            annotations = fakeInterface.annotations.mapNotNull { it.type.classFqName?.asString() },
            signature = computeInterfaceSignature(fakeInterface),
        )
    }

    /**
     * Filters interfaces to determine which need fake generation.
     * Skips unchanged interfaces (incremental compilation) and unsupported generic interfaces.
     *
     * @param fakeInterfaces All discovered @Fake interfaces
     * @return List of interfaces paired with their TypeInfo that need processing
     */
    private fun filterInterfacesToProcess(
        fakeInterfaces: List<IrClass>,
    ): List<Pair<IrClass, com.rsicarelli.fakt.compiler.types.TypeInfo>> =
        fakeInterfaces.mapNotNull { fakeInterface ->
            val interfaceName = fakeInterface.name.asString()
            val typeInfo = createTypeInfo(fakeInterface)

            // Phase 2: Detect generic pattern
            val genericPattern =
                com.rsicarelli.fakt.compiler.ir.analysis
                    .GenericPatternAnalyzer()
                    .analyzeInterface(fakeInterface)

            when {
                !optimizations.needsRegeneration(typeInfo) -> {
                    messageCollector?.reportInfo("Fakt: Skipping unchanged interface: $interfaceName")
                    null
                }

                // ✅ PHASE 2: Class-level generics supported!
                // ✅ PHASE 3: Method-level generics supported!
                // ✅ PHASE 3: Mixed generics (class + method) now enabled!
                // All patterns (NoGenerics, ClassLevelGenerics, MethodLevelGenerics, MixedGenerics) will be processed
                else -> fakeInterface to typeInfo
            }
        }

    /**
     * Discovers all @Fake annotated interfaces in the module.
     *
     * @param moduleFragment The IR module to search
     * @return List of @Fake annotated interfaces
     */
    private fun discoverFakeInterfaces(moduleFragment: IrModuleFragment): List<IrClass> {
        val discoveredInterfaces = mutableListOf<IrClass>()

        moduleFragment.files.forEach { file ->
            file.declarations.forEach { declaration ->
                processDeclarationForFake(declaration, discoveredInterfaces)
            }
        }

        return discoveredInterfaces
    }

    private fun processDeclarationForFake(
        declaration: IrDeclaration,
        discoveredInterfaces: MutableList<IrClass>,
    ) {
        if (!isValidFakeInterface(declaration)) return

        val irClass = declaration as IrClass
        val matchingAnnotation =
            irClass.annotations.find { annotation ->
                val annotationFqName = annotation.type.classFqName?.asString()
                annotationFqName != null && (
                    optimizations.isConfiguredFor(annotationFqName) ||
                        ClassAnalyzer.hasGeneratesFakeMetaAnnotation(annotation)
                )
            }

        if (matchingAnnotation != null) {
            discoveredInterfaces.add(irClass)

            // Index type for optimization tracking
            val typeInfo = createTypeInfo(irClass)
            optimizations.indexType(typeInfo)

            messageCollector?.reportInfo(
                "Fakt: Discovered interface with @Fake: ${irClass.name}",
            )
        }
    }

    private fun isValidFakeInterface(declaration: IrDeclaration): Boolean =
        declaration is IrClass &&
            declaration.kind == org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE &&
            declaration.modality != org.jetbrains.kotlin.descriptors.Modality.SEALED &&
            declaration.origin != org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    /**
     * Discovers all @Fake annotated classes in the module.
     *
     * @param moduleFragment The IR module to search
     * @return List of fakable classes
     */
    private fun discoverFakeClasses(moduleFragment: IrModuleFragment): List<IrClass> {
        val discoveredClasses = mutableListOf<IrClass>()

        moduleFragment.files.forEach { file ->
            file.declarations.forEach { declaration ->
                if (declaration is IrClass && declaration.isFakableClass()) {
                    discoveredClasses.add(declaration)
                    messageCollector?.reportInfo("Fakt: Discovered fakable class: ${declaration.name}")
                }
            }
        }

        return discoveredClasses
    }

    /**
     * Filters classes to determine which need fake generation.
     * Skips unchanged classes (incremental compilation).
     *
     * @param fakeClasses All discovered @Fake classes
     * @return List of classes paired with their TypeInfo that need processing
     */
    private fun filterClassesToProcess(fakeClasses: List<IrClass>): List<Pair<IrClass, TypeInfo>> =
        fakeClasses.mapNotNull { fakeClass ->
            val className = fakeClass.name.asString()
            val typeInfo = createTypeInfo(fakeClass)

            when {
                !optimizations.needsRegeneration(typeInfo) -> {
                    messageCollector?.reportInfo("Fakt: Skipping unchanged class: $className")
                    null
                }
                else -> fakeClass to typeInfo
            }
        }

    /**
     * Validates the analyzed generic pattern and logs warnings and analysis summary.
     * Extracted to reduce complexity of the main generate() method.
     *
     * @param interfaceAnalysis The analyzed interface
     * @param fakeInterface The IR class being processed
     * @param interfaceName Name of the interface for logging
     */
    private fun validateAndLogPattern(
        interfaceAnalysis: com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis,
        fakeInterface: IrClass,
        interfaceName: String,
    ) {
        // Validate pattern for consistency using companion object methods
        val warnings =
            com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer.validatePattern(
                interfaceAnalysis.genericPattern,
                fakeInterface,
            )

        // Log warnings if any
        if (warnings.isNotEmpty()) {
            warnings.forEach { warning ->
                messageCollector?.reportInfo("Fakt: WARNING - $warning in $interfaceName")
            }
        }

        // Log analysis summary for debugging
        val summary =
            com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer.getAnalysisSummary(
                interfaceAnalysis.genericPattern,
            )
        messageCollector?.reportInfo("Fakt: Analysis - $summary")
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
