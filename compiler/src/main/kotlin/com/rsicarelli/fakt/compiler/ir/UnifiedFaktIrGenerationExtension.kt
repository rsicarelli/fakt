// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir

import com.rsicarelli.fakt.compiler.FaktSharedContext
import com.rsicarelli.fakt.compiler.codegen.CodeGenerator
import com.rsicarelli.fakt.compiler.codegen.CodeGenerators
import com.rsicarelli.fakt.compiler.codegen.ConfigurationDslGenerator
import com.rsicarelli.fakt.compiler.codegen.FactoryGenerator
import com.rsicarelli.fakt.compiler.codegen.ImplementationGenerator
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalyzer
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalyzer.isFakableClass
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalyzer
import com.rsicarelli.fakt.compiler.ir.analysis.IrAnalysisHelper
import com.rsicarelli.fakt.compiler.ir.analysis.SourceSetExtractor
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
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import java.security.MessageDigest

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
 * **Phase 4 - FIR/IR Separation (v1.3.0)**:
 * - Receives FaktSharedContext for FIR→IR communication (Metro pattern)
 * - Accesses validated metadata from FIR phase via metadataStorage
 * - Eliminates redundant discovery/validation in IR phase
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
    private val sharedContext: FaktSharedContext,
) : IrGenerationExtension {
    // Extract fields from sharedContext
    private val outputDir: String? = sharedContext.options.outputDir
    private val fakeAnnotations: List<String> = sharedContext.fakeAnnotations

    private val optimizations = CompilerOptimizations(fakeAnnotations, outputDir, logger)

    companion object {
        private var headerLogged = false
        private val headerLock = Any()
    }

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
        logHeaderOnce()

        try {
            // Phase 4.2: Check if FIR analysis is enabled
            if (sharedContext.useFirAnalysis()) {
                // Use FIR metadata (Metro pattern)
                generateFromFirMetadata(moduleFragment, pluginContext)
            } else {
                // Legacy mode: IR discovery
                generateFromIrDiscovery(moduleFragment)
            }
        } catch (e: Exception) {
            logGenerationError(e)
        }
    }

    /**
     * Generate fakes from FIR metadata (Phase 3B.3 - Fixed anti-pattern).
     *
     * **Previous Anti-Pattern (Phase 4.3)**:
     * - Converted FIR metadata to IrClass instances
     * - Passed to processInterfaces() → analyzeInterfaceDynamically()
     * - Re-analyzed what FIR already validated (duplicate work!)
     *
     * **Phase 3B.3 Solution**:
     * - Uses FirToIrTransformer to transform FIR metadata → IrGenerationMetadata
     * - NO re-analysis of IrClass.declarations
     * - Follows Metro pattern: FIR analyzes, IR generates
     *
     * @param moduleFragment IR module for generation
     * @param pluginContext IR plugin context (for future enhancements)
     */
    private fun generateFromFirMetadata(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        logger.trace("Phase 3B.3: Generating code from FIR metadata (fixed anti-pattern)")

        // Load validated interfaces from FIR phase
        val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()
        val validatedClasses = sharedContext.metadataStorage.getAllClasses()

        logger.trace("FIR metadata loaded: ${validatedInterfaces.size} interfaces, ${validatedClasses.size} classes")

        if (validatedInterfaces.isEmpty() && validatedClasses.isEmpty()) {
            logger.trace("No validated interfaces or classes to generate")
            return
        }

        // Build ClassId → IrClass map for fast lookup
        val irClassMap = buildIrClassMap(moduleFragment)

        // Phase 3B.3: Transform FIR metadata → IrGenerationMetadata (NO re-analysis!)
        val transformer = FirToIrTransformer()

        val interfaceMetadata =
            validatedInterfaces.mapNotNull { firInterface ->
                val irClass = irClassMap[firInterface.classId]
                if (irClass == null) {
                    logger.warn("Could not find IrClass for validated interface: ${firInterface.classId.asFqNameString()}")
                    null
                } else {
                    logger.trace("Transforming FIR metadata for ${firInterface.simpleName}")
                    transformer.transform(firInterface, irClass)
                }
            }

        // TODO Phase 3B.3: Handle classes (not yet implemented)
        val classesToProcess =
            validatedClasses.mapNotNull { firClass ->
                val irClass = irClassMap[firClass.classId]
                if (irClass == null) {
                    logger.warn("Could not find IrClass for validated class: ${firClass.classId.asFqNameString()}")
                    null
                } else {
                    logger.trace("Matched FIR class ${firClass.simpleName} to IrClass")
                    irClass
                }
            }

        logger.info("Phase 3B.3: Transformed ${interfaceMetadata.size}/${validatedInterfaces.size} interfaces")

        // Phase 3B.3: Process using IrGenerationMetadata (NO analyzeInterfaceDynamically!)
        if (interfaceMetadata.isNotEmpty()) {
            processInterfacesFromMetadata(interfaceMetadata, moduleFragment)
        }

        // Phase 3C.1: Transform classes using FirToIrTransformer
        val classMetadata =
            validatedClasses.mapNotNull { firClassMetadata ->
                val irClass = irClassMap[firClassMetadata.classId]
                if (irClass == null) {
                    logger.warn(
                        "Phase 3C.1: IrClass not found for ${firClassMetadata.simpleName}. " +
                            "Skipping class transformation.",
                    )
                    null
                } else {
                    transformer.transformClass(firClassMetadata, irClass)
                }
            }

        logger.info("Phase 3C.1: Transformed ${classMetadata.size}/${validatedClasses.size} classes")

        // Phase 3C.1: Process using IrClassGenerationMetadata (NO analyzeClass!)
        if (classMetadata.isNotEmpty()) {
            processClassesFromMetadata(classMetadata, moduleFragment)
        }

        logGenerationCompletion(interfaceMetadata.size, classMetadata.size, moduleFragment)
    }

    /**
     * Build a map of ClassId → IrClass for fast lookup.
     *
     * Walks the entire module fragment to find all classes/interfaces.
     *
     * @param moduleFragment IR module to scan
     * @return Map from ClassId to IrClass
     */
    private fun buildIrClassMap(moduleFragment: IrModuleFragment): Map<org.jetbrains.kotlin.name.ClassId, IrClass> {
        val map = mutableMapOf<org.jetbrains.kotlin.name.ClassId, IrClass>()

        moduleFragment.files.forEach { file ->
            file.declarations.filterIsInstance<IrClass>().forEach { irClass ->
                val classId =
                    org.jetbrains.kotlin.name.ClassId(
                        irClass.packageFqName ?: org.jetbrains.kotlin.name.FqName.ROOT,
                        org.jetbrains.kotlin.name
                            .FqName(irClass.name.asString()),
                        false, // isLocal
                    )
                map[classId] = irClass

                // Also add nested classes
                irClass.declarations.filterIsInstance<IrClass>().forEach { nestedClass ->
                    val nestedClassId =
                        org.jetbrains.kotlin.name.ClassId(
                            irClass.packageFqName ?: org.jetbrains.kotlin.name.FqName.ROOT,
                            org.jetbrains.kotlin.name
                                .FqName("${irClass.name.asString()}.${nestedClass.name.asString()}"),
                            false,
                        )
                    map[nestedClassId] = nestedClass
                }
            }
        }

        logger.trace("Built IR class map with ${map.size} classes")
        return map
    }

    /**
     * Generate fakes from IR discovery (legacy mode).
     *
     * This is the original implementation that discovers @Fake annotations
     * and analyzes interfaces directly in IR phase.
     *
     * @param moduleFragment IR module for generation
     */
    private fun generateFromIrDiscovery(moduleFragment: IrModuleFragment) {
        val (fakeInterfaces, fakeClasses) = discoverAndLogFakes(moduleFragment) ?: return

        val interfacesToProcess = filterInterfacesToProcess(fakeInterfaces)
        val classesToProcess = filterClassesToProcess(fakeClasses)

        processInterfaces(interfacesToProcess, moduleFragment)
        processClasses(classesToProcess, moduleFragment)

        logGenerationCompletion(interfacesToProcess.size, classesToProcess.size, moduleFragment)
    }

    private fun discoverAndLogFakes(moduleFragment: IrModuleFragment): Pair<List<IrClass>, List<IrClass>>? {
        val phaseId = telemetry.startPhase("DISCOVERY")

        val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
        val fakeClasses = discoverFakeClasses(moduleFragment)

        telemetry.metricsCollector.incrementInterfacesDiscovered(fakeInterfaces.size)
        telemetry.metricsCollector.incrementClassesDiscovered(fakeClasses.size)

        telemetry.endPhase(phaseId)

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

            // Track analysis timing
            val analysisStartTime = System.nanoTime()
            val interfaceAnalysis = interfaceAnalyzer.analyzeInterfaceDynamically(fakeInterface)
            val analysisEndTime = System.nanoTime()
            validateAndLogPattern(interfaceAnalysis, fakeInterface, interfaceName)

            // Track generation timing and capture generated code
            val generationStartTime = System.nanoTime()
            val generatedCode =
                codeGenerator.generateWorkingFakeImplementation(
                    sourceInterface = fakeInterface,
                    analysis = interfaceAnalysis,
                    moduleFragment = moduleFragment,
                )
            val generationEndTime = System.nanoTime()

            // Calculate metrics
            val loc = generatedCode.calculateTotalLOC()
            val analysisTime = analysisEndTime - analysisStartTime
            val generationTime = generationEndTime - generationStartTime

            // Record fake metrics
            val memberCount = interfaceAnalysis.properties.size + interfaceAnalysis.functions.size
            telemetry.recordFakeMetrics(
                com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics(
                    name = interfaceName,
                    pattern = interfaceAnalysis.genericPattern,
                    memberCount = memberCount,
                    typeParamCount = interfaceAnalysis.typeParameters.size,
                    analysisTimeNanos = analysisTime,
                    generationTimeNanos = generationTime,
                    generatedLOC = loc,
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = 0, // TODO: Track import count from ImportResolver
                ),
            )

            telemetry.metricsCollector.incrementInterfacesProcessed()
            optimizations.recordGeneration(typeInfo)

            // TRACE: Log tree-style per-interface processing
            if (logger.logLevel >= com.rsicarelli.fakt.compiler.api.LogLevel.TRACE) {
                val packageName = fakeInterface.packageFqName?.asString() ?: ""
                val packagePath = packageName.replace('.', '/')
                val fakeFileName = "Fake${interfaceName}Impl.kt"
                val relativePath = if (packagePath.isNotEmpty()) "$packagePath/$fakeFileName" else fakeFileName
                val outputPath = if (outputDir != null) "$outputDir/$relativePath" else relativePath

                val analysisDetail =
                    buildString {
                        val typeParamCount = interfaceAnalysis.typeParameters.size
                        if (typeParamCount > 0) {
                            append("$typeParamCount type parameters, ")
                        }
                        append("$memberCount members")
                    }

                logFakeProcessing(
                    name = interfaceName,
                    analysisTimeNanos = analysisTime,
                    generationTimeNanos = generationTime,
                    loc = loc,
                    outputPath = outputPath,
                    analysisDetail = analysisDetail,
                )
            }
        }

        telemetry.endPhase(phaseId)
    }

    /**
     * Process interfaces from IrGenerationMetadata (Phase 3B.3 - NO re-analysis).
     *
     * This method replaces the anti-pattern of passing IrClass to processInterfaces(),
     * which triggered analyzeInterfaceDynamically() and re-analyzed what FIR already did.
     *
     * **Key Difference from processInterfaces()**:
     * - ❌ NO call to analyzeInterfaceDynamically() (FIR already analyzed!)
     * - ✅ Uses IrGenerationMetadata.toInterfaceAnalysis() adapter
     * - ✅ Passes directly to code generators
     * - ✅ Follows Metro pattern: FIR analyzes, IR generates
     *
     * @param interfaceMetadata List of transformed FIR metadata (IrTypes + IR nodes)
     * @param moduleFragment Module for file creation
     */
    private fun processInterfacesFromMetadata(
        interfaceMetadata: List<IrGenerationMetadata>,
        moduleFragment: IrModuleFragment,
    ) {
        val phaseId = telemetry.startPhase("GENERATION")

        for (metadata in interfaceMetadata) {
            val interfaceName = metadata.interfaceName

            // Phase 3B.3: Convert to InterfaceAnalysis using adapter (NO re-analysis!)
            val analysisStartTime = System.nanoTime()
            val interfaceAnalysis = metadata.toInterfaceAnalysis()
            val analysisEndTime = System.nanoTime()
            val analysisTime = analysisEndTime - analysisStartTime // Adapter overhead only (~1μs)

            // Validate pattern (reuses existing validation logic)
            validateAndLogPattern(interfaceAnalysis, metadata.sourceInterface, interfaceName)

            // Track generation timing and capture generated code
            val generationStartTime = System.nanoTime()
            val generatedCode =
                codeGenerator.generateWorkingFakeImplementation(
                    sourceInterface = metadata.sourceInterface,
                    analysis = interfaceAnalysis,
                    moduleFragment = moduleFragment,
                )
            val generationEndTime = System.nanoTime()

            // Calculate metrics
            val loc = generatedCode.calculateTotalLOC()
            val generationTime = generationEndTime - generationStartTime

            // Record fake metrics
            val memberCount = interfaceAnalysis.properties.size + interfaceAnalysis.functions.size
            telemetry.recordFakeMetrics(
                com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics(
                    name = interfaceName,
                    pattern = interfaceAnalysis.genericPattern,
                    memberCount = memberCount,
                    typeParamCount = interfaceAnalysis.typeParameters.size,
                    analysisTimeNanos = analysisTime, // Adapter overhead, not FIR analysis time
                    generationTimeNanos = generationTime,
                    generatedLOC = loc,
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = 0, // TODO: Track import count from ImportResolver
                ),
            )

            telemetry.metricsCollector.incrementInterfacesProcessed()

            // TODO Phase 3B.3: Add cache support for FIR mode (skip for now)
            // optimizations.recordGeneration(typeInfo)

            // TRACE: Log tree-style per-interface processing
            if (logger.logLevel >= com.rsicarelli.fakt.compiler.api.LogLevel.TRACE) {
                val packageName = metadata.packageName
                val packagePath = packageName.replace('.', '/')
                val fakeFileName = "Fake${interfaceName}Impl.kt"
                val relativePath = if (packagePath.isNotEmpty()) "$packagePath/$fakeFileName" else fakeFileName
                val outputPath = if (outputDir != null) "$outputDir/$relativePath" else relativePath

                val analysisDetail =
                    buildString {
                        val typeParamCount = interfaceAnalysis.typeParameters.size
                        if (typeParamCount > 0) {
                            append("$typeParamCount type parameters, ")
                        }
                        append("$memberCount members")
                    }

                logFakeProcessing(
                    name = interfaceName,
                    analysisTimeNanos = analysisTime,
                    generationTimeNanos = generationTime,
                    loc = loc,
                    outputPath = outputPath,
                    analysisDetail = analysisDetail,
                )
            }
        }

        telemetry.endPhase(phaseId)
    }

    /**
     * Process abstract classes from IrClassGenerationMetadata (Phase 3C.1 - NO re-analysis).
     *
     * Similar to processInterfacesFromMetadata() but handles abstract classes with
     * separation of abstract/open members.
     *
     * **Key Features**:
     * - ❌ NO call to analyzeClass() (FIR already analyzed!)
     * - ✅ Uses IrClassGenerationMetadata.toInterfaceAnalysis() adapter
     * - ✅ Passes directly to code generators (treats abstract+open members the same)
     * - ✅ Follows Metro pattern: FIR analyzes, IR generates
     *
     * **Phase 3C.1 Note**: Currently combines abstract and open members since existing
     * generators treat all members the same way (all need implementation/override).
     * Future phases may enhance this to support super delegation for open members.
     *
     * @param classMetadata List of transformed FIR class metadata (IrTypes + IR nodes)
     * @param moduleFragment Module for file creation
     */
    private fun processClassesFromMetadata(
        classMetadata: List<IrClassGenerationMetadata>,
        moduleFragment: IrModuleFragment,
    ) {
        val phaseId = telemetry.startPhase("GENERATION")

        for (metadata in classMetadata) {
            val className = metadata.className

            // Phase 3C.5: Convert to ClassAnalysis using adapter (preserves abstract/open distinction)
            val analysisStartTime = System.nanoTime()
            val classAnalysis = metadata.toClassAnalysis()
            val analysisEndTime = System.nanoTime()
            val analysisTime = analysisEndTime - analysisStartTime // Adapter overhead only (~1μs)

            // Validate pattern (ClassAnalysis has genericPattern property via IrGenerationMetadata)
            // validateAndLogPattern requires InterfaceAnalysis, so we skip it for now
            // Phase 3C.5: TODO - add class-specific validation if needed

            // Track generation timing and capture generated code
            val generationStartTime = System.nanoTime()
            val generatedCode =
                codeGenerator.generateWorkingClassFake(
                    sourceClass = metadata.sourceClass,
                    analysis = classAnalysis,
                    moduleFragment = moduleFragment,
                )
            val generationEndTime = System.nanoTime()

            // Calculate metrics
            val loc = generatedCode.calculateTotalLOC()
            val generationTime = generationEndTime - generationStartTime

            // Record fake metrics
            // Phase 3C.5: Count all members (abstract + open)
            val memberCount = (
                classAnalysis.abstractProperties.size + classAnalysis.openProperties.size +
                    classAnalysis.abstractMethods.size + classAnalysis.openMethods.size
            )
            telemetry.recordFakeMetrics(
                com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics(
                    name = className,
                    pattern = metadata.genericPattern, // Get from metadata, not classAnalysis
                    memberCount = memberCount,
                    typeParamCount = classAnalysis.typeParameters.size,
                    analysisTimeNanos = analysisTime, // Adapter overhead, not FIR analysis time
                    generationTimeNanos = generationTime,
                    generatedLOC = loc,
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = 0, // TODO: Track import count from ImportResolver
                ),
            )

            telemetry.metricsCollector.incrementClassesProcessed()

            // TRACE: Log tree-style per-class processing
            if (logger.logLevel >= com.rsicarelli.fakt.compiler.api.LogLevel.TRACE) {
                val packageName = metadata.packageName
                val packagePath = packageName.replace('.', '/')
                val fakeFileName = "Fake${className}Impl.kt"
                val relativePath = if (packagePath.isNotEmpty()) "$packagePath/$fakeFileName" else fakeFileName
                val outputPath = if (outputDir != null) "$outputDir/$relativePath" else relativePath

                val analysisDetail =
                    buildString {
                        val typeParamCount = classAnalysis.typeParameters.size
                        if (typeParamCount > 0) {
                            append("$typeParamCount type parameters, ")
                        }
                        // Phase 3C.5: Count all properties and methods (abstract + open)
                        val propCount = classAnalysis.abstractProperties.size + classAnalysis.openProperties.size
                        val methodCount = classAnalysis.abstractMethods.size + classAnalysis.openMethods.size
                        append("$propCount properties, ")
                        append("$methodCount methods")
                    }

                logger.trace(
                    "  ├─ $className ($analysisDetail)\n" +
                        "  │  Pattern: ${metadata.genericPattern.javaClass.simpleName}\n" + // Get from metadata
                        "  │  Output: $outputPath\n" +
                        "  │  LOC: $loc lines\n" +
                        "  │  Analysis time: ${analysisTime / 1_000}μs (adapter only)\n" +
                        "  │  Generation time: ${generationTime / 1_000_000}ms",
                )
            }
        }

        telemetry.endPhase(phaseId)
    }

    private fun processClasses(
        classesToProcess: List<Pair<IrClass, TypeInfo>>,
        moduleFragment: IrModuleFragment,
    ) {
        for ((fakeClass, typeInfo) in classesToProcess) {
            val className = fakeClass.name.asString()

            // Track analysis timing
            val analysisStartTime = System.nanoTime()
            val classAnalysis = ClassAnalyzer.analyzeClass(fakeClass)
            val analysisEndTime = System.nanoTime()
            val analysisTime = analysisEndTime - analysisStartTime

            // Track generation timing and capture generated code
            val generationStartTime = System.nanoTime()
            val generatedCode =
                codeGenerator.generateWorkingClassFake(
                    sourceClass = fakeClass,
                    analysis = classAnalysis,
                    moduleFragment = moduleFragment,
                )
            val generationEndTime = System.nanoTime()
            val generationTime = generationEndTime - generationStartTime

            // Record class metrics with timing and LOC
            val memberCount =
                classAnalysis.abstractProperties.size +
                    classAnalysis.openProperties.size +
                    classAnalysis.abstractMethods.size +
                    classAnalysis.openMethods.size
            val loc = generatedCode.calculateTotalLOC()

            telemetry.recordFakeMetrics(
                com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics(
                    name = className,
                    // Classes don't have generic patterns tracked
                    pattern = com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern.NoGenerics,
                    memberCount = memberCount,
                    typeParamCount = 0, // TODO: Track class type parameters
                    analysisTimeNanos = analysisTime,
                    generationTimeNanos = generationTime,
                    generatedLOC = loc,
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = 0, // TODO: Track import count from ImportResolver
                ),
            )

            telemetry.metricsCollector.incrementClassesProcessed()
            optimizations.recordGeneration(typeInfo)

            // TRACE: Log tree-style per-class processing
            if (logger.logLevel >= com.rsicarelli.fakt.compiler.api.LogLevel.TRACE) {
                val packageName = fakeClass.packageFqName?.asString() ?: ""
                val packagePath = packageName.replace('.', '/')
                val fakeFileName = "Fake${className}Impl.kt"
                val relativePath = if (packagePath.isNotEmpty()) "$packagePath/$fakeFileName" else fakeFileName
                val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment, SourceSetExtractor.extractSourceSet(fakeClass))
                val outputPath = if (outputDir != null) "$outputDir/$relativePath" else relativePath

                val analysisDetail =
                    buildString {
                        val parts = mutableListOf<String>()
                        if (classAnalysis.abstractProperties.isNotEmpty()) {
                            parts.add("${classAnalysis.abstractProperties.size} abstract props")
                        }
                        if (classAnalysis.openProperties.isNotEmpty()) {
                            parts.add("${classAnalysis.openProperties.size} open props")
                        }
                        if (classAnalysis.abstractMethods.isNotEmpty()) {
                            parts.add("${classAnalysis.abstractMethods.size} abstract methods")
                        }
                        if (classAnalysis.openMethods.isNotEmpty()) {
                            parts.add("${classAnalysis.openMethods.size} open methods")
                        }
                        append(parts.joinToString(", "))
                    }

                logFakeProcessing(
                    name = className,
                    analysisTimeNanos = analysisTime,
                    generationTimeNanos = generationTime,
                    loc = loc,
                    outputPath = outputPath,
                    analysisDetail = analysisDetail,
                )
            }
        }
    }

    private fun logHeaderOnce() {
        synchronized(headerLock) {
            if (headerLogged) return
            headerLogged = true

            logger.trace("════════════════════════════════════════════════════════════")
            logger.trace("Fakt Plugin initialized")
            logger.trace("├─ enabled: true")
            logger.trace("├─ logLevel: ${logger.logLevel}")
            logger.trace("├─ detectedAnnotations: ${fakeAnnotations.joinToString(", ")}")
            if (outputDir != null) {
                val simplifiedPath =
                    outputDir
                        .substringAfter("/ktfake/samples/", "")
                        .ifEmpty { outputDir }
                logger.trace("├─ output: $simplifiedPath")
            }
            logger.trace("└─ cache: ${optimizations.cacheSize()} signatures loaded")
            logger.trace("════════════════════════════════════════════════════════════")
        }
    }

    private fun logGenerationCompletion(
        interfaceCount: Int,
        classCount: Int,
        moduleFragment: IrModuleFragment,
    ) {
        // Calculate total time from all completed phases
        val totalTime =
            telemetry.phaseTracker
                .getAllCompleted()
                .values
                .sumOf { it.duration }

        // Generate compilation report
        val summary =
            telemetry.metricsCollector.buildSummary(
                totalTimeNanos = totalTime,
                phaseBreakdown = telemetry.phaseTracker.getAllCompleted().mapKeys { it.value.name },
                outputDirectory = outputDir ?: "auto-detect",
            )

        // Log report based on level
        val report = CompilationReport.generate(summary, logger.logLevel)
        if (report.isNotEmpty()) {
            report
                .lines()
                .filter { it.isNotBlank() } // Skip empty lines
                .forEach { line ->
                    // Use trace() for TRACE level to avoid "Fakt:" prefix
                    if (logger.logLevel >= com.rsicarelli.fakt.compiler.api.LogLevel.TRACE) {
                        logger.trace(line)
                    } else {
                        logger.info(line)
                    }
                }
        }
    }

    private fun logGenerationError(exception: Exception) {
        logger.error("IR-native generation failed: ${exception.message}")
    }

    /**
     * Creates TypeInfo for incremental compilation tracking.
     *
     * **Performance**: Signature computation is lazy - computed only when checking cache.
     * This avoids 3-4ms overhead during discovery phase for interfaces that won't be processed.
     *
     * @param fakeInterface The IR class (interface or class) to create TypeInfo for
     * @return TypeInfo containing type metadata with lazy signature computation
     */
    private fun createTypeInfo(fakeInterface: IrClass): TypeInfo {
        val typeName = fakeInterface.name.asString()
        val fullyQualifiedName = fakeInterface.kotlinFqName.asString()
        // Use FQN as fileName since we don't have direct access to IrFile from IrClass
        // This is sufficient for incremental compilation tracking
        val fileName = "$fullyQualifiedName.kt"

        return TypeInfo(
            name = typeName,
            fullyQualifiedName = fullyQualifiedName,
            packageName = fakeInterface.packageFqName?.asString() ?: "",
            fileName = fileName,
            annotations = fakeInterface.annotations.mapNotNull { it.type.classFqName?.asString() },
            signature = "", // Placeholder - computed lazily when needed
        )
    }

    /**
     * Computes signature for cache checking only when needed (lazy evaluation).
     *
     * @param irClass The IR class to compute signature for
     * @return MD5 hash of the structural signature
     */
    private fun computeSignatureForCacheCheck(irClass: IrClass): String = computeTypeSignature(irClass)

    /**
     * Filters interfaces to determine which need fake generation.
     * Skips unchanged interfaces (incremental compilation) and unsupported generic interfaces.
     *
     * **Performance**: Signature is computed lazily only during cache check, not during discovery.
     * This moves the 3-4ms overhead from discovery to filtering phase and only for interfaces being checked.
     *
     * @param fakeInterfaces All discovered @Fake interfaces
     * @return List of interfaces paired with their TypeInfo that need processing
     */
    private fun filterInterfacesToProcess(
        fakeInterfaces: List<IrClass>,
    ): List<Pair<IrClass, com.rsicarelli.fakt.compiler.types.TypeInfo>> =
        fakeInterfaces.mapNotNull { fakeInterface ->
            val interfaceName = fakeInterface.name.asString()

            // Lazy signature computation: compute only when checking cache
            val signature = computeSignatureForCacheCheck(fakeInterface)
            val typeInfoWithSignature = createTypeInfo(fakeInterface).copy(signature = signature)

            // Phase 2: Detect generic pattern
            val genericPattern =
                com.rsicarelli.fakt.compiler.ir.analysis
                    .GenericPatternAnalyzer()
                    .analyzeInterface(fakeInterface)

            when {
                !optimizations.needsRegeneration(typeInfoWithSignature) -> {
                    telemetry.metricsCollector.incrementInterfacesCached()
                    null
                }

                // ✅ PHASE 2: Class-level generics supported!
                // ✅ PHASE 3: Method-level generics supported!
                // ✅ PHASE 3: Mixed generics (class + method) now enabled!
                // All patterns (NoGenerics, ClassLevelGenerics, MethodLevelGenerics, MixedGenerics) will be processed
                else -> fakeInterface to typeInfoWithSignature
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
                annotationFqName != null && optimizations.isConfiguredFor(annotationFqName)
            }

        if (matchingAnnotation != null) {
            discoveredInterfaces.add(irClass)

            // Index type for optimization tracking
            val typeInfo = createTypeInfo(irClass)
            optimizations.indexType(typeInfo)
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
                }
            }
        }

        return discoveredClasses
    }

    /**
     * Filters classes to determine which need fake generation.
     * Skips unchanged classes (incremental compilation).
     *
     * **Performance**: Signature is computed lazily only during cache check.
     *
     * @param fakeClasses All discovered @Fake classes
     * @return List of classes paired with their TypeInfo that need processing
     */
    private fun filterClassesToProcess(fakeClasses: List<IrClass>): List<Pair<IrClass, TypeInfo>> =
        fakeClasses.mapNotNull { fakeClass ->
            val className = fakeClass.name.asString()

            // Lazy signature computation: compute only when checking cache
            val signature = computeSignatureForCacheCheck(fakeClass)
            val typeInfoWithSignature = createTypeInfo(fakeClass).copy(signature = signature)

            when {
                !optimizations.needsRegeneration(typeInfoWithSignature) -> {
                    telemetry.metricsCollector.incrementClassesCached()
                    null
                }
                else -> fakeClass to typeInfoWithSignature
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
    }

    /**
     * Computes a comprehensive signature for a type (interface or class) to enable accurate change detection.
     *
     * This signature captures the complete structural contract including:
     * - Type FQN and type parameters with bounds
     * - All properties: name, type, mutability, nullability
     * - All functions: name, parameters (names + types), return type, modifiers
     * - Deterministic ordering (alphabetical) for stability
     *
     * The signature is hashed with MD5 for efficient storage and comparison.
     * ANY structural change invalidates the cache, ensuring safety.
     *
     * Performance: ~1-5 microseconds per interface (negligible overhead)
     *
     * @param irClass The IR class (interface or class) to compute signature for
     * @return MD5 hash of the structural signature (32 characters)
     */
    private fun computeTypeSignature(irClass: IrClass): String {
        val signature =
            buildString {
                // 1. Type FQN and kind
                val kind = if (irClass.kind == org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE) "interface" else "class"
                append("$kind ${irClass.kotlinFqName}")

                // 2. Type parameters with bounds (sorted for determinism)
                val typeParams =
                    irClass.typeParameters
                        .map { typeParam ->
                            val bounds =
                                typeParam.superTypes.joinToString(",") { bound ->
                                    typeResolver.irTypeToKotlinString(bound)
                                }
                            if (bounds.isNotEmpty()) {
                                "${typeParam.name}:$bounds"
                            } else {
                                typeParam.name.asString()
                            }
                        }.sorted()

                if (typeParams.isNotEmpty()) {
                    append("|typeParams:<${typeParams.joinToString(",")}>")
                }

                // 3. Properties (sorted alphabetically by name)
                val properties =
                    irClass.declarations
                        .filterIsInstance<IrProperty>()
                        .map { property ->
                            val name = property.name.asString()
                            val type = typeResolver.irTypeToKotlinString(property.getter?.returnType ?: property.backingField?.type!!)
                            val mutability = if (property.isVar) "var" else "val"
                            val nullability = if (property.getter?.returnType?.isMarkedNullable() == true) "nullable" else "nonNull"
                            "$name:$type:$mutability:$nullability"
                        }.sorted()

                if (properties.isNotEmpty()) {
                    append("|properties:${properties.size}|${properties.joinToString("|")}")
                }

                // 4. Functions (sorted alphabetically by name)
                val functions =
                    irClass.declarations
                        .filterIsInstance<IrSimpleFunction>()
                        .filterNot { IrAnalysisHelper.isSpecialFunction(it) }
                        .map { function ->
                            val name = function.name.asString()

                            // Function parameters with names and types (using non-deprecated API)
                            val params =
                                function.parameters
                                    .filter { it.kind == IrParameterKind.Regular }
                                    .joinToString(",") { param ->
                                        val paramName = param.name.asString()
                                        val paramType = typeResolver.irTypeToKotlinString(param.type)
                                        val vararg = if (param.varargElementType != null) "vararg:" else ""
                                        val default = if (param.defaultValue != null) ":default" else ""
                                        "$vararg$paramName:$paramType$default"
                                    }

                            // Return type
                            val returnType = typeResolver.irTypeToKotlinString(function.returnType)

                            // Modifiers
                            val suspend = if (function.isSuspend) "suspend" else ""
                            val inline = if (function.isInline) "inline" else ""
                            val modifiers = listOf(suspend, inline).filter { it.isNotEmpty() }.joinToString(",")

                            // Method-level type parameters
                            val methodTypeParams = function.typeParameters.map { it.name.asString() }.sorted()
                            val typeParamsStr =
                                if (methodTypeParams.isNotEmpty()) {
                                    "<${methodTypeParams.joinToString(",")}>"
                                } else {
                                    ""
                                }

                            "$name$typeParamsStr($params):$returnType${if (modifiers.isNotEmpty()) ":$modifiers" else ""}"
                        }.sorted()

                if (functions.isNotEmpty()) {
                    append("|functions:${functions.size}|${functions.joinToString("|")}")
                }
            }

        // Hash the signature with MD5 for efficient storage (32 chars)
        return signature.toMD5Hash()
    }

    /**
     * Computes MD5 hash of a string for efficient signature storage.
     *
     * Creates a new MessageDigest instance for thread safety.
     * Performance: ~1-2 microseconds per call (negligible overhead).
     *
     * @return 32-character hexadecimal MD5 hash
     */
    private fun String.toMD5Hash(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Logs fake processing in tree-style format for TRACE level.
     *
     * Consolidates all per-fake information into clean hierarchy:
     * ```
     * FakeName
     * ├─ Analysis: details (timing)
     * ├─ Generation: FakeFakeNameImpl (LOC, timing)
     * └─ Output: /path/to/file
     * ```
     *
     * @param name Interface or class name
     * @param analysisTimeNanos Analysis phase duration in nanoseconds
     * @param generationTimeNanos Generation phase duration in nanoseconds
     * @param loc Lines of code generated
     * @param outputPath Absolute path to generated file
     * @param analysisDetail Optional analysis details (e.g., "3 type parameters, 5 members")
     */
    private fun logFakeProcessing(
        name: String,
        analysisTimeNanos: Long,
        generationTimeNanos: Long,
        loc: Int,
        outputPath: String,
        analysisDetail: String = "",
    ) {
        logger.trace("$name")
        val analysisTime =
            com.rsicarelli.fakt.compiler.api.TimeFormatter
                .format(analysisTimeNanos)
        val generationTime =
            com.rsicarelli.fakt.compiler.api.TimeFormatter
                .format(generationTimeNanos)

        if (analysisDetail.isNotEmpty()) {
            logger.trace("├─ Analysis: $analysisDetail ($analysisTime)")
        } else {
            logger.trace("├─ Analysis: $analysisTime")
        }

        logger.trace("├─ Generation: Fake${name}Impl ($loc LOC, $generationTime)")
        logger.trace("└─ Output: $outputPath")
    }
}
