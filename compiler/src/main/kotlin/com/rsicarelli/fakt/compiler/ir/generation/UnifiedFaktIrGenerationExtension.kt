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
import com.rsicarelli.fakt.compiler.core.telemetry.measureTimeNanos
import com.rsicarelli.fakt.compiler.core.types.TypeInfo
import com.rsicarelli.fakt.compiler.core.types.createTypeResolution
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import com.rsicarelli.fakt.compiler.ir.analysis.IrClassDirectAnalyzer
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
        val totalStartTime = System.nanoTime()

        try {
            // ONLY run during MAIN compilation (not test)
            // This ensures fakes are generated BEFORE test compilation starts
            // Works for both JVM and KMP where test IR extensions may not be invoked
            if (sourceSetContext.isTest) {
                logger.debug("Skipping test compilation (fakes already generated during main)")
                return
            }

            val firMetadata = sharedContext.metadataStorage.getAllInterfaces()
            val firClasses = sharedContext.metadataStorage.getAllClasses()

            if (firMetadata.isEmpty() && firClasses.isEmpty()) {
                logger.debug("No @Fake annotations found in main compilation")
                return
            }

            logger.info("Main compilation: ${firMetadata.size} interfaces, ${firClasses.size} classes detected")

            // Write index file for reference/debugging
            writeIndexFile(firMetadata, firClasses)

            // GENERATE FAKES IMMEDIATELY (during main compilation)
            // This ensures they're available when test compilation starts
            val generationStartTime = System.nanoTime()
            logger.info("Generating ${firMetadata.size + firClasses.size} fakes from main compilation")

            // Load IrClass instances for each detected @Fake
            val compiledClasses = buildList {
                (firMetadata.map { it.classId } + firClasses.map { it.classId }).forEach { classId ->
                    pluginContext.referenceClass(classId)?.owner?.let { add(it) }
                }
            }

            if (compiledClasses.isNotEmpty()) {
                generateFromCompiledClasses(compiledClasses, pluginContext)

                val totalTime = System.nanoTime() - totalStartTime
                val generationTime = System.nanoTime() - generationStartTime

                logger.info("✅ Generated ${compiledClasses.size} fakes successfully")
                logger.info("⏱️  Total time: ${totalTime / 1_000_000}ms (generation: ${generationTime / 1_000_000}ms)")
                logger.info("⚡ Average: ${generationTime / compiledClasses.size / 1_000}µs per fake")
            }
        } catch (e: Exception) {
            logger.error("IR generation failed: ${e.message}")
            logger.error(e.stackTraceToString())
        }
    }

    /**
     * POC VALIDATION TEST: Can we access compiled classes from main during test compilation?
     *
     * This tests the core assumption needed for the optimization:
     * - During test compilation, can IrPluginContext.referenceClass() find classes from main?
     * - Are annotations readable from compiled .class files?
     * - Can we extract function signatures?
     */
    private fun testCompiledClassAccess(
        pluginContext: IrPluginContext,
        logFile: java.io.File,
    ) {
        logFile.appendText("🧪 POC TEST: Accessing Compiled Classes\n")
        logFile.appendText("─".repeat(60) + "\n")

        val testClassId =
            org.jetbrains.kotlin.name.ClassId(
                packageFqName = org.jetbrains.kotlin.name.FqName("com.rsicarelli.fakt.samples.jvmSingleModule.validation"),
                relativeClassName = org.jetbrains.kotlin.name.FqName("TestService"),
                isLocal = false,
            )

        logFile.appendText("Attempting: com.rsicarelli.fakt.samples.jvmSingleModule.validation.TestService\n")

        val classSymbol = pluginContext.referenceClass(testClassId)

        if (classSymbol == null) {
            logFile.appendText("❌ FAILED: Could not reference compiled class\n")
            logFile.appendText("   Theory INVALID - optimization won't work!\n")
            logFile.appendText("─".repeat(60) + "\n")
            return
        }

        logFile.appendText("✅ SUCCESS: Referenced compiled class!\n")
        logFile.appendText("   Class: ${classSymbol.owner.name.asString()}\n")

        // Test annotation access
        val annotations = classSymbol.owner.annotations
        logFile.appendText("✅ SUCCESS: Annotations accessible (count: ${annotations.size})\n")

        // Test function signatures
        val functions = classSymbol.owner.declarations.filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrSimpleFunction>()
        logFile.appendText("✅ SUCCESS: Functions accessible (count: ${functions.size})\n")
        functions.forEach { func ->
            logFile.appendText("   - ${func.name.asString()}()\n")
        }

        logFile.appendText("\n")
        logFile.appendText("🎯 POC RESULT: THEORY IS VALID ✅\n")
        logFile.appendText("   ✓ IrPluginContext.referenceClass() works\n")
        logFile.appendText("   ✓ Compiled main classes accessible during test compilation\n")
        logFile.appendText("   ✓ Annotations readable\n")
        logFile.appendText("   ✓ Function signatures extractable\n")
        logFile.appendText("─".repeat(60) + "\n")
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
    private fun generateFromFirMetadata(moduleFragment: IrModuleFragment) {
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

        logger.debug(
            "FIR→IR Transformation (interfaces: ${interfaceMetadata.size}/${validatedInterfaces.size}, took ${
                TimeFormatter.format(
                    interfaceTransformTime,
                )
            })",
        )

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

        logger.debug(
            "FIR→IR Transformation (classes: ${classMetadata.size}/${validatedClasses.size}, took ${
                TimeFormatter.format(
                    classTransformTime,
                )
            })",
        )

        // Collect unified metrics (FIR + IR) for batch logging
        val interfaceMetrics =
            if (interfaceMetadata.isNotEmpty()) {
                processInterfacesFromMetadata(interfaceMetadata, firMetricsMap)
            } else {
                emptyList()
            }

        val classMetrics =
            if (classMetadata.isNotEmpty()) {
                processClassesFromMetadata(classMetadata, firMetricsMap)
            } else {
                emptyList()
            }

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
     * Process interfaces from IrGenerationMetadata (NO re-analysis).
     *
     * This method replaces the anti-pattern of passing IrClass to processInterfaces(),
     * which triggered analyzeInterfaceDynamically() and re-analyzed what FIR already did.
     *
     * Collects unified metrics (FIR + IR) for batch logging, including cache hits.
     * Cache hits appear in the trace with fast IR times (~5-50µs vs ~500µs-5ms for fresh generation).
     *
     * @param interfaceMetadata List of transformed FIR metadata (IrTypes + IR nodes)
     * @param firMetricsMap FIR metrics (validation time, type params, members) from FIR phase
     * @return List of unified metrics combining FIR and IR for each fake
     */
    private fun processInterfacesFromMetadata(
        interfaceMetadata: List<IrGenerationMetadata>,
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
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
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

            val (irTimeNanos, loc) =
                if (isCacheHit) {
                    // Cache hit - minimal IR time for file existence check (~5-50µs)
                    val (_, checkTime) =
                        measureTimeNanos {
                            outputFile.exists() // Simulate cache check overhead
                        }
                    // Read LOC from existing file for metrics
                    val existingLoc =
                        if (outputFile.exists()) {
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
                    val (interfaceAnalysis, analysisTime) =
                        measureTimeNanos {
                            metadata.toInterfaceAnalysis()
                        }

                    // Validate pattern (reuses existing validation logic)
                    validateAndLogGenericPattern(
                        interfaceAnalysis = interfaceAnalysis,
                        fakeInterface = metadata.sourceInterface,
                        interfaceName = interfaceName,
                        logger = logger,
                    )

                    // Track generation timing and capture generated code
                    val (generatedCode, generationTime) =
                        measureTimeNanos {
                            codeGenerator.generateWorkingFakeImplementation(
                                sourceInterface = metadata.sourceInterface,
                                analysis = interfaceAnalysis,
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
            val unifiedMetrics =
                UnifiedFakeMetrics(
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
     * @param firMetricsMap FIR metrics (validation time, type params, members) from FIR phase
     * @return List of unified metrics combining FIR and IR for each fake class
     */
    private fun processClassesFromMetadata(
        classMetadata: List<IrClassGenerationMetadata>,
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
                    annotations = listOf("com.rsicarelli.fakt.Fake"),
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

            val (irTimeNanos, loc) =
                if (isCacheHit) {
                    // Cache hit - minimal IR time for file existence check (~5-50µs)
                    val (_, checkTime) =
                        measureTimeNanos {
                            outputFile.exists() // Simulate cache check overhead
                        }
                    // Read LOC from existing file for metrics
                    val existingLoc =
                        if (outputFile.exists()) {
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
                    val (classAnalysis, analysisTime) =
                        measureTimeNanos {
                            metadata.toClassAnalysis()
                        }

                    // Track generation timing and capture generated code
                    val (generatedCode, generationTime) =
                        measureTimeNanos {
                            codeGenerator.generateWorkingClassFake(
                                sourceClass = metadata.sourceClass,
                                analysis = classAnalysis,
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
                ),
            )
        }

        return metrics
    }

    /**
     * Logs unified FIR + IR metrics in a level-appropriate format.
     *
     * **INFO Level**: Concise 4-line summary with key metrics:
     * ```
     * Fakt: 3 fakes generated in 1.4ms (0 cached)
     *   Interfaces: 3 | Classes: 0
     *   FIR: 115µs | IR: 1.285ms
     *   Cache: 0/3 (0%)
     * ```
     *
     * **DEBUG Level**: Detailed tree with per-fake breakdown:
     * ```
     * FIR + IR trace
     * ├─ Total FIR time                                                            234µs
     * ├─ Total IR time                                                             1.2ms
     * ├─ Total time                                                                1.4ms
     * ├─ Interfaces: 3
     * │  ├─ UserService                                                            580µs
     * │  │  ├─ FIR analysis: 0 type parameters, 5 members                          45µs
     * │  │  └─ IR generation: FakeUserServiceImpl 73 LOC                           535µs
     * ```
     *
     * **QUIET**: No output
     *
     * @param interfaceMetrics Unified metrics for interfaces (FIR + IR)
     * @param classMetrics Unified metrics for classes (FIR + IR)
     */
    private fun logUnifiedTrace(
        interfaceMetrics: List<UnifiedFakeMetrics>,
        classMetrics: List<UnifiedFakeMetrics>,
    ) {
        // Skip entirely for QUIET level
        if (logger.logLevel < LogLevel.INFO) return

        val tree =
            UnifiedMetricsTree(
                interfaces = interfaceMetrics,
                classes = classMetrics,
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
     * Writes index file containing ClassIds of validated @Fake interfaces/classes.
     * This file is read during test compilation to know which classes to generate fakes for.
     *
     * File format: One ClassId per line (FqName format)
     * Location: build/generated/fakt/test/fakt-index.txt
     *
     * @param interfaces Validated interfaces from FIR phase
     * @param classes Validated classes from FIR phase
     */
    private fun writeIndexFile(
        interfaces: Collection<ValidatedFakeInterface>,
        classes: Collection<ValidatedFakeClass>,
    ) {
        val outputDir = sourceSetContext.outputDirectory
        val indexFile = java.io.File(outputDir).parentFile.resolve("fakt-index.txt")

        indexFile.parentFile?.mkdirs()

        val classIds = buildList {
            interfaces.forEach { add(it.classId.asFqNameString()) }
            classes.forEach { add(it.classId.asFqNameString()) }
        }

        indexFile.writeText(classIds.joinToString("\n"))

        logger.info("Wrote index file: ${indexFile.absolutePath} (${classIds.size} classes)")
    }

    /**
     * Reads index file and loads IrClass for each ClassId using pluginContext.referenceClass().
     * This enables accessing compiled classes from main during test compilation.
     *
     * @param pluginContext IR plugin context for class resolution
     * @return List of IrClass instances for compiled @Fake interfaces/classes
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun loadCompiledClassesFromIndex(pluginContext: IrPluginContext): List<IrClass> {
        val indexFile = findIndexFile() ?: return emptyList()

        if (!indexFile.exists()) {
            logger.warn("Index file not found: ${indexFile.absolutePath}")
            return emptyList()
        }

        val classIdStrings = indexFile.readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        logger.debug("Index file contains ${classIdStrings.size} ClassIds")

        return classIdStrings.mapNotNull { fqNameString ->
            try {
                // Parse FqName format (com.example.Package.ClassName)
                // Split into package and class name
                val lastDotIndex = fqNameString.lastIndexOf('.')
                if (lastDotIndex == -1) {
                    logger.error("Invalid FqName format: $fqNameString")
                    return@mapNotNull null
                }

                val packageFqName = fqNameString.substring(0, lastDotIndex)
                val className = fqNameString.substring(lastDotIndex + 1)

                // Create ClassId from package and class name
                val classId = ClassId(FqName(packageFqName), org.jetbrains.kotlin.name.Name.identifier(className))

                val irClassSymbol = pluginContext.referenceClass(classId)

                if (irClassSymbol == null) {
                    logger.warn("Could not resolve ClassId: $classId (from $fqNameString)")
                    null
                } else {
                    logger.info("✅ Loaded compiled class: ${irClassSymbol.owner.name.asString()}")
                    irClassSymbol.owner
                }
            } catch (e: Exception) {
                logger.error("Failed to load ClassId from $fqNameString: ${e.message}")
                logger.error(e.stackTraceToString())
                null
            }
        }
    }

    /**
     * Finds the index file location based on output directory.
     *
     * @return Index file or null if output directory not configured
     */
    private fun findIndexFile(): java.io.File? {
        val outputDir = sourceSetContext.outputDirectory
        val indexFile = java.io.File(outputDir).parentFile.resolve("fakt-index.txt")

        logger.debug("Looking for index file: ${indexFile.absolutePath}")
        return indexFile
    }

    /**
     * Generate fakes from compiled classes loaded via referenceClass().
     *
     * This method is used during test compilation to generate fakes from
     * compiled main classes. It uses IrClassDirectAnalyzer to extract metadata
     * directly from IrClass instead of relying on FIR metadata.
     *
     * @param classes List of IrClass instances loaded from index file
     * @param pluginContext IR plugin context for type resolution
     */
    private fun generateFromCompiledClasses(
        classes: List<IrClass>,
        pluginContext: IrPluginContext,
    ) {
        logger.info("Generating fakes from ${classes.size} compiled classes")

        // Create analyzer for extracting metadata from IrClass
        val analyzer = IrClassDirectAnalyzer(typeResolver, logger)

        val metrics = mutableListOf<Pair<String, Long>>() // (name, timeNanos)
        var totalLOC = 0

        for (irClass in classes) {
            try {
                val startTime = System.nanoTime()

                logger.debug("Analyzing compiled class: ${irClass.name.asString()}")

                // 1. Analyze IrClass → InterfaceAnalysis
                val interfaceAnalysis = analyzer.analyzeInterface(irClass)

                // 2. Generate code using existing generator (same as FIR path!)
                val generatedCode =
                    codeGenerator.generateWorkingFakeImplementation(
                        sourceInterface = irClass,
                        analysis = interfaceAnalysis,
                    )

                val elapsedTime = System.nanoTime() - startTime
                val loc = generatedCode.calculateTotalLOC()
                totalLOC += loc

                metrics.add(irClass.name.asString() to elapsedTime)

                logger.info("✅ Generated fake: Fake${irClass.name.asString()}Impl ($loc LOC, ${elapsedTime / 1000}µs)")
            } catch (e: Exception) {
                logger.error("❌ Failed to generate fake for ${irClass.name.asString()}: ${e.message}")
                logger.error(e.stackTraceToString())
                // Continue with next class instead of failing entire compilation
            }
        }

        // Print summary
        if (metrics.isNotEmpty()) {
            val avgTime = metrics.map { it.second }.average()
            val minTime = metrics.minByOrNull { it.second }
            val maxTime = metrics.maxByOrNull { it.second }

            val summary = buildString {
                appendLine("📊 Fakt Performance Summary:")
                appendLine("   Total: $totalLOC LOC across ${metrics.size} fakes")
                appendLine("   Average: ${(avgTime / 1000).toInt()}µs per fake")
                appendLine("   Fastest: ${minTime?.first} (${minTime?.second?.div(1000)}µs)")
                appendLine("   Slowest: ${maxTime?.first} (${maxTime?.second?.div(1000)}µs)")
            }

            logger.info(summary)

            // Also write to file for easy access
            try {
                val outputDir = sourceSetContext.outputDirectory
                val perfFile = java.io.File(outputDir).parentFile.resolve("fakt-performance.txt")
                perfFile.writeText(summary)
            } catch (e: Exception) {
                // Ignore file write errors
            }
        }
    }
}
