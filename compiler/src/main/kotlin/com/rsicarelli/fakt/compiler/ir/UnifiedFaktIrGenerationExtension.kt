// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir

import com.rsicarelli.fakt.compiler.FaktSharedContext
import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.codegen.CodeGenerator
import com.rsicarelli.fakt.compiler.codegen.CodeGenerators
import com.rsicarelli.fakt.compiler.codegen.ConfigurationDslGenerator
import com.rsicarelli.fakt.compiler.codegen.FactoryGenerator
import com.rsicarelli.fakt.compiler.codegen.ImplementationGenerator
import com.rsicarelli.fakt.compiler.ir.analysis.SourceSetExtractor
import com.rsicarelli.fakt.compiler.ir.utils.IrGenerationLogging
import com.rsicarelli.fakt.compiler.ir.utils.validateAndLogGenericPattern
import com.rsicarelli.fakt.compiler.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.optimization.buildSignature
import com.rsicarelli.fakt.compiler.output.SourceSetMapper
import com.rsicarelli.fakt.compiler.types.TypeInfo
import java.io.File
import com.rsicarelli.fakt.compiler.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.telemetry.FaktTelemetry
import com.rsicarelli.fakt.compiler.telemetry.metrics.FakeMetrics
import com.rsicarelli.fakt.compiler.types.ImportResolver
import com.rsicarelli.fakt.compiler.types.TypeResolver
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * FIR-based fake generation following the Metro pattern.
 *
 * This implementation follows the "FIR analyzes, IR generates" architectural pattern:
 * - FIR phase (FaktFirExtensionRegistrar) validates @Fake annotations and extracts metadata
 * - IR phase (this class) transforms FIR metadata to IrTypes and generates code
 * - NO redundant analysis or validation occurs in IR phase
 *
 * **Architecture**:
 * - Receives FaktSharedContext for FIR→IR communication (Metro pattern)
 * - Accesses validated metadata from FIR phase via metadataStorage
 * - Uses FirToIrTransformer to convert FIR metadata → IrGenerationMetadata
 * - Generates Kotlin code using CodeGenerator modules
 *
 * **Code Generation**:
 * For each @Fake annotated interface/class, generates:
 * - FakeXxxImpl.kt - Implementation class with configurable behavior properties
 * - fakeXxx() - Type-safe factory function
 * - FakeXxxConfig - Configuration DSL class
 *
 * ## Threading Model
 *
 * **Thread Safety**: This extension is designed to be thread-safe:
 * - FirMetadataStorage uses ConcurrentHashMap for thread-safe access
 * - Telemetry uses synchronized collections for concurrent metric updates
 * - Logging utilities use synchronized blocks to ensure single initialization
 * - All instance state is effectively immutable after construction
 *
 * **Concurrency**: Kotlin compiler may process modules in parallel. This extension
 * handles concurrent invocations safely through synchronized state management.
 *
 * @property logger Logger for compilation feedback at configured verbosity level
 * @property sharedContext Shared context for FIR→IR communication and configuration
 */
class UnifiedFaktIrGenerationExtension(
    private val logger: FaktLogger,
    private val sharedContext: FaktSharedContext,
) : IrGenerationExtension {
    // Extract fields from sharedContext
    private val outputDir: String? = sharedContext.options.outputDir
    private val fakeAnnotations: List<String> = sharedContext.fakeAnnotations

    private val optimizations = CompilerOptimizations(
        fakeAnnotations = fakeAnnotations,
        outputDir = outputDir,
        logger = logger
    )

    // Extracted modules following DRY principles
    private val typeResolver = TypeResolver()
    private val importResolver = ImportResolver(typeResolver)

    private val sourceSetMapper =
        SourceSetMapper(
            outputDir = outputDir,
            logger = logger,
        )

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

    private val telemetry = FaktTelemetry.initialize(logger)

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        IrGenerationLogging.logHeaderOnce(
            logger = logger,
            fakeAnnotations = fakeAnnotations,
            outputDir = outputDir,
            optimizations = optimizations
        )

        try {
            generateFromFirMetadata(moduleFragment)
        } catch (e: Exception) {
            IrGenerationLogging.logGenerationError(logger, e)
        }
    }

    /**
     * Generate fakes from FIR metadata following the Metro pattern.
     *
     * This method implements the "FIR analyzes, IR generates" architectural pattern where:
     * 1. FIR phase already validated and extracted structural metadata
     * 2. IR phase transforms FIR strings → IrTypes and generates code
     * 3. NO redundant analysis or validation occurs
     *
     * **Previous Anti-Pattern (Fixed in v1.3.0)**:
     * - ❌ Converted FIR metadata to IrClass instances
     * - ❌ Passed to processInterfaces() → analyzeInterfaceDynamically()
     * - ❌ Re-analyzed what FIR already validated (duplicate work!)
     *
     * **Current Solution**:
     * - ✅ Uses FirToIrTransformer to transform FIR metadata → IrGenerationMetadata
     * - ✅ NO re-analysis of IrClass.declarations
     * - ✅ Follows Metro pattern: FIR analyzes, IR generates
     *
     * **Performance**:
     * - ClassId → IrClass mapping: O(n) where n = total classes in module
     * - FIR → IR transformation: O(m) where m = @Fake declarations
     * - Typical cost: < 10ms for 100 interfaces
     * - Transformation overhead: ~100-500μs per interface (IrType resolution only)
     *
     * **Error Handling**:
     * - Logs warning if IrClass not found for validated FIR metadata (indicates compiler bug)
     * - Skips unmatchable interfaces/classes and continues processing others
     * - Never throws exceptions - handles errors gracefully
     *
     * @param moduleFragment IR module for code generation context and file creation
     *
     * @see FirToIrTransformer for the transformation logic
     * @see processInterfacesFromMetadata for generation without re-analysis
     * @see processClassesFromMetadata for class generation
     */
    private fun generateFromFirMetadata(
        moduleFragment: IrModuleFragment,
    ) {
        logger.trace("Generating code from FIR metadata (fixed anti-pattern)")

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

        // Transform FIR metadata → IrGenerationMetadata (NO re-analysis!)
        val transformer = FirToIrTransformer()

        val interfaceMetadata = validatedInterfaces.mapNotNull { firInterface ->
            val irClass = irClassMap[firInterface.classId]
            if (irClass == null) {
                logger.warn("Could not find IrClass for validated interface: ${firInterface.classId.asFqNameString()}")
                null
            } else {
                logger.trace("Transforming FIR metadata for ${firInterface.simpleName}")
                transformer.transform(firInterface, irClass)
            }
        }

        logger.info("Transformed ${interfaceMetadata.size}/${validatedInterfaces.size} interfaces")

        // Process using IrGenerationMetadata (NO analyzeInterfaceDynamically!)
        if (interfaceMetadata.isNotEmpty()) {
            processInterfacesFromMetadata(interfaceMetadata, moduleFragment)
        }

        // Transform classes using FirToIrTransformer
        val classMetadata = validatedClasses.mapNotNull { firClassMetadata ->
            val irClass = irClassMap[firClassMetadata.classId]
            if (irClass == null) {
                logger.warn(
                    "IrClass not found for ${firClassMetadata.simpleName}. " +
                            "Skipping class transformation.",
                )
                null
            } else {
                transformer.transformClass(firClassMetadata, irClass)
            }
        }

        logger.info("Transformed ${classMetadata.size}/${validatedClasses.size} classes")

        // Process using IrClassGenerationMetadata (NO analyzeClass!)
        if (classMetadata.isNotEmpty()) {
            processClassesFromMetadata(classMetadata, moduleFragment)
        }

        IrGenerationLogging.logGenerationCompletion(logger, telemetry, outputDir)
    }

    /**
     * Build a map of ClassId → IrClass for fast O(1) lookup during FIR→IR transformation.
     *
     * This method walks the entire module fragment to index all classes and interfaces,
     * including nested classes. The map enables efficient lookup when matching FIR metadata
     * to IR nodes without repeated tree traversal.
     *
     * **Performance**:
     * - Full module traversal: O(n) where n = total classes in module
     * - Nested class traversal: O(n * d) where d = average nesting depth (typically 1-2)
     * - Typical cost: 5-15ms for modules with 1000+ classes
     * - Map lookup: O(1) after construction
     *
     * **ClassId Construction**:
     * Manually constructs ClassId from IrClass properties because IrClass doesn't
     * expose ClassId directly. Uses:
     * - packageFqName: Package of the class
     * - name: Simple class name
     * - isLocal = false: Top-level and nested classes only (no local classes)
     *
     * **Thread Safety**:
     * Uses local mutableMap - safe for concurrent calls with different module fragments.
     * Returned map is immutable after construction.
     *
     * @param moduleFragment IR module to scan for all class declarations
     * @return Immutable map from ClassId to IrClass for fast lookup
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildIrClassMap(moduleFragment: IrModuleFragment): Map<ClassId, IrClass> {
        val map = mutableMapOf<ClassId, IrClass>()

        moduleFragment.files.forEach { file ->
            file.declarations.filterIsInstance<IrClass>().forEach { irClass ->
                val classId = ClassId(
                    packageFqName = irClass.packageFqName ?: FqName.ROOT,
                    relativeClassName = FqName(irClass.name.asString()),
                    isLocal = false,
                )
                map[classId] = irClass

                // Also add nested classes
                irClass.declarations.filterIsInstance<IrClass>().forEach { nestedClass ->
                    val nestedClassId = ClassId(
                        packageFqName = irClass.packageFqName ?: FqName.ROOT,
                        relativeClassName = FqName("${irClass.name.asString()}.${nestedClass.name.asString()}"),
                        isLocal = false,
                    )
                    map[nestedClassId] = nestedClass
                }
            }
        }

        logger.trace("Built IR class map with ${map.size} classes")
        return map
    }

    /**
     * Process interfaces from IrGenerationMetadata (NO re-analysis).
     *
     * This method replaces the anti-pattern of passing IrClass to processInterfaces(),
     * which triggered analyzeInterfaceDynamically() and re-analyzed what FIR already did.
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
            val packageName = metadata.packageName

            // Build signature for cache tracking
            val signature = metadata.buildSignature()

            // Build TypeInfo for cache operations
            val typeInfo =
                TypeInfo(
                    name = interfaceName,
                    fullyQualifiedName = "$packageName.$interfaceName",
                    packageName = packageName,
                    fileName = "$interfaceName.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"), // TODO: Support custom annotations
                    signature = signature,
                )

            // Build output file path (same logic as CodeGenerator)
            val sourceSetName = SourceSetExtractor.extractSourceSet(metadata.sourceInterface)
            val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment, sourceSetName)
            val packagePath = packageName.replace('.', '/')
            val packageDir = outputDir.resolve(packagePath)
            val fakeClassName = "Fake${interfaceName}Impl"
            val outputFile = packageDir.resolve("$fakeClassName.kt")

            // Check cache: file exists AND signature matches
            if (outputFile.exists() && !optimizations.needsRegeneration(typeInfo)) {
                // Cache hit - skip generation
                logger.debug("Cache hit: Skipping $interfaceName (file exists, signature matches)")
                telemetry.metricsCollector.incrementInterfacesCached()
                continue
            }

            // Cache miss - check if file exists with different signature
            if (outputFile.exists()) {
                // Signature changed - delete old file
                logger.debug("Signature changed: Deleting old fake for $interfaceName")
                outputFile.delete()
            }

            // Convert to InterfaceAnalysis using adapter (NO re-analysis!)
            val analysisStartTime = System.nanoTime()
            val interfaceAnalysis = metadata.toInterfaceAnalysis()
            val analysisEndTime = System.nanoTime()
            val analysisTime = analysisEndTime - analysisStartTime // Adapter overhead only (~1μs)

            // Validate pattern (reuses existing validation logic)
            validateAndLogGenericPattern(
                interfaceAnalysis = interfaceAnalysis,
                fakeInterface = metadata.sourceInterface,
                interfaceName = interfaceName,
                logger = logger
            )

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
                FakeMetrics(
                    name = interfaceName,
                    pattern = interfaceAnalysis.genericPattern,
                    memberCount = memberCount,
                    typeParamCount = interfaceAnalysis.typeParameters.size,
                    analysisTimeNanos = analysisTime, // Adapter overhead, not FIR analysis time
                    generationTimeNanos = generationTime,
                    generatedLOC = loc,
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = generatedCode.importCount,
                ),
            )

            telemetry.metricsCollector.incrementInterfacesProcessed()

            // Record successful generation in cache
            optimizations.recordGeneration(typeInfo)

            // TRACE: Log tree-style per-interface processing
            if (logger.logLevel >= LogLevel.TRACE) {
                val packageName = metadata.packageName
                val packagePath = packageName.replace('.', '/')
                val fakeFileName = "Fake${interfaceName}Impl.kt"
                val relativePath =
                    if (packagePath.isNotEmpty()) "$packagePath/$fakeFileName" else fakeFileName
                val outputPath = if (outputDir != null) "$outputDir/$relativePath" else relativePath

                val analysisDetail =
                    buildString {
                        val typeParamCount = interfaceAnalysis.typeParameters.size
                        if (typeParamCount > 0) {
                            append("$typeParamCount type parameters, ")
                        }
                        append("$memberCount members")
                    }

                IrGenerationLogging.logFakeProcessing(
                    logger = logger,
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
     * Process abstract classes from IrClassGenerationMetadata (NO re-analysis).
     *
     * **Note**: Currently combines abstract and open members since existing
     * generators treat all members the same way (all need implementation/override).
     * Future enhancements may support super delegation for open members.
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
            val packageName = metadata.packageName

            // Build signature for cache tracking
            val signature = metadata.buildSignature()

            // Build TypeInfo for cache operations
            val typeInfo =
                TypeInfo(
                    name = className,
                    fullyQualifiedName = "$packageName.$className",
                    packageName = packageName,
                    fileName = "$className.kt",
                    annotations = listOf("com.rsicarelli.fakt.Fake"), // TODO: Support custom annotations
                    signature = signature,
                )

            // Build output file path (same logic as CodeGenerator)
            val sourceSetName = SourceSetExtractor.extractSourceSet(metadata.sourceClass)
            val outputDir = sourceSetMapper.getGeneratedSourcesDir(moduleFragment, sourceSetName)
            val packagePath = packageName.replace('.', '/')
            val packageDir = outputDir.resolve(packagePath)
            val fakeClassName = "Fake${className}Impl"
            val outputFile = packageDir.resolve("$fakeClassName.kt")

            // Check cache: file exists AND signature matches
            if (outputFile.exists() && !optimizations.needsRegeneration(typeInfo)) {
                // Cache hit - skip generation
                logger.debug("Cache hit: Skipping class $className (file exists, signature matches)")
                telemetry.metricsCollector.incrementClassesCached()
                continue
            }

            // Cache miss - check if file exists with different signature
            if (outputFile.exists()) {
                // Signature changed - delete old file
                logger.debug("Signature changed: Deleting old fake for class $className")
                outputFile.delete()
            }

            // Convert to ClassAnalysis using adapter (preserves abstract/open distinction)
            val analysisStartTime = System.nanoTime()
            val classAnalysis = metadata.toClassAnalysis()
            val analysisEndTime = System.nanoTime()
            val analysisTime = analysisEndTime - analysisStartTime // Adapter overhead only (~1μs)

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
            // Count all members (abstract + open)
            val memberCount = (
                    classAnalysis.abstractProperties.size + classAnalysis.openProperties.size +
                            classAnalysis.abstractMethods.size + classAnalysis.openMethods.size
                    )
            telemetry.recordFakeMetrics(
                FakeMetrics(
                    name = className,
                    pattern = metadata.genericPattern, // Get from metadata, not classAnalysis
                    memberCount = memberCount,
                    typeParamCount = classAnalysis.typeParameters.size,
                    analysisTimeNanos = analysisTime, // Adapter overhead, not FIR analysis time
                    generationTimeNanos = generationTime,
                    generatedLOC = loc,
                    fileSizeBytes = generatedCode.calculateTotalBytes(),
                    importCount = generatedCode.importCount,
                ),
            )

            telemetry.metricsCollector.incrementClassesProcessed()

            // Record successful generation in cache
            optimizations.recordGeneration(typeInfo)

            // TRACE: Log tree-style per-class processing
            if (logger.logLevel >= LogLevel.TRACE) {
                val packageName = metadata.packageName
                val packagePath = packageName.replace('.', '/')
                val fakeFileName = "Fake${className}Impl.kt"
                val relativePath =
                    if (packagePath.isNotEmpty()) "$packagePath/$fakeFileName" else fakeFileName
                val outputPath = if (outputDir != null) "$outputDir/$relativePath" else relativePath

                val analysisDetail =
                    buildString {
                        val typeParamCount = classAnalysis.typeParameters.size
                        if (typeParamCount > 0) {
                            append("$typeParamCount type parameters, ")
                        }
                        // Count all properties and methods (abstract + open)
                        val propCount =
                            classAnalysis.abstractProperties.size + classAnalysis.openProperties.size
                        val methodCount =
                            classAnalysis.abstractMethods.size + classAnalysis.openMethods.size
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

}
