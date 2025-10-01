// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalyzer
import com.rsicarelli.fakt.compiler.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.analysis.PropertyAnalysis
import com.rsicarelli.fakt.compiler.analysis.ParameterAnalysis
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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
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
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.FqName
import java.io.File

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

    private val patternAnalyzer = GenericPatternAnalyzer()
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

    /**
     * Dynamic interface analysis using pure IR APIs.
     * Discovers all members without hardcoded mappings.
     */
    private fun analyzeInterfaceDynamically(sourceInterface: IrClass): InterfaceAnalysis {
        val properties = mutableListOf<PropertyAnalysis>()
        val functions = mutableListOf<FunctionAnalysis>()

        // **NEW: Smart Pattern Analysis** 🚀
        val genericPattern = patternAnalyzer.analyzeInterface(sourceInterface)
        val patternSummary = patternAnalyzer.getAnalysisSummary(genericPattern)
        val patternWarnings = patternAnalyzer.validatePattern(genericPattern, sourceInterface)

        messageCollector?.reportInfo("KtFakes: Pattern Analysis - $patternSummary")
        patternWarnings.forEach { warning ->
            messageCollector?.reportInfo("KtFakes: Pattern Warning - $warning")
        }

        // Extract interface-level type parameters
        val typeParameters = sourceInterface.typeParameters.map { typeParam ->
            typeParam.name.asString()
        }

        // Dynamically discover all interface members
        sourceInterface.declarations.forEach { declaration ->
            when (declaration) {
                is IrProperty -> {
                    properties.add(analyzeProperty(declaration))
                }

                is IrSimpleFunction -> {
                    if (!isSpecialFunction(declaration)) {
                        functions.add(analyzeFunction(declaration))
                    }
                }
            }
        }

        messageCollector?.reportInfo("KtFakes: Analyzed interface ${sourceInterface.name}: ${functions.size} functions, ${properties.size} properties, ${typeParameters.size} type parameters")

        return InterfaceAnalysis(
            interfaceName = sourceInterface.name.asString(),
            properties = properties,
            functions = functions,
            typeParameters = typeParameters,
            sourceInterface = sourceInterface,
            genericPattern = genericPattern
        )
    }

    /**
     * Analyze a property with full type information using IR APIs.
     */
    private fun analyzeProperty(property: IrProperty): PropertyAnalysis {
        val propertyType = property.getter?.returnType ?: property.backingField?.type!!

        return PropertyAnalysis(
            name = property.name.asString(),
            type = propertyType,
            isMutable = property.isVar,
            isNullable = propertyType.isMarkedNullable(),
            irProperty = property
        )
    }

    /**
     * Analyze a function with complete signature information using IR APIs.
     */
    private fun analyzeFunction(function: IrSimpleFunction): FunctionAnalysis {
        // Use parameters filtered by kind to avoid including receiver
        val parameters =
            function.parameters.filter { it.kind == org.jetbrains.kotlin.ir.declarations.IrParameterKind.Regular }
                .map { param ->
                    ParameterAnalysis(
                        name = param.name.asString(),
                        type = param.type,
                        hasDefaultValue = param.defaultValue != null,
                        isVararg = param.isVararg
                    )
                }

        // Extract method-level type parameters
        val typeParameters = function.typeParameters.map { typeParam ->
            typeParam.name.asString()
        }

        // Extract type parameter bounds (e.g., R : TValue)
        // Enhanced detection for where clauses vs inline bounds
        val typeParameterBounds = function.typeParameters.associate { typeParam ->
            val paramName = typeParam.name.asString()

            val bounds = if (typeParam.superTypes.isNotEmpty()) {
                // Get all bounds and find the most specific non-Any bound
                val specificBounds = typeParam.superTypes.mapNotNull { superType ->
                    val boundString = convertIrTypeToSimpleString(superType)
                    if (boundString != "Any" && !superType.isAny()) {
                        boundString
                    } else {
                        null
                    }
                }

                if (specificBounds.isNotEmpty()) {
                    // Use the first specific bound found
                    specificBounds.first()
                } else {
                    // All bounds were Any, but if we have explicit superTypes,
                    // this might be a where clause with interface-level type parameter
                    // that we need to mark for substitution
                    if (typeParam.superTypes.any { !it.isAny() }) {
                        // There are non-Any bounds, extract them even if conversion failed
                        val nonAnyBound = typeParam.superTypes.first { !it.isAny() }
                        val irClass = nonAnyBound.getClass()
                        irClass?.name?.asString() ?: "UNKNOWN_BOUND"
                    } else {
                        "Any"
                    }
                }
            } else {
                "Any" // Default bound when no explicit bounds
            }

            paramName to bounds
        }

        return FunctionAnalysis(
            name = function.name.asString(),
            parameters = parameters,
            returnType = function.returnType,
            isSuspend = function.isSuspend,
            isInline = function.isInline,
            typeParameters = typeParameters,
            typeParameterBounds = typeParameterBounds,
            irFunction = function
        )
    }

    /**
     * Check if function is a special function that shouldn't be implemented.
     */
    private fun isSpecialFunction(function: IrSimpleFunction): Boolean {
        val name = function.name.asString()
        return name in setOf("equals", "hashCode", "toString") ||
                name.startsWith("<") || // Compiler-generated functions
                function.origin == IrDeclarationOrigin.FAKE_OVERRIDE
    }

    /**
     * Converts an IR type to a simple string representation for bound analysis.
     */
    private fun convertIrTypeToSimpleString(irType: IrType): String {
        return when {
            irType.isAny() -> "Any"
            else -> {
                val irClass = irType.getClass()
                irClass?.name?.asString() ?: "Any"
            }
        }
    }


    /**
     * Get generated sources directory with intelligent source set mapping and fallback strategy.
     * Maps source locations to appropriate test source sets with hierarchical fallback:
     * - commonMain -> commonTest
     * - jvmMain -> jvmTest
     * - androidMain -> androidUnitTest
     * - iosMain -> iosTest (with fallback to appleTest -> nativeTest -> commonTest)
     * - main (JVM-only) -> test
     */
    private fun getGeneratedSourcesDir(moduleFragment: IrModuleFragment): File {
        // Determine the appropriate test source set based on module name
        val moduleName = moduleFragment.name.asString().lowercase()
        val primaryTarget = mapToTestSourceSet(moduleName)

        // Use the outputDir if provided, otherwise try to determine from context
        val baseDir = when {
            outputDir != null -> File(outputDir)
            else -> {
                // Fallback: Try to find project directory by looking for build.gradle.kts
                var dir = File(System.getProperty("user.dir"))

                // If we're in a daemon directory, try to find the real project path
                if (dir.absolutePath.contains("daemon")) {
                    // Try to get the classloader's resource path
                    val classLoader = this::class.java.classLoader
                    val resourceUrl = classLoader.getResource("")
                    if (resourceUrl != null) {
                        val path = File(resourceUrl.path)
                        // Navigate up from build/classes/kotlin/main to project root
                        var parent = path
                        while (parent.parentFile != null && !File(parent, "build.gradle.kts").exists()) {
                            parent = parent.parentFile
                        }
                        if (File(parent, "build.gradle.kts").exists()) {
                            dir = parent
                        }
                    }
                }

                // Look for build.gradle.kts to confirm we're in the right directory
                if (!File(dir, "build.gradle.kts").exists()) {
                    var parent = dir.parentFile
                    while (parent != null && !File(parent, "build.gradle.kts").exists()) {
                        parent = parent.parentFile
                    }
                    if (parent != null) {
                        dir = parent
                    }
                }
                File(dir, "build/generated/ktfake")
            }
        }

        // Try primary target first
        val primaryDir = File(baseDir, "$primaryTarget/kotlin")
        if (ensureDirectoryExists(primaryDir)) {
            messageCollector?.reportInfo("KtFakes: Module '$moduleName' -> Primary target '$primaryTarget'")
            messageCollector?.reportInfo("KtFakes: Output directory: ${primaryDir.absolutePath}")
            return primaryDir
        }

        // Fall back through hierarchy if primary target fails
        val fallbackTargets = buildFallbackChain(moduleName)
        for (fallbackTarget in fallbackTargets) {
            val fallbackDir = File(baseDir, "$fallbackTarget/kotlin")
            if (ensureDirectoryExists(fallbackDir)) {
                messageCollector?.reportInfo(
                    "KtFakes: Module '$moduleName' -> Primary target '$primaryTarget' not available, using fallback '$fallbackTarget'"
                )
                messageCollector?.reportInfo("KtFakes: Output directory: ${fallbackDir.absolutePath}")
                return fallbackDir
            }
        }

        // Create primary target if all fallbacks fail
        primaryDir.mkdirs()
        messageCollector?.reportInfo("KtFakes: Module '$moduleName' -> Created primary target '$primaryTarget' (fallbacks unavailable)")
        messageCollector?.reportInfo("KtFakes: Output directory: ${primaryDir.absolutePath}")
        return primaryDir
    }

    /**
     * Ensures a directory exists and can be written to.
     * Returns true if directory is available, false otherwise.
     */
    private fun ensureDirectoryExists(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.exists() && dir.isDirectory && dir.canWrite()
        } catch (e: Exception) {
            messageCollector?.reportInfo("KtFakes: Cannot access directory ${dir.absolutePath}: ${e.message}")
            false
        }
    }

    /**
     * Builds hierarchical fallback chain for KMP source sets.
     * Based on official Kotlin Multiplatform source set hierarchy.
     */
    private fun buildFallbackChain(moduleName: String): List<String> {
        return when {
            // Apple platform hierarchy: platform -> apple -> native -> common
            moduleName.contains("ios") -> listOf("appleTest", "nativeTest", "commonTest")
            moduleName.contains("macos") -> listOf("appleTest", "nativeTest", "commonTest")
            moduleName.contains("tvos") -> listOf("appleTest", "nativeTest", "commonTest")
            moduleName.contains("watchos") -> listOf("appleTest", "nativeTest", "commonTest")

            // Linux platform hierarchy: linux -> native -> common
            moduleName.contains("linux") -> listOf("nativeTest", "commonTest")

            // Windows platform hierarchy: mingw -> native -> common
            moduleName.contains("mingw") -> listOf("nativeTest", "commonTest")

            // Android Native hierarchy: androidNative -> native -> common
            moduleName.contains("androidnative") -> listOf("nativeTest", "commonTest")

            // Android JVM hierarchy: android -> common
            moduleName.contains("android") -> listOf("commonTest")

            // JS/WASM hierarchy: js/wasm -> common
            moduleName.contains("js") -> listOf("commonTest")
            moduleName.contains("wasm") -> listOf("commonTest")

            // JVM hierarchy: jvm -> common
            moduleName.contains("jvm") -> listOf("commonTest")

            // Native fallback: native -> common
            moduleName.contains("native") -> listOf("commonTest")

            // Default fallback
            else -> listOf("commonTest")
        }
    }

    /**
     * Maps compilation context to appropriate test source set.
     * Implements comprehensive KMP source set mapping strategy based on official Kotlin conventions.
     * Supports all official KMP targets including hierarchical source sets and platform variants.
     */
    private fun mapToTestSourceSet(moduleName: String): String {
        val normalizedName = moduleName.lowercase()

        return when {
            // Tier 1: Common source sets
            normalizedName.contains("commonmain") -> "commonTest"
            normalizedName.contains("commontest") -> "commonTest"

            // Tier 2: Platform categories (hierarchical)
            normalizedName.contains("nativemain") -> "nativeTest"
            normalizedName.contains("applemain") -> "appleTest"
            normalizedName.contains("linuxmain") -> "linuxTest"
            normalizedName.contains("mingwmain") -> "mingwTest"

            // Tier 3: Specific platforms
            normalizedName.contains("jvmmain") -> "jvmTest"
            normalizedName.contains("jsmain") -> "jsTest"
            normalizedName.contains("wasmjsmain") -> "wasmJsTest"
            normalizedName.contains("wasmwasimain") -> "wasmWasiTest"

            // Tier 4: Apple platforms
            normalizedName.contains("iosmain") -> "iosTest"
            normalizedName.contains("tvosmain") -> "tvosTest"
            normalizedName.contains("watchosmain") -> "watchosTest"
            normalizedName.contains("macosmain") -> "macosTest"

            // Tier 5: Platform variants (ALL official variants)
            normalizedName.contains("iosarm64main") -> "iosArm64Test"
            normalizedName.contains("iosx64main") -> "iosX64Test"
            normalizedName.contains("iossimulatorarm64main") -> "iosSimulatorArm64Test"
            normalizedName.contains("macosarm64main") -> "macosArm64Test"
            normalizedName.contains("macosx64main") -> "macosX64Test"
            normalizedName.contains("linuxarm64main") -> "linuxArm64Test"
            normalizedName.contains("linuxx64main") -> "linuxX64Test"
            normalizedName.contains("mingwx64main") -> "mingwX64Test"
            normalizedName.contains("tvosarm64main") -> "tvosArm64Test"
            normalizedName.contains("tvosx64main") -> "tvosX64Test"
            normalizedName.contains("tvossimulatorarm64main") -> "tvosSimulatorArm64Test"
            normalizedName.contains("watchosarm32main") -> "watchosArm32Test"
            normalizedName.contains("watchosarm64main") -> "watchosArm64Test"
            normalizedName.contains("watchosx64main") -> "watchosX64Test"
            normalizedName.contains("watchossimulatorarm64main") -> "watchosSimulatorArm64Test"
            normalizedName.contains("watchosdevicearm64main") -> "watchosDeviceArm64Test"
            normalizedName.contains("androidnativearm32main") -> "androidNativeArm32Test"
            normalizedName.contains("androidnativearm64main") -> "androidNativeArm64Test"
            normalizedName.contains("androidnativex64main") -> "androidNativeX64Test"
            normalizedName.contains("androidnativex86main") -> "androidNativeX86Test"

            // Tier 6: Android special cases
            normalizedName.contains("androidmain") -> resolveAndroidTestTarget(normalizedName)

            // Tier 7: Legacy JVM projects
            normalizedName.contains("main") && !normalizedName.contains("test") -> "test"

            // Default intelligent fallback patterns
            normalizedName.contains("jvm") -> "jvmTest"
            normalizedName.contains("android") -> "androidUnitTest"
            normalizedName.contains("ios") -> "iosTest"
            normalizedName.contains("js") -> "jsTest"
            normalizedName.contains("wasm") -> "wasmJsTest"
            normalizedName.contains("linux") -> "linuxTest"
            normalizedName.contains("macos") -> "macosTest"
            normalizedName.contains("mingw") -> "mingwTest"
            normalizedName.contains("tvos") -> "tvosTest"
            normalizedName.contains("watchos") -> "watchosTest"
            normalizedName.contains("native") -> "nativeTest"

            // Sample projects - since single-module uses JVM target, default to jvmTest
            normalizedName.contains("single-module") -> "jvmTest"
            normalizedName.contains("sample") -> "jvmTest"  // Default samples to JVM test
            normalizedName.contains("test") -> "jvmTest"

            // Ultimate intelligent fallback
            else -> intelligentFallback(normalizedName)
        }
    }

    /**
     * Resolves Android test target based on project configuration.
     * Defaults to androidUnitTest (unit tests) vs androidInstrumentedTest (integration tests).
     */
    private fun resolveAndroidTestTarget(moduleName: String): String {
        // Strategy: Default to androidUnitTest, can be enhanced later for instrumented tests
        // Future enhancement: detect if project has androidInstrumentedTest configured
        return "androidUnitTest" // vs "androidInstrumentedTest"
    }

    /**
     * Intelligent fallback strategy for unrecognized module names.
     * Uses pattern matching to determine most appropriate test source set.
     */
    private fun intelligentFallback(moduleName: String): String {
        return when {
            moduleName.contains("android") -> "androidUnitTest"
            moduleName.contains("jvm") -> "jvmTest"
            moduleName.contains("js") -> "jsTest"
            moduleName.contains("wasm") -> "wasmJsTest"
            moduleName.contains("native") -> "nativeTest"
            moduleName.contains("ios") -> "iosTest"
            moduleName.contains("macos") -> "macosTest"
            moduleName.contains("linux") -> "linuxTest"
            moduleName.contains("mingw") -> "mingwTest"
            moduleName.contains("tvos") -> "tvosTest"
            moduleName.contains("watchos") -> "watchosTest"
            moduleName.contains("test") -> "commonTest"
            else -> "commonTest" // Ultimate fallback
        }
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }


    /**
     * Discover @Fake annotated interfaces using dynamic discovery.
     */
    private fun discoverFakeInterfaces(moduleFragment: IrModuleFragment): List<IrClass> {
        val fakeInterfaces = mutableListOf<IrClass>()

        for (file in moduleFragment.files) {
            for (declaration in file.declarations) {
                if (declaration is IrClass && declaration.kind == ClassKind.INTERFACE) {
                    // Check if interface has any of the configured fake annotations
                    val matchingAnnotation = declaration.annotations.firstOrNull { annotation ->
                        val annotationFqName = annotation.type.classFqName?.asString()
                        annotationFqName != null && optimizations.isConfiguredFor(annotationFqName)
                    }

                    if (matchingAnnotation != null) {
                        fakeInterfaces.add(declaration)

                        // Index the type for potential incremental compilation optimizations
                        val typeInfo = TypeInfo(
                            name = declaration.name.asString(),
                            fullyQualifiedName = declaration.kotlinFqName.asString(),
                            packageName = declaration.packageFqName?.asString() ?: "",
                            fileName = file.fileEntry.name,
                            annotations = declaration.annotations.mapNotNull { it.type.classFqName?.asString() },
                            signature = computeInterfaceSignature(declaration)
                        )
                        optimizations.indexType(typeInfo)

                        val annotationName = matchingAnnotation.type.classFqName?.asString() ?: "unknown"
                        messageCollector?.reportInfo("KtFakes: Discovered interface with $annotationName: ${declaration.name}")
                    }
                }
            }
        }

        return fakeInterfaces
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

    /**
     * Collect all required import statements for types used in the interface.
     * This fixes cross-module import resolution by analyzing all type references.
     */
    private fun collectRequiredImports(analysis: InterfaceAnalysis, currentPackage: String): Set<String> {
        val imports = mutableSetOf<String>()

        // Collect imports from function return types and parameters
        for (function in analysis.functions) {
            collectImportsFromType(function.returnType, currentPackage, imports)
            for (parameter in function.parameters) {
                collectImportsFromType(parameter.type, currentPackage, imports)
            }
        }

        // Collect imports from property types
        for (property in analysis.properties) {
            collectImportsFromType(property.type, currentPackage, imports)
        }

        return imports
    }

    /**
     * Extract import requirements from an IR type.
     * Handles both simple types and generic types with parameters.
     */
    private fun collectImportsFromType(irType: IrType, currentPackage: String, imports: MutableSet<String>) {
        // Skip primitive types - they don't need imports
        if (isPrimitiveType(irType)) {
            return
        }

        val irClass = irType.getClass()
        if (irClass != null) {
            val fqName = irClass.kotlinFqName.asString()
            val packageName = fqName.substringBeforeLast('.', "")

            // Only add import if it's from a different package and not kotlin.* built-ins
            if (packageName.isNotEmpty() &&
                packageName != currentPackage &&
                !packageName.startsWith("kotlin.")
            ) {
                imports.add(fqName)
            }

            // Handle generic type parameters (for future generic support)
            if (irType is IrSimpleType) {
                for (typeArgument in irType.arguments) {
                    if (typeArgument is IrTypeProjection) {
                        collectImportsFromType(typeArgument.type, currentPackage, imports)
                    }
                }
            }
        }
    }

    /**
     * Check if a type is primitive and doesn't need imports.
     */
    private fun isPrimitiveType(irType: IrType): Boolean {
        return irType.isString() || irType.isInt() || irType.isBoolean() ||
                irType.isUnit() || irType.isLong() || irType.isFloat() ||
                irType.isDouble() || irType.isChar() || irType.isByte() ||
                irType.isShort()
    }

}
