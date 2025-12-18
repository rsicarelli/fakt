// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.TimeFormatter
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.optimization.buildSignature
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedFakeMetrics
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedMetricsTree
import com.rsicarelli.fakt.compiler.core.telemetry.calculateLOC
import com.rsicarelli.fakt.compiler.core.telemetry.measureTimeNanos
import com.rsicarelli.fakt.compiler.core.types.TypeInfo
import com.rsicarelli.fakt.compiler.core.types.createTypeResolution
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import com.rsicarelli.fakt.compiler.ir.transform.FirToIrTransformer
import com.rsicarelli.fakt.compiler.ir.transform.IrClassGenerationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrGenerationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.toClassAnalysis
import com.rsicarelli.fakt.compiler.ir.transform.toInterfaceAnalysis
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Parameters for unified trace logging to reduce parameter count.
 *
 * @property interfaceMetrics Unified metrics for interfaces (FIR + IR), empty at INFO level
 * @property classMetrics Unified metrics for classes (FIR + IR), empty at INFO level
 * @property interfaceCount Number of interfaces processed
 * @property classCount Number of classes processed
 * @property irCacheHits Number of fakes that were IR-cached (skipped generation)
 * @property transformationTimeNanos Time spent in FIR→IR transformation
 * @property savedFirTimeNanos FIR time saved by using cache (0 if no cache)
 * @property aggregateIrTimeNanos Total IR processing time (used when metrics is empty)
 */
private data class TraceLogParams(
    val interfaceMetrics: List<UnifiedFakeMetrics>,
    val classMetrics: List<UnifiedFakeMetrics>,
    val interfaceCount: Int,
    val classCount: Int,
    val irCacheHits: Int,
    val transformationTimeNanos: Long,
    val savedFirTimeNanos: Long,
    val aggregateIrTimeNanos: Long,
)

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
 * @property sharedContext Shared context for FIR→IR communication, containing logger, telemetry, and configuration
 */
class UnifiedFaktIrGenerationExtension(
    private val sharedContext: FaktSharedContext,
) : IrGenerationExtension {
    // Extract logger and telemetry from sharedContext
    private val logger: FaktLogger = sharedContext.logger

    // Extract fields from sharedContext
    private val optimizations = sharedContext.optimizations

    // Get SourceSetContext from compiler options (provided by Gradle plugin)
    private val sourceSetContext =
        sharedContext.options.sourceSetContext
            ?: error("SourceSetContext is required. Ensure Gradle plugin version matches compiler plugin.")

    // Extracted modules following DRY principles
    private val typeResolver = createTypeResolution()
    private val importResolver = ImportResolver(typeResolver)

    private val generators =
        CodeGenerators(
            implementation = ImplementationGenerator(typeResolver),
            factory = FactoryGenerator(),
            configDsl = ConfigurationDslGenerator(typeResolver),
        )

    private val codeGenerator =
        CodeGenerator(
            importResolver = importResolver,
            sourceSetContext = sourceSetContext,
            generators = generators,
            logger = logger,
        )

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        try {
            generateFromFirMetadata(moduleFragment)
        } catch (e: Exception) {
            logger.error("IR-native generation failed: ${e.message}")
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
     * **KMP Cross-Compilation Caching**:
     * In producer mode (metadata compilation), writes FIR cache after analysis completes.
     * This enables platform compilations to skip redundant FIR analysis.
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
    private fun generateFromFirMetadata(moduleFragment: IrModuleFragment) {
        // KMP optimization: Write cache in producer mode (metadata compilation)
        // This allows platform compilations to skip FIR analysis
        writeCacheIfProducer()

        // Load validated interfaces from FIR phase
        val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()
        val validatedClasses = sharedContext.metadataStorage.getAllClasses()

        if (validatedInterfaces.isEmpty() && validatedClasses.isEmpty()) {
            logger.debug("No validated interfaces or classes to generate")
            return
        }

        // Only collect detailed metrics (LOC, per-fake timing) at DEBUG level
        // INFO level only needs: totalFakes, totalTimeNanos, irCacheHits
        val collectDetailedMetrics = logger.logLevel >= LogLevel.DEBUG

        // Build FIR metrics map for unified logging (name → FIR metrics)
        // Skip when not DEBUG - only used for detailed tree output
        val firMetricsMap =
            if (collectDetailedMetrics) {
                buildFirMetricsMap(validatedInterfaces, validatedClasses)
            } else {
                emptyMap()
            }

        // Build ClassId → IrClass map for fast lookup
        val irClassMap = buildIrClassMap(moduleFragment)

        // Transform FIR metadata → IrGenerationMetadata (NO re-analysis!)
        val transformer = FirToIrTransformer()

        val (interfaceMetadata, interfaceTransformTime) =
            measureTimeNanos {
                validatedInterfaces.mapNotNull { firInterface ->
                    val irClass = irClassMap[firInterface.classId]
                    if (irClass == null) {
                        logger.warn(
                            "Could not find IrClass for validated interface: ${firInterface.classId.asFqNameString()}",
                        )
                        null
                    } else {
                        transformer.transform(firInterface, irClass)
                    }
                }
            }

        // Transform classes using FirToIrTransformer
        val (classMetadata, classTransformTime) =
            measureTimeNanos {
                validatedClasses.mapNotNull { firClassMetadata ->
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
            }

        // Track transformation time for unified logging
        val transformationTimeNanos = interfaceTransformTime + classTransformTime

        // Collect unified metrics (FIR + IR) for batch logging
        val interfaceResult =
            if (interfaceMetadata.isNotEmpty()) {
                processInterfacesFromMetadata(interfaceMetadata, firMetricsMap, collectDetailedMetrics)
            } else {
                ProcessingResult(emptyList(), 0)
            }

        val classResult =
            if (classMetadata.isNotEmpty()) {
                processClassesFromMetadata(classMetadata, firMetricsMap, collectDetailedMetrics)
            } else {
                ProcessingResult(emptyList(), 0)
            }

        // Total IR cache hits (interfaces + classes)
        val totalIrCacheHits = interfaceResult.cacheHits + classResult.cacheHits

        // Get saved FIR time from cache manager (if consumer mode)
        val savedFirTimeNanos = sharedContext.cacheManager.getSavedFirTimeNanos()

        // Log consolidated unified trace (FIR + IR combined)
        // At INFO level, metrics lists are empty but we have aggregate times
        logUnifiedTrace(
            TraceLogParams(
                interfaceMetrics = interfaceResult.metrics,
                classMetrics = classResult.metrics,
                interfaceCount = interfaceMetadata.size,
                classCount = classMetadata.size,
                irCacheHits = totalIrCacheHits,
                transformationTimeNanos = transformationTimeNanos,
                savedFirTimeNanos = savedFirTimeNanos,
                aggregateIrTimeNanos = interfaceResult.totalTimeNanos + classResult.totalTimeNanos,
            ),
        )
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
                val classId =
                    ClassId(
                        packageFqName = irClass.packageFqName ?: FqName.ROOT,
                        relativeClassName = FqName(irClass.name.asString()),
                        isLocal = false,
                    )
                map[classId] = irClass

                // Also add nested classes
                irClass.declarations.filterIsInstance<IrClass>().forEach { nestedClass ->
                    val nestedClassId =
                        ClassId(
                            packageFqName = irClass.packageFqName ?: FqName.ROOT,
                            relativeClassName = FqName("${irClass.name.asString()}.${nestedClass.name.asString()}"),
                            isLocal = false,
                        )
                    map[nestedClassId] = nestedClass
                }
            }
        }

        return map
    }

    /**
     * Build FIR metrics map from validated interfaces and classes.
     *
     * Extracts timing and metadata from FIR phase to later merge with IR generation metrics.
     * This enables unified logging showing both FIR analysis and IR generation in one tree.
     *
     * @param validatedInterfaces Interfaces validated in FIR phase
     * @param validatedClasses Classes validated in FIR phase
     * @return Map of name → FIR metrics (validation time, type param count, member count)
     */
    private fun buildFirMetricsMap(
        validatedInterfaces: Collection<ValidatedFakeInterface>,
        validatedClasses: Collection<ValidatedFakeClass>,
    ): Map<String, FirMetrics> {
        val map = mutableMapOf<String, FirMetrics>()

        // Collect interface FIR metrics
        validatedInterfaces.forEach { firInterface ->
            val memberCount =
                firInterface.properties.size +
                    firInterface.functions.size +
                    firInterface.inheritedProperties.size +
                    firInterface.inheritedFunctions.size

            val firMetrics =
                FirMetrics(
                    validationTimeNanos = firInterface.validationTimeNanos,
                    typeParamCount = firInterface.typeParameters.size,
                    memberCount = memberCount,
                )
            map[firInterface.simpleName] = firMetrics
        }

        // Collect class FIR metrics
        validatedClasses.forEach { firClass ->
            val memberCount =
                firClass.abstractProperties.size +
                    firClass.openProperties.size +
                    firClass.abstractMethods.size +
                    firClass.openMethods.size

            map[firClass.simpleName] =
                FirMetrics(
                    validationTimeNanos = firClass.validationTimeNanos,
                    typeParamCount = firClass.typeParameters.size,
                    memberCount = memberCount,
                )
        }

        return map
    }

    /**
     * FIR metrics extracted from validated interfaces/classes.
     *
     * @property validationTimeNanos Time spent validating in FIR phase
     * @property typeParamCount Number of type parameters
     * @property memberCount Total member count (properties + functions)
     */
    private data class FirMetrics(
        val validationTimeNanos: Long,
        val typeParamCount: Int,
        val memberCount: Int,
    )

    /**
     * Result of processing interfaces: metrics and cache statistics.
     *
     * @property metrics List of unified metrics for each fake (empty when not DEBUG)
     * @property cacheHits Number of interfaces that were IR-cached (skipped generation)
     * @property totalTimeNanos Aggregate processing time (used when metrics is empty)
     */
    private data class ProcessingResult(
        val metrics: List<UnifiedFakeMetrics>,
        val cacheHits: Int,
        val totalTimeNanos: Long = 0,
    )

    /**
     * Process interfaces from IrGenerationMetadata (NO re-analysis).
     *
     * This method replaces the anti-pattern of passing IrClass to processInterfaces(),
     * which triggered analyzeInterfaceDynamically() and re-analyzed what FIR already did.
     *
     * Collects unified metrics (FIR + IR) for batch logging, including cache hits.
     * Cache hits appear in the trace with fast IR times (~5-50µs vs ~500µs-5ms for fresh generation).
     *
     * **Performance Optimization**:
     * When `collectDetailedMetrics` is false (INFO/QUIET level), skips:
     * - Per-fake `UnifiedFakeMetrics` objects
     * - LOC calculation (`calculateLOC` / file I/O)
     * - Per-fake `measureTimeNanos` calls
     *
     * Only aggregate time and cache hits are tracked for INFO summary.
     *
     * @param interfaceMetadata List of transformed FIR metadata (IrTypes + IR nodes)
     * @param firMetricsMap FIR metrics (validation time, type params, members) from FIR phase
     * @param collectDetailedMetrics Whether to collect per-fake metrics (DEBUG only)
     * @return ProcessingResult with metrics and IR cache hit count
     */
    private fun processInterfacesFromMetadata(
        interfaceMetadata: List<IrGenerationMetadata>,
        firMetricsMap: Map<String, FirMetrics>,
        collectDetailedMetrics: Boolean,
    ): ProcessingResult {
        val metrics = if (collectDetailedMetrics) mutableListOf<UnifiedFakeMetrics>() else null
        var irCacheHits = 0
        val startTime = System.nanoTime()

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
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = signature,
                )

            // Build output file path using SourceSetContext (Gradle-provided directory)
            val outputDir = java.io.File(sourceSetContext.outputDirectory)
            val packagePath = packageName.replace('.', '/')
            val packageDir = outputDir.resolve(packagePath)
            val fakeClassName = "Fake${interfaceName}Impl"
            val outputFile = packageDir.resolve("$fakeClassName.kt")

            // Check cache: file exists AND signature matches
            val isCacheHit = outputFile.exists() && !optimizations.needsRegeneration(typeInfo)
            if (isCacheHit) {
                irCacheHits++
                continue // Skip all processing for cache hits - no need to show in tree
            }

            // Get FIR metrics for this interface (only needed for detailed metrics)
            val firMetrics =
                if (collectDetailedMetrics) {
                    firMetricsMap[interfaceName] ?: run {
                        logger.warn("No FIR metrics found for $interfaceName - skipping")
                        continue
                    }
                } else {
                    null
                }

            // Cache miss - delete old file if signature changed
            val isRegeneration = outputFile.exists()
            if (isRegeneration) {
                logger.info("Signature changed: Regenerating $interfaceName")
                outputFile.delete()
            }

            // Convert to InterfaceAnalysis using adapter (NO re-analysis!)
            val interfaceAnalysis = metadata.toInterfaceAnalysis()

            // Validate pattern (reuses existing validation logic)
            validateAndLogGenericPattern(
                interfaceAnalysis = interfaceAnalysis,
                fakeInterface = metadata.sourceInterface,
                interfaceName = interfaceName,
                logger = logger,
            )

            // Generate fake implementation with timing
            val (generatedCode, generationTimeNanos) =
                measureTimeNanos {
                    codeGenerator.generateWorkingFakeImplementation(
                        sourceInterface = metadata.sourceInterface,
                        analysis = interfaceAnalysis,
                    )
                }

            // Record successful generation in cache
            optimizations.recordGeneration(typeInfo)

            // Collect unified metrics only at DEBUG level (only for regenerated fakes)
            if (collectDetailedMetrics && firMetrics != null) {
                metrics?.add(
                    UnifiedFakeMetrics(
                        name = interfaceName,
                        firTimeNanos = firMetrics.validationTimeNanos,
                        firTypeParamCount = firMetrics.typeParamCount,
                        firMemberCount = firMetrics.memberCount,
                        irTimeNanos = generationTimeNanos,
                        irLOC = generatedCode.calculateTotalLOC(),
                    ),
                )
            }
        }

        return ProcessingResult(
            metrics = metrics ?: emptyList(),
            cacheHits = irCacheHits,
            totalTimeNanos = System.nanoTime() - startTime,
        )
    }

    /**
     * Process abstract classes from IrClassGenerationMetadata (NO re-analysis).
     *
     * **Note**: Currently combines abstract and open members since existing
     * generators treat all members the same way (all need implementation/override).
     * Future enhancements may support super delegation for open members.
     *
     * Collects unified metrics (FIR + IR) for batch logging, including cache hits.
     * Cache hits appear in the trace with fast IR times (~5-50µs vs ~500µs-5ms for fresh generation).
     *
     * **Performance Optimization**:
     * When `collectDetailedMetrics` is false (INFO/QUIET level), skips:
     * - Per-fake `UnifiedFakeMetrics` objects
     * - LOC calculation (`calculateLOC` / file I/O)
     * - Per-fake `measureTimeNanos` calls
     *
     * Only aggregate time and cache hits are tracked for INFO summary.
     *
     * @param classMetadata List of transformed FIR class metadata (IrTypes + IR nodes)
     * @param firMetricsMap FIR metrics (validation time, type params, members) from FIR phase
     * @param collectDetailedMetrics Whether to collect per-fake metrics (DEBUG only)
     * @return ProcessingResult with metrics and IR cache hit count
     */
    private fun processClassesFromMetadata(
        classMetadata: List<IrClassGenerationMetadata>,
        firMetricsMap: Map<String, FirMetrics>,
        collectDetailedMetrics: Boolean,
    ): ProcessingResult {
        val metrics = if (collectDetailedMetrics) mutableListOf<UnifiedFakeMetrics>() else null
        var irCacheHits = 0
        val startTime = System.nanoTime()

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
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
                    signature = signature,
                )

            // Build output file path using SourceSetContext (Gradle-provided directory)
            val outputDir = java.io.File(sourceSetContext.outputDirectory)
            val packagePath = packageName.replace('.', '/')
            val packageDir = outputDir.resolve(packagePath)
            val fakeClassName = "Fake${className}Impl"
            val outputFile = packageDir.resolve("$fakeClassName.kt")

            // Check cache: file exists AND signature matches
            val isCacheHit = outputFile.exists() && !optimizations.needsRegeneration(typeInfo)
            if (isCacheHit) {
                irCacheHits++
                continue // Skip all processing for cache hits - no need to show in tree
            }

            // Get FIR metrics for this class (only needed for detailed metrics)
            val firMetrics =
                if (collectDetailedMetrics) {
                    firMetricsMap[className] ?: run {
                        logger.warn("No FIR metrics found for class $className - skipping")
                        continue
                    }
                } else {
                    null
                }

            // Cache miss - delete old file if signature changed
            val isRegeneration = outputFile.exists()
            if (isRegeneration) {
                logger.info("Signature changed: Regenerating $className")
                outputFile.delete()
            }

            // Convert to ClassAnalysis using adapter (preserves abstract/open distinction)
            val classAnalysis = metadata.toClassAnalysis()

            // Generate fake implementation with timing
            val (generatedCode, generationTimeNanos) =
                measureTimeNanos {
                    codeGenerator.generateWorkingClassFake(
                        sourceClass = metadata.sourceClass,
                        analysis = classAnalysis,
                    )
                }

            // Record successful generation in cache
            optimizations.recordGeneration(typeInfo)

            // Collect unified metrics only at DEBUG level (only for regenerated fakes)
            if (collectDetailedMetrics && firMetrics != null) {
                metrics?.add(
                    UnifiedFakeMetrics(
                        name = className,
                        firTimeNanos = firMetrics.validationTimeNanos,
                        firTypeParamCount = firMetrics.typeParamCount,
                        firMemberCount = firMetrics.memberCount,
                        irTimeNanos = generationTimeNanos,
                        irLOC = generatedCode.calculateTotalLOC(),
                    ),
                )
            }
        }

        return ProcessingResult(
            metrics = metrics ?: emptyList(),
            cacheHits = irCacheHits,
            totalTimeNanos = System.nanoTime() - startTime,
        )
    }

    /**
     * Logs unified FIR + IR metrics in a level-appropriate format.
     *
     * **INFO Level**: Concise summary:
     * ```
     * Fakt: 122 fakes (all cached)
     * ```
     *
     * **DEBUG Level**: Detailed tree with consolidated metrics:
     * ```
     * Fakt Trace
     * ├─ Total fakes                                                               122
     * ├─ FIR→IR: transformation (101 interfaces, 21 classes)                       1ms
     * ├─ FIR Time (122 from cache saved 6ms)                                       0µs
     * ├─ IR Time (122 from cache)                                                207µs
     * └─ Total time                                                                1ms
     * ```
     *
     * **QUIET**: No output
     *
     * @param params Parameters for trace logging (see TraceLogParams)
     */
    private fun logUnifiedTrace(params: TraceLogParams) {
        // Skip entirely for QUIET level
        if (logger.logLevel < LogLevel.INFO) return

        val tree =
            UnifiedMetricsTree(
                interfaces = params.interfaceMetrics,
                classes = params.classMetrics,
                interfaceCount = params.interfaceCount,
                classCount = params.classCount,
                interfaceCacheHits = sharedContext.metadataStorage.interfaceCacheHits,
                classCacheHits = sharedContext.metadataStorage.classCacheHits,
                irCacheHits = params.irCacheHits,
                transformationTimeNanos = params.transformationTimeNanos,
                savedFirTimeNanos = params.savedFirTimeNanos,
                aggregateIrTimeNanos = params.aggregateIrTimeNanos,
            )

        // INFO: Concise summary (4 lines)
        if (logger.logLevel == LogLevel.INFO) {
            logger.info(tree.toInfoSummary())
            return
        }

        // DEBUG: Detailed tree breakdown
        if (logger.logLevel >= LogLevel.DEBUG) {
            logger.debug(tree.toTreeString())
        }
    }

    /**
     * Write FIR metadata cache in producer mode.
     *
     * **KMP Cross-Compilation Optimization**:
     * In producer mode (metadata compilation), writes validated FIR metadata to cache file.
     * Platform compilations can then read this cache and skip redundant FIR analysis.
     *
     * **Call Timing**:
     * Called at the start of IR generation, after FIR analysis has completed
     * and FirMetadataStorage is fully populated.
     *
     * **Producer Mode**: metadataOutputPath is set (writes cache)
     * **Consumer Mode**: metadataCachePath is set (cache already loaded in FIR phase)
     * **Non-KMP/Disabled**: Neither path set (no caching)
     */
    private fun writeCacheIfProducer() {
        val cacheManager = sharedContext.cacheManager ?: return
        if (!cacheManager.isProducerMode) return

        // FIR phase writes cache after each interface/class.
        // IR phase writes once more (backup) and logs the final summary.
        try {
            cacheManager.writeCache(sharedContext.metadataStorage)
            // Log summary once at end of IR phase
            cacheManager.getLastWriteResult()?.let { result ->
                val time = TimeFormatter.format(result.durationNanos)
                logger.info("Cache written: ${result.interfaceCount} interfaces, ${result.classCount} classes ($time)")
            }
        } catch (e: Exception) {
            // Cache write failure is non-fatal - compilation continues
            logger.warn("Failed to write KMP cache: ${e.message}")
        }
    }
}
