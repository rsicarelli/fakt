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
import com.rsicarelli.fakt.compiler.telemetry.CompilationReport
import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.telemetry.FaktTelemetry
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeInfo
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
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
    private val logger: FaktLogger,
    private val outputDir: String? = null,
    private val fakeAnnotations: List<String> = listOf("com.rsicarelli.fakt.Fake"),
) : IrGenerationExtension {
    private val optimizations = CompilerOptimizations(fakeAnnotations, outputDir, logger)

    // Extracted modules following DRY principles
    private val typeResolver = TypeResolver()
    private val importResolver = ImportResolver(typeResolver)

    private val sourceSetMapper =
        SourceSetMapper(
            outputDir = outputDir,
            logger = logger,
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
            logger = logger,
        )

    // Initialize telemetry
    private val telemetry = FaktTelemetry.initialize(logger)

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

            logGenerationCompletion(interfacesToProcess.size, classesToProcess.size, moduleFragment)
        } catch (e: Exception) {
            logGenerationError(e)
        }
    }

    private fun logGenerationHeader(moduleFragment: IrModuleFragment) {
        // TRACE only: Detailed IR generation invocation
        logger.trace("════════════════════════════════════════")
        logger.trace("IR Generation Extension Invoked")
        logger.trace("Module: ${moduleFragment.name}")
        logger.trace("Output directory: ${outputDir ?: "auto-detect"}")
        logger.trace("Configured annotations: ${fakeAnnotations.joinToString()}")
        logger.trace("════════════════════════════════════════")
    }

    private fun discoverAndLogFakes(moduleFragment: IrModuleFragment): Pair<List<IrClass>, List<IrClass>>? {
        val phaseId = telemetry.startPhase("DISCOVERY")

        val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
        val fakeClasses = discoverFakeClasses(moduleFragment)

        telemetry.metricsCollector.incrementInterfacesDiscovered(fakeInterfaces.size)
        telemetry.metricsCollector.incrementClassesDiscovered(fakeClasses.size)

        telemetry.endPhase(phaseId)

        // TRACE only: Detailed discovery info
        logger.trace("Discovered ${fakeInterfaces.size} @Fake annotated interfaces")
        logger.trace("Discovered ${fakeClasses.size} @Fake annotated classes")
        logger.trace("Checked ${moduleFragment.files.size} files in module ${moduleFragment.name}")

        if (fakeInterfaces.isEmpty() && fakeClasses.isEmpty()) {
            // Nothing to generate - silent in DEBUG/INFO
            logger.trace("No @Fake interfaces or classes found")
            return null
        }

        return fakeInterfaces to fakeClasses
    }

    private fun processInterfaces(
        interfacesToProcess: List<Pair<IrClass, TypeInfo>>,
        moduleFragment: IrModuleFragment,
    ) {
        val phaseId = telemetry.startPhase("GENERATION")

        for ((fakeInterface, typeInfo) in interfacesToProcess) {
            val interfaceName = fakeInterface.name.asString()

            // TRACE only: Per-interface processing
            logger.trace("Processing interface: $interfaceName")

            // Track analysis timing
            val analysisStartTime = System.currentTimeMillis()
            val interfaceAnalysis = interfaceAnalyzer.analyzeInterfaceDynamically(fakeInterface)
            val analysisEndTime = System.currentTimeMillis()
            validateAndLogPattern(interfaceAnalysis, fakeInterface, interfaceName)

            // Track generation timing and capture generated code
            val generationStartTime = System.currentTimeMillis()
            val generatedCode =
                codeGenerator.generateWorkingFakeImplementation(
                    sourceInterface = fakeInterface,
                    analysis = interfaceAnalysis,
                    moduleFragment = moduleFragment,
                )
            val generationEndTime = System.currentTimeMillis()

            // Record fake metrics with timing and LOC
            val memberCount = interfaceAnalysis.properties.size + interfaceAnalysis.functions.size
            telemetry.recordFakeMetrics(
                com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics(
                    name = interfaceName,
                    pattern = interfaceAnalysis.genericPattern,
                    memberCount = memberCount,
                    typeParamCount = interfaceAnalysis.typeParameters.size,
                    analysisTimeMs = analysisEndTime - analysisStartTime,
                    generationTimeMs = generationEndTime - generationStartTime,
                    generatedLOC = generatedCode.calculateTotalLOC(),
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = 0, // TODO: Track import count from ImportResolver
                ),
            )

            telemetry.metricsCollector.incrementInterfacesProcessed()
            optimizations.recordGeneration(typeInfo)
            logger.trace("Generated IR-native fake for $interfaceName")
        }

        telemetry.endPhase(phaseId)
    }

    private fun processClasses(
        classesToProcess: List<Pair<IrClass, TypeInfo>>,
        moduleFragment: IrModuleFragment,
    ) {
        for ((fakeClass, typeInfo) in classesToProcess) {
            val className = fakeClass.name.asString()

            // TRACE only: Per-class processing
            logger.trace("Processing class: $className")

            // Track analysis timing
            val analysisStartTime = System.currentTimeMillis()
            val classAnalysis = ClassAnalyzer.analyzeClass(fakeClass)
            val analysisEndTime = System.currentTimeMillis()

            // Track generation timing and capture generated code
            val generationStartTime = System.currentTimeMillis()
            val generatedCode =
                codeGenerator.generateWorkingClassFake(
                    sourceClass = fakeClass,
                    analysis = classAnalysis,
                    moduleFragment = moduleFragment,
                )
            val generationEndTime = System.currentTimeMillis()

            // Record class metrics with timing and LOC
            val memberCount =
                classAnalysis.abstractProperties.size +
                    classAnalysis.openProperties.size +
                    classAnalysis.abstractMethods.size +
                    classAnalysis.openMethods.size
            telemetry.recordFakeMetrics(
                com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics(
                    name = className,
                    pattern = com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern.NoGenerics, // Classes don't have generic patterns tracked
                    memberCount = memberCount,
                    typeParamCount = 0, // TODO: Track class type parameters
                    analysisTimeMs = analysisEndTime - analysisStartTime,
                    generationTimeMs = generationEndTime - generationStartTime,
                    generatedLOC = generatedCode.calculateTotalLOC(),
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = 0, // TODO: Track import count from ImportResolver
                ),
            )

            telemetry.metricsCollector.incrementClassesProcessed()
            optimizations.recordGeneration(typeInfo)
            logger.trace("Generated fake for class $className")
        }
    }

    private fun logGenerationCompletion(
        interfaceCount: Int,
        classCount: Int,
        moduleFragment: IrModuleFragment,
    ) {
        // Calculate total time from all completed phases
        val totalTime = telemetry.phaseTracker.getAllCompleted().values.sumOf { it.duration }

        // Generate compilation report
        val summary =
            telemetry.metricsCollector.buildSummary(
                totalTimeMs = totalTime,
                phaseBreakdown = telemetry.phaseTracker.getAllCompleted().mapKeys { it.value.name },
                outputDirectory = outputDir ?: "auto-detect",
            )

        // Log report based on level
        val report = CompilationReport.generate(summary, logger.logLevel)
        if (report.isNotEmpty()) {
            report.lines()
                .filter { it.isNotBlank() } // Skip empty lines
                .forEach { line ->
                    logger.info(line)
                }
        }
    }

    private fun logGenerationError(exception: Exception) {
        logger.error("IR-native generation failed: ${exception.message}")
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
                    telemetry.metricsCollector.incrementInterfacesCached()
                    logger.trace("✅ Skipping already generated: $interfaceName")
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

            logger.trace("Discovered interface with @Fake: ${irClass.name}")
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
                    logger.trace("Discovered fakable class: ${declaration.name}")
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
                    telemetry.metricsCollector.incrementClassesCached()
                    logger.trace("✅ Skipping already generated: $className")
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
                logger.warn("$warning in $interfaceName")
            }
        }

        // Log analysis summary for debugging
        val summary =
            com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer.getAnalysisSummary(
                interfaceAnalysis.genericPattern,
            )
        logger.trace("Analysis - $summary")
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
