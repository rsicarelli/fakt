// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.TimeFormatter
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.context.ImportResolver
import com.rsicarelli.fakt.compiler.core.optimization.buildSignature
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.telemetry.GeneratedFakeMetrics
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedFakeMetrics
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedMetricsTree
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
    private val sourceSetContext = sharedContext.options.sourceSetContext
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
        // Load validated interfaces from FIR phase
        val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()
        val validatedClasses = sharedContext.metadataStorage.getAllClasses()

        if (validatedInterfaces.isEmpty() && validatedClasses.isEmpty()) {
            logger.debug("No validated interfaces or classes to generate")
            return
        }

        // Build FIR metrics map for unified logging (name → FIR metrics)
        val firMetricsMap = buildFirMetricsMap(validatedInterfaces, validatedClasses)

        // Build ClassId → IrClass map for fast lookup
        val irClassMap = buildIrClassMap(moduleFragment)

        // Transform FIR metadata → IrGenerationMetadata (NO re-analysis!)
        val transformer = FirToIrTransformer()

        val (interfaceMetadata, interfaceTransformTime) = measureTimeNanos {
            validatedInterfaces.mapNotNull { firInterface ->
                val irClass = irClassMap[firInterface.classId]
                if (irClass == null) {
                    logger.warn("Could not find IrClass for validated interface: ${firInterface.classId.asFqNameString()}")
                    null
                } else {
                    transformer.transform(firInterface, irClass)
                }
            }
        }

        logger.debug(
            "FIR→IR Transformation (interfaces: ${interfaceMetadata.size}/${validatedInterfaces.size}, took ${
                TimeFormatter.format(
                    interfaceTransformTime
                )
            })"
        )

        // Transform classes using FirToIrTransformer
        val (classMetadata, classTransformTime) = measureTimeNanos {
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

        logger.debug(
            "FIR→IR Transformation (classes: ${classMetadata.size}/${validatedClasses.size}, took ${
                TimeFormatter.format(
                    classTransformTime
                )
            })"
        )

        // Collect unified metrics (FIR + IR) for batch logging
        val interfaceMetrics = if (interfaceMetadata.isNotEmpty()) {
            processInterfacesFromMetadata(interfaceMetadata, moduleFragment, firMetricsMap)
        } else emptyList()

        val classMetrics = if (classMetadata.isNotEmpty()) {
            processClassesFromMetadata(classMetadata, moduleFragment, firMetricsMap)
        } else emptyList()

        // Log consolidated unified trace (FIR + IR combined)
        logUnifiedTrace(interfaceMetrics, classMetrics)
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

        logger.debug("Built IR class map with ${map.size} classes")
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
            val memberCount = firInterface.properties.size +
                            firInterface.functions.size +
                            firInterface.inheritedProperties.size +
                            firInterface.inheritedFunctions.size

            val firMetrics = FirMetrics(
                validationTimeNanos = firInterface.validationTimeNanos,
                typeParamCount = firInterface.typeParameters.size,
                memberCount = memberCount,
            )
            map[firInterface.simpleName] = firMetrics
        }

        // Collect class FIR metrics
        validatedClasses.forEach { firClass ->
            val memberCount = firClass.abstractProperties.size +
                            firClass.openProperties.size +
                            firClass.abstractMethods.size +
                            firClass.openMethods.size

            map[firClass.simpleName] = FirMetrics(
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
     * Process interfaces from IrGenerationMetadata (NO re-analysis).
     *
     * This method replaces the anti-pattern of passing IrClass to processInterfaces(),
     * which triggered analyzeInterfaceDynamically() and re-analyzed what FIR already did.
     *
     * Collects unified metrics (FIR + IR) for batch logging, including cache hits.
     * Cache hits appear in the trace with fast IR times (~5-50µs vs ~500µs-5ms for fresh generation).
     *
     * @param interfaceMetadata List of transformed FIR metadata (IrTypes + IR nodes)
     * @param moduleFragment Module for file creation
     * @param firMetricsMap FIR metrics (validation time, type params, members) from FIR phase
     * @return List of unified metrics combining FIR and IR for each fake
     */
    private fun processInterfacesFromMetadata(
        interfaceMetadata: List<IrGenerationMetadata>,
        moduleFragment: IrModuleFragment,
        firMetricsMap: Map<String, FirMetrics>,
    ): List<UnifiedFakeMetrics> {
        val metrics = mutableListOf<UnifiedFakeMetrics>()

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

            // Build output file path using SourceSetContext (Gradle-provided directory)
            val outputDir = java.io.File(sourceSetContext.outputDirectory)
            val packagePath = packageName.replace('.', '/')
            val packageDir = outputDir.resolve(packagePath)
            val fakeClassName = "Fake${interfaceName}Impl"
            val outputFile = packageDir.resolve("$fakeClassName.kt")

            // Get FIR metrics for this interface
            val firMetrics = firMetricsMap[interfaceName]
            if (firMetrics == null) {
                logger.warn("No FIR metrics found for $interfaceName - skipping")
                continue
            }

            // Check cache: file exists AND signature matches
            val isCacheHit = outputFile.exists() && !optimizations.needsRegeneration(typeInfo)

            val (irTimeNanos, loc) = if (isCacheHit) {
                // Cache hit - minimal IR time for file existence check (~5-50µs)
                val (_, checkTime) = measureTimeNanos {
                    outputFile.exists() // Simulate cache check overhead
                }
                // Read LOC from existing file for metrics
                val existingLoc = if (outputFile.exists()) {
                    outputFile.readLines().size
                } else {
                    0
                }
                checkTime to existingLoc
            } else {
                // Cache miss - check if file exists with different signature
                if (outputFile.exists()) {
                    logger.debug("Signature changed: Deleting old fake for $interfaceName")
                    outputFile.delete()
                }

                // Convert to InterfaceAnalysis using adapter (NO re-analysis!)
                val (interfaceAnalysis, analysisTime) = measureTimeNanos {
                    metadata.toInterfaceAnalysis()
                }

                // Validate pattern (reuses existing validation logic)
                validateAndLogGenericPattern(
                    interfaceAnalysis = interfaceAnalysis,
                    fakeInterface = metadata.sourceInterface,
                    interfaceName = interfaceName,
                    logger = logger
                )

                // Track generation timing and capture generated code
                val (generatedCode, generationTime) = measureTimeNanos {
                    codeGenerator.generateWorkingFakeImplementation(
                        sourceInterface = metadata.sourceInterface,
                        analysis = interfaceAnalysis,
                        moduleFragment = moduleFragment,
                    )
                }

                // Calculate LOC
                val generatedLoc = generatedCode.calculateTotalLOC()

                // Record successful generation in cache
                optimizations.recordGeneration(typeInfo)

                // Return IR time (analysis + generation) and LOC
                (analysisTime + generationTime) to generatedLoc
            }

            // Collect unified metrics (FIR + IR) for batch logging
            val unifiedMetrics = UnifiedFakeMetrics(
                name = interfaceName,
                firTimeNanos = firMetrics.validationTimeNanos,
                firTypeParamCount = firMetrics.typeParamCount,
                firMemberCount = firMetrics.memberCount,
                irTimeNanos = irTimeNanos,
                irLOC = loc,
            )
            metrics.add(unifiedMetrics)
        }

        return metrics
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
     * @param classMetadata List of transformed FIR class metadata (IrTypes + IR nodes)
     * @param moduleFragment Module for file creation
     * @param firMetricsMap FIR metrics (validation time, type params, members) from FIR phase
     * @return List of unified metrics combining FIR and IR for each fake class
     */
    private fun processClassesFromMetadata(
        classMetadata: List<IrClassGenerationMetadata>,
        moduleFragment: IrModuleFragment,
        firMetricsMap: Map<String, FirMetrics>,
    ): List<UnifiedFakeMetrics> {
        val metrics = mutableListOf<UnifiedFakeMetrics>()

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

            // Build output file path using SourceSetContext (Gradle-provided directory)
            val outputDir = java.io.File(sourceSetContext.outputDirectory)
            val packagePath = packageName.replace('.', '/')
            val packageDir = outputDir.resolve(packagePath)
            val fakeClassName = "Fake${className}Impl"
            val outputFile = packageDir.resolve("$fakeClassName.kt")

            // Get FIR metrics for this class
            val firMetrics = firMetricsMap[className]
            if (firMetrics == null) {
                logger.warn("No FIR metrics found for class $className - skipping")
                continue
            }

            // Check cache: file exists AND signature matches
            val isCacheHit = outputFile.exists() && !optimizations.needsRegeneration(typeInfo)

            val (irTimeNanos, loc) = if (isCacheHit) {
                // Cache hit - minimal IR time for file existence check (~5-50µs)
                val (_, checkTime) = measureTimeNanos {
                    outputFile.exists() // Simulate cache check overhead
                }
                // Read LOC from existing file for metrics
                val existingLoc = if (outputFile.exists()) {
                    outputFile.readLines().size
                } else {
                    0
                }
                checkTime to existingLoc
            } else {
                // Cache miss - check if file exists with different signature
                if (outputFile.exists()) {
                    logger.debug("Signature changed: Deleting old fake for class $className")
                    outputFile.delete()
                }

                // Convert to ClassAnalysis using adapter (preserves abstract/open distinction)
                val (classAnalysis, analysisTime) = measureTimeNanos {
                    metadata.toClassAnalysis()
                }

                // Track generation timing and capture generated code
                val (generatedCode, generationTime) = measureTimeNanos {
                    codeGenerator.generateWorkingClassFake(
                        sourceClass = metadata.sourceClass,
                        analysis = classAnalysis,
                        moduleFragment = moduleFragment,
                    )
                }

                // Calculate LOC
                val generatedLoc = generatedCode.calculateTotalLOC()

                // Record successful generation in cache
                optimizations.recordGeneration(typeInfo)

                // Return IR time (analysis + generation) and LOC
                (analysisTime + generationTime) to generatedLoc
            }

            // Collect unified metrics (FIR + IR) for batch logging
            metrics.add(
                UnifiedFakeMetrics(
                    name = className,
                    firTimeNanos = firMetrics.validationTimeNanos,
                    firTypeParamCount = firMetrics.typeParamCount,
                    firMemberCount = firMetrics.memberCount,
                    irTimeNanos = irTimeNanos,
                    irLOC = loc,
                )
            )
        }

        return metrics
    }

    /**
     * Logs consolidated unified FIR + IR trace with all generated fakes.
     *
     * Outputs tree-style format at DEBUG level matching FIR phase style.
     * Replaces per-fake logging with batch output for cleaner logs.
     *
     * **Example Output (DEBUG level):**
     * ```
     * i: Fakt: IR Generation Trace (took 45ms)
     * i: Fakt: ├─ Interfaces: 101
     * i: Fakt: │  ├─ MutableListHandler                19µs
     * i: Fakt: │  │     ├─ 1 type parameters, 1 members
     * i: Fakt: │  │     └─ FakeMutableListHandlerImpl 23 LOC
     * i: Fakt: │  ├─ CallbackHandler                   5µs
     * i: Fakt: │  │     ├─ 1 type parameters, 1 members
     * i: Fakt: │  │     └─ FakeCallbackHandlerImpl 23 LOC
     * i: Fakt: └─ Classes: 21
     * i: Fakt:    ├─ AsyncDataFetcher                  735µs
     * i: Fakt:    │     ├─ 2 type parameters, 4 members
     * i: Fakt:    │     └─ FakeAsyncDataFetcherImpl 45 LOC
     * ```
     *
     * @param interfaceMetrics List of metrics for generated interfaces
     * @param classMetrics List of metrics for generated classes
     */
    /**
     * Log unified FIR + IR trace showing both analysis and generation metrics in one tree.
     *
     * Shows complete metrics for each fake:
     * - FIR analysis: validation time, type parameters, members
     * - IR generation: generation time, LOC
     *
     * Cache hits naturally appear with fast total times (~50-200µs vs ~1-10ms for fresh generation).
     *
     * Example output:
     * ```
     * i: Fakt: FIR + IR trace
     * i: Fakt: ├─ Interfaces: 101
     * i: Fakt: │  ├─ UserService                                     1ms total
     * i: Fakt: │  │  ├─ FIR analysis: 0 type parameters, 5 members   45µs
     * i: Fakt: │  │  └─ IR generation: FakeUserServiceImpl 73 LOC    955µs
     * i: Fakt: │  ├─ DataCache                                       50µs total ← cache hit!
     * i: Fakt: │  │  ├─ FIR analysis: 1 type parameters, 6 members   45µs
     * i: Fakt: │  │  └─ IR generation: FakeDataCacheImpl 83 LOC      5µs
     * i: Fakt: └─ Classes: 21
     * i: Fakt:    ├─ KeyValueCache                                   2ms total
     * i: Fakt:    │  ├─ FIR analysis: 2 type parameters, 5 members   50µs
     * i: Fakt:    │  └─ IR generation: FakeKeyValueCacheImpl 72 LOC  1.95ms
     * ```
     *
     * @param interfaceMetrics Unified metrics for interfaces (FIR + IR)
     * @param classMetrics Unified metrics for classes (FIR + IR)
     */
    private fun logUnifiedTrace(
        interfaceMetrics: List<UnifiedFakeMetrics>,
        classMetrics: List<UnifiedFakeMetrics>,
    ) {
        if (logger.logLevel < LogLevel.DEBUG) return

        val tree = UnifiedMetricsTree(
            interfaces = interfaceMetrics,
            classes = classMetrics
        )

        logger.debug(tree.toTreeString())
    }

}
